package app.trombito.souschef.ui.ficha

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.trombito.souschef.R
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.comun.DensidadPlan
import app.trombito.souschef.ui.comun.DiagramaPlan
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.FichaPalette

/**
 * El diagrama completo de la ficha: el mismo dibujo del Resumen en densidad de detalle (cada barra con el número de su
 * paso, el perfil de fuego y la lista numerada debajo), a pantalla completa y sobre papel. Con [inicioPlanMin] el eje
 * lleva la hora real.
 */
@Composable
fun DialogoDiagramaFicha(
    receta: Receta,
    inicioPlanMin: Int?,
    onCerrar: () -> Unit,
) {
    Dialog(onCerrar, DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = FichaPalette.papel, contentColor = FichaPalette.tinta) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.ficha_diagrama_titulo),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onCerrar) {
                        Icon(Icono.Cerrar.vector, stringResource(R.string.ficha_diagrama_cerrar))
                    }
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    DiagramaPlan(receta, DensidadPlan.DETALLE, inicioPlanMin = inicioPlanMin)
                }
            }
        }
    }
}
