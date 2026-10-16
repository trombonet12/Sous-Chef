package app.souschef.data.alarma

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.core.net.toUri
import app.souschef.MainActivity
import app.souschef.R
import app.souschef.domain.crono.AvisoNotificacion
import app.souschef.domain.crono.CarrilNotificacion
import app.souschef.domain.crono.EnMarcha
import app.souschef.domain.crono.IdsNotificacion
import app.souschef.domain.crono.Notificador
import app.souschef.ui.crono.AlarmaActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Notificaciones del cronómetro. Una por carril en el canal silencioso `crono_carriles` (ongoing, con cuenta atrás y
 * progreso); cuando acaba un paso, el mismo id se vuelve a publicar en el canal `crono_alarma_v3` (HIGH, tono de alarma
 * del sistema, insistente hasta que se cancela o a los 60 s) y Parar/Hecho/Siguiente lo devuelven a su estado normal.
 * El sonido lo pone solo el canal: no hay reproductor propio ni servicio (ADR-0005). "En marcha" es el resumen ongoing
 * promocionado (Live Update en Android 16), suelto y sin acción de descarte.
 */
@Suppress("TooManyFunctions")
class NotificadorAndroid
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Notificador {
        private val gestor = context.getSystemService(NotificationManager::class.java)
        private var canalesListos = false

        override fun publicar(avisos: List<AvisoNotificacion>) {
            crearCanales()
            // Un aviso nuevo del mismo carril sustituye al anterior (mismo id): manda el último.
            avisos.groupBy { it.slug to it.aviso.carril }.values.forEach { grupo ->
                val a = grupo.maxBy { it.aviso.instanteMs }
                val id = IdsNotificacion.carril(a.aviso.carril)
                val builder =
                    Notification
                        .Builder(context, CANAL_ALARMA)
                        .setSmallIcon(R.drawable.ic_stat_souschef)
                        .setContentTitle("${a.carril} · ${a.tituloReceta}")
                        .setContentText(a.aviso.texto)
                        .setCategory(Notification.CATEGORY_ALARM)
                        .setShowWhen(true)
                        .setWhen(a.aviso.instanteMs)
                        .setTimeoutAfter(IdsNotificacion.CADUCIDAD_ALARMA_MS)
                        .setGroup(IdsNotificacion.grupoCarriles(a.slug))
                        .setContentIntent(abrirCrono(a.slug))
                        .setFullScreenIntent(alarma(a, id), true)
                        .addAction(accion(R.string.crono_notif_siguiente, siguiente(a.slug)))
                        .addAction(accion(R.string.crono_notif_parar, parar(a.slug, id)))
                val notificacion = builder.build()
                notificacion.flags = notificacion.flags or Notification.FLAG_INSISTENT
                gestor.notify(IdsNotificacion.tagCarril(a.slug), id, notificacion)
            }
        }

        override fun carriles(
            slug: String,
            carriles: List<CarrilNotificacion>,
        ) {
            val tag = IdsNotificacion.tagCarril(slug)
            val activas = gestor.activeNotifications.filter { it.tag == tag }
            val ids = carriles.map { it.id }.toSet()
            activas
                .filter { it.id != IdsNotificacion.ID_RESUMEN && it.id !in ids }
                .forEach { gestor.cancel(tag, it.id) }
            if (carriles.isEmpty()) {
                gestor.cancel(tag, IdsNotificacion.ID_RESUMEN)
                return
            }
            crearCanales()
            // Un carril que está sonando no se pisa: lo devuelve a normal Parar, Hecho o Siguiente.
            val sonando = activas.filter { it.notification.channelId == CANAL_ALARMA }.map { it.id }.toSet()
            carriles.filter { it.id !in sonando }.forEach { gestor.notify(tag, it.id, notificacionCarril(it)) }
            gestor.notify(tag, IdsNotificacion.ID_RESUMEN, resumen(slug, carriles.first().tituloReceta))
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
                    .Builder(context, CANAL_CARRILES)
                    .setSmallIcon(R.drawable.ic_stat_souschef)
                    .setContentTitle(modelo.titulo)
                    .setContentText(modelo.resumen)
                    .setStyle(Notification.BigTextStyle().bigText(modelo.lineas.joinToString("\n")))
                    .setCategory(Notification.CATEGORY_PROGRESS)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(abrirCrono(slug))
                    .addAction(accion(R.string.crono_notif_siguiente, siguiente(slug)))
                    .addAction(accion(R.string.crono_notif_abrir, abrirCrono(slug)))
            cuentaAtras(builder, modelo.finAtMs)
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
            carriles.forEach { gestor.cancel(IdsNotificacion.tagCarril(slug), IdsNotificacion.carril(it)) }
        }

        override fun silenciar(slug: String) {
            val tag = IdsNotificacion.tagCarril(slug)
            gestor.activeNotifications
                .filter { it.tag == tag && it.notification.channelId == CANAL_ALARMA }
                .forEach { gestor.cancel(tag, it.id) }
        }

        override fun limpiar(slug: String) {
            val tag = IdsNotificacion.tagCarril(slug)
            gestor.activeNotifications.filter { it.tag == tag }.forEach { gestor.cancel(tag, it.id) }
            gestor.cancel(IdsNotificacion.tagEnMarcha(slug), IdsNotificacion.ID_EN_MARCHA)
        }

        private fun notificacionCarril(c: CarrilNotificacion): Notification {
            val color = colorDe(c.color)
            val builder =
                Notification
                    .Builder(context, CANAL_CARRILES)
                    .setSmallIcon(R.drawable.ic_stat_souschef)
                    .setContentTitle(c.titulo)
                    .setContentText(c.detalle)
                    .setCategory(Notification.CATEGORY_PROGRESS)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setColor(color)
                    .setGroup(IdsNotificacion.grupoCarriles(c.slug))
                    .setContentIntent(abrirCrono(c.slug))
                    .addAction(accion(R.string.crono_notif_siguiente, siguiente(c.slug)))
            cuentaAtras(builder, c.finAtMs)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                val tramos = List(c.total) { Notification.ProgressStyle.Segment(1).setColor(color) }
                builder.setStyle(
                    Notification
                        .ProgressStyle()
                        .setProgressSegments(tramos)
                        .setProgress(c.hechos)
                        .setStyledByProgress(false),
                )
            } else {
                builder.setProgress(c.total, c.hechos, false)
            }
            return builder.build()
        }

        private fun resumen(
            slug: String,
            titulo: String,
        ): Notification =
            Notification
                .Builder(context, CANAL_CARRILES)
                .setSmallIcon(R.drawable.ic_stat_souschef)
                .setContentTitle(titulo)
                .setGroup(IdsNotificacion.grupoCarriles(slug))
                .setGroupSummary(true)
                .setGroupAlertBehavior(Notification.GROUP_ALERT_CHILDREN)
                .setOnlyAlertOnce(true)
                .setContentIntent(abrirCrono(slug))
                .build()

        private fun cuentaAtras(
            builder: Notification.Builder,
            finAtMs: Long?,
        ) {
            if (finAtMs != null) {
                builder
                    .setShowWhen(true)
                    .setWhen(finAtMs)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
            } else {
                builder.setShowWhen(false)
            }
        }

        private fun colorDe(hex: String): Int = runCatching { Color.parseColor(hex) }.getOrDefault(COLOR_NEUTRO)

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

        /** Idempotente: crear un canal que ya existe no lo toca. Los antiguos se borran una vez por proceso. */
        private fun crearCanales() {
            if (canalesListos) return
            val tono =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val alarma =
                NotificationChannel(
                    CANAL_ALARMA,
                    context.getString(R.string.crono_canal_alarma),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    setSound(
                        tono,
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 800)
                }
            val carriles =
                NotificationChannel(
                    CANAL_CARRILES,
                    context.getString(R.string.crono_canal_carriles),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                }
            CANALES_ANTIGUOS.forEach { gestor.deleteNotificationChannel(it) }
            gestor.createNotificationChannels(listOf(alarma, carriles))
            canalesListos = true
        }

        private companion object {
            const val CANAL_ALARMA = "crono_alarma_v3"
            const val CANAL_CARRILES = "crono_carriles"
            val CANALES_ANTIGUOS = listOf("crono_avisos", "crono_avisos_v2", "crono_silencioso")
            val COLOR_NEUTRO = Color.rgb(0xB0, 0xA8, 0x99)

            // Notification.EXTRA_REQUEST_PROMOTED_ONGOING: setRequestPromotedOngoing no está en el SDK 36.0.
            const val EXTRA_PEDIR_PROMOCION = "android.requestPromotedOngoing"
        }
    }
