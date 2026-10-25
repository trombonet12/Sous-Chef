package app.trombito.souschef.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.trombito.souschef.data.db.SousChefDatabase
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.OtraDelDia
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class PropuestasRepoRoomTest {
    private lateinit var db: SousChefDatabase
    private lateinit var repo: PropuestasRepoRoom
    private lateinit var historial: HistorialRepoRoom
    private val hoy = LocalDate.of(2026, 10, 25)

    @Before
    fun abrir() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SousChefDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repo = PropuestasRepoRoom(db.metaDao())
        historial = HistorialRepoRoom(db, db.historialDao())
    }

    @After
    fun cerrar() = db.close()

    private fun cocinada(
        slug: String,
        fecha: String = hoy.toString(),
    ) = EntradaHistorial("$slug-$fecha", slug, slug, fecha)

    @Test
    fun sinNadaGuardadoNoHayOtraNiCocinadas() =
        runBlocking {
            assertNull(repo.observarOtra().first())
            assertEquals(0, repo.observarCocinadas().first())
        }

    @Test
    fun otraSumaEnElDiaYSeReiniciaAlSiguiente() =
        runBlocking {
            repo.otra(hoy)
            repo.otra(hoy)
            assertEquals(OtraDelDia(hoy, 2), repo.observarOtra().first())

            repo.otra(hoy.plusDays(1))
            assertEquals(OtraDelDia(hoy.plusDays(1), 1), repo.observarOtra().first())
        }

    @Test
    fun cocinarUnaRecetaPropuestaSumaUnoAlContador() =
        runBlocking {
            repo.mostrar(hoy, listOf("curry", "lasana"))

            historial.agregar(cocinada("curry"))
            assertEquals(1, repo.observarCocinadas().first())

            historial.agregar(cocinada("paella"))
            assertEquals(1, repo.observarCocinadas().first())
        }

    @Test
    fun volverAGuardarLaMismaCocinadaNoLaCuentaDosVeces() =
        runBlocking {
            repo.mostrar(hoy, listOf("curry"))

            historial.agregar(cocinada("curry"))
            historial.agregar(cocinada("curry").copy(nota = "más sal"))

            assertEquals(1, repo.observarCocinadas().first())
        }

    @Test
    fun unaPropuestaDeOtroDiaNoCuenta() =
        runBlocking {
            repo.mostrar(hoy.minusDays(1), listOf("curry"))

            historial.agregar(cocinada("curry"))

            assertEquals(0, repo.observarCocinadas().first())
        }
}
