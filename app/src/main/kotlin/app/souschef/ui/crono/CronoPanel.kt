package app.souschef.ui.crono

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.souschef.R
import app.souschef.receta.Carril
import app.souschef.receta.Holgura
import app.souschef.receta.Paso
import app.souschef.receta.TipoCarril
import app.souschef.receta.crono.ConsultasCrono
import app.souschef.receta.crono.EtiquetaParalelo
import app.souschef.receta.crono.Paralelo
import app.souschef.ui.comun.Tabular
import app.souschef.ui.theme.CronoPalette

private const val SEGMENTOS_POTENCIA = 9

/** Panel del paso actual de un recipiente (o "Terminado"). */
@Composable
fun PanelCarril(
    datos: DatosCrono,
    carril: Carril,
    tecnicas: Map<String, String>,
    onTecnica: (String) -> Unit,
    onEnfocar: (String) -> Unit,
) {
    val paso = datos.paso(carril.id)
    val color = datos.color(carril.id)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (paso == null) {
            PanelTerminado(datos, carril, onEnfocar)
        } else {
            val total = datos.cola(carril.id).size
            val i = (datos.ec(carril.id)?.i ?: 0) + 1
            Text(
                stringResource(
                    R.string.crono_cabecera_paso,
                    carril.nombre,
                    i,
                    total,
                    formatoHora(ConsultasCrono.sugerido(datos.estado, datos.receta, carril.id) ?: datos.ahoraMs),
                ),
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
            Text(paso.t, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (carril.tipoCarril == TipoCarril.FUEGO) BloqueFuego(paso, color)
            Pildoras(datos, carril.id, paso)
            Requisitos(datos, paso, onEnfocar)
            Paralelos(datos, carril.id, onEnfocar)
            RelojDelPaso(datos, carril, paso, tecnicas, onTecnica)
        }
    }
}

@Composable
private fun PanelTerminado(
    datos: DatosCrono,
    carril: Carril,
    onEnfocar: (String) -> Unit,
) {
    Text(stringResource(R.string.crono_terminado), style = MaterialTheme.typography.headlineMedium)
    val siguiente = ConsultasCrono.siguienteAccionable(datos.estado, datos.receta, datos.ahoraMs)
    Text(
        stringResource(R.string.crono_carril_fuera, datos.nombre(siguiente ?: carril.id)),
        color = CronoPalette.apagado,
    )
    if (siguiente != null) {
        val hora = ConsultasCrono.sugerido(datos.estado, datos.receta, siguiente)
        BotonSecundario(
            stringResource(R.string.crono_siguiente, datos.nombre(siguiente), hora?.let { formatoHora(it) }.orEmpty()),
        ) { onEnfocar(siguiente) }
    }
}

/** Potencia 0-9: número grande, barra de 9 segmentos y tapa. */
@Composable
private fun BloqueFuego(
    paso: Paso,
    color: Color,
) {
    val potencia = (paso.f ?: 0).coerceIn(0, SEGMENTOS_POTENCIA)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "$potencia",
            fontSize = 56.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (potencia == 0) CronoPalette.tenue else color,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(SEGMENTOS_POTENCIA) { s ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (s < potencia) color else CronoPalette.carta2),
                    )
                }
            }
            val tapa = paso.tapa?.takeIf { it.isNotBlank() }
            if (tapa != null) {
                val resaltada = tapa.uppercase().let { "PUESTA" in it || "MEDIAS" in it }
                Text(
                    tapa.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (resaltada) CronoPalette.tapa else CronoPalette.tenue,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Pildoras(
    datos: DatosCrono,
    carril: String,
    paso: Paso,
) {
    val ec = datos.ec(carril)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (paso.holguraTipo) {
            Holgura.FLEXIBLE -> Pildora(stringResource(R.string.crono_holgura_flexible), CronoPalette.ok)
            Holgura.AJUSTADO -> Pildora(stringResource(R.string.crono_holgura_ajustado), CronoPalette.acento)
            Holgura.CRITICO -> Pildora(stringResource(R.string.crono_holgura_critico), CronoPalette.alerta)
        }
        Pildora(minutosPaso(paso), CronoPalette.apagado)
        if (ec?.pausado == true) {
            Pildora(stringResource(R.string.crono_est_pausa), CronoPalette.acento)
        } else if (ec?.corriendo != true) {
            Pildora(stringResource(R.string.crono_sin_arrancar), CronoPalette.apagado)
        }
    }
}

@Composable
internal fun Pildora(
    texto: String,
    color: Color,
) {
    Text(
        texto,
        Modifier
            .clip(
                RoundedCornerShape(50),
            ).background(CronoPalette.carta2)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun Requisitos(
    datos: DatosCrono,
    paso: Paso,
    onEnfocar: (String) -> Unit,
) {
    val req = requisitos(datos.receta, datos.estado, paso)
    if (paso.dep.isNotEmpty()) {
        val falta = req.pendientes.isNotEmpty()
        Text(
            stringResource(if (falta) R.string.crono_antes_hay_que else R.string.crono_requisito_cumplido),
            style = MaterialTheme.typography.labelMedium,
            color = if (falta) CronoPalette.alerta else CronoPalette.ok,
        )
        (req.pendientes + req.cumplidos).forEach {
            Text(
                "${it.t} (${datos.nombre(it.carril)})",
                Modifier.heightIn(min = 32.dp).clickable { onEnfocar(it.carril) },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    paso.junto?.let { datos.receta.paso(it) }?.let {
        Text(
            stringResource(R.string.crono_a_la_vez, it.t),
            style = MaterialTheme.typography.bodyMedium,
            color = CronoPalette.acento,
        )
    }
}

/** "Mientras esto corre": qué otros recipientes piden atención en los próximos 12 minutos. */
@Composable
private fun Paralelos(
    datos: DatosCrono,
    carril: String,
    onEnfocar: (String) -> Unit,
) {
    val filas = ConsultasCrono.paralelo(datos.estado, datos.receta, carril, datos.ahoraMs)
    if (filas.isEmpty()) return
    Text(
        stringResource(R.string.crono_mientras_corre),
        style = MaterialTheme.typography.labelMedium,
        color = CronoPalette.tenue,
    )
    filas.forEach { f -> FilaParalela(datos, f) { onEnfocar(f.carril) } }
}

@Composable
private fun FilaParalela(
    datos: DatosCrono,
    fila: Paralelo,
    onClick: () -> Unit,
) {
    val ahora = fila.etiqueta == EtiquetaParalelo.AHORA || fila.etiqueta == EtiquetaParalelo.VENCIDO
    val etiqueta =
        when (fila.etiqueta) {
            EtiquetaParalelo.VENCIDO -> stringResource(R.string.crono_par_vencido)
            EtiquetaParalelo.PAUSA -> stringResource(R.string.crono_est_pausa)
            EtiquetaParalelo.AHORA -> stringResource(R.string.crono_par_ahora)
            EtiquetaParalelo.EN_MIN -> stringResource(R.string.crono_par_en_min, fila.minutos)
        }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (ahora) CronoPalette.parpadeo else CronoPalette.carta)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            datos.nombre(fila.carril),
            color = datos.color(fila.carril),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
        Text(fila.paso, Modifier.weight(1f), fontSize = 13.sp, maxLines = 1)
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelMedium.merge(Tabular),
            color = if (ahora) CronoPalette.alerta else CronoPalette.apagado,
        )
    }
}
