package app.trombito.souschef.ui.recetas

import androidx.compose.material3.MaterialTheme
import app.trombito.souschef.domain.OrdenRecetas
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.crono.LimpiezaCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class RecetasTiempoTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val recetas =
        listOf(
            Receta(slug = "larga", titulo = "Larga", totalMin = 200.0),
            Receta(slug = "corta", titulo = "Corta", totalMin = 20.0),
            Receta(slug = "media", titulo = "Media", totalMin = 60.0),
        )

    private fun estado(
        tiempo: ModoTiempo?,
        ahoraMin: Int = 14 * 60,
    ) = construirEstadoRecetas(
        recetas,
        emptyMap(),
        emptyMap(),
        FiltroRecetas(orden = OrdenRecetas.ALFABETICO, tiempo = tiempo),
        ContextoLista(LocalDate.of(2026, 10, 20), ahoraMin = ahoraMin),
    )

    private fun pintar(
        estado: RecetasUiState,
        tiempo: AccionesTiempo = AccionesTiempo(),
    ): BancoCompose {
        val banco = BancoCompose()
        banco.poner {
            MaterialTheme {
                RecetasContenido(estado, {}, AccionesFiltro({}, {}, {}), AccionesLista({ _, _ -> }, {}), tiempo)
            }
        }
        return banco
    }

    @Test
    fun sinModoLaListaNoCambiaYNoHayEtiquetas() {
        val e = estado(null)
        assertEquals(listOf("Corta", "Larga", "Media"), e.tarjetas.map { it.receta.titulo })
        assertTrue(e.tarjetas.all { it.llegada == null })
        assertFalse(e.tiempoVencido)
    }

    @Test
    fun conModoLleganPrimeroPorLaHoraDeEmpezarMasTardiaYLuegoLasDemas() {
        val e = estado(ModoTiempo(15 * 60 + 30))
        assertEquals(listOf("Corta", "Media", "Larga"), e.tarjetas.map { it.receta.titulo })
        assertEquals(listOf(true, true, false), e.tarjetas.map { it.llegada?.llega })
    }

    @Test
    fun soloLlegoQuitaLaQueNoLlega() {
        val e = estado(ModoTiempo(15 * 60 + 30, soloLlego = true))
        assertEquals(listOf("Corta", "Media"), e.tarjetas.map { it.receta.titulo })
    }

    @Test
    fun conLaHoraDeServirVencidaNoLlegaNingunaYSeAvisa() {
        val e = estado(ModoTiempo(15 * 60 + 30), ahoraMin = 20 * 60)
        assertTrue(e.tiempoVencido)
        assertTrue(e.tarjetas.none { it.llegada?.llega == true })
    }

    @Test
    fun elChipApagadoSeOfreceYEncendidoMuestraLaHoraYLaX() {
        val apagado = pintar(estado(null))
        assertTrue(apagado.textos(apagado.raiz()).contains("¿Qué me da tiempo?"))
        assertTrue(apagado.etiquetas("chip-tiempo-quitar").isEmpty())

        val encendido = pintar(estado(ModoTiempo(15 * 60 + 30)))
        assertTrue(encendido.textos(encendido.raiz()).contains("Servir a las 15:30"))
        assertEquals(listOf("chip-tiempo-quitar"), encendido.etiquetas("chip-tiempo-quitar"))
    }

    @Test
    fun lasTarjetasLlevanLaEtiquetaDeLlegasYNoLlegas() {
        val banco = pintar(estado(ModoTiempo(15 * 60 + 30)))
        val textos = banco.textos(banco.raiz())
        assertTrue(textos.toString(), textos.contains("Llegas · empieza 15:10"))
        assertTrue(textos.toString(), textos.contains("Llegas · empieza 14:30"))
        assertTrue(textos.toString(), textos.contains("No llegas · acabarías 17:20"))
        assertEquals(3, banco.etiquetas("etiqueta-llegada").size)
    }

    @Test
    fun pulsarElChipApagadoEnciendeElModoYLaXLoApaga() {
        val llamadas = mutableListOf<String>()
        val acciones = AccionesTiempo(activar = { llamadas += "activar" }, salir = { llamadas += "salir" })
        val apagado = pintar(estado(null), acciones)
        apagado.pulsar(apagado.conEtiqueta("chip-tiempo"))
        assertEquals(listOf("activar"), llamadas)

        val encendido = pintar(estado(ModoTiempo(15 * 60 + 30)), acciones)
        encendido.pulsar(encendido.conEtiqueta("chip-tiempo-quitar"))
        assertEquals(listOf("activar", "salir"), llamadas)
    }

    @Test
    fun sinNingunaQueLlegueSoloLlegoDiceQueNoHayNinguna() {
        val e = estado(ModoTiempo(14 * 60 + 10, soloLlego = true))
        assertTrue(e.tarjetas.isEmpty())
        val banco = pintar(e)
        assertTrue(banco.textos(banco.raiz()).contains("Ninguna llega a tiempo"))
    }
}
