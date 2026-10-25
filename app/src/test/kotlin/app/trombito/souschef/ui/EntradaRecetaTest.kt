package app.trombito.souschef.ui

import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.navigation.NavController
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** La puerta de las rutas de receta (ficha y cronómetro) y el saneado del intent que llega de fuera. */
@RunWith(RobolectricTestRunner::class)
class EntradaRecetaTest {
    private val puerta = EntradaRecetaViewModel(FakeRecetasRepo(listOf(receta("curry"))))

    @Test
    fun admiteSoloUnSlugValidoDeUnaRecetaQueExiste() =
        runTest {
            assertTrue(puerta.admite("curry"))
            assertFalse(puerta.admite("no-existe"))
            assertFalse(puerta.admite("../curry"))
            assertFalse(puerta.admite("CURRY"))
            assertFalse(puerta.admite(""))
        }

    @Test
    fun soloLaEntradaPorEnlacePasaPorLaPuerta() {
        assertFalse(llegadaPorEnlace(null))
        assertFalse(llegadaPorEnlace(Bundle().apply { putString(Rutas.ARG_SLUG, "curry") }))
        val porEnlace = Bundle().apply { putParcelable(NavController.KEY_DEEP_LINK_INTENT, Intent()) }
        assertTrue(llegadaPorEnlace(porEnlace))
    }

    @Test
    fun sanearQuitaLosExtrasDeNavegacion() {
        val intent =
            Intent(Intent.ACTION_VIEW, "souschef://ficha/curry".toUri())
                .putExtra(NavController.KEY_DEEP_LINK_IDS, intArrayOf(1, 2))
                .putExtra(NavController.KEY_DEEP_LINK_ARGS, arrayOf(Bundle()))
                .putExtra(NavController.KEY_DEEP_LINK_EXTRAS, Bundle())
                .putExtra(NavController.KEY_DEEP_LINK_INTENT, Intent())

        val saneado = sanearIntent(intent)

        assertSame(intent, saneado)
        assertFalse(saneado.hasExtra(NavController.KEY_DEEP_LINK_IDS))
        assertFalse(saneado.hasExtra(NavController.KEY_DEEP_LINK_ARGS))
        assertFalse(saneado.hasExtra(NavController.KEY_DEEP_LINK_EXTRAS))
        assertFalse(saneado.hasExtra(NavController.KEY_DEEP_LINK_INTENT))
        assertEquals("souschef://ficha/curry", saneado.data.toString())
    }

    @Test
    fun sanearDejaPasarSoloLosEnlacesPropios() {
        listOf("souschef://crono/curry", "souschef://ficha/curry").forEach {
            assertEquals(it, sanearIntent(Intent(Intent.ACTION_VIEW, it.toUri())).data.toString())
        }
        listOf(
            "souschef://ajustes/curry",
            "souschef://buzon",
            "https://ficha/curry",
            "otra://crono/curry",
            "content://crono/curry",
        ).forEach { assertNull("«$it» no pasa", sanearIntent(Intent(Intent.ACTION_VIEW, it.toUri())).data) }
    }

    @Test
    fun sanearConservaElAtajoYUnIntentSinData() {
        val atajo = Intent().putExtra(EXTRA_ATAJO, Atajo.VOLVER_COCINA.clave)
        assertEquals(Atajo.VOLVER_COCINA, atajoDe(sanearIntent(atajo)))
        assertNull(sanearIntent(atajo).data)
    }

    @Test
    fun sanearConservaElAtajoAunqueVengaConLosExtrasAjenos() {
        val intent =
            Intent(Intent.ACTION_VIEW, "https://evil.example/".toUri())
                .putExtra(EXTRA_ATAJO, Atajo.CENAS_RAPIDAS.clave)
                .putExtra(NavController.KEY_DEEP_LINK_IDS, intArrayOf(7))
        val saneado = sanearIntent(intent)
        assertEquals(Atajo.CENAS_RAPIDAS, atajoDe(saneado))
        assertNull(saneado.data)
        assertFalse(saneado.hasExtra(NavController.KEY_DEEP_LINK_IDS))
    }
}
