package app.trombito.souschef.ui.crono.clasica

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
import app.trombito.souschef.R
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.ui.crono.AccionesCarril
import app.trombito.souschef.ui.crono.BotonDeshacer
import app.trombito.souschef.ui.crono.DatosCrono
import app.trombito.souschef.ui.crono.PorqueSheet
import app.trombito.souschef.ui.crono.formatoHora
import app.trombito.souschef.ui.crono.formatoReloj
import app.trombito.souschef.ui.crono.guia.GuiaIds
import app.trombito.souschef.ui.crono.guia.guia
import app.trombito.souschef.ui.crono.minutosPaso
import app.trombito.souschef.ui.crono.progreso
import app.trombito.souschef.ui.crono.recursoTapa
import app.trombito.souschef.ui.crono.requisitos
import app.trombito.souschef.ui.crono.vistasCarriles
import app.trombito.souschef.ui.theme.CronoPalette

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
    val req = requisitos(datos.recetaUi, datos.estado, paso)
    val bloqueado = req.pendientes.isNotEmpty() && !ec.corriendo && !ec.pausado && !ec.salto
    val vencido = ec.corriendo && datos.quedan(carril.id) <= 0
    // «Empezar» es el primario mientras el paso no ha arrancado; «Hecho» pasa a serlo al arrancar.
    val sinArrancar = !bloqueado && !ec.corriendo && !ec.pausado
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            bloqueado -> {
                BotonAntes(req.pendientes.first(), datos, acciones)
            }

            sinArrancar -> {
                BotonEmpezar { acciones.alternar(carril.id) }
            }

            else -> {
                BotonHecho(datos, carril.id, vencido) { acciones.hecho(carril.id) }
            }
        }
        FilaSecundaria(datos, carril, acciones, bloqueado, sinArrancar)
    }
}

/** Segunda fila de mandos: «Empezar igual», «Hecho» o Pausa/Seguir según el momento, +30 s y deshacer. */
@Composable
private fun FilaSecundaria(
    datos: DatosCrono,
    carril: Carril,
    acciones: AccionesCarril,
    bloqueado: Boolean,
    sinArrancar: Boolean,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            bloqueado -> {
                BotonSecundario(stringResource(R.string.crono_empezar_igual), Modifier.weight(1f)) {
                    acciones.empezarIgual(carril.id)
                }
            }

            sinArrancar -> {
                BotonSecundario(textoHecho(datos, carril.id), Modifier.weight(1f).guia(GuiaIds.HECHO)) {
                    acciones.hecho(carril.id)
                }
            }

            else -> {
                val corriendo = datos.ec(carril.id)?.corriendo == true
                val etiqueta = if (corriendo) R.string.crono_pausa else R.string.crono_seguir
                BotonSecundario(stringResource(etiqueta), Modifier.weight(1f)) { acciones.alternar(carril.id) }
            }
        }
        BotonSecundario(stringResource(R.string.crono_mas_30), Modifier.weight(1f).guia(GuiaIds.PRORROGA)) {
            acciones.masTreinta(carril.id)
        }
        BotonDeshacerClasico(datos, carril, acciones)
    }
}

@Composable
private fun BotonAntes(
    dep: Paso,
    datos: DatosCrono,
    acciones: AccionesCarril,
) {
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
}

/** Botón primario «Empezar» del paso que aún no ha arrancado. */
@Composable
private fun BotonEmpezar(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = CronoPalette.principal,
                contentColor = CronoPalette.onPrincipal,
            ),
    ) {
        Text(stringResource(R.string.crono_empezar), style = MaterialTheme.typography.titleMedium)
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
private fun textoHecho(
    datos: DatosCrono,
    carril: String,
): String {
    val ultimo = (datos.ec(carril)?.i ?: 0) >= datos.cola(carril).lastIndex
    return stringResource(if (ultimo) R.string.crono_hecho_cerrar else R.string.crono_hecho_siguiente)
}

/** «Hecho · siguiente»: de contorno mientras el paso corre y lleno (rojo) al llegar a cero. */
@Composable
private fun BotonHecho(
    datos: DatosCrono,
    carril: String,
    vencido: Boolean,
    onClick: () -> Unit,
) {
    val modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).guia(GuiaIds.HECHO)
    val contenido: @Composable () -> Unit = {
        Text(textoHecho(datos, carril), style = MaterialTheme.typography.titleMedium)
    }
    if (vencido) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = CronoPalette.urgente,
                    contentColor = CronoPalette.onUrgente,
                ),
        ) { contenido() }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            border = BorderStroke(1.5.dp, CronoPalette.principal),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CronoPalette.texto),
        ) { contenido() }
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
