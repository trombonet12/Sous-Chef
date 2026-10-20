package app.trombito.souschef.ui.comun

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.shell.Icono
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class RejillaCategoriasTest {
    private val categorias =
        listOf(
            CategoriaVista("pasta", "Pasta", Icono.Pasta, Paleta.de(TonoPaleta.AMBAR, false), "5 recetas"),
            CategoriaVista("arroz", "Arroz", Icono.Arroz, Paleta.de(TonoPaleta.OLIVA, false), "1 receta"),
            CategoriaVista("patata", "Patata", Icono.Patata, Paleta.de(TonoPaleta.TEJA, false), "3 recetas"),
            CategoriaVista("otra", "Otra", Icono.Plato, Paleta.de(TonoPaleta.PIEDRA, false), "2 recetas"),
        )

    private fun pintar(onAbrir: (String) -> Unit = {}): BancoCompose {
        val banco = BancoCompose()
        banco.poner { MaterialTheme { RejillaCategorias(categorias, onAbrir, columnas = 3) } }
        return banco
    }

    @Test
    fun `con cuatro categorias y tres columnas pinta cuatro cuadraditos`() {
        val etiquetas = pintar().etiquetas("categoria-")
        assertEquals(listOf("categoria-pasta", "categoria-arroz", "categoria-patata", "categoria-otra"), etiquetas)
    }

    @Test
    fun `tocar un cuadradito abre su categoria`() {
        val abiertas = mutableListOf<String>()
        val banco = pintar { abiertas += it }

        banco.pulsar(banco.conEtiqueta("categoria-patata"))

        assertEquals(listOf("patata"), abiertas)
    }

    @Test
    fun `la descripcion incluye nombre y recuento`() {
        val banco = pintar()
        val descripcion =
            banco
                .conEtiqueta("categoria-arroz")
                .config
                .getOrNull(SemanticsProperties.ContentDescription)
                .orEmpty()
        assertTrue(descripcion.toString(), descripcion.contains("Arroz, 1 receta"))
    }
}
