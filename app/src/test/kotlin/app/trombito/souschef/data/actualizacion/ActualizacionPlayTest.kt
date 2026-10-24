package app.trombito.souschef.data.actualizacion

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.test.core.app.ApplicationProvider
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActualizacionPlayTest {
    // Espía sobre el falso de Play para contar cuántas veces se lanza la pantalla de actualización.
    private val manager = spyk(FakeAppUpdateManager(ApplicationProvider.getApplicationContext()))
    private val lanzador = mockk<ActivityResultLauncher<IntentSenderRequest>>(relaxed = true)

    private fun actualizacion(
        instalador: String? = DecisionActualizacion.INSTALADOR_PLAY,
        viva: Boolean = false,
    ) = ActualizacionPlay(manager, { instalador }, { viva })

    private fun hayActualizacion(prioridad: Int) {
        manager.setUpdateAvailable(2)
        manager.setUpdatePriority(prioridad)
    }

    private fun lanzamientos(n: Int) =
        verify(exactly = n) {
            manager.startUpdateFlowForResult(any(), any<ActivityResultLauncher<IntentSenderRequest>>(), any())
        }

    /** Deja una actualización inmediata a medias: Play ya está descargando y la app se reabre. */
    private fun actualizacionAMedias() {
        hayActualizacion(5)
        manager.startUpdateFlowForResult(
            manager.appUpdateInfo.result,
            lanzador,
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
        )
        manager.userAcceptsUpdate()
        manager.downloadStarts()
        assertEquals(
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
            manager.appUpdateInfo.result.updateAvailability(),
        )
        clearMocks(manager, answers = false)
    }

    @Test
    fun lanzaElFlujoInmediatoConPrioridad5() =
        runTest {
            hayActualizacion(5)
            actualizacion().comprobar(lanzador)
            lanzamientos(1)
            assertTrue(manager.isImmediateFlowVisible)
        }

    @Test
    fun noLanzaConPrioridad3() =
        runTest {
            hayActualizacion(3)
            actualizacion().comprobar(lanzador)
            lanzamientos(0)
        }

    @Test
    fun noLanzaConCocinadaViva() =
        runTest {
            hayActualizacion(5)
            actualizacion(viva = true).comprobar(lanzador)
            lanzamientos(0)
        }

    @Test
    fun noLanzaSiElInstaladorNoEsPlay() =
        runTest {
            hayActualizacion(5)
            actualizacion(instalador = "org.example.obtainium").comprobar(lanzador)
            actualizacion(instalador = null).comprobar(lanzador)
            lanzamientos(0)
        }

    @Test
    fun reanudaLaActualizacionInmediataAMedias() =
        runTest {
            actualizacionAMedias()
            actualizacion().comprobar(lanzador)
            lanzamientos(1)
        }

    @Test
    fun noLanzaSiLaCocinadaArrancaMientrasSeConsultaAPlay() =
        runTest {
            hayActualizacion(5)
            // Primera comprobación libre; la cocinada arranca durante requestAppUpdateInfo.
            var llamadas = 0
            val actualizacion =
                ActualizacionPlay(
                    manager,
                    { DecisionActualizacion.INSTALADOR_PLAY },
                    {
                        llamadas++
                        llamadas >= 2
                    },
                )
            actualizacion.comprobar(lanzador)
            assertEquals(2, llamadas)
            lanzamientos(0)
        }

    @Test
    fun noReanudaConCocinadaViva() =
        runTest {
            actualizacionAMedias()
            actualizacion(viva = true).comprobar(lanzador)
            lanzamientos(0)
        }

    @Test
    fun decisionPuraSinActualizacionONiInmediato() {
        val p = DecisionActualizacion
        assertFalse(p.lanzar(p.INSTALADOR_PLAY, false, UpdateAvailability.UPDATE_NOT_AVAILABLE, 5, true))
        assertFalse(p.lanzar(p.INSTALADOR_PLAY, false, UpdateAvailability.UPDATE_AVAILABLE, 5, false))
        assertTrue(p.lanzar(p.INSTALADOR_PLAY, false, UpdateAvailability.UPDATE_AVAILABLE, 4, true))
    }
}
