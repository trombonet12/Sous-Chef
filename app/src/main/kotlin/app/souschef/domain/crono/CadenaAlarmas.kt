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

    /** Pinta (o, con null, quita) la notificación ongoing "En marcha" de una cocinada. */
    fun enMarcha(
        slug: String,
        modelo: EnMarcha?,
    )

    /** Cancela los avisos pendientes de unos carriles (su paso se ha dado por hecho). */
    fun cancelarAvisos(
        slug: String,
        carriles: Collection<String>,
    )

    /** Cierre de la cocinada: fuera todos sus avisos y la ongoing. */
    fun limpiar(slug: String)
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
        repintar()
    }

    /** Repinta la ongoing "En marcha" de cada cocinada guardada (o la quita si ya no hay nada en marcha). */
    suspend fun repintar() {
        val ahora = reloj.ahoraMs()
        for (e in almacen.todos()) {
            val modelo = recetas.obtener(e.slug)?.let { ConstructorEnMarcha.construir(it, e, ahora) }
            notificador.enMarcha(e.slug, modelo)
        }
    }

    /** Tras un mando: los avisos de los carriles que han avanzado dejan de tener sentido. */
    fun descartarObsoletos(
        antes: EstadoCrono?,
        despues: EstadoCrono,
    ) {
        val avanzados = IdsNotificacion.carrilesAvanzados(antes, despues)
        if (avanzados.isNotEmpty()) notificador.cancelarAvisos(despues.slug, avanzados)
    }

    /** Cierre de una cocinada: sin avisos ni ongoing. */
    fun cerrado(slug: String) = notificador.limpiar(slug)

    /** Acción "Siguiente" de la notificación. Devuelve el carril afectado o null si no había nada que hacer. */
    suspend fun siguiente(slug: String): String? {
        val receta = recetas.obtener(slug) ?: return null
        val ahora = reloj.ahoraMs()
        var antes: EstadoCrono? = null
        var carril: String? = null
        val nuevo =
            almacen.modificar(slug) { actual ->
                antes = actual
                val r = AccionSiguiente.aplicar(actual, receta, ahora)
                carril = r.carril
                r.estado
            }
        if (nuevo != null) descartarObsoletos(antes, nuevo)
        reprogramar()
        return carril
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
