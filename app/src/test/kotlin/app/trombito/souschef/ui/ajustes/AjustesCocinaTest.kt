package app.trombito.souschef.ui.ajustes

import androidx.compose.material3.MaterialTheme
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.ui.Rutas
import app.trombito.souschef.ui.crono.BancoCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AjustesCocinaTest {
    private val banco = BancoCompose()
    private val gasSinNumeros = Cocina(TipoCocina.GAS, Mando.ANALOGICO, null)

    @Test
    fun `la pantalla muestra los tres selectores y las raciones por defecto`() {
        banco.poner { MaterialTheme { AjustesCocina(Ajustes(fuegos = 4, racionesDefecto = 3), {}, {}, {}, {}, {}) } }

        val textos = banco.textos(banco.raiz())
        assertTrue(textos.toString(), "Tu cocina" in textos)
        assertTrue(textos.toString(), "Tipo de cocina" in textos)
        assertTrue(textos.toString(), "Mando" in textos)
        assertTrue(textos.toString(), "Potencia máxima" in textos)
        assertTrue(textos.toString(), "Tu equipo" in textos)
        assertTrue(textos.toString(), "Fuegos" in textos)
        assertTrue(textos.toString(), "4" in textos)
        assertTrue(textos.toString(), "Tengo horno" in textos)
        assertTrue(textos.toString(), "Raciones por defecto" in textos)
        assertTrue(textos.toString(), "3 raciones" in textos)
        assertTrue(
            textos.toString(),
            "Cada receta tiene su rango: la ficha se ajusta a lo que admite." in textos,
        )
        banco.conEtiqueta("cocina-tipo-GAS")
        banco.conEtiqueta("equipo-horno")
    }

    @Test
    fun `la pantalla avisa de cada cambio con el valor ya elegido`() {
        val cocinas = mutableListOf<Cocina>()
        val fuegos = mutableListOf<Int>()
        val hornos = mutableListOf<Boolean>()
        val raciones = mutableListOf<Int>()
        banco.poner {
            MaterialTheme {
                AjustesCocina(
                    Ajustes(fuegos = 3, horno = true, racionesDefecto = 2),
                    {},
                    { cocinas += it },
                    { fuegos += it },
                    { hornos += it },
                    { raciones += it },
                )
            }
        }

        banco.pulsar(banco.conEtiqueta("cocina-tipo-INDUCCION"))
        banco.pulsar(banco.botonConDescripcion("Un fuego más"))
        banco.pulsar(banco.botonConDescripcion("Un fuego menos"))
        banco.pulsar(banco.conEtiqueta("equipo-horno"))
        banco.pulsar(banco.botonConDescripcion("Una ración más"))
        banco.pulsar(banco.botonConDescripcion("Una ración menos"))

        assertEquals(listOf(Cocina(TipoCocina.INDUCCION)), cocinas)
        assertEquals(listOf(4, 2), fuegos)
        assertEquals(listOf(false), hornos)
        assertEquals(listOf(3, 1), raciones)
    }

    @Test
    fun `fuegos y raciones no salen de su rango`() {
        val fuegos = mutableListOf<Int>()
        val raciones = mutableListOf<Int>()
        val ajustes = Ajustes(fuegos = 6, racionesDefecto = 1)
        banco.poner {
            MaterialTheme { AjustesCocina(ajustes, {}, {}, { fuegos += it }, {}, { raciones += it }) }
        }

        banco.pulsar(banco.botonConDescripcion("Un fuego más"))
        banco.pulsar(banco.botonConDescripcion("Una ración menos"))

        assertEquals(listOf(6), fuegos)
        assertEquals(listOf(1), raciones)
    }

    @Test
    fun `el menu abre Tu cocina desde la primera fila con su resumen`() {
        val abiertas = mutableListOf<String>()
        val ajustes = Ajustes(cocina = gasSinNumeros, fuegos = 3, horno = true)
        banco.poner { MaterialTheme { AjustesMenu(ajustes, "1", {}, { abiertas += it }) } }

        val textos = banco.textos(banco.raiz())
        assertTrue(textos.toString(), "Tu cocina" in textos)
        assertTrue(textos.toString(), "Gas · sin números · 3 fuegos · horno" in textos)
        assertEquals("ajustes-${Rutas.AJUSTES_COCINA}", banco.etiquetas("ajustes-ajustes/").first())

        banco.pulsar(banco.conEtiqueta("ajustes-${Rutas.AJUSTES_COCINA}"))

        assertEquals(listOf(Rutas.AJUSTES_COCINA), abiertas)
    }

    @Test
    fun `el resumen de una vitro digital lleva el rango y omite el horno si no hay`() {
        val ajustes = Ajustes(cocina = Cocina.VITRO_9, fuegos = 3, horno = false)
        banco.poner { MaterialTheme { AjustesMenu(ajustes, "1", {}, {}) } }

        assertTrue(banco.textos(banco.raiz()).toString(), "Vitro digital 1-9 · 3 fuegos" in banco.textos(banco.raiz()))
    }

    @Test
    fun `el resumen distingue ruleta con numeros e induccion con un fuego`() {
        val ajustes = Ajustes(cocina = Cocina(TipoCocina.INDUCCION, Mando.ANALOGICO, 12), fuegos = 1, horno = true)
        banco.poner { MaterialTheme { AjustesMenu(ajustes, "1", {}, {}) } }

        val textos = banco.textos(banco.raiz())
        assertTrue(textos.toString(), "Inducción · ruleta 1-12 · 1 fuego · horno" in textos)
    }
}
