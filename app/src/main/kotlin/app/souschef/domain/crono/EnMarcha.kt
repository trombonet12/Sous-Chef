package app.souschef.domain.crono

import app.souschef.receta.Receta
import app.souschef.receta.crono.ConsultasCrono
import app.souschef.receta.crono.EstadoCrono
import app.souschef.receta.crono.SesionCrono
import app.souschef.receta.crono.TipoAviso

/**
 * Lo que dice la notificación ongoing "En marcha" de una cocinada. [finAtMs] es el instante en que acaba el paso que
 * acaba antes (cuenta atrás nativa); null si ninguno está corriendo con tiempo por delante.
 */
data class EnMarcha(
    val slug: String,
    val titulo: String,
    val finAtMs: Long?,
    /** Tiempo corto para el chip de la barra de estado (Android 16). */
    val cortoCritico: String,
    /** Primera línea: el paso más urgente. */
    val resumen: String,
    /** Una línea por carril vivo: "SARTÉN · Tostar el sésamo · 2:10". */
    val lineas: List<String>,
)

object ConstructorEnMarcha {
    private const val SEGUNDOS_HORA = 3600
    private const val SEGUNDOS_MINUTO = 60

    /** null si la cocinada no está abierta, ha caducado o ya ha terminado: no hay nada que mostrar. */
    fun construir(
        receta: Receta,
        e: EstadoCrono,
        ahoraMs: Long,
    ): EnMarcha? {
        val abierta = e.inicioMs != null && !SesionCrono.caducado(e, ahoraMs) && !ConsultasCrono.todoTerminado(e)
        val filas = if (abierta) filas(receta, e, ahoraMs) else emptyList()
        return if (filas.isEmpty()) null else componer(receta, e, filas)
    }

    private fun filas(
        receta: Receta,
        e: EstadoCrono,
        ahoraMs: Long,
    ): List<Fila> =
        ConsultasCrono.carriles(receta).mapNotNull { c ->
            val ec = e.carriles[c.id]?.takeUnless { it.fin } ?: return@mapNotNull null
            val paso = receta.pasos.filter { it.carril == c.id }.getOrNull(ec.i) ?: return@mapNotNull null
            val q = ConsultasCrono.quedan(e, receta, c.id, ahoraMs)
            val estado =
                when {
                    ec.corriendo && q <= 0 -> "¡listo!"
                    ec.corriendo -> reloj(q)
                    ec.pausado -> "en pausa ${reloj(q)}"
                    else -> "sin arrancar"
                }
            Fila("${c.nombre.uppercase()} · ${paso.t} · $estado", ec.finAtMs.takeIf { ec.corriendo }, q)
        }

    private fun componer(
        receta: Receta,
        e: EstadoCrono,
        filas: List<Fila>,
    ): EnMarcha {
        val vencida = filas.firstOrNull { it.finAtMs != null && it.quedan <= 0 }
        val proxima = filas.filter { it.finAtMs != null && it.quedan > 0 }.minByOrNull { it.finAtMs ?: Long.MAX_VALUE }
        return EnMarcha(
            slug = e.slug,
            titulo = receta.titulo,
            finAtMs = if (vencida == null) proxima?.finAtMs else null,
            cortoCritico = if (vencida == null) proxima?.let { reloj(it.quedan) }.orEmpty() else "¡Hecho!",
            resumen = (vencida ?: proxima ?: filas.first()).texto,
            lineas = filas.map { it.texto },
        )
    }

    /** "m:ss" o "h:mm:ss". */
    fun reloj(segundos: Int): String {
        val s = maxOf(0, segundos)
        val h = s / SEGUNDOS_HORA
        val m = s % SEGUNDOS_HORA / SEGUNDOS_MINUTO
        val resto = s % SEGUNDOS_MINUTO
        return if (h > 0) "%d:%02d:%02d".format(h, m, resto) else "%d:%02d".format(m, resto)
    }

    private class Fila(
        val texto: String,
        val finAtMs: Long?,
        val quedan: Int,
    )
}

/** Ids de las notificaciones: una por carril (tag `carril:<slug>`, id estable) y la ongoing "En marcha". */
object IdsNotificacion {
    const val ID_EN_MARCHA = 1
    const val ID_RESUMEN = 2

    /** Tope del sonido insistente de un aviso (el sistema lo corta al caducar la notificación). */
    const val CADUCIDAD_ALARMA_MS = 60_000L

    fun carril(carril: String): Int = carril.hashCode()

    fun tagCarril(slug: String): String = "carril:$slug"

    fun grupoCarriles(slug: String): String = "carriles:$slug"

    fun tagEnMarcha(slug: String): String = "enmarcha:$slug"

    /** Avisos que despiertan la pantalla y suenan: los que piden una acción o marcan un hito, si no llegan tarde. */
    fun despiertaPantalla(
        tipo: TipoAviso,
        silencioso: Boolean,
    ): Boolean = !silencioso && tipo != TipoAviso.DIEZ_S

    /** Carriles cuyo paso ha cambiado (Hecho) o terminado entre dos estados: sus avisos ya no valen. */
    fun carrilesAvanzados(
        antes: EstadoCrono?,
        despues: EstadoCrono,
    ): Set<String> {
        if (antes == null) return emptySet()
        return despues.carriles
            .filter { (id, ec) ->
                val previo = antes.carriles[id]
                previo != null && (previo.i != ec.i || previo.fin != ec.fin)
            }.keys
    }
}
