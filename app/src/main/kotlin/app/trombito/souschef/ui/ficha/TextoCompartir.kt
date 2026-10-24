package app.trombito.souschef.ui.ficha

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

private fun linea(
    item: Ingrediente,
    f: Double,
): String = "• " + listOf(Escalado.cantidad(item, f), item.n).filter { it.isNotBlank() }.joinToString(" ")
