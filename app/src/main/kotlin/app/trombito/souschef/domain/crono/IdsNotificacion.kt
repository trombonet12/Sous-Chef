package app.trombito.souschef.domain.crono

import app.trombito.souschef.receta.crono.EstadoCrono

/** Ids y tags de las notificaciones: una fija de cocina y una de alarma por cocinada (el tag lleva el slug). */
object IdsNotificacion {
    /** La notificación fija con todos los carriles (fuera de la app). */
    const val ID_COCINA = 3

    /** La única notificación de alarma: agrupa lo que coincide. */
    const val ID_ALARMA = 4

    /** Tope del sonido insistente de la alarma (el sistema la quita al caducar). */
    const val CADUCIDAD_ALARMA_MS = 60_000L

    /** El aviso «Avísame para empezar» (ADR-0016): una notificación normal, aparte de la cocina y la alarma. */
    const val ID_RECORDATORIO = 5

    fun tagRecordatorio(slug: String): String = "recordatorio:$slug"

    fun tagCocina(slug: String): String = "cocina:$slug"

    fun tagAlarma(slug: String): String = "alarma:$slug"

    /** Tag de las antiguas notificaciones por carril (ya no se publican; se cancelan al limpiar). */
    fun tagCarrilAntiguo(slug: String): String = "carril:$slug"

    /** Tag de la antigua notificación "En marcha" (ya no se publica; se cancela para limpiar versiones previas). */
    fun tagEnMarcha(slug: String): String = "enmarcha:$slug"

    /** Id que tenía la antigua notificación "En marcha". */
    const val ID_EN_MARCHA = 1

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
