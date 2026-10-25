package app.trombito.souschef.ui.ficha

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.theme.FichaPalette

/** Los dos botones de compartir de la pestaña Ingredientes: la lista y «Hoy comemos». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilaCompartir(
    receta: Receta,
    estado: FichaUiState,
    porZona: Boolean,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BotonCompartir(receta, estado.raciones, porZona)
        BotonHoyComemos(receta, estado)
    }
}

/** Manda como texto plano la lista que se ve (según «Todo junto» / «Por zona» y las raciones elegidas). */
@Composable
private fun BotonCompartir(
    receta: Receta,
    raciones: Int,
    porZona: Boolean,
) {
    val contexto = LocalContext.current
    val descripcion = stringResource(R.string.ficha_compartir_ingredientes_descripcion)
    OutlinedButton(
        onClick = { compartirIngredientes(contexto, receta, raciones, porZona) },
        modifier =
            Modifier
                .padding(top = 8.dp)
                .heightIn(min = 48.dp)
                .testTag("compartir-ingredientes")
                .semantics { contentDescription = descripcion },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = FichaPalette.tinta),
        border = BorderStroke(1.dp, FichaPalette.tinta3),
    ) { Text(stringResource(R.string.ficha_compartir_ingredientes)) }
}

/** Manda «Hoy comemos/cenamos: <plato> a las HH:mm. N raciones.» con la hora de servir y las raciones de la ficha. */
@Composable
private fun BotonHoyComemos(
    receta: Receta,
    estado: FichaUiState,
) {
    val contexto = LocalContext.current
    val descripcion = stringResource(R.string.ficha_compartir_hoy_descripcion)
    OutlinedButton(
        onClick = { compartirHoyComemos(contexto, receta, estado) },
        modifier =
            Modifier
                .padding(top = 8.dp)
                .heightIn(min = 48.dp)
                .testTag("compartir-hoy-comemos")
                .semantics { contentDescription = descripcion },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = FichaPalette.tinta),
        border = BorderStroke(1.dp, FichaPalette.tinta3),
    ) { Text(stringResource(R.string.ficha_compartir_hoy)) }
}

private fun compartirIngredientes(
    contexto: Context,
    receta: Receta,
    raciones: Int,
    porZona: Boolean,
) {
    val texto = textoIngredientes(receta, raciones, porZona) { textoRaciones(contexto, it) }
    enviarTexto(contexto, texto, R.string.ficha_compartir_titulo)
}

private fun compartirHoyComemos(
    contexto: Context,
    receta: Receta,
    estado: FichaUiState,
) {
    val cenando = esCena(estado.servirMin, estado.paraCenar)
    val texto =
        textoHoyComemos(receta, estado.servirMin, estado.raciones, cenando) { textoRaciones(contexto, it) }
    enviarTexto(contexto, texto, R.string.ficha_compartir_hoy_titulo)
}

private fun textoRaciones(
    contexto: Context,
    n: Int,
): String = "$n " + contexto.resources.getQuantityString(R.plurals.ficha_unidad_racion, n)

private fun enviarTexto(
    contexto: Context,
    texto: String,
    @StringRes titulo: Int,
) {
    val envio =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
    val selector = Intent.createChooser(envio, contexto.getString(titulo))
    if (contexto !is Activity) selector.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    contexto.startActivity(selector)
}
