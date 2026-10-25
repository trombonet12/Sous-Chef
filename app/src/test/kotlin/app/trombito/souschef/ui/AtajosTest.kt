package app.trombito.souschef.ui

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.material3.Text
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.trombito.souschef.MainActivity
import app.trombito.souschef.R
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.crono.LimpiezaCompose
import app.trombito.souschef.ui.novedades.esEntradaDirecta
import app.trombito.souschef.ui.shell.Pestana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

private const val NS_ANDROID = "http://schemas.android.com/apk/res/android"

/** Accesos directos del icono (F17): el extra `atajo`, a dónde lleva cada uno y que `shortcuts.xml` es coherente. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AtajosTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    // El extra y los destinos

    @Test
    fun `el extra atajo se lee del intent y uno desconocido o ausente no es ningun atajo`() {
        fun intent(valor: String?) = Intent().apply { if (valor != null) putExtra(EXTRA_ATAJO, valor) }

        assertEquals(Atajo.CENAS_RAPIDAS, atajoDe(intent("cenas_rapidas")))
        assertEquals(Atajo.QUE_ME_DA_TIEMPO, atajoDe(intent("que_me_da_tiempo")))
        assertEquals(Atajo.VOLVER_COCINA, atajoDe(intent("volver_cocina")))
        assertNull(atajoDe(intent("otra_cosa")))
        assertNull(atajoDe(intent(null)))
        assertNull(atajoDe(null))
    }

    @Test
    fun `cenas rapidas abre la lista de cenas rapidas y que me da tiempo abre Recetas con el modo`() {
        assertEquals(
            DestinoAtajo("recetas/familia/cenas-rapidas"),
            destinoDeAtajo(Atajo.CENAS_RAPIDAS, slugViva = "lasana"),
        )
        assertEquals(DestinoAtajo("recetas", modoTiempo = true), destinoDeAtajo(Atajo.QUE_ME_DA_TIEMPO, null))
    }

    @Test
    fun `volver a la cocina abre el cronometro de la viva y sin viva abre Recetas`() {
        assertEquals(DestinoAtajo("crono/lasana"), destinoDeAtajo(Atajo.VOLVER_COCINA, "lasana"))
        assertEquals(DestinoAtajo("recetas"), destinoDeAtajo(Atajo.VOLVER_COCINA, null))
    }

    @Test
    fun `un atajo es una entrada normal para las novedades, no una entrada directa`() {
        Atajo.entries.forEach { atajo ->
            val intent = Intent().putExtra(EXTRA_ATAJO, atajo.clave)
            assertFalse(atajo.name, esEntradaDirecta(intent))
        }
    }

    // shortcuts.xml y el manifest

    private class Atajado(
        val id: String,
        val clase: String?,
        val extra: String?,
        val accion: String?,
    )

    private fun leerShortcuts(): List<Atajado> {
        val xml = RuntimeEnvironment.getApplication().resources.getXml(R.xml.shortcuts)
        val etiquetas = mutableListOf<Pair<String, Map<String, String?>>>()
        while (xml.next() != XmlPullParser.END_DOCUMENT) {
            if (xml.eventType != XmlPullParser.START_TAG) continue
            val atributos = listOf("shortcutId", "targetClass", "action", "name", "value")
            etiquetas += xml.name to atributos.associateWith { xml.getAttributeValue(NS_ANDROID, it) }
        }
        return etiquetas.indices.filter { etiquetas[it].first == "shortcut" }.map { i ->
            val propias = etiquetas.drop(i + 1).takeWhile { it.first != "shortcut" }
            val intent = propias.first { it.first == "intent" }.second
            val extra = propias.firstOrNull { it.first == "extra" && it.second["name"] == EXTRA_ATAJO }?.second
            Atajado(
                id = etiquetas[i].second["shortcutId"].orEmpty(),
                clase = intent["targetClass"],
                extra = extra?.get("value"),
                accion = intent["action"],
            )
        }
    }

    @Test
    fun `shortcuts xml tiene los tres atajos con intent explicito a MainActivity y el extra de cada Atajo`() {
        val atajos = leerShortcuts()

        assertEquals(Atajo.entries.size, atajos.size)
        assertEquals(Atajo.entries.map { it.clave }.toSet(), atajos.mapNotNull { it.extra }.toSet())
        atajos.forEach {
            assertEquals(it.extra, it.id)
            assertEquals(MainActivity::class.java.name, it.clase)
            assertEquals(Intent.ACTION_VIEW, it.accion)
        }
    }

    @Test
    fun `MainActivity enlaza los atajos y ningun enlace externo la abre`() {
        val app = RuntimeEnvironment.getApplication()
        val nombre = ComponentName(app, MainActivity::class.java)
        val info = app.packageManager.getActivityInfo(nombre, PackageManager.GET_META_DATA)
        assertEquals(R.xml.shortcuts, info.metaData.getInt("android.app.shortcuts"))

        // Sin intent-filter público: ningún VIEW con BROWSABLE se resuelve en la app.
        val enlace =
            Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setPackage(app.packageName)
        assertTrue(app.packageManager.queryIntentActivities(enlace, 0).isEmpty())
    }

    // El enrutado, con navegación real

    private class Montaje(
        atajo: String?,
        viva: String?,
        inicio: String = Pestana.Recetas.ruta,
    ) {
        val banco = BancoCompose()
        var nav: NavHostController? = null
        var aplicados = 0

        init {
            banco.poner {
                val controlador = rememberNavController()
                nav = controlador
                NavHost(controlador, startDestination = inicio) {
                    composable(Pestana.Recetas.ruta) { Text("recetas") }
                    composable(
                        Rutas.RECETAS_FAMILIA,
                        arguments = listOf(navArgument(Rutas.ARG_FAMILIA) { type = NavType.StringType }),
                    ) { Text("familia") }
                    composable(
                        Rutas.CRONO,
                        arguments =
                            listOf(
                                navArgument(Rutas.ARG_SLUG) { type = NavType.StringType },
                                navArgument(Rutas.ARG_RACIONES) {
                                    type = NavType.IntType
                                    defaultValue = 0
                                },
                                navArgument(Rutas.ARG_SERVIR) {
                                    type = NavType.StringType
                                    defaultValue = ""
                                },
                            ),
                    ) { Text("crono") }
                    composable(Rutas.BIENVENIDA) { Text("bienvenida") }
                }
                AplicarAtajo(controlador, atajo, { viva }) { aplicados++ }
            }
            banco.reposo()
        }

        val ruta: String? get() = nav?.currentBackStackEntry?.destination?.route
        val debajo: String? get() = nav?.previousBackStackEntry?.destination?.route

        fun argumento(nombre: String): String? = nav?.currentBackStackEntry?.arguments?.getString(nombre)
    }

    @Test
    fun `cenas rapidas lleva a la lista de cenas rapidas con Recetas debajo`() {
        val m = Montaje("cenas_rapidas", viva = null)
        assertEquals(Rutas.RECETAS_FAMILIA, m.ruta)
        assertEquals("cenas-rapidas", m.argumento(Rutas.ARG_FAMILIA))
        assertEquals(Pestana.Recetas.ruta, m.debajo)
        assertEquals(1, m.aplicados)
    }

    @Test
    fun `que me da tiempo se queda en Recetas y deja la marca para encender el modo`() {
        val m = Montaje("que_me_da_tiempo", viva = null)
        assertEquals(Pestana.Recetas.ruta, m.ruta)
        val entrada = m.nav?.getBackStackEntry(Pestana.Recetas.ruta)
        assertEquals(true, entrada?.savedStateHandle?.get<Boolean>(CLAVE_ATAJO_TIEMPO))
        assertEquals(1, m.aplicados)
    }

    @Test
    fun `volver a la cocina con una viva abre su cronometro`() {
        val m = Montaje("volver_cocina", viva = "lasana")
        assertEquals(Rutas.CRONO, m.ruta)
        assertEquals("lasana", m.argumento(Rutas.ARG_SLUG))
        assertEquals(Pestana.Recetas.ruta, m.debajo)
        assertEquals(1, m.aplicados)
    }

    @Test
    fun `volver a la cocina sin ninguna viva se queda en Recetas`() {
        val m = Montaje("volver_cocina", viva = null)
        assertEquals(Pestana.Recetas.ruta, m.ruta)
        assertNull(m.debajo)
        assertEquals(1, m.aplicados)
    }

    @Test
    fun `sin atajo no navega ni consume nada y uno desconocido se descarta sin navegar`() {
        val sin = Montaje(null, viva = "lasana")
        assertEquals(Pestana.Recetas.ruta, sin.ruta)
        assertEquals(0, sin.aplicados)

        val raro = Montaje("otra_cosa", viva = "lasana")
        assertEquals(Pestana.Recetas.ruta, raro.ruta)
        assertEquals(1, raro.aplicados)
    }

    @Test
    fun `con la intro sin ver el atajo se descarta y se queda la intro`() {
        val m = Montaje("cenas_rapidas", viva = null, inicio = Rutas.BIENVENIDA)
        assertEquals(Rutas.BIENVENIDA, m.ruta)
        assertEquals(1, m.aplicados)
    }
}
