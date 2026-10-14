package app.souschef.ui.despensa

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.souschef.R
import app.souschef.domain.EstadoDespensa
import app.souschef.domain.ItemCompra
import app.souschef.domain.ItemDespensa
import app.souschef.ui.comun.CampoBusqueda
import app.souschef.ui.comun.EstadoVacio
import app.souschef.ui.comun.Etiqueta
import app.souschef.ui.comun.Tono
import app.souschef.ui.shell.Icono
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DespensaPantalla(viewModel: DespensaViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        val vistas = VistaDespensa.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            vistas.forEachIndexed { i, v ->
                SegmentedButton(
                    selected = estado.vista == v,
                    onClick = { viewModel.elegirVista(v) },
                    shape = SegmentedButtonDefaults.itemShape(i, vistas.size),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        stringResource(
                            if (v ==
                                VistaDespensa.DESPENSA
                            ) {
                                R.string.despensa_vista_despensa
                            } else {
                                R.string.despensa_vista_compra
                            },
                        ),
                    )
                }
            }
        }
        when {
            estado.cargando -> {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() }
            }

            estado.vista == VistaDespensa.DESPENSA -> {
                VistaDespensaLista(estado, viewModel)
            }

            else -> {
                VistaCompra(estado, viewModel)
            }
        }
    }
}

@Composable
private fun etiquetaEstado(estado: EstadoDespensa): String =
    stringResource(
        when (estado) {
            EstadoDespensa.HAY -> R.string.despensa_estado_hay
            EstadoDespensa.POCO -> R.string.despensa_estado_poco
            EstadoDespensa.FALTA -> R.string.despensa_estado_falta
            EstadoDespensa.VETADO -> R.string.despensa_estado_vetado
        },
    )

@Composable
private fun VistaDespensaLista(
    estado: DespensaUiState,
    vm: DespensaViewModel,
) {
    var dialogo by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoBusqueda(estado.consulta, stringResource(R.string.despensa_buscar), vm::buscar)
                FiltroEstados(estado.filtro, vm::filtrar)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { dialogo = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icono.Anadir.vector, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.despensa_anadir), modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(
                        onClick = vm::pasarACompra,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.despensa_pasar_compra), maxLines = 2) }
                }
                BotonCompartirDespensa(vm)
                estado.avisoPasados?.let { n -> Aviso(textoPasados(n), vm::descartarAviso) }
            }
        }
        cuerpoDespensa(estado, vm)
    }
    if (dialogo) {
        DialogoAnadir(
            categorias = estado.categorias,
            onCancelar = { dialogo = false },
            onGuardar = { n, c ->
                vm.agregarItem(n, c)
                dialogo = false
            },
        )
    }
}

@Composable
private fun BotonCompartirDespensa(vm: DespensaViewModel) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val titulo = stringResource(R.string.despensa_texto_titulo, LocalDate.now().toString())
    val tituloEnvio = stringResource(R.string.despensa_compartir_titulo)
    val etiquetas = EstadoDespensa.entries.associateWith { etiquetaEstado(it) }
    OutlinedButton(
        onClick = {
            alcance.launch {
                val texto = textoDespensa(vm.itemsParaCompartir(), titulo) { etiquetas.getValue(it) }
                compartirTexto(contexto, texto, tituloEnvio)
            }
        },
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Text(stringResource(R.string.despensa_compartir))
    }
}

@Composable
private fun textoPasados(n: Int): String =
    if (n == 0) {
        stringResource(R.string.despensa_aviso_ninguno)
    } else {
        pluralStringResource(R.plurals.despensa_aviso_pasados, n, n)
    }

@Composable
internal fun Aviso(
    texto: String,
    onCerrar: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onCerrar, modifier = Modifier.size(48.dp)) {
            Icon(Icono.Cerrar.vector, contentDescription = stringResource(R.string.despensa_aviso_cerrar))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltroEstados(
    activo: EstadoDespensa?,
    onElegir: (EstadoDespensa?) -> Unit,
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = activo == null,
            onClick = { onElegir(null) },
            label = { Text(stringResource(R.string.despensa_filtro_todos)) },
        )
        EstadoDespensa.entries.forEach { e ->
            FilterChip(
                selected = activo == e,
                onClick = { onElegir(if (activo == e) null else e) },
                label = { Text(etiquetaEstado(e)) },
            )
        }
    }
}

private fun LazyListScope.cuerpoDespensa(
    estado: DespensaUiState,
    vm: DespensaViewModel,
) {
    when {
        estado.totalDespensa == 0 -> {
            item {
                EstadoVacio(
                    stringResource(R.string.despensa_vacia_titulo),
                    stringResource(R.string.despensa_vacia_detalle),
                )
            }
        }

        estado.grupos.isEmpty() -> {
            item { EstadoVacio(stringResource(R.string.despensa_nada), null) }
        }

        else -> {
            estado.grupos.forEach { g ->
                item(key = "g-${g.categoria}-${g.esVetados}") {
                    Text(
                        if (g.esVetados) stringResource(R.string.despensa_vetados) else g.categoria,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 2.dp),
                    )
                }
                items(g.items, key = { "i-${it.id}" }) { item ->
                    FilaDespensa(item, onCiclar = { vm.ciclarEstado(item) }, onBorrar = { vm.borrarItem(item.id) })
                }
            }
        }
    }
}

@Composable
private fun FilaDespensa(
    item: ItemDespensa,
    onCiclar: () -> Unit,
    onBorrar: () -> Unit,
) {
    val vetado = item.estado == EstadoDespensa.VETADO
    val estadoTexto = etiquetaEstado(item.estado)
    val descripcion =
        if (vetado) {
            stringResource(R.string.despensa_item_vetado_desc, item.nombre)
        } else {
            stringResource(R.string.despensa_item_desc, item.nombre, estadoTexto)
        }
    val descripcionBorrar = stringResource(R.string.despensa_borrar_desc, item.nombre)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .then(if (vetado) Modifier else Modifier.clickable(role = Role.Button, onClick = onCiclar))
                .clearAndSetSemantics { contentDescription = descripcion },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                item.nombre,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (vetado) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f).alpha(if (vetado) 0.6f else 1f),
            )
            Etiqueta(
                estadoTexto,
                tono =
                    when (item.estado) {
                        EstadoDespensa.HAY -> Tono.Oliva
                        EstadoDespensa.POCO -> Tono.Ambar
                        EstadoDespensa.FALTA -> Tono.Teja
                        EstadoDespensa.VETADO -> Tono.Neutro
                    },
            )
        }
        IconButton(onClick = onBorrar, modifier = Modifier.size(48.dp)) {
            Icon(Icono.Borrar.vector, contentDescription = descripcionBorrar)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoAnadir(
    categorias: List<String>,
    onCancelar: () -> Unit,
    onGuardar: (String, String) -> Unit,
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.despensa_anadir_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.despensa_nombre)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text(stringResource(R.string.despensa_categoria_nueva)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (categorias.isNotEmpty()) {
                    Text(stringResource(R.string.despensa_categoria), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categorias.forEach { c ->
                            FilterChip(
                                selected = categoria.trim().equals(c, ignoreCase = true),
                                onClick = { categoria = c },
                                label = { Text(c) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(nombre, categoria) }, enabled = nombre.isNotBlank()) {
                Text(stringResource(R.string.despensa_guardar))
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.despensa_cancelar)) } },
    )
}
