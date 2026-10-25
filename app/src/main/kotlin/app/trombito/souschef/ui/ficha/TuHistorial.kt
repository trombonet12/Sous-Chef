package app.trombito.souschef.ui.ficha

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.trombito.souschef.R
import app.trombito.souschef.domain.ResumenReceta
import app.trombito.souschef.domain.Valoracion
import app.trombito.souschef.ui.comun.SEPARADOR_META
import app.trombito.souschef.ui.recetas.Hace
import app.trombito.souschef.ui.recetas.texto
import app.trombito.souschef.ui.theme.FichaPalette
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Una línea con lo que el historial cuenta de esta receta: «La has cocinado 3 veces · última hace 2 semanas · sueles
 * tardar 70 min (plan 62) · valoración: Para repetir». Sin [resumen] (nunca se ha cocinado) no pinta nada.
 */
@Composable
fun TuHistorial(
    resumen: ResumenReceta?,
    modifier: Modifier = Modifier,
    hoy: LocalDate = LocalDate.now(ZoneOffset.UTC),
) {
    if (resumen == null) return
    val partes =
        listOfNotNull(
            pluralStringResource(R.plurals.historial_receta_veces, resumen.veces, resumen.veces),
            stringResource(R.string.historial_receta_ultima, Hace.de(resumen.ultimaFecha, hoy).texto()),
            resumen.tardaMin?.let { stringResource(R.string.historial_receta_tarda, it, resumen.planMin) },
            resumen.valoracion?.let {
                stringResource(R.string.historial_receta_valoracion, stringResource(it.etiqueta()))
            },
        )
    Text(
        partes.joinToString(SEPARADOR_META),
        modifier = modifier.fillMaxWidth().testTag("tu-historial"),
        style = MaterialTheme.typography.bodyMedium,
        color = FichaPalette.tinta2,
    )
}

private fun Valoracion.etiqueta(): Int =
    when (this) {
        Valoracion.REPETIR -> R.string.historial_valoracion_repetir
        Valoracion.AJUSTAR -> R.string.historial_valoracion_ajustar
        Valoracion.NO_REPETIR -> R.string.historial_valoracion_no_repetir
    }
