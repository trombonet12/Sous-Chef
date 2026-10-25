package app.trombito.souschef.ui.crono

import androidx.annotation.StringRes
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.trombito.souschef.ui.theme.CronoPalette

/** Una entrada del menú ⋮: cierra el menú ([cerrar]) y hace su [accion]. */
@Composable
internal fun ItemMenu(
    @StringRes texto: Int,
    cerrar: () -> Unit,
    accion: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(texto), color = CronoPalette.texto) },
        onClick = {
            cerrar()
            accion()
        },
    )
}
