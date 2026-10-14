package app.souschef.data.alarma

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.net.toUri
import app.souschef.MainActivity
import app.souschef.R
import app.souschef.domain.crono.AvisoNotificacion
import app.souschef.domain.crono.Notificador
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Canal HIGH con sonido de alarma (USAGE_ALARM) para los avisos; canal LOW sin sonido para los que ya llevan más de
 * 90 s vencidos (solo informan). Sin permiso de notificaciones el sistema descarta la publicación.
 */
class NotificadorAndroid
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Notificador {
        private val gestor = context.getSystemService(NotificationManager::class.java)

        override fun publicar(avisos: List<AvisoNotificacion>) {
            crearCanales()
            avisos.forEach { a ->
                val canal = if (a.aviso.silencioso) CANAL_SILENCIOSO else CANAL_AVISOS
                val notificacion =
                    Notification
                        .Builder(context, canal)
                        .setSmallIcon(R.drawable.ic_stat_souschef)
                        .setContentTitle("${a.carril} · ${a.tituloReceta}")
                        .setContentText(a.aviso.texto)
                        .setCategory(Notification.CATEGORY_ALARM)
                        .setAutoCancel(true)
                        .setShowWhen(true)
                        .setWhen(a.aviso.instanteMs)
                        .setContentIntent(abrirCrono(a.slug))
                        .build()
                gestor.notify(idNotificacion(a), notificacion)
            }
        }

        private fun idNotificacion(a: AvisoNotificacion): Int = "${a.slug}/${a.carril}/${a.aviso.tipo}".hashCode()

        private fun abrirCrono(slug: String): PendingIntent =
            PendingIntent.getActivity(
                context,
                slug.hashCode(),
                Intent(Intent.ACTION_VIEW, "souschef://crono/$slug".toUri(), context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun crearCanales() {
            val sonido =
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            val avisos =
                NotificationChannel(
                    CANAL_AVISOS,
                    context.getString(R.string.crono_canal_avisos),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), sonido)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 150, 100, 150, 100, 300)
                }
            val silencioso =
                NotificationChannel(
                    CANAL_SILENCIOSO,
                    context.getString(R.string.crono_canal_silencioso),
                    NotificationManager.IMPORTANCE_LOW,
                )
            gestor.createNotificationChannels(listOf(avisos, silencioso))
        }

        private companion object {
            const val CANAL_AVISOS = "crono_avisos"
            const val CANAL_SILENCIOSO = "crono_silencioso"
        }
    }
