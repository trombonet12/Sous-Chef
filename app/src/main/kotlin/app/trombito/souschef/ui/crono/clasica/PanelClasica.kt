package app.trombito.souschef.ui.crono.clasica

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Holgura
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.TipoCarril
import app.trombito.souschef.receta.crono.ConsultasCrono
import app.trombito.souschef.receta.crono.EtiquetaParalelo
import app.trombito.souschef.receta.crono.Paralelo
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.crono.AccionesCarril
import app.trombito.souschef.ui.crono.DatosCrono
import app.trombito.souschef.ui.crono.PorqueSheet
import app.trombito.souschef.ui.crono.formatoHora
import app.trombito.souschef.ui.crono.formatoReloj
import app.trombito.souschef.ui.crono.minutosPaso
import app.trombito.souschef.ui.crono.progreso
import app.trombito.souschef.ui.crono.recursoTapa
import app.trombito.souschef.ui.crono.requisitos
import app.trombito.souschef.ui.crono.textoDesvio
import app.trombito.souschef.ui.crono.vistasCarriles
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.colorCarrilTexto

private const val TAMANO_NUMERO = 56
private const val TAMANO_PALABRA = 30

/** Panel del paso actual de un recipiente (o "Terminado"). */
@Composable
internal fun PanelCarril(
    datos: DatosCrono,
    carril: Carril,
    tecnicas: Map<String, String>,
    onTecnica: (String) -> Unit,
    onEnfocar: (String) -> Unit,
) {
    val paso = datos.paso(carril.id)
    val color = datos.color(carril.id)
    val colorTexto = colorCarrilTexto(color)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (paso == null) {
            PanelTerminado(datos, carril, onEnfocar)
        } else {
            val total = datos.cola(carril.id).size
            val i = (datos.ec(carril.id)?.i ?: 0) + 1
            val desvio = vistasCarriles(datos).firstOrNull { it.id == carril.id }?.desvioMin ?: 0
            Text(
                buildAnnotatedString {
                    append(
                        stringResource(
                            R.string.crono_cabecera_paso,
                            carril.nombre,
                            i,
                            total,
                            formatoHora(
                                ConsultasCrono.sugerido(datos.estado, datos.receta, carril.id) ?: datos.ahoraMs,
                            ),
                        ),
                    )
                    textoDesvio(desvio)?.let {
                        withStyle(SpanStyle(color = if (desvio > 0) CronoPalette.alerta else CronoPalette.ok)) {
                            append(" $it")
                        }
                    }
                },
                style = MaterialTheme.typography.labelLarge,
                color = colorTexto,
            )
            Text(paso.t, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (carril.tipoCarril == TipoCarril.FUEGO) BloqueFuego(paso, color, datos.cocina)
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

/** Potencia en la cocina del usuario: número (o palabra) grande, barra de segmentos y tapa. */
@Composable
private fun BloqueFuego(
    paso: Paso,
    color: Color,
    cocina: Cocina,
) {
    val barra = barraPotencia(paso.f ?: 0, cocina)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            barra.texto,
            fontSize = if (barra.esPalabra) TAMANO_PALABRA.sp else TAMANO_NUMERO.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (barra.encendidos == 0) CronoPalette.tenue else colorCarrilTexto(color),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(barra.segmentos) { s ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (s < barra.encendidos) color else CronoPalette.carta2),
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
    val req = requisitos(datos.recetaUi, datos.estado, paso)
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
                Modifier.heightIn(min = 48.dp).clickable { onEnfocar(it.carril) },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    paso.junto?.let { datos.recetaUi.paso(it) }?.let {
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
            color = colorCarrilTexto(datos.color(fila.carril)),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
        Text(datos.texto(fila.paso), Modifier.weight(1f), fontSize = 13.sp, maxLines = 1)
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelMedium.merge(Tabular),
            color = if (ahora) CronoPalette.alerta else CronoPalette.apagado,
        )
    }
}
