package app.trombito.souschef.data.resena

import android.app.Activity
import androidx.test.core.app.ApplicationProvider
import app.trombito.souschef.data.actualizacion.DecisionActualizacion
import app.trombito.souschef.domain.resena.ResultadoResena
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.testing.FakeReviewManager
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResenaPlayTest {
    private val actividad = Robolectric.buildActivity(Activity::class.java).get()

    @Test
    fun `instalada desde Play pide la resena`() =
        runTest {
            val manager = FakeReviewManager(ApplicationProvider.getApplicationContext())
            val resena = ResenaPlay(manager, { DecisionActualizacion.INSTALADOR_PLAY }, { actividad })
            assertEquals(ResultadoResena.PEDIDA, resena.lanzar())
        }

    @Test
    fun `sin Play no toca la API`() =
        runTest {
            val manager = mockk<ReviewManager>(relaxed = true)
            // Obtainium, debug o sin instalador: no hay a quién pedirle nada.
            listOf("dev.imranr.obtainium", null).forEach { instalador ->
                val resena = ResenaPlay(manager, { instalador }, { actividad })
                assertEquals(ResultadoResena.NO_DISPONIBLE, resena.lanzar())
            }
            verify(exactly = 0) { manager.requestReviewFlow() }
        }

    @Test
    fun `sin actividad visible falla sin lanzar nada`() =
        runTest {
            val manager = FakeReviewManager(ApplicationProvider.getApplicationContext())
            val resena = ResenaPlay(manager, { DecisionActualizacion.INSTALADOR_PLAY }, { null })
            assertEquals(ResultadoResena.FALLO, resena.lanzar())
        }
}
