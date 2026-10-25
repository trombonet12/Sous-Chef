package app.trombito.souschef.domain

import app.trombito.souschef.domain.copia.Copia
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Equipo
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
     * Importación de una copia, todo en una sola transacción: añade las [entradas] cuyo id no existe y devuelve
     * cuántas entraron; nunca pisa una cocinada que ya está. [valoraciones] (id -> valoración, ya fusionadas con
     * `CopiaHistorial.valoracionesNuevas`) solo rellena las cocinadas locales que no tienen valoración: si la local ya
     * la tiene, gana la local.
     */
    suspend fun importar(
        entradas: List<EntradaHistorial>,
        valoraciones: Map<String, Valoracion> = emptyMap(),
    ): Int

    /** Fija (o quita, con null) la valoración de una cocinada ya guardada. */
    suspend fun valorar(
        id: String,
        valoracion: Valoracion?,
    )

    /**
     * Cambia solo la nota de una cocinada ya guardada (un UPDATE puntual: no toca la valoración ni el resto). La nota
     * se guarda con `normalizarNota`: recortada y null si queda vacía. Si el id no existe, no hace nada.
     */
    suspend fun editarNota(
        id: String,
        nota: String?,
    )

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

    suspend fun fijarQuiereHacerla(
        slug: String,
        quiere: Boolean,
    )

    /** Slugs marcados con «quiero hacerla» (vacío si ninguno); se emite al cambiar la marca de cualquiera. */
    fun observarQuiereHacerla(): Flow<Set<String>>

    /**
     * Importación de las preferencias de [copia]. La lectura de las actuales, la fusión
     * (`CopiaHistorial.preferenciasNuevas`: favorito y «quiero hacerla» suman, raciones no pisa) y el guardado van en
     * una misma transacción de este repositorio: un cambio concurrente (p. ej. `fijarFavorito`) no se pierde y las
     * preferencias entran todas o ninguna. No comparte transacción con `HistorialRepo.importar`.
     */
    suspend fun importar(copia: Copia)
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

    /** Hora de cenar, [minutos] del día, 0..1439. */
    suspend fun fijarCenar(minutos: Int)

    suspend fun fijarHoraTarjeta(h: HoraTarjeta)

    /** Se guarda ya ajustada con `Cocina.valida()`. */
    suspend fun fijarCocina(c: Cocina)

    /** [n] se ajusta a [FUEGOS_MIN]..[FUEGOS_MAX]. */
    suspend fun fijarFuegos(n: Int)

    suspend fun fijarHorno(tiene: Boolean)

    /**
     * Marca si el usuario tiene [aparato] (batidora, microondas, plancha; el horno es [fijarHorno]). Por defecto solo
     * entiende el horno; la implementación con base de datos lo sobrescribe.
     */
    suspend fun fijarEquipo(
        aparato: Equipo,
        tiene: Boolean,
    ) {
        if (aparato == Equipo.HORNO) fijarHorno(tiene)
    }

    /** [n] se ajusta a [RACIONES_MIN]..[RACIONES_MAX]. */
    suspend fun fijarRacionesDefecto(n: Int)

    /** Con `false` la intro se vuelve a ver (desde Ajustes). */
    suspend fun marcarIntroVista(vista: Boolean)

    /**
     * Fin de la intro: marca la intro como vista y [version] (`major.minor`) como novedades vistas, las dos claves en
     * una sola transacción (una instalación nueva no tiene «novedades» que enseñar). Si [version] no se entiende,
     * solo marca la intro.
     */
    suspend fun terminarIntro(version: String)

    /** Con `false` la guía del cronómetro se vuelve a ver (desde Ajustes). */
    suspend fun marcarGuiaCronoVista(vista: Boolean)

    /** Guarda el `major.minor` de [version] ("1.6.2" guarda "1.6"); si no se entiende, no hace nada. */
    suspend fun marcarNovedadesVistas(version: String)
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
