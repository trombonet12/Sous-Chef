package app.souschef.ui.crono

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.souschef.R
import app.souschef.receta.Carril
import app.souschef.receta.TipoCarril
import app.souschef.receta.crono.ClaseEstado
import app.souschef.ui.comun.Tabular
import app.souschef.ui.comun.colorCarril
import app.souschef.ui.theme.CronoPalette

/** Pestañas de recipientes: nombre en su color, reloj, paso actual y estado. La activa lleva borde superior. */
@Composable
fun PestanasCarriles(
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
    val color = colorCarril(carril.color)
    val pasado = vista.clase == ClaseEstado.ATENDER
    val parpadeo = pasado && (datos.ahoraMs / 1000) % 2 == 0L
    val fondo =
        when {
            parpadeo -> CronoPalette.parpadeo
            activa -> CronoPalette.carta2
            else -> CronoPalette.carta
        }
    Column(
        Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(fondo)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(if (activa) color else Color.Transparent))
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(carril.nombre, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

/** "fuego N · tapada · estado" / "sin fuego · estado". */
@Composable
private fun segundaLinea(
    datos: DatosCrono,
    carril: Carril,
): String {
    val vista = datos.vista(carril.id)
    val estado = textoEstado(datos, vista)
    val potencia = datos.paso(carril.id)?.f ?: 0
    val fuego =
        when {
            carril.tipoCarril != TipoCarril.FUEGO || datos.paso(carril.id) == null -> null
            potencia > 0 -> stringResource(R.string.ficha_fuego_bajo, potencia)
            else -> stringResource(R.string.ficha_sin_fuego_bajo)
        }
    return if (fuego == null) estado else "$fuego · $estado"
}
