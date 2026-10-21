package app.trombito.souschef.ui.bienvenida

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.ui.comun.EstadoPermisos
import app.trombito.souschef.ui.comun.PermisoApp

/** 11. Los tres permisos del cronómetro: cada uno con su estado (✓ o «Permitir»). */
@Composable
internal fun PaginaPermisos(
    permisos: EstadoPermisos,
    onPermitir: (PermisoApp) -> Unit,
) {
    PaginaMarco(PaginaIntro.PERMISOS) {
        FilaPermiso(
            PermisoApp.NOTIFICACIONES,
            R.string.intro_perm_notificaciones,
            R.string.intro_perm_notificaciones_texto,
            permisos,
            onPermitir,
        )
        FilaPermiso(
            PermisoApp.ALARMA_EXACTA,
            R.string.intro_perm_alarma,
            R.string.intro_perm_alarma_texto,
            permisos,
            onPermitir,
        )
        FilaPermiso(
            PermisoApp.PANTALLA_COMPLETA,
            R.string.intro_perm_pantalla,
            R.string.intro_perm_pantalla_texto,
            permisos,
            onPermitir,
        )
    }
}

@Composable
private fun FilaPermiso(
    permiso: PermisoApp,
    titulo: Int,
    detalle: Int,
    permisos: EstadoPermisos,
    onPermitir: (PermisoApp) -> Unit,
) {
    val concedido = permisos.concedido(permiso)
    val estado = stringResource(if (concedido) R.string.intro_perm_concedido else R.string.intro_perm_falta)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag("permiso-${permiso.name}")
            .semantics(mergeDescendants = true) { stateDescription = estado },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(detalle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (concedido) {
            Text(
                stringResource(R.string.intro_perm_marca),
                Modifier.padding(horizontal = 12.dp).testTag("permiso-ok-${permiso.name}").clearAndSetSemantics {},
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            OutlinedButton(
                onClick = { onPermitir(permiso) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("permiso-pedir-${permiso.name}"),
            ) { Text(stringResource(R.string.intro_perm_permitir)) }
        }
    }
}
