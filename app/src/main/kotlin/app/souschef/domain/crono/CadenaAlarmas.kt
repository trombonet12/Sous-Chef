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
    /**
     * Avisos que suenan: cada uno vuelve a publicar la notificación de su carril en el canal de alarma (sonido
     * insistente hasta Parar, Hecho/Siguiente o 60 s). Los silenciosos no llegan aquí, solo repintan el carril.
     */
    fun publicar(avisos: List<AvisoNotificacion>)

    /**
     * Pinta una notificación por carril de la cocinada y quita las de los carriles que ya no están. Un carril que
     * está sonando no se pisa hasta que se pare.
     */
    fun carriles(
        slug: String,
        carriles: List<CarrilNotificacion>,
    )

    /** Pinta (o, con null, quita) la notificación ongoing "En marcha" de una cocinada. */
    fun enMarcha(
        slug: String,
        modelo: EnMarcha?,
    )

    /** Quita la notificación (y su sonido) de unos carriles: su paso se ha dado por hecho. */
    fun cancelarAvisos(
        slug: String,
        carriles: Collection<String>,
    )

    /** Cierre de la cocinada: fuera las notificaciones de sus carriles y la ongoing. */
    fun limpiar(slug: String)

    /** Para el sonido: las notificaciones de carril que están sonando se quitan (la ongoing se queda). */
    fun silenciar(slug: String)
}

/**
 * La cadena de alarmas (ADR-0005): [reprogramar] deja armada solo la alarma del próximo evento de todas las
 * cocinadas abiertas; [procesar] (al sonar la alarma, tras un reinicio o un cambio de hora) emite lo vencido,
 * lo persiste y arma la siguiente.
 */
@Suppress("TooManyFunctions")
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

    /**
     * Repinta la ongoing "En marcha" y las notificaciones de carril solo de las cocinadas vivas; las demás (sin
     * arrancar, caducadas) las pierden.
     */
    suspend fun repintar() {
        val ahora = reloj.ahoraMs()
        for (e in almacen.todos()) {
            val receta = recetas.obtener(e.slug)?.takeIf { vivo(e, ahora) }
            notificador.enMarcha(e.slug, receta?.let { ConstructorEnMarcha.construir(it, e, ahora) })
            notificador.carriles(e.slug, receta?.let { ConstructorCarriles.construir(it, e, ahora) }.orEmpty())
        }
    }

    /** Otra cocinada viva distinta de [slug] (solo se cocina una a la vez), o null. */
    suspend fun otraViva(slug: String): EstadoCrono? {
        val ahora = reloj.ahoraMs()
        return almacen.todos().firstOrNull { it.slug != slug && vivo(it, ahora) }
    }

    /** Abandona una cocinada: borra su estado, quita avisos y ongoing y rearma la cadena. */
    suspend fun abandonar(slug: String) {
        almacen.borrar(slug)
        notificador.limpiar(slug)
        reprogramar()
    }

    /** Para el sonido y quita los avisos de la cocinada. */
    suspend fun silenciar(slug: String) {
        notificador.silenciar(slug)
        repintar()
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
        val suenan = publicar.filter { IdsNotificacion.despiertaPantalla(it.aviso.tipo, it.aviso.silencioso) }
        if (suenan.isNotEmpty()) notificador.publicar(suenan)
        reprogramar()
    }

    private fun vivo(
        e: EstadoCrono,
        ahora: Long,
    ): Boolean = e.inicioMs != null && !SesionCrono.caducado(e, ahora) && !ConsultasCrono.todoTerminado(e)
}
