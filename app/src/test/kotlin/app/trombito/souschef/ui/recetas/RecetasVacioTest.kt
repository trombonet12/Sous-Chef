package app.trombito.souschef.ui.recetas

import androidx.compose.material3.MaterialTheme
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.crono.LimpiezaCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** «Nada con eso» (F6): «Pedir esta receta» lleva lo buscado al Buzón y «Quitar filtros» limpia los filtros. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class RecetasVacioTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val vacio = RecetasUiState(cargando = false, totalRecetas = 3)

    private class Registro {
        val pedidos = mutableListOf<String>()
        var limpiados = 0
    }

    private fun pintar(
        estado: RecetasUiState,
        registro: Registro = Registro(),
    ): BancoCompose {
        val banco = BancoCompose()
        val acciones = AccionesFiltro({}, { registro.limpiados++ }, {})
        banco.poner {
            MaterialTheme {
                val lista = AccionesLista({ _, _ -> }, {}, pedir = { registro.pedidos += it })
                RecetasContenido(estado, {}, acciones, lista)
            }
        }
        return banco
    }

    private val filtro = OpcionFiltro.Mostrar(MostrarRecetas.FAVORITAS)

    @Test
    fun `con una busqueda sin resultados sale Pedir esta receta y lleva lo buscado`() {
        val registro = Registro()
        val banco = pintar(vacio.copy(consulta = "  paella de verduras "), registro)

        assertTrue(banco.textos(banco.raiz()).contains("Pedir esta receta"))
        banco.pulsar(banco.conEtiqueta("recetas-pedir"))

        assertEquals(listOf("paella de verduras"), registro.pedidos)
        assertTrue(banco.etiquetas("recetas-quitar-filtros").isEmpty())
    }

    @Test
    fun `sin busqueda no hay Pedir esta receta`() {
        val banco = pintar(vacio.copy(activos = listOf(filtro)))
        assertTrue(banco.etiquetas("recetas-pedir").isEmpty())
    }

    @Test
    fun `con filtros activos sale Quitar filtros y los limpia`() {
        val registro = Registro()
        val banco = pintar(vacio.copy(consulta = "paella", activos = listOf(filtro)), registro)

        assertTrue(banco.textos(banco.raiz()).contains("Quitar filtros"))
        banco.pulsar(banco.conEtiqueta("recetas-quitar-filtros"))

        assertEquals(1, registro.limpiados)
        assertEquals(emptyList<String>(), registro.pedidos)
    }

    @Test
    fun `sin filtros no hay Quitar filtros`() {
        val banco = pintar(vacio.copy(consulta = "paella"))
        assertTrue(banco.etiquetas("recetas-quitar-filtros").isEmpty())
    }
}
