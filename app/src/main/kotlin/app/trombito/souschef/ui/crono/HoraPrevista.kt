package app.trombito.souschef.ui.crono

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import app.trombito.souschef.R
import app.trombito.souschef.receta.crono.ClaseEstado
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.theme.CronoPalette

/** Qué dice la línea de hora de un carril; solo depende del estado de arranque, nunca del desvío del plan. */
internal enum class TipoPrevista { EN_MIN, ESPERA_A, AHORA }

/** [minutos] solo cuenta en EN_MIN; [carril] es el número del carril que se espera (null si no se sabe). */
internal data class Prevista(
    val tipo: TipoPrevista,
    val minutos: Int = 0,
    val carril: Int? = null,
)

/** ESPERA: «en N min»; BLOQ: «espera a ②»; YA: «ahora»; en marcha, en pausa, vencido o terminado: nada. */
internal fun previstaDe(v: VistaCarril): Prevista? =
    when (v.clase) {
        ClaseEstado.ESPERA -> v.faltanMin?.let { Prevista(TipoPrevista.EN_MIN, minutos = it) }
        ClaseEstado.BLOQ -> Prevista(TipoPrevista.ESPERA_A, carril = v.bloqueadoPorN)
        ClaseEstado.YA -> Prevista(TipoPrevista.AHORA)
        else -> null
    }

private const val PRIMER_CIRCULO = 0x2460
private const val ULTIMO_NUMERO_CIRCULO = 20

/** El número del carril como la insignia: ①…⑳ y, de ahí en adelante, la cifra sin círculo. */
internal fun numeroCarril(n: Int): String =
    if (n in 1..ULTIMO_NUMERO_CIRCULO) (PRIMER_CIRCULO + n - 1).toChar().toString() else n.toString()

/**
 * Lo que toca con el carril: «en 22 min · 11:41» si espera su hora, «espera a ②» si espera a otro carril y «ahora» si
 * toca ya. En marcha no dice nada. Mismo color apagado siempre (sin alarmas); «ahora» va en negrita.
 */
@Composable
internal fun HoraPrevista(
    v: VistaCarril,
    modifier: Modifier = Modifier,
    estilo: TextStyle = MaterialTheme.typography.labelLarge,
) {
    val prevista = previstaDe(v) ?: return
    val hora = v.sugeridoMs?.let { formatoHora(it) }.orEmpty()
    val (texto, descripcion) =
        when (prevista.tipo) {
            TipoPrevista.EN_MIN -> {
                stringResource(R.string.crono_prevista_en_min, prevista.minutos, hora) to
                    stringResource(R.string.crono_a11y_prevista_en_min, prevista.minutos, hora)
            }

            TipoPrevista.ESPERA_A -> {
                val n = prevista.carril
                if (n == null) {
                    stringResource(R.string.crono_prevista_espera) to
                        stringResource(R.string.crono_a11y_prevista_espera)
                } else {
                    stringResource(R.string.crono_prevista_espera_a, numeroCarril(n)) to
                        stringResource(R.string.crono_a11y_prevista_espera_a, n)
                }
            }

            TipoPrevista.AHORA -> {
                stringResource(R.string.crono_prevista_ahora) to stringResource(R.string.crono_a11y_prevista_ahora)
            }
        }
    val ahora = prevista.tipo == TipoPrevista.AHORA
    Text(
        texto,
        modifier.semantics { contentDescription = descripcion },
        color = if (ahora) CronoPalette.texto else CronoPalette.apagado,
        fontWeight = if (ahora) FontWeight.Bold else null,
        style = estilo.merge(Tabular),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
