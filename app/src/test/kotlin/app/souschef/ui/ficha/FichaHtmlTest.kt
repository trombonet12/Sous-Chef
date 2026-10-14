package app.souschef.ui.ficha

import app.souschef.receta.BloqueMep
import app.souschef.receta.Carril
import app.souschef.receta.Corte
import app.souschef.receta.Grupo
import app.souschef.receta.Hito
import app.souschef.receta.Ingrediente
import app.souschef.receta.Paso
import app.souschef.receta.Receta
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FichaHtmlTest {
    private val textos =
        TextosFicha(
            ingredientes = "SEC_ING",
            tiempo = "SEC_TIEMPO",
            mep = "SEC_MEP",
            pasos = "SEC_PASOS",
            tecnica = "SEC_TECNICA",
            compra = "SEC_COMPRA",
            compraVacia = "vacia",
            eje = "EJE",
            meto = "Meto",
            saco = "Saco",
            sinFuego = "sin fuego",
            raciones = { "$it raciones" },
            duracion = { "$it′" },
            rango = { a, b -> "min $a-$b" },
            fuego = { "fuego $it" },
        )

    private val svg = "<svg viewBox=\"0 0 10 10\"><circle r=\"3\"/></svg>"

    private val receta =
        Receta(
            slug = "x",
            titulo = "Arroz <script>alert(1)</script>",
            descripcion = "Rico & sano",
            raciones = 2,
            totalMin = 30.0,
            datos = listOf(listOf("30", "min")),
            compra = listOf(Ingrediente("limón", a = 2.0, u = "ud")),
            carriles = listOf(Carril("f1", "Fuego 1", "#A8421C", "fuego"), Carril("m", "Mesa", "red;x", "mesa")),
            grupos = listOf(Grupo("Base", listOf(Ingrediente("arroz", a = 100.0, u = "g")))),
            notaEscala = "Nota <i>",
            pasos =
                listOf(
                    Paso(
                        "p1",
                        "Sofreír",
                        "f1",
                        0.0,
                        10.0,
                        f = 5,
                        p = "Con <b>aceite</b> y <u>sal</u>",
                        meto = "arroz",
                        hitos = listOf(Hito(30.0, "Remover")),
                    ),
                    Paso("p2", "Picar", "m", 10.0, 5.0, nota = "ok"),
                ),
            lectura = "Todo a la vez",
            mep = listOf(BloqueMep("Corta", orden = true, l = listOf("<b>Cebolla</b> <script>x</script>"))),
            cortes = listOf(Corte("Brunoise", "Dados <b>finos</b>", svg)),
            porque = listOf(listOf("Por qué", "Porque sí")),
            pie = "Pie & fin",
        )

    private val html = fichaHtml(receta, 6, textos)

    @Test
    fun cantidadesEscaladasASeisRaciones() {
        assertTrue(html.contains("<b>300 g</b>"))
        assertTrue(html.contains("6</b><span>raciones"))
    }

    @Test
    fun escapaElTextoPeroRespetaSvgYNegrita() {
        assertFalse(html.contains("<script>"))
        assertTrue(html.contains("Arroz &lt;script&gt;alert(1)&lt;/script&gt;"))
        assertTrue(html.contains("Rico &amp; sano"))
        assertTrue(html.contains(svg))
        assertTrue(html.contains("Con <b>aceite</b> y &lt;u&gt;sal&lt;/u&gt;"))
        assertTrue(html.contains("<li><b>Cebolla</b> &lt;script&gt;x&lt;/script&gt;</li>"))
    }

    @Test
    fun aparecenTodasLasSeccionesEnOrden() {
        val orden =
            listOf("SEC_ING", "SEC_TIEMPO", "SEC_MEP", "SEC_PASOS", "SEC_TECNICA", "SEC_COMPRA", "Pie &amp; fin")
        val pos = orden.map { html.indexOf(it) }
        assertTrue(pos.all { it >= 0 })
        assertTrue(pos == pos.sorted())
    }

    @Test
    fun ganttUsaColorDeCarrilYSaneaElInvalido() {
        assertTrue(html.contains("background:#A8421C"))
        assertFalse(html.contains("red;x"))
        assertTrue(html.contains("background:#B0A899"))
        assertTrue(html.contains("+0:30 Remover"))
        assertTrue(html.contains("@page{size:A4;margin:13mm}"))
    }
}
