package app.trombito.souschef.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.trombito.souschef.data.db.MetaEntity
import app.trombito.souschef.data.db.SousChefDatabase
import app.trombito.souschef.domain.crono.Recordatorio
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecordatorioRepoRoomTest {
    private lateinit var db: SousChefDatabase
    private lateinit var repo: RecordatorioRepoRoom
    private val r = Recordatorio("sopa", 1_800_000_000_000L, 1_800_005_400_000L)

    @Before
    fun abrir() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SousChefDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repo = RecordatorioRepoRoom(db.metaDao())
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun sinNadaGuardadoNoHayRecordatorio() =
        runBlocking {
            assertNull(repo.leer())
            assertNull(repo.observar().first())
        }

    @Test
    fun guardarYLeerDevuelveElMismo() =
        runBlocking {
            repo.guardar(r)
            assertEquals(r, repo.leer())
            assertEquals(r, repo.observar().first())
        }

    @Test
    fun guardarOtroSustituyeAlAnterior() =
        runBlocking {
            repo.guardar(r)
            repo.guardar(r.copy(slug = "arroz"))
            assertEquals("arroz", repo.leer()?.slug)
        }

    @Test
    fun borrarLoDejaSinRecordatorioYBorrarSinNadaNoFalla() =
        runBlocking {
            repo.borrar()
            repo.guardar(r)
            repo.borrar()
            assertNull(repo.leer())
            assertNull(repo.observar().first())
        }

    @Test
    fun unValorIlegibleCuentaComoSinRecordatorio() =
        runBlocking {
            db.metaDao().fijar(MetaEntity(RecordatorioRepoRoom.CLAVE, "esto no es un recordatorio"))
            assertNull(repo.leer())
        }
}
