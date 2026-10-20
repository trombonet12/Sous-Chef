package app.trombito.souschef.ui.ajustes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.ui.crono.BancoCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Primer botón con esa descripción de accesibilidad. */
internal fun BancoCompose.botonConDescripcion(descripcion: String): SemanticsNode {
    fun contiene(n: SemanticsNode): Boolean =
        n.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(descripcion) == true ||
            n.children.any(::contiene)

    fun pulsables(n: SemanticsNode): List<SemanticsNode> =
        listOfNotNull(n.takeIf { it.config.contains(SemanticsActions.OnClick) }) + n.children.flatMap(::pulsables)
    return checkNotNull(pulsables(raiz()).firstOrNull(::contiene)) { "No hay botón $descripcion" }
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SelectorCocinaTest {
    private val banco = BancoCompose()

    private fun ponerConEstado(inicial: Cocina = Cocina.VITRO_9): MutableList<Cocina> {
        val elegidas = mutableListOf<Cocina>()
        banco.poner {
            MaterialTheme {
                var actual by remember { mutableStateOf(inicial) }
                SelectorCocina(actual, {
                    elegidas += it
                    actual = it
                })
            }
        }
        return elegidas
    }

    private fun ejemplo(): String = banco.textos(banco.conEtiqueta("cocina-ejemplo")).single()

    private fun marcado(etiqueta: String): Boolean? =
        banco.conEtiqueta(etiqueta).config.getOrNull(SemanticsProperties.Selected)

    @Test
    fun `por defecto es vitro digital y el ejemplo conserva los numeros`() {
        ponerConEstado()

        assertEquals(
            listOf("cocina-tipo-VITRO", "cocina-tipo-INDUCCION", "cocina-tipo-GAS"),
            banco.etiquetas("cocina-tipo-"),
        )
        assertEquals(true, marcado("cocina-tipo-VITRO"))
        assertEquals(true, marcado("cocina-mando-DIGITAL"))
        assertEquals("Pon la sartén al 8 y, cuando humee, baja al 6.", ejemplo())
        assertTrue(banco.textos(banco.conEtiqueta("cocina-ejemplo-chip")).contains("🔥 6"))
    }

    @Test
    fun `elegir gas propone ruleta sin numeros y pone el ejemplo en palabras`() {
        val elegidas = ponerConEstado()

        banco.pulsar(banco.conEtiqueta("cocina-tipo-GAS"))

        assertEquals(Cocina(TipoCocina.GAS, Mando.ANALOGICO, null), elegidas.last())
        assertEquals("Pon la sartén a fuego alto y, cuando humee, baja a fuego medio-alto.", ejemplo())
        assertTrue(banco.textos(banco.conEtiqueta("cocina-ejemplo-chip")).contains("🔥 medio-alto"))
        assertEquals(true, marcado("cocina-tipo-GAS"))
        assertEquals(false, marcado("cocina-tipo-VITRO"))
    }

    @Test
    fun `con ruleta el maximo cabe en el rango del mando y el ejemplo se traduce a medios`() {
        val elegidas = ponerConEstado(Cocina(TipoCocina.GAS, Mando.ANALOGICO, 6))

        // 8 de 9 en una ruleta 1-6 son 5,3 -> 5,5; y 6 de 9 son 4.
        assertEquals("Pon la sartén al 5,5 y, cuando humee, baja al 4.", ejemplo())

        banco.pulsar(banco.botonConDescripcion("Bajar la potencia máxima"))
        banco.pulsar(banco.botonConDescripcion("Bajar la potencia máxima"))
        banco.pulsar(banco.botonConDescripcion("Bajar la potencia máxima"))
        banco.pulsar(banco.botonConDescripcion("Bajar la potencia máxima"))

        assertEquals(Cocina.ANALOGICO_MIN, elegidas.last().max)
    }

    @Test
    fun `pasar de ruleta a digital recoloca el maximo en 9 como minimo`() {
        val elegidas = ponerConEstado(Cocina(TipoCocina.VITRO, Mando.ANALOGICO, 6))

        banco.pulsar(banco.conEtiqueta("cocina-mando-DIGITAL"))

        assertEquals(Cocina(TipoCocina.VITRO, Mando.DIGITAL, Cocina.DIGITAL_MIN), elegidas.last())
    }

    @Test
    fun `en digital sube la potencia maxima y no hay interruptor de sin numeros`() {
        val elegidas = ponerConEstado()

        banco.pulsar(banco.botonConDescripcion("Subir la potencia máxima"))

        assertEquals(10, elegidas.last().max)
        assertEquals(emptyList<String>(), banco.etiquetas("cocina-sin-numeros"))
    }

    @Test
    fun `apagar sin numeros devuelve un mando numerado`() {
        val elegidas = ponerConEstado(Cocina(TipoCocina.GAS, Mando.ANALOGICO, null))

        banco.pulsar(banco.conEtiqueta("cocina-sin-numeros"))

        assertEquals(Cocina(TipoCocina.GAS, Mando.ANALOGICO, 9), elegidas.last())
    }
}
