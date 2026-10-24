package app.trombito.souschef.ui.comun

import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True si conviene quitar el movimiento continuo (marquesinas): la escala de duración de animaciones está a 0 o
 * TalkBack (exploración táctil) está activo. Se lee una vez por composición; no observa cambios en vivo.
 */
@Composable
fun animacionesReducidas(): Boolean {
    val contexto = LocalContext.current
    return remember(contexto) {
        val escala = Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val accesibilidad = contexto.getSystemService(AccessibilityManager::class.java)
        escala == 0f || accesibilidad?.isTouchExplorationEnabled == true
    }
}
