package app.trombito.souschef.data.alarma

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.core.net.toUri
import app.trombito.souschef.MainActivity
import app.trombito.souschef.R
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.crono.CarrilNotificacion
import app.trombito.souschef.domain.crono.IdsNotificacion
import app.trombito.souschef.domain.crono.Notificador
import app.trombito.souschef.domain.crono.ResumenCocina
import app.trombito.souschef.domain.crono.SituacionCarril
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Potencia
import app.trombito.souschef.receta.crono.AgrupadorAlarmas
import app.trombito.souschef.receta.crono.AlarmaActiva
import app.trombito.souschef.ui.crono.AlarmaActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Notificaciones del cronómetro, pocas a propósito. [cocina]: una sola fija (canal silencioso `crono_cocina`,
 * ongoing) con una línea por carril, cuenta atrás nativa y, en Android 16, chip de Live Update con la hora de fin.
 * [alarma]: una sola en el canal `crono_alarma_v3` (HIGH, tono de alarma del sistema), que suena solo si `sonar`
 * (insistente hasta Parar, Hecho o 60 s) y agrupa en líneas lo que coincide. El sonido lo pone solo el canal: no hay
 * reproductor propio ni servicio (ADR-0005). La pantalla completa solo se pide con el móvil bloqueado y si el sistema
 * la concede.
 */
@Suppress("TooManyFunctions")
class NotificadorAndroid
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val ajustes: AjustesRepo,
    ) : Notificador {
        private val gestor = context.getSystemService(NotificationManager::class.java)
        private var canalesListos = false

        /**
         * Cocina del usuario (ADR-0014). La cadena de alarmas trabaja con la receta canónica; el texto y la potencia
         * se traducen aquí, al pintar. Se lee de Ajustes en cada pintado: `first()` suspende y Room consulta fuera del
         * hilo principal, así que nada bloquea y siempre es la cocina real (también con el proceso recién arrancado).
         */
        private suspend fun cocinaDelUsuario(): Cocina = ajustes.observar().first().cocina

        override suspend fun cocina(resumen: ResumenCocina) {
            val slug = resumen.slug
            limpiarAntiguas(slug)
            if (resumen.carriles.isEmpty()) {
                quitarCocina(slug)
                return
            }
            crearCanales()
            gestor.notify(IdsNotificacion.tagCocina(slug), IdsNotificacion.ID_COCINA, notificacionCocina(resumen))
        }

        override suspend fun alarma(
            slug: String,
            titulo: String,
            alarma: AlarmaActiva,
            sonar: Boolean,
            urgente: String?,
        ) {
            if (alarma.lineas.isEmpty()) {
                quitarAlarma(slug)
                return
            }
            crearCanales()
            val ahora = System.currentTimeMillis()
            // El tope es el de la alarma entera: una actualización en silencio no alarga lo que queda de sonido.
            val restante = (AgrupadorAlarmas.VENTANA_MS - (ahora - alarma.inicioMs)).coerceAtLeast(TIMEOUT_MINIMO_MS)
            val cocina = cocinaDelUsuario()
            val textos = alarma.lineas.map { Potencia.reescribir(it.texto, cocina) }
            val builder =
                Notification
                    .Builder(context, CANAL_ALARMA)
                    .setSmallIcon(R.drawable.ic_stat_souschef)
                    .setContentTitle(titulo)
                    .setContentText(textos.first())
                    .setStyle(Notification.BigTextStyle().bigText(textos.joinToString("\n")))
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setShowWhen(true)
                    .setWhen(alarma.inicioMs)
                    .setOnlyAlertOnce(!sonar)
                    .setTimeoutAfter(restante)
                    .setContentIntent(abrirCrono(slug))
                    .addAction(accion(R.string.crono_notif_parar, parar(slug)))
            if (urgente != null) builder.addAction(accion(R.string.crono_notif_hecho_siguiente, siguiente(slug)))
            if (sonar && bloqueado() && puedePantallaCompleta()) {
                builder.setFullScreenIntent(pantallaCompleta(slug, titulo, textos, alarma.inicioMs, urgente), true)
            }
            val notificacion = builder.build()
            if (sonar) notificacion.flags = notificacion.flags or Notification.FLAG_INSISTENT
            gestor.notify(IdsNotificacion.tagAlarma(slug), IdsNotificacion.ID_ALARMA, notificacion)
        }

        override fun quitarAlarma(slug: String) {
            gestor.cancel(IdsNotificacion.tagAlarma(slug), IdsNotificacion.ID_ALARMA)
        }

        override fun quitarCocina(slug: String) {
            gestor.cancel(IdsNotificacion.tagCocina(slug), IdsNotificacion.ID_COCINA)
        }

        override fun limpiar(slug: String) {
            quitarAlarma(slug)
            quitarCocina(slug)
            limpiarAntiguas(slug)
        }

        /** Las notificaciones de versiones anteriores (En marcha, una por carril) se quitan por si quedaron. */
        private fun limpiarAntiguas(slug: String) {
            gestor.cancel(IdsNotificacion.tagEnMarcha(slug), IdsNotificacion.ID_EN_MARCHA)
            val tag = IdsNotificacion.tagCarrilAntiguo(slug)
            gestor.activeNotifications.filter { it.tag == tag }.forEach { gestor.cancel(tag, it.id) }
        }

        /**
         * Desde Android 14 Play ya no concede USE_FULL_SCREEN_INTENT de oficio a un temporizador y el usuario puede
         * retirarlo. Sin él la alarma sale igual (HIGH, con el sonido del canal), solo que sin pantalla completa.
         * Es una propiedad para poder simular la denegación en tests: Robolectric no la expone.
         */
        internal var puedePantallaCompleta: () -> Boolean = {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || gestor.canUseFullScreenIntent()
        }

        /** Bloqueado o con la pantalla apagada: la pantalla completa la enciende y deja la alarma a la vista. */
        private fun bloqueado(): Boolean =
            context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true ||
                context.getSystemService(PowerManager::class.java)?.isInteractive == false

        private suspend fun notificacionCocina(r: ResumenCocina): Notification {
            val cocina = cocinaDelUsuario()
            val lineas = r.carriles.map { linea(it, cocina) }
            // BigTextStyle (no Inbox): es de los estilos que Android 16 deja promocionar a Live Update.
            val builder =
                Notification
                    .Builder(context, CANAL_COCINA)
                    .setSmallIcon(R.drawable.ic_stat_souschef)
                    .setContentTitle(titulo(r))
                    .setContentText(lineas.first())
                    .setStyle(Notification.BigTextStyle().bigText(lineas.joinToString("\n")))
                    .setCategory(Notification.CATEGORY_PROGRESS)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(abrirCrono(r.slug))
            cuentaAtras(builder, r.finProximoMs)
            if (r.urgente != null) builder.addAction(accion(R.string.crono_notif_hecho, siguiente(r.slug)))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && r.finProximoMs != null) {
                builder.addExtras(Bundle().apply { putBoolean(EXTRA_PEDIR_PROMOCION, true) })
                // Hora absoluta de fin: no envejece entre repintados, a diferencia de una cuenta atrás.
                builder.setShortCriticalText(hora(r.finProximoMs))
            }
            return builder.build()
        }

        private fun titulo(r: ResumenCocina): String =
            r.llegadaMs?.let { context.getString(R.string.crono_cocina_titulo, r.tituloReceta, hora(it)) }
                ?: r.tituloReceta

        /** «SARTÉN GRANDE · Primera fritura · fuego 4 · acaba 15:12». Siempre horas absolutas, nunca cuentas atrás. */
        private fun linea(
            c: CarrilNotificacion,
            cocina: Cocina,
        ): String =
            listOfNotNull(
                c.nombre,
                Potencia.reescribir(c.texto, cocina),
                c.fuego?.let { context.getString(R.string.crono_cocina_fuego, Potencia.texto(it, cocina)) },
                when (c.situacion) {
                    SituacionCarril.CORRIENDO -> {
                        c.finAtMs?.let { context.getString(R.string.crono_cocina_acaba, hora(it)) }
                    }

                    SituacionCarril.VENCIDO -> {
                        context.getString(R.string.crono_cocina_listo)
                    }

                    SituacionCarril.PAUSA -> {
                        context.getString(R.string.crono_cocina_pausa)
                    }

                    SituacionCarril.SIN_ARRANCAR -> {
                        context.getString(R.string.crono_cocina_sin_arrancar)
                    }
                },
            ).joinToString(SEPARADOR)

        private fun hora(ms: Long): String =
            FORMATO_HORA.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

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

        private fun parar(slug: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                IdsNotificacion.ID_ALARMA xor slug.hashCode(),
                Intent(context, PararReceiver::class.java)
                    .setAction(PararReceiver.ACCION_PARAR)
                    .setData("souschef://parar/$slug".toUri())
                    .putExtra(PararReceiver.EXTRA_SLUG, slug),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun pantallaCompleta(
            slug: String,
            titulo: String,
            lineas: List<String>,
            instanteMs: Long,
            urgente: String?,
        ): PendingIntent =
            PendingIntent.getActivity(
                context,
                IdsNotificacion.ID_ALARMA xor slug.hashCode(),
                Intent(context, AlarmaActivity::class.java)
                    .setData("souschef://alarma/$slug".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(AlarmaActivity.EXTRA_SLUG, slug)
                    .putExtra(AlarmaActivity.EXTRA_TITULO, titulo)
                    .putExtra(AlarmaActivity.EXTRA_LINEAS, lineas.toTypedArray())
                    .putExtra(AlarmaActivity.EXTRA_INSTANTE, instanteMs)
                    .putExtra(AlarmaActivity.EXTRA_HECHO, urgente != null),
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
            val cocina =
                NotificationChannel(
                    CANAL_COCINA,
                    context.getString(R.string.crono_canal_cocina),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                }
            CANALES_ANTIGUOS.forEach { gestor.deleteNotificationChannel(it) }
            gestor.createNotificationChannels(listOf(alarma, cocina))
            canalesListos = true
        }

        private companion object {
            const val CANAL_ALARMA = "crono_alarma_v3"
            const val CANAL_COCINA = "crono_cocina"
            val CANALES_ANTIGUOS = listOf("crono_avisos", "crono_avisos_v2", "crono_silencioso", "crono_carriles")
            const val SEPARADOR = " · "
            const val TIMEOUT_MINIMO_MS = 1_000L
            val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            // Notification.EXTRA_REQUEST_PROMOTED_ONGOING: setRequestPromotedOngoing no está en el SDK 36.0.
            const val EXTRA_PEDIR_PROMOCION = "android.requestPromotedOngoing"
        }
    }
