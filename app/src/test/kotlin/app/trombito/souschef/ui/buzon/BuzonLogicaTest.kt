package app.trombito.souschef.ui.buzon

import android.content.ClipboardManager
import android.content.Intent
import androidx.activity.ComponentActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class BuzonLogicaTest {
    private val datos =
        DatosApp(version = "1.4.0", modelo = "Pixel 9", androidRelease = "16", sdk = 36, cocina = "Vitro digital 1-9")

    private fun texto(id: Int): String = RuntimeEnvironment.getApplication().getString(id)

    private fun correo(f: FormularioBuzon) = construirCorreo(f, datos, ::texto)

    @Test
    fun elAsuntoLlevaLaMarcaYElTipo() {
        assertEquals("[Sous Chef] Pedir una receta", correo(FormularioBuzon(TipoBuzon.RECETA)).asunto)
        assertEquals("[Sous Chef] Contar un fallo", correo(FormularioBuzon(TipoBuzon.FALLO)).asunto)
        assertEquals("[Sous Chef] Sugerencia", correo(FormularioBuzon(TipoBuzon.SUGERENCIA)).asunto)
    }

    @Test
    fun laRecetaPedidaLlevaSusCamposYElBloqueDeDatos() {
        val f =
            FormularioBuzon(
                tipo = TipoBuzon.RECETA,
                plato = "Albóndigas",
                ingredientes = "Usar: ternera. Evitar: nata",
                tiempo = TiempoMaximo.DE_45_A_90,
                raciones = "4",
                queOcurrio = "no debe salir",
            )
        val esperado =
            listOf(
                "Plato: Albóndigas",
                "Ingredientes (usar o evitar): Usar: ternera. Evitar: nata",
                "Tiempo máximo: 45–90 min",
                "Raciones: 4",
                "",
                "— Datos —",
                "Versión de la app: 1.4.0",
                "Dispositivo: Pixel 9",
                "Android: 16 (SDK 36)",
                "Cocina: Vitro digital 1-9",
            ).joinToString("\n")
        assertEquals(esperado, correo(f).cuerpo)
    }

    @Test
    fun losCamposVaciosSeOmiten() {
        val cuerpo = correo(FormularioBuzon(TipoBuzon.RECETA, plato = "  Tortilla  ")).cuerpo
        assertTrue(cuerpo, cuerpo.startsWith("Plato: Tortilla\n\n— Datos —"))
        assertFalse(cuerpo.contains("Ingredientes"))
        assertFalse(cuerpo.contains("Raciones"))
    }

    @Test
    fun elFalloLlevaQueOcurrioQueHaciasYLaPantalla() {
        val f =
            FormularioBuzon(
                tipo = TipoBuzon.FALLO,
                queOcurrio = "Se cierra\nal abrir",
                queHacias = "Empezar la lasaña",
                pantalla = SeccionApp.CRONOMETRO,
                plato = "no debe salir",
            )
        val cuerpo = correo(f).cuerpo
        val inicio = "Qué pasó:\nSe cierra\nal abrir\nQué estaba haciendo: Empezar la lasaña\nPantalla: Cronómetro\n"
        assertTrue(cuerpo, cuerpo.startsWith(inicio))
        assertFalse(cuerpo.contains("Plato"))
    }

    @Test
    fun laSugerenciaLlevaElTextoLibre() {
        val cuerpo = correo(FormularioBuzon(TipoBuzon.SUGERENCIA, texto = "Un modo oscuro", plato = "x")).cuerpo
        assertTrue(cuerpo, cuerpo.startsWith("Sugerencia: Un modo oscuro\n\n— Datos —"))
    }

    @Test
    fun soloSePuedeEnviarConElCampoPrincipalDelTipoRelleno() {
        assertFalse(FormularioBuzon(TipoBuzon.RECETA, queOcurrio = "x").valido)
        assertTrue(FormularioBuzon(TipoBuzon.RECETA, plato = "x").valido)
        assertFalse(FormularioBuzon(TipoBuzon.FALLO, plato = "x", queOcurrio = "  ").valido)
        assertTrue(FormularioBuzon(TipoBuzon.FALLO, queOcurrio = "x").valido)
        assertFalse(FormularioBuzon(TipoBuzon.SUGERENCIA).valido)
        assertTrue(FormularioBuzon(TipoBuzon.SUGERENCIA, texto = "x").valido)
    }

    @Test
    fun elIntentEsUnSendtoMailtoConAsuntoYCuerpo() {
        val c = correo(FormularioBuzon(TipoBuzon.SUGERENCIA, texto = "Hola"))
        val intent = intentCorreo(c)
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto:trombonet12@trombonet12.es", intent.data.toString())
        assertEquals("[Sous Chef] Sugerencia", intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals(c.cuerpo, intent.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals(listOf("trombonet12@trombonet12.es"), intent.getStringArrayExtra(Intent.EXTRA_EMAIL)?.toList())
    }

    @Test
    fun enviarLanzaElIntentYCopiarPoneElTextoEnElPortapapeles() {
        // Como en la app, el contexto es el de una actividad.
        val contexto = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val c = correo(FormularioBuzon(TipoBuzon.SUGERENCIA, texto = "Hola"))

        assertTrue(enviarCorreo(contexto, c))
        assertEquals(Intent.ACTION_SENDTO, shadowOf(contexto).nextStartedActivity.action)

        copiarCorreo(contexto, c)
        val gestor = contexto.getSystemService(ClipboardManager::class.java)
        assertEquals(
            c.textoPlano,
            gestor.primaryClip
                ?.getItemAt(0)
                ?.text
                .toString(),
        )
    }
}
