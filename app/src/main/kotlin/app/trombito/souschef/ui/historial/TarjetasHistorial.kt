package app.trombito.souschef.ui.historial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.Valoracion
import app.trombito.souschef.ui.comun.BloqueLateral
import app.trombito.souschef.ui.comun.CifraBloque
import app.trombito.souschef.ui.comun.DatoBloque
import app.trombito.souschef.ui.comun.Etiqueta
import app.trombito.souschef.ui.comun.MarcoTarjeta
import app.trombito.souschef.ui.comun.RotuloBloque
import app.trombito.souschef.ui.comun.SEPARADOR_META
import app.trombito.souschef.ui.comun.TituloClasico
import app.trombito.souschef.ui.comun.TituloCompacto
import app.trombito.souschef.ui.comun.Tono
import app.trombito.souschef.ui.comun.fechaCorta
import app.trombito.souschef.ui.recetas.FamiliaBase
import app.trombito.souschef.ui.recetas.colorBase
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.Espacio

private const val MAX_TECNICAS = 3
private val ALTO_BOTON = 48.dp
private val ICONO_REPETIR = 18.dp

/** Tarjeta del historial en la disposición elegida en Ajustes, teñida por la familia de la base. */
@Composable
fun TarjetaHistorial(
    fila: FilaHistorial,
    disposicion: DisposicionLista,
    confirmando: Boolean,
    onBorrar: () -> Unit,
    onRepetir: () -> Unit,
) {
    val color = colorBase(FamiliaBase.de(fila.entrada.base))
    when (disposicion) {
        DisposicionLista.TINTE -> {
            MarcoTarjeta(disposicion, color, onClick = null, bloque = { BloqueFecha(fila) }) {
                CuerpoHistorial(fila, confirmando, onBorrar, onRepetir, cabecera = null)
            }
        }

        DisposicionLista.FRANJA -> {
            MarcoTarjeta(disposicion, color, onClick = null) {
                CuerpoHistorial(fila, confirmando, onBorrar, onRepetir, cabecera = textoFechaYMinutos(fila))
            }
        }

        DisposicionLista.CLASICA -> {
            MarcoTarjeta(disposicion, color, onClick = null) {
                CuerpoClasico(fila, confirmando, onBorrar, onRepetir)
            }
        }
    }
}

/** «14 oct · 45 min» o solo la fecha si no hay minutos reales. */
@Composable
private fun textoFechaYMinutos(fila: FilaHistorial): String {
    val e = fila.entrada
    val minutos = e.minutosReales?.let { stringResource(R.string.historial_bloque_min, it) }
    return listOfNotNull(fechaCorta(e.fecha), minutos).joinToString(SEPARADOR_META)
}

@Composable
private fun BloqueFecha(fila: FilaHistorial) {
    val e = fila.entrada
    val diaMes = diaYMes(e.fecha)
    val minutosReales = e.minutosReales?.let { stringResource(R.string.historial_min_reales, it) }
    val descripcion = listOfNotNull(fechaCorta(e.fecha), minutosReales).joinToString(", ")
    BloqueLateral(descripcion = descripcion) {
        if (diaMes != null) {
            CifraBloque(diaMes.first)
            RotuloBloque(diaMes.second)
        } else {
            DatoBloque(e.fecha, textAlign = TextAlign.Center)
        }
        e.minutosReales?.let {
            DatoBloque(stringResource(R.string.historial_bloque_min, it), modifier = Modifier.padding(top = Espacio.xs))
        }
    }
}

/** Cuerpo común de Tinte y Franja; con [cabecera] (Franja) abre con la fecha y los minutos. */
@Composable
private fun CuerpoHistorial(
    fila: FilaHistorial,
    confirmando: Boolean,
    onBorrar: () -> Unit,
    onRepetir: () -> Unit,
    cabecera: String?,
) {
    val e = fila.entrada
    val meta = (listOfNotNull(e.base) + e.tecnica.take(MAX_TECNICAS)).joinToString(SEPARADOR_META)
    Column(
        Modifier.padding(start = Espacio.m, top = Espacio.m, end = Espacio.s, bottom = Espacio.xs),
        verticalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        if (cabecera != null) {
            Text(cabecera, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        TituloCompacto(e.titulo)
        if (fila.repite) Etiqueta(stringResource(R.string.historial_dos_seguidas_corta), tono = Tono.Teja)
        if (meta.isNotEmpty()) {
            Text(
                meta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IndicadorValoracion(e.valoracion)
        Nota(e.nota)
        AccionesFila(fila, confirmando, onBorrar, onRepetir)
    }
}

/** La tarjeta de siempre: fecha y etiqueta arriba, título grande y base, técnicas y minutos debajo. */
@Composable
private fun CuerpoClasico(
    fila: FilaHistorial,
    confirmando: Boolean,
    onBorrar: () -> Unit,
    onRepetir: () -> Unit,
) {
    val e = fila.entrada
    Column(
        Modifier.padding(start = Espacio.l, top = Espacio.m, end = Espacio.s, bottom = Espacio.xs),
        verticalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Text(
                fechaCorta(e.fecha),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (fila.repite && e.base != null) {
                Etiqueta(stringResource(R.string.historial_dos_seguidas, e.base), tono = Tono.Teja)
            }
        }
        TituloClasico(e.titulo)
        val minutos = e.minutosReales?.let { stringResource(R.string.historial_min_reales, it) }
        val meta =
            (listOfNotNull(e.base.takeUnless { fila.repite }) + e.tecnica.take(MAX_TECNICAS) + listOfNotNull(minutos))
                .joinToString(SEPARADOR_META)
        if (meta.isNotEmpty()) {
            Text(
                meta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IndicadorValoracion(e.valoracion)
        Nota(e.nota)
        AccionesFila(fila, confirmando, onBorrar, onRepetir)
    }
}

/** Etiqueta pequeña con la valoración de la cocinada; nada si no se valoró. El color no va solo: lleva texto. */
@Composable
private fun IndicadorValoracion(valoracion: Valoracion?) {
    if (valoracion == null) return
    val (texto, tono) =
        when (valoracion) {
            Valoracion.REPETIR -> R.string.historial_valoracion_repetir to Tono.Oliva
            Valoracion.AJUSTAR -> R.string.historial_valoracion_ajustar to Tono.Ambar
            Valoracion.NO_REPETIR -> R.string.historial_valoracion_no_repetir to Tono.Neutro
        }
    Etiqueta(stringResource(texto), tono = tono)
}

@Composable
private fun Nota(nota: String?) {
    nota?.takeIf { it.isNotBlank() }?.let {
        Text(
            stringResource(R.string.historial_nota, it),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
        )
    }
}

@Composable
private fun AccionesFila(
    fila: FilaHistorial,
    confirmando: Boolean,
    onBorrar: () -> Unit,
    onRepetir: () -> Unit,
) {
    val titulo = fila.entrada.titulo
    val descripcionBorrar = stringResource(R.string.historial_borrar_desc, titulo)
    val descripcionRepetir = stringResource(R.string.historial_repetir_desc, titulo)
    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalAlignment = Alignment.CenterVertically) {
        if (fila.recetaExiste) {
            TextButton(
                onClick = onRepetir,
                modifier = Modifier.heightIn(min = ALTO_BOTON).semantics { contentDescription = descripcionRepetir },
            ) {
                Text(stringResource(R.string.historial_repetir))
                Icon(Icono.Chevron.vector, contentDescription = null, modifier = Modifier.size(ICONO_REPETIR))
            }
        }
        TextButton(
            onClick = onBorrar,
            modifier = Modifier.heightIn(min = ALTO_BOTON).semantics { contentDescription = descripcionBorrar },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(if (confirmando) R.string.historial_seguro else R.string.historial_borrar))
        }
    }
}
