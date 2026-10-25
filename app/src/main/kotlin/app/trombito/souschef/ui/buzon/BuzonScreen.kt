package app.trombito.souschef.ui.buzon

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trombito.souschef.R
import app.trombito.souschef.domain.ResumenUso
import app.trombito.souschef.ui.ajustes.PantallaAjustes
import app.trombito.souschef.ui.ajustes.resumenCocina
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.Espacio

private const val MAX_RACIONES = 2
private const val LINEAS_MIN = 3

/** Lo que hace el Buzón fuera de la pantalla: volver, editar el formulario y mandar o copiar el correo. */
class AccionesBuzon(
    val atras: () -> Unit,
    val editar: ((FormularioBuzon) -> FormularioBuzon) -> Unit,
    /** Abre la app de correo; false si no hay ninguna. */
    val enviar: (Correo) -> Boolean,
    val copiar: (Correo) -> Unit,
)

@Composable
fun BuzonScreen(
    onAtras: () -> Unit,
    viewModel: BuzonViewModel = hiltViewModel(),
) {
    val formulario by viewModel.formulario.collectAsStateWithLifecycle()
    val ajustes by viewModel.ajustes.collectAsStateWithLifecycle()
    val resumen by viewModel.resumen.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val version = viewModel.version
    val cocina = resumenCocina(ajustes)
    val datos =
        remember(version, cocina, resumen) {
            DatosApp(version, Build.MODEL, Build.VERSION.RELEASE, Build.VERSION.SDK_INT, cocina, resumen)
        }
    val acciones =
        remember(viewModel, contexto, onAtras) {
            AccionesBuzon(
                atras = onAtras,
                editar = viewModel::editar,
                enviar = { enviarCorreo(contexto, it) },
                copiar = { copiarCorreo(contexto, it) },
            )
        }
    BuzonContenido(formulario, datos, acciones)
}

/** Pantalla sin ViewModel ni contexto: formulario y datos llegan hechos. */
@Composable
internal fun BuzonContenido(
    formulario: FormularioBuzon,
    datos: DatosApp,
    acciones: AccionesBuzon,
) {
    val recursos = LocalResources.current
    var sinCorreo by rememberSaveable { mutableStateOf(false) }
    var copiado by rememberSaveable { mutableStateOf(false) }
    PantallaAjustes(stringResource(R.string.buzon_titulo), acciones.atras) {
        Text(
            stringResource(R.string.buzon_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SelectorTipo(formulario.tipo) { tipo -> acciones.editar { it.copy(tipo = tipo) } }
        when (formulario.tipo) {
            TipoBuzon.RECETA -> CamposReceta(formulario, acciones.editar)
            TipoBuzon.FALLO -> CamposFallo(formulario, acciones.editar)
            TipoBuzon.SUGERENCIA -> CamposSugerencia(formulario, acciones.editar)
        }
        CasillaResumen(formulario.adjuntarResumen, datos.resumen) { marcada ->
            acciones.editar { it.copy(adjuntarResumen = marcada) }
        }
        Button(
            onClick = {
                val correo = construirCorreo(formulario, datos) { recursos.getString(it) }
                sinCorreo = !acciones.enviar(correo)
                copiado = false
            },
            enabled = formulario.valido,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(top = Espacio.s)
                    .testTag("buzon-enviar"),
        ) {
            Icon(Icono.Correo.vector, contentDescription = null)
            Text(stringResource(R.string.buzon_enviar), modifier = Modifier.padding(start = Espacio.s))
        }
        if (sinCorreo) {
            AvisoSinCorreo(copiado) {
                acciones.copiar(construirCorreo(formulario, datos) { recursos.getString(it) })
                copiado = true
            }
        }
    }
}

/**
 * «Adjuntar resumen de uso» (F6): desmarcada por defecto; marcada, enseña el texto exacto que se añade al correo. Sin
 * cifras todavía ([resumen] nulo), la casilla se ve pero no hay vista previa.
 */
@Composable
private fun CasillaResumen(
    marcada: Boolean,
    resumen: ResumenUso?,
    onCambio: (Boolean) -> Unit,
) {
    val recursos = LocalResources.current
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(value = marcada, role = Role.Checkbox, onValueChange = onCambio)
                    .testTag("buzon-resumen-casilla"),
        ) {
            Checkbox(checked = marcada, onCheckedChange = null)
            Text(
                stringResource(R.string.buzon_resumen_adjuntar),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = Espacio.s),
            )
        }
        if (marcada && resumen != null) {
            Text(
                stringResource(R.string.buzon_resumen_explica),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().testTag("buzon-resumen-vista"),
            ) {
                Text(
                    textoResumen(resumen) { recursos.getString(it) },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(Espacio.m),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorTipo(
    actual: TipoBuzon,
    onElegir: (TipoBuzon) -> Unit,
) {
    val descripcion = stringResource(R.string.buzon_tipo_descripcion)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().semantics { contentDescription = descripcion }) {
        TipoBuzon.entries.forEachIndexed { indice, tipo ->
            SegmentedButton(
                selected = tipo == actual,
                onClick = { onElegir(tipo) },
                shape = SegmentedButtonDefaults.itemShape(indice, TipoBuzon.entries.size),
                modifier = Modifier.testTag("buzon-tipo-${tipo.name.lowercase()}"),
            ) {
                Text(stringResource(tipo.etiqueta), maxLines = 2, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CamposReceta(
    f: FormularioBuzon,
    editar: ((FormularioBuzon) -> FormularioBuzon) -> Unit,
) {
    Campo(f.plato, R.string.buzon_plato, "buzon-plato") { v -> editar { it.copy(plato = v) } }
    Campo(f.ingredientes, R.string.buzon_ingredientes, "buzon-ingredientes", multilinea = true) { v ->
        editar { it.copy(ingredientes = v) }
    }
    Text(stringResource(R.string.buzon_tiempo_max), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
        TiempoMaximo.entries.forEach { t ->
            FilterChip(
                selected = f.tiempo == t,
                onClick = { editar { it.copy(tiempo = if (it.tiempo == t) null else t) } },
                label = { Text(stringResource(t.etiqueta)) },
                modifier = Modifier.testTag("buzon-tiempo-${t.name.lowercase()}"),
            )
        }
    }
    OutlinedTextField(
        value = f.raciones,
        onValueChange = { v -> editar { it.copy(raciones = v.filter(Char::isDigit).take(MAX_RACIONES)) } },
        label = { Text(stringResource(R.string.buzon_raciones)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().testTag("buzon-raciones"),
    )
}

@Composable
private fun CamposFallo(
    f: FormularioBuzon,
    editar: ((FormularioBuzon) -> FormularioBuzon) -> Unit,
) {
    Campo(f.queOcurrio, R.string.buzon_que_paso, "buzon-que-paso", multilinea = true) { v ->
        editar { it.copy(queOcurrio = v) }
    }
    Campo(f.queHacias, R.string.buzon_que_hacias, "buzon-que-hacias", multilinea = true) { v ->
        editar { it.copy(queHacias = v) }
    }
    DesplegablePantalla(f.pantalla) { p -> editar { it.copy(pantalla = p) } }
}

@Composable
private fun CamposSugerencia(
    f: FormularioBuzon,
    editar: ((FormularioBuzon) -> FormularioBuzon) -> Unit,
) {
    Campo(f.texto, R.string.buzon_sugerencia, "buzon-sugerencia", multilinea = true) { v ->
        editar { it.copy(texto = v) }
    }
}

@Composable
private fun Campo(
    valor: String,
    etiqueta: Int,
    testTag: String,
    multilinea: Boolean = false,
    onCambio: (String) -> Unit,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(stringResource(etiqueta)) },
        singleLine = !multilinea,
        minLines = if (multilinea) LINEAS_MIN else 1,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
    )
}

/** Lista desplegable con las secciones de la app. */
@Composable
private fun DesplegablePantalla(
    actual: SeccionApp?,
    onElegir: (SeccionApp) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        Text(stringResource(R.string.buzon_pantalla), style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(
                onClick = { abierto = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("buzon-pantalla"),
            ) {
                Text(
                    stringResource(actual?.etiqueta ?: R.string.buzon_pantalla_elegir),
                    modifier = Modifier.weight(1f),
                )
                Icon(Icono.Chevron.vector, contentDescription = null)
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                SeccionApp.entries.forEach { seccion ->
                    DropdownMenuItem(
                        text = { Text(stringResource(seccion.etiqueta)) },
                        onClick = {
                            abierto = false
                            onElegir(seccion)
                        },
                        modifier = Modifier.testTag("buzon-pantalla-${seccion.name.lowercase()}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun AvisoSinCorreo(
    copiado: Boolean,
    onCopiar: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().testTag("buzon-sin-correo"),
    ) {
        Column(Modifier.padding(Espacio.m), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Text(stringResource(R.string.buzon_sin_correo), style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onCopiar, modifier = Modifier.heightIn(min = 48.dp).testTag("buzon-copiar")) {
                    Icon(Icono.Copiar.vector, contentDescription = null)
                    Text(stringResource(R.string.buzon_copiar), modifier = Modifier.padding(start = Espacio.s))
                }
                if (copiado) {
                    Text(
                        stringResource(R.string.buzon_copiado),
                        modifier = Modifier.padding(start = Espacio.m).testTag("buzon-copiado"),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
