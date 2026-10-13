package app.souschef.ui.shell

import androidx.annotation.StringRes
import app.souschef.R

/** Pestañas de la barra inferior (decisión del usuario: tres; el resto en el menú). */
enum class Pestana(
    val ruta: String,
    @param:StringRes val titulo: Int,
    val icono: Icono,
) {
    Recetas("recetas", R.string.tab_recetas, Icono.Libro),
    Despensa("despensa", R.string.tab_despensa, Icono.Tarro),
    Historial("historial", R.string.tab_historial, Icono.Reloj),
}
