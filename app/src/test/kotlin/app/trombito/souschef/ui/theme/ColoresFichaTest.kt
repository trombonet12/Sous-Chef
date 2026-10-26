package app.trombito.souschef.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.trombito.souschef.ui.ficha.colorBarraSobre
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColoresFichaTest {
    @Test
    fun fichaClara_conservaLosValoresDelPapel() {
        assertEquals(Color(0xFFF3EFE4), FichaClaro.papel)
        assertEquals(Color(0xFFEAE3D2), FichaClaro.papel2)
        assertEquals(Color(0xFF17150F), FichaClaro.tinta)
        assertEquals(Color(0xFF4E483C), FichaClaro.tinta2)
        assertEquals(Color(0xFF857C6A), FichaClaro.tinta3)
        assertEquals(Color(0xFF6E6656), FichaClaro.meta)
        assertEquals(Color(0xFFD5CDB9), FichaClaro.linea)
    }

    @Test
    fun fichaOscura_textoCumpleAASobreAmbosPapeles() {
        listOf(FichaOscuro.papel, FichaOscuro.papel2).forEach { papel ->
            listOf(FichaOscuro.tinta, FichaOscuro.tinta2, FichaOscuro.meta).forEach { texto ->
                assertTrue("$texto sobre $papel: ${contraste(texto, papel)}", contraste(texto, papel) >= 4.5f)
            }
        }
    }

    @Test
    fun fichaOscura_bordesDeControlLleganA3Sobre_elPapel() {
        assertTrue(contraste(FichaOscuro.tinta3, FichaOscuro.papel) >= 3f)
        assertTrue(contraste(FichaOscuro.tinta3, FichaOscuro.papel2) >= 3f)
    }

    @Test
    fun fichaOscura_esOscuraYLaClaraEsClara() {
        assertTrue(FichaOscuro.papel.luminance() < 0.1f)
        assertTrue(FichaOscuro.papel2.luminance() < 0.1f)
        assertTrue(FichaClaro.papel.luminance() > 0.5f)
    }

    @Test
    fun fichaOscura_sigueLaFamiliaDelCronometro() {
        assertEquals(CronoOscuro.carta, FichaOscuro.papel)
        assertEquals(CronoOscuro.carta2, FichaOscuro.papel2)
        assertEquals(CronoOscuro.linea, FichaOscuro.linea)
    }

    @Test
    fun barras_enOscuroTodosLosColoresDeCarrilLleganA3Sobre_elFondoDeBarra() {
        val fondo = FichaOscuro.papel2
        val carriles =
            listOf(0xFFC0B7A6, 0xFFE8836B, 0xFFF0AE4A, 0xFF6B6356, 0xFF77558A, 0xFF5B3A6E, 0xFF8E5A8C, 0xFFB5372A)
        carriles.forEach {
            val color = colorBarraSobre(Color(it), fondo)
            assertTrue("$it -> $color: ${contraste(color, fondo)}", contraste(color, fondo) >= 3f)
        }
    }

    @Test
    fun barras_enClaroElColorNoCambia() {
        val violeta = Color(0xFF77558A)
        assertEquals(violeta, colorBarraSobre(violeta, FichaClaro.papel2))
    }

    @Test
    fun barras_enOscuroUnColorQueYaContrastaNoSeToca() {
        val ambar = Color(0xFFF0AE4A)
        assertEquals(ambar, colorBarraSobre(ambar, FichaOscuro.papel2))
    }
}
