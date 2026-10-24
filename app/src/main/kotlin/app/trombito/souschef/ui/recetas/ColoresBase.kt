package app.trombito.souschef.ui.recetas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import app.trombito.souschef.ui.comun.ColorFamilia
import app.trombito.souschef.ui.comun.Paleta
import app.trombito.souschef.ui.comun.TonoPaleta
import app.trombito.souschef.ui.comun.colorTono

/** Color de una familia de base: acento para la franja y el punto, tinte para el fondo. */
typealias ColorBase = ColorFamilia

/** Cada familia de base usa un tono de la paleta común. */
object ColoresBase {
    private val TONOS =
        mapOf(
            FamiliaBase.PASTA to TonoPaleta.AMBAR,
            FamiliaBase.ARROZ to TonoPaleta.OLIVA,
            FamiliaBase.PATATA to TonoPaleta.TEJA,
            FamiliaBase.PESCADO to TonoPaleta.AZUL,
            FamiliaBase.VERDURA to TonoPaleta.VERDE,
            FamiliaBase.MASAS to TonoPaleta.ROSA,
            FamiliaBase.SALTEADO to TonoPaleta.LILA,
            FamiliaBase.CARNES to TonoPaleta.GRANATE,
            FamiliaBase.POSTRES to TonoPaleta.CIRUELA,
            FamiliaBase.OTRA to TonoPaleta.PIEDRA,
        )

    fun tono(familia: FamiliaBase): TonoPaleta = TONOS.getValue(familia)

    fun de(
        familia: FamiliaBase,
        oscuro: Boolean,
    ): ColorBase = Paleta.de(tono(familia), oscuro)
}

/** Color de la familia según el tema en uso (se deduce del fondo, así respeta un tema forzado). */
@Composable
@ReadOnlyComposable
fun colorBase(familia: FamiliaBase): ColorBase = colorTono(ColoresBase.tono(familia))
