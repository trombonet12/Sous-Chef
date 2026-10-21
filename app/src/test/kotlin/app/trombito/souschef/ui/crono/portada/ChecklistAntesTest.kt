package app.trombito.souschef.ui.crono.portada

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.ui.comun.LocalCocina
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.crono.BloqueCasillas
import app.trombito.souschef.ui.crono.Casilla
import app.trombito.souschef.ui.crono.LimpiezaCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** La checklist de la portada en la JVM: progreso, bloques plegables, «Marcar todo», ⓘ y chip de minuto. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ChecklistAntesTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val bloques =
        listOf(
            BloqueCasillas(
                "Mesa",
                listOf(
                    Casilla("a", "<b>Cebolla</b> · picada fina, en cuadritos pequeños · minuto 3."),
                    Casilla("b", "<b>Aceite</b> · 2 cucharadas"),
                ),
            ),
            BloqueCasillas("Fuego", listOf(Casilla("c", "<b>Sartén</b> · al {7}"))),
        )

    private var marcados by mutableStateOf(emptySet<String>())
    private val llamadas = mutableListOf<Pair<List<String>, Boolean>>()

    private fun poner(
        banco: BancoCompose,
        cocina: Cocina = Cocina.VITRO_9,
        lista: List<BloqueCasillas> = bloques,
        conDetalle: Boolean = true,
    ) = banco.poner {
        CompositionLocalProvider(LocalCocina provides cocina) {
            ChecklistAntes(
                encabezado = EncabezadoChecklist("Antes de encender nada", "Una receta cualquiera"),
                bloques = lista,
                marcados = marcados,
                conDetalle = conDetalle,
                acciones =
                    AccionesChecklist(
                        marcar = { id, m -> marcados = if (m) marcados + id else marcados - id },
                        marcarVarias = { ids, m ->
                            llamadas += ids to m
                            marcados = if (m) marcados + ids else marcados - ids.toSet()
                        },
                    ),
            )
        }
    }

    private fun BancoCompose.casilla(titulo: String): SemanticsNode =
        nodos().first { n ->
            n.config.getOrNull(SemanticsProperties.ToggleableState) != null && textos(n).contains(titulo)
        }

    private fun BancoCompose.botonesDetalle(): List<SemanticsNode> =
        nodos().filter { n ->
            n.config.getOrNull(SemanticsProperties.ContentDescription)?.any { it.startsWith("Detalle de") } == true
        }

    @Test
    fun elProgresoCuentaLasCasillasMarcadasSobreElTotal() {
        marcados = setOf("a", "c")
        val banco = BancoCompose()
        poner(banco)
        assertTrue(banco.textos(banco.raiz()).contains("Antes de encender nada · 2/3"))
        assertTrue(banco.textos(banco.raiz()).contains("1/2"))
    }

    @Test
    fun unBloqueCompletoSePliegaMuestraHechoYSePuedeReabrir() {
        marcados = emptySet()
        val banco = BancoCompose()
        poner(banco)
        assertTrue(banco.textos(banco.conEtiqueta("bloque-0")).contains("Cebolla"))
        banco.pulsar(banco.casilla("Cebolla"))
        banco.pulsar(banco.casilla("Aceite"))
        val bloque = banco.textos(banco.conEtiqueta("bloque-0"))
        assertTrue(bloque.contains("✓ hecho"))
        assertFalse(bloque.contains("Cebolla"))
        banco.pulsar(banco.nodos().first { it.config.getOrNull(SemanticsProperties.StateDescription) == "plegado" })
        assertTrue(banco.textos(banco.conEtiqueta("bloque-0")).contains("Cebolla"))
    }

    @Test
    fun marcarTodoMarcaSoloSuBloqueYDespuesDesmarca() {
        marcados = emptySet()
        llamadas.clear()
        val banco = BancoCompose()
        poner(banco)
        banco.pulsar(banco.pulsableConTexto("Marcar todo", banco.conEtiqueta("bloque-0")))
        assertEquals(setOf("a", "b"), marcados)
        assertEquals(listOf(listOf("a", "b") to true), llamadas)
        banco.pulsar(banco.pulsableConTexto("Desmarcar todo", banco.conEtiqueta("bloque-0")))
        assertEquals(emptySet<String>(), marcados)
    }

    @Test
    fun laInfoMuestraElDetalleDeLaLinea() {
        marcados = emptySet()
        val banco = BancoCompose()
        poner(banco)
        assertFalse(banco.textos(banco.raiz()).any { it.startsWith("picada fina") })
        assertEquals(3, banco.botonesDetalle().size)
        val icono =
            banco.botonesDetalle().first {
                it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("Detalle de Cebolla") == true
            }
        val pulsable = generateSequence(icono) { it.parent }.first { it.config.contains(SemanticsActions.OnClick) }
        banco.pulsar(pulsable)
        assertTrue(banco.textos(banco.raiz()).any { it.startsWith("picada fina, en cuadritos pequeños") })
    }

    @Test
    fun elChipDeMinutoSaleConElNumeroDeLaLinea() {
        marcados = emptySet()
        val banco = BancoCompose()
        poner(banco)
        val textos = banco.textos(banco.raiz())
        assertEquals(listOf("min 3"), textos.filter { it.startsWith("min ") })
    }

    @Test
    fun laNotaDeCocinaSoloSaleSiNoEsVitro() {
        marcados = emptySet()
        val vitro = BancoCompose()
        poner(vitro)
        assertFalse(vitro.textos(vitro.raiz()).any { it.startsWith("Recetas pensadas para vitro") })
        val gas = BancoCompose()
        poner(gas, Cocina(TipoCocina.GAS, Mando.ANALOGICO, null))
        assertTrue(gas.textos(gas.raiz()).any { it.startsWith("Recetas pensadas para vitro") })
    }

    @Test
    fun sinDetalleLaLineaSePintaEnteraYSinBotonInfo() {
        marcados = emptySet()
        val banco = BancoCompose()
        val grupos = listOf(BloqueCasillas("Verduras", listOf(Casilla("g", "<b>200 g</b> · cebolla"))))
        poner(banco, lista = grupos, conDetalle = false)
        assertTrue(banco.textos(banco.raiz()).contains("200 g · cebolla"))
        assertEquals(0, banco.botonesDetalle().size)
    }
}
