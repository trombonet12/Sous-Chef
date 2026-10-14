package app.souschef.data.alarma

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Tono de alarma (USAGE_ALARM) en bucle hasta [parar] o [MAX_MS]; un único dueño para poder pararlo. */
@Singleton
class SonidoAlarma
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val principal = Handler(Looper.getMainLooper())
        private val autoParada = Runnable { detener() }
        private var reproductor: MediaPlayer? = null

        fun sonar() {
            principal.post {
                principal.removeCallbacks(autoParada)
                if (reproductor == null) reproductor = crear()
                if (reproductor != null) principal.postDelayed(autoParada, MAX_MS)
            }
        }

        fun parar() {
            principal.post { detener() }
        }

        private fun detener() {
            principal.removeCallbacks(autoParada)
            reproductor?.runCatching {
                stop()
                release()
            }
            reproductor = null
        }

        @Suppress("TooGenericExceptionCaught")
        private fun crear(): MediaPlayer? {
            val uri =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            return try {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    setDataSource(context, uri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.e("SousChef", "No se pudo reproducir el sonido de alarma", e)
                null
            }
        }

        private companion object {
            const val MAX_MS = 60_000L
        }
    }
