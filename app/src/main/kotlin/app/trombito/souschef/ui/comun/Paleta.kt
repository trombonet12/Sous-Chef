package app.trombito.souschef.ui.comun

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Color de un tono: [acento] para la franja y el punto, [tinte] para el fondo de la tarjeta. */
data class ColorFamilia(
    val acento: Color,
    val tinte: Color,
)

/** Los diez tonos con nombre de la paleta: apagados, del mismo papel que la app. */
enum class TonoPaleta { AMBAR, OLIVA, TEJA, AZUL, VERDE, ROSA, LILA, PIEDRA, GRANATE, CIRUELA }

private const val LUMINANCIA_OSCURO = 0.5f

/**
 * Pares acento/tinte de cada [TonoPaleta] en claro y oscuro. El color nunca va solo: siempre hay un texto al lado.
 */
object Paleta {
    private val CLAROS =
        mapOf(
            TonoPaleta.AMBAR to ColorFamilia(Color(0xFFC99A2E), Color(0xFFF5EBD3)),
            TonoPaleta.OLIVA to ColorFamilia(Color(0xFF8C9A5B), Color(0xFFEAEDDB)),
            TonoPaleta.TEJA to ColorFamilia(Color(0xFFB9774A), Color(0xFFF3E3D6)),
            TonoPaleta.AZUL to ColorFamilia(Color(0xFF4F86A6), Color(0xFFDEEAF0)),
            TonoPaleta.VERDE to ColorFamilia(Color(0xFF5E9A6B), Color(0xFFDCEBDF)),
            TonoPaleta.ROSA to ColorFamilia(Color(0xFFB5707A), Color(0xFFF2DEE0)),
            TonoPaleta.LILA to ColorFamilia(Color(0xFF8A78B0), Color(0xFFE7E2F0)),
            TonoPaleta.PIEDRA to ColorFamilia(Color(0xFF9A978A), Color(0xFFECEAE1)),
            TonoPaleta.GRANATE to ColorFamilia(Color(0xFFA8564F), Color(0xFFF0DCD9)),
            TonoPaleta.CIRUELA to ColorFamilia(Color(0xFF94658C), Color(0xFFEEE0EC)),
        )

    private val OSCUROS =
        mapOf(
            TonoPaleta.AMBAR to ColorFamilia(Color(0xFFE0B657), Color(0xFF2E2717)),
            TonoPaleta.OLIVA to ColorFamilia(Color(0xFFB3C27E), Color(0xFF252A19)),
            TonoPaleta.TEJA to ColorFamilia(Color(0xFFD9976A), Color(0xFF2F2219)),
            TonoPaleta.AZUL to ColorFamilia(Color(0xFF7FB3D1), Color(0xFF18252D)),
            TonoPaleta.VERDE to ColorFamilia(Color(0xFF8CC79A), Color(0xFF18291D)),
            TonoPaleta.ROSA to ColorFamilia(Color(0xFFD9939D), Color(0xFF2E1E21)),
            TonoPaleta.LILA to ColorFamilia(Color(0xFFB3A2D9), Color(0xFF241F30)),
            TonoPaleta.PIEDRA to ColorFamilia(Color(0xFFB9B6A8), Color(0xFF24251F)),
            TonoPaleta.GRANATE to ColorFamilia(Color(0xFFDB8D85), Color(0xFF2E1D1B)),
            TonoPaleta.CIRUELA to ColorFamilia(Color(0xFFCB9CC2), Color(0xFF2A1E28)),
        )

    fun de(
        tono: TonoPaleta,
        oscuro: Boolean,
    ): ColorFamilia = (if (oscuro) OSCUROS else CLAROS).getValue(tono)
}

/** Color del tono según el tema en uso (se deduce del fondo, así respeta un tema forzado). */
@Composable
@ReadOnlyComposable
fun colorTono(tono: TonoPaleta): ColorFamilia =
    Paleta.de(tono, MaterialTheme.colorScheme.background.luminance() < LUMINANCIA_OSCURO)
