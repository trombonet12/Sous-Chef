package app.trombito.souschef.ui.buzon

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import androidx.core.net.toUri

internal const val CORREO_BUZON = "trombonet12@trombonet12.es"

/** El intent de correo: `mailto:` con asunto y cuerpo; lo atiende cualquier app de correo. */
internal fun intentCorreo(correo: Correo): Intent =
    Intent(Intent.ACTION_SENDTO, "mailto:$CORREO_BUZON".toUri())
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(CORREO_BUZON))
        .putExtra(Intent.EXTRA_SUBJECT, correo.asunto)
        .putExtra(Intent.EXTRA_TEXT, correo.cuerpo)

/** Abre la app de correo; false si no hay ninguna que lo atienda. */
internal fun enviarCorreo(
    contexto: Context,
    correo: Correo,
): Boolean =
    try {
        contexto.startActivity(intentCorreo(correo))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

/** Copia asunto y cuerpo al portapapeles. */
internal fun copiarCorreo(
    contexto: Context,
    correo: Correo,
) {
    val clip = ClipData.newPlainText(correo.asunto, correo.textoPlano)
    contexto.getSystemService<ClipboardManager>()?.setPrimaryClip(clip)
}
