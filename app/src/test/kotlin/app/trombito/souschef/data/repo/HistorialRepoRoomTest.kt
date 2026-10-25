package app.trombito.souschef.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.trombito.souschef.data.db.SousChefDatabase
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.Valoracion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistorialRepoRoomTest {
    private lateinit var db: SousChefDatabase
    private lateinit var repo: HistorialRepoRoom
    private val curry = EntradaHistorial("curry-2026-10-01", "curry", "Curry", "2026-10-01")

    @Before
    fun abrir() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SousChefDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repo = HistorialRepoRoom(db, db.historialDao())
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun volverAGuardarLaMismaCocinadaSinValoracionNoBorraLaQueYaTiene() =
        runBlocking {
            repo.agregar(curry)
            repo.valorar(curry.id, Valoracion.REPETIR)

            repo.agregar(curry.copy(nota = "con más sal", valoracion = null))

            val guardada = repo.observarTodo().first().single()
            assertEquals(Valoracion.REPETIR, guardada.valoracion)
            assertEquals("con más sal", guardada.nota)
        }

    @Test
    fun volverAGuardarConValoracionLaSustituye() =
        runBlocking {
            repo.agregar(curry.copy(valoracion = Valoracion.REPETIR))

            repo.agregar(curry.copy(valoracion = Valoracion.AJUSTAR))

            val guardada = repo.observarTodo().first().single()
            assertEquals(Valoracion.AJUSTAR, guardada.valoracion)
        }

    @Test
    fun unaCocinadaNuevaSinValoracionSeGuardaSinValoracion() =
        runBlocking {
            repo.agregar(curry)

            assertEquals(curry, repo.observarTodo().first().single())
        }
}
