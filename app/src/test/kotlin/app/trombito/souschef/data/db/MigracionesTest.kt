package app.trombito.souschef.data.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Migraciones Room sin MigrationTestHelper (Robolectric solo ve los assets de `debug`, y los schemas no van al APK):
 * se crea la versión vieja con el SQL de su schema JSON, se abre Room en la nueva con la migración y Room valida
 * que el esquema resultante es exactamente el de las entidades actuales.
 */
@RunWith(RobolectricTestRunner::class)
class MigracionesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun borrar() {
        context.deleteDatabase(NOMBRE)
    }

    @Test
    fun de1a2BorraDespensaYCompraYConservaLoDemas() {
        crearVersion(1) { db ->
            db.execSQL("INSERT INTO despensa_item (nombre, categoria, estado) VALUES ('Sal', 'Especias', 'HAY')")
            db.execSQL("INSERT INTO compra_item (texto, zona, origen, marcado, orden) VALUES ('pan', '', 'sopa', 0, 0)")
            db.execSQL("INSERT INTO meta (clave, valor) VALUES ('despensa_hash', 'abc')")
            db.execSQL("INSERT INTO meta (clave, valor) VALUES ('datos_hash', 'xyz')")
            db.execSQL("INSERT INTO preferencia (slug, favorito, raciones) VALUES ('sopa', 1, 4)")
        }

        val room =
            Room
                .databaseBuilder(context, SousChefDatabase::class.java, NOMBRE)
                .addMigrations(MIGRATION_1_2)
                .allowMainThreadQueries()
                .build()
        val db = room.openHelper.writableDatabase

        val tablas =
            db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { c ->
                buildList { while (c.moveToNext()) add(c.getString(0)) }
            }
        assertFalse("despensa_item" in tablas)
        assertFalse("compra_item" in tablas)
        val claves =
            db.query("SELECT clave FROM meta ORDER BY clave").use { c ->
                buildList { while (c.moveToNext()) add(c.getString(0)) }
            }
        assertEquals(listOf("datos_hash"), claves)
        db.query("SELECT raciones FROM preferencia WHERE slug = 'sopa'").use { c ->
            c.moveToFirst()
            assertEquals(4, c.getInt(0))
        }
        room.close()
    }

    /** Crea la base en la [version] dada con el SQL de `schemas/<db>/<version>.json` y la rellena con [datos]. */
    private fun crearVersion(
        version: Int,
        datos: (SupportSQLiteDatabase) -> Unit,
    ) {
        val schema = Json.parseToJsonElement(File("$SCHEMAS/$version.json").readText()).jsonObject
        val bd = schema.getValue("database").jsonObject
        val sentencias =
            bd.getValue("entities").jsonArray.flatMap { e ->
                val entidad = e.jsonObject
                val tabla = entidad.getValue("tableName").jsonPrimitive.content
                val indices = entidad["indices"]?.jsonArray.orEmpty().map { it.jsonObject.getValue("createSql") }
                (listOf(entidad.getValue("createSql")) + indices).map {
                    it.jsonPrimitive.content.replace("\${TABLE_NAME}", tabla)
                }
            } + bd.getValue("setupQueries").jsonArray.map { it.jsonPrimitive.content }
        val config =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(NOMBRE)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(version) {
                        override fun onCreate(db: SupportSQLiteDatabase) = sentencias.forEach(db::execSQL)

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build()
        FrameworkSQLiteOpenHelperFactory().create(config).use { helper -> datos(helper.writableDatabase) }
    }

    private companion object {
        const val NOMBRE = "migracion-test.db"
        const val SCHEMAS = "schemas/app.trombito.souschef.data.db.SousChefDatabase"
    }
}
