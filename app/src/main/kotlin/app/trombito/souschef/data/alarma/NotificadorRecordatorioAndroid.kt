package app.trombito.souschef.data.alarma

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import app.trombito.souschef.MainActivity
import app.trombito.souschef.R
import app.trombito.souschef.domain.crono.IdsNotificacion
import app.trombito.souschef.domain.crono.NotificadorRecordatorio
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * El aviso de «Avísame para empezar» (ADR-0016): una notificación normal, en su canal `crono_recordatorio` (importancia
 * por defecto: un sonido), sin insistir, sin tono de alarma y sin pantalla completa. Se cierra al tocarla y abre la
 * ficha de la receta con un intent explícito e inmutable a [MainActivity], con `data = souschef://ficha/{slug}`:
 * Navigation lee ese enlace, que no está declarado en el manifest (ninguna app ni web puede lanzarlo).
 */
class NotificadorRecordatorioAndroid
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : NotificadorRecordatorio {
        private val gestor = context.getSystemService(NotificationManager::class.java)

        override fun avisar(
            slug: String,
            titulo: String,
            servirMs: Long,
        ) {
            gestor.createNotificationChannel(
                NotificationChannel(
                    CANAL_RECORDATORIO,
                    context.getString(R.string.recordatorio_canal),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
            val hora = FORMATO_HORA.format(Instant.ofEpochMilli(servirMs).atZone(ZoneId.systemDefault()))
            val notificacion =
                Notification
                    .Builder(context, CANAL_RECORDATORIO)
                    .setSmallIcon(R.drawable.ic_stat_souschef)
                    .setContentTitle(context.getString(R.string.recordatorio_notif_titulo, titulo))
                    .setContentText(context.getString(R.string.recordatorio_notif_texto, hora))
                    .setCategory(Notification.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setContentIntent(abrirFicha(slug))
                    .build()
            gestor.notify(IdsNotificacion.tagRecordatorio(slug), IdsNotificacion.ID_RECORDATORIO, notificacion)
        }

        private fun abrirFicha(slug: String): PendingIntent =
            PendingIntent.getActivity(
                context,
                IdsNotificacion.ID_RECORDATORIO xor slug.hashCode(),
                Intent(Intent.ACTION_VIEW, "$ENLACE_FICHA$slug".toUri(), context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        companion object {
            const val CANAL_RECORDATORIO = "crono_recordatorio"

            /** Enlace interno de la ficha (lo lee Navigation; el manifest no lo declara). */
            const val ENLACE_FICHA = "souschef://ficha/"

            private val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        }
    }
