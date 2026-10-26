package app.trombito.souschef.ui.recetas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.trombito.souschef.R
import app.trombito.souschef.domain.MotivoPropuesta
import app.trombito.souschef.domain.Propuesta
import app.trombito.souschef.ui.ficha.Servir
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.Espacio

private val ANCHO_MAX_PANEL = 480.dp
private val SOMBRA_PANEL = 8.dp
private val MINIMO_TACTIL = 48.dp

/** Chip «¿Qué cocino hoy?», junto al de «¿Qué me da tiempo?» y con su mismo estilo: abre el [PanelPropuestas]. */
@Composable
internal fun ChipPropuestas(onAbrir: () -> Unit) {
    AssistChip(
        onClick = onAbrir,
        label = { Text(stringResource(R.string.propuestas_titulo)) },
        leadingIcon = { Icon(Icono.Plato.vector, contentDescription = null) },
        modifier = Modifier.testTag("chip-propuestas"),
    )
}

/**
 * Panel flotante de «¿Qué cocino hoy?»: hasta 3 recetas que llegan a la hora, cada una con su motivo y «empieza
 * HH:mm». Tocar una la abre ([onAbrir]; quien llama cierra el panel); el ↻ pide otras ([onOtra]) y sale siempre: si no
 * hay más candidatas se quedan las mismas. Sin propuestas dice que hoy no hay nada que llegue a tiempo.
 *
 * Es un `Dialog` de tarjeta (no un `Popup` anclado bajo el chip): el diálogo es una ventana modal que TalkBack anuncia
 * y donde atrapa el foco, se cierra con Atrás o tocando fuera sin código extra, y en horizontal o con letra grande se
 * centra y la tarjeta se desplaza, mientras que un `Popup` anclado se saldría de la pantalla.
 * [onVistas] avisa al abrirse y cada vez que cambian las propuestas mostradas (tras el ↻), para anotarlas.
 */
@Composable
fun PanelPropuestas(
    propuestas: List<Propuesta>,
    onAbrir: (String) -> Unit,
    onOtra: () -> Unit,
    onVistas: () -> Unit,
    onCerrar: () -> Unit,
) {
    val titulo = stringResource(R.string.propuestas_titulo)
    LaunchedEffect(propuestas.map { it.receta.slug }) { onVistas() }
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier =
                Modifier
                    .padding(horizontal = Espacio.l)
                    .widthIn(max = ANCHO_MAX_PANEL)
                    .fillMaxWidth()
                    .testTag("panel-propuestas")
                    .semantics { paneTitle = titulo },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = SOMBRA_PANEL,
            shadowElevation = SOMBRA_PANEL,
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(vertical = Espacio.s),
                verticalArrangement = Arrangement.spacedBy(Espacio.xs),
            ) {
                CabeceraPanel(titulo, onOtra)
                if (propuestas.isEmpty()) {
                    Text(
                        stringResource(R.string.propuestas_vacio),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Espacio.l, vertical = Espacio.m),
                    )
                } else {
                    propuestas.forEach { FilaPropuesta(it, onAbrir) }
                }
            }
        }
    }
}

@Composable
private fun CabeceraPanel(
    titulo: String,
    onOtra: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = Espacio.l, end = Espacio.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            titulo,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        // Icono no tiene un «recargar»: la flecha curva de Deshacer es la que más se le parece.
        IconButton(onClick = onOtra, modifier = Modifier.heightIn(min = MINIMO_TACTIL).testTag("propuestas-otra")) {
            Icon(Icono.Reiniciar.vector, contentDescription = stringResource(R.string.propuestas_otras))
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
            .heightIn(min = MINIMO_TACTIL)
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
