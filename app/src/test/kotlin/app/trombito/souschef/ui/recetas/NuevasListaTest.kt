package app.trombito.souschef.ui.recetas

import androidx.compose.material3.MaterialTheme
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.crono.BancoCompose
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NuevasListaTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val hoy = LocalDate.of(2026, 10, 24)
    private val reciente = receta("reciente").copy(creada = "2026-10-20T00:00:00Z")
    private val cocinada = receta("cocinada").copy(creada = "2026-10-20T00:00:00Z")
    private val vieja = receta("vieja").copy(creada = "2026-01-05T00:00:00Z")
    private val sinFecha = receta("sin-fecha")
    private val todas = listOf(reciente, cocinada, vieja, sinFecha)
    private val nuevas = OpcionFiltro.Mostrar(MostrarRecetas.NUEVAS)

    private fun estado(activos: Set<OpcionFiltro> = emptySet()): RecetasUiState =
        construirEstadoRecetas(
            todas,
            emptyMap(),
            mapOf("cocinada" to "2026-10-22"),
            FiltroRecetas("", activos),
            ContextoLista(hoy),
        )

    @Test
    fun `solo la receta sin cocinar y reciente lleva la marca nueva`() {
        val e = estado()

        assertEquals(listOf("reciente"), e.tarjetas.filter { it.nueva }.map { it.receta.slug })
    }

    @Test
    fun `el filtro Nuevas deja solo las nuevas y su recuento las cuenta`() {
        val e = estado()
        assertEquals(1, chipNuevas(e).n)

        assertEquals(listOf("reciente"), estado(setOf(nuevas)).tarjetas.map { it.receta.slug })
    }

    @Test
    fun `Nuevas es una opcion de Mostrar aunque no haya ninguna`() {
        val e =
            construirEstadoRecetas(
                listOf(vieja),
                emptyMap(),
                emptyMap(),
                FiltroRecetas(),
                ContextoLista(hoy),
            )

        assertEquals(0, chipNuevas(e).n)
    }

    private fun chipNuevas(e: RecetasUiState): ChipFiltro {
        val mostrar = e.secciones.first { it.seccion == SeccionFiltro.MOSTRAR }
        return mostrar.chips.first { it.opcion == nuevas }
    }

    private fun textos(
        receta: Receta,
        nueva: Boolean,
        disposicion: DisposicionLista,
    ): List<String> {
        val banco = BancoCompose()
        val tarjeta =
            TarjetaReceta(
                receta = receta,
                favorita = false,
                fuegos = 0,
                ultimaFecha = null,
                hace = Hace(UnidadHace.SIN_ESTRENAR),
                nueva = nueva,
            )
        banco.poner { MaterialTheme { TarjetaLista(tarjeta, disposicion, {}, {}) } }
        return banco.textos(banco.raiz())
    }

    @Test
    fun `las tres tarjetas muestran Nueva dentro de la tarjeta`() {
        DisposicionLista.entries.forEach { d ->
            val textos = textos(reciente, nueva = true, disposicion = d)
            assertTrue("$d $textos", "Nueva" in textos)
        }
    }

    @Test
    fun `una tarjeta que no es nueva no muestra la etiqueta`() {
        DisposicionLista.entries.forEach { d ->
            assertFalse("$d", "Nueva" in textos(vieja, nueva = false, disposicion = d))
        }
    }
}
