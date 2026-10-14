package app.souschef.domain

import app.souschef.receta.Receta
import app.souschef.receta.Tecnica
import kotlinx.coroutines.flow.Flow

/** Solo lectura: la app nunca edita recetas. */
interface RecetasRepo {
    fun observarTodas(): Flow<List<Receta>>

    fun observar(slug: String): Flow<Receta?>

    suspend fun obtener(slug: String): Receta?
}

/** Solo lectura. */
interface TecnicasRepo {
    fun observarTodas(): Flow<List<Tecnica>>

    fun observar(slug: String): Flow<Tecnica?>

    suspend fun obtener(slug: String): Tecnica?
}

interface HistorialRepo {
    /** Más reciente primero (fecha y hora del id descendentes). */
    fun observarTodo(): Flow<List<EntradaHistorial>>

    /** slug -> fecha (yyyy-MM-dd) de la última vez cocinada. */
    fun observarUltimaFechaPorSlug(): Flow<Map<String, String>>

    suspend fun agregar(entrada: EntradaHistorial)

    suspend fun borrar(id: String)
}

interface PreferenciasRepo {
    fun observarTodas(): Flow<Map<String, Preferencia>>

    fun observar(slug: String): Flow<Preferencia?>

    suspend fun fijarFavorito(
        slug: String,
        favorito: Boolean,
    )

    suspend fun fijarRaciones(
        slug: String,
        raciones: Int,
    )
}

interface DespensaRepo {
    fun observar(): Flow<List<ItemDespensa>>

    suspend fun agregar(
        nombre: String,
        categoria: String,
        estado: EstadoDespensa = EstadoDespensa.HAY,
    ): Long

    suspend fun cambiarEstado(
        id: Long,
        estado: EstadoDespensa,
    )

    suspend fun actualizar(item: ItemDespensa)

    suspend fun borrar(id: Long)
}

interface CompraRepo {
    /** Ordenada por `orden`. */
    fun observar(): Flow<List<ItemCompra>>

    suspend fun agregar(
        texto: String,
        zona: String = "",
        origen: String? = null,
    ): Long

    suspend fun agregarVarios(
        textos: List<String>,
        origen: String? = null,
    )

    suspend fun marcar(
        id: Long,
        marcado: Boolean,
    )

    suspend fun borrar(id: Long)

    suspend fun borrarMarcados()

    /** Reasigna `orden` según la posición en [idsEnOrden]. */
    suspend fun reordenar(idsEnOrden: List<Long>)
}

interface CronoEstadoRepo {
    fun observar(slug: String): Flow<CronoEstado?>

    suspend fun obtener(slug: String): CronoEstado?

    suspend fun guardar(
        slug: String,
        json: String,
    )

    suspend fun borrar(slug: String)
}
