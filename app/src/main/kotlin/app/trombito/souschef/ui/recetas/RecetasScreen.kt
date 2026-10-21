package app.trombito.souschef.ui.recetas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trombito.souschef.R
import app.trombito.souschef.ui.comun.BarraTitulo
import app.trombito.souschef.ui.comun.CategoriaVista
import app.trombito.souschef.ui.comun.EstadoVacio
import app.trombito.souschef.ui.comun.RejillaCategorias
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.Espacio

@Composable
fun RecetasScreen(
    onAbrir: (String) -> Unit,
    onCategoria: (String) -> Unit = {},
    onAtras: () -> Unit = {},
    viewModel: RecetasViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val acciones =
        remember(viewModel) {
            AccionesFiltro(viewModel::alternarFiltro, viewModel::limpiarFiltros, viewModel::elegirOrden)
        }
    val lista = AccionesLista(viewModel::alternarFavorita, onAbrir, onCategoria, onAtras)
    val tiempo =
        remember(viewModel) {
            AccionesTiempo(
                viewModel::activarTiempo,
                viewModel::salirTiempo,
                viewModel::moverServir,
                viewModel::soloLlego,
            )
        }
    RecetasContenido(estado, viewModel::buscar, acciones, lista, tiempo)
}

/** Lo que se puede hacer desde la lista: marcar favorita, abrir una receta o una familia, y volver. */
class AccionesLista(
    val favorita: (String, Boolean) -> Unit,
    val abrir: (String) -> Unit,
    val categoria: (String) -> Unit = {},
    val atras: () -> Unit = {},
)

@Composable
fun RecetasContenido(
    estado: RecetasUiState,
    onBuscar: (String) -> Unit,
    acciones: AccionesFiltro,
    lista: AccionesLista,
    tiempo: AccionesTiempo = AccionesTiempo(),
) {
    var hojaAbierta by rememberSaveable { mutableStateOf(false) }
    if (hojaAbierta) HojaFiltros(estado, acciones) { hojaAbierta = false }
    var hojaTiempo by rememberSaveable { mutableStateOf(false) }
    if (hojaTiempo) HojaQueMeDaTiempo(estado, tiempo) { hojaTiempo = false }
    val abrirTiempo = {
        if (estado.tiempo == null) tiempo.activar()
        hojaTiempo = true
    }
    val salirTiempo = {
        hojaTiempo = false
        tiempo.salir()
    }
    val familia = estado.familiaFija
    Column(Modifier.fillMaxSize()) {
        if (familia != null) BarraTitulo(textoOpcion(OpcionFiltro.Base(familia)), lista.atras)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            item {
                CabeceraRecetas(
                    estado,
                    onBuscar,
                    acciones.alternar,
                    { hojaAbierta = true },
                    { ChipTiempo(estado, abrirTiempo, salirTiempo) },
                )
            }
            val sinBusqueda = estado.consulta.isBlank() && estado.activos.isEmpty()
            if (familia == null && sinBusqueda && estado.categorias.size >= MIN_CATEGORIAS) {
                item { CategoriasFamilia(estado.categorias, lista.categoria) }
                item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            }
            cuerpo(estado, lista.favorita, lista.abrir)
        }
    }
}

private const val MIN_CATEGORIAS = 2

/** Los cuadraditos de familia de la pestaña: tocar uno abre la lista de esa familia (clave = nombre en minúsculas). */
@Composable
private fun CategoriasFamilia(
    categorias: List<Pair<FamiliaBase, Int>>,
    onCategoria: (String) -> Unit,
) {
    val vistas =
        categorias.map { (familia, n) ->
            CategoriaVista(
                clave = familia.name.lowercase(),
                nombre = textoOpcion(OpcionFiltro.Base(familia)),
                icono = familia.icono(),
                color = colorBase(familia),
                recuento = pluralStringResource(R.plurals.recetas_conteo, n, n),
            )
        }
    RejillaCategorias(vistas, onCategoria)
}

private fun FamiliaBase.icono(): Icono =
    when (this) {
        FamiliaBase.PASTA -> Icono.Pasta
        FamiliaBase.ARROZ -> Icono.Arroz
        FamiliaBase.PATATA -> Icono.Patata
        FamiliaBase.PESCADO -> Icono.Pescado
        FamiliaBase.VERDURA -> Icono.Verdura
        FamiliaBase.MASAS -> Icono.Masas
        FamiliaBase.SALTEADO -> Icono.Salteado
        FamiliaBase.OTRA -> Icono.Plato
    }

private fun LazyListScope.cuerpo(
    estado: RecetasUiState,
    onFavorita: (String, Boolean) -> Unit,
    onAbrir: (String) -> Unit,
) {
    when {
        estado.cargando -> {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() } }
        }

        estado.tarjetas.isEmpty() -> {
            item { RecetasVacio(estado) }
        }

        else -> {
            items(estado.tarjetas, key = { it.receta.slug }) { t ->
                TarjetaLista(
                    t,
                    estado.disposicion,
                    { onFavorita(t.receta.slug, !t.favorita) },
                    { onAbrir(t.receta.slug) },
                )
            }
        }
    }
}

@Composable
private fun RecetasVacio(estado: RecetasUiState) {
    if (estado.totalRecetas == 0) {
        EstadoVacio(stringResource(R.string.recetas_vacio_titulo), stringResource(R.string.recetas_vacio_detalle))
    } else if (estado.tiempo?.soloLlego == true && estado.activos.isEmpty() && estado.consulta.isBlank()) {
        EstadoVacio(stringResource(R.string.que_tiempo_vacio_titulo), stringResource(R.string.que_tiempo_vacio_detalle))
    } else {
        val detalle =
            if (estado.activos.isNotEmpty()) R.string.recetas_nada_detalle_filtro else R.string.recetas_nada_detalle
        EstadoVacio(stringResource(R.string.recetas_nada_titulo), stringResource(detalle))
    }
}

@Composable
fun Hace.texto(): String =
    when (unidad) {
        UnidadHace.SIN_ESTRENAR -> stringResource(R.string.hace_sin_estrenar)
        UnidadHace.HOY -> stringResource(R.string.hace_hoy)
        UnidadHace.AYER -> stringResource(R.string.hace_ayer)
        UnidadHace.DIAS -> pluralStringResource(R.plurals.hace_dias, cantidad, cantidad)
        UnidadHace.SEMANAS -> pluralStringResource(R.plurals.hace_semanas, cantidad, cantidad)
        UnidadHace.MESES -> pluralStringResource(R.plurals.hace_meses, cantidad, cantidad)
        UnidadHace.ANOS -> pluralStringResource(R.plurals.hace_anos, cantidad, cantidad)
    }
