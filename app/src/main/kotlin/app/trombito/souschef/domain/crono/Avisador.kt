package app.trombito.souschef.domain.crono

import app.trombito.souschef.receta.crono.TipoAviso

/** Sonido y vibración con la app en primer plano (docs/spec/app-legado.md §4.11). */
interface Avisador {
    fun sonar(tipo: TipoAviso)

    /** Triple pitido al volver de segundo plano con avisos perdidos. */
    fun resumen()

    /** Triple pitido de "todo listo". */
    fun cierre()
}
