package app.trombito.souschef.ui.crono.voz

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import app.trombito.souschef.ui.ajustes.FilaOrdenesVoz
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.crono.LimpiezaCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Las órdenes de voz a mano: la lista compartida con la intro y la fila de Ajustes que abre la hoja. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class OrdenesVozUiTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val banco = BancoCompose()

    @Test
    fun `la lista reutiliza los textos de la intro`() {
        banco.poner { MaterialTheme { GruposOrdenesVoz(Color.Red) } }

        val textos = banco.textos(banco.raiz()).joinToString("\n")

        assertTrue("Avanzar" in textos)
        assertTrue("«Chef, hecho»" in textos)
        assertTrue("«Chef, cuánto queda»" in textos)
        assertTrue("Alarma y repetición" in textos)
    }

    @Test
    fun `la fila de Ajustes se pulsa para abrir la hoja`() {
        var aperturas = 0
        banco.poner { MaterialTheme { FilaOrdenesVoz { aperturas++ } } }

        val fila = banco.conEtiqueta("ajustes-ordenes-voz")
        assertTrue("Qué puedes decirle al Chef" in banco.textos(fila))
        banco.pulsar(fila)

        assertEquals(1, aperturas)
    }
}
