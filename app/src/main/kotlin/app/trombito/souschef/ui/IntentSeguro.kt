package app.trombito.souschef.ui

import android.content.Intent
import androidx.navigation.NavController

private const val ESQUEMA_APP = "souschef"
private val HOSTS_APP = setOf("crono", "ficha")

/** Extras con los que Navigation abriría cualquier destino con cualquier argumento (los trae `handleDeepLink`). */
private val EXTRAS_DE_NAVEGACION =
    listOf(
        // Literales: las constantes de NavController son @RestrictTo (lint RestrictedApi). Valen lo mismo.
        "android-support-nav:controller:deepLinkIds",
        "android-support-nav:controller:deepLinkArgs",
        "android-support-nav:controller:deepLinkExtras",
        NavController.KEY_DEEP_LINK_INTENT,
    )

/**
 * Sanea in situ un intent recibido de fuera, antes de que Navigation lo vea: quita los extras de navegación y deja la
 * `data` solo si es `souschef://crono/...` o `souschef://ficha/...`. Conserva el extra `atajo` y el resto (las
 * notificaciones del crono abren la app solo con la `data`). Devuelve el mismo [intent].
 */
fun sanearIntent(intent: Intent): Intent {
    EXTRAS_DE_NAVEGACION.forEach(intent::removeExtra)
    val data = intent.data
    if (data != null && !(data.scheme == ESQUEMA_APP && data.host in HOSTS_APP)) intent.data = null
    return intent
}
