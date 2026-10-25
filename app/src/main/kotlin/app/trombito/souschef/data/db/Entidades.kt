package app.trombito.souschef.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "receta")
data class RecetaEntity(
    @PrimaryKey val slug: String,
    val doc: String,
    val hash: String,
)

@Entity(tableName = "tecnica")
data class TecnicaEntity(
    @PrimaryKey val slug: String,
    val doc: String,
    val hash: String,
)

@Entity(tableName = "historial", indices = [Index("slug"), Index("fecha")])
data class HistorialEntity(
    @PrimaryKey val id: String,
    val slug: String,
    val titulo: String,
    val fecha: String,
    val base: String?,
    val tecnicaJson: String,
    val raciones: Int?,
    val minutosReales: Int?,
    val nota: String?,
    val pasosRealesJson: String?,
    /** Código de `Valoracion` (`repetir`, `ajustar`, `no_repetir`); null si no se valoró (v3). */
    val valoracion: String? = null,
    /** Total previsto (min, `ceil`) de la receta escalada en esa cocinada; null en las anteriores a v3. */
    val previstoMin: Int? = null,
)

@Entity(tableName = "preferencia")
data class PreferenciaEntity(
    @PrimaryKey val slug: String,
    val favorito: Boolean,
    val raciones: Int?,
    /** «Quiero hacerla» (v3). NOT NULL con 0 por defecto: no hay un «sin decidir» distinto de «no». */
    @ColumnInfo(defaultValue = "0") val quiereHacerla: Boolean = false,
)

@Entity(tableName = "crono_estado")
data class CronoEstadoEntity(
    @PrimaryKey val slug: String,
    val json: String,
    val actualizadoMs: Long,
)

@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val clave: String,
    val valor: String,
)
