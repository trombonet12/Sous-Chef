package app.trombito.souschef.ui.crono

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** El botón del Chef: visible con la voz activa aunque no haya reconocedor, y silencia o reactiva al Chef. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BotonMicrofonoTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val banco = BancoCompose()

    private fun descripciones(): List<String> =
        banco
            .nodos()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.ContentDescription) }
            .flatten()

    private fun poner(
        ui: CronoUi,
        silenciar: () -> Unit = {},
    ) = banco.poner { BotonMicrofono(ui, silenciar, pedirPermiso = {}) }

    @Test
    fun conLaVozActivaSeVeAunqueNoHayaReconocedor() {
        poner(CronoUi(vozActiva = true, voz = EstadoVoz.APAGADA))

        assertEquals(listOf("Silenciar al Chef"), descripciones())
    }

    @Test
    fun conLaVozSilenciadaOfreceActivarAlChef() {
        poner(CronoUi(vozActiva = true, vozSilenciada = true, voz = EstadoVoz.APAGADA))

        assertEquals(listOf("Activar al Chef"), descripciones())
    }

    @Test
    fun sinElAjusteDeVozNoSePinta() {
        poner(CronoUi(vozActiva = false))

        assertTrue(descripciones().isEmpty())
    }

    @Test
    fun elToquePideSilenciar() {
        var toques = 0
        poner(CronoUi(vozActiva = true)) { toques++ }

        val boton = banco.nodos().first { it.config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick) }
        banco.pulsar(boton)

        assertEquals(1, toques)
    }
}
