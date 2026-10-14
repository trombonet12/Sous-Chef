package app.souschef.domain.crono

import app.souschef.domain.RecetasRepo
import app.souschef.domain.Reloj
import app.souschef.receta.crono.Aviso
import app.souschef.receta.crono.AvisosCrono
import app.souschef.receta.crono.ConsultasCrono
import app.souschef.receta.crono.EstadoCrono
import app.souschef.receta.crono.Evento
import app.souschef.receta.crono.SesionCrono

/** Programa una única alarma del sistema (ADR-0005): la del próximo evento. Implementación Android en `data.alarma`. */
interface AlarmaSistema {
    fun programar(ms: Long)

    fun cancelar()
}

/** Un aviso listo para publicar como notificación. */
data class AvisoNotificacion(
    val slug: String,
    val tituloReceta: String,
    val carril: String,
    val aviso: Aviso,
)

interface Notificador {
    fun publicar(avisos: List<AvisoNotificacion>)
}

/**
 * La cadena de alarmas (ADR-0005): [reprogramar] deja armada solo la alarma del próximo evento de todas las
 * cocinadas abiertas; [procesar] (al sonar la alarma, tras un reinicio o un cambio de hora) emite lo vencido,
 * lo persiste y arma la siguiente.
 */
class CadenaAlarmas(
    val almacen: AlmacenCrono,
    private val recetas: RecetasRepo,
    private val reloj: Reloj,
    private val sistema: AlarmaSistema,
    private val notificador: Notificador,
) {
    /** El evento más cercano entre todas las cocinadas en marcha, o null si no hay ninguno. */
    suspend fun proximoEvento(): Evento? {
        val ahora = reloj.ahoraMs()
        return almacen
            .todos()
            .filter { vivo(it, ahora) }
            .mapNotNull { e -> recetas.obtener(e.slug)?.let { AvisosCrono.siguienteEvento(e, it) } }
            .minWithOrNull(compareBy({ it.ms }, { it.tipo.ordinal }))
    }

    suspend fun reprogramar() {
        val evento = proximoEvento()
        if (evento == null) sistema.cancelar() else sistema.programar(evento.ms)
    }

    suspend fun procesar() {
        val ahora = reloj.ahoraMs()
        val publicar = mutableListOf<AvisoNotificacion>()
        for (e in almacen.todos().filter { vivo(it, ahora) }) {
            val receta = recetas.obtener(e.slug) ?: continue
            val nombres = ConsultasCrono.carriles(receta).associate { it.id to it.nombre }
            var avisos = emptyList<Aviso>()
            almacen.modificar(e.slug) { actual ->
                val v = AvisosCrono.vencidos(actual, receta, ahora)
                avisos = v.avisos
                v.estado
            }
            publicar += avisos.map { AvisoNotificacion(e.slug, receta.titulo, nombres[it.carril] ?: it.carril, it) }
        }
        if (publicar.isNotEmpty()) notificador.publicar(publicar)
        reprogramar()
    }

    private fun vivo(
        e: EstadoCrono,
        ahora: Long,
    ): Boolean = e.inicioMs != null && !SesionCrono.caducado(e, ahora) && !ConsultasCrono.todoTerminado(e)
}
