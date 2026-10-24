package app.trombito.souschef.domain

import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tecnica
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

    /**
     * Importación de una copia: añade en una sola transacción las entradas cuyo id no existe y devuelve cuántas
     * entraron. Nunca pisa una cocinada que ya está.
     */
    suspend fun importar(entradas: List<EntradaHistorial>): Int

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

@Suppress("TooManyFunctions") // un setter por ajuste
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

    /** Se guarda ya ajustada con `Cocina.valida()`. */
    suspend fun fijarCocina(c: Cocina)

    /** [n] se ajusta a [FUEGOS_MIN]..[FUEGOS_MAX]. */
    suspend fun fijarFuegos(n: Int)

    suspend fun fijarHorno(tiene: Boolean)

    /** [n] se ajusta a [RACIONES_MIN]..[RACIONES_MAX]. */
    suspend fun fijarRacionesDefecto(n: Int)

    /** Con `false` la intro se vuelve a ver (desde Ajustes). */
    suspend fun marcarIntroVista(vista: Boolean)

    /** Con `false` la guía del cronómetro se vuelve a ver (desde Ajustes). */
    suspend fun marcarGuiaCronoVista(vista: Boolean)
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
