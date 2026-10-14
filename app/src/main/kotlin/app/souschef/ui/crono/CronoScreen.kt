package app.souschef.ui.crono

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
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
    viewModel: CronoViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    MantenerPantallaEncendida()
    AvisarAlSalirDeLaVista(viewModel)
    LaunchedEffect(ui.salida) { if (ui.salida) onSalir() }
    MaterialTheme(colorScheme = DarkColors, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        Surface(Modifier.fillMaxSize(), color = CronoPalette.fondo, contentColor = CronoPalette.texto) {
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

                resumen != null -> {
                    CronoCierre(receta, estado, resumen, viewModel::cerrar)
                }

                estado.inicioMs == null -> {
                    CronoPortada(receta, estado, viewModel, onSalir)
                }

                else -> {
                    CronoCocina(receta, estado, ui, viewModel, onSalir)
                }
            }
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
            LifecycleEventObserver { _, evento -> if (evento == Lifecycle.Event.ON_STOP) viewModel.enSegundoPlano() }
        propietario.lifecycle.addObserver(observador)
        onDispose {
            propietario.lifecycle.removeObserver(observador)
            viewModel.enSegundoPlano()
        }
    }
}
