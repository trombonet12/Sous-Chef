@file:Suppress("MatchingDeclarationName")

package app.trombito.souschef.ui.comun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.NotaCocinada
import app.trombito.souschef.ui.recetas.Hace
import app.trombito.souschef.ui.recetas.texto
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.FichaPalette
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/** Colores de la tarjeta «La última vez»: se pasan de fuera para usarla en la ficha (papel) y en el cronómetro. */
data class ColoresUltimaVez(
    val fondo: Color,
    val texto: Color,
    val acento: Color,
    val apagado: Color,
) {
    companion object {
        /** Papel de la ficha. */
        val Papel =
            ColoresUltimaVez(
                fondo = FichaPalette.papel2,
                texto = FichaPalette.tinta,
                acento = FichaPalette.tinta2,
                apagado = FichaPalette.meta,
            )

        /** Paleta del cronómetro en uso (clara u oscura): carta, texto, acento y apagado ya cumplen contraste. */
        val Crono: ColoresUltimaVez
            @Composable
            @ReadOnlyComposable
            get() =
                ColoresUltimaVez(
                    fondo = CronoPalette.carta,
                    texto = CronoPalette.texto,
                    acento = CronoPalette.acento,
                    apagado = CronoPalette.apagado,
                )
    }
}

/**
 * Tarjeta discreta con la nota «para la próxima vez» más reciente de la receta y, si hay más, un botón para ver las
 * anteriores. [notas] va de la más reciente a la más antigua; si está vacía no pinta nada.
 */
@Composable
fun UltimaVez(
    notas: List<NotaCocinada>,
    colores: ColoresUltimaVez,
    modifier: Modifier = Modifier,
    hoy: LocalDate = LocalDate.now(ZoneOffset.UTC),
) {
    if (notas.isEmpty()) return
    var abierta by rememberSaveable { mutableStateOf(false) }
    val visibles = if (abierta) notas else notas.take(1)
    val anteriores = notas.size - 1
    Column(
        modifier
            .fillMaxWidth()
            .testTag("ultima-vez")
            .background(colores.fondo, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            Modifier.semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.ultima_vez_titulo),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colores.acento,
            )
            visibles.forEachIndexed { i, n ->
                Column(Modifier.testTag("ultima-vez-nota-$i")) {
                    Text(n.nota, style = MaterialTheme.typography.bodyMedium, color = colores.texto)
                    Text(fechaNota(n.fecha, hoy), style = MaterialTheme.typography.labelMedium, color = colores.apagado)
                }
            }
        }
        if (anteriores > 0) {
            TextButton(
                onClick = { abierta = !abierta },
                modifier = Modifier.heightIn(min = 48.dp).testTag("ultima-vez-mas"),
                colors = ButtonDefaults.textButtonColors(contentColor = colores.acento),
            ) {
                Text(
                    if (abierta) {
                        stringResource(R.string.ultima_vez_ver_menos)
                    } else {
                        pluralStringResource(R.plurals.ultima_vez_ver_mas, anteriores, anteriores)
                    },
                )
            }
        }
    }
}

/** «hace 3 semanas · 12 oct» a partir de una fecha ISO; si no se entiende, la deja tal cual. */
@Composable
private fun fechaNota(
    iso: String,
    hoy: LocalDate,
): String {
    val meses = stringArrayResource(R.array.ultima_vez_meses)
    val fecha =
        try {
            LocalDate.parse(iso)
        } catch (_: DateTimeParseException) {
            return iso
        }
    val corta = stringResource(R.string.ultima_vez_fecha, fecha.dayOfMonth, meses[fecha.monthValue - 1])
    return Hace.de(iso, hoy).texto() + SEPARADOR_META + corta
}
