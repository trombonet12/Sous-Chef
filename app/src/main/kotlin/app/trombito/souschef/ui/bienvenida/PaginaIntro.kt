package app.trombito.souschef.ui.bienvenida

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.ui.shell.Icono

/**
 * Las páginas de la intro, en orden. [conControles] es `true` si la página tiene algo que elegir: ahí no se pasa de
 * página deslizando (un gesto sin querer no debe cambiar una elección).
 */
internal enum class PaginaIntro(
    @param:StringRes val titulo: Int,
    @param:StringRes val texto: Int,
    val conControles: Boolean = false,
) {
    BIENVENIDA(R.string.intro_p1_titulo, R.string.intro_p1_texto),
    RECETAS(R.string.intro_p2_titulo, R.string.intro_p2_texto),
    CRONOMETRO(R.string.intro_p3_titulo, R.string.intro_p3_texto),
    COCINA(R.string.intro_p5_titulo, R.string.intro_p5_texto, conControles = true),
    RACIONES(R.string.intro_p6_titulo, R.string.intro_p6_texto, conControles = true),
    HORARIO(R.string.intro_p7_titulo, R.string.intro_p7_texto, conControles = true),
    APARIENCIA(R.string.intro_p8_titulo, R.string.intro_p8_texto, conControles = true),
    DISPOSICION(R.string.intro_p9_titulo, R.string.intro_p9_texto, conControles = true),
    VOZ(R.string.intro_p10_titulo, R.string.intro_p10_texto, conControles = true),
    PERMISOS(R.string.intro_p11_titulo, R.string.intro_p11_texto, conControles = true),
    LISTO(R.string.intro_p12_titulo, R.string.intro_p12_texto),
}

/**
 * Marco común de una página: [arriba] opcional, título grande, una o dos frases y su contenido, con scroll por si la
 * letra es grande.
 */
@Composable
internal fun PaginaMarco(
    pagina: PaginaIntro,
    modifier: Modifier = Modifier,
    arriba: @Composable ColumnScope.() -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        arriba()
        Text(
            stringResource(pagina.titulo),
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(stringResource(pagina.texto), style = MaterialTheme.typography.bodyLarge)
        contenido()
    }
}

/** Una fila con icono, título y explicación. */
@Composable
internal fun FilaExplica(
    icono: Icono,
    titulo: Int,
    texto: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icono.vector, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(texto),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
