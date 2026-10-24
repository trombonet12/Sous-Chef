package app.trombito.souschef.ui.crono

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.ResumenCrono
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** El cierre se usa con TalkBack: el campo de nota lleva su rótulo y «Deshacer» es un botón visible de 48 dp. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CierreAccesibleTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val banco = BancoCompose()

    @Test
    fun elCampoDeNotaTieneSuRotuloDentroDelPropioCampo() {
        val estado = EstadoCrono("r", 2, 0L, emptyMap(), actualizadoMs = 0L)
        val resumen = ResumenCrono(null, null, 0L, 0L, emptyList(), emptyList(), emptyList())
        banco.poner { MaterialTheme { CronoCierre(recetaCrono(), estado, resumen) {} } }

        val campo = banco.nodos().first { it.config.contains(SemanticsActions.SetText) }

        assertTrue("Para la próxima vez (opcional)" in banco.textos(campo))
    }

    @Test
    fun trasElCierreHayUnBotonDeshacerDe48dpQueDeshace() {
        var deshechos = 0
        banco.poner { MaterialTheme { CierreGuardado { deshechos++ } } }

        val boton = banco.conEtiqueta("deshacer-cierre")
        val recursos = RuntimeEnvironment.getApplication().resources
        val densidad = recursos.displayMetrics.density
        val minimo = 48 * densidad

        assertTrue(boton.size.height >= minimo && boton.size.width >= minimo)
        assertTrue("Deshacer" in banco.textos(boton))
        assertTrue(boton.config.getOrNull(SemanticsProperties.ContentDescription)?.isNotEmpty() == true)
        banco.pulsar(boton)
        assertEquals(1, deshechos)
    }
}
