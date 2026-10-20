package app.trombito.souschef.ui.ficha

import androidx.annotation.StringRes
import app.trombito.souschef.R

/** Las cinco pestañas de la ficha; el orden es el de lectura mientras se cocina. */
enum class PestanaFicha(
    @StringRes val titulo: Int,
) {
    RESUMEN(R.string.ficha_tab_resumen),
    INGREDIENTES(R.string.ficha_tab_ingredientes),
    PREPARACION(R.string.ficha_tab_preparacion),
    PASOS(R.string.ficha_tab_pasos),
    PORQUE(R.string.ficha_tab_porque),
}
