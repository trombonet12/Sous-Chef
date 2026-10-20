package app.trombito.souschef.ui.crono.portada

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.ui.comun.LocalCocina
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.CronoPalette

private const val ANGULO_ABIERTO = 90f

@Composable
internal fun CabeceraChecklist(
    encabezado: EncabezadoChecklist,
    total: Progreso,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.crono_checklist_progreso, encabezado.titulo, total.hechas, total.total),
            style = MaterialTheme.typography.titleMedium.merge(Tabular),
            fontWeight = FontWeight.Bold,
            color = CronoPalette.acento,
        )
        LinearProgressIndicator(
            progress = { total.fraccion },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = CronoPalette.ok,
            trackColor = CronoPalette.linea,
        )
        if (encabezado.descripcion.isNotBlank()) DescripcionPlegable(encabezado.descripcion)
        NotaCocina()
    }
}

@Composable
private fun DescripcionPlegable(descripcion: String) {
    var entera by rememberSaveable { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                role = Role.Button,
                onClickLabel =
                    stringResource(
                        if (entera) R.string.crono_checklist_ver_menos else R.string.crono_checklist_ver_mas,
                    ),
            ) { entera = !entera },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            descripcion,
            Modifier.weight(1f),
            color = CronoPalette.apagado,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (entera) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
        )
        Chevron(abierto = entera)
    }
}

@Composable
private fun NotaCocina() {
    val nombre =
        when (LocalCocina.current.tipo) {
            TipoCocina.GAS -> R.string.ficha_cocina_gas
            TipoCocina.INDUCCION -> R.string.ficha_cocina_induccion
            TipoCocina.VITRO -> return
        }
    Text(
        stringResource(R.string.ficha_nota_cocina, stringResource(nombre)),
        color = CronoPalette.apagado,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
internal fun Chevron(abierto: Boolean) {
    Icon(
        Icono.Chevron.vector,
        contentDescription = null,
        modifier = Modifier.size(20.dp).rotate(if (abierto) ANGULO_ABIERTO else 0f),
        tint = CronoPalette.apagado,
    )
}
