package app.trombito.souschef.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.trombito.souschef.data.db.SousChefDatabase
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.PasoReal
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

    @Test
    fun editarNotaEsUnUpdatePuntualQueNoTocaElRestoDeLaCocinada() =
        runBlocking {
            val completa =
                curry.copy(
                    raciones = 3,
                    minutosReales = 52,
                    nota = "antes",
                    previstoMin = 48,
                    valoracion = Valoracion.AJUSTAR,
                    pasosReales = listOf(PasoReal("p1", "Sofrito", 480)),
                )
            repo.agregar(completa)

            repo.editarNota(completa.id, "  más pimentón ")

            assertEquals(completa.copy(nota = "más pimentón"), repo.observarTodo().first().single())
        }

    @Test
    fun editarNotaVaciaLaQuitaYUnIdQueNoExisteNoCreaNada() =
        runBlocking {
            repo.agregar(curry.copy(nota = "antes"))

            repo.editarNota(curry.id, "   ")
            repo.editarNota("no-existe", "fantasma")

            val todas = repo.observarTodo().first()
            assertEquals(listOf(curry), todas)
        }

    @Test
    fun borrarYVolverAAgregarDejaLaMismaEntradaConSuValoracion() =
        runBlocking {
            val valorada = curry.copy(valoracion = Valoracion.REPETIR, nota = "con limón")
            repo.agregar(valorada)

            repo.borrar(valorada.id)
            repo.agregar(valorada)

            assertEquals(valorada, repo.observarTodo().first().single())
        }
}
