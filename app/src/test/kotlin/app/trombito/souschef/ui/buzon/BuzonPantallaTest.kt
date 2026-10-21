package app.trombito.souschef.ui.buzon

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.ui.ajustes.AjustesMenu
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

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BuzonPantallaTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val datos = DatosApp("1.4.0", "Pixel 9", "16", 36, "Vitro digital 1-9")

    private class Registro {
        val correos = mutableListOf<Correo>()
        val copias = mutableListOf<Correo>()
        val ediciones = mutableListOf<(FormularioBuzon) -> FormularioBuzon>()
        var hayCorreo = true
    }

    private fun pintar(
        formulario: FormularioBuzon,
        registro: Registro = Registro(),
    ): BancoCompose {
        val acciones =
            AccionesBuzon(
                atras = {},
                editar = { registro.ediciones += it },
                enviar = {
                    registro.correos += it
                    registro.hayCorreo
                },
                copiar = { registro.copias += it },
            )
        val banco = BancoCompose()
        banco.poner { MaterialTheme { BuzonContenido(formulario, datos, acciones) } }
        return banco
    }

    @Test
    fun elSelectorTieneLosTresTiposYLaRecetaMuestraSusCampos() {
        val banco = pintar(FormularioBuzon(TipoBuzon.RECETA))
        assertEquals(
            setOf("buzon-tipo-receta", "buzon-tipo-fallo", "buzon-tipo-sugerencia"),
            banco.etiquetas("buzon-tipo-").toSet(),
        )
        assertEquals(
            setOf("buzon-tiempo-menos_45", "buzon-tiempo-de_45_a_90", "buzon-tiempo-mas_90"),
            banco.etiquetas("buzon-tiempo-").toSet(),
        )
        assertTrue(banco.etiquetas("buzon-").containsAll(listOf("buzon-plato", "buzon-ingredientes", "buzon-raciones")))
        assertFalse(banco.etiquetas("buzon-").contains("buzon-que-paso"))
    }

    @Test
    fun elFalloMuestraSusCamposYElDesplegableConLaSeccionElegida() {
        val vacio = pintar(FormularioBuzon(TipoBuzon.FALLO))
        val etiquetas = vacio.etiquetas("buzon-")
        assertTrue(
            etiquetas.toString(),
            etiquetas.containsAll(listOf("buzon-que-paso", "buzon-que-hacias", "buzon-pantalla")),
        )
        assertFalse(etiquetas.contains("buzon-plato"))
        assertTrue(vacio.textos(vacio.raiz()).contains("Elige una pantalla"))

        val elegida = pintar(FormularioBuzon(TipoBuzon.FALLO, pantalla = SeccionApp.CRONOMETRO))
        assertTrue(elegida.textos(elegida.raiz()).contains("Cronómetro"))
    }

    @Test
    fun laSugerenciaSoloTieneUnCampoLibre() {
        val etiquetas = pintar(FormularioBuzon(TipoBuzon.SUGERENCIA)).etiquetas("buzon-")
        assertTrue(etiquetas.contains("buzon-sugerencia"))
        assertFalse(etiquetas.contains("buzon-plato"))
        assertFalse(etiquetas.contains("buzon-que-paso"))
    }

    @Test
    fun tocarUnTipoEditaElFormulario() {
        val registro = Registro()
        val banco = pintar(FormularioBuzon(TipoBuzon.RECETA), registro)

        banco.pulsar(banco.conEtiqueta("buzon-tipo-fallo"))

        assertEquals(TipoBuzon.FALLO, registro.ediciones.single()(FormularioBuzon()).tipo)
    }

    @Test
    fun elBotonEstaApagadoSinElCampoPrincipal() {
        val banco = pintar(FormularioBuzon(TipoBuzon.RECETA))
        assertTrue(banco.conEtiqueta("buzon-enviar").config.contains(SemanticsProperties.Disabled))
        val relleno = pintar(FormularioBuzon(TipoBuzon.RECETA, plato = "Lasaña"))
        assertFalse(relleno.conEtiqueta("buzon-enviar").config.contains(SemanticsProperties.Disabled))
    }

    @Test
    fun enviarPasaElCorreoMontadoYConAppDeCorreoNoHayAviso() {
        val registro = Registro()
        val banco = pintar(FormularioBuzon(TipoBuzon.RECETA, plato = "Lasaña", raciones = "2"), registro)

        banco.pulsar(banco.conEtiqueta("buzon-enviar"))

        val correo = registro.correos.single()
        assertEquals("[Sous Chef] Pedir una receta", correo.asunto)
        assertTrue(correo.cuerpo, correo.cuerpo.startsWith("Plato: Lasaña\nRaciones: 2\n\n— Datos —"))
        assertTrue(banco.etiquetas("buzon-sin-correo").isEmpty())
    }

    @Test
    fun sinAppDeCorreoSaleElAvisoYElBotonCopiaElTexto() {
        val registro = Registro().apply { hayCorreo = false }
        val banco = pintar(FormularioBuzon(TipoBuzon.SUGERENCIA, texto = "Hola"), registro)

        banco.pulsar(banco.conEtiqueta("buzon-enviar"))

        assertTrue(banco.textos(banco.raiz()).contains("No hay app de correo; copia el texto"))
        assertTrue(banco.etiquetas("buzon-copiado").isEmpty())

        banco.pulsar(banco.conEtiqueta("buzon-copiar"))

        assertEquals(registro.correos.single(), registro.copias.single())
        assertEquals(listOf("buzon-copiado"), banco.etiquetas("buzon-copiado"))
    }

    @Test
    fun lasFilaDeAjustesAbreElBuzon() {
        var abierto = 0
        val banco = BancoCompose()
        banco.poner { MaterialTheme { AjustesMenu(Ajustes(), "1", {}, {}, { abierto++ }) } }

        banco.pulsar(banco.conEtiqueta("ajustes-buzon"))

        assertEquals(1, abierto)
    }
}
