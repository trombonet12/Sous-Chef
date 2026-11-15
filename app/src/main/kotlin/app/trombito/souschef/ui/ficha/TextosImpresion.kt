package app.trombito.souschef.ui.ficha

import android.content.Context
import app.trombito.souschef.R

/** Cuál de las dos salidas del HTML de la ficha se imprime (ronda 19). */
enum class TipoFicha {
    /** 1 o 2 páginas: ingredientes una vez, diagrama por carriles y cronología de órdenes; sin porqués. */
    HOJA,

    /** La hoja más la lectura del tiempo, la preparación, el porqué de cada paso, los cortes y la técnica. */
    COMPLETA,
}

/** Textos propios de la impresión en dos salidas (vienen de `strings_impresion.xml`; el HTML se prueba sin Context). */
data class TextosImpresion(
    val hoja: String,
    val completa: String,
    val cronologia: String,
    val empieza: String,
    val sirve: String,
    val mete: String,
    val avisos: String,
    val senal: String,
    val saca: String,
    val porque: String,
    val minuto: (String) -> String,
) {
    companion object {
        /** Sin etiquetas: solo para llamadas que aún no pasan los textos de la impresión. */
        val VACIO =
            TextosImpresion(
                hoja = "",
                completa = "",
                cronologia = "",
                empieza = "",
                sirve = "",
                mete = "",
                avisos = "",
                senal = "",
                saca = "",
                porque = "",
                minuto = { it },
            )

        fun desde(contexto: Context): TextosImpresion {
            val r = contexto.resources
            return TextosImpresion(
                hoja = r.getString(R.string.impr_hoja),
                completa = r.getString(R.string.impr_completa),
                cronologia = r.getString(R.string.impr_cronologia),
                empieza = r.getString(R.string.impr_empieza),
                sirve = r.getString(R.string.impr_sirve),
                mete = r.getString(R.string.impr_mete),
                avisos = r.getString(R.string.impr_avisos),
                senal = r.getString(R.string.impr_senal),
                saca = r.getString(R.string.impr_saca),
                porque = r.getString(R.string.impr_porque),
                minuto = { m -> r.getString(R.string.impr_min, m) },
            )
        }
    }
}
