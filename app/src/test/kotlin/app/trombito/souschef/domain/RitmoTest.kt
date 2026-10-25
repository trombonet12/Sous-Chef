package app.trombito.souschef.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RitmoTest {
    private fun e(
        id: Int,
        minutos: Int?,
        slug: String = "sopa",
    ) = EntradaHistorial(id.toString(), slug, slug, "2026-01-%02d".format(id), minutosReales = minutos)

    private fun conPrevisto(
        id: Int,
        minutos: Int,
        previsto: Int?,
    ) = e(id, minutos).copy(previstoMin = previsto)

    @Test
    fun siLaEntradaTienePrevistoElDesvioSeCuentaContraEl() {
        // Hoy la receta dura 30, pero esas cocinadas se previeron en 40 y 50: desvíos +4 y +5 -> 4,5 -> 5.
        val entradas = listOf(conPrevisto(1, 44, 40), conPrevisto(2, 55, 50))
        assertEquals(5, desvioHabitualMin(entradas, "sopa", 30))
    }

    @Test
    fun sinPrevistoSeCuentaContraElTotalActualYSePuedenMezclar() {
        // Desvíos: +10 (contra el total actual, 30), +2 (contra su previsto, 40) y +8 (contra 30) -> mediana +8.
        val entradas = listOf(e(1, 40), conPrevisto(2, 42, 40), e(3, 38))
        assertEquals(8, desvioHabitualMin(entradas, "sopa", 30))
    }

    @Test
    fun conPrevistoQueCoincideNoMerecePenaAvisar() {
        val entradas = listOf(conPrevisto(1, 41, 40), conPrevisto(2, 42, 40))
        assertNull(desvioHabitualMin(entradas, "sopa", 30))
    }

    @Test
    fun unPrevistoNoPositivoSeIgnoraYSeUsaElTotalActual() {
        val entradas = listOf(conPrevisto(1, 40, 0), conPrevisto(2, 40, null))
        assertEquals(10, desvioHabitualMin(entradas, "sopa", 30))
    }

    @Test
    fun conMenosDeDosCocinadasNoHayRitmo() {
        assertNull(desvioHabitualMin(emptyList(), "sopa", 30))
        assertNull(desvioHabitualMin(listOf(e(1, 45)), "sopa", 30))
    }

    @Test
    fun conDosHacePromedioYRedondea() {
        // Desvíos +7 y +10 -> 8,5 -> 9.
        assertEquals(9, desvioHabitualMin(listOf(e(1, 37), e(2, 40)), "sopa", 30))
    }

    @Test
    fun conTresTomaLaMedianaDeLasTresMasRecientes() {
        // Desvíos de las 3 recientes (id 5, 4, 3): +20, +8, +5 -> mediana +8. La antigua (id 1, -50) no cuenta.
        val entradas = listOf(e(1, 1), e(3, 35), e(4, 38), e(5, 50))
        assertEquals(8, desvioHabitualMin(entradas, "sopa", 30))
    }

    @Test
    fun ignoraLasQueNoTienenMinutosValidos() {
        val entradas = listOf(e(1, 40), e(2, 38), e(3, null), e(4, 0), e(5, 721), e(6, 100, slug = "arroz"))
        assertEquals(9, desvioHabitualMin(entradas, "sopa", 30))
        assertNull(desvioHabitualMin(listOf(e(1, 40), e(2, null), e(3, 0)), "sopa", 30))
    }

    @Test
    fun conMenosDeTresMinutosNoMerecePenaAvisar() {
        assertNull(desvioHabitualMin(listOf(e(1, 32), e(2, 32), e(3, 31)), "sopa", 30))
        assertNull(desvioHabitualMin(listOf(e(1, 28), e(2, 28), e(3, 29)), "sopa", 30))
        assertEquals(3, desvioHabitualMin(listOf(e(1, 33), e(2, 33)), "sopa", 30))
    }

    @Test
    fun siTardaMenosElDesvioEsNegativo() {
        assertEquals(-5, desvioHabitualMin(listOf(e(1, 25), e(2, 20), e(3, 25)), "sopa", 30))
    }
}
