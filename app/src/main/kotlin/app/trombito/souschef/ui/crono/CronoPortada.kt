package app.trombito.souschef.ui.crono

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.trombito.souschef.R
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.SesionCrono
import app.trombito.souschef.ui.comun.BarraTitulo
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.crono.portada.AccionesChecklist
import app.trombito.souschef.ui.crono.portada.ChecklistAntes
import app.trombito.souschef.ui.crono.portada.EncabezadoChecklist
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.CronoPalette

private const val PASO_SERVIR_MIN = 15

/** Una casilla de la portada: id estable por contenido y texto con negritas. */
internal data class Casilla(
    val id: String,
    val texto: String,
)

internal data class BloqueCasillas(
    val titulo: String?,
    val casillas: List<Casilla>,
)

/**
 * Checklist de mise en place: los bloques `mep`, o los `grupos` ("<b>cantidad</b> · nombre") si no hay.
 * El id de cada casilla sale de la línea de la receta canónica [receta] (no cambia con la cocina del usuario,
 * ADR-0014) y el texto de la traducida [traducida], que empareja por posición.
 */
internal fun bloquesPortada(
    receta: Receta,
    traducida: Receta,
    raciones: Int,
): List<BloqueCasillas> =
    if (receta.mep.isNotEmpty()) {
        receta.mep.mapIndexed { i, b ->
            val lineas = traducida.mep.getOrNull(i)?.l ?: emptyList()
            BloqueCasillas(
                b.t,
                b.l.mapIndexed { j, linea -> Casilla(SesionCrono.idCasilla(linea), lineas.getOrElse(j) { linea }) },
            )
        }
    } else {
        val f = Escalado.factor(raciones, receta.raciones)
        receta.grupos.map { g ->
            BloqueCasillas(
                g.n,
                g.items.map {
                    val cantidad = Escalado.cantidad(it, f).removeSuffix(" *")
                    val linea = if (cantidad.isBlank()) it.n else "<b>$cantidad</b> · ${it.n}"
                    Casilla(SesionCrono.idCasilla(linea), linea)
                },
            )
        }
    }

@Composable
fun CronoPortada(
    receta: Receta,
    recetaUi: Receta,
    estado: EstadoCrono,
    viewModel: CronoViewModel,
    onSalir: () -> Unit,
) {
    val contexto = LocalContext.current
    val pedirPermiso =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.abrirCocina() }
    val abrir = {
        val necesita =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        if (necesita) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS) else viewModel.abrirCocina()
    }
    val bloques = bloquesPortada(receta, recetaUi, estado.raciones)
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BarraTitulo(receta.titulo, onSalir)
            AvisoPantallaCompleta()
            ChecklistAntes(
                encabezado =
                    EncabezadoChecklist(
                        stringResource(
                            if (receta.mep.isNotEmpty()) R.string.crono_antes_de_encender else R.string.crono_saca_esto,
                        ),
                        recetaUi.descripcion,
                    ),
                bloques = bloques,
                marcados = estado.marcados,
                conDetalle = receta.mep.isNotEmpty(),
                acciones = AccionesChecklist(viewModel::marcar, viewModel::marcarVarias),
            )
        }
        PieAbrir(estado.servirMs, { viewModel.cambiarServir(it) }, abrir)
    }
}

@Composable
private fun PieAbrir(
    servirMs: Long,
    onServir: (Int) -> Unit,
    onAbrir: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(R.string.crono_servir_a_las),
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = CronoPalette.apagado,
            )
            IconButton(onClick = { onServir(-PASO_SERVIR_MIN) }, Modifier.heightIn(min = 48.dp)) {
                Icon(Icono.Menos.vector, stringResource(R.string.ficha_servir_menos))
            }
            Text(
                formatoHora(servirMs),
                style = MaterialTheme.typography.headlineSmall.merge(Tabular),
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { onServir(PASO_SERVIR_MIN) }, Modifier.heightIn(min = 48.dp)) {
                Icon(Icono.Anadir.vector, stringResource(R.string.ficha_servir_mas))
            }
        }
        Button(
            onClick = onAbrir,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = CronoPalette.principal,
                    contentColor = CronoPalette.onPrincipal,
                ),
        ) {
            Text(stringResource(R.string.crono_abrir_cocina), style = MaterialTheme.typography.titleMedium)
        }
    }
}
