package app.souschef.ui.crono

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import app.souschef.receta.crono.TipoAviso
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/** Sonido y vibración con la app en primer plano (docs/spec/app-legado.md §4.11). */
interface Avisador {
    fun sonar(tipo: TipoAviso)

    /** Triple pitido al volver de segundo plano con avisos perdidos. */
    fun resumen()

    /** Triple pitido de "todo listo". */
    fun cierre()
}

class AvisadorAndroid
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Avisador {
        private val manejador = Handler(Looper.getMainLooper())
        private val vibrador = context.getSystemService(Vibrator::class.java)

        override fun sonar(tipo: TipoAviso) {
            when (tipo) {
                TipoAviso.HITO -> {
                    pitidos(listOf(0L to ToneGenerator.TONE_PROP_BEEP, 200L to ToneGenerator.TONE_PROP_BEEP2))
                    vibrar(longArrayOf(0, 90, 70, 90))
                }

                TipoAviso.DIEZ_S -> {
                    pitidos(listOf(0L to ToneGenerator.TONE_PROP_BEEP))
                    vibrar(longArrayOf(0, 120))
                }

                TipoAviso.FIN -> {
                    triple()
                    vibrar(longArrayOf(0, 150, 100, 150, 100, 300))
                }

                TipoAviso.TOCA_EMPEZAR -> {
                    pitidos(listOf(0L to ToneGenerator.TONE_PROP_BEEP))
                    vibrar(longArrayOf(0, 100, 80, 100))
                }
            }
        }

        override fun resumen() {
            triple()
            vibrar(longArrayOf(0, 200, 90, 200))
        }

        override fun cierre() {
            triple()
            vibrar(longArrayOf(0, 200, 100, 200))
        }

        private fun triple() = pitidos(List(3) { it * 300L to ToneGenerator.TONE_PROP_BEEP2 })

        private fun pitidos(secuencia: List<Pair<Long, Int>>) {
            val generador = ToneGenerator(AudioManager.STREAM_ALARM, VOLUMEN)
            secuencia.forEach { (retardo, tono) ->
                manejador.postDelayed({ generador.startTone(tono, DURACION_MS) }, retardo)
            }
            manejador.postDelayed({ generador.release() }, (secuencia.maxOf { it.first }) + LIBERAR_MS)
        }

        private fun vibrar(patron: LongArray) {
            if (vibrador?.hasVibrator() == true) {
                vibrador.vibrate(VibrationEffect.createWaveform(patron, -1))
            }
        }

        private companion object {
            const val VOLUMEN = 100
            const val DURACION_MS = 200
            const val LIBERAR_MS = 800L
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class AvisadorModule {
    @Binds abstract fun avisador(impl: AvisadorAndroid): Avisador
}
