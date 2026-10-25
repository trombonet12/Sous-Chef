package app.trombito.souschef.domain.copia

import app.trombito.souschef.domain.Reloj
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/** Cómo acabó un intento de copia automática. */
sealed interface ResultadoAuto {
    /** Escrita, releída bien y rotada; [borradas] son las copias viejas que se han quitado. */
    data class Hecha(
        val borradas: Int,
    ) : ResultadoAuto

    /** No hay carpeta elegida: no se hace nada. */
    data object SinCarpeta : ResultadoAuto

    /** No salió; el fallo ya está guardado en `meta`. */
    data class Fallida(
        val fallo: FalloCopiaAuto,
    ) : ResultadoAuto
}

/**
 * Copia automática del historial en la carpeta elegida (ADR-0015). Cada cierre escribe un archivo nuevo con la copia v2
 * completa, lo relee, y solo entonces rota las viejas (se quedan las [RotacionCopias.CONSERVAR] más recientes).
 *
 * Corre en [ambito] (el de la aplicación) sobre [contexto] (IO), con un [Mutex] para que no se solapen dos copias y
 * sin WorkManager: si el proceso muere antes de escribir, la copia siguiente lo recoge porque lleva el historial
 * entero.
 */
@Suppress("LongParameterList") // dependencias de un servicio de aplicación: cada una es un colaborador distinto
class CopiaAutomatica(
    private val gestor: GestorCopia,
    private val carpeta: CarpetaCopia,
    private val estado: CopiaAutoRepo,
    private val reloj: Reloj,
    private val ambito: CoroutineScope,
    private val contexto: CoroutineContext = Dispatchers.IO,
    private val zona: ZoneId = ZoneId.systemDefault(),
    /** Se llama con la clase de una excepción inesperada (nunca con la URI ni con el mensaje). */
    private val alFallar: (String) -> Unit = {},
) {
    private val candado = Mutex()
    private val encolada = AtomicBoolean(false)

    /**
     * Pide una copia sin esperarla. Si ya hay una esperando su turno no se encola otra: la que espera leerá el
     * historial cuando le toque, así que ya lleva lo último.
     */
    fun solicitar(): Job? {
        if (!encolada.compareAndSet(false, true)) return null
        val manejador = CoroutineExceptionHandler { _, e -> alFallar(e.javaClass.simpleName) }
        return ambito.launch(contexto + manejador) {
            candado.withLock {
                encolada.set(false)
                copiarAhora()
            }
        }
    }

    /**
     * Elige [uri] como carpeta (el selector ya devolvió el permiso). Suelta el permiso de la anterior si era otra, y
     * pide una primera copia para comprobar enseguida que la carpeta funciona. false si el sistema no deja guardar el
     * permiso: entonces no cambia nada.
     */
    suspend fun elegirCarpeta(uri: String): Boolean {
        val anterior = estado.leer().carpeta
        if (!carpeta.adoptar(uri)) return false
        estado.fijarCarpeta(uri)
        if (anterior != null && anterior != uri) carpeta.soltar(anterior)
        solicitar()
        return true
    }

    /** Deja de copiar: suelta el permiso y olvida la carpeta y el fallo. Las copias ya hechas se quedan donde están. */
    suspend fun desactivar() {
        val anterior = estado.leer().carpeta
        estado.fijarCarpeta(null)
        if (anterior != null) carpeta.soltar(anterior)
    }

    /**
     * Una copia ahora, esperando a que acabe. Orden: bytes validados, archivo nuevo escrito con [EscrituraSegura],
     * releído y válido, y solo entonces la rotación. Si falla la relectura se borra el archivo nuevo y no se rota.
     */
    suspend fun copiarAhora(): ResultadoAuto {
        val destino = estado.leer().carpeta ?: return ResultadoAuto.SinCarpeta
        val copia = gestor.preparar()
        val ahora = reloj.ahoraMs()
        return when (val escrita = escribirYReleer(destino, copia, ahora)) {
            is ResultadoCreacion.Creado -> {
                estado.registrarCopia(ahora)
                ResultadoAuto.Hecha(rotar(destino, escrita.documento))
            }

            ResultadoCreacion.CarpetaNoVale -> {
                fallar(FalloCopiaAuto.CARPETA_NO_VALE)
            }

            is ResultadoCreacion.Rechazada -> {
                fallar(falloDe(escrita.motivo))
            }
        }
    }

    /** Crea el archivo nuevo y lo relee; si la relectura no pasa, lo borra y lo da por rechazado (no se rota). */
    private suspend fun escribirYReleer(
        destino: String,
        copia: CopiaPreparada,
        ahora: Long,
    ): ResultadoCreacion {
        val creado = carpeta.crear(destino, NombresCopiaAuto.nombre(ahora, zona), copia.bytes, copia.valida)
        val releidaBien =
            (creado as? ResultadoCreacion.Creado)?.let { carpeta.leer(it.documento)?.let(copia.valida) ?: false }
        if (creado is ResultadoCreacion.Creado && releidaBien != true) {
            carpeta.borrar(creado.documento)
            return ResultadoCreacion.Rechazada(ResultadoEscritura.Fallo)
        }
        return creado
    }

    /** Borra las copias automáticas que sobran; si no se puede listar o borrar, la copia nueva sigue siendo buena. */
    private suspend fun rotar(
        destino: String,
        nuevo: String,
    ): Int {
        val existentes = carpeta.listar(destino) ?: return 0
        return RotacionCopias.aBorrar(existentes, nuevo).count { carpeta.borrar(it.id) }
    }

    private suspend fun fallar(fallo: FalloCopiaAuto): ResultadoAuto {
        estado.registrarFallo(fallo)
        return ResultadoAuto.Fallida(fallo)
    }

    private fun falloDe(motivo: ResultadoEscritura): FalloCopiaAuto =
        if (motivo == ResultadoEscritura.SinPermiso) FalloCopiaAuto.CARPETA_NO_VALE else FalloCopiaAuto.NO_SE_PUDO
}
