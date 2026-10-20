package app.trombito.souschef.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 1 → 2: despensa y compra salen de la app; la despensa vive solo en `data/` para diseñar recetas. */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS despensa_item")
            db.execSQL("DROP TABLE IF EXISTS compra_item")
            db.execSQL("DELETE FROM meta WHERE clave = 'despensa_hash'")
        }
    }
