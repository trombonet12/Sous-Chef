package app.souschef.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Tokens de la app vieja (docs/spec/app-legado.md §6). Papel, tinta y cuatro acentos: ámbar, azul, oliva, teja.

val LightColors =
    lightColorScheme(
        primary = Color(0xFF8C5A04), // ámbar oscurecido: #B07408 daba 3,35:1 sobre papel
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFF6E7C6),
        onPrimaryContainer = Color(0xFF3A2700),
        secondary = Color(0xFF2C6E92),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD6E7F0),
        onSecondaryContainer = Color(0xFF0E2A3A),
        tertiary = Color(0xFF4F6B26),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE0E8CD),
        onTertiaryContainer = Color(0xFF1A260A),
        error = Color(0xFFA8421C),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF5DED4),
        onErrorContainer = Color(0xFF3D1406),
        background = Color(0xFFEFEDE3),
        onBackground = Color(0xFF191A14),
        surface = Color(0xFFEFEDE3),
        onSurface = Color(0xFF191A14),
        surfaceVariant = Color(0xFFE7E5D9),
        onSurfaceVariant = Color(0xFF4A4C40),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFBFAF6),
        surfaceContainer = Color(0xFFF6F4EC),
        surfaceContainerHigh = Color(0xFFEDEBE0),
        surfaceContainerHighest = Color(0xFFE7E5D9),
        outline = Color(0xFF8A8C7C),
        outlineVariant = Color(0xFFD6D5C6),
        inverseSurface = Color(0xFF2E3028),
        inverseOnSurface = Color(0xFFEDEDE2),
        inversePrimary = Color(0xFFE5AB46),
    )

val DarkColors =
    darkColorScheme(
        primary = Color(0xFFE5AB46),
        onPrimary = Color(0xFF2A1C00),
        primaryContainer = Color(0xFF33280F),
        onPrimaryContainer = Color(0xFFF6E7C6),
        secondary = Color(0xFF79BEDE),
        onSecondary = Color(0xFF07202C),
        secondaryContainer = Color(0xFF14262F),
        onSecondaryContainer = Color(0xFFD6E7F0),
        tertiary = Color(0xFFA3C46A),
        onTertiary = Color(0xFF16220A),
        tertiaryContainer = Color(0xFF1D2611),
        onTertiaryContainer = Color(0xFFE0E8CD),
        error = Color(0xFFE8865F),
        onError = Color(0xFF2E0E03),
        errorContainer = Color(0xFF331709),
        onErrorContainer = Color(0xFFF5DED4),
        background = Color(0xFF12130F),
        onBackground = Color(0xFFEDEDE2),
        surface = Color(0xFF12130F),
        onSurface = Color(0xFFEDEDE2),
        surfaceVariant = Color(0xFF24261F),
        onSurfaceVariant = Color(0xFFBFC0B2),
        surfaceContainerLowest = Color(0xFF0D0E0B),
        surfaceContainerLow = Color(0xFF1B1C17),
        surfaceContainer = Color(0xFF1F201B),
        surfaceContainerHigh = Color(0xFF24261F),
        surfaceContainerHighest = Color(0xFF2E3028),
        outline = Color(0xFF7C7E70),
        outlineVariant = Color(0xFF2E3028),
        inverseSurface = Color(0xFFEDEDE2),
        inverseOnSurface = Color(0xFF191A14),
        inversePrimary = Color(0xFF8C5A04),
    )

/** La ficha es papel siempre, también en tema oscuro (se lee e imprime igual). */
object FichaPalette {
    val papel = Color(0xFFF3EFE4)
    val papel2 = Color(0xFFEAE3D2)
    val tinta = Color(0xFF17150F)
    val tinta2 = Color(0xFF4E483C)
    val tinta3 = Color(0xFF857C6A)
    val meta = Color(0xFF6E6656)
    val linea = Color(0xFFD5CDB9)
}

/** El cronómetro es oscuro siempre: se mira de reojo en la encimera. */
object CronoPalette {
    val fondo = Color(0xFF0D0C0A)
    val carta = Color(0xFF1C1915)
    val carta2 = Color(0xFF272219)
    val linea = Color(0xFF3A342C)
    val texto = Color(0xFFF4F1E8)
    val apagado = Color(0xFFB0A899)
    val tenue = Color(0xFF948B7C)
    val meto = Color(0xFF9ACB68)
    val saco = Color(0xFFEE9B79)
    val tapa = Color(0xFFE8836B)
    val alerta = Color(0xFFFF6B52)
    val ok = Color(0xFF8FC97A)
    val acento = Color(0xFFF0AE4A)
    val bordeOk = Color(0xFF3E5A34)
    val bordeMal = Color(0xFF5C2318)
    val parpadeo = Color(0xFF33140F)
    val principal = Color(0xFFF4F1E8)
    val onPrincipal = Color(0xFF0D0C0A)
    val urgente = Color(0xFFFF6B52)
    val onUrgente = Color(0xFF2A0A05)
}
