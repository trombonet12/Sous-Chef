package app.trombito.souschef.data.resena

import android.app.Activity
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/** La actividad visible, para que Play pueda pintar su diálogo encima. Referencia débil: no retiene la actividad. */
@Singleton
class ActividadActual
    @Inject
    constructor() {
        @Volatile private var referencia: WeakReference<Activity>? = null

        fun fijar(actividad: Activity) {
            referencia = WeakReference(actividad)
        }

        /** Quita [actividad] solo si sigue siendo la actual (una nueva puede haberla sustituido ya). */
        fun quitar(actividad: Activity) {
            if (referencia?.get() === actividad) referencia = null
        }

        fun actual(): Activity? = referencia?.get()
    }
