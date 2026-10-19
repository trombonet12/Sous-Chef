package app.souschef.ui.recetas

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Color de una familia de base: [acento] para la franja y el punto, [tinte] para el fondo de la tarjeta. */
data class ColorBase(
    val acento: Color,
    val tinte: Color,
)

private const val LUMINANCIA_OSCURO = 0.5f

/**
 * Siete tonos apagados del mismo papel que la app, para distinguir la base de un vistazo sin saturar. El color nunca va
 * solo: la base siempre aparece escrita al lado.
 */
object ColoresBase {
    private val CLAROS =
        mapOf(
            FamiliaBase.PASTA to ColorBase(Color(0xFFC99A2E), Color(0xFFF5EBD3)),
            FamiliaBase.ARROZ to ColorBase(Color(0xFF8C9A5B), Color(0xFFEAEDDB)),
            FamiliaBase.PATATA to ColorBase(Color(0xFFB9774A), Color(0xFFF3E3D6)),
            FamiliaBase.PESCADO to ColorBase(Color(0xFF4F86A6), Color(0xFFDEEAF0)),
            FamiliaBase.VERDURA to ColorBase(Color(0xFF5E9A6B), Color(0xFFDCEBDF)),
            FamiliaBase.MASAS to ColorBase(Color(0xFFB5707A), Color(0xFFF2DEE0)),
            FamiliaBase.SALTEADO to ColorBase(Color(0xFF8A78B0), Color(0xFFE7E2F0)),
            FamiliaBase.OTRA to ColorBase(Color(0xFF9A978A), Color(0xFFECEAE1)),
        )

    private val OSCUROS =
        mapOf(
            FamiliaBase.PASTA to ColorBase(Color(0xFFE0B657), Color(0xFF2E2717)),
            FamiliaBase.ARROZ to ColorBase(Color(0xFFB3C27E), Color(0xFF252A19)),
            FamiliaBase.PATATA to ColorBase(Color(0xFFD9976A), Color(0xFF2F2219)),
            FamiliaBase.PESCADO to ColorBase(Color(0xFF7FB3D1), Color(0xFF18252D)),
            FamiliaBase.VERDURA to ColorBase(Color(0xFF8CC79A), Color(0xFF18291D)),
            FamiliaBase.MASAS to ColorBase(Color(0xFFD9939D), Color(0xFF2E1E21)),
            FamiliaBase.SALTEADO to ColorBase(Color(0xFFB3A2D9), Color(0xFF241F30)),
            FamiliaBase.OTRA to ColorBase(Color(0xFFB9B6A8), Color(0xFF24251F)),
        )

    fun de(
        familia: FamiliaBase,
        oscuro: Boolean,
    ): ColorBase = (if (oscuro) OSCUROS else CLAROS).getValue(familia)
}

/** Color de la familia según el tema en uso (se deduce del fondo, así respeta un tema forzado). */
@Composable
@ReadOnlyComposable
fun colorBase(familia: FamiliaBase): ColorBase =
    ColoresBase.de(familia, MaterialTheme.colorScheme.background.luminance() < LUMINANCIA_OSCURO)
