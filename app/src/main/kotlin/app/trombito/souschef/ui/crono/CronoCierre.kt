package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.ResumenCrono
import app.trombito.souschef.ui.theme.CronoPalette

/** Cierre: resumen de la cocinada, nota opcional para la próxima vez y paso al historial. */
@Composable
fun CronoCierre(
    receta: Receta,
    estado: EstadoCrono,
    resumen: ResumenCrono,
    onCerrar: (String) -> Unit,
) {
    var nota by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.crono_todo_fuera), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.crono_listo_emplata, receta.titulo), color = CronoPalette.apagado)
        resumen.carriles.forEach {
            val tramo = stringResource(R.string.crono_resumen_carril, it.carril, it.pasos, it.minutos)
            val fuego = if (it.deFuego) stringResource(R.string.crono_de_fuego) else ""
            Text(tramo + fuego)
        }
        resumen.minutosReales?.let {
            Text(stringResource(R.string.crono_has_tardado, it), fontWeight = FontWeight.Bold, color = CronoPalette.ok)
        }
        Text(
            stringResource(
                R.string.crono_servido_previsto,
                formatoHora(resumen.servidoMs),
                formatoHora(estado.servirMs),
            ),
            color = CronoPalette.apagado,
        )
        Text(
            stringResource(R.string.crono_proxima_vez),
            style = MaterialTheme.typography.labelLarge,
            color = CronoPalette.acento,
        )
        OutlinedTextField(
            value = nota,
            onValueChange = { nota = it.take(MAX_NOTA) },
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("${nota.length}/$MAX_NOTA") },
        )
        Button(
            onClick = { onCerrar(nota) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = CronoPalette.principal,
                    contentColor = CronoPalette.onPrincipal,
                ),
        ) {
            Text(stringResource(R.string.crono_ya_esta), style = MaterialTheme.typography.titleMedium)
        }
    }
}
