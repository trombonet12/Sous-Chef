package app.souschef.ui.crono

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.souschef.R
import app.souschef.receta.Carril
import app.souschef.receta.Paso
import app.souschef.receta.TipoCarril
import app.souschef.ui.comun.Tabular
import app.souschef.ui.comun.colorCarril
import app.souschef.ui.comun.textoRico
import app.souschef.ui.theme.CronoPalette
import app.souschef.ui.theme.LocalFonts

private const val MARGEN_AHORA_HITO_S = 90

/** Reloj grande, barra con hitos, chips de hitos, METO/SACO, nota, porqué y "Después aquí". */
@Composable
fun RelojDelPaso(
    datos: DatosCrono,
    carril: Carril,
    paso: Paso,
    tecnicas: Map<String, String>,
    onTecnica: (String) -> Unit,
) {
    val q = datos.quedan(carril.id)
    val color = colorCarril(carril.color)
    Text(
        formatoReloj(q),
        Modifier.fillMaxWidth(),
        fontSize = 76.sp,
        fontWeight = FontWeight.ExtraBold,
        style = MaterialTheme.typography.displayLarge.merge(Tabular),
        color = if (q < 0) CronoPalette.alerta else CronoPalette.texto,
    )
    BarraProgreso(paso, q, if (q < 0) CronoPalette.alerta else color)
    ChipsHitos(datos, carril.id, paso, q)
    LineasMeto(paso, carril.tipoCarril)
    paso.nota?.takeIf { it.isNotBlank() }?.let {
        Text(
            textoRico(it),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = LocalFonts.current.newsreader,
            fontStyle = FontStyle.Italic,
        )
    }
    Porque(datos, carril, paso, tecnicas, onTecnica)
    PieDelPaso(datos, carril.id)
}

@Composable
private fun BarraProgreso(
    paso: Paso,
    quedan: Int,
    color: Color,
) {
    val fraccion = progreso(paso.segundos, quedan)
    val hitos = paso.hitosValidos
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val alto = size.height
        drawRoundRect(CronoPalette.carta2, size = Size(size.width, alto), cornerRadius = CornerRadius(alto / 2))
        drawRoundRect(color, size = Size(size.width * fraccion, alto), cornerRadius = CornerRadius(alto / 2))
        hitos.forEach {
            val x = size.width * (it.en / paso.segundos).toFloat()
            drawLine(CronoPalette.texto, Offset(x, 0f), Offset(x, alto), strokeWidth = 2.dp.toPx())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsHitos(
    datos: DatosCrono,
    carril: String,
    paso: Paso,
    quedan: Int,
) {
    val hitos = paso.hitosValidos
    if (hitos.isEmpty()) return
    val ec = datos.ec(carril)
    val transcurrido = paso.segundos - quedan
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        hitos.forEachIndexed { i, h ->
            val avisado = ec != null && i in ec.hitosAvisados
            val reciente = avisado && ec?.corriendo == true && transcurrido - h.en <= MARGEN_AHORA_HITO_S
            val marca =
                when {
                    reciente -> stringResource(R.string.crono_hito_ahora)
                    avisado -> stringResource(R.string.crono_hito_hecho)
                    else -> formatoReloj(h.en.toInt())
                }
            Pildora("$marca · ${h.t}", if (reciente) CronoPalette.acento else CronoPalette.apagado)
        }
    }
}

@Composable
private fun LineasMeto(
    paso: Paso,
    tipo: TipoCarril,
) {
    paso.meto?.takeIf { it.isNotBlank() }?.let { LineaEtiquetada(R.string.crono_meto, CronoPalette.meto, it) }
    paso.saco?.takeIf { it.isNotBlank() }?.let { LineaEtiquetada(R.string.crono_saco, CronoPalette.saco, it) }
    if (tipo != TipoCarril.FUEGO) {
        paso.tapa?.takeIf { it.isNotBlank() }?.let { LineaEtiquetada(R.string.crono_tapa, CronoPalette.tapa, it) }
    }
}

@Composable
internal fun LineaEtiquetada(
    rotulo: Int,
    color: Color,
    texto: String,
) {
    val etiqueta = stringResource(rotulo)
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.ExtraBold)) { append("$etiqueta  ") }
            append(textoRico(texto))
        },
        style = MaterialTheme.typography.bodyLarge,
    )
}

/** "Ver el porqué": abre una hoja con el paso completo; se cierra sola al pasar al paso siguiente. */
@Composable
private fun Porque(
    datos: DatosCrono,
    carril: Carril,
    paso: Paso,
    tecnicas: Map<String, String>,
    onTecnica: (String) -> Unit,
) {
    if (paso.p.isNullOrBlank() && tecnicas.isEmpty()) return
    var abierto by remember(carril.id, datos.ec(carril.id)?.i) { mutableStateOf(false) }
    Text(
        stringResource(R.string.crono_ver_porque),
        Modifier.heightIn(min = 48.dp).clickable { abierto = true },
        color = CronoPalette.acento,
        style = MaterialTheme.typography.labelLarge,
    )
    if (abierto) PorqueSheet(paso, carril, tecnicas, onTecnica) { abierto = false }
}

@Composable
private fun PieDelPaso(
    datos: DatosCrono,
    carril: String,
) {
    val i = datos.ec(carril)?.i ?: return
    val siguiente = datos.cola(carril).getOrNull(i + 1)
    val texto =
        if (siguiente == null) {
            stringResource(R.string.crono_ultimo_paso)
        } else {
            val f = siguiente.f
            val duracion = minutosPaso(siguiente)
            val detalle =
                if (datos.carril(carril)?.tipoCarril == TipoCarril.FUEGO && f != null) {
                    stringResource(R.string.crono_despues_fuego, siguiente.t, duracion, f)
                } else {
                    stringResource(R.string.crono_despues, siguiente.t, duracion)
                }
            detalle
        }
    Column(Modifier.fillMaxWidth()) {
        Text(texto, style = MaterialTheme.typography.bodySmall, color = CronoPalette.tenue)
    }
}
