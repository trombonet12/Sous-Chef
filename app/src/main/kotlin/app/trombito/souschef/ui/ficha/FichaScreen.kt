package app.trombito.souschef.ui.ficha

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trombito.souschef.R
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.comun.BarraTitulo
import app.trombito.souschef.ui.comun.EstadoVacio
import app.trombito.souschef.ui.comun.LocalCocina
import app.trombito.souschef.ui.comun.PasosMasMenos
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.FichaPalette

@Composable
fun FichaScreen(
    onAtras: () -> Unit,
    onTecnica: (String) -> Unit,
    onCocinar: (raciones: Int, servir: String) -> Unit,
    viewModel: FichaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    // Con la cocinada viva, salir de la ficha es abandonarla (con confirmación); ir a una técnica no.
    var confirmarSalida by remember { mutableStateOf(false) }
    val salir = { if (estado.cocinando != null) confirmarSalida = true else onAtras() }
    BackHandler(enabled = estado.cocinando != null) { confirmarSalida = true }
    if (confirmarSalida) {
        DialogoAbandonarDesdeFicha(
            onSi = {
                confirmarSalida = false
                viewModel.abandonarCocinada()
                onAtras()
            },
            onNo = { confirmarSalida = false },
        )
    }
    val acciones =
        AccionesFicha(
            onAtras = salir,
            onTecnica = onTecnica,
            onCocinar = onCocinar,
            onRaciones = viewModel::cambiarRaciones,
            onServir = viewModel::ajustarServir,
            onFavorita = viewModel::alternarFavorita,
            onQuiereHacerla = viewModel::alternarQuiereHacerla,
        )
    when {
        estado.cargando -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        }

        estado.receta == null -> {
            Column {
                BarraTitulo("", onAtras)
                EstadoVacio(stringResource(R.string.ficha_no_encontrada), null)
            }
        }

        else -> {
            FichaContenido(estado, acciones)
        }
    }
}

/**
 * La ficha en capas: cabecera fija (título, raciones, hora, cronómetro) y cinco pestañas con su propio scroll. Es
 * estado y callbacks, sin ViewModel, para poder probarla.
 */
@Composable
fun FichaContenido(
    estado: FichaUiState,
    acciones: AccionesFicha,
) {
    val receta = estado.receta ?: return
    var elegida by rememberSaveable { mutableIntStateOf(0) }
    CompositionLocalProvider(LocalCocina provides estado.cocina) {
        Column(Modifier.fillMaxSize()) {
            CabeceraFicha(estado, receta, acciones)
            BarraPestanas(PestanaFicha.entries[elegida]) { elegida = it.ordinal }
            // La hoja es papel siempre, también con el tema oscuro del sistema.
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.medium,
                color = FichaPalette.papel,
                contentColor = FichaPalette.tinta,
            ) {
                ContenidoPestana(PestanaFicha.entries[elegida], receta, estado, acciones.onTecnica)
            }
        }
    }
}

/** Aviso de que esta receta tiene la cocina abierta: el botón de arriba vuelve a ella. */
@Composable
private fun BandaCocinando(carriles: Int) {
    Text(
        pluralStringResource(R.plurals.ficha_cocinando, carriles, carriles),
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun DialogoAbandonarDesdeFicha(
    onSi: () -> Unit,
    onNo: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onNo,
        title = { Text(stringResource(R.string.crono_abandonar_titulo)) },
        text = { Text(stringResource(R.string.ficha_salir_abandona)) },
        confirmButton = { TextButton(onClick = onSi) { Text(stringResource(R.string.crono_abandonar_si)) } },
        dismissButton = { TextButton(onClick = onNo) { Text(stringResource(R.string.crono_abandonar_no)) } },
    )
}

/** Lo que no se mueve al leer: título, favorita, imprimir, raciones, hora, cronómetro y la banda «Cocinando». */
@Composable
private fun CabeceraFicha(
    estado: FichaUiState,
    receta: Receta,
    acciones: AccionesFicha,
) {
    Column {
        val contexto = LocalContext.current
        val nombrePdf = stringResource(R.string.ficha_pdf_nombre, receta.titulo)
        BarraTitulo(receta.titulo, acciones.onAtras) {
            IconButton(onClick = acciones.onFavorita) {
                val nombre =
                    stringResource(
                        if (estado.favorita) R.string.favorita_quitar else R.string.favorita_marcar,
                        receta.titulo,
                    )
                val esquema = MaterialTheme.colorScheme
                Icon(
                    if (estado.favorita) Icono.EstrellaLlena.vector else Icono.Estrella.vector,
                    contentDescription = nombre,
                    tint = if (estado.favorita) esquema.primary else esquema.onSurfaceVariant,
                )
            }
            BotonQuiereHacerla(estado.quiereHacerla, acciones.onQuiereHacerla)
            IconButton(
                onClick = {
                    imprimirFicha(
                        contexto,
                        nombrePdf,
                        fichaHtml(receta, estado.raciones, TextosFicha.desde(contexto, estado.cocina)),
                    )
                },
            ) {
                Icon(Icono.Imprimir.vector, contentDescription = stringResource(R.string.ficha_imprimir))
            }
        }
        FilaControles(estado, receta, acciones)
        BotonCocinar(estado, acciones)
        estado.cocinando?.let { BandaCocinando(it) }
    }
}

/** Marcador conmutable «Quiero hacerla»: la receta queda marcada para cocinarla pronto. */
@Composable
private fun BotonQuiereHacerla(
    marcada: Boolean,
    onClick: () -> Unit,
) {
    val esquema = MaterialTheme.colorScheme
    IconButton(onClick = onClick, modifier = Modifier.testTag("quiero-hacerla")) {
        Icon(
            if (marcada) Icono.MarcadorLleno.vector else Icono.Marcador.vector,
            contentDescription =
                stringResource(if (marcada) R.string.quiero_hacerla_quitar else R.string.quiero_hacerla_marcar),
            tint = if (marcada) esquema.primary else esquema.onSurfaceVariant,
        )
    }
}

/** «Raciones ± · Servir ± · Empieza HH:mm»; las raciones solo si la receta admite más de un valor. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilaControles(
    estado: FichaUiState,
    receta: Receta,
    acciones: AccionesFicha,
) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        val permitidas = estado.racionesPermitidas
        if (permitidas.size > 1) {
            val i = permitidas.indexOf(estado.raciones)
            Column {
                Text(stringResource(R.string.ficha_raciones_etiqueta), style = MaterialTheme.typography.labelMedium)
                PasosMasMenos(
                    valor = estado.raciones.toString(),
                    descMenos = stringResource(R.string.ficha_raciones_menos),
                    descMas = stringResource(R.string.ficha_raciones_mas),
                    onMenos = { permitidas.getOrNull(i - 1)?.let(acciones.onRaciones) },
                    onMas = { permitidas.getOrNull(i + 1)?.let(acciones.onRaciones) },
                )
            }
        }
        Column {
            Text(stringResource(R.string.ficha_servir_a), style = MaterialTheme.typography.labelMedium)
            PasosMasMenos(
                valor = Servir.formato(estado.servirMin),
                descMenos = stringResource(R.string.ficha_servir_menos),
                descMas = stringResource(R.string.ficha_servir_mas),
                onMenos = { acciones.onServir(-Servir.PASO) },
                onMas = { acciones.onServir(Servir.PASO) },
            )
            if (estado.paraCenar) {
                Text(
                    stringResource(R.string.para_cenar, Servir.formato(estado.servirMin)),
                    modifier = Modifier.testTag("para-cenar"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val inicio = Servir.inicio(estado.servirMin, receta)
        HoraDeEmpezar(inicio, estado.ritmoMin)
    }
}

/**
 * Una sola hora destacada: sin ritmo, la del plan; con ritmo, la ajustada («Sueles tardar 8 min más: empieza a las
 * 14:35») y la del plan debajo, en secundario («según la receta, 14:43»).
 */
@Composable
private fun HoraDeEmpezar(
    inicio: Int,
    ritmoMin: Int?,
) {
    Column {
        if (ritmoMin == null) {
            Text(
                stringResource(R.string.ficha_empezar_a, Servir.formato(inicio)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        val conRitmo = Servir.formato(Servir.ajustar(inicio, -ritmoMin))
        Text(
            if (ritmoMin > 0) {
                stringResource(R.string.ficha_ritmo_mas, ritmoMin, conRitmo)
            } else {
                stringResource(R.string.ficha_ritmo_menos, -ritmoMin, conRitmo)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.ficha_segun_receta, Servir.formato(inicio)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BotonCocinar(
    estado: FichaUiState,
    acciones: AccionesFicha,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Button(
            onClick = { acciones.onCocinar(estado.raciones, Servir.formato(estado.servirMin)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Icon(Icono.Crono.vector, contentDescription = null)
            Text(
                stringResource(if (estado.cocinando != null) R.string.ficha_volver_cocina else R.string.ficha_cocinar),
                Modifier.padding(start = 8.dp),
            )
        }
    }
}
