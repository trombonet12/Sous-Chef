package app.trombito.souschef.ui.buzon

import android.content.ClipboardManager
import android.content.Intent
import androidx.activity.ComponentActivity
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.ResumenUso
import app.trombito.souschef.domain.Valoracion
import app.trombito.souschef.domain.resumenDeUso
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.TipoCocina
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class BuzonLogicaTest {
    private val datos =
        DatosApp(version = "1.4.0", modelo = "Pixel 9", androidRelease = "16", sdk = 36, cocina = "Vitro digital 1-9")

    private fun texto(id: Int): String = RuntimeEnvironment.getApplication().getString(id)

    private fun correo(f: FormularioBuzon) = construirCorreo(f, datos, ::texto)

    private val resumen =
        ResumenUso(
            version = "1.7.0",
            cocinadas = 12,
            cocinadasUltimoMes = 4,
            recetasDistintas = 7,
            repetir = 5,
            ajustar = 2,
            noRepetir = 1,
            disposicion = DisposicionCrono.APILADA,
            cocina = Cocina(),
            vozActiva = true,
            quieroHacerla = 3,
        )

    private val textoEsperadoDelResumen =
        listOf(
            "— Resumen de uso —",
            "Versión: 1.7.0",
            "Cocinadas: 12 (4 en el último mes)",
            "Recetas distintas: 7",
            "Valoradas: Repetir 5 · Ajustar 2 · No repetir 1",
            "Cronómetro: Apilada",
            "Cocina: Vitro digital 1-9",
            "Voz: activada",
            "Quiero hacerla: 3",
        ).joinToString("\n")

    @Test
    fun elResumenDeUsoSeEscribeConSusCifras() {
        assertEquals(textoEsperadoDelResumen, textoResumen(resumen, ::texto))
    }

    @Test
    fun elResumenReflejaLaCocinaDeGasLaVozApagadaYLaDisposicionClasica() {
        val otro =
            resumen.copy(
                disposicion = DisposicionCrono.CLASICA,
                cocina = Cocina(tipo = TipoCocina.GAS, mando = Mando.ANALOGICO, max = 8),
                vozActiva = false,
            )
        val t = textoResumen(otro, ::texto)
        assertTrue(t, t.contains("Cronómetro: Clásica"))
        assertTrue(t, t.contains("Cocina: Gas · ruleta 1-8"))
        assertTrue(t, t.contains("Voz: desactivada"))
        val sinNumeros = textoResumen(resumen.copy(cocina = Cocina(TipoCocina.GAS, Mando.ANALOGICO, null)), ::texto)
        assertTrue(sinNumeros, sinNumeros.contains("Cocina: Gas · sin números"))
    }

    @Test
    fun elResumenSoloViajaSiSeMarcaLaCasillaYEsElMismoTextoDeLaVistaPrevia() {
        val conResumen = datos.copy(resumen = resumen)
        val f = FormularioBuzon(TipoBuzon.SUGERENCIA, texto = "Hola")

        val sin = construirCorreo(f, conResumen, ::texto).cuerpo
        assertFalse(sin, sin.contains("Resumen de uso"))

        val con = construirCorreo(f.copy(adjuntarResumen = true), conResumen, ::texto).cuerpo
        assertTrue(con, con.endsWith("Cocina: Vitro digital 1-9\n\n$textoEsperadoDelResumen"))

        // Marcada pero sin cifras todavía: no se añade nada.
        assertEquals(sin, construirCorreo(f.copy(adjuntarResumen = true), datos, ::texto).cuerpo)
    }

    @Test
    fun elCorreoConResumenNoLlevaNotasNiTitulosNiSlugs() {
        // El resumen se calcula desde un historial con datos privados; nada de eso puede salir en el correo.
        val entradas =
            listOf(
                EntradaHistorial(
                    id = "lasana-secreta-2026-10-20",
                    slug = "lasana-secreta",
                    titulo = "Lasaña de la suegra",
                    fecha = "2026-10-20",
                    nota = "NOTA-PRIVADA: le faltó sal",
                    valoracion = Valoracion.AJUSTAR,
                ),
            )
        val calculado = resumenDeUso(entradas, 1, Ajustes(), "1.7.0", LocalDate.of(2026, 10, 25))
        val f = FormularioBuzon(TipoBuzon.FALLO, queOcurrio = "Se cierra", adjuntarResumen = true)
        val cuerpo = construirCorreo(f, datos.copy(resumen = calculado), ::texto).cuerpo

        assertTrue(cuerpo, cuerpo.contains("Cocinadas: 1 (1 en el último mes)"))
        assertTrue(cuerpo, cuerpo.contains("Valoradas: Repetir 0 · Ajustar 1 · No repetir 0"))
        listOf("NOTA-PRIVADA", "le faltó sal", "lasana-secreta", "Lasaña de la suegra", "2026-10-20").forEach {
            assertFalse("$it no debe salir: $cuerpo", cuerpo.contains(it))
        }
    }

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
