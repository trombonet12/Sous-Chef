package app.trombito.souschef.domain.crono

import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.Reloj
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId

/** Dónde se guarda el único recordatorio (la tabla `meta`). Implementación Android en `data.repo`. */
interface RecordatorioRepo {
    suspend fun leer(): Recordatorio?

    suspend fun guardar(r: Recordatorio)

    suspend fun borrar()

    fun observar(): Flow<Recordatorio?>
}

/** La alarma del sistema del recordatorio (`setAlarmClock`, ADR-0016): aparte de la de la cadena. */
interface AlarmaRecordatorio {
    fun programar(ms: Long)

    fun cancelar()
}

/** La notificación del aviso: normal, sin pantalla completa, que abre la ficha de la receta. */
interface NotificadorRecordatorio {
    fun avisar(
        slug: String,
        titulo: String,
        servirMs: Long,
    )
}

/**
 * «Avísame para empezar» (ADR-0016). Un solo recordatorio: armar otro sustituye al anterior. Los receptores y la ficha
 * lo usan a la vez, así que todo pasa por un [Mutex].
 */
class GestorRecordatorio(
    private val repo: RecordatorioRepo,
    private val recetas: RecetasRepo,
    private val reloj: Reloj,
    private val alarma: AlarmaRecordatorio,
    private val notificador: NotificadorRecordatorio,
    private val zona: () -> ZoneId = { ZoneId.systemDefault() },
) {
    private val cerrojo = Mutex()

    /** El recordatorio puesto, para pintar el chip. */
    fun observar(): Flow<Recordatorio?> = repo.observar()

    /**
     * Arma el recordatorio para empezar [slug] a las [empezarMin] (la hora que enseña la ficha) y servir a las
     * [servirMin]. Sustituye al anterior. Devuelve false, sin tocar nada, si esa hora ya pasó.
     */
    suspend fun armar(
        slug: String,
        empezarMin: Int,
        servirMin: Int,
    ): Boolean =
        cerrojo.withLock {
            val r = RecordatorioEmpezar.crear(slug, empezarMin, servirMin, reloj.ahoraMs(), zona())
            if (r != null) {
                repo.guardar(r)
                alarma.programar(r.empezarMs)
            }
            r != null
        }

    /** Quita el recordatorio y su alarma. */
    suspend fun quitar() = cerrojo.withLock { quitarSl() }

    /**
     * Abrir cualquier cocina cancela el recordatorio (solo se cocina una a la vez). La cadena lo llama al reprogramar:
     * si hay una cocinada viva, no queda recordatorio. Sin recordatorio no consulta las cocinadas.
     */
    suspend fun quitarSiCocinando(vivas: CocinadasVivas) =
        cerrojo.withLock {
            if (repo.leer() != null && vivas.hayViva()) quitarSl()
        }

    /**
     * Relee el recordatorio guardado y actúa según [RecordatorioEmpezar.alReleer]: lo vuelve a armar, avisa en el acto
     * o lo borra. Se llama al sonar la alarma, tras un reinicio, una actualización o un cambio de hora, zona o permiso.
     * Si la receta ya no está en el catálogo, se borra sin avisar.
     */
    suspend fun procesar() =
        cerrojo.withLock {
            val r = repo.leer()
            val receta = r?.let { recetas.obtener(it.slug) }
            when (val accion = RecordatorioEmpezar.alReleer(r, reloj.ahoraMs())) {
                AccionRecordatorio.Nada -> {
                    Unit
                }

                is AccionRecordatorio.Armar -> {
                    if (receta == null) quitarSl() else alarma.programar(accion.ms)
                }

                AccionRecordatorio.Avisar -> {
                    if (r != null && receta != null) notificador.avisar(r.slug, receta.titulo, r.servirMs)
                    quitarSl()
                }

                AccionRecordatorio.Borrar -> {
                    quitarSl()
                }
            }
        }

    private suspend fun quitarSl() {
        repo.borrar()
        alarma.cancelar()
    }
}
