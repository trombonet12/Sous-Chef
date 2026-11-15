package app.trombito.souschef.ui.ficha

import androidx.compose.ui.graphics.toArgb
import app.trombito.souschef.receta.BloqueMep
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Corte
import app.trombito.souschef.receta.Grupo
import app.trombito.souschef.receta.Hito
import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.theme.FichaOscuro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FichaHtmlTest {
    private val textos =
        TextosFicha(
            ingredientes = "SEC_ING",
            todoJunto = "TODO_JUNTO",
            tiempo = "SEC_TIEMPO",
            mep = "SEC_MEP",
            pasos = "SEC_PASOS",
            tecnica = "SEC_TECNICA",
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
            listOf("SEC_ING", "SEC_TIEMPO", "SEC_MEP", "SEC_PASOS", "SEC_TECNICA", "Pie &amp; fin")
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

    @Test
    fun laImpresionSaleSiempreEnClaro() {
        assertTrue(html.contains("html,body{margin:0;background:#fff;color:#191A14}"))
        val mayusculas = html.uppercase()
        listOf(FichaOscuro.papel, FichaOscuro.papel2, FichaOscuro.tinta).forEach {
            assertFalse(mayusculas.contains("#%06X".format(it.toArgb() and 0xFFFFFF)))
        }
    }

    @Test
    fun laProsaSalePorParrafosYListasConNegritaYEscapado() {
        val prosa =
            "Primero <b>fuerte</b>.\n\n- Uno & dos\n- Tres <u>x</u>\n\nÚltimo."
        val conProsa =
            receta.copy(
                descripcion = prosa,
                lectura = prosa,
                pasos = receta.pasos.map { it.copy(p = prosa) },
                cortes = listOf(Corte("Brunoise", prosa, svg)),
            )
        val h = fichaHtml(conProsa, 2, textos)
        val esperado =
            "<p>Primero <b>fuerte</b>.</p><ul><li>Uno &amp; dos</li>" +
                "<li>Tres &lt;u&gt;x&lt;/u&gt;</li></ul><p>Último.</p>"
        assertTrue(h.contains("<div class=\"prosa desc\">$esperado</div>"))
        assertTrue(h.contains("<div class=\"prosa lectura\">$esperado</div>"))
        assertTrue(h.contains("<div class=\"prosa\">$esperado</div>"))
        assertTrue(h.contains("div.prosa ul{"))
        assertTrue(h.contains("div.prosa p,div.prosa ul{break-inside:avoid}"))
    }

    @Test
    fun unaProsaVaciaNoDejaContenedor() {
        val h = fichaHtml(receta.copy(descripcion = "  ", lectura = "\n"), 2, textos)
        assertFalse(h.contains("<div class=\"prosa desc\">"))
        assertFalse(h.contains("<div class=\"prosa lectura\">"))
    }

    private val imp =
        TextosImpresion(
            hoja = "HOJA_COCINA",
            completa = "FICHA_COMPLETA",
            cronologia = "CRONOLOGIA",
            empieza = "empieza",
            sirve = "sirve",
            mete = "METE",
            avisos = "AVISOS",
            senal = "SEÑAL",
            saca = "SACA",
            porque = "POR_QUE",
            minuto = { "min $it" },
        )

    /** Dos fuegos y la mesa; el array trae un salto atrás (la sartén pequeña va después de un paso posterior). */
    private val huevos =
        Receta(
            slug = "h",
            titulo = "Huevos rotos",
            raciones = 2,
            totalMin = 47.0,
            carriles =
                listOf(
                    Carril("mesa", "MESA", "#6B6C5F", "mesa"),
                    Carril("s1", "SARTÉN GRANDE", "#A8421C", "fuego"),
                    Carril("s2", "SARTÉN PEQUEÑA", "#E9C46A", "fuego"),
                ),
            grupos =
                listOf(
                    Grupo("SARTÉN GRANDE", listOf(Ingrediente("patata", a = 500.0, u = "g"))),
                    Grupo("SARTÉN PEQUEÑA", listOf(Ingrediente("huevo", a = 4.0, u = "ud"))),
                ),
            pasos =
                listOf(
                    Paso(
                        "a",
                        "Freír la patata",
                        "s1",
                        6.0,
                        20.0,
                        f = 4,
                        tapa = "sin tapa",
                        nota = "Tiernas y pálidas",
                        meto = "500 g de patata",
                        saco = "la patata, al plato",
                        p = "PORQUE_PATATA",
                    ),
                    Paso("c", "Subir al 8", "s1", 31.0, 5.0, f = 8, hitos = listOf(Hito(60.0, "Dale la vuelta"))),
                    Paso("b", "Calentar la sartén pequeña", "s2", 28.0, 2.0, f = 5, nota = "Humea ligero"),
                    Paso("z", "Servir", "mesa", 45.0, 2.0),
                ),
            lectura = "LECTURA_TIEMPO",
            mep = listOf(BloqueMep("Corta", l = listOf("Patata"))),
            cortes = listOf(Corte("Brunoise", "Dados", svg)),
            porque = listOf(listOf("Por qué", "PORQUE_RECETA")),
            pie = "PIE_RECETA",
        )

    private fun hoja(servirMin: Int? = 15 * 60 + 30) = fichaHtml(huevos, 2, textos, TipoFicha.HOJA, servirMin, imp)

    private fun completa(servirMin: Int? = 15 * 60 + 30) =
        fichaHtml(huevos, 2, textos, TipoFicha.COMPLETA, servirMin, imp)

    private fun apariciones(
        texto: String,
        trozo: String,
    ) = texto.windowed(trozo.length).count { it == trozo }

    @Test
    fun laCronologiaVaPorOrdenDeOffEnLasDosSalidas() {
        listOf(hoja(), completa()).forEach { h ->
            val titulos = listOf("Freír la patata", "Calentar la sartén pequeña", "Subir al 8", "Servir")
            val pos = titulos.map { h.indexOf("<b>$it</b>") }
            assertTrue(pos.all { it >= 0 })
            assertTrue(pos == pos.sorted())
        }
        // El número de la cronología sigue el orden por off: la sartén pequeña (off 28) es el 2.
        assertTrue(hoja().contains("<i>2</i><div class=\"cab\"><span class=\"cm\">min 28"))
    }

    @Test
    fun cadaPasoDiceMinutoHoraCarrilYTitulo() {
        val h = hoja()
        assertTrue(h.contains("min 6 · 14:49 · SARTÉN GRANDE ·</span> <b>Freír la patata</b>"))
        assertTrue(h.contains("min 28 · 15:11 · SARTÉN PEQUEÑA ·</span> <b>Calentar la sartén pequeña</b>"))
        // Sin hora de servir no hay hora de reloj, pero sí el minuto.
        val sinHora = hoja(servirMin = null)
        assertTrue(sinHora.contains("min 6 · SARTÉN GRANDE ·"))
        assertFalse(sinHora.contains("14:49"))
    }

    @Test
    fun losIngredientesSalenUnaSolaVezPorZona() {
        listOf(hoja(), completa()).forEach { h ->
            assertFalse(h.contains("TODO_JUNTO"))
            assertEquals(1, apariciones(h, "<span>patata</span>"))
            assertEquals(1, apariciones(h, "<span>huevo</span>"))
            assertTrue(h.contains("<h3>SARTÉN GRANDE</h3>"))
            assertTrue(h.contains("<b>500 g</b>"))
        }
    }

    @Test
    fun laHojaTraeLaOrdenEntera() {
        val h = hoja()
        val paso = h.substring(h.indexOf("<b>Freír la patata</b>"), h.indexOf("<b>Calentar la sartén pequeña</b>"))
        val orden = listOf("METE", "fuego 4 · sin tapa · 20′", "SEÑAL", "SACA").map { paso.indexOf(it) }
        assertTrue(orden.all { it >= 0 })
        assertTrue(orden == orden.sorted())
        assertTrue(paso.contains("500 g de patata"))
        assertTrue(paso.contains("Tiernas y pálidas"))
        assertTrue(paso.contains("la patata, al plato"))
        assertTrue(h.contains("AVISOS</span> +1:00 Dale la vuelta"))
        assertTrue(h.contains("sin fuego · 2′"))
    }

    @Test
    fun laHojaNoLlevaPorquesNiPreparacionNiTecnica() {
        val h = hoja()
        listOf("PORQUE_PATATA", "PORQUE_RECETA", "LECTURA_TIEMPO", "PIE_RECETA", "SEC_MEP", "SEC_TECNICA", "POR_QUE")
            .forEach { assertFalse(it, h.contains(it)) }
        assertFalse(h.contains(svg))
        assertTrue(h.contains("HOJA_COCINA"))
        assertTrue(h.contains("14:43</b><span>empieza"))
        assertTrue(h.contains("15:30</b><span>sirve"))
    }

    @Test
    fun laFichaCompletaAnadePorquesPreparacionTecnicaYSenal() {
        val h = completa()
        listOf("PORQUE_PATATA", "PORQUE_RECETA", "LECTURA_TIEMPO", "PIE_RECETA", "SEC_MEP", "SEC_TECNICA", "POR_QUE")
            .forEach { assertTrue(it, h.contains(it)) }
        assertTrue(h.contains(svg))
        assertTrue(h.contains("FICHA_COMPLETA"))
        assertTrue(h.contains("SEÑAL</span> Tiernas y pálidas"))
        assertTrue(h.contains("METE</span> 500 g de patata"))
        assertTrue(h.contains("SACA</span> la patata, al plato"))
    }

    @Test
    fun laNotaSeImprimeComoSenalYNoComoPorque() {
        val h = fichaHtml(receta, 2, textos, TipoFicha.HOJA, null, imp)
        assertTrue(h.contains("SEÑAL</span> ok"))
        assertFalse(h.contains("POR_QUE"))
    }

    @Test
    fun elDiagramaEsPorCarrilesConLosPasosNumeradosYSinPartirse() {
        val h = hoja()
        assertEquals(3, apariciones(h, "<div class=\"et\">"))
        listOf("1", "2", "3", "4").forEach { assertTrue(h.contains(">$it</div>")) }
        assertTrue(h.contains("<section class=\"diagrama\">"))
        assertTrue(h.contains(".diagrama{break-inside:avoid}"))
        assertTrue(h.contains("background:#A8421C;color:#FFFFFF"))
        assertTrue(h.contains("background:#E9C46A;color:#191A14"))
    }

    @Test
    fun losAvisosSeImprimenLegiblesYLosBloquesNoSeParten() {
        val h = completa()
        assertFalse(h.contains("font-size:8.5pt;color:#46473D;font-variant-numeric:tabular-nums}"))
        assertTrue(h.contains(".l{margin:1pt 0 0;font-size:9.5pt"))
        assertTrue(h.contains(".ord,"))
        assertTrue(h.contains("break-inside:avoid"))
    }
}
