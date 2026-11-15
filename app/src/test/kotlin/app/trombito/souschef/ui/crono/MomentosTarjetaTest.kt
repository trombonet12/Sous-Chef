package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Hito
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.crono.EstadoCarril
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.DarkColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** La tarjeta enseña lo que toca en cada momento (antes, en marcha, vencido) y la franja «Ahora / Luego» lo resume. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class MomentosTarjetaTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val ahora = 1_800_000_000_000L
    private val servir = ahora + 15 * 60_000L

    private val receta =
        Receta(
            slug = "r",
            titulo = "R",
            totalMin = 15.0,
            carriles =
                listOf(
                    Carril("f0", nombre = "SARTÉN", color = "#E4572E", tipo = "fuego"),
                    Carril("f1", nombre = "OLLA", color = "#3E8FD1", tipo = "fuego"),
                    Carril("m", nombre = "MESA", color = "#B0A899", tipo = "mesa"),
                ),
            pasos =
                listOf(
                    Paso(
                        "p1",
                        "Sofrito",
                        "f0",
                        0.0,
                        5.0,
                        f = 5,
                        tapa = "PUESTA",
                        meto = "el bol: 200 g de cebolla",
                        saco = "la cebolla, al plato",
                        nota = "Transparente, sin color",
                        hitos = listOf(Hito(150.0, "Dales la vuelta")),
                        p = "Porque sí",
                    ),
                    Paso("p2", "Reposo", "f0", 5.0, 10.0, f = 8),
                    Paso("p3", "Hervir", "f1", 0.0, 10.0, f = 9),
                    Paso("p4", "Cortar", "m", 0.0, 2.0),
                ),
        )

    private fun corriendo(quedanS: Int) = EstadoCarril(finAtMs = ahora + quedanS * 1000L)

    private fun vistas(
        f0: EstadoCarril = EstadoCarril(),
        f1: EstadoCarril = corriendo(400),
        m: EstadoCarril = corriendo(100),
    ): List<VistaCarril> {
        val estado = EstadoCrono("r", 2, servir, mapOf("f0" to f0, "f1" to f1, "m" to m), actualizadoMs = ahora)
        return vistasCarriles(DatosCrono(receta, estado, ahora))
    }

    private class Pulsaciones {
        val llamadas = mutableListOf<String>()

        val acciones =
            AccionesCarril(
                hecho = { llamadas += "hecho:$it" },
                alternar = { llamadas += "alternar:$it" },
                masTreinta = { llamadas += "mas30:$it" },
                empezarIgual = { llamadas += "igual:$it" },
                enfocar = { llamadas += "enfocar:$it" },
                porque = { llamadas += "porque:$it" },
            )
    }

    private fun BancoCompose.cocina(contenido: @Composable () -> Unit) =
        poner {
            MaterialTheme(colorScheme = DarkColors) {
                Surface(Modifier.fillMaxSize(), color = CronoPalette.fondo) {
                    Box(Modifier.fillMaxSize()) { contenido() }
                }
            }
        }

    @Test
    fun `antes de empezar la tarjeta dice METE y el fuego con tapa y duracion en vez del reloj`() {
        val banco = BancoCompose()
        val v = vistas()
        banco.cocina { CronoCarriles(v, Pulsaciones().acciones, Modifier.fillMaxSize()) }

        val textos = banco.textos(banco.conEtiqueta("tarjeta-f0"))

        assertTrue(textos.any { it.startsWith("METE") && "200 g de cebolla · el bol" in it })
        assertEquals(1, textos.count { it == "fuego 5 · tapada · 5 min" })
        assertTrue(textos.any { it.startsWith("SEÑAL") && "Transparente" in it })
        assertTrue(textos.none { it == "05:00" || it.startsWith("SACA") })
        assertTrue(textos.contains("EMPEZAR"))
        assertEquals(1, textos.count { it == "Luego: Reposo · fuego 8 · 10 min" })
    }

    @Test
    fun `en marcha la tarjeta enseña el reloj el proximo aviso y la senal y no METE`() {
        val banco = BancoCompose()
        val v = vistas(f0 = corriendo(200))
        banco.cocina { CronoCarriles(v, Pulsaciones().acciones, Modifier.fillMaxSize()) }

        val textos = banco.textos(banco.conEtiqueta("tarjeta-f0"))

        assertTrue(textos.contains("03:20"))
        assertEquals(1, textos.count { it.contains("Dales la vuelta") })
        assertTrue(textos.any { it.startsWith("SEÑAL") })
        assertTrue(textos.none { it.startsWith("METE") || it.startsWith("SACA") })
    }

    @Test
    fun `vencida la tarjeta dice SACA y Luego`() {
        val banco = BancoCompose()
        val v = vistas(f0 = corriendo(-5))
        banco.cocina { CronoCarriles(v, Pulsaciones().acciones, Modifier.fillMaxSize()) }

        val textos = banco.textos(banco.conEtiqueta("tarjeta-f0"))

        assertTrue(textos.any { it.startsWith("SACA") && "la cebolla" in it })
        assertTrue(textos.any { it.startsWith("Luego: Reposo") })
        assertTrue(textos.none { it.startsWith("METE") })
        assertTrue(textos.contains("HECHO"))
    }

    @Test
    fun `la franja dice AHORA con el carril que toca y su boton lo empieza si esta en fila`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas(f1 = EstadoCarril(), m = EstadoCarril())
        banco.cocina { FranjaAhora(v, p.acciones) }

        val franja = banco.textos(banco.conEtiqueta("franja-ahora"))

        assertTrue(franja.contains("AHORA"))
        assertTrue(franja.contains("Sofrito"))
        assertTrue(franja.contains("LUEGO"))
        assertTrue(franja.contains("EMPEZAR"))
        banco.pulsar(banco.conEtiqueta("franja-accion"))
        assertEquals(listOf("alternar:f0"), p.llamadas)
    }

    @Test
    fun `la franja de un carril vencido en fila ofrece HECHO`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas(f0 = corriendo(-5))
        banco.cocina { FranjaAhora(v, p.acciones) }

        assertTrue(banco.textos(banco.conEtiqueta("franja-ahora")).contains("HECHO"))
        banco.pulsar(banco.conEtiqueta("franja-accion"))
        assertEquals(listOf("hecho:f0"), p.llamadas)
    }

    @Test
    fun `la franja no repite el boton si el carril que toca ya esta expandido`() {
        val banco = BancoCompose()
        val v = vistas(f1 = EstadoCarril(), m = EstadoCarril())
        banco.cocina { FranjaAhora(v, Pulsaciones().acciones, enTarjeta = setOf("f0")) }

        val franja = banco.textos(banco.conEtiqueta("franja-ahora"))

        assertTrue(franja.contains("Sofrito"))
        assertTrue(franja.none { it == "EMPEZAR" })
        assertTrue(banco.etiquetas("franja-accion").isEmpty())
    }

    @Test
    fun `en carriles el boton EMPEZAR del carril que toca sale una sola vez en su tarjeta`() {
        val banco = BancoCompose()
        val v = vistas(f1 = EstadoCarril(), m = EstadoCarril())
        banco.cocina { CronoCarriles(v, Pulsaciones().acciones, Modifier.fillMaxSize()) }

        assertTrue("tarjeta-f0" in banco.etiquetas("tarjeta-"))
        assertTrue(banco.etiquetas("franja-accion").isEmpty())
        assertEquals(1, banco.textos(banco.conEtiqueta("tarjeta-f0")).count { it == "EMPEZAR" })
        assertTrue(banco.textos(banco.conEtiqueta("franja-ahora")).none { it == "EMPEZAR" })
    }

    @Test
    fun `con sitio para una tarjeta el vencido mas antiguo la lleva y la franja no repite su HECHO`() {
        val banco = BancoCompose()
        val v = vistas(f0 = corriendo(-5), f1 = corriendo(-60), m = EstadoCarril())
        banco.cocina { CronoCarriles(v, Pulsaciones().acciones, Modifier.fillMaxWidth().height(300.dp)) }

        assertEquals(listOf("tarjeta-f1"), banco.etiquetas("tarjeta-"))
        assertTrue(banco.etiquetas("franja-accion").isEmpty())
        assertTrue(banco.textos(banco.conEtiqueta("franja-ahora")).contains("Hervir"))
    }

    @Test
    fun `sin carriles vivos no hay franja`() {
        val banco = BancoCompose()
        val fin = EstadoCarril(i = 2, fin = true)
        val v = vistas(f0 = fin, f1 = fin, m = fin)
        banco.cocina { CronoCarriles(v, Pulsaciones().acciones, Modifier.fillMaxSize()) }

        assertTrue(banco.etiquetas("franja-").isEmpty())
    }
}
