package app.trombito.souschef.ui.ajustes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.ui.crono.BancoCompose
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SelectorDisposicionTest {
    @Test
    fun `tocar una tarjeta elige su disposicion y solo esa queda marcada`() {
        val banco = BancoCompose()
        val elegidas = mutableListOf<DisposicionCrono>()
        banco.poner {
            MaterialTheme {
                var actual by remember { mutableStateOf(DisposicionCrono.APILADA) }
                SelectorDisposicion(actual, {
                    elegidas += it
                    actual = it
                })
            }
        }
        assertEquals(
            listOf("disposicion-APILADA", "disposicion-FOCO", "disposicion-CLASICA"),
            banco.etiquetas("disposicion-"),
        )

        banco.pulsar(banco.conEtiqueta("disposicion-CLASICA"))

        assertEquals(listOf(DisposicionCrono.CLASICA), elegidas)
        val marcadas =
            DisposicionCrono.entries.filter {
                banco.conEtiqueta("disposicion-${it.name}").config.getOrNull(SemanticsProperties.Selected) == true
            }
        assertEquals(listOf(DisposicionCrono.CLASICA), marcadas)
    }
}
