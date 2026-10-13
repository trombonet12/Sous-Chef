package app.souschef.ui.theme

import android.content.res.AssetManager
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Fuentes variables en assets/fonts (compartidas con el HTML de impresión, ADR-0006).
private const val BRICOLAGE = "fonts/BricolageGrotesque.ttf"
private const val NEWSREADER = "fonts/Newsreader.ttf"
private const val NEWSREADER_ITALIC = "fonts/Newsreader-Italic.ttf"

private val PESOS =
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold)

private fun variable(
    path: String,
    assets: AssetManager,
    weight: FontWeight,
    style: FontStyle = FontStyle.Normal,
) = Font(
    path = path,
    assetManager = assets,
    weight = weight,
    style = style,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

class SousChefFonts(
    val bricolage: FontFamily,
    val newsreader: FontFamily,
)

fun sousChefFonts(assets: AssetManager): SousChefFonts =
    SousChefFonts(
        bricolage = FontFamily(PESOS.map { variable(BRICOLAGE, assets, it) }),
        newsreader =
            FontFamily(
                PESOS.map { variable(NEWSREADER, assets, it) } +
                    PESOS.map { variable(NEWSREADER_ITALIC, assets, it, FontStyle.Italic) },
            ),
    )

/** Titulares, UI y números en Bricolage Grotesque; texto corrido en Newsreader. */
fun sousChefTypography(fonts: SousChefFonts): Typography {
    val base = Typography()

    fun TextStyle.ui(weight: FontWeight) = copy(fontFamily = fonts.bricolage, fontWeight = weight)

    fun TextStyle.texto() = copy(fontFamily = fonts.newsreader)
    return base.copy(
        displayLarge = base.displayLarge.ui(FontWeight.ExtraBold),
        displayMedium = base.displayMedium.ui(FontWeight.ExtraBold),
        displaySmall = base.displaySmall.ui(FontWeight.ExtraBold),
        headlineLarge = base.headlineLarge.ui(FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.ui(FontWeight.Bold),
        headlineSmall = base.headlineSmall.ui(FontWeight.Bold),
        titleLarge = base.titleLarge.ui(FontWeight.Bold),
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
