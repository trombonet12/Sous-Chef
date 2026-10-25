package app.trombito.souschef.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.trombito.souschef.data.db.MetaEntity
import app.trombito.souschef.data.db.SousChefDatabase
import app.trombito.souschef.domain.copia.EstadoCopiaAuto
import app.trombito.souschef.domain.copia.FalloCopiaAuto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CopiaAutoRepoRoomTest {
    private lateinit var db: SousChefDatabase
    private lateinit var repo: CopiaAutoRepoRoom

    @Before
    fun abrir() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SousChefDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repo = CopiaAutoRepoRoom(db.metaDao())
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun sinNadaGuardadoElEstadoEstaVacio() =
        runBlocking {
            assertEquals(EstadoCopiaAuto(), repo.leer())
            assertEquals(EstadoCopiaAuto(), repo.observar().first())
        }

    @Test
    fun guardaYLeeLaCarpetaLasCopiasYElFallo() =
        runBlocking {
            repo.fijarCarpeta("content://carpeta")
            repo.registrarCopia(1_000)
            repo.registrarManual(2_000)
            repo.registrarFallo(FalloCopiaAuto.NO_SE_PUDO)

            val esperado = EstadoCopiaAuto("content://carpeta", 1_000, 2_000, FalloCopiaAuto.NO_SE_PUDO)
            assertEquals(esperado, repo.leer())
            assertEquals(esperado, repo.observar().first())
        }

    @Test
    fun unaCopiaBuenaBorraElFallo() =
        runBlocking {
            repo.fijarCarpeta("content://carpeta")
            repo.registrarFallo(FalloCopiaAuto.CARPETA_NO_VALE)
            repo.registrarCopia(5_000)
            assertEquals(EstadoCopiaAuto("content://carpeta", 5_000), repo.leer())
        }

    @Test
    fun cambiarOQuitarLaCarpetaBorraElFalloPeroNoLasFechas() =
        runBlocking {
            repo.fijarCarpeta("content://vieja")
            repo.registrarCopia(5_000)
            repo.registrarManual(6_000)
            repo.registrarFallo(FalloCopiaAuto.CARPETA_NO_VALE)

            repo.fijarCarpeta("content://nueva")
            assertEquals(EstadoCopiaAuto("content://nueva", 5_000, 6_000), repo.leer())

            repo.fijarCarpeta(null)
            assertEquals(EstadoCopiaAuto(null, 5_000, 6_000), repo.leer())
        }

    @Test
    fun unValorIlegibleCuentaComoAusente() =
        runBlocking {
            db.metaDao().fijar(MetaEntity(EstadoCopiaAuto.CLAVE_ULTIMA_AUTO, "ayer"))
            db.metaDao().fijar(MetaEntity(EstadoCopiaAuto.CLAVE_FALLO, "EXPLOTO"))
            assertEquals(EstadoCopiaAuto(), repo.leer())
        }
}
