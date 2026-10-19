package app.souschef.ui.crono

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.souschef.R
import app.souschef.ui.comun.Tabular
import app.souschef.ui.theme.CronoOscuro

/** Círculo con el número del carril (1-based, el mismo que dice la voz: «del 2»), con el color del carril. */
@Composable
fun InsigniaCarril(
    v: VistaCarril,
    color: Color,
    modifier: Modifier = Modifier,
    tamano: Dp = 22.dp,
) {
    val descripcion = stringResource(R.string.crono_carril_n, v.indiceCarril + 1)
    Box(
        modifier
            .size(tamano)
            .clip(CircleShape)
            .background(color)
            .clearAndSetSemantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            (v.indiceCarril + 1).toString(),
            color = CronoOscuro.fondo,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            style = Tabular,
            maxLines = 1,
        )
    }
}
