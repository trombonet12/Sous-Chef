package app.trombito.souschef.domain.crono

/**
 * Qué cocina se está viendo en pantalla. Solo en memoria: si el proceso muere vuelve a null, que es lo correcto
 * porque nadie la está mirando. Con la cocina visible no hay notificaciones: avisa la propia app.
 */
class PresenciaCrono {
    /** Slug de la cocina en pantalla, o null. */
    @Volatile
    var visible: String? = null
}
