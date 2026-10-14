package app.souschef.data.alarma

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.core.net.toUri
import app.souschef.MainActivity
import app.souschef.R
import app.souschef.domain.crono.AvisoNotificacion
import app.souschef.domain.crono.EnMarcha
import app.souschef.domain.crono.IdsNotificacion
import app.souschef.domain.crono.Notificador
import app.souschef.ui.crono.AlarmaActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Canal HIGH solo con vibración para los avisos (el sonido de alarma lo da [SonidoAlarma]); canal LOW sin sonido
 * para los que ya llevan más de
 * 90 s vencidos (solo informan) y para la ongoing "En marcha" (Live Update en Android 16). Sin permiso de
 * notificaciones el sistema descarta la publicación. Los avisos llevan tag = slug e id = carril+tipo, así que cada
 * aviso nuevo sustituye al anterior, y caducan solos.
 */
@Suppress("TooManyFunctions")
class NotificadorAndroid
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val sonido: SonidoAlarma,
    ) : Notificador {
        private val gestor = context.getSystemService(NotificationManager::class.java)

        override fun publicar(avisos: List<AvisoNotificacion>) {
            crearCanales()
            avisos.forEach { a ->
                val canal = if (a.aviso.silencioso) CANAL_SILENCIOSO else CANAL_AVISOS
                val id = IdsNotificacion.aviso(a.carril, a.aviso.tipo)
                val builder =
                    Notification
                        .Builder(context, canal)
                        .setSmallIcon(R.drawable.ic_stat_souschef)
                        .setContentTitle("${a.carril} · ${a.tituloReceta}")
                        .setContentText(a.aviso.texto)
                        .setCategory(Notification.CATEGORY_ALARM)
                        .setAutoCancel(true)
                        .setShowWhen(true)
                        .setWhen(a.aviso.instanteMs)
                        .setTimeoutAfter(IdsNotificacion.caducidadMs(a.aviso.tipo))
                        .setGroup("crono:${a.slug}")
                        .setContentIntent(abrirCrono(a.slug))
                        .setDeleteIntent(parar(a.slug, id))
                val suena = IdsNotificacion.despiertaPantalla(a.aviso.tipo, a.aviso.silencioso)
                if (!a.aviso.silencioso) builder.addAction(accion(R.string.crono_notif_parar, parar(a.slug, id)))
                if (suena) builder.setFullScreenIntent(alarma(a, id), true)
                gestor.notify(a.slug, id, builder.build())
                if (suena) sonido.sonar()
            }
        }

        override fun enMarcha(
            slug: String,
            modelo: EnMarcha?,
        ) {
            val tag = IdsNotificacion.tagEnMarcha(slug)
            if (modelo == null) {
                gestor.cancel(tag, IdsNotificacion.ID_EN_MARCHA)
                return
            }
            crearCanales()
            val builder =
                Notification
                    .Builder(context, CANAL_SILENCIOSO)
                    .setSmallIcon(R.drawable.ic_stat_souschef)
                    .setContentTitle(modelo.titulo)
                    .setContentText(modelo.resumen)
                    .setStyle(Notification.BigTextStyle().bigText(modelo.lineas.joinToString("\n")))
                    .setCategory(Notification.CATEGORY_PROGRESS)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(abrirCrono(slug))
                    .setDeleteIntent(abandonar(slug))
                    .addAction(accion(R.string.crono_notif_siguiente, siguiente(slug)))
                    .addAction(accion(R.string.crono_notif_abrir, abrirCrono(slug)))
            val fin = modelo.finAtMs
            if (fin != null) {
                builder
                    .setShowWhen(true)
                    .setWhen(fin)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
            } else {
                builder.setShowWhen(false)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                builder.addExtras(Bundle().apply { putBoolean(EXTRA_PEDIR_PROMOCION, true) })
                if (modelo.cortoCritico.isNotEmpty()) builder.setShortCriticalText(modelo.cortoCritico)
            }
            gestor.notify(tag, IdsNotificacion.ID_EN_MARCHA, builder.build())
        }

        override fun cancelarAvisos(
            slug: String,
            carriles: Collection<String>,
        ) {
            carriles.forEach { c -> IdsNotificacion.avisosDe(c).forEach { gestor.cancel(slug, it) } }
            sonido.parar()
        }

        override fun silenciar(slug: String) {
            sonido.parar()
            gestor.activeNotifications.filter { it.tag == slug }.forEach { gestor.cancel(slug, it.id) }
        }

        override fun limpiar(slug: String) {
            sonido.parar()
            gestor.activeNotifications.filter { it.tag == slug }.forEach { gestor.cancel(slug, it.id) }
            gestor.cancel(IdsNotificacion.tagEnMarcha(slug), IdsNotificacion.ID_EN_MARCHA)
        }

        private fun accion(
            titulo: Int,
            intent: PendingIntent,
        ): Notification.Action =
            Notification.Action
                .Builder(
                    Icon.createWithResource(context, R.drawable.ic_stat_souschef),
                    context.getString(titulo),
                    intent,
                ).build()

        private fun siguiente(slug: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                slug.hashCode(),
                Intent(context, SiguienteReceiver::class.java)
                    .setAction(SiguienteReceiver.ACCION_SIGUIENTE)
                    .setData("souschef://siguiente/$slug".toUri())
                    .putExtra(SiguienteReceiver.EXTRA_SLUG, slug),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun parar(
            slug: String,
            id: Int,
        ): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                id xor slug.hashCode(),
                Intent(context, PararReceiver::class.java)
                    .setAction(PararReceiver.ACCION_PARAR)
                    .setData("souschef://parar/$slug/$id".toUri())
                    .putExtra(PararReceiver.EXTRA_SLUG, slug)
                    .putExtra(PararReceiver.EXTRA_ID, id),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun abandonar(slug: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                slug.hashCode(),
                Intent(context, PararReceiver::class.java)
                    .setAction(PararReceiver.ACCION_ABANDONAR)
                    .setData("souschef://abandonar/$slug".toUri())
                    .putExtra(PararReceiver.EXTRA_SLUG, slug),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun alarma(
            a: AvisoNotificacion,
            id: Int,
        ): PendingIntent =
            PendingIntent.getActivity(
                context,
                id xor a.slug.hashCode(),
                Intent(context, AlarmaActivity::class.java)
                    .setData("souschef://alarma/${a.slug}/$id".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(AlarmaActivity.EXTRA_SLUG, a.slug)
                    .putExtra(AlarmaActivity.EXTRA_TITULO, a.tituloReceta)
                    .putExtra(AlarmaActivity.EXTRA_CARRIL, a.carril)
                    .putExtra(AlarmaActivity.EXTRA_TEXTO, a.aviso.texto)
                    .putExtra(AlarmaActivity.EXTRA_INSTANTE, a.aviso.instanteMs)
                    .putExtra(AlarmaActivity.EXTRA_ID, id),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun abrirCrono(slug: String): PendingIntent =
            PendingIntent.getActivity(
                context,
                slug.hashCode(),
                Intent(Intent.ACTION_VIEW, "souschef://crono/$slug".toUri(), context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun crearCanales() {
            val avisos =
                NotificationChannel(
                    CANAL_AVISOS,
                    context.getString(R.string.crono_canal_avisos),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    setSound(null, null)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 150, 100, 150, 100, 300)
                }
            val silencioso =
                NotificationChannel(
                    CANAL_SILENCIOSO,
                    context.getString(R.string.crono_canal_silencioso),
                    NotificationManager.IMPORTANCE_LOW,
                )
            gestor.deleteNotificationChannel(CANAL_AVISOS_ANTIGUO)
            gestor.createNotificationChannels(listOf(avisos, silencioso))
        }

        private companion object {
            const val CANAL_AVISOS = "crono_avisos_v2"
            const val CANAL_AVISOS_ANTIGUO = "crono_avisos"
            const val CANAL_SILENCIOSO = "crono_silencioso"

            // Notification.EXTRA_REQUEST_PROMOTED_ONGOING: setRequestPromotedOngoing no está en el SDK 36.0.
            const val EXTRA_PEDIR_PROMOCION = "android.requestPromotedOngoing"
        }
    }
