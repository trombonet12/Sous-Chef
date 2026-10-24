package app.trombito.souschef.domain

import app.trombito.souschef.ui.crono.FakeAjustesRepo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComprobarNovedadesTest {
    private val ajustes = FakeAjustesRepo()
    private var viva = false
    private val todas = listOf(novedad("1.6"), novedad("1.5"))

    private fun caso(version: String = "1.6.0") =
        ComprobarNovedades(ajustes, FuenteNovedadesFija(todas), VersionFija(version)) { viva }

    @Test
    fun `de 1_5 a 1_6 ensena la 1_6 y marcarla evita repetirla`() =
        runBlocking {
            ajustes.actual.value = Ajustes(introVista = true, novedadesVistas = "1.5")
            val uc = caso()

            assertEquals(listOf("1.6"), uc(entradaDirecta = false)?.map { it.version })
            uc.marcarVistas()

            assertEquals("1.6", ajustes.actual.value.novedadesVistas)
            assertNull(uc(entradaDirecta = false))
        }

    @Test
    fun `sin intro vista, con cocinada viva o con entrada directa no ensena nada y no marca`() =
        runBlocking {
            ajustes.actual.value = Ajustes(introVista = false)
            assertNull(caso()(entradaDirecta = false))

            ajustes.actual.value = Ajustes(introVista = true, novedadesVistas = "1.5")
            viva = true
            assertNull(caso()(entradaDirecta = false))
            viva = false
            assertNull(caso()(entradaDirecta = true))
            assertNull(caso()(entradaDirecta = false, playPendiente = true))
            assertEquals("1.5", ajustes.actual.value.novedadesVistas)
        }
}
