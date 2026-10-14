package app.souschef.ui.despensa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.souschef.domain.CompraRepo
import app.souschef.domain.DespensaRepo
import app.souschef.domain.EstadoDespensa
import app.souschef.domain.ItemDespensa
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Origen con el que se marcan los items pasados desde la despensa. */
const val ORIGEN_DESPENSA = "despensa"

private data class Controles(
    val vista: VistaDespensa = VistaDespensa.DESPENSA,
    val consulta: String = "",
    val filtro: EstadoDespensa? = null,
    val aviso: Int? = null,
)

@Suppress("TooManyFunctions") // una acción de UI por función
@HiltViewModel
class DespensaViewModel
    @Inject
    constructor(
        private val despensa: DespensaRepo,
        private val compra: CompraRepo,
    ) : ViewModel() {
        private val controles = MutableStateFlow(Controles())

        val estado: StateFlow<DespensaUiState> =
            combine(despensa.observar(), compra.observar(), controles) { items, lista, c ->
                val grupos = agruparCompra(lista)
                DespensaUiState(
                    vista = c.vista,
                    cargando = false,
                    consulta = c.consulta,
                    filtro = c.filtro,
                    totalDespensa = items.size,
                    grupos = agruparDespensa(items, c.consulta, c.filtro),
                    categorias = categoriasExistentes(items),
                    totalCompra = lista.size,
                    grupoCompra = grupos,
                    textoCompra = textoCompra(grupos),
                    hayMarcados = lista.any { it.marcado },
                    avisoPasados = c.aviso,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DespensaUiState())

        fun elegirVista(vista: VistaDespensa) = controles.update { it.copy(vista = vista) }

        fun buscar(texto: String) = controles.update { it.copy(consulta = texto) }

        fun filtrar(filtro: EstadoDespensa?) = controles.update { it.copy(filtro = filtro) }

        fun descartarAviso() = controles.update { it.copy(aviso = null) }

        fun ciclarEstado(item: ItemDespensa) {
            if (item.estado == EstadoDespensa.VETADO) return
            viewModelScope.launch { despensa.cambiarEstado(item.id, siguienteEstado(item.estado)) }
        }

        fun agregarItem(
            nombre: String,
            categoria: String,
        ) {
            val n = nombre.trim()
            if (n.isEmpty()) return
            viewModelScope.launch { despensa.agregar(n, categoria.trim().ifEmpty { CATEGORIA_POR_DEFECTO }) }
        }

        fun borrarItem(id: Long) {
            viewModelScope.launch { despensa.borrar(id) }
        }

        /** Pasa FALTA y POCO a la compra sin repetir textos ya presentes. */
        fun pasarACompra() {
            viewModelScope.launch {
                val textos = textosParaCompra(despensa.observar().first(), compra.observar().first())
                if (textos.isNotEmpty()) compra.agregarVarios(textos, ORIGEN_DESPENSA)
                controles.update { it.copy(aviso = textos.size) }
            }
        }

        fun agregarCompra(texto: String) {
            val t = texto.trim()
            if (t.isEmpty()) return
            viewModelScope.launch { compra.agregar(t) }
        }

        fun marcar(
            id: Long,
            marcado: Boolean,
        ) {
            viewModelScope.launch { compra.marcar(id, marcado) }
        }

        fun borrarCompra(id: Long) {
            viewModelScope.launch { compra.borrar(id) }
        }

        fun borrarMarcados() {
            viewModelScope.launch { compra.borrarMarcados() }
        }
    }
