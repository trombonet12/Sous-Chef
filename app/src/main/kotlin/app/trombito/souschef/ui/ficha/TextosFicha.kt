package app.trombito.souschef.ui.ficha

import android.content.Context
import app.trombito.souschef.R

/** Textos de la ficha impresa (vienen de `strings.xml`; así la función de HTML se prueba sin Context). */
data class TextosFicha(
    val ingredientes: String,
    val todoJunto: String,
    val tiempo: String,
    val mep: String,
    val pasos: String,
    val tecnica: String,
    val eje: String,
    val meto: String,
    val saco: String,
    val sinFuego: String,
    val raciones: (Int) -> String,
    val duracion: (String) -> String,
    val rango: (String, String) -> String,
    val fuego: (Int) -> String,
) {
    companion object {
        fun desde(contexto: Context): TextosFicha {
            val r = contexto.resources
            return TextosFicha(
                ingredientes = r.getString(R.string.ficha_sec_ingredientes),
                todoJunto = r.getString(R.string.ficha_ing_todo_junto),
                tiempo = r.getString(R.string.ficha_sec_tiempo),
                mep = r.getString(R.string.ficha_sec_mep),
                pasos = r.getString(R.string.ficha_sec_pasos),
                tecnica = r.getString(R.string.ficha_sec_tecnica),
                eje = r.getString(R.string.ficha_eje),
                meto = r.getString(R.string.ficha_meto),
                saco = r.getString(R.string.ficha_saco),
                sinFuego = r.getString(R.string.ficha_sin_fuego_bajo),
                raciones = { n -> "$n " + r.getQuantityString(R.plurals.ficha_unidad_racion, n) },
                duracion = { m -> r.getString(R.string.ficha_duracion, m) },
                rango = { a, b -> r.getString(R.string.ficha_min_rango, a, b) },
                fuego = { f -> r.getString(R.string.ficha_fuego_bajo, f) },
            )
        }
    }
}
