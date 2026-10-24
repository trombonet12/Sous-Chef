package app.trombito.souschef.ui.ficha

import androidx.compose.material3.MaterialTheme
import app.trombito.souschef.receta.Grupo
import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.crono.LimpiezaCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** El botón «Compartir» al pie de la lista de ingredientes. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-xxhdpi")
class FichaCompartirTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val receta =
        Receta(
            slug = "sopa",
            titulo = "Sopa",
            descripcion = "",
            raciones = 2,
            totalMin = 20.0,
            grupos = listOf(Grupo("Base", listOf(Ingrediente("Cebolla", a = 100.0, u = "g")))),
        )

    private val estado = FichaUiState(cargando = false, receta = receta, racionesPermitidas = listOf(2))

    @Test
    fun `el boton compartir esta al pie de la lista, con texto y descripcion`() {
        val banco = BancoFicha()
        banco.poner {
            MaterialTheme {
                PestanaIngredientes(receta, estado)
            }
        }
        banco.conEtiqueta("compartir-ingredientes")
        assertTrue(banco.textos().contains("Compartir"))
        assertEquals(1, banco.descripciones().count { it == "Compartir la lista de ingredientes" })
    }
}
