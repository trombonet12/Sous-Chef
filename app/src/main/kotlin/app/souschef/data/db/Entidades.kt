package app.souschef.data.db

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
)

@Entity(tableName = "preferencia")
data class PreferenciaEntity(
    @PrimaryKey val slug: String,
    val favorito: Boolean,
    val raciones: Int?,
)

@Entity(tableName = "despensa_item")
data class DespensaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val categoria: String,
    /** HAY, POCO, FALTA o VETADO. */
    val estado: String,
    val nota: String?,
)

@Entity(tableName = "compra_item")
data class CompraItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val texto: String,
    val zona: String,
    val origen: String?,
    val marcado: Boolean,
    val orden: Int,
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
