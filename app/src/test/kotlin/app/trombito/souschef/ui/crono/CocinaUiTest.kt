package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Hito
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.crono.ClaseEstado
import app.trombito.souschef.receta.crono.EstadoCarril
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.ui.crono.clasica.CronoClasico
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.DarkColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** La cocina en la JVM: reparto de FOCO y APILADA, sin scroll, toques y botón principal. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CocinaUiTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val ahora = 1_800_000_000_000L
    private val servir = ahora + 30 * 60_000L

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
                        meto = "Cebolla",
                        saco = "Nada",
                        nota = "Remueve",
                        hitos = listOf(Hito(150.0, "Dales la vuelta")),
                        p = "Porque sí",
                    ),
                    Paso("p2", "Reposo", "f0", 5.0, 10.0, f = 8),
                    Paso("p3", "Hervir", "f1", 0.0, 10.0, f = 9),
                    Paso("p4", "Cortar", "m", 0.0, 2.0),
                    Paso("p5", "Emplatar", "m", 2.0, 1.0, dep = listOf("p3")),
                ),
        )

    /**
     * Como [receta], pero con la mesa a los 10 min (no al 0): con otros carriles ya en marcha, un paso de `off` 0 sin
     * arrancar toca ya (ADR-0018: sigue al arranque de la cocina); uno con `off` 10 espera su hora.
     */
    private val recetaMesaEspera =
        receta.copy(
            pasos =
                receta.pasos.map {
                    when (it.id) {
                        "p4" -> it.copy(off = 10.0)
                        "p5" -> it.copy(off = 12.0)
                        else -> it
                    }
                },
        )

    private fun corriendo(quedanS: Int) = EstadoCarril(finAtMs = ahora + quedanS * 1000L)

    private fun vistas(
        f0: EstadoCarril = corriendo(200),
        f1: EstadoCarril = corriendo(400),
        m: EstadoCarril = EstadoCarril(),
        r: Receta = receta,
    ): List<VistaCarril> {
        val estado = EstadoCrono("r", 2, servir, mapOf("f0" to f0, "f1" to f1, "m" to m), actualizadoMs = ahora)
        return vistasCarriles(DatosCrono(r, estado, ahora))
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
                deshacer = { llamadas += "deshacer:$it" },
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
    fun `foco pinta una tarjeta completa y el resto en filas compactas`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas(r = recetaMesaEspera)
        banco.cocina { CronoFoco(v, elegirFoco(v, null), p.acciones, Modifier.fillMaxSize()) }

        assertEquals(listOf("tarjeta-f0"), banco.etiquetas("tarjeta-"))
        assertEquals(listOf("fila-f1", "fila-m"), banco.etiquetas("fila-"))
        assertFalse(banco.hayDesplazable())
        // El dato aparece una sola vez en los carriles: el título del paso en foco no se repite en ninguna fila.
        val enCarriles = banco.etiquetas("tarjeta-") + banco.etiquetas("fila-")
        assertEquals(1, enCarriles.sumOf { e -> banco.textos(banco.conEtiqueta(e)).count { it == "Sofrito" } })
    }

    @Test
    fun `apilada pinta una tarjeta por carril sin filas ni scroll`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas()
        banco.cocina { CronoApilado(v, p.acciones, Modifier.fillMaxSize()) }

        assertEquals(listOf("tarjeta-f0", "tarjeta-f1", "tarjeta-m"), banco.etiquetas("tarjeta-"))
        assertTrue(banco.etiquetas("fila-").isEmpty())
        assertFalse(banco.hayDesplazable())
    }

    @Test
    fun `apilada reparte la altura a partes iguales`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas()
        banco.cocina { CronoApilado(v, p.acciones, Modifier.fillMaxSize()) }

        val alturas = listOf("f0", "f1", "m").map { banco.conEtiqueta("tarjeta-$it").size.height }
        assertTrue(alturas.all { it > 0 })
        assertTrue(alturas.max() - alturas.min() <= 1)
    }

    @Test
    fun `apilada deja en fila compacta el carril terminado`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas(m = EstadoCarril(i = 1, fin = true))
        banco.cocina { CronoApilado(v, p.acciones, Modifier.fillMaxSize()) }

        assertEquals(listOf("tarjeta-f0", "tarjeta-f1"), banco.etiquetas("tarjeta-"))
        assertEquals(listOf("fila-m"), banco.etiquetas("fila-"))
    }

    @Test
    fun `tocar una fila compacta la fija en el foco`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas()
        banco.cocina { CronoFoco(v, "f0", p.acciones, Modifier.fillMaxSize()) }

        banco.pulsar(banco.conEtiqueta("fila-f1"))

        assertEquals(listOf("enfocar:f1"), p.llamadas)
    }

    @Test
    fun `el foco cambia de tarjeta cuando el fijado es otro carril vivo`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas()
        banco.cocina { CronoFoco(v, elegirFoco(v, "f1"), p.acciones, Modifier.fillMaxSize()) }

        assertEquals(listOf("tarjeta-f1"), banco.etiquetas("tarjeta-"))
        assertEquals(listOf("fila-f0", "fila-m"), banco.etiquetas("fila-"))
    }

    @Test
    fun `el boton principal ejecuta la accion del carril`() {
        val casos =
            listOf(
                Triple("corriendo", vistas(f0 = corriendo(200)), "hecho:f0"),
                Triple("vencido", vistas(f0 = corriendo(-5)), "hecho:f0"),
                Triple("pausado", vistas(f0 = EstadoCarril(pausado = true, restanteS = 100)), "alternar:f0"),
                Triple("sin empezar", vistas(f0 = EstadoCarril()), "alternar:f0"),
            )
        casos.forEach { (nombre, v, esperado) ->
            val banco = BancoCompose()
            val p = Pulsaciones()
            banco.cocina { CronoApilado(v, p.acciones, Modifier.fillMaxSize()) }

            banco.pulsar(banco.conEtiqueta("accion-f0"))

            assertEquals(nombre, listOf(esperado), p.llamadas)
        }
    }

    @Test
    fun `el boton principal dice la accion que ejecuta`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v =
            vistas(
                f0 = corriendo(200),
                f1 = EstadoCarril(pausado = true, restanteS = 100),
                m = EstadoCarril(),
                r = recetaMesaEspera,
            )
        banco.cocina { CronoApilado(v, p.acciones, Modifier.fillMaxSize()) }

        assertEquals(listOf("HECHO"), banco.textos(banco.conEtiqueta("accion-f0")))
        assertEquals(listOf("SEGUIR"), banco.textos(banco.conEtiqueta("accion-f1")))
        // «m» espera su hora (faltan unos 7 min): enlace «Empezar antes», no botón lleno.
        assertEquals(listOf("Empezar antes"), banco.textos(banco.conEtiqueta("accion-m")))
    }

    @Test
    fun `solo el carril que toca ya lleva el boton lleno EMPEZAR`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        // El servir está a 30 min y el plan dura 15: «m» (p4, off 0) toca a los 15 min. Con el servir a 15 min toca ya.
        val carriles = mapOf("f0" to corriendo(200), "f1" to corriendo(400), "m" to EstadoCarril())
        val estado = EstadoCrono("r", 2, ahora + 15 * 60_000L, carriles, actualizadoMs = ahora)
        val v = vistasCarriles(DatosCrono(receta, estado, ahora))
        assertEquals(ClaseEstado.YA, v.first { it.id == "m" }.clase)
        banco.cocina { CronoApilado(v, p.acciones, Modifier.fillMaxSize()) }

        assertEquals(listOf("EMPEZAR"), banco.textos(banco.conEtiqueta("accion-m")))
        banco.pulsar(banco.conEtiqueta("accion-m"))
        assertEquals(listOf("alternar:m"), p.llamadas)
    }

    @Test
    fun `el enlace Empezar antes empieza el carril que espera su hora`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas(m = EstadoCarril(), r = recetaMesaEspera)
        assertEquals(ClaseEstado.ESPERA, v.first { it.id == "m" }.clase)
        banco.cocina { CronoApilado(v, p.acciones, Modifier.fillMaxSize()) }

        banco.pulsar(banco.pulsableConTexto("Empezar antes"))

        assertEquals(listOf("alternar:m"), p.llamadas)
    }

    @Test
    fun `hecho es de contorno mientras corre y lleno al vencer`() {
        val corre = estiloAccion(vistas(f0 = corriendo(200)).first { it.id == "f0" })
        val vence = estiloAccion(vistas(f0 = corriendo(-5)).first { it.id == "f0" })
        assertEquals(EstiloAccion.CONTORNO, corre)
        assertEquals(EstiloAccion.LLENO, vence)
    }

    @Test
    fun `un carril que espera a otro dice a cual y ofrece empezar igual`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas(m = EstadoCarril(i = 1))
        assertEquals(AccionCarril.BLOQUEADO, v.first { it.id == "m" }.accion)
        banco.cocina { CronoApilado(v, p.acciones, Modifier.fillMaxSize()) }

        assertTrue(banco.textos(banco.conEtiqueta("tarjeta-m")).contains("espera a ②"))
        banco.pulsar(banco.pulsableConTexto("Empezar igual"))
        assertEquals(listOf("igual:m"), p.llamadas)
    }

    @Test
    fun `la tarjeta completa de un carril bloqueado no lleva boton lleno y dice Antes y a la vez que`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas(m = EstadoCarril(i = 1)).first { it.id == "m" }.copy(juntoN = 1)
        assertEquals(AccionCarril.BLOQUEADO, v.accion)
        banco.cocina { TarjetaCarril(v, p.acciones, 3, Modifier.height(420.dp)) }

        val texto = banco.conEtiqueta("accion-m")
        assertNull(texto.config.getOrNull(SemanticsActions.OnClick))
        assertEquals(listOf("Antes: Hervir (OLLA) · a la vez que ①"), banco.textos(texto))
        // TalkBack no lee «①» sin contexto: dice «el carril 1».
        assertEquals(
            listOf("Antes: Hervir (OLLA) · a la vez que el carril 1"),
            texto.config.getOrNull(SemanticsProperties.ContentDescription),
        )
    }

    @Test
    fun `la tarjeta completa de un carril que toca ya dice a la vez que`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas(m = EstadoCarril()).first { it.id == "m" }.copy(juntoN = 1)
        banco.cocina { TarjetaCarril(v, p.acciones, 3, Modifier.height(420.dp)) }

        assertTrue(banco.textos(banco.conEtiqueta("tarjeta-m")).contains("a la vez que ①"))
    }

    @Test
    fun `pausa y mas treinta son secundarios y actuan sobre su carril`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas()
        banco.cocina { CronoFoco(v, "f0", p.acciones, Modifier.fillMaxSize()) }
        val tarjeta = banco.conEtiqueta("tarjeta-f0")

        banco.pulsar(banco.pulsableConTexto("Pausa", tarjeta))
        banco.pulsar(banco.pulsableConTexto("+30 s", tarjeta))

        assertEquals(listOf("alternar:f0", "mas30:f0"), p.llamadas)
    }

    @Test
    fun `tocar el titulo abre el porque solo si lo hay`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas()
        banco.cocina { CronoFoco(v, "f0", p.acciones, Modifier.fillMaxSize()) }

        banco.pulsar(banco.pulsableConTexto("Sofrito", banco.conEtiqueta("tarjeta-f0")))

        assertEquals(listOf("porque:f0"), p.llamadas)
        val sinPorque = vistas().map { it.copy(tienePorque = false) }
        val otro = BancoCompose()
        otro.cocina { CronoFoco(sinPorque, "f0", p.acciones, Modifier.fillMaxSize()) }
        assertNull(
            otro.nodos().firstOrNull {
                it.config.contains(SemanticsActions.OnClick) && "Sofrito" in otro.textos(it)
            },
        )
    }

    @Test
    fun `la tarjeta en marcha muestra la senal y el siguiente hito una sola vez y no lo que entra o sale`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas()
        banco.cocina { CronoFoco(v, "f0", p.acciones, Modifier.fillMaxSize()) }

        val textos = banco.textos(banco.conEtiqueta("tarjeta-f0"))

        assertTrue(textos.any { it.startsWith("SEÑAL") && "Remueve" in it })
        assertTrue(textos.none { it.startsWith("METE") || it.startsWith("SACA") })
        assertEquals(1, textos.count { it.contains("Dales la vuelta") })
        assertEquals(1, textos.count { it == "Luego: Reposo · fuego 8 · 10 min" })
        assertEquals(1, textos.count { it == "tapada" })
    }

    @Test
    fun `clasica pinta pestanas, el panel del foco con scroll y sus mandos`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val estado =
            EstadoCrono(
                "r",
                2,
                servir,
                mapOf("f0" to corriendo(200), "f1" to corriendo(400), "m" to EstadoCarril()),
                foco = "f0",
                actualizadoMs = ahora,
            )
        banco.cocina {
            CronoClasico(
                DatosCrono(receta, estado, ahora),
                emptyMap(),
                p.acciones,
                {},
                Modifier.fillMaxSize(),
            )
        }

        banco.conEtiqueta("crono-clasico")
        assertTrue(banco.hayDesplazable())
        val textos = banco.textos(banco.raiz())
        assertTrue("SARTÉN" in textos && "OLLA" in textos && "MESA" in textos)
        banco.pulsar(banco.pulsableConTexto("+30 s"))
        assertEquals(listOf("mas30:f0"), p.llamadas)
    }

    @Test
    fun `el boton de deshacer solo sale con algo que deshacer y deshace su carril`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas()
        banco.cocina { CronoApilado(v, p.acciones, Modifier.fillMaxSize()) }
        assertEquals(emptyList<String>(), banco.etiquetas("deshacer-"))

        val conDeshacer = v.map { if (it.id == "f1") it.copy(deshacerA = "Lavar") else it }
        val banco2 = BancoCompose()
        banco2.cocina { CronoApilado(conDeshacer, p.acciones, Modifier.fillMaxSize()) }
        assertEquals(listOf("deshacer-f1"), banco2.etiquetas("deshacer-"))
        banco2.pulsar(banco2.conEtiqueta("deshacer-f1"))
        assertEquals(listOf("deshacer:f1"), p.llamadas)
    }
}
