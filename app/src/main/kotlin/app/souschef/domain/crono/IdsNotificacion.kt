package app.souschef.domain.crono

import app.souschef.receta.crono.EstadoCrono
import app.souschef.receta.crono.TipoAviso

/** Ids de las notificaciones: una por carril (tag `carril:<slug>`, id estable) . */
object IdsNotificacion {
    const val ID_RESUMEN = 2

    /** Tope del sonido insistente de un aviso (el sistema lo corta al caducar la notificación). */
    const val CADUCIDAD_ALARMA_MS = 60_000L

    fun carril(carril: String): Int = carril.hashCode()

    fun tagCarril(slug: String): String = "carril:$slug"

    fun grupoCarriles(slug: String): String = "carriles:$slug"

    /** Tag de la antigua notificación "En marcha" (ya no se publica; se cancela para limpiar versiones previas). */
    fun tagEnMarcha(slug: String): String = "enmarcha:$slug"

    /** Id que tenía la antigua notificación "En marcha". */
    const val ID_EN_MARCHA = 1

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
