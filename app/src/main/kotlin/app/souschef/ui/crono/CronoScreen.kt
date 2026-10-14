package app.souschef.ui.crono

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.souschef.R
import app.souschef.ui.comun.EstadoVacio
import app.souschef.ui.theme.CronoPalette
import app.souschef.ui.theme.DarkColors

/** Cronómetro de una receta: portada, cocina y cierre. Siempre oscuro y con la pantalla encendida. */
@Composable
fun CronoScreen(
    onSalir: () -> Unit,
    onTecnica: (String) -> Unit = {},
    viewModel: CronoViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    MantenerPantallaEncendida()
    BarraDeEstadoClara()
    EncenderPantallaConAvisos()
    val snackbar = remember { SnackbarHostState() }
    val mensajeGuardado = stringResource(R.string.crono_guardado)
    val accionDeshacer = stringResource(R.string.crono_deshacer)
    LaunchedEffect(ui.cierrePendiente) {
        if (ui.cierrePendiente) {
            val r = snackbar.showSnackbar(mensajeGuardado, accionDeshacer, duration = SnackbarDuration.Indefinite)
            if (r == SnackbarResult.ActionPerformed) viewModel.deshacerCierre()
        }
    }
    AvisarAlSalirDeLaVista(viewModel)
    LaunchedEffect(ui.salida) { if (ui.salida) onSalir() }
    MaterialTheme(colorScheme = DarkColors, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        Surface(Modifier.fillMaxSize(), color = CronoPalette.fondo, contentColor = CronoPalette.texto) {
            Box(Modifier.fillMaxSize()) {
                Contenido(ui, viewModel, onSalir, onTecnica)
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
                ui.conflicto?.let { DialogoOtraCocinada(it, viewModel) }
            }
        }
    }
}

@Composable
private fun Contenido(
    ui: CronoUi,
    viewModel: CronoViewModel,
    onSalir: () -> Unit,
    onTecnica: (String) -> Unit,
) {
    val receta = ui.receta
    val estado = ui.estado
    val resumen = ui.resumen
    when {
        ui.cargando -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        }

        receta == null -> {
            Mensaje(R.string.crono_no_encontrada, null, onSalir)
        }

        receta.pasos.isEmpty() -> {
            Mensaje(R.string.crono_sin_pasos_titulo, R.string.crono_sin_pasos_detalle, onSalir)
        }

        estado == null -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        }

        ui.cierrePendiente -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text(stringResource(R.string.crono_guardado)) }
        }

        resumen != null -> {
            CronoCierre(receta, estado, resumen, viewModel::cerrar)
        }

        estado.inicioMs == null -> {
            CronoPortada(receta, estado, viewModel, onSalir)
        }

        else -> {
            CronoCocina(receta, estado, ui, viewModel, SalidasCrono(onSalir, onTecnica))
        }
    }
}

@Composable
private fun Mensaje(
    titulo: Int,
    detalle: Int?,
    onSalir: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        EstadoVacio(stringResource(titulo), detalle?.let { stringResource(it) })
        Button(onClick = onSalir) { Text(stringResource(R.string.atras)) }
    }
}

@Composable
private fun MantenerPantallaEncendida() {
    val vista = LocalView.current
    DisposableEffect(vista) {
        vista.keepScreenOn = true
        onDispose { vista.keepScreenOn = false }
    }
}

/** Al dejar de verse la pantalla la cadena de alarmas queda armada con lo persistido. */
@Composable
private fun AvisarAlSalirDeLaVista(viewModel: CronoViewModel) {
    val propietario = LocalLifecycleOwner.current
    DisposableEffect(propietario) {
        val observador =
            LifecycleEventObserver { _, evento ->
                when (evento) {
                    Lifecycle.Event.ON_RESUME -> viewModel.alVer()
                    Lifecycle.Event.ON_STOP -> viewModel.enSegundoPlano()
                    else -> Unit
                }
            }
        propietario.lifecycle.addObserver(observador)
        onDispose {
            propietario.lifecycle.removeObserver(observador)
            viewModel.enSegundoPlano()
        }
    }
}

private tailrec fun Context.actividad(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.actividad()
        else -> null
    }

/** El crono es oscuro: iconos claros en la barra de estado mientras se ve y se restaura el ajuste al salir. */
@Composable
private fun BarraDeEstadoClara() {
    val vista = LocalView.current
    val actividad = LocalContext.current.actividad()
    DisposableEffect(actividad) {
        val control = actividad?.window?.let { WindowCompat.getInsetsController(it, vista) }
        val previo = control?.isAppearanceLightStatusBars
        control?.isAppearanceLightStatusBars = false
        onDispose { if (previo != null) control.isAppearanceLightStatusBars = previo }
    }
}

/** Mientras el crono está abierto, un aviso puede encender la pantalla y mostrarse sobre el bloqueo. */
@Composable
private fun EncenderPantallaConAvisos() {
    val actividad = LocalContext.current.actividad()
    DisposableEffect(actividad) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            actividad?.setShowWhenLocked(true)
            actividad?.setTurnScreenOn(true)
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                actividad?.setShowWhenLocked(false)
                actividad?.setTurnScreenOn(false)
            }
        }
    }
}

/** Solo se cocina una receta a la vez: abrir esta exige abandonar la que sigue en marcha. */
@Composable
private fun DialogoOtraCocinada(
    titulo: String,
    viewModel: CronoViewModel,
) {
    AlertDialog(
        onDismissRequest = viewModel::descartarConflicto,
        containerColor = CronoPalette.carta2,
        textContentColor = CronoPalette.texto,
        text = { Text(stringResource(R.string.crono_ya_cocinando, titulo)) },
        confirmButton = {
            TextButton(onClick = viewModel::abandonarOtraYAbrir) {
                Text(stringResource(R.string.crono_abandonar_si), color = CronoPalette.alerta)
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::descartarConflicto) {
                Text(stringResource(R.string.crono_abandonar_no), color = CronoPalette.texto)
            }
        },
    )
}
