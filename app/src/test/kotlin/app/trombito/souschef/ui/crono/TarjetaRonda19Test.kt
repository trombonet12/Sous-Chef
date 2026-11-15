package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.DarkColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Ronda 19, tarjeta del cronómetro: la «i» pulsable (H2), la pausa visible (M1, L6), el aviso en cuenta atrás (M2),
 * el primer EMPEZAR (M12) y el aviso de gramos (M13).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class TarjetaRonda19Test {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val ahora = 1_800_000_000_000L
    private val servirLejos = ahora + 60 * 60_000L

    private val receta =
        Receta(
            slug = "r",
            titulo = "R",
            totalMin = 15.0,
            carriles =
                listOf(
                    Carril("f0", nombre = "SARTÉN", color = "#E4572E", tipo = "fuego"),
                    Carril("f1", nombre = "OLLA", color = "#3E8FD1", tipo = "fuego"),
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
                        meto = "el bol: 200 g de cebolla",
                        nota = "Remueve",
                        hitos = listOf(Hito(150.0, "Dales la vuelta")),
                        p = "Porque sí",
                    ),
                    Paso("p2", "Reposo", "f0", 5.0, 10.0, f = 8),
                    Paso("p3", "Hervir", "f1", 0.0, 10.0, f = 9, p = "Para que cueza"),
                ),
        )

    private fun corriendo(quedanS: Int) = EstadoCarril(finAtMs = ahora + quedanS * 1000L)

    private fun vistas(
        f0: EstadoCarril = EstadoCarril(),
        f1: EstadoCarril = EstadoCarril(),
        servir: Long = ahora + 15 * 60_000L,
    ): List<VistaCarril> {
        val estado = EstadoCrono("r", 2, servir, mapOf("f0" to f0, "f1" to f1), actualizadoMs = ahora)
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

    // ---- H2: la «i» es un botón en las tres tarjetas

    private fun comprobarBotonPorque(
        v: VistaCarril,
        alto: Int,
        areaCompleta: Boolean,
    ) {
        val banco = BancoCompose()
        val p = Pulsaciones()
        banco.cocina { TarjetaCarril(v, p.acciones, 2, Modifier.fillMaxWidth().height(alto.dp)) }

        val boton = banco.conEtiqueta("porque-${v.id}")
        assertNotNull("la «i» debe ser pulsable", boton.config.getOrNull(SemanticsActions.OnClick))
        assertEquals(listOf("Ver el porqué"), boton.config.getOrNull(SemanticsProperties.ContentDescription))
        // 48 dp a 3 px por dp (xxhdpi).
        assertTrue("ancho ${boton.size.width}", boton.size.width >= 144)
        if (areaCompleta) assertTrue("alto ${boton.size.height}", boton.size.height >= 144)
        banco.pulsar(boton)
        assertEquals(listOf("porque:${v.id}"), p.llamadas)
    }

    @Test
    fun `la i de la tarjeta completa es un boton de 48 dp que abre el porque`() {
        val v = vistas(f0 = corriendo(200)).first { it.id == "f0" }
        comprobarBotonPorque(v, alto = 460, areaCompleta = true)
    }

    @Test
    fun `la i de la tarjeta compacta es pulsable con su descripcion`() {
        val v = vistas(f0 = corriendo(200)).first { it.id == "f0" }
        comprobarBotonPorque(v, alto = 230, areaCompleta = false)
    }

    @Test
    fun `la i de la tarjeta por empezar es pulsable con su descripcion`() {
        val v = vistas().first { it.id == "f0" }
        comprobarBotonPorque(v, alto = 420, areaCompleta = false)
    }

    // ---- M1 y L6: pausa y vencido

    @Test
    fun `en pausa la tarjeta dice EN PAUSA y apaga el reloj`() {
        val banco = BancoCompose()
        val v = vistas(f0 = EstadoCarril(pausado = true, restanteS = 100)).first { it.id == "f0" }
        assertTrue(v.enPausa)
        banco.cocina { TarjetaCarril(v, Pulsaciones().acciones, 2, Modifier.height(460.dp)) }

        val textos = banco.textos(banco.conEtiqueta("tarjeta-f0"))
        assertTrue(textos.contains("EN PAUSA"))
        assertTrue(textos.contains("SEGUIR"))
        assertTrue(textos.none { it == "Pausa" })
    }

    @Test
    fun `en pausa la tarjeta compacta tambien lo dice y en marcha no`() {
        val enPausa = vistas(f0 = EstadoCarril(pausado = true, restanteS = 100)).first { it.id == "f0" }
        val enMarcha = vistas(f0 = corriendo(100)).first { it.id == "f0" }
        val banco = BancoCompose()
        banco.cocina { TarjetaCarril(enPausa, Pulsaciones().acciones, 4, Modifier.height(230.dp)) }
        assertTrue(banco.textos(banco.conEtiqueta("tarjeta-f0")).contains("EN PAUSA"))

        val otro = BancoCompose()
        otro.cocina { TarjetaCarril(enMarcha, Pulsaciones().acciones, 4, Modifier.height(230.dp)) }
        assertTrue(otro.textos(otro.conEtiqueta("tarjeta-f0")).none { it == "EN PAUSA" })
        assertTrue(!enMarcha.enPausa)
    }

    @Test
    fun `un paso vencido no ofrece Pausa y conserva mas 30 s`() {
        val banco = BancoCompose()
        val v = vistas(f0 = corriendo(-5)).first { it.id == "f0" }
        banco.cocina { TarjetaCarril(v, Pulsaciones().acciones, 2, Modifier.height(460.dp)) }

        val textos = banco.textos(banco.conEtiqueta("tarjeta-f0"))
        assertTrue(textos.none { it == "Pausa" })
        assertTrue(textos.contains("+30 s"))
        assertTrue(textos.contains("HECHO"))
    }

    // ---- M2: aviso en cuenta atrás

    @Test
    fun `el aviso cuenta hacia atras hasta que suene`() {
        // Paso de 300 s con un aviso a los 150 s: con 200 s por delante han pasado 100, faltan 50 para el aviso.
        val v = vistas(f0 = corriendo(200)).first { it.id == "f0" }
        val h = checkNotNull(v.proximoHito)
        assertEquals(50, segundosHastaAviso(v, h))
        assertEquals("0:50", cuentaAtras(50))
        assertEquals("3:07", cuentaAtras(187))
        assertEquals("12:00", cuentaAtras(720))
        val banco = BancoCompose()
        banco.cocina { TarjetaCarril(v, Pulsaciones().acciones, 2, Modifier.height(460.dp)) }
        assertTrue(banco.textos(banco.conEtiqueta("tarjeta-f0")).contains("en 0:50 · Dales la vuelta"))
    }

    @Test
    fun `el aviso no baja de cero ni cambia con el reloj en pausa`() {
        val tarde = vistas(f0 = corriendo(100)).first { it.id == "f0" }
        assertEquals(0, segundosHastaAviso(tarde, checkNotNull(tarde.proximoHito)))
        val pausa = vistas(f0 = EstadoCarril(pausado = true, restanteS = 200)).first { it.id == "f0" }
        assertEquals(50, segundosHastaAviso(pausa, checkNotNull(pausa.proximoHito)))
    }

    // ---- M12: el primero lleva EMPEZAR lleno

    @Test
    fun `sin nada arrancado el que toca primero lleva el boton lleno aunque falte mas de 3 minutos`() {
        val v = vistas(servir = servirLejos)
        assertTrue(v.all { it.clase == ClaseEstado.ESPERA })
        assertEquals("f0", primerCarrilSinArrancar(v))
        assertEquals(EstiloAccion.LLENO, estiloAccion(v.first { it.id == "f0" }, primeroSinArrancar = true))
        assertEquals(EstiloAccion.ENLACE, estiloAccion(v.first { it.id == "f1" }, primeroSinArrancar = false))
        assertEquals(EstiloAccion.ENLACE, estiloAccion(v.first { it.id == "f0" }))
    }

    @Test
    fun `la tarjeta del primer paso ensena EMPEZAR y las demas el enlace Empezar antes`() {
        val banco = BancoCompose()
        val v = vistas(servir = servirLejos)
        banco.cocina {
            CompositionLocalProvider(LocalPrimerArranque provides primerCarrilSinArrancar(v)) {
                CronoCarriles(v, Pulsaciones().acciones, Modifier.fillMaxSize())
            }
        }

        assertEquals(listOf("EMPEZAR"), banco.textos(banco.conEtiqueta("accion-f0")))
        assertEquals(listOf("Empezar antes"), banco.textos(banco.conEtiqueta("accion-f1")))
    }

    @Test
    fun `con algo empezado ya no hay primer paso`() {
        assertNull(primerCarrilSinArrancar(vistas(f0 = corriendo(200), servir = servirLejos)))
        assertNull(primerCarrilSinArrancar(vistas(f0 = EstadoCarril(pausado = true, restanteS = 90))))
        assertNull(primerCarrilSinArrancar(vistas(f0 = EstadoCarril(i = 1))))
    }

    // ---- M13: gramos de la receta base

    @Test
    fun `la cabecera avisa de que los gramos son de la receta base si las raciones cambian`() {
        val banco = BancoCompose()
        banco.cocina { LlegadaCabecera("15:30", Modifier.fillMaxWidth(), null, gramosPara = 2) }
        assertTrue(banco.textos(banco.conEtiqueta("llegada")).contains("Gramos para 2 raciones"))
    }

    @Test
    fun `sin cambio de raciones la cabecera no avisa`() {
        val banco = BancoCompose()
        banco.cocina { LlegadaCabecera("15:30", Modifier.fillMaxWidth(), null) }
        assertTrue(banco.textos(banco.conEtiqueta("llegada")).none { it.startsWith("Gramos") })
    }
}
