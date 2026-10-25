package app.trombito.souschef.ui.ficha

import androidx.compose.material3.MaterialTheme
import app.trombito.souschef.domain.crono.ChipAviso
import app.trombito.souschef.ui.crono.LimpiezaCompose
import app.trombito.souschef.ui.receta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** El chip «Avísame a las HH:mm» de la ficha (ADR-0016), bajo «Empieza a las». */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-xxhdpi")
class FichaAvisoTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private fun pintar(
        aviso: ChipAviso?,
        acciones: AccionesFicha = AccionesFicha({}, {}, { _, _ -> }, {}, {}, {}),
    ): BancoFicha {
        val banco = BancoFicha()
        banco.poner {
            MaterialTheme {
                FichaContenido(FichaUiState(cargando = false, receta = receta("sopa"), aviso = aviso), acciones)
            }
        }
        return banco
    }

    @Test
    fun sinAvisoNoHayChip() {
        val banco = pintar(null)
        assertFalse(banco.etiquetas("aviso-").toString(), "aviso-empezar" in banco.etiquetas("aviso-"))
    }

    @Test
    fun elChipOfreceAvisarConLaHoraYAlPulsarloPideArmar() {
        var armado = 0
        val banco =
            pintar(
                ChipAviso(15 * 60, puesto = false),
                AccionesFicha({}, {}, { _, _ -> }, {}, {}, {}, onAvisar = { armado++ }),
            )
        assertTrue(banco.textos().toString(), "Avísame a las 15:00" in banco.textos())
        banco.pulsar(banco.conEtiqueta("aviso-empezar"))
        assertEquals(1, armado)
    }

    @Test
    fun conElAvisoPuestoElChipLoDiceYAlPulsarloLoQuita() {
        var quitado = 0
        val banco =
            pintar(
                ChipAviso(14 * 60 + 51, puesto = true),
                AccionesFicha({}, {}, { _, _ -> }, {}, {}, {}, onQuitarAviso = { quitado++ }),
            )
        assertTrue(banco.textos().toString(), "Aviso a las 14:51 · Quitar" in banco.textos())
        assertTrue(banco.descripciones().toString(), "Quitar el aviso de las 14:51" in banco.descripciones())
        banco.pulsar(banco.conEtiqueta("aviso-empezar"))
        assertEquals(1, quitado)
    }
}
