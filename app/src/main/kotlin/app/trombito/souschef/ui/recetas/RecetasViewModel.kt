package app.trombito.souschef.ui.recetas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.HistorialRepo
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.domain.OrdenRecetas
import app.trombito.souschef.domain.PreferenciasRepo
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.Reloj
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Lista de recetas. El texto y los filtros viven aquí y se pierden al cerrar la app; el orden y el diseño de las
 * tarjetas se guardan en Ajustes.
 */
@HiltViewModel
class RecetasViewModel
    @Inject
    constructor(
        recetas: RecetasRepo,
        private val preferencias: PreferenciasRepo,
        historial: HistorialRepo,
        private val ajustes: AjustesRepo,
        private val reloj: Reloj,
    ) : ViewModel() {
        private val filtro = MutableStateFlow(FiltroRecetas())

        val estado: StateFlow<RecetasUiState> =
            combine(
                recetas.observarTodas(),
                preferencias.observarTodas(),
                historial.observarUltimaFechaPorSlug(),
                filtro,
                ajustesYHora(),
            ) { r, p, h, f, (a, ahoraMs) ->
                construirEstadoRecetas(
                    r,
                    p,
                    h,
                    f.copy(orden = a.orden),
                    contexto(a, ahoraMs),
                ).copy(disposicion = a.lista)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecetasUiState())

        /**
         * Los ajustes con la hora actual. Solo en el modo «acabas» hace falta que la hora avance: entonces emite al
         * cambiar cada minuto; en el otro, una vez.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        private fun ajustesYHora(): Flow<Pair<Ajustes, Long>> =
            ajustes.observar().flatMapLatest { a ->
                if (a.horaTarjeta == HoraTarjeta.ACABAR) {
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

        private fun contexto(
            a: Ajustes,
            ahoraMs: Long,
        ): ContextoLista {
            val local = Instant.ofEpochMilli(ahoraMs).atZone(ZoneId.systemDefault())
            return ContextoLista(
                hoy = Instant.ofEpochMilli(ahoraMs).atZone(ZoneOffset.UTC).toLocalDate(),
                ahoraMin = local.hour * MIN_HORA + local.minute,
                servirMin = a.servirMin,
                modo = a.horaTarjeta,
            )
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

        fun elegirOrden(orden: OrdenRecetas) {
            viewModelScope.launch { ajustes.fijarOrden(orden) }
        }

        fun alternarFavorita(
            slug: String,
            favorita: Boolean,
        ) {
            viewModelScope.launch { preferencias.fijarFavorito(slug, favorita) }
        }

        private companion object {
            const val MS_MINUTO = 60_000L
            const val MIN_HORA = 60
        }
    }
