package app.souschef.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.souschef.data.db.MetaEntity
import app.souschef.data.db.SousChefDatabase
import app.souschef.domain.Ajustes
import app.souschef.domain.DisposicionCrono
import app.souschef.domain.DisposicionLista
import app.souschef.domain.HoraTarjeta
import app.souschef.domain.OrdenRecetas
import app.souschef.domain.TemaApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AjustesRepoRoomTest {
    private lateinit var db: SousChefDatabase
    private lateinit var repo: AjustesRepoRoom

    @Before
    fun abrir() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SousChefDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repo = AjustesRepoRoom(db.metaDao())
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun sinDatosDevuelveElDefecto() =
        runBlocking {
            assertEquals(Ajustes(DisposicionCrono.APILADA, true), repo.observar().first())
            assertEquals(DisposicionLista.TINTE, repo.observar().first().lista)
            assertEquals(OrdenRecetas.FAVORITAS, repo.observar().first().orden)
        }

    @Test
    fun fijarYObservar() =
        runBlocking {
            repo.fijarDisposicion(DisposicionCrono.APILADA)
            repo.fijarVoz(false)
            assertEquals(Ajustes(DisposicionCrono.APILADA, false), repo.observar().first())

            repo.fijarDisposicion(DisposicionCrono.FOCO)
            assertEquals(Ajustes(DisposicionCrono.FOCO, false), repo.observar().first())
        }

    @Test
    fun valorCorruptoVuelveAlDefecto() =
        runBlocking {
            db.metaDao().fijar(MetaEntity(AjustesRepoRoom.CLAVE_DISPOSICION, "diagonal"))
            db.metaDao().fijar(MetaEntity(AjustesRepoRoom.CLAVE_VOZ, "quizá"))
            db.metaDao().fijar(MetaEntity(AjustesRepoRoom.CLAVE_LISTA, "mosaico"))
            db.metaDao().fijar(MetaEntity(AjustesRepoRoom.CLAVE_ORDEN, "aleatorio"))
            db.metaDao().fijar(MetaEntity(AjustesRepoRoom.CLAVE_TEMA, "sepia"))
            db.metaDao().fijar(MetaEntity(AjustesRepoRoom.CLAVE_SERVIR, "25:99"))
            db.metaDao().fijar(MetaEntity(AjustesRepoRoom.CLAVE_HORA, "manana"))
            assertEquals(Ajustes(), repo.observar().first())
        }

    @Test
    fun fijarDisenoYOrdenDeLaLista() =
        runBlocking {
            repo.fijarLista(DisposicionLista.FRANJA)
            repo.fijarOrden(OrdenRecetas.RAPIDA)
            val a = repo.observar().first()
            assertEquals(DisposicionLista.FRANJA, a.lista)
            assertEquals(OrdenRecetas.RAPIDA, a.orden)
            assertEquals(DisposicionCrono.APILADA, a.disposicion)
        }

    @Test
    fun temaHoraDeServirYHoraDeLasTarjetas() =
        runBlocking {
            assertEquals(TemaApp.AUTOMATICO, repo.observar().first().tema)
            assertEquals(15 * 60 + 30, repo.observar().first().servirMin)
            repo.fijarTema(TemaApp.OSCURO)
            repo.fijarServir(14 * 60 + 15)
            repo.fijarHoraTarjeta(HoraTarjeta.ACABAR)
            val a = repo.observar().first()
            assertEquals(TemaApp.OSCURO, a.tema)
            assertEquals(14 * 60 + 15, a.servirMin)
            assertEquals(HoraTarjeta.ACABAR, a.horaTarjeta)
            assertEquals("14:15", db.metaDao().valor(AjustesRepoRoom.CLAVE_SERVIR))
        }
}
