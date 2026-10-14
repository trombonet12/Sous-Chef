package app.souschef.ui.despensa

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
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

@Composable
internal fun VistaCompra(
    estado: DespensaUiState,
    vm: DespensaViewModel,
) {
    val contexto = LocalContext.current
    var copiada by remember { mutableStateOf(false) }
    val tituloCompartir = stringResource(R.string.compra_compartir_titulo)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoAnadirCompra(vm::agregarCompra)
                if (estado.totalCompra > 0) {
                    AccionesCompra(
                        hayMarcados = estado.hayMarcados,
                        onBorrarMarcados = vm::borrarMarcados,
                        onCopiar = {
                            copiarTexto(contexto, estado.textoCompra)
                            copiada = true
                        },
                        onCompartir = { compartirTexto(contexto, estado.textoCompra, tituloCompartir) },
                    )
                    if (copiada) Aviso(stringResource(R.string.compra_copiada)) { copiada = false }
                }
            }
        }
        if (estado.totalCompra == 0) {
            item {
                EstadoVacio(
                    stringResource(R.string.compra_vacia_titulo),
                    stringResource(R.string.compra_vacia_detalle),
                )
            }
        } else {
            estado.grupoCompra.forEach { g ->
                item(key = "cg-${g.clave}") {
                    Text(
                        g.clave.ifEmpty { stringResource(R.string.compra_sin_zona) },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 2.dp),
                    )
                }
                items(g.items, key = { "ci-${it.id}" }) { item ->
                    FilaCompra(item, vm::marcar, onBorrar = { vm.borrarCompra(item.id) })
                }
            }
        }
    }
}

@Composable
private fun CampoAnadirCompra(onAnadir: (String) -> Unit) {
    var texto by rememberSaveable { mutableStateOf("") }
    val enviar = {
        onAnadir(texto)
        texto = ""
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.compra_anadir_hint)) },
        )
        IconButton(onClick = enviar, enabled = texto.isNotBlank(), modifier = Modifier.size(48.dp)) {
            Icon(Icono.Anadir.vector, contentDescription = stringResource(R.string.compra_anadir))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccionesCompra(
    hayMarcados: Boolean,
    onBorrarMarcados: () -> Unit,
    onCopiar: () -> Unit,
    onCompartir: () -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onCopiar, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icono.Copiar.vector, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.compra_copiar), modifier = Modifier.padding(start = 6.dp))
        }
        OutlinedButton(onClick = onCompartir, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.compra_compartir))
        }
        OutlinedButton(onClick = onBorrarMarcados, enabled = hayMarcados, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.compra_borrar_marcados))
        }
    }
}

@Composable
private fun FilaCompra(
    item: ItemCompra,
    onMarcar: (Long, Boolean) -> Unit,
    onBorrar: () -> Unit,
) {
    val estadoTexto = stringResource(if (item.marcado) R.string.compra_marcado else R.string.compra_pendiente)
    val descripcion = stringResource(R.string.compra_item_desc, item.texto, estadoTexto)
    val descripcionBorrar = stringResource(R.string.despensa_borrar_desc, item.texto)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(role = Role.Checkbox) { onMarcar(item.id, !item.marcado) }
                .clearAndSetSemantics { contentDescription = descripcion },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Checkbox(checked = item.marcado, onCheckedChange = null, modifier = Modifier.padding(12.dp))
            Text(
                item.texto,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (item.marcado) TextDecoration.LineThrough else null,
                modifier = Modifier.alpha(if (item.marcado) 0.6f else 1f),
            )
        }
        IconButton(onClick = onBorrar, modifier = Modifier.size(48.dp)) {
            Icon(Icono.Borrar.vector, contentDescription = descripcionBorrar)
        }
    }
}

private fun copiarTexto(
    contexto: Context,
    texto: String,
) {
    val cm = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("compra", texto))
}

private fun compartirTexto(
    contexto: Context,
    texto: String,
    titulo: String,
) {
    val envio =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
    contexto.startActivity(Intent.createChooser(envio, titulo).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
