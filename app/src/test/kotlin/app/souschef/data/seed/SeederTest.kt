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

    private fun conSemilla(
        base: FuenteSemilla,
        transformar: (String) -> String,
    ): FuenteSemilla =
        object : FuenteSemilla by base {
            override fun leer(ruta: String): String =
                base.leer(ruta).let { if (ruta == "despensa-semilla.md") transformar(it) else it }
        }

    private fun semillaConExtra(
        extra: String,
        original: String,
    ): String = original.replaceFirst("## Carnes\n", "## Carnes\n$extra · ")

    @Test
    fun mismaSemillaNoTocaLaDespensa() =
        runBlocking {
            val base = FuenteAssets(context)
            Seeder(db, base).sembrar()
            val dao = db.despensaDao()
            val primero = dao.observar().first()
            dao.borrar(primero.first().id)
            val tras = dao.observar().first().size

            Seeder(db, base).sembrar()

            assertEquals(tras, dao.observar().first().size)
        }

    @Test
    fun semillaConArticuloNuevoLoAnadeComoHaySinPisarLoExistente() =
        runBlocking {
            val base = FuenteAssets(context)
            Seeder(db, base).sembrar()
            val dao = db.despensaDao()
            val bacon = dao.observar().first().first { it.nombre == "Bacon" }
            dao.cambiarEstado(bacon.id, EstadoDespensa.FALTA.name)
            val antes = dao.observar().first().size

            Seeder(db, conSemilla(base) { semillaConExtra("Trufa negra", it) }).sembrar()

            val despues = dao.observar().first()
            assertEquals(antes + 1, despues.size)
            val trufa = despues.single { it.nombre == "Trufa negra" }
            assertEquals(EstadoDespensa.HAY.name, trufa.estado)
            assertEquals("Carnes", trufa.categoria)
            assertEquals(EstadoDespensa.FALTA.name, despues.first { it.id == bacon.id }.estado)
        }

    @Test
    fun nombresConOtraTildeOMayusculaNoSeDuplican() =
        runBlocking {
            val base = FuenteAssets(context)
            Seeder(db, base).sembrar()
            val antes =
                db
                    .despensaDao()
                    .observar()
                    .first()
                    .size

            Seeder(
                db,
                conSemilla(base) { semillaConExtra("BACON · jamón serrano en lonchas · HÉLICES o pajaritas", it) },
            ).sembrar()

            assertEquals(
                antes,
                db
                    .despensaDao()
                    .observar()
                    .first()
                    .size,
            )
        }

    @Test
    fun instalacionPrevisaSinHashRecibeLosNuevos() =
        runBlocking {
            val base = FuenteAssets(context)
            Seeder(db, base).sembrar()
            val dao = db.despensaDao()
            val emperador = dao.observar().first().first { it.nombre == "Emperador" }
            dao.borrar(emperador.id)
            db.openHelper.writableDatabase.execSQL("DELETE FROM meta WHERE clave = '${Seeder.CLAVE_HASH_DESPENSA}'")

            Seeder(db, base).sembrar()

            assertTrue(dao.observar().first().any { it.nombre == "Emperador" })
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
