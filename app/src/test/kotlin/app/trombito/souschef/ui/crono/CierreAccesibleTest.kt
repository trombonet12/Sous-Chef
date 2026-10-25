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
        banco.poner { MaterialTheme { CierreGuardado(7, { deshechos++ }, {}) } }

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

    @Test
    fun laCuentaAtrasYGuardarYaConfirmanSinEsperar() {
        var guardados = 0
        banco.poner { MaterialTheme { CierreGuardado(7, {}, { guardados++ }) } }

        val boton = banco.conEtiqueta("guardar-cierre")

        assertTrue("Se guarda en 7 s" in banco.textos(banco.conEtiqueta("cierre-cuenta-atras")))
        assertTrue("Guardar y salir ya" in banco.textos(boton))
        val recursos = RuntimeEnvironment.getApplication().resources
        assertTrue(boton.size.height >= 48 * recursos.displayMetrics.density)
        banco.pulsar(boton)
        assertEquals(1, guardados)
    }

    @Test
    fun sinTicSeDiceQueSeGuardaEnUnosSegundos() {
        banco.poner { MaterialTheme { CierreGuardado(null, {}, {}) } }

        assertTrue("Se guarda en unos segundos" in banco.textos(banco.conEtiqueta("cierre-cuenta-atras")))
    }

    @Test
    fun laCuentaAtrasRedondeaConCeilYNuncaBajaDeUno() {
        val ui = CronoUi(cierrePendiente = true, cierreHastaMs = 13_000L, ahoraMs = 1_000L)

        assertEquals(12, segundosParaGuardar(ui))
        assertEquals(1, segundosParaGuardar(ui.copy(ahoraMs = 11_100L)))
        assertEquals(1, segundosParaGuardar(ui.copy(ahoraMs = 20_000L)))
        assertEquals(null, segundosParaGuardar(ui.copy(ahoraMs = 0L)))
        assertEquals(null, segundosParaGuardar(ui.copy(cierreHastaMs = null)))
    }

    @Test
    fun elCierreCompara15MinPrevistosConLosRealesYMuestraLaNotaDeLaVezAnterior() {
        val estado = EstadoCrono("r", 2, 0L, emptyMap(), actualizadoMs = 0L)
        val resumen = ResumenCrono(1_080L, 18, 0L, 0L, emptyList(), emptyList(), emptyList())
        banco.poner { MaterialTheme { CronoCierre(recetaCrono(), estado, resumen, "Menos sal") {} } }

        val textos = banco.textos(banco.raiz())

        assertTrue("Real 18 min · previsto 15 (+3)" in textos)
        assertTrue("La última vez: Menos sal" in banco.textos(banco.conEtiqueta("nota-anterior")))
    }

    @Test
    fun siSaleAntesElDesvioVaConMenosYYaEstaALaMesaSigueAlcanzable() {
        val estado = EstadoCrono("r", 2, 0L, emptyMap(), actualizadoMs = 0L)
        val resumen = ResumenCrono(720L, 12, 0L, 0L, emptyList(), emptyList(), emptyList())
        var cerrados = 0
        banco.poner { MaterialTheme { CronoCierre(recetaCrono(), estado, resumen) { cerrados++ } } }

        assertTrue("Real 12 min · previsto 15 (−3)" in banco.textos(banco.raiz()))
        assertTrue(banco.nodos().none { it.config.getOrNull(SemanticsProperties.TestTag) == "nota-anterior" })
        banco.pulsar(banco.conEtiqueta("ya-esta"))
        assertEquals(1, cerrados)
    }
}
