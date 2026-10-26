package app.trombito.souschef.ui.recetas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
    onPedir: (String) -> Unit = {},
    viewModel: RecetasViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val acciones =
        remember(viewModel) {
            AccionesFiltro(viewModel::alternarFiltro, viewModel::limpiarFiltros, viewModel::elegirOrden)
        }
    val lista =
        AccionesLista(
            viewModel::alternarFavorita,
            onAbrir,
            onCategoria,
            onAtras,
            viewModel::otraPropuesta,
            onPedir,
            viewModel::propuestasVistas,
        )
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

/**
 * Lo que se puede hacer desde la lista: marcar favorita, abrir una receta o una familia, volver, pedir otras propuestas
 * (↻), anotar las que enseña el panel y, con la búsqueda vacía, pedir al Buzón lo buscado.
 */
@Suppress("LongParameterList") // un callback por gesto de la lista
class AccionesLista(
    val favorita: (String, Boolean) -> Unit,
    val abrir: (String) -> Unit,
    val categoria: (String) -> Unit = {},
    val atras: () -> Unit = {},
    val otra: () -> Unit = {},
    val pedir: (String) -> Unit = {},
    val vistas: () -> Unit = {},
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
    val enPestana = familia == null
    var panelAbierto by rememberSaveable { mutableStateOf(false) }
    if (panelAbierto && enPestana) {
        PanelPropuestas(
            propuestas = estado.propuestas,
            onAbrir = {
                panelAbierto = false
                lista.abrir(it)
            },
            onOtra = lista.otra,
            onVistas = lista.vistas,
            onCerrar = { panelAbierto = false },
        )
    }
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
                    {
                        if (enPestana) ChipPropuestas { panelAbierto = true }
                        ChipTiempo(estado, abrirTiempo, salirTiempo)
                    },
                )
            }
            val sinBusqueda = estado.consulta.isBlank() && estado.activos.isEmpty()
            if (enPestana && sinBusqueda && estado.categorias.size >= MIN_CATEGORIAS) {
                item { CategoriasFamilia(estado.categorias, lista.categoria) }
                item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            }
            cuerpo(estado, lista, acciones.limpiar)
        }
    }
}

private const val MIN_CATEGORIAS = 2

/** Los cuadraditos de la pestaña, uno por familia: tocar uno abre su lista (clave = la familia en minúsculas). */
@Composable
private fun CategoriasFamilia(
    categorias: List<Pair<FamiliaBase, Int>>,
    onCategoria: (String) -> Unit,
) {
    val familias =
        categorias.map { (familia, n) ->
            CategoriaVista(
                clave = familia.name.lowercase(),
                nombre = textoOpcion(OpcionFiltro.Base(familia)),
                icono = familia.icono(),
                color = colorBase(familia),
                recuento = pluralStringResource(R.plurals.recetas_conteo, n, n),
            )
        }
    RejillaCategorias(familias, onCategoria)
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
        FamiliaBase.CARNES -> Icono.Carnes
        FamiliaBase.POSTRES -> Icono.Postres
        FamiliaBase.OTRA -> Icono.Plato
    }

private fun LazyListScope.cuerpo(
    estado: RecetasUiState,
    lista: AccionesLista,
    onQuitarFiltros: () -> Unit,
) {
    when {
        estado.cargando -> {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() } }
        }

        estado.tarjetas.isEmpty() -> {
            item { RecetasVacio(estado, lista.pedir, onQuitarFiltros) }
        }

        else -> {
            items(estado.tarjetas, key = { it.receta.slug }) { t ->
                TarjetaLista(
                    t,
                    estado.disposicion,
                    { lista.favorita(t.receta.slug, !t.favorita) },
                    { lista.abrir(t.receta.slug) },
                )
            }
        }
    }
}

/**
 * La lista sin nada que enseñar. En «Nada con eso» (búsqueda o filtros sin resultado): «Pedir esta receta» abre el
 * Buzón con lo buscado, si se buscó algo, y «Quitar filtros» aparece si hay filtros activos.
 */
@Composable
private fun RecetasVacio(
    estado: RecetasUiState,
    onPedir: (String) -> Unit,
    onQuitarFiltros: () -> Unit,
) {
    if (estado.totalRecetas == 0) {
        EstadoVacio(stringResource(R.string.recetas_vacio_titulo), stringResource(R.string.recetas_vacio_detalle))
    } else if (estado.tiempo?.soloLlego == true && estado.activos.isEmpty() && estado.consulta.isBlank()) {
        EstadoVacio(stringResource(R.string.que_tiempo_vacio_titulo), stringResource(R.string.que_tiempo_vacio_detalle))
    } else {
        val detalle =
            if (estado.activos.isNotEmpty()) R.string.recetas_nada_detalle_filtro else R.string.recetas_nada_detalle
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            EstadoVacio(stringResource(R.string.recetas_nada_titulo), stringResource(detalle))
            if (estado.consulta.isNotBlank()) {
                Button(
                    onClick = { onPedir(estado.consulta.trim()) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("recetas-pedir"),
                ) {
                    Text(stringResource(R.string.recetas_nada_pedir))
                }
            }
            if (estado.activos.isNotEmpty()) {
                OutlinedButton(
                    onClick = onQuitarFiltros,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("recetas-quitar-filtros"),
                ) {
                    Text(stringResource(R.string.recetas_nada_quitar_filtros))
                }
            }
        }
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
