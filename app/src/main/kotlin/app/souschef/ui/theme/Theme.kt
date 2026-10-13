package app.souschef.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val LocalFonts = staticCompositionLocalOf<SousChefFonts> { error("SousChefTheme no aplicado") }

private val SousChefShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

@Composable
fun SousChefTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val assets = LocalContext.current.assets
    val fonts = remember(assets) { sousChefFonts(assets) }
    val typography = remember(fonts) { sousChefTypography(fonts) }
    CompositionLocalProvider(LocalFonts provides fonts) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = typography,
            shapes = SousChefShapes,
            content = content,
        )
    }
}
