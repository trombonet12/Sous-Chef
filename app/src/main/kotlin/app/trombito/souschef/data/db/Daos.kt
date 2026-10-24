package app.trombito.souschef.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecetaDao {
    @Query("SELECT * FROM receta ORDER BY slug")
    fun observarTodas(): Flow<List<RecetaEntity>>

    @Query("SELECT * FROM receta WHERE slug = :slug")
    fun observar(slug: String): Flow<RecetaEntity?>

    @Query("SELECT * FROM receta WHERE slug = :slug")
    suspend fun obtener(slug: String): RecetaEntity?

    @Query("SELECT COUNT(*) FROM receta")
    suspend fun contar(): Int

    @Query("DELETE FROM receta")
    suspend fun borrarTodas()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(recetas: List<RecetaEntity>)
}

@Dao
interface TecnicaDao {
    @Query("SELECT * FROM tecnica ORDER BY slug")
    fun observarTodas(): Flow<List<TecnicaEntity>>

    @Query("SELECT * FROM tecnica WHERE slug = :slug")
    fun observar(slug: String): Flow<TecnicaEntity?>

    @Query("SELECT * FROM tecnica WHERE slug = :slug")
    suspend fun obtener(slug: String): TecnicaEntity?

    @Query("SELECT COUNT(*) FROM tecnica")
    suspend fun contar(): Int

    @Query("DELETE FROM tecnica")
    suspend fun borrarTodas()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(tecnicas: List<TecnicaEntity>)
}

data class UltimaFecha(
    val slug: String,
    val fecha: String,
)

@Dao
interface HistorialDao {
    @Query("SELECT * FROM historial ORDER BY fecha DESC, id DESC")
    fun observarTodo(): Flow<List<HistorialEntity>>

    @Query("SELECT slug, MAX(fecha) AS fecha FROM historial GROUP BY slug")
    fun observarUltimaFecha(): Flow<List<UltimaFecha>>

    @Query("SELECT COUNT(*) FROM historial")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(entrada: HistorialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(entradas: List<HistorialEntity>)

    /**
     * Solo para importar una copia: en lote y en una transacción, y las que ya existen (mismo id) se ignoran en vez de
     * pisarse. Devuelve el rowid de cada una insertada, o -1 si se ignoró.
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarNuevas(entradas: List<HistorialEntity>): List<Long>

    @Query("DELETE FROM historial WHERE id = :id")
    suspend fun borrar(id: String)
}

@Dao
interface PreferenciaDao {
    @Query("SELECT * FROM preferencia")
    fun observarTodas(): Flow<List<PreferenciaEntity>>

    @Query("SELECT * FROM preferencia WHERE slug = :slug")
    fun observar(slug: String): Flow<PreferenciaEntity?>

    @Query("SELECT * FROM preferencia WHERE slug = :slug")
    suspend fun obtener(slug: String): PreferenciaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(preferencia: PreferenciaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(preferencias: List<PreferenciaEntity>)
}

@Dao
interface CronoEstadoDao {
    @Query("SELECT * FROM crono_estado WHERE slug = :slug")
    fun observar(slug: String): Flow<CronoEstadoEntity?>

    @Query("SELECT * FROM crono_estado WHERE slug = :slug")
    suspend fun obtener(slug: String): CronoEstadoEntity?

    @Query("SELECT * FROM crono_estado")
    suspend fun todos(): List<CronoEstadoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(estado: CronoEstadoEntity)

    @Query("DELETE FROM crono_estado WHERE slug = :slug")
    suspend fun borrar(slug: String)
}

@Dao
interface MetaDao {
    @Query("SELECT valor FROM meta WHERE clave = :clave")
    suspend fun valor(clave: String): String?

    @Query("SELECT valor FROM meta WHERE clave = :clave")
    fun observar(clave: String): Flow<String?>

    /** Una sola consulta para varias claves: el observador ve siempre un estado coherente (ausentes no salen). */
    @Query("SELECT * FROM meta WHERE clave IN (:claves)")
    fun observarVarias(claves: List<String>): Flow<List<MetaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun fijar(meta: MetaEntity)

    /** Escribe todas las filas en una sola transacción: o entran todas o ninguna. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun fijarVarias(metas: List<MetaEntity>)
}
