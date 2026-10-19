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

interface AjustesRepo {
    fun observar(): Flow<Ajustes>

    suspend fun fijarDisposicion(d: DisposicionCrono)

    suspend fun fijarVoz(activa: Boolean)

    suspend fun fijarLista(d: DisposicionLista)

    suspend fun fijarOrden(o: OrdenRecetas)

    suspend fun fijarTema(t: TemaApp)

    /** [minutos] del día, 0..1439. */
    suspend fun fijarServir(minutos: Int)

    suspend fun fijarHoraTarjeta(h: HoraTarjeta)
}

interface CronoEstadoRepo {
    fun observar(slug: String): Flow<CronoEstado?>

    suspend fun obtener(slug: String): CronoEstado?

    /** Todas las cocinadas guardadas (la cadena de alarmas las recorre tras un reinicio). */
    suspend fun todos(): List<CronoEstado>

    suspend fun guardar(
        slug: String,
        json: String,
    )

    suspend fun borrar(slug: String)
}
