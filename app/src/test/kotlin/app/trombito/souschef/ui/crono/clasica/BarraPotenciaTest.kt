package app.trombito.souschef.ui.crono.clasica

import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.ui.comun.nivel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarraPotenciaTest {
    private val gasSinNumeros = Cocina(TipoCocina.GAS, Mando.ANALOGICO, null)
    private val digital12 = Cocina(TipoCocina.INDUCCION, Mando.DIGITAL, 12)
    private val analogico6 = Cocina(TipoCocina.GAS, Mando.ANALOGICO, 6)

    @Test
    fun enVitroDigitalEsLaDeAntesNueveSegmentosYElNumeroDeLaReceta() {
        (0..9).forEach { f ->
            val b = barraPotencia(f, Cocina.VITRO_9)
            assertEquals(9, b.segmentos)
            assertEquals(f, b.encendidos)
            assertEquals("$f", b.texto)
            assertFalse(b.esPalabra)
        }
    }

    @Test
    fun conDigital12HayDoceSegmentosYLosEncendidosSonProporcionales() {
        val b = barraPotencia(9, digital12)
        assertEquals(12, b.segmentos)
        assertEquals(12, b.encendidos)
        assertEquals("12", b.texto)
        val medio = barraPotencia(5, digital12)
        assertEquals(12, medio.segmentos)
        assertEquals(7, medio.encendidos)
        assertEquals("7", medio.texto)
        assertEquals(0, barraPotencia(0, digital12).encendidos)
        assertEquals("0", barraPotencia(0, digital12).texto)
    }

    @Test
    fun enAnalogicoConNumerosLosMediosRedondeanHaciaArribaEnLaBarra() {
        // 7 * 6 / 9 = 4,67 -> 4,5: se leen "4,5" y se encienden 5 de 6 segmentos.
        val b = barraPotencia(7, analogico6)
        assertEquals(6, b.segmentos)
        assertEquals(5, b.encendidos)
        assertEquals("4,5", b.texto)
    }

    @Test
    fun sinNumerosHaySieteSegmentosUnoPorPalabraYLaPalabraEnLugarDelNumero() {
        val esperado =
            listOf(1 to "mínimo", 2 to "bajo", 4 to "medio-bajo", 5 to "medio", 7 to "medio-alto", 8 to "alto")
        esperado.forEachIndexed { i, (f, palabra) ->
            val b = barraPotencia(f, gasSinNumeros)
            assertEquals(7, b.segmentos)
            assertEquals(palabra, b.texto)
            assertTrue(b.esPalabra)
            assertTrue(b.encendidos in 1..7)
            if (i > 0) assertTrue(b.encendidos > barraPotencia(esperado[i - 1].first, gasSinNumeros).encendidos)
        }
        assertEquals(7, barraPotencia(9, gasSinNumeros).encendidos)
        assertEquals("máximo", barraPotencia(9, gasSinNumeros).texto)
        // Dos niveles de la misma palabra encienden lo mismo.
        assertEquals(barraPotencia(2, gasSinNumeros).encendidos, barraPotencia(3, gasSinNumeros).encendidos)
        assertEquals(0, barraPotencia(0, gasSinNumeros).encendidos)
        assertEquals("apagado", barraPotencia(0, gasSinNumeros).texto)
    }

    @Test
    fun elChipYElDespuesAquiUsanLaMismaGramaticaQueElTexto() {
        assertEquals("7", Cocina.VITRO_9.nivel(7))
        assertEquals("9", digital12.nivel(7))
        assertEquals("medio-alto", gasSinNumeros.nivel(7))
        assertEquals("al 7", Cocina.VITRO_9.despuesNivel(7))
        assertEquals("a fuego medio-alto", gasSinNumeros.despuesNivel(7))
        assertEquals("al 12", digital12.despuesNivel(9))
    }
}
