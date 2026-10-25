package app.trombito.souschef.domain.crono

import app.trombito.souschef.receta.crono.MotorCrono
import app.trombito.souschef.receta.crono.SesionCrono
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.crono.FakeAlarmaRecordatorio
import app.trombito.souschef.ui.crono.FakeAlarmaSistema
import app.trombito.souschef.ui.crono.FakeCronoEstadoRepo
import app.trombito.souschef.ui.crono.FakeNotificador
import app.trombito.souschef.ui.crono.FakeNotificadorRecordatorio
import app.trombito.souschef.ui.crono.FakeRecordatorioRepo
import app.trombito.souschef.ui.crono.RelojFalso
import app.trombito.souschef.ui.crono.T0
import app.trombito.souschef.ui.crono.recetaCrono
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class GestorRecordatorioTest {
    private val zona = ZoneId.systemDefault()
    private val reloj = RelojFalso()
    private val repo = FakeRecordatorioRepo()
    private val alarma = FakeAlarmaRecordatorio()
    private val aviso = FakeNotificadorRecordatorio()
    private val recetas = FakeRecetasRepo(listOf(recetaCrono("a"), recetaCrono("b")))
    private val gestor = GestorRecordatorio(repo, recetas, reloj, alarma, aviso) { zona }

    /** T0 es hoy a las 12:00: las 14:00 son T0 + 2 h. */
    private val dosHoras = T0 + 2 * 60 * 60_000L
    private val servirMin = 15 * 60 + 30

    @Test
    fun armarGuardaElRecordatorioYProgramaLaAlarmaALaHora() =
        runTest {
            assertTrue(gestor.armar("a", 14 * 60, servirMin))
            assertEquals(dosHoras, alarma.programada)
            assertEquals(Recordatorio("a", dosHoras, dosHoras + 90 * 60_000L), repo.dato.value)
        }

    @Test
    fun armarOtroSustituyeAlAnteriorUnoALaVez() =
        runTest {
            gestor.armar("a", 14 * 60, servirMin)
            gestor.armar("b", 15 * 60, servirMin)
            assertEquals("b", repo.dato.value?.slug)
            assertEquals(T0 + 3 * 60 * 60_000L, alarma.programada)
        }

    @Test
    fun siLaHoraYaPasoNoArmaNiTocaLoQueHabia() =
        runTest {
            gestor.armar("a", 14 * 60, servirMin)
            assertFalse(gestor.armar("b", 9 * 60, servirMin))
            assertEquals("a", repo.dato.value?.slug)
            assertEquals(dosHoras, alarma.programada)
        }

    @Test
    fun quitarBorraElRecordatorioYCancelaLaAlarma() =
        runTest {
            gestor.armar("a", 14 * 60, servirMin)
            gestor.quitar()
            assertNull(repo.dato.value)
            assertNull(alarma.programada)
        }

    @Test
    fun procesarAntesDeLaHoraVuelveAArmar() =
        runTest {
            repo.dato.value = Recordatorio("a", dosHoras, dosHoras + 90 * 60_000L)
            gestor.procesar()
            assertEquals(dosHoras, alarma.programada)
            assertTrue(aviso.avisos.isEmpty())
            assertEquals("a", repo.dato.value?.slug)
        }

    @Test
    fun alSonarLaAlarmaAvisaYBorra() =
        runTest {
            val servirMs = dosHoras + 90 * 60_000L
            repo.dato.value = Recordatorio("a", dosHoras, servirMs)
            reloj.ms = dosHoras + 3_000L
            gestor.procesar()
            assertEquals(listOf(Triple("a", recetaCrono("a").titulo, servirMs)), aviso.avisos)
            assertNull(repo.dato.value)
        }

    @Test
    fun trasUnReinicioConLaHoraPasadaHaceDiezMinutosOMenosAvisa() =
        runTest {
            repo.dato.value = Recordatorio("a", dosHoras, dosHoras + 90 * 60_000L)
            reloj.ms = dosHoras + 10 * 60_000L
            gestor.procesar()
            assertEquals(1, aviso.avisos.size)
            assertNull(repo.dato.value)
        }

    @Test
    fun trasUnReinicioConLaHoraPasadaHaceMasDeDiezMinutosSeBorraSinAvisar() =
        runTest {
            repo.dato.value = Recordatorio("a", dosHoras, dosHoras + 90 * 60_000L)
            reloj.ms = dosHoras + 10 * 60_000L + 1
            gestor.procesar()
            assertTrue(aviso.avisos.isEmpty())
            assertNull(repo.dato.value)
            assertNull(alarma.programada)
        }

    @Test
    fun siLaRecetaYaNoEstaEnElCatalogoSeBorraSinAvisar() =
        runTest {
            repo.dato.value = Recordatorio("fantasma", dosHoras, dosHoras + 90 * 60_000L)
            reloj.ms = dosHoras
            gestor.procesar()
            assertTrue(aviso.avisos.isEmpty())
            assertNull(repo.dato.value)
        }

    @Test
    fun sinRecordatorioProcesarNoHaceNada() =
        runTest {
            gestor.procesar()
            assertTrue(aviso.avisos.isEmpty())
            assertEquals(0, alarma.programaciones)
        }

    // Abrir cualquier cocina cancela el recordatorio: la cadena lo comprueba al reprogramar.
    private val repoCrono = FakeCronoEstadoRepo()
    private val almacen = AlmacenCrono(repoCrono)
    private val cadena =
        CadenaAlarmas(almacen, recetas, reloj, FakeAlarmaSistema(), FakeNotificador(), recordatorio = gestor)

    private suspend fun abrirCocina(slug: String) {
        val receta = recetaCrono(slug)
        var e = SesionCrono.inicial(receta, T0 + 15 * 60_000L, T0)
        e = SesionCrono.abrir(e, receta, T0)
        e = MotorCrono.empezar(e, receta, "f0", T0)
        almacen.fijar(e)
    }

    @Test
    fun abrirLaCocinaDeCualquierRecetaCancelaElRecordatorio() =
        runTest {
            gestor.armar("a", 14 * 60, servirMin)
            abrirCocina("b")
            cadena.reprogramar()
            assertNull(repo.dato.value)
            assertNull(alarma.programada)
        }

    @Test
    fun sinCocinadaVivaLaCadenaNoTocaElRecordatorio() =
        runTest {
            gestor.armar("a", 14 * 60, servirMin)
            // Una cocinada sin abrir no está viva.
            almacen.fijar(SesionCrono.inicial(recetaCrono("b"), T0 + 15 * 60_000L, T0))
            cadena.reprogramar()
            assertEquals("a", repo.dato.value?.slug)
            assertEquals(dosHoras, alarma.programada)
        }

    @Test
    fun elRecordatorioNoCuentaComoCocinadaViva() =
        runTest {
            gestor.armar("a", 14 * 60, servirMin)
            assertFalse(cadena.vivas.hayViva())
            assertNull(cadena.proximoEvento())
        }
}
