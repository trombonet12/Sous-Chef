package app.trombito.souschef.ui.crono.clasica

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.TipoCarril
import app.trombito.souschef.receta.crono.ClaseEstado
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.comun.nivel
import app.trombito.souschef.ui.crono.AccionesCarril
import app.trombito.souschef.ui.crono.DatosCrono
import app.trombito.souschef.ui.crono.PorqueSheet
import app.trombito.souschef.ui.crono.formatoHora
import app.trombito.souschef.ui.crono.formatoReloj
import app.trombito.souschef.ui.crono.minutosPaso
import app.trombito.souschef.ui.crono.progreso
import app.trombito.souschef.ui.crono.recursoTapa
import app.trombito.souschef.ui.crono.requisitos
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.colorCarrilTexto

/** Pestañas de recipientes: nombre, reloj, paso y fuego/tapa. La activa lleva borde y fondo de su color. */
@Composable
internal fun PestanasCarriles(
    datos: DatosCrono,
    activa: Int,
    onElegir: (String) -> Unit,
) {
    val lista = rememberLazyListState()
    LaunchedEffect(activa) { lista.animateScrollToItem(activa) }
    LazyRow(
        state = lista,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(datos.carriles, key = { _, c -> c.id }) { i, c ->
            Pestana(datos, c, i == activa) { onElegir(c.id) }
        }
    }
}

@Composable
private fun Pestana(
    datos: DatosCrono,
    carril: Carril,
    activa: Boolean,
    onClick: () -> Unit,
) {
    val vista = datos.vista(carril.id)
    val color = datos.color(carril.id)
    val pasado = vista.clase == ClaseEstado.ATENDER
    val parpadeo = pasado && (datos.ahoraMs / 1000) % 2 == 0L
    val fondo =
        when {
            parpadeo -> CronoPalette.parpadeo
            activa -> color.copy(alpha = FONDO_ACTIVA).compositeOver(CronoPalette.carta)
            else -> CronoPalette.carta
        }
    val forma = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .width(150.dp)
            .clip(forma)
            .background(fondo)
            .border(if (activa) 2.dp else 1.dp, if (activa) color else CronoPalette.linea, forma)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            carril.nombre,
            color = colorCarrilTexto(color, sobre = fondo),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
        Text(
            relojPestana(datos, carril.id, vista.clase),
            style = MaterialTheme.typography.titleMedium.merge(Tabular),
            color = if (pasado) CronoPalette.alerta else CronoPalette.texto,
        )
        Text(
            datos.paso(carril.id)?.t ?: stringResource(R.string.crono_terminado),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            segundaLinea(datos, carril),
            fontSize = 11.sp,
            color = CronoPalette.apagado,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun relojPestana(
    datos: DatosCrono,
    carril: String,
    clase: ClaseEstado,
): String =
    if (clase ==
        ClaseEstado.FIN
    ) {
        stringResource(R.string.crono_sin_reloj)
    } else {
        formatoReloj(datos.quedan(carril))
    }

/** Una sola línea: "fuego N · tapada"; si hay algo que atender o esperar, el estado en su lugar. */
@Composable
private fun segundaLinea(
    datos: DatosCrono,
    carril: Carril,
): String {
    val vista = datos.vista(carril.id)
    val paso = datos.paso(carril.id)
    if (paso == null || vista.clase !in listOf(ClaseEstado.ESPERA, ClaseEstado.YA, ClaseEstado.MARCHA)) {
        return textoEstado(datos, vista)
    }
    val potencia = paso.f ?: 0
    val fuego =
        when {
            carril.tipoCarril != TipoCarril.FUEGO -> null
            potencia > 0 -> stringResource(R.string.ficha_fuego_bajo, datos.cocina.nivel(potencia))
            else -> stringResource(R.string.ficha_sin_fuego_bajo)
        }
    val tapa = recursoTapa(paso.tapa)?.let { stringResource(it) }
    return listOfNotNull(fuego, tapa).joinToString(" · ").ifEmpty { NBSP }
}

private const val NBSP = " "
private const val FONDO_ACTIVA = 0.16f
