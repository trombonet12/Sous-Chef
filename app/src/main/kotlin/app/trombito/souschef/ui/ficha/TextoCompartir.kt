package app.trombito.souschef.ui.ficha

import app.trombito.souschef.domain.CENA_DESDE_MIN
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.Receta

/**
 * Texto plano de la lista de ingredientes que se está viendo, para compartirla. [textoRaciones] escribe «N raciones» o
 * «1 ración» (viene de `strings.xml`; así la función se prueba en la JVM sin Context).
 */
fun textoIngredientes(
    receta: Receta,
    raciones: Int,
    porZona: Boolean,
    textoRaciones: (Int) -> String,
): String {
    val f = Escalado.factor(raciones, receta.raciones)
    val lineas = mutableListOf("${receta.titulo} · ${textoRaciones(raciones)}")
    if (porZona) {
        receta.grupos.forEach { g ->
            lineas += ""
            lineas += g.n
            lineas += g.items.map { linea(it, f) }
        }
    } else {
        lineas += ""
        lineas += Escalado.listaGeneral(receta).map { linea(it, f) }
    }
    return lineas.joinToString("\n")
}

/**
 * «Hoy comemos/cenamos: <título> a las HH:mm. <N raciones>.» para mandar el plato del día. [servirMin] es la hora de
 * servir elegida en la ficha; [cenando] decide el verbo (véase [esCena]).
 */
fun textoHoyComemos(
    receta: Receta,
    servirMin: Int,
    raciones: Int,
    cenando: Boolean,
    textoRaciones: (Int) -> String,
): String {
    val verbo = if (cenando) "cenamos" else "comemos"
    return "Hoy $verbo: ${receta.titulo} a las ${Servir.formato(servirMin)}. ${textoRaciones(raciones)}."
}

/** Se cena si la ficha ya viene con la hora de cenar de Ajustes ([paraCenar]) o se sirve a las 17:00 o después. */
fun esCena(
    servirMin: Int,
    paraCenar: Boolean,
): Boolean = paraCenar || servirMin >= CENA_DESDE_MIN

private fun linea(
    item: Ingrediente,
    f: Double,
): String = "• " + listOf(Escalado.cantidad(item, f), item.n).filter { it.isNotBlank() }.joinToString(" ")
