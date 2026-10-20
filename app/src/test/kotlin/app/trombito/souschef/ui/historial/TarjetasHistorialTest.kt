package app.trombito.souschef.ui.historial

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.ui.crono.BancoCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class TarjetasHistorialTest {
    private val fila =
        FilaHistorial(
            entrada =
                EntradaHistorial(
                    id = "e1",
                    slug = "macarrones",
                    titulo = "Macarrones al horno",
                    fecha = "2026-10-14",
                    base = "pasta",
                    tecnica = listOf("sofrito"),
                    minutosReales = 45,
                    nota = "Quedó jugosa",
                ),
            repite = true,
            recetaExiste = true,
        )

    private fun pintar(
        disposicion: DisposicionLista,
        fila: FilaHistorial = this.fila,
        repetir: () -> Unit = {},
        borrar: () -> Unit = {},
    ): BancoCompose {
        val banco = BancoCompose()
        banco.poner {
            MaterialTheme {
                TarjetaHistorial(fila, disposicion, confirmando = false, onBorrar = borrar, onRepetir = repetir)
            }
        }
        return banco
    }

    private fun BancoCompose.descripciones(): List<String> =
        nodos().mapNotNull { it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull() }

    private fun BancoCompose.textos(): List<String> = textos(raiz())

    @Test
    fun cadaDisposicionPintaTituloYAcciones() {
        for (d in DisposicionLista.entries) {
            val textos = pintar(d).textos()
            assertTrue("$d $textos", "Macarrones al horno" in textos)
            assertTrue("$d $textos", "Repetir →" in textos)
            assertTrue("$d $textos", "Borrar" in textos)
        }
    }

    @Test
    fun tinteMuestraDiaMesYMinutosConUnaDescripcionUnica() {
        val banco = pintar(DisposicionLista.TINTE)
        val textos = banco.textos()
        assertTrue(textos.toString(), "14" in textos)
        assertTrue(textos.toString(), "oct" in textos)
        assertTrue(textos.toString(), "45 min" in textos)
        assertTrue(textos.toString(), "Dos seguidas" in textos)
        assertTrue(textos.toString(), "pasta · sofrito" in textos)
        assertTrue(banco.descripciones().toString(), "14 oct, 45 min reales" in banco.descripciones())
    }

    @Test
    fun franjaAbreConFechaYMinutosEnUnaLinea() {
        val textos = pintar(DisposicionLista.FRANJA).textos()
        assertTrue(textos.toString(), "14 oct · 45 min" in textos)
        assertTrue(textos.toString(), "pasta · sofrito" in textos)
    }

    @Test
    fun clasicaConservaLaTarjetaDeSiempre() {
        val textos = pintar(DisposicionLista.CLASICA).textos()
        assertTrue(textos.toString(), "14 oct" in textos)
        assertTrue(textos.toString(), "pasta · dos seguidas" in textos)
        assertTrue(textos.toString(), "sofrito · 45 min reales" in textos)
    }

    @Test
    fun repetirYBorrarLlamanASusCallbacksEnTodasLasDisposiciones() {
        for (d in DisposicionLista.entries) {
            val llamadas = mutableListOf<String>()
            val banco = pintar(d, repetir = { llamadas += "repetir" }, borrar = { llamadas += "borrar" })

            banco.pulsar(banco.pulsableConTexto("Repetir →"))
            banco.pulsar(banco.pulsableConTexto("Borrar"))

            assertEquals(d.toString(), listOf("repetir", "borrar"), llamadas)
        }
    }

    @Test
    fun sinRecetaNoHayRepetirYLasAccionesTienenDescripcion() {
        val banco = pintar(DisposicionLista.TINTE, fila.copy(recetaExiste = false))
        val textos = banco.textos()
        assertTrue(textos.toString(), "Repetir →" !in textos)
        assertTrue(
            banco.descripciones().toString(),
            "Borrar Macarrones al horno del historial" in banco.descripciones(),
        )
    }
}
