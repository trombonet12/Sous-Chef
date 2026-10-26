package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import app.trombito.souschef.ui.shell.Icono
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Las entradas del menú ⋮ de la cocina: sin «Ingredientes», todas en frase normal y cada una con su icono. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class MenuCocinaTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val banco = BancoCompose()

    private val iconos =
        listOf(Icono.Gantt, Icono.Rejilla, Icono.RelojArena, Icono.Microfono, Icono.Reiniciar, Icono.Salir)

    private fun ponerMenu(
        armado: Boolean = false,
        eventos: MutableList<String> = mutableListOf(),
    ) {
        val menu =
            AccionesMenu(
                volver = { eventos += "volver" },
                abandonar = { eventos += "abandonar" },
                diagrama = { eventos += "diagrama" },
                interfaz = { eventos += "interfaz" },
                temporizador = { eventos += "temporizador" },
                ordenes = { eventos += "ordenes" },
            )
        banco.poner {
            MaterialTheme {
                Column {
                    EntradasMenuCocina(
                        reiniciarArmado = armado,
                        menu = menu,
                        cerrar = { eventos += "cerrar" },
                        onReiniciar = { eventos += "reiniciar" },
                    )
                }
            }
        }
    }

    private fun textos(): List<String> = banco.textos(banco.raiz())

    @Test
    fun noHayEntradaIngredientes() {
        ponerMenu()

        assertFalse("Ingredientes" in textos())
    }

    @Test
    fun todasLasEntradasEstanEnFraseNormal() {
        ponerMenu()

        assertEquals(
            listOf("Diagrama de tiempos", "Interfaz", "Temporizador", "Órdenes de voz", "Reiniciar", "Abandonar"),
            textos(),
        )
        textos().forEach { assertTrue(it, it != it.uppercase()) }
    }

    @Test
    fun cadaEntradaTieneSuIconoDecorativo() {
        ponerMenu()

        iconos.forEach { icono ->
            val nodo = banco.conEtiqueta(etiquetaIconoMenu(icono))
            assertTrue(icono.name, nodo.size.width > 0 && nodo.size.height > 0)
        }
        assertEquals(iconos.size, banco.etiquetas("icono-menu-").size)
    }

    @Test
    fun reiniciarArmadoPideConfirmacionEnFraseNormal() {
        ponerMenu(armado = true)

        assertTrue("¿Seguro?" in textos())
        assertFalse("Reiniciar" in textos())
    }

    @Test
    fun abandonarCierraElMenuYLoPide() {
        val eventos = mutableListOf<String>()
        ponerMenu(eventos = eventos)

        banco.pulsar(banco.pulsableConTexto("Abandonar"))

        assertEquals(listOf("cerrar", "abandonar"), eventos)
    }

    @Test
    fun reiniciarSinArmarNoCierraElMenu() {
        val eventos = mutableListOf<String>()
        ponerMenu(eventos = eventos)

        banco.pulsar(banco.pulsableConTexto("Reiniciar"))

        assertEquals(listOf("reiniciar"), eventos)
    }
}
