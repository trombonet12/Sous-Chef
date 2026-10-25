@file:Suppress("TooManyFunctions") // tres tarjetas y sus piezas comunes

package app.trombito.souschef.ui.recetas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import app.trombito.souschef.R
import app.trombito.souschef.domain.CualHora
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Tags
import app.trombito.souschef.ui.comun.BloqueLateral
import app.trombito.souschef.ui.comun.CifraBloque
import app.trombito.souschef.ui.comun.DatoBloque
import app.trombito.souschef.ui.comun.Etiqueta
import app.trombito.souschef.ui.comun.LINEAS_RESUMEN_CLASICA
import app.trombito.souschef.ui.comun.MarcoTarjeta
import app.trombito.souschef.ui.comun.ResumenTarjeta
import app.trombito.souschef.ui.comun.RotuloBloque
import app.trombito.souschef.ui.comun.SEPARADOR_META
import app.trombito.souschef.ui.comun.TituloClasico
import app.trombito.souschef.ui.comun.TituloCompacto
import app.trombito.souschef.ui.comun.Tono
import app.trombito.souschef.ui.ficha.Servir
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.Espacio

private const val SIN_PROTEINA = "ninguna"
private const val LINEAS_PIE = 2

/** Tarjeta de la lista de recetas en la disposición elegida en Ajustes. */
@Composable
fun TarjetaLista(
    tarjeta: TarjetaReceta,
    disposicion: DisposicionLista,
    onFavorita: () -> Unit,
    onAbrir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (disposicion) {
        DisposicionLista.TINTE -> TarjetaTinte(tarjeta, onFavorita, onAbrir, modifier)
        DisposicionLista.FRANJA -> TarjetaFranja(tarjeta, onFavorita, onAbrir, modifier)
        DisposicionLista.CLASICA -> TarjetaClasica(tarjeta, onFavorita, onAbrir, modifier)
    }
}

// ---- TINTE: fondo del color de la base, tiempo y hora de empezar a la izquierda ----

@Composable
private fun TarjetaTinte(
    tarjeta: TarjetaReceta,
    onFavorita: () -> Unit,
    onAbrir: () -> Unit,
    modifier: Modifier,
) {
    MarcoTarjeta(
        disposicion = DisposicionLista.TINTE,
        color = colorBase(tarjeta.familia),
        onClick = onAbrir,
        modifier = modifier,
        bloque = { BloqueTiempo(tarjeta) },
    ) {
        CuerpoTinte(tarjeta, onFavorita)
    }
}

@Composable
private fun BloqueTiempo(
    tarjeta: TarjetaReceta,
    modifier: Modifier = Modifier,
) {
    val minutos = Escalado.minutos(tarjeta.receta.totalEfectivo)
    val hora = Servir.formato(tarjeta.hora)
    val descripcion = stringResource(R.string.tarjeta_minutos, minutos) + ", " + textoHora(tarjeta)
    BloqueLateral(modifier, descripcion) {
        CifraBloque(minutos)
        RotuloBloque(stringResource(R.string.lista_min))
        val palabra =
            if (tarjeta.modoHora ==
                HoraTarjeta.ACABAR
            ) {
                R.string.lista_acabas_corto
            } else {
                R.string.lista_empieza_corto
            }
        RotuloBloque(stringResource(palabra), modifier = Modifier.padding(top = Espacio.xs))
        DatoBloque(hora)
        if (tarjeta.conRitmo) RotuloBloque(stringResource(R.string.lista_con_tu_ritmo), textAlign = TextAlign.Center)
    }
}

@Composable
private fun CuerpoTinte(
    tarjeta: TarjetaReceta,
    onFavorita: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseProteina = baseYProteina(tarjeta.receta.tags)
    Column(modifier.padding(start = Espacio.m, bottom = Espacio.s)) {
        TituloConEstrella(tarjeta, onFavorita)
        Column(Modifier.padding(end = Espacio.m)) {
            Descripcion(tarjeta.receta.descripcion)
            if (baseProteina.isNotEmpty()) {
                Text(
                    baseProteina,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                lineaDetalle(tarjeta, conBase = false),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = LINEAS_PIE,
                overflow = TextOverflow.Ellipsis,
            )
            EtiquetasEquipo(tarjeta)
            ParaCenar(tarjeta)
            EtiquetaLlegada(tarjeta)
        }
    }
}

// ---- FRANJA: tarjeta clara con una franja de color a la izquierda ----

@Composable
private fun TarjetaFranja(
    tarjeta: TarjetaReceta,
    onFavorita: () -> Unit,
    onAbrir: () -> Unit,
    modifier: Modifier,
) {
    MarcoTarjeta(DisposicionLista.FRANJA, colorBase(tarjeta.familia), onAbrir, modifier) {
        CuerpoFranja(tarjeta, onFavorita)
    }
}

@Composable
private fun CuerpoFranja(
    tarjeta: TarjetaReceta,
    onFavorita: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tiempo =
        stringResource(R.string.tarjeta_minutos, Escalado.minutos(tarjeta.receta.totalEfectivo)) +
            SEPARADOR_META + textoHora(tarjeta)
    Column(modifier.padding(start = Espacio.m, bottom = Espacio.m)) {
        TituloConEstrella(tarjeta, onFavorita)
        Column(Modifier.padding(end = Espacio.m)) {
            Descripcion(tarjeta.receta.descripcion)
            Text(tiempo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                lineaDetalle(tarjeta, conBase = true),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = LINEAS_PIE,
                overflow = TextOverflow.Ellipsis,
            )
            EtiquetasEquipo(tarjeta)
            ParaCenar(tarjeta)
            EtiquetaLlegada(tarjeta)
        }
    }
}

// ---- CLÁSICA: la tarjeta de siempre, con la huella de carriles ----

@Composable
private fun TarjetaClasica(
    tarjeta: TarjetaReceta,
    onFavorita: () -> Unit,
    onAbrir: () -> Unit,
    modifier: Modifier,
) {
    val receta = tarjeta.receta
    MarcoTarjeta(DisposicionLista.CLASICA, colorBase(tarjeta.familia), onAbrir, modifier) {
        Column(Modifier.padding(start = Espacio.l, top = Espacio.s, end = Espacio.xs, bottom = Espacio.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TituloClasico(receta.titulo, modifier = Modifier.weight(1f))
                BotonFavorita(tarjeta.favorita, receta.titulo, onFavorita)
            }
            Column(Modifier.padding(end = Espacio.m), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                ResumenTarjeta(receta.descripcion, maxLines = LINEAS_RESUMEN_CLASICA)
                Huella(receta)
                PieTarjeta(tarjeta)
                EtiquetasEquipo(tarjeta)
                ParaCenar(tarjeta)
                EtiquetaLlegada(tarjeta)
            }
        }
    }
}

/** Una sola línea de metadatos (tiempo, fuegos, última vez) de la tarjeta clásica. */
@Composable
private fun PieTarjeta(tarjeta: TarjetaReceta) {
    val inicio =
        listOfNotNull(
            stringResource(R.string.tarjeta_minutos, Escalado.minutos(tarjeta.receta.totalEfectivo)),
            textoHora(tarjeta),
            textoFuegos(tarjeta),
        ).joinToString(SEPARADOR_META)
    val hace = tarjeta.hace.texto()
    val resalte = colorHace(tarjeta)
    val texto =
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(inicio) }
            append(SEPARADOR_META)
            withStyle(SpanStyle(color = resalte)) { append(hace) }
        }
    Text(
        texto,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = LINEAS_PIE,
        overflow = TextOverflow.Ellipsis,
    )
}

// ---- Piezas comunes ----

/**
 * «Nueva» (sin cocinar y de hace menos de 30 días), «Pide 4 fuegos» y «Pide horno» (la receta pide más equipo del que
 * hay en la cocina del usuario). Al ir dentro de la tarjeta, TalkBack lee «Nueva» con el resto de su texto.
 */
@Composable
private fun EtiquetasEquipo(tarjeta: TarjetaReceta) {
    if (!tarjeta.nueva && tarjeta.pideFuegos == 0 && !tarjeta.pideHorno) return
    Row(Modifier.padding(top = Espacio.xs), horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        if (tarjeta.nueva) {
            Etiqueta(
                stringResource(R.string.nueva_etiqueta),
                modifier = Modifier.testTag("etiqueta-nueva"),
                tono = Tono.Ambar,
                negrita = true,
            )
        }
        if (tarjeta.pideFuegos > 0) {
            Etiqueta(stringResource(R.string.lista_pide_fuegos, tarjeta.pideFuegos), tono = Tono.Teja)
        }
        if (tarjeta.pideHorno) Etiqueta(stringResource(R.string.lista_pide_horno), tono = Tono.Teja)
    }
}

/** «para cenar a las 21:00»: si la hora de la tarjeta (modo «empieza») cuenta la cena y no hay «¿Qué me da tiempo?». */
@Composable
private fun ParaCenar(tarjeta: TarjetaReceta) {
    val cenando = tarjeta.usada.cual == CualHora.CENAR
    if (!cenando || tarjeta.modoHora != HoraTarjeta.EMPEZAR || tarjeta.llegada != null) return
    Text(
        stringResource(R.string.para_cenar, Servir.formato(tarjeta.usada.minutos)),
        modifier = Modifier.padding(top = Espacio.xs).testTag("para-cenar"),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** «Llegas · empieza 14:43» o «No llegas · acabarías 16:02», solo con «¿Qué me da tiempo?» puesto. */
@Composable
private fun EtiquetaLlegada(tarjeta: TarjetaReceta) {
    val llegada = tarjeta.llegada ?: return
    val texto =
        if (llegada.llega) {
            stringResource(R.string.que_tiempo_llegas, Servir.formato(llegada.empiezaMin))
        } else {
            stringResource(R.string.que_tiempo_no_llegas, Servir.formato(llegada.acabariaMin ?: llegada.empiezaMin))
        }
    Etiqueta(
        texto,
        modifier = Modifier.padding(top = Espacio.xs).testTag("etiqueta-llegada"),
        tono = if (llegada.llega) Tono.Oliva else Tono.Teja,
    )
}

/** «empieza 14:43» o «acabas 16:02», según el modo elegido en Ajustes; con ritmo, «empieza 14:35 con tu ritmo». */
@Composable
private fun textoHora(tarjeta: TarjetaReceta): String {
    val hora =
        stringResource(
            if (tarjeta.modoHora == HoraTarjeta.ACABAR) R.string.lista_acabas else R.string.lista_empieza,
            Servir.formato(tarjeta.hora),
        )
    return if (tarjeta.conRitmo) hora + " " + stringResource(R.string.lista_con_tu_ritmo) else hora
}

@Composable
private fun TituloConEstrella(
    tarjeta: TarjetaReceta,
    onFavorita: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TituloCompacto(tarjeta.receta.titulo, modifier = Modifier.weight(1f))
        BotonFavorita(tarjeta.favorita, tarjeta.receta.titulo, onFavorita)
    }
}

/** Descripción de la receta en una sola línea y en gris. */
@Composable
private fun Descripcion(texto: String) = ResumenTarjeta(texto)

@Composable
private fun BotonFavorita(
    favorita: Boolean,
    titulo: String,
    onClick: () -> Unit,
) {
    val descripcion =
        stringResource(if (favorita) R.string.favorita_quitar else R.string.favorita_marcar, titulo)
    IconButton(onClick = onClick) {
        Icon(
            if (favorita) Icono.EstrellaLlena.vector else Icono.Estrella.vector,
            contentDescription = descripcion,
            tint = if (favorita) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun textoFuegos(tarjeta: TarjetaReceta): String? =
    if (tarjeta.fuegos > 0) pluralStringResource(R.plurals.tarjeta_fuegos, tarjeta.fuegos, tarjeta.fuegos) else null

@Composable
private fun colorHace(tarjeta: TarjetaReceta): Color =
    if (tarjeta.hace.resaltado) MaterialTheme.colorScheme.error else Color.Unspecified

/** «[Base · proteína · ]N fuegos · horno · hace X», con «hace X» en rojo si lleva tiempo sin cocinarse. */
@Composable
private fun lineaDetalle(
    tarjeta: TarjetaReceta,
    conBase: Boolean,
): AnnotatedString {
    val partes =
        listOfNotNull(
            baseYProteina(tarjeta.receta.tags).takeIf { conBase && it.isNotEmpty() },
            textoFuegos(tarjeta),
            stringResource(R.string.lista_horno).takeIf { tarjeta.conHorno },
        )
    val hace = tarjeta.hace.texto()
    val resalte = colorHace(tarjeta)
    return buildAnnotatedString {
        partes.forEach {
            append(it)
            append(SEPARADOR_META)
        }
        withStyle(SpanStyle(color = resalte)) { append(hace) }
    }
}

/** «Pasta · pollo»: base y proteína con la primera letra en mayúscula; sin la proteína si es «ninguna». */
internal fun baseYProteina(tags: Tags): String {
    val base = tags.base.orEmpty().trim()
    val proteina =
        tags.proteina
            .orEmpty()
            .trim()
            .takeUnless { it.equals(SIN_PROTEINA, ignoreCase = true) }
            .orEmpty()
    return listOf(base, proteina)
        .filter { it.isNotEmpty() }
        .joinToString(SEPARADOR_META)
        .replaceFirstChar { it.uppercase() }
}
