package app.trombito.souschef.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.trombito.souschef.data.db.SousChefDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResenaRepoRoomTest {
    private lateinit var db: SousChefDatabase
    private lateinit var repo: ResenaRepoRoom

    @Before
    fun abrir() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SousChefDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repo = ResenaRepoRoom(db.metaDao())
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun `al principio no esta pedida y despues de marcarla si`() =
        runBlocking {
            assertFalse(repo.yaPedida())
            repo.marcarPedida()
            assertTrue(repo.yaPedida())
        }
}
