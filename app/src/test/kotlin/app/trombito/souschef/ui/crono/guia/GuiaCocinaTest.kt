package app.trombito.souschef.ui.crono.guia

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.theme.DarkColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** La superposición de la guía en la JVM: avanza, salta los puntos ausentes y avisa al acabar o saltar. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class GuiaCocinaTest {
    private val todos = listOf("carril", "hecho", "prorroga", "deshacer", "micro", "menu")

    private fun texto(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    private class Cuenta {
        var terminadas = 0
    }

    /** Una cocina de juguete con solo los puntos [ids], como una disposición sin micro o sin deshacer. */
    private fun BancoCompose.cocina(
        ids: List<String>,
        cuenta: Cuenta,
    ) = poner {
        MaterialTheme(colorScheme = DarkColors) {
            val estado = remember { GuiaEstado() }
            CompositionLocalProvider(LocalGuia provides estado) {
                Box(Modifier.fillMaxSize()) {
                    Column { ids.forEach { Box(Modifier.size(100.dp).guia(it)) } }
                    GuiaCocina(estado, alTerminar = { cuenta.terminadas++ })
                }
            }
        }
    }

    private fun BancoCompose.globo() = textos(conEtiqueta("guia-globo"))

    private fun BancoCompose.pulsarBoton(etiqueta: String) = pulsar(conEtiqueta(etiqueta))

    @Test
    fun `empieza por el carril y avisa de que el reloj corre`() {
        val banco = BancoCompose()
        banco.cocina(listOf("carril", "hecho", "menu"), Cuenta())

        val globo = banco.globo()
        assertTrue(texto(R.string.guia_carril_titulo) in globo)
        assertTrue(texto(R.string.guia_carril_texto) in globo)
        assertTrue(texto(R.string.guia_aviso_reloj) in globo)
        assertTrue(texto(R.string.guia_siguiente) in globo)
        assertTrue(texto(R.string.guia_saltar) in globo)
    }

    @Test
    fun `avanza y se salta los puntos que no existen`() {
        val banco = BancoCompose()
        val cuenta = Cuenta()
        banco.cocina(listOf("carril", "hecho", "menu"), cuenta)

        banco.pulsarBoton("guia-siguiente")
        var globo = banco.globo()
        assertTrue(texto(R.string.guia_hecho_titulo) in globo)
        assertTrue(texto(R.string.guia_aviso_reloj) !in globo)
        assertTrue(texto(R.string.guia_siguiente) in globo)

        // Sin prórroga, deshacer ni micro: tras «hecho» va directo el menú, que es el último.
        banco.pulsarBoton("guia-siguiente")
        globo = banco.globo()
        assertTrue(texto(R.string.guia_menu_titulo) in globo)
        assertTrue(texto(R.string.guia_a_cocinar) in globo)
        assertEquals(0, cuenta.terminadas)

        banco.pulsarBoton("guia-siguiente")
        assertEquals(1, cuenta.terminadas)
    }

    @Test
    fun `saltar la guia avisa de que termino`() {
        val banco = BancoCompose()
        val cuenta = Cuenta()
        banco.cocina(todos, cuenta)

        banco.pulsarBoton("guia-siguiente")
        banco.pulsarBoton("guia-saltar")

        assertEquals(1, cuenta.terminadas)
    }

    @Test
    fun `con todos los puntos los cuenta de uno en uno`() {
        val banco = BancoCompose()
        banco.cocina(todos, Cuenta())

        assertTrue("1 de 6" in banco.globo())
        repeat(4) { banco.pulsarBoton("guia-siguiente") }
        assertTrue(texto(R.string.guia_micro_titulo) in banco.globo())
        assertTrue("5 de 6" in banco.globo())
    }

    @Test
    fun `sin ningun punto a la vista no pinta nada`() {
        val banco = BancoCompose()
        banco.cocina(emptyList(), Cuenta())

        assertTrue(banco.etiquetas("guia").isEmpty())
    }
}
