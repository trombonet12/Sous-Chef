package app.souschef.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCarrilTextoTest {
    private val ambar = Color(0xFFF0AE4A)
    private val carta = Color(0xFFFBFAF6)

    @Test
    fun enClaro_elAmbarLlegaA45Sobre_laCarta() {
        val texto = colorCarrilTextoSobre(ambar, carta, claro = true)
        assertTrue("contraste ${contraste(texto, carta)}", contraste(texto, carta) >= 4.5f)
    }

    @Test
    fun enClaro_unColorOscuroNoSeTocaSiYaContrasta() {
        val oscuro = Color(0xFF2C6E92)
        assertEquals(oscuro, colorCarrilTextoSobre(oscuro, carta, claro = true))
    }

    @Test
    fun enOscuro_elColorNoCambia() {
        assertEquals(ambar, colorCarrilTextoSobre(ambar, CronoOscuro.carta, claro = false))
    }

    @Test
    fun paletaClara_cumpleAA_sobreLaCarta() {
        val c = CronoClaro
        listOf(c.texto, c.apagado, c.tenue, c.meto, c.saco, c.tapa, c.alerta, c.ok, c.acento, c.urgente)
            .forEach { assertTrue("$it", contraste(it, c.carta) >= 4.5f) }
        assertTrue(contraste(c.onPrincipal, c.principal) >= 4.5f)
        assertTrue(contraste(c.onUrgente, c.urgente) >= 4.5f)
    }
}
