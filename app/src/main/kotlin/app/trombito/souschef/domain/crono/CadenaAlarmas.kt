package app.trombito.souschef.domain.crono

import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.crono.AgrupadorAlarmas
import app.trombito.souschef.receta.crono.AlarmaActiva
import app.trombito.souschef.receta.crono.Aviso
import app.trombito.souschef.receta.crono.AvisosCrono
import app.trombito.souschef.receta.crono.ConsultasCrono
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.Evento
import app.trombito.souschef.receta.crono.SesionCrono
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/** Programa una única alarma del sistema (ADR-0005): la del próximo evento. Implementación Android en `data.alarma`. */
interface AlarmaSistema {
    fun programar(ms: Long)

    fun cancelar()
}

/**
 * Lo que enseña la notificación fija de la cocina (fuera de la app): una línea por carril.
 * [urgente] es el carril de la acción «Hecho» (el que lleva el paso vencido) o null; [finProximoMs] alimenta la cuenta
 * atrás nativa y el chip de Live Update; [llegadaMs] es la hora estimada en que se acaba todo ("llegas HH:MM").
 */
data class ResumenCocina(
    val slug: String,
    val tituloReceta: String,
    val carriles: List<CarrilNotificacion>,
    val urgente: String?,
    val finProximoMs: Long?,
    val llegadaMs: Long? = null,
)

/** Notificaciones de la cocina: una fija con todos los carriles y una única alarma que agrupa lo que coincide. */
interface Notificador {
    /** Pinta o actualiza la notificación fija de la cocina (sin sonido). */
    fun cocina(resumen: ResumenCocina)

    /**
     * Pinta la alarma única. Con [sonar] suena (insistente hasta Parar, Hecho o 60 s); sin [sonar] se actualiza en
     * silencio. [urgente] != null ofrece «Hecho y siguiente».
     */
    fun alarma(
        slug: String,
        titulo: String,
        alarma: AlarmaActiva,
        sonar: Boolean,
        urgente: String?,
    )

    /** Quita la alarma (y su sonido). */
    fun quitarAlarma(slug: String)

    /** Quita la notificación fija de la cocina. */
    fun quitarCocina(slug: String)

    /** Cierre de la cocinada: fuera las dos. */
    fun limpiar(slug: String)
}

/**
 * La cadena de alarmas (ADR-0005): [reprogramar] deja armada solo la alarma del próximo evento de todas las
 * cocinadas abiertas; [procesar] (al sonar la alarma, tras un reinicio o un cambio de hora) emite lo vencido,
 * lo persiste y arma la siguiente.
 *
 * Con la cocina en pantalla ([PresenciaCrono]) no hay notificaciones: avisa la propia app y el tic del ViewModel
 * consume los avisos. Fuera de la pantalla hay una fija con todos los carriles y una sola alarma que agrupa lo que
 * coincide (< 60 s).
 *
 * Los receptores (hilo IO) y el ViewModel (Main) la usan a la vez: los métodos públicos que leen y escriben [activas]
 * o la presencia toman un [Mutex] (no reentrante), así que por dentro se llaman las variantes `...Sl` sin cerrojo.
 */
@Suppress("TooManyFunctions")
class CadenaAlarmas(
    val almacen: AlmacenCrono,
    private val recetas: RecetasRepo,
    private val reloj: Reloj,
    private val sistema: AlarmaSistema,
    private val notificador: Notificador,
    val presencia: PresenciaCrono = PresenciaCrono(),
) {
    /** Alarma que hay ahora en la bandeja por cocinada, para agrupar lo que coincide. Solo en memoria. */
    private val activas = ConcurrentHashMap<String, AlarmaPublicada>()

    private val cerrojo = Mutex()

    private class AlarmaPublicada(
        val titulo: String,
        val alarma: AlarmaActiva,
    )

    /**
     * El evento más cercano entre todas las cocinadas en marcha, o null si no hay ninguno. La cocina que se está
     * viendo no cuenta: la avisa la app y, al salir, se vuelve a armar.
     */
    suspend fun proximoEvento(): Evento? {
        val ahora = reloj.ahoraMs()
        val visible = presencia.visible
        return almacen
            .todos()
            .filter { it.slug != visible && vivo(it, ahora) }
            .mapNotNull { e -> recetas.obtener(e.slug)?.let { AvisosCrono.siguienteEvento(e, it) } }
            .minWithOrNull(compareBy({ it.ms }, { it.tipo.ordinal }))
    }

    suspend fun reprogramar() = cerrojo.withLock { reprogramarSl() }

    private suspend fun reprogramarSl() {
        val evento = proximoEvento()
        if (evento == null) sistema.cancelar() else sistema.programar(evento.ms)
        repintarSl()
    }

    /**
     * Repinta la notificación fija de las cocinadas vivas que no se están viendo; las demás (en pantalla, sin
     * arrancar, caducadas) la pierden.
     */
    suspend fun repintar() = cerrojo.withLock { repintarSl() }

    private suspend fun repintarSl() {
        val ahora = reloj.ahoraMs()
        for (e in almacen.todos()) {
            val receta = recetas.obtener(e.slug)?.takeIf { vivo(e, ahora) }
            val resumen = if (receta == null || presencia.visible == e.slug) null else resumen(receta, e, ahora)
            if (resumen == null) notificador.quitarCocina(e.slug) else notificador.cocina(resumen)
            if (receta == null && activas.containsKey(e.slug)) quitarAlarmaDe(e.slug)
        }
    }

    private fun resumen(
        receta: Receta,
        e: EstadoCrono,
        ahora: Long,
    ): ResumenCocina? {
        val carriles = ConstructorCarriles.construir(receta, e, ahora)
        if (carriles.isEmpty()) return null
        return ResumenCocina(
            slug = e.slug,
            tituloReceta = receta.titulo,
            carriles = carriles,
            urgente = urgenteDe(carriles),
            finProximoMs = carriles.mapNotNull { it.finAtMs }.minOrNull(),
            llegadaMs = ConsultasCrono.plan(e, receta, ahora).llegadaMs,
        )
    }

    private fun urgenteDe(carriles: List<CarrilNotificacion>): String? =
        carriles.firstOrNull { it.situacion == SituacionCarril.VENCIDO }?.carril

    /** Otra cocinada viva distinta de [slug] (solo se cocina una a la vez), o null. */
    suspend fun otraViva(slug: String): EstadoCrono? {
        val ahora = reloj.ahoraMs()
        return almacen.todos().firstOrNull { it.slug != slug && vivo(it, ahora) }
    }

    /** Hay alguna cocinada viva (abierta, sin caducar y sin terminar): no se debe interrumpir con nada ajeno. */
    suspend fun hayViva(): Boolean {
        val ahora = reloj.ahoraMs()
        return almacen.todos().any { vivo(it, ahora) }
    }

    /** La cocinada de [slug] mientras está viva (abierta, sin caducar y sin terminar); null si no lo está. */
    fun viva(slug: String): Flow<EstadoCrono?> =
        almacen.observar(slug).map { e -> e?.takeIf { vivo(it, reloj.ahoraMs()) } }

    /** Abandona una cocinada: borra su estado, quita avisos y notificaciones y rearma la cadena. */
    suspend fun abandonar(slug: String) =
        cerrojo.withLock {
            almacen.borrar(slug)
            cerrado(slug)
            reprogramarSl()
        }

    /** Para el sonido y quita la alarma de la cocinada; repinta la fija si procede. */
    suspend fun silenciar(slug: String) =
        cerrojo.withLock {
            quitarAlarmaDe(slug)
            repintarSl()
        }

    /**
     * Tras un mando: los avisos de los carriles que han avanzado dejan de tener sentido. Su línea sale de la alarma
     * y, si no queda ninguna, la alarma se quita; si quedan, se actualiza en silencio.
     */
    suspend fun descartarObsoletos(
        antes: EstadoCrono?,
        despues: EstadoCrono,
    ) = cerrojo.withLock { descartarObsoletosSl(antes, despues) }

    private suspend fun descartarObsoletosSl(
        antes: EstadoCrono?,
        despues: EstadoCrono,
    ) {
        val avanzados = IdsNotificacion.carrilesAvanzados(antes, despues)
        if (avanzados.isEmpty()) return
        val actual = activas[despues.slug]
        if (actual == null) {
            // Tras una muerte del proceso `activas` está vacío pero la alarma puede seguir sonando en la bandeja.
            notificador.quitarAlarma(despues.slug)
            return
        }
        val resto = AgrupadorAlarmas.quitar(actual.alarma, avanzados)
        if (resto == null) {
            quitarAlarmaDe(despues.slug)
        } else {
            activas[despues.slug] = AlarmaPublicada(actual.titulo, resto)
            val receta = recetas.obtener(despues.slug)
            val urgente = receta?.let { urgenteDe(ConstructorCarriles.construir(it, despues, reloj.ahoraMs())) }
            notificador.alarma(despues.slug, actual.titulo, resto, sonar = false, urgente = urgente)
        }
    }

    /** Cierre de una cocinada: sin avisos ni notificaciones. */
    fun cerrado(slug: String) {
        activas.remove(slug)
        notificador.limpiar(slug)
    }

    /** Acción "Hecho y siguiente" de la notificación. Devuelve el carril afectado, o null si no había nada. */
    suspend fun siguiente(slug: String): String? =
        cerrojo.withLock {
            val receta = recetas.obtener(slug) ?: return@withLock null
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
            if (nuevo != null) descartarObsoletosSl(antes, nuevo)
            reprogramarSl()
            carril
        }

    suspend fun procesar() =
        cerrojo.withLock {
            val ahora = reloj.ahoraMs()
            val visible = presencia.visible
            // La cocina en pantalla no se toca: sus avisos los consume el tic del ViewModel y suenan dentro de la app.
            for (e in almacen.todos().filter { it.slug != visible && vivo(it, ahora) }) {
                val receta = recetas.obtener(e.slug) ?: continue
                avisarFuera(e, receta, ahora)
            }
            reprogramarSl()
        }

    /** Consume lo vencido de una cocinada que no se ve y lo publica en la alarma única, agrupándolo. */
    private suspend fun avisarFuera(
        e: EstadoCrono,
        receta: Receta,
        ahora: Long,
    ) {
        val nombres = ConsultasCrono.carriles(receta).associate { it.id to it.nombre }
        var avisos = emptyList<Aviso>()
        almacen.modificar(e.slug) { actual ->
            val v = AvisosCrono.vencidos(actual, receta, ahora)
            avisos = v.avisos
            v.estado
        }
        val lineas = AgrupadorAlarmas.lineasDe(avisos, nombres)
        if (lineas.isNotEmpty()) {
            val agrupado = AgrupadorAlarmas.combinar(activas[e.slug]?.alarma, lineas, ahora)
            activas[e.slug] = AlarmaPublicada(receta.titulo, agrupado.alarma)
            val urgente = urgenteDe(ConstructorCarriles.construir(receta, e, ahora))
            notificador.alarma(e.slug, receta.titulo, agrupado.alarma, agrupado.sonar, urgente)
        }
    }

    private fun quitarAlarmaDe(slug: String) {
        activas.remove(slug)
        notificador.quitarAlarma(slug)
    }

    private fun vivo(
        e: EstadoCrono,
        ahora: Long,
    ): Boolean = e.inicioMs != null && !SesionCrono.caducado(e, ahora) && !ConsultasCrono.todoTerminado(e)
}
