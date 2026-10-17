package app.souschef.data.actualizacion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import app.souschef.R
import app.souschef.domain.actualizacion.ReleasePublicada
import app.souschef.domain.actualizacion.VersionNueva
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pregunta a GitHub por la última release y, si es más nueva que la instalada y no se avisó ya, lo notifica
 * (ADR-0009). Bloqueante: llamar fuera del hilo principal. No envía datos del usuario.
 */
class ComprobadorActualizaciones(
    private val context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Al abrir la app: solo si pasó el intervalo desde la última consulta. */
    fun comprobarSiToca() {
        if (VersionNueva.tocaComprobar(System.currentTimeMillis(), prefs.getLong(CLAVE_ULTIMA, 0L))) comprobar()
    }

    /** Devuelve false si la consulta falló (para que el job lo reintente). */
    fun comprobar(): Boolean {
        if (esDebug()) return true
        val release =
            try {
                Result.success(consultar())
            } catch (e: IOException) {
                Log.w(TAG, "Sin respuesta de GitHub: ${e.javaClass.simpleName}")
                Result.failure(e)
            }
        release.getOrNull()?.let(::avisarSiHaceFalta)
        return release.isSuccess
    }

    private fun avisarSiHaceFalta(release: ReleasePublicada) {
        prefs.edit { putLong(CLAVE_ULTIMA, System.currentTimeMillis()) }
        val instalada = versionInstalada()
        val avisar =
            instalada != null && VersionNueva.debeAvisar(release.tag, instalada, prefs.getString(CLAVE_AVISADA, null))
        if (avisar && notificar(release)) prefs.edit { putString(CLAVE_AVISADA, release.tag) }
    }

    private fun consultar(): ReleasePublicada? {
        val conexion = URL(API).openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = TIEMPO_MS
            conexion.readTimeout = TIEMPO_MS
            conexion.setRequestProperty("Accept", "application/vnd.github+json")
            conexion.setRequestProperty("User-Agent", "SousChef-Android")
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) return null
            VersionNueva.leerRelease(conexion.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conexion.disconnect()
        }
    }

    private fun notificar(release: ReleasePublicada): Boolean {
        val gestor = NotificationManagerCompat.from(context)
        if (!gestor.areNotificationsEnabled()) return false
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CANAL,
                context.getString(R.string.actualizacion_canal),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val version = release.tag.removePrefix("v")
        val toque =
            PendingIntent.getActivity(
                context,
                0,
                intentAlTocar(release),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val notificacion =
            NotificationCompat
                .Builder(context, CANAL)
                .setSmallIcon(R.drawable.ic_stat_souschef)
                .setContentTitle(context.getString(R.string.actualizacion_titulo, version))
                .setContentText(context.getString(R.string.actualizacion_texto))
                .setContentIntent(toque)
                .setAutoCancel(true)
                .build()
        @Suppress("MissingPermission") // comprobado arriba con areNotificationsEnabled()
        gestor.notify(ID_NOTIFICACION, notificacion)
        return true
    }

    /** Obtainium (cualquiera de sus dos paquetes) o, si no está, la página de la release. */
    private fun intentAlTocar(release: ReleasePublicada): Intent =
        PAQUETES_OBTAINIUM.firstNotNullOfOrNull { context.packageManager.getLaunchIntentForPackage(it) }
            ?: Intent(Intent.ACTION_VIEW, (release.url ?: PAGINA_RELEASES).toUri())

    private fun versionInstalada(): String? = context.packageManager.getPackageInfo(context.packageName, 0).versionName

    private fun esDebug(): Boolean = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    companion object {
        private const val TAG = "SousChef"
        private const val API = "https://api.github.com/repos/trombonet12/Sous-Chef/releases/latest"
        private const val PAGINA_RELEASES = "https://github.com/trombonet12/Sous-Chef/releases"
        val PAQUETES_OBTAINIUM = listOf("dev.imranr.obtainium", "dev.imranr.obtainium.fdroid")
        private const val PREFS = "actualizaciones"
        private const val CLAVE_ULTIMA = "ultima_consulta_ms"
        private const val CLAVE_AVISADA = "version_avisada"
        private const val CANAL = "actualizaciones"
        private const val ID_NOTIFICACION = 9_001
        private const val TIEMPO_MS = 10_000
    }
}
