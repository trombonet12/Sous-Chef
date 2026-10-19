package app.souschef.ui.ajustes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.souschef.domain.DisposicionLista
import app.souschef.ui.crono.BancoCompose
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SelectorListaTest {
    @Test
    fun `tocar una tarjeta elige su diseno y solo ese queda marcado`() {
        val banco = BancoCompose()
        val elegidas = mutableListOf<DisposicionLista>()
        banco.poner {
            MaterialTheme {
                var actual by remember { mutableStateOf(DisposicionLista.TINTE) }
                SelectorLista(actual, {
                    elegidas += it
                    actual = it
                })
            }
        }
        assertEquals(listOf("lista-TINTE", "lista-FRANJA", "lista-CLASICA"), banco.etiquetas("lista-"))

        banco.pulsar(banco.conEtiqueta("lista-FRANJA"))

        assertEquals(listOf(DisposicionLista.FRANJA), elegidas)
        val marcadas =
            DisposicionLista.entries.filter {
                banco.conEtiqueta("lista-${it.name}").config.getOrNull(SemanticsProperties.Selected) == true
            }
        assertEquals(listOf(DisposicionLista.FRANJA), marcadas)
    }
}
