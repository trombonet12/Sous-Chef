package app.trombito.souschef.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RecetaEntity::class,
        TecnicaEntity::class,
        HistorialEntity::class,
        PreferenciaEntity::class,
        CronoEstadoEntity::class,
        MetaEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class SousChefDatabase : RoomDatabase() {
    abstract fun recetaDao(): RecetaDao

    abstract fun tecnicaDao(): TecnicaDao

    abstract fun historialDao(): HistorialDao

    abstract fun preferenciaDao(): PreferenciaDao

    abstract fun cronoEstadoDao(): CronoEstadoDao

    abstract fun metaDao(): MetaDao
}
