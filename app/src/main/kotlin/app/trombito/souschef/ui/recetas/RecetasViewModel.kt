package app.trombito.souschef.ui.recetas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.ContadorPropuestas
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.HistorialRepo
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.domain.OrdenRecetas
import app.trombito.souschef.domain.OtraDelDia
import app.trombito.souschef.domain.Preferencia
import app.trombito.souschef.domain.PreferenciasRepo
import app.trombito.souschef.domain.PropuestasRepo
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.domain.SinPropuestas
import app.trombito.souschef.domain.candidatas
import app.trombito.souschef.domain.ventana
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.CLAVE_ATAJO_TIEMPO
import app.trombito.souschef.ui.Rutas
import app.trombito.souschef.ui.ficha.Servir
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Lista de recetas. El texto y los filtros viven aquí y se pierden al cerrar la app; el orden y el diseño de las
 * tarjetas se guardan en Ajustes.
 */
@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions") // un repositorio por fuente y una función por gesto de la lista
class RecetasViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        recetas: RecetasRepo,
        private val preferencias: PreferenciasRepo,
        historial: HistorialRepo,
        private val ajustes: AjustesRepo,
        private val reloj: Reloj,
        private val propuestasRepo: PropuestasRepo = SinPropuestas,
    ) : ViewModel() {
        /** La familia de la ruta `recetas/familia/{familia}` (nombre del enum en minúsculas), o null en la pestaña. */
        private val familiaFija: FamiliaBase? =
            savedStateHandle.get<String>(Rutas.ARG_FAMILIA)?.let { nombre ->
                FamiliaBase.entries.firstOrNull { it.name.equals(nombre, ignoreCase = true) }
            }
        private val filtro = MutableStateFlow(FiltroRecetas(familiaFija = familiaFija))

        /** Lo último que se anotó como enseñado, para no repetir la misma anotación. */
        private var ultimaAnotada: Pair<LocalDate, List<String>>? = null

        val estado: StateFlow<RecetasUiState> =
            combine(
                recetas.observarTodas(),
                preferencias.observarTodas(),
                combine(
                    historial.observarUltimaFechaPorSlug(),
                    historial.observarTodo(),
                    propuestasRepo.observarOtra(),
                ) { u, t, o -> Triple(u, t, o) },
                filtro,
                ajustesYHora(),
            ) { r, p, (ultimas, entradas, otra), f, (a, ahoraMs) ->
                construirEstadoRecetas(
                    r,
                    p,
                    ultimas,
                    f.copy(orden = a.orden),
                    contextoLista(a, ahoraMs, ritmosPorSlug(r, entradas)),
                ).copy(disposicion = a.lista).conPropuestas(r, entradas, p, a, ahoraMs, otra, f)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecetasUiState())

        init {
            // Atajo «¿Qué me da tiempo?» (F17): la navegación deja una marca de una vez en el estado de esta entrada.
            viewModelScope.launch {
                savedStateHandle.getStateFlow(CLAVE_ATAJO_TIEMPO, false).collect { pedido ->
                    if (pedido) {
                        savedStateHandle[CLAVE_ATAJO_TIEMPO] = false
                        activarTiempo()
                    }
                }
            }
        }

        /**
         * Los ajustes con la hora actual. Solo en el modo «acabas» o con «¿Qué me da tiempo?» puesto hace falta que la
         * hora avance: entonces emite al cambiar cada minuto; en el otro, una vez.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        private fun ajustesYHora(): Flow<Pair<Ajustes, Long>> =
            combine(ajustes.observar(), filtro.map { it.tiempo != null }.distinctUntilChanged()) { a, conTiempo ->
                a to conTiempo
            }.flatMapLatest { (a, conTiempo) ->
                if (a.horaTarjeta == HoraTarjeta.ACABAR || conTiempo) {
                    flow {
                        while (true) {
                            val ahora = reloj.ahoraMs()
                            emit(a to ahora)
                            delay(MS_MINUTO - ahora.mod(MS_MINUTO))
                        }
                    }
                } else {
                    flowOf(a to reloj.ahoraMs())
                }
            }

        fun buscar(texto: String) {
            filtro.update { it.copy(consulta = texto) }
        }

        fun alternarFiltro(opcion: OpcionFiltro) {
            filtro.update { f -> f.copy(activos = if (opcion in f.activos) f.activos - opcion else f.activos + opcion) }
        }

        fun limpiarFiltros() {
            filtro.update { it.copy(activos = emptySet()) }
        }

        /** Enciende «¿Qué me da tiempo?» con la hora de servir de Ajustes; si ya está puesto no cambia nada. */
        fun activarTiempo() {
            viewModelScope.launch {
                val servir = ajustes.observar().first().servirMin
                filtro.update { it.copy(tiempo = it.tiempo ?: ModoTiempo(servir)) }
            }
        }

        fun salirTiempo() {
            filtro.update { it.copy(tiempo = null) }
        }

        /** Mueve la hora de servir [delta] minutos (±15), sin salirse del día. */
        fun moverServir(delta: Int) {
            filtro.update { f ->
                f.copy(tiempo = f.tiempo?.let { it.copy(servirMin = Servir.ajustar(it.servirMin, delta)) })
            }
        }

        fun soloLlego(activo: Boolean) {
            filtro.update { f -> f.copy(tiempo = f.tiempo?.copy(soloLlego = activo)) }
        }

        fun elegirOrden(orden: OrdenRecetas) {
            viewModelScope.launch { ajustes.fijarOrden(orden) }
        }

        /**
         * El ↻ del panel «¿Qué cocino hoy?»: pasa a las 3 propuestas siguientes (el contador se reinicia cada día).
         * Si no hay más candidatas que las 3 de ahora, se quedan las mismas.
         */
        fun otraPropuesta() {
            viewModelScope.launch { propuestasRepo.otra(diaLocal(reloj.ahoraMs())) }
        }

        /**
         * El panel «¿Qué cocino hoy?» enseña las propuestas de ahora (al abrirse y tras cada ↻): quedan anotadas para
         * saber, al cocinar, si la receta estaba propuesta ese día. Pintar la lista sin abrir el panel no anota nada.
         */
        fun propuestasVistas() {
            val e = estado.value
            val dia = e.diaPropuestas ?: return
            val slugs = e.propuestas.map { it.receta.slug }
            if (slugs.isEmpty() || ultimaAnotada == dia to slugs) return
            ultimaAnotada = dia to slugs
            viewModelScope.launch { propuestasRepo.mostrar(dia, slugs) }
        }

        fun alternarFavorita(
            slug: String,
            favorita: Boolean,
        ) {
            viewModelScope.launch { preferencias.fijarFavorito(slug, favorita) }
        }

        private companion object {
            const val MS_MINUTO = 60_000L
        }
    }

private const val MIN_HORA = 60

private fun diaLocal(ahoraMs: Long): LocalDate =
    Instant
        .ofEpochMilli(ahoraMs)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

/**
 * Pone las propuestas del día en el estado. Solo en la pestaña (donde sale el chip), con o sin búsqueda y filtros: el
 * panel no depende de la lista. La semilla es la fecha local, así que no cambian durante el día salvo con el ↻.
 */
@Suppress("LongParameterList") // todo lo que mira la función pura, ya leído por el flujo
private fun RecetasUiState.conPropuestas(
    recetas: List<Receta>,
    historial: List<EntradaHistorial>,
    preferencias: Map<String, Preferencia>,
    ajustes: Ajustes,
    ahoraMs: Long,
    otra: OtraDelDia?,
    filtro: FiltroRecetas,
): RecetasUiState {
    if (filtro.familiaFija != null) return this
    val ahora = Instant.ofEpochMilli(ahoraMs).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val dia = ahora.toLocalDate()
    val todas = candidatas(recetas, historial, preferencias, ajustes, ahora, dia)
    return copy(
        propuestas = ventana(todas, ContadorPropuestas.vecesHoy(otra, dia)),
        diaPropuestas = dia,
    )
}

private fun contextoLista(
    a: Ajustes,
    ahoraMs: Long,
    ritmos: Map<String, Int>,
): ContextoLista {
    val local = Instant.ofEpochMilli(ahoraMs).atZone(ZoneId.systemDefault())
    return ContextoLista(
        hoy = Instant.ofEpochMilli(ahoraMs).atZone(ZoneOffset.UTC).toLocalDate(),
        ahoraMin = local.hour * MIN_HORA + local.minute,
        servirMin = a.servirMin,
        cenarMin = a.cenarMin,
        modo = a.horaTarjeta,
        fuegos = a.fuegos,
        horno = a.horno,
        equipo = a.equipo,
        ritmos = ritmos,
    )
}
