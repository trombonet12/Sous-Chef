package app.trombito.souschef.ui.ajustes

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.trombito.souschef.R
import app.trombito.souschef.ui.shell.Icono

private const val CORREO = "trombonet12@trombonet12.es"
private const val WEB = "https://www.trombonet12.es/"

/** Lado del círculo con la mascota. */
private val LADO_ICONO = 96.dp

/** El adaptive icon mide 108 dp y su zona segura 72: se amplía para que la mascota llene el círculo. */
private val LADO_MASCOTA = LADO_ICONO * 108 / 72

@Composable
fun AjustesAcercaScreen(
    onAtras: () -> Unit,
    onIntro: () -> Unit = {},
) {
    val contexto = LocalContext.current
    val version = versionApp()
    val asunto = stringResource(R.string.ajustes_contacto_asunto, version)
    AjustesAcerca(
        version = version,
        onAtras = onAtras,
        onCorreo = {
            val correo = Intent(Intent.ACTION_SENDTO, "mailto:$CORREO".toUri()).putExtra(Intent.EXTRA_SUBJECT, asunto)
            contexto.abrir(correo)
        },
        onWeb = { contexto.abrir(Intent(Intent.ACTION_VIEW, WEB.toUri())) },
        onIntro = onIntro,
    )
}

/** Pantalla «Acerca de» sin contexto: la versión llega hecha y correo y web se delegan en los callbacks. */
@Composable
internal fun AjustesAcerca(
    version: String,
    onAtras: () -> Unit,
    onCorreo: () -> Unit,
    onWeb: () -> Unit,
    onIntro: () -> Unit = {},
) {
    PantallaAjustes(stringResource(R.string.ajustes_acerca), onAtras) {
        Cabecera(version)
        FilaAjuste(
            icono = Icono.Libro,
            titulo = stringResource(R.string.intro_ver),
            resumen = stringResource(R.string.intro_ver_resumen),
            final = Icono.Chevron,
            etiqueta = "ajustes-intro",
            onClick = onIntro,
        )
        Text(
            stringResource(R.string.ajustes_acerca_contacto),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        FilaAjuste(
            icono = Icono.Correo,
            titulo = stringResource(R.string.ajustes_acerca_correo),
            resumen = stringResource(R.string.ajustes_contacto),
            final = Icono.Externo,
            etiqueta = "ajustes-contacto",
            onClick = onCorreo,
        )
        FilaAjuste(
            icono = Icono.Web,
            titulo = stringResource(R.string.ajustes_acerca_web),
            resumen = stringResource(R.string.ajustes_web),
            final = Icono.Externo,
            etiqueta = "ajustes-web",
            onClick = onWeb,
        )
    }
}

@Composable
private fun Cabecera(version: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(LADO_ICONO).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.requiredSize(LADO_MASCOTA),
            )
        }
        Text(
            stringResource(R.string.app_name),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            stringResource(R.string.ajustes_acerca_version, version),
            modifier = Modifier.testTag("acerca-version"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.ajustes_autor),
            modifier = Modifier.testTag("acerca-autor"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Lanza una actividad externa; sin app que la atienda no hace nada. */
private fun Context.abrir(intent: Intent) {
    runCatching { startActivity(intent) }
}
