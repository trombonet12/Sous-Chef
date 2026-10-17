package app.souschef.ui.crono

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.souschef.receta.Carril
import app.souschef.receta.Hito
import app.souschef.receta.Paso
import app.souschef.receta.Receta
import app.souschef.receta.crono.EstadoCarril
import app.souschef.receta.crono.EstadoCrono
import app.souschef.ui.theme.CronoPalette
import app.souschef.ui.theme.DarkColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** La cocina en la JVM: reparto de FOCO y APILADA, sin scroll, toques y botón principal. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CocinaUiTest {
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
        val v = vistas()
        banco.cocina { CronoFoco(v, elegirFoco(v, null), p.acciones, Modifier.fillMaxSize()) }

        assertEquals(listOf("tarjeta-f0"), banco.etiquetas("tarjeta-"))
        assertEquals(listOf("fila-f1", "fila-m"), banco.etiquetas("fila-"))
        assertFalse(banco.hayDesplazable())
        // El dato aparece una sola vez: el título del paso en foco no se repite en ninguna fila.
        assertEquals(1, banco.nodos().count { n -> banco.textos(n).let { "Sofrito" in it && n.children.isEmpty() } })
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
        val v = vistas(f0 = corriendo(200), f1 = EstadoCarril(pausado = true, restanteS = 100), m = EstadoCarril())
        banco.cocina { CronoApilado(v, p.acciones, Modifier.fillMaxSize()) }

        assertEquals(listOf("HECHO"), banco.textos(banco.conEtiqueta("accion-f0")))
        assertEquals(listOf("SEGUIR"), banco.textos(banco.conEtiqueta("accion-f1")))
        assertEquals(listOf("EMPEZAR"), banco.textos(banco.conEtiqueta("accion-m")))
    }

    @Test
    fun `un carril que espera a otro tiene el boton principal deshabilitado y empezar igual`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas(m = EstadoCarril(i = 1))
        assertEquals(AccionCarril.BLOQUEADO, v.first { it.id == "m" }.accion)
        banco.cocina { CronoApilado(v, p.acciones, Modifier.fillMaxSize()) }

        val boton = banco.conEtiqueta("accion-m")
        assertNotNull(boton.config.getOrNull(SemanticsProperties.Disabled))
        assertEquals(listOf("Antes: Hervir (OLLA)"), banco.textos(boton))

        banco.pulsar(banco.pulsableConTexto("Empezar igual"))
        assertEquals(listOf("igual:m"), p.llamadas)
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
    fun `la tarjeta muestra meto saco nota y el siguiente hito una sola vez`() {
        val banco = BancoCompose()
        val p = Pulsaciones()
        val v = vistas()
        banco.cocina { CronoFoco(v, "f0", p.acciones, Modifier.fillMaxSize()) }

        val textos = banco.textos(banco.conEtiqueta("tarjeta-f0"))

        assertTrue(textos.any { it.startsWith("METO") && "Cebolla" in it })
        assertTrue(textos.any { it.startsWith("SACO") })
        assertTrue(textos.any { it.contains("Remueve") })
        assertEquals(1, textos.count { it.contains("Dales la vuelta") })
        assertEquals(1, textos.count { it == "Después: Reposo · fuego 8" })
        assertEquals(1, textos.count { it == "tapada" })
    }

    // ---- reparto por altura

    @Test
    fun `con altura de sobra la tarjeta lo muestra todo`() {
        val plan = planTarjeta(alturaDp = 560f, carriles = 3, hayMetoSaco = true, hayNota = true)
        assertTrue(plan.nota)
        assertTrue(plan.metoSaco)
        assertEquals(2, plan.tituloLineas)
        assertEquals(88, plan.relojSp)
    }

    @Test
    fun `con tres carriles apilados se oculta la nota antes que meto y saco`() {
        val plan = planTarjeta(alturaDp = 380f, carriles = 3, hayMetoSaco = true, hayNota = true)
        assertFalse(plan.nota)
        assertTrue(plan.metoSaco)
    }

    @Test
    fun `con poca altura se ocultan nota y meto saco y el titulo queda en una linea`() {
        val plan = planTarjeta(alturaDp = 230f, carriles = 3, hayMetoSaco = true, hayNota = true)
        assertFalse(plan.nota)
        assertFalse(plan.metoSaco)
        assertEquals(1, plan.tituloLineas)
        assertTrue(plan.relojSp >= 36)
    }

    @Test
    fun `con cuatro carriles en un movil la tarjeta pasa a compacta`() {
        // ~150 dp por tarjeta: con la parte fija no queda sitio para el reloj.
        assertTrue(tarjetaCompacta(alturaDp = 150f, escalaLetra = 1f))
        assertFalse(tarjetaCompacta(alturaDp = 260f, escalaLetra = 1f))
    }

    @Test
    fun `con la letra del sistema grande se compacta antes`() {
        assertTrue(tarjetaCompacta(alturaDp = 260f, escalaLetra = 1.3f))
        assertFalse(tarjetaCompacta(alturaDp = 400f, escalaLetra = 1.3f))
    }

    @Test
    fun `la tarjeta compacta mete hito, despues y nota segun el alto`() {
        assertEquals(3, lineasExtraCompacta(alturaDp = 150f, escalaLetra = 1f))
        assertEquals(2, lineasExtraCompacta(alturaDp = 150f, escalaLetra = 1.3f))
        assertEquals(0, lineasExtraCompacta(alturaDp = 90f, escalaLetra = 1f))
        assertEquals(3, lineasExtraCompacta(alturaDp = 400f, escalaLetra = 1f))
    }

    @Test
    fun `con cuatro o mas carriles meto y saco nunca se muestran`() {
        val plan = planTarjeta(alturaDp = 560f, carriles = 4, hayMetoSaco = true, hayNota = true)
        assertFalse(plan.metoSaco)
    }
}
