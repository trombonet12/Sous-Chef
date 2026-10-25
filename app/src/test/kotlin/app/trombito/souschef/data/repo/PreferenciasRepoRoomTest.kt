package app.trombito.souschef.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.trombito.souschef.data.db.SousChefDatabase
import app.trombito.souschef.domain.Preferencia
import app.trombito.souschef.domain.copia.Copia
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreferenciasRepoRoomTest {
    private lateinit var db: SousChefDatabase
    private lateinit var repo: PreferenciasRepoRoom

    @Before
    fun abrir() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SousChefDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repo = PreferenciasRepoRoom(db, db.preferenciaDao())
    }

    @After
    fun cerrar() = db.close()

    private fun copia(vararg prefs: Preferencia) = Copia(historial = emptyList(), preferencias = prefs.toList())

    @Test
    fun importarGuardaTodasLasPreferencias() =
        runBlocking {
            repo.fijarRaciones("curry", 3)

            repo.importar(
                copia(
                    Preferencia("curry", favorito = true, raciones = 3),
                    Preferencia("tacos", favorito = true, raciones = 4),
                ),
            )

            assertEquals(
                mapOf(
                    "curry" to Preferencia("curry", favorito = true, raciones = 3),
                    "tacos" to Preferencia("tacos", favorito = true, raciones = 4),
                ),
                repo.observarTodas().first(),
            )
        }

    @Test
    fun importarFusionaConLoQueHayEnLaBaseNoConUnaFotoVieja() =
        runBlocking {
            // Un cambio hecho justo antes de importar (el caso de un fijarFavorito concurrente) no se revierte.
            repo.fijarFavorito("curry", true)
            repo.fijarQuiereHacerla("tacos", true)

            repo.importar(
                copia(
                    Preferencia("curry", favorito = false, raciones = 5),
                    Preferencia("tacos", favorito = false, raciones = 2),
                ),
            )

            assertEquals(
                mapOf(
                    "curry" to Preferencia("curry", favorito = true, raciones = 5),
                    "tacos" to Preferencia("tacos", favorito = false, raciones = 2, quiereHacerla = true),
                ),
                repo.observarTodas().first(),
            )
        }

    @Test
    fun importarEsTodoONadaSiUnaFilaFallaAMedias() {
        runBlocking {
            repo.fijarRaciones("curry", 3)
            // La segunda fila aborta el INSERT: simula un fallo a medias del lote.
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER rechaza_malo BEFORE INSERT ON preferencia WHEN NEW.slug = 'malo' " +
                    "BEGIN SELECT RAISE(ABORT, 'fila rechazada'); END",
            )
            val antes = repo.observarTodas().first()

            assertThrows(Exception::class.java) {
                runBlocking {
                    repo.importar(
                        copia(
                            Preferencia("curry", favorito = true, raciones = 3),
                            Preferencia("tacos", favorito = true),
                            Preferencia("malo", favorito = true),
                        ),
                    )
                }
            }

            assertEquals(antes, repo.observarTodas().first())
            assertEquals(Preferencia("curry", favorito = false, raciones = 3), antes["curry"])
        }
    }
}
