package app.trombito.souschef.ui.recetas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.MotivoPropuesta
import app.trombito.souschef.domain.Propuesta
import app.trombito.souschef.ui.ficha.Servir
import app.trombito.souschef.ui.theme.Espacio

/**
 * «¿Qué cocino hoy?»: hasta 3 recetas que llegan a la hora, cada una con su motivo y «empieza HH:mm». Tocar una abre
 * su ficha; «Otra» pasa a las siguientes (solo si hay más candidatas). Sin propuestas no pinta nada.
 */
@Composable
fun TarjetaPropuestas(
    propuestas: List<Propuesta>,
    hayOtra: Boolean,
    onAbrir: (String) -> Unit,
    onOtra: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (propuestas.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth().testTag("propuestas"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(Modifier.padding(vertical = Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
            Row(
                Modifier.fillMaxWidth().padding(start = Espacio.l, end = Espacio.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.propuestas_titulo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (hayOtra) {
                    TextButton(onClick = onOtra, modifier = Modifier.testTag("propuestas-otra")) {
                        Text(stringResource(R.string.propuestas_otra))
                    }
                }
            }
            propuestas.forEach { FilaPropuesta(it, onAbrir) }
        }
    }
}

@Composable
private fun FilaPropuesta(
    propuesta: Propuesta,
    onAbrir: (String) -> Unit,
) {
    val slug = propuesta.receta.slug
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onAbrir(slug) }
            .padding(horizontal = Espacio.l, vertical = Espacio.xs)
            .testTag("propuesta-$slug"),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            propuesta.receta.titulo,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val empieza = stringResource(R.string.lista_empieza, Servir.formato(propuesta.empiezaMin))
        Text(
            stringResource(R.string.propuesta_detalle, propuesta.motivo.textoMotivo(), empieza),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** El motivo en una frase corta: «Quieres hacerla», «Sin estrenar», «Hace 3 semanas» o «Llega para cenar». */
@Composable
fun MotivoPropuesta.textoMotivo(): String =
    when (this) {
        MotivoPropuesta.QuieresHacerla -> stringResource(R.string.propuesta_quieres)
        MotivoPropuesta.SinEstrenar -> stringResource(R.string.propuesta_sin_estrenar)
        MotivoPropuesta.LlegaParaCenar -> stringResource(R.string.propuesta_cenar)
        is MotivoPropuesta.HaceTiempo -> Hace.deDias(dias).texto().replaceFirstChar { it.uppercase() }
    }
