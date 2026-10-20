package app.trombito.souschef.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Letra del sistema para toda la app. Se conservan los dos nombres (`bricolage`, `newsreader`)
 * como alias de la misma familia para no romper los usos existentes en `ui/crono`.
 */
class SousChefFonts(
    val bricolage: FontFamily = FontFamily.Default,
    val newsreader: FontFamily = FontFamily.Default,
)

/** Titulares en peso alto con tracking negativo; cuerpo en Normal. */
fun sousChefTypography(fonts: SousChefFonts = SousChefFonts()): Typography {
    val base = Typography()

    fun TextStyle.ui(
        weight: FontWeight,
        tracking: Double = 0.0,
    ) = copy(fontFamily = fonts.bricolage, fontWeight = weight, letterSpacing = tracking.sp)

    fun TextStyle.texto() = copy(fontFamily = fonts.newsreader, fontWeight = FontWeight.Normal)
    return base.copy(
        displayLarge = base.displayLarge.ui(FontWeight.Bold, -1.0),
        displayMedium = base.displayMedium.ui(FontWeight.Bold, -0.8),
        displaySmall = base.displaySmall.ui(FontWeight.Bold, -0.5),
        headlineLarge = base.headlineLarge.ui(FontWeight.Bold, -0.5),
        headlineMedium = base.headlineMedium.ui(FontWeight.Bold, -0.3),
        headlineSmall = base.headlineSmall.ui(FontWeight.SemiBold, -0.2),
        titleLarge = base.titleLarge.ui(FontWeight.SemiBold, -0.2),
        titleMedium = base.titleMedium.ui(FontWeight.SemiBold),
        titleSmall = base.titleSmall.ui(FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.texto().copy(fontSize = 18.sp, lineHeight = 27.sp),
        bodyMedium = base.bodyMedium.texto().copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodySmall = base.bodySmall.texto().copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelLarge = base.labelLarge.ui(FontWeight.SemiBold),
        labelMedium = base.labelMedium.ui(FontWeight.SemiBold),
        labelSmall = base.labelSmall.ui(FontWeight.SemiBold),
    )
}
