package app.trombito.souschef.ui.crono.guia

import androidx.compose.material3.MaterialTheme
import app.trombito.souschef.R
import app.trombito.souschef.ui.ajustes.FilaRepetirGuia
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.crono.LimpiezaCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Ajustes, apartado Cronómetro: la fila «Repetir la guía de la cocina». */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class FilaRepetirGuiaTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private fun texto(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    @Test
    fun `tocar la fila pide desmarcar la guia`() {
        val banco = BancoCompose()
        var pulsaciones = 0
        banco.poner { MaterialTheme { FilaRepetirGuia(vista = true, onRepetir = { pulsaciones++ }) } }

        banco.pulsar(banco.conEtiqueta("ajustes-repetir-guia"))

        assertEquals(1, pulsaciones)
    }

    @Test
    fun `confirma que saldra la proxima vez cuando esta pendiente`() {
        val banco = BancoCompose()
        banco.poner { MaterialTheme { FilaRepetirGuia(vista = false, onRepetir = {}) } }

        val textos = banco.textos(banco.conEtiqueta("ajustes-repetir-guia"))
        assertTrue(texto(R.string.ajustes_guia_repetir) in textos)
        assertTrue(texto(R.string.ajustes_guia_repetida) in textos)
    }

    @Test
    fun `explica la fila cuando la guia ya se vio`() {
        val banco = BancoCompose()
        banco.poner { MaterialTheme { FilaRepetirGuia(vista = true, onRepetir = {}) } }

        val textos = banco.textos(banco.conEtiqueta("ajustes-repetir-guia"))
        assertTrue(texto(R.string.ajustes_guia_repetir_detalle) in textos)
    }
}
