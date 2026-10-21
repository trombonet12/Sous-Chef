package app.trombito.souschef.ui.recetas

import androidx.compose.material3.MaterialTheme
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tags
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
class EtiquetasEquipoTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private fun receta(
        slug: String,
        fuegos: Int,
        horno: Boolean,
        base: String? = "pasta",
    ): Receta {
        val hornos = if (horno) listOf(Carril("h", tipo = "horno")) else emptyList()
        val carriles = List(fuegos) { Carril("f${it + 1}", tipo = "fuego") } + hornos
        return Receta(
            slug = slug,
            titulo = "Receta $slug",
            totalMin = 30.0,
            tags = Tags(base = base),
            carriles = carriles,
            pasos = listOf(Paso("p1", "Hacer", carriles.first().id, 0.0, 10.0)),
        )
    }

    private fun estado(
        fuegos: Int,
        horno: Boolean,
        vararg recetas: Receta,
    ): RecetasUiState =
        construirEstadoRecetas(
            recetas.toList(),
            emptyMap(),
            emptyMap(),
            FiltroRecetas(),
            ContextoLista(LocalDate.of(2026, 10, 20), fuegos = fuegos, horno = horno),
        )

    private fun tarjeta(
        e: RecetasUiState,
        slug: String,
    ) = e.tarjetas.first { it.receta.slug == slug }

    @Test
    fun `el estado expone los fuegos y el horno de Ajustes`() {
        val e = estado(2, false, receta("a", 1, false))

        assertEquals(2, e.fuegos)
        assertFalse(e.horno)
    }

    @Test
    fun `solo pide fuegos la receta con mas carriles de fuego que la cocina`() {
        val e = estado(3, true, receta("tres", 3, false), receta("cuatro", 4, false), receta("seis", 6, false))

        assertEquals(0, tarjeta(e, "tres").pideFuegos)
        assertEquals(4, tarjeta(e, "cuatro").pideFuegos)
        assertEquals(6, tarjeta(e, "seis").pideFuegos)
    }

    @Test
    fun `solo pide horno la receta con carril de horno cuando no hay horno`() {
        val sinHorno = estado(3, false, receta("con", 1, true), receta("sin", 1, false))
        assertTrue(tarjeta(sinHorno, "con").pideHorno)
        assertFalse(tarjeta(sinHorno, "sin").pideHorno)

        val conHorno = estado(3, true, receta("con", 1, true))
        assertFalse(tarjeta(conHorno, "con").pideHorno)
    }

    @Test
    fun `bunuelo es de la familia de las masas`() {
        assertEquals(FamiliaBase.MASAS, FamiliaBase.de("buñuelo"))
        assertEquals(FamiliaBase.MASAS, FamiliaBase.de(" Buñuelo "))
    }

    private fun pintar(
        disposicion: DisposicionLista,
        pideFuegos: Int,
        pideHorno: Boolean,
    ): List<String> {
        val banco = BancoCompose()
        val tarjeta =
            TarjetaReceta(
                receta = receta("x", 4, true),
                favorita = false,
                fuegos = 4,
                ultimaFecha = null,
                hace = Hace(UnidadHace.SIN_ESTRENAR),
                conHorno = true,
                pideFuegos = pideFuegos,
                pideHorno = pideHorno,
            )
        banco.poner { MaterialTheme { TarjetaLista(tarjeta, disposicion, {}, {}) } }
        return banco.textos(banco.raiz())
    }

    @Test
    fun `las tres tarjetas muestran Pide N fuegos y Pide horno`() {
        DisposicionLista.entries.forEach { d ->
            val textos = pintar(d, pideFuegos = 4, pideHorno = true)
            assertTrue("$d $textos", "Pide 4 fuegos" in textos)
            assertTrue("$d $textos", "Pide horno" in textos)
        }
    }

    @Test
    fun `sin falta de equipo no hay etiquetas`() {
        DisposicionLista.entries.forEach { d ->
            val textos = pintar(d, pideFuegos = 0, pideHorno = false)
            assertTrue("$d $textos", textos.none { it.startsWith("Pide") })
        }
    }

    @Test
    fun `solo horno o solo fuegos muestran una etiqueta`() {
        val soloHorno = pintar(DisposicionLista.FRANJA, pideFuegos = 0, pideHorno = true)
        assertTrue("Pide horno" in soloHorno)
        assertTrue(soloHorno.none { it.startsWith("Pide ") && it != "Pide horno" })

        val soloFuegos = pintar(DisposicionLista.TINTE, pideFuegos = 5, pideHorno = false)
        assertTrue("Pide 5 fuegos" in soloFuegos)
        assertFalse("Pide horno" in soloFuegos)
    }
}
