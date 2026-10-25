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

/**
 * 2 → 3 (v1.7): valoración y total previsto de cada cocinada del historial, y «quiero hacerla» en las preferencias.
 * Columnas nuevas nullable en historial (las cocinadas viejas quedan sin valorar y sin previsto) y `quiereHacerla`
 * NOT NULL DEFAULT 0 en preferencia (las existentes quedan en «no»).
 */
val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE historial ADD COLUMN valoracion TEXT")
            db.execSQL("ALTER TABLE historial ADD COLUMN previstoMin INTEGER")
            db.execSQL("ALTER TABLE preferencia ADD COLUMN quiereHacerla INTEGER NOT NULL DEFAULT 0")
        }
    }
