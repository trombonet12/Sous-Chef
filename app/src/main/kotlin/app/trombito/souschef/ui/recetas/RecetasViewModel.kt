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
import app.trombito.souschef.domain.PROPUESTAS_MAX
import app.trombito.souschef.domain.Preferencia
import app.trombito.souschef.domain.PreferenciasRepo
import app.trombito.souschef.domain.PropuestasRepo
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.domain.SinPropuestas
import app.trombito.souschef.domain.candidatas
import app.trombito.souschef.domain.ventana
import app.trombito.souschef.receta.Receta
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
import kotlinx.coroutines.flow.onEach
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
        /**
         * La familia de la ruta `recetas/familia/{familia}` (nombre del enum en minúsculas), o null en la pestaña. La
         * clave [Rutas.CENAS_RAPIDAS] abre la misma lista con los filtros de cena y rápida puestos.
         */
        private val familiaFija: FamiliaBase? =
            savedStateHandle.get<String>(Rutas.ARG_FAMILIA)?.let { nombre ->
                FamiliaBase.entries.firstOrNull { it.name.equals(nombre, ignoreCase = true) }
            }
        private val cenasRapidas: Boolean = savedStateHandle.get<String>(Rutas.ARG_FAMILIA) == Rutas.CENAS_RAPIDAS
        private val filtro =
            MutableStateFlow(
                FiltroRecetas(
                    activos = if (cenasRapidas) FILTROS_CENAS_RAPIDAS else emptySet(),
                    familiaFija = familiaFija,
                    cenasRapidas = cenasRapidas,
                ),
            )

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
            }.onEach { e ->
                // Lo que se enseña queda anotado para saber, al cocinar, si la receta estaba propuesta ese día.
                val dia = e.diaPropuestas
                if (dia != null && e.propuestas.isNotEmpty()) {
                    propuestasRepo.mostrar(dia, e.propuestas.map { it.receta.slug })
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecetasUiState())

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

        /** «Otra» en «¿Qué cocino hoy?»: pasa a las 3 propuestas siguientes (el contador se reinicia cada día). */
        fun otraPropuesta() {
            viewModelScope.launch { propuestasRepo.otra(diaLocal(reloj.ahoraMs())) }
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
 * Pone las propuestas del día en el estado. Solo en la pestaña sin buscar ni filtrar (donde sale la tarjeta); la
 * semilla es la fecha local, así que no cambian durante el día salvo con «Otra».
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
    val enPestana = filtro.familiaFija == null && !filtro.cenasRapidas
    if (!enPestana || filtro.consulta.isNotBlank() || filtro.activos.isNotEmpty()) return this
    val ahora = Instant.ofEpochMilli(ahoraMs).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val dia = ahora.toLocalDate()
    val todas = candidatas(recetas, historial, preferencias, ajustes, ahora, dia)
    return copy(
        propuestas = ventana(todas, ContadorPropuestas.vecesHoy(otra, dia)),
        hayOtraPropuesta = todas.size > PROPUESTAS_MAX,
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
