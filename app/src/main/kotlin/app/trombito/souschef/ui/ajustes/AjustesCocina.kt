package app.trombito.souschef.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trombito.souschef.R
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.FUEGOS_MAX
import app.trombito.souschef.domain.FUEGOS_MIN
import app.trombito.souschef.domain.RACIONES_MAX
import app.trombito.souschef.domain.RACIONES_MIN
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.ui.comun.PasosMasMenos

private const val SEPARADOR = " · "

@Composable
fun AjustesCocinaScreen(
    onAtras: () -> Unit,
    viewModel: AjustesViewModel = hiltViewModel(),
) {
    val ajustes by viewModel.estado.collectAsStateWithLifecycle()
    AjustesCocina(
        ajustes,
        onAtras,
        viewModel::elegirCocina,
        viewModel::ajustarFuegos,
        viewModel::activarHorno,
        viewModel::ajustarRacionesDefecto,
    )
}

/** «Tu cocina»: tipo y mando, fuegos y horno, y raciones por defecto. Los callbacks reciben el valor ya elegido. */
@Suppress("LongParameterList") // una pieza por ajuste de la pantalla
@Composable
internal fun AjustesCocina(
    ajustes: Ajustes,
    onAtras: () -> Unit,
    onCocina: (Cocina) -> Unit,
    onFuegos: (Int) -> Unit,
    onHorno: (Boolean) -> Unit,
    onRaciones: (Int) -> Unit,
) {
    PantallaAjustes(stringResource(R.string.ajustes_cocina), onAtras) {
        Text(
            stringResource(R.string.ajustes_cocina_nota),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SelectorCocina(ajustes.cocina, onCocina)
        SelectorEquipo(ajustes.fuegos, ajustes.horno, onFuegos, onHorno, Modifier.padding(top = 16.dp))
        SelectorRaciones(ajustes.racionesDefecto, onRaciones, Modifier.padding(top = 16.dp))
    }
}

/** Fuegos (1 a [FUEGOS_MAX]) y horno. [onFuegos] recibe el número ya elegido. */
@Composable
fun SelectorEquipo(
    fuegos: Int,
    horno: Boolean,
    onFuegos: (Int) -> Unit,
    onHorno: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloApartado(stringResource(R.string.equipo_titulo))
        Text(stringResource(R.string.equipo_fuegos), style = MaterialTheme.typography.bodyLarge)
        PasosMasMenos(
            valor = fuegos.toString(),
            descMenos = stringResource(R.string.equipo_fuegos_menos),
            descMas = stringResource(R.string.equipo_fuegos_mas),
            onMenos = { onFuegos((fuegos - 1).coerceIn(FUEGOS_MIN, FUEGOS_MAX)) },
            onMas = { onFuegos((fuegos + 1).coerceIn(FUEGOS_MIN, FUEGOS_MAX)) },
        )
        Row(
            Modifier
                .fillMaxWidth()
                .testTag("equipo-horno")
                .toggleable(horno, role = Role.Switch, onValueChange = onHorno)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.equipo_horno), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.equipo_horno_detalle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = horno, onCheckedChange = null)
        }
    }
}

/** Raciones con las que se abre una receta (1 a [RACIONES_MAX]). [onRaciones] recibe el número ya elegido. */
@Composable
fun SelectorRaciones(
    raciones: Int,
    onRaciones: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloApartado(stringResource(R.string.raciones_titulo))
        PasosMasMenos(
            valor = pluralStringResource(R.plurals.raciones_valor, raciones, raciones),
            descMenos = stringResource(R.string.ficha_raciones_menos),
            descMas = stringResource(R.string.ficha_raciones_mas),
            onMenos = { onRaciones((raciones - 1).coerceIn(RACIONES_MIN, RACIONES_MAX)) },
            onMas = { onRaciones((raciones + 1).coerceIn(RACIONES_MIN, RACIONES_MAX)) },
        )
        Text(
            stringResource(R.string.raciones_nota),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Resumen de la fila del menú: «Gas · sin números · 3 fuegos · horno» o «Vitro digital 1-9 · 3 fuegos». */
@Composable
internal fun resumenCocina(ajustes: Ajustes): String {
    val cocina = ajustes.cocina.valida()
    val tipo =
        stringResource(
            when (cocina.tipo) {
                TipoCocina.VITRO -> R.string.ajustes_resumen_tipo_vitro
                TipoCocina.INDUCCION -> R.string.ajustes_resumen_tipo_induccion
                TipoCocina.GAS -> R.string.ajustes_resumen_tipo_gas
            },
        )
    val max = cocina.max
    val mando =
        when {
            max == null -> stringResource(R.string.ajustes_resumen_cocina_sin_numeros, tipo)
            cocina.mando == Mando.DIGITAL -> stringResource(R.string.ajustes_resumen_cocina_digital, tipo, max)
            else -> stringResource(R.string.ajustes_resumen_cocina_ruleta, tipo, max)
        }
    return listOfNotNull(
        mando,
        pluralStringResource(R.plurals.tarjeta_fuegos, ajustes.fuegos, ajustes.fuegos),
        stringResource(R.string.lista_horno).takeIf { ajustes.horno },
    ).joinToString(SEPARADOR)
}
