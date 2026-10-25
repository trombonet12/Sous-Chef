package app.trombito.souschef.ui.historial

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import app.trombito.souschef.R
import app.trombito.souschef.domain.MAX_NOTA

/** Diálogo con el campo de nota de una cocinada ya guardada; el mismo límite [MAX_NOTA] que al cerrar la cocina. */
@Composable
fun DialogoNota(
    titulo: String,
    notaInicial: String,
    onGuardar: (String) -> Unit,
    onCancelar: () -> Unit,
) {
    var nota by rememberSaveable { mutableStateOf(notaInicial.take(MAX_NOTA)) }
    val teclado = LocalSoftwareKeyboardController.current
    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Column {
                Text(stringResource(R.string.historial_nota_titulo))
                Text(titulo, style = MaterialTheme.typography.bodyMedium)
            }
        },
        text = {
            OutlinedTextField(
                value = nota,
                onValueChange = { nota = it.take(MAX_NOTA) },
                modifier = Modifier.fillMaxWidth().testTag("nota-editar"),
                label = { Text(stringResource(R.string.crono_proxima_vez)) },
                minLines = 1,
                maxLines = 4,
                keyboardOptions =
                    KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { teclado?.hide() }),
                supportingText = { Text("${nota.length}/$MAX_NOTA") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(nota) }, modifier = Modifier.testTag("nota-guardar")) {
                Text(stringResource(R.string.historial_nota_guardar))
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.historial_nota_cancelar)) } },
    )
}
