package app.trombito.souschef.ui.comun

import java.text.Normalizer
import java.time.LocalDate

private val MARCAS_DIACRITICAS = Regex("\\p{Mn}+")
private val NEGRITA = Regex("<b>(.*?)</b>", RegexOption.DOT_MATCHES_ALL)
private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** Minúsculas y sin acentos, para buscar sin fijarse en ellos. */
fun String.normalizado(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(MARCAS_DIACRITICAS, "").lowercase()

/** Trozo de texto con o sin negrita (la ficha solo admite `<b>`). */
data class Trozo(
    val texto: String,
    val negrita: Boolean,
)

fun conNegritas(texto: String): List<Trozo> {
    val trozos = mutableListOf<Trozo>()
    var pos = 0
    for (m in NEGRITA.findAll(texto)) {
        if (m.range.first > pos) trozos += Trozo(texto.substring(pos, m.range.first), false)
        trozos += Trozo(m.groupValues[1], true)
        pos = m.range.last + 1
    }
    if (pos < texto.length) trozos += Trozo(texto.substring(pos), false)
    return trozos
}

/** Pares (valor, veces) ordenados por valor, para los contadores de los chips. */
fun <T> List<T>.contarPor(clave: (T) -> String?): List<Pair<String, Int>> =
    mapNotNull(clave)
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedBy { it.first }

/** "2026-09-12" -> "12 sep"; si no se entiende, el texto tal cual. */
fun fechaCorta(fecha: String): String =
    runCatching { LocalDate.parse(fecha.take(10)) }
        .map { "${it.dayOfMonth} ${MESES[it.monthValue - 1]}" }
        .getOrDefault(fecha)
