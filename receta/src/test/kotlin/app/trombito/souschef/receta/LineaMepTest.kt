package app.trombito.souschef.receta

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LineaMepTest {
    @Test
    fun partePolloConMinuto() {
        val l =
            LineaMep.parse(
                "<b>Pollo · 400 g de pechuga (2 pechugas)</b> · recién salido del congelador, " +
                    "en dados de 2 cm con " +
                    "las tiras cortadas contra la fibra. A la bolsa en el minuto 25. " +
                    "Después, tabla, cuchillo y manos a lavar.",
            )
        assertEquals("Pollo · 400 g de pechuga (2 pechugas)", l.cabecera)
        assertEquals(
            "recién salido del congelador, en dados de 2 cm con las tiras cortadas contra la fibra. " +
                "A la bolsa en el minuto 25. Después, tabla, cuchillo y manos a lavar.",
            l.detalle,
        )
        assertEquals(25, l.minuto)
    }

    @Test
    fun tomaElMinutoDeEntradaNoElPrimeroNombrado() {
        val l =
            LineaMep.parse(
                "<b>Zanahoria · 150 g (1 grande)</b> · pelada, en bastones de 4 mm y 5 cm de largo. " +
                    "Se corta en el 90 y entra en el minuto 124.",
            )
        assertEquals("Zanahoria · 150 g (1 grande)", l.cabecera)
        assertEquals(124, l.minuto)
    }

    @Test
    fun minutoCeroEsValido() {
        val l =
            LineaMep.parse(
                "<b>Cebolla · 250 g (1 cebolla grande)</b> · recortada, pelada y en rodajas de 1 cm " +
                    "cruzando la fibra, sin partirla por la mitad; de cada rodaja, los 3 o 4 aros de fuera. " +
                    "Los centros, a un táper para " +
                    "un sofrito. Se corta en el minuto 0.",
            )
        assertEquals("Cebolla · 250 g (1 cebolla grande)", l.cabecera)
        assertEquals(0, l.minuto)
    }

    @Test
    fun cabeceraSimpleYDetalleSinSeparador() {
        val l =
            LineaMep.parse(
                "<b>Bandeja o fuente grande</b> · vacía, para dejar los aros rebozados sin que se toquen. " +
                    "A la nevera en el minuto 18.",
            )
        assertEquals("Bandeja o fuente grande", l.cabecera)
        assertEquals(
            "vacía, para dejar los aros rebozados sin que se toquen. A la nevera en el minuto 18.",
            l.detalle,
        )
        assertEquals(18, l.minuto)
    }

    @Test
    fun lineaConDosNumerosYMinutoAlFinal() {
        val l =
            LineaMep.parse(
                "<b>La salsa</b> · 150 ml de caldo de pollo (⅔ de vaso) + 20 ml de salsa de soja, " +
                    "removido hasta que no quede polvo. Se mezcla en el 101 y entra en el minuto 130.",
            )
        assertEquals("La salsa", l.cabecera)
        assertEquals(130, l.minuto)
    }

    @Test
    fun sinNegritaInicialTodoEsDetalle() {
        val l = LineaMep.parse("Tabla seca y cuchillo <b>afilado</b> sobre la mesa")
        assertNull(l.cabecera)
        assertEquals("Tabla seca y cuchillo afilado sobre la mesa", l.detalle)
        assertNull(l.minuto)
    }

    @Test
    fun minutoEnMayusculaOEnPluralNoCuenta() {
        val l = LineaMep.parse("<b>Harina</b> · 50 g, tamizada. Minuto 5 no vale; 4 minutos tampoco.")
        assertEquals("Harina", l.cabecera)
        assertNull(l.minuto)
    }

    @Test
    fun jamonMarcaCuandoEntraEnLaSartenNoCuandoSeRasga() {
        val l =
            LineaMep.parse(
                "<b>Jamón serrano · 80 g (unas 5 lonchas)</b> · rasgado a mano en trozos de unos 3 cm. " +
                    "Se rasga en el minuto 12, con la patata friéndose. Entra en la sartén pequeña en el minuto 30.",
            )
        assertEquals(30, l.minuto)
    }

    @Test
    fun conVariasEntradasTomaLaUltimaYUnaAccionPosteriorNoLaPisa() {
        val l =
            LineaMep.parse(
                "<b>Taza</b> · 4 g de azúcar. Entran en el minuto 21 con el tomate; recoge aceite en el minuto 38.",
            )
        assertEquals(21, l.minuto)
        val dos = LineaMep.parse("<b>Agua</b> · 150 ml. Entra en el minuto 10; entran más en el minuto 22.")
        assertEquals(22, dos.minuto)
    }

    @Test
    fun sinPalabraEntraTomaElUltimoMinuto() {
        val l = LineaMep.parse("<b>Bol</b> · 120 g de patata (minuto 3) + 350 g de calabacín (minuto 6); a la olla.")
        assertEquals(6, l.minuto)
    }

    @Test
    fun palabrasQueContienenEntraNoCuentan() {
        val l = LineaMep.parse("<b>Pan</b> · concentra el sabor en el minuto 5; entrante aparte en el minuto 9.")
        assertEquals(9, l.minuto)
    }
}
