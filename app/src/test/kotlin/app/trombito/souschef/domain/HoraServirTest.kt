package app.trombito.souschef.domain

import app.trombito.souschef.receta.Momento
import org.junit.Assert.assertEquals
import org.junit.Test

class HoraServirTest {
    private val ajustes = Ajustes(servirMin = 15 * 60 + 30, cenarMin = 21 * 60)
    private val comer = HoraUsada(15 * 60 + 30, CualHora.COMER)
    private val cenar = HoraUsada(21 * 60, CualHora.CENAR)
    private val manana = 10 * 60
    private val tarde = 18 * 60

    @Test
    fun losDefectosSonLas1530YLas2100() {
        assertEquals(15 * 60 + 30, SERVIR_DEFECTO_MIN)
        assertEquals(21 * 60, CENAR_DEFECTO_MIN)
        assertEquals(CENAR_DEFECTO_MIN, Ajustes().cenarMin)
    }

    @Test
    fun sinMomentoEsLaHoraDeComerAUnaHoraCualquiera() {
        assertEquals(comer, horaServir(null, manana, ajustes))
        assertEquals(comer, horaServir(null, tarde, ajustes))
    }

    @Test
    fun soloCenaEsLaHoraDeCenarAUnaHoraCualquiera() {
        assertEquals(cenar, horaServir(listOf(Momento.CENA), manana, ajustes))
        assertEquals(cenar, horaServir(listOf(Momento.CENA), tarde, ajustes))
    }

    @Test
    fun comidaYCenaDependenDeLaHoraDeAhora() {
        val ambas = listOf(Momento.COMIDA, Momento.CENA)
        assertEquals(comer, horaServir(ambas, manana, ajustes))
        assertEquals(comer, horaServir(ambas, 16 * 60 + 59, ajustes))
        assertEquals(cenar, horaServir(ambas, 17 * 60, ajustes))
        assertEquals(cenar, horaServir(ambas, 23 * 60 + 30, ajustes))
    }

    @Test
    fun comidaPostreYMeriendaSonLaHoraDeComerAunqueSeaDeNoche() {
        assertEquals(comer, horaServir(listOf(Momento.COMIDA), tarde, ajustes))
        assertEquals(comer, horaServir(listOf(Momento.POSTRE), tarde, ajustes))
        assertEquals(comer, horaServir(listOf(Momento.MERIENDA, Momento.POSTRE), tarde, ajustes))
    }

    @Test
    fun elDesayunoUsaLaHoraDeComerPorqueLaAppNoTieneModoSinHora() {
        assertEquals(comer, horaServir(listOf(Momento.DESAYUNO), manana, ajustes))
        assertEquals(comer, horaServir(listOf(Momento.DESAYUNO, Momento.MERIENDA), tarde, ajustes))
    }

    @Test
    fun usaLasHorasQueHayEnAjustes() {
        val otros = Ajustes(servirMin = 14 * 60, cenarMin = 22 * 60 + 15)
        assertEquals(HoraUsada(14 * 60, CualHora.COMER), horaServir(null, manana, otros))
        assertEquals(HoraUsada(22 * 60 + 15, CualHora.CENAR), horaServir(listOf(Momento.CENA), manana, otros))
    }

    @Test
    fun conLasHorasSueltasDaLoMismoQueConAjustes() {
        assertEquals(cenar, horaServir(listOf(Momento.CENA), manana, 15 * 60 + 30, 21 * 60))
    }
}
