package app.souschef.ui.crono.clasica

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.souschef.R
import app.souschef.receta.Carril
import app.souschef.ui.crono.AccionesCarril
import app.souschef.ui.crono.BotonDeshacer
import app.souschef.ui.crono.DatosCrono
import app.souschef.ui.crono.PorqueSheet
import app.souschef.ui.crono.formatoHora
import app.souschef.ui.crono.formatoReloj
import app.souschef.ui.crono.minutosPaso
import app.souschef.ui.crono.progreso
import app.souschef.ui.crono.recursoTapa
import app.souschef.ui.crono.requisitos
import app.souschef.ui.crono.vistasCarriles
import app.souschef.ui.theme.CronoPalette

private const val MAX_CARACTERES_DEP = 16

/** Fila fija de mandos del recipiente enfocado (todos ≥ 48 dp). */
@Composable
internal fun MandosCarril(
    datos: DatosCrono,
    carril: Carril,
    acciones: AccionesCarril,
) {
    val ec = datos.ec(carril.id)
    val paso = datos.paso(carril.id)
    if (ec == null || paso == null) {
        // Carril terminado: solo queda deshacer su último Hecho, si fue sin querer.
        Row(Modifier.fillMaxWidth().padding(12.dp)) { BotonDeshacerClasico(datos, carril, acciones) }
        return
    }
    val req = requisitos(datos.receta, datos.estado, paso)
    val bloqueado = req.pendientes.isNotEmpty() && !ec.corriendo && !ec.pausado && !ec.salto
    val vencido = ec.corriendo && datos.quedan(carril.id) <= 0
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (bloqueado) {
            val dep = req.pendientes.first()
            OutlinedButton(
                onClick = { acciones.enfocar(dep.carril) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                border = BorderStroke(1.5.dp, CronoPalette.alerta),
            ) {
                Text(
                    stringResource(R.string.crono_antes_ir, dep.t.abreviado(), datos.nombre(dep.carril)),
                    color = CronoPalette.alerta,
                    maxLines = 1,
                )
            }
        } else {
            BotonHecho(datos, carril.id, vencido) { acciones.hecho(carril.id) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (bloqueado) {
                BotonSecundario(stringResource(R.string.crono_empezar_igual), Modifier.weight(1f)) {
                    acciones.empezarIgual(carril.id)
                }
            } else {
                val etiqueta =
                    when {
                        ec.corriendo -> R.string.crono_pausa
                        ec.pausado -> R.string.crono_seguir
                        else -> R.string.crono_empezar
                    }
                BotonSecundario(stringResource(etiqueta), Modifier.weight(1f)) { acciones.alternar(carril.id) }
            }
            BotonSecundario(stringResource(R.string.crono_mas_30), Modifier.weight(1f)) {
                acciones.masTreinta(carril.id)
            }
            BotonDeshacerClasico(datos, carril, acciones)
        }
    }
}

/** El mismo minibotón ↶ que en las otras disposiciones. */
@Composable
private fun BotonDeshacerClasico(
    datos: DatosCrono,
    carril: Carril,
    acciones: AccionesCarril,
) {
    vistasCarriles(datos).firstOrNull { it.id == carril.id }?.let { BotonDeshacer(it, acciones) }
}

private fun String.abreviado(): String = if (length <= MAX_CARACTERES_DEP) this else take(MAX_CARACTERES_DEP) + "…"

@Composable
private fun BotonHecho(
    datos: DatosCrono,
    carril: String,
    vencido: Boolean,
    onClick: () -> Unit,
) {
    val ultimo = (datos.ec(carril)?.i ?: 0) >= datos.cola(carril).lastIndex
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (vencido) CronoPalette.urgente else CronoPalette.principal,
                contentColor = if (vencido) CronoPalette.onUrgente else CronoPalette.onPrincipal,
            ),
    ) {
        Text(
            stringResource(if (ultimo) R.string.crono_hecho_cerrar else R.string.crono_hecho_siguiente),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
internal fun BotonSecundario(
    texto: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        border = BorderStroke(1.dp, CronoPalette.linea),
    ) {
        Text(texto, color = CronoPalette.texto, maxLines = 1)
    }
}
