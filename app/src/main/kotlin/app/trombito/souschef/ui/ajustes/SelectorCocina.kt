package app.trombito.souschef.ui.ajustes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.Potencia
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.ui.comun.PasosMasMenos
import app.trombito.souschef.ui.comun.nivel
import app.trombito.souschef.ui.theme.CronoPalette

/** Potencia canónica del chip del ejemplo en vivo: la del segundo número marcado en el texto de ejemplo. */
private const val POTENCIA_CHIP = 6

/** Número más alto con el que arranca un mando de ruleta al apagar «sin números». */
private const val ANALOGICO_DEFECTO = 9

private class OpcionTipo(
    val tipo: TipoCocina,
    val titulo: Int,
    val detalle: Int,
)

private val OPCIONES_TIPO =
    listOf(
        OpcionTipo(TipoCocina.VITRO, R.string.cocina_tipo_vitro, R.string.cocina_tipo_vitro_detalle),
        OpcionTipo(TipoCocina.INDUCCION, R.string.cocina_tipo_induccion, R.string.cocina_tipo_induccion_detalle),
        OpcionTipo(TipoCocina.GAS, R.string.cocina_tipo_gas, R.string.cocina_tipo_gas_detalle),
    )

/** Encabezado de un apartado de Ajustes. */
@Composable
internal fun TituloApartado(
    texto: String,
    modifier: Modifier = Modifier,
) {
    Text(
        texto,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

/**
 * Cocina del usuario (ADR-0014): tipo, mando, potencia máxima y un ejemplo en vivo de cómo se leerán las recetas.
 * Sin estado: [cocina] llega hecha y [onCocina] recibe la cocina ya válida.
 */
@Composable
fun SelectorCocina(
    cocina: Cocina,
    onCocina: (Cocina) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actual = cocina.valida()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloApartado(stringResource(R.string.cocina_tipo_titulo))
        SelectorTipo(actual.tipo) { onCocina(alCambiarTipo(actual, it)) }
        TituloApartado(stringResource(R.string.cocina_mando_titulo), Modifier.padding(top = 8.dp))
        SelectorMando(actual.mando) { onCocina(actual.copy(mando = it).valida()) }
        PotenciaMaxima(actual, onCocina)
        EjemploEnVivo(actual)
    }
}

@Composable
private fun SelectorTipo(
    elegido: TipoCocina,
    onTipo: (TipoCocina) -> Unit,
) {
    val tarjetas =
        OPCIONES_TIPO.map {
            OpcionTarjeta(it.titulo, it.detalle, "cocina-tipo-${it.tipo.name}", it.tipo == elegido)
        }
    SelectorTarjetas(tarjetas, { onTipo(OPCIONES_TIPO[it].tipo) }) { i, mod ->
        MiniaturaTipo(OPCIONES_TIPO[i].tipo, mod)
    }
}

@Composable
private fun SelectorMando(
    elegido: Mando,
    onMando: (Mando) -> Unit,
) {
    val opciones =
        listOf(
            Mando.DIGITAL to R.string.cocina_mando_digital,
            Mando.ANALOGICO to R.string.cocina_mando_analogico,
        )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { i, (mando, texto) ->
            SegmentedButton(
                selected = mando == elegido,
                onClick = { onMando(mando) },
                shape = SegmentedButtonDefaults.itemShape(i, opciones.size),
                modifier = Modifier.testTag("cocina-mando-${mando.name}"),
            ) { Text(stringResource(texto)) }
        }
    }
}

/** El número más alto del mando (con el interruptor «sin números» si es de ruleta). */
@Composable
private fun PotenciaMaxima(
    cocina: Cocina,
    onCocina: (Cocina) -> Unit,
) {
    val max = cocina.max
    if (max != null) {
        val rango =
            if (cocina.mando == Mando.DIGITAL) {
                Cocina.DIGITAL_MIN..Cocina.DIGITAL_MAX
            } else {
                Cocina.ANALOGICO_MIN..Cocina.ANALOGICO_MAX
            }
        TituloApartado(stringResource(R.string.cocina_max_titulo), Modifier.padding(top = 8.dp))
        PasosMasMenos(
            valor = max.toString(),
            descMenos = stringResource(R.string.cocina_max_menos),
            descMas = stringResource(R.string.cocina_max_mas),
            onMenos = { onCocina(cocina.copy(max = (max - 1).coerceIn(rango))) },
            onMas = { onCocina(cocina.copy(max = (max + 1).coerceIn(rango))) },
        )
        Text(
            stringResource(R.string.cocina_max_nota),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (cocina.mando == Mando.ANALOGICO) {
        InterruptorSinNumeros(sinNumeros = max == null) {
            onCocina(cocina.copy(max = if (it) null else ANALOGICO_DEFECTO))
        }
    }
}

@Composable
private fun InterruptorSinNumeros(
    sinNumeros: Boolean,
    onCambio: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .testTag("cocina-sin-numeros")
            .toggleable(sinNumeros, role = Role.Switch, onValueChange = onCambio)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.cocina_sin_numeros), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.cocina_sin_numeros_detalle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = sinNumeros, onCheckedChange = null)
    }
}

/** El texto real de una receta, traducido a la cocina elegida, y el chip de fuego como lo pinta el cronómetro. */
@Composable
private fun EjemploEnVivo(cocina: Cocina) {
    val texto = Potencia.reescribir(stringResource(R.string.cocina_ejemplo_texto), cocina)
    val potencia = cocina.nivel(POTENCIA_CHIP)
    val descripcion = stringResource(R.string.cocina_ejemplo_chip_desc, potencia)
    Surface(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.cocina_ejemplo_titulo),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(texto, Modifier.testTag("cocina-ejemplo"), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.crono_fuego_chip, potencia),
                Modifier
                    .testTag("cocina-ejemplo-chip")
                    .clip(RoundedCornerShape(50))
                    .background(CronoPalette.carta2)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .semantics { contentDescription = descripcion },
                color = CronoPalette.acento,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

/** Al pasar a gas se propone el mando más común (ruleta sin números); el resto de cambios conserva el mando. */
internal fun alCambiarTipo(
    actual: Cocina,
    tipo: TipoCocina,
): Cocina =
    if (tipo == TipoCocina.GAS && actual.tipo != TipoCocina.GAS) {
        actual.copy(tipo = tipo, mando = Mando.ANALOGICO, max = null).valida()
    } else {
        actual.copy(tipo = tipo)
    }
