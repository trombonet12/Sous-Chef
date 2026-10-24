package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.crono.EstadoCarril
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.DarkColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Accesibilidad del cronómetro: los mandos dicen de qué carril son y el paso vencido se dice, no solo se pinta. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AccesibilidadCronoTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val ahora = 1_800_000_000_000L
    private val servir = ahora + 30 * 60_000L

    private val receta =
        Receta(
            slug = "r",
            titulo = "R",
            totalMin = 15.0,
            carriles = listOf(Carril("f0", nombre = "SARTÉN", color = "#E4572E", tipo = "fuego")),
            pasos = listOf(Paso("p1", "Sofrito", "f0", 0.0, 5.0, f = 5), Paso("p2", "Reposo", "f0", 5.0, 10.0, f = 8)),
        )

    private fun vista(quedanS: Int): VistaCarril {
        val estado =
            EstadoCrono(
                "r",
                2,
                servir,
                mapOf("f0" to EstadoCarril(finAtMs = ahora + quedanS * 1000L)),
                actualizadoMs = ahora,
            )
        return vistasCarriles(DatosCrono(receta, estado, ahora)).single()
    }

    private fun BancoCompose.tarjeta(v: VistaCarril) =
        poner {
            MaterialTheme(colorScheme = DarkColors) {
                Surface(Modifier.fillMaxSize(), color = CronoPalette.fondo) {
                    Box(Modifier.fillMaxSize()) { TarjetaCarril(v, AccionesCarril.INERTES, carriles = 1) }
                }
            }
        }

    private fun BancoCompose.descripcion(etiqueta: String) =
        conEtiqueta(etiqueta).config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()

    @Test
    fun `el boton Hecho dice el carril y el paso`() {
        val banco = BancoCompose()
        banco.tarjeta(vista(quedanS = 200))

        assertEquals("Hecho: SARTÉN, Sofrito", banco.descripcion("accion-f0"))
    }

    @Test
    fun `un paso vencido dice Vencido y se anuncia`() {
        val banco = BancoCompose()
        banco.tarjeta(vista(quedanS = -5))

        val config = banco.conEtiqueta("tarjeta-f0").config
        assertEquals("Vencido", config.getOrNull(SemanticsProperties.StateDescription))
        assertEquals(LiveRegionMode.Assertive, config.getOrNull(SemanticsProperties.LiveRegion))
    }

    @Test
    fun `un paso en marcha no se anuncia como vencido`() {
        val banco = BancoCompose()
        banco.tarjeta(vista(quedanS = 200))

        val config = banco.conEtiqueta("tarjeta-f0").config
        assertNull(config.getOrNull(SemanticsProperties.StateDescription))
        assertNull(config.getOrNull(SemanticsProperties.LiveRegion))
    }
}
