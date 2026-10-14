package app.souschef.data.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.souschef.data.db.SousChefDatabase
import app.souschef.domain.EstadoDespensa
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeederTest {
    private lateinit var db: SousChefDatabase
    private lateinit var context: Context

    @Before
    fun abrir() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SousChefDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun siembraLosDatosDelRepo() =
        runBlocking {
            Seeder(db, FuenteAssets(context)).sembrar()

            assertEquals(13, db.recetaDao().contar())
            assertEquals(17, db.tecnicaDao().contar())
            assertEquals(
                11,
                db
                    .historialDao()
                    .observarTodo()
                    .first()
                    .size,
            )
            assertEquals(
                2,
                db
                    .preferenciaDao()
                    .observarTodas()
                    .first()
                    .size,
            )
            val despensa = db.despensaDao().observar().first()
            assertTrue(despensa.any { it.estado == EstadoDespensa.VETADO.name && it.nombre == "Anchoas" })
            assertTrue(despensa.count { it.estado == EstadoDespensa.HAY.name } > 100)
        }

    @Test
    fun segundaVezNoDuplicaNiPisaLoDelUsuario() =
        runBlocking {
            val seeder = Seeder(db, FuenteAssets(context))
            seeder.sembrar()
            db.historialDao().borrar(
                db
                    .historialDao()
                    .observarTodo()
                    .first()
                    .first()
                    .id,
            )
            db
                .despensaDao()
                .observar()
                .first()
                .first()
                .let { db.despensaDao().borrar(it.id) }
            val despensaAntes =
                db
                    .despensaDao()
                    .observar()
                    .first()
                    .size

            seeder.sembrar()

            assertEquals(13, db.recetaDao().contar())
            assertEquals(
                10,
                db
                    .historialDao()
                    .observarTodo()
                    .first()
                    .size,
            )
            assertEquals(
                despensaAntes,
                db
                    .despensaDao()
                    .observar()
                    .first()
                    .size,
            )
        }

    @Test
    fun recargaRecetasSiCambiaElHash() =
        runBlocking {
            val base = FuenteAssets(context)
            Seeder(db, base).sembrar()
            val modificada =
                object : FuenteSemilla by base {
                    override fun leer(ruta: String): String =
                        base.leer(ruta).let {
                            if (ruta ==
                                "recetas/huevos-rotos.json"
                            ) {
                                it.replaceFirst("\"titulo\": \"", "\"titulo\": \"X ")
                            } else {
                                it
                            }
                        }
                }

            Seeder(db, modificada).sembrar()

            assertEquals(13, db.recetaDao().contar())
            assertTrue(requireNotNull(db.recetaDao().obtener("huevos-rotos")).doc.contains("\"titulo\": \"X "))
        }
}
