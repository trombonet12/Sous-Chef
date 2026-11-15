@file:Suppress("TooManyFunctions") // modelo de la franja y sus piezas (AHORA, botón, LUEGO)

package app.trombito.souschef.ui.crono

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.crono.ClaseEstado
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.theme.CronoPalette

/** Qué toca ahora, según el estado de los carriles (la franja lo pinta; no hay lógica en el composable). */
internal sealed interface Ahora {
    /** Un carril pide un toque: [lleno] si toca ya o ha vencido; si no, solo puede empezar (enlace «Empezar antes»). */
    data class Toca(
        val v: VistaCarril,
        val lleno: Boolean,
    ) : Ahora

    /**
     * Nada pide un toque y hay carriles en marcha: se enseña el que acaba antes entre los que corren; solo si todos
     * están parados, uno en pausa ([enPausa]: su reloj no avanza y la franja lo dice).
     */
    data class EnMarcha(
        val v: VistaCarril,
        val enPausa: Boolean = false,
    ) : Ahora

    /** Nada que empezar ni en marcha. */
    data object Nada : Ahora
}

/** Cómo espera una fila de LUEGO a otra: «cuando acabe ①» (`dep`) o «a la vez que ②» (`junto`). */
internal enum class Relacion { CUANDO_ACABE, A_LA_VEZ }

/** Una fila de LUEGO: el carril que empezará, y de qué carril ([referencia], 1-based) depende su arranque. */
internal data class FilaLuego(
    val v: VistaCarril,
    val relacion: Relacion?,
    val referencia: Int?,
)

internal data class ModeloFranja(
    val ahora: Ahora,
    val luego: List<FilaLuego>,
)

/** Alto objetivo de la franja: 40-100 dp (una línea si no hay nada pendiente, hasta tres con LUEGO). */
internal const val MAX_LUEGO = 2

/**
 * AHORA: el carril vencido más antiguo; si no, el que toca ya (el de hora más temprana); si no hay nada en marcha, el
 * primero de la cola con sus dependencias cumplidas (aunque su hora de reloj no haya llegado: así no se esconde un
 * carril que ya puede arrancar); y si algo corre, el que acaba antes. LUEGO: los carriles que aún no han empezado, por
 * hora prevista, con «cuando acabe ①» o «a la vez que ②». Null si todos los carriles han terminado.
 */
internal fun modeloFranja(
    vistas: List<VistaCarril>,
    maxLuego: Int = MAX_LUEGO,
): ModeloFranja? {
    val vivas = vistas.filter { !it.terminado }
    if (vivas.isEmpty()) return null
    val porHora = compareBy<VistaCarril> { it.sugeridoMs ?: Long.MAX_VALUE }
    val empezables = vivas.filter { it.accion == AccionCarril.EMPEZAR }
    val corriendo = vivas.filter { it.momento == MomentoPaso.EN_MARCHA }
    val vencido = vivas.filter { it.vencido }.minByOrNull { it.finAtMs ?: Long.MAX_VALUE }
    val ya = empezables.filter { it.clase == ClaseEstado.YA }.minWithOrNull(porHora)
    val ahora =
        when {
            vencido != null -> {
                Ahora.Toca(vencido, lleno = true)
            }

            ya != null -> {
                Ahora.Toca(ya, lleno = true)
            }

            corriendo.isEmpty() && empezables.isNotEmpty() -> {
                Ahora.Toca(empezables.minWith(porHora), lleno = false)
            }

            corriendo.isNotEmpty() -> {
                val andando = corriendo.filter { it.accion != AccionCarril.SEGUIR }
                val elegido = andando.ifEmpty { corriendo }.minBy { it.quedanS }
                Ahora.EnMarcha(elegido, enPausa = elegido.accion == AccionCarril.SEGUIR)
            }

            else -> {
                Ahora.Nada
            }
        }
    val enAhora = (ahora as? Ahora.Toca)?.v?.id ?: (ahora as? Ahora.EnMarcha)?.v?.id
    val luego =
        vivas
            .filter { it.momento == MomentoPaso.POR_EMPEZAR && it.id != enAhora }
            .sortedWith(porHora)
            .take(maxLuego)
            .map(::filaLuego)
    return ModeloFranja(ahora, luego)
}

/**
 * Carril cuyo botón lleva la franja (el AHORA que pide un toque), o null si la franja no lleva ninguno. Si ese carril
 * está en fila, la fila no repite el botón: un solo control por carril.
 */
internal fun idBotonFranja(vistas: List<VistaCarril>): String? = (modeloFranja(vistas)?.ahora as? Ahora.Toca)?.v?.id

private fun filaLuego(v: VistaCarril): FilaLuego =
    when {
        v.accion == AccionCarril.BLOQUEADO && v.bloqueadoPorN != null -> {
            FilaLuego(v, Relacion.CUANDO_ACABE, v.bloqueadoPorN)
        }

        v.juntoN != null -> {
            FilaLuego(v, Relacion.A_LA_VEZ, v.juntoN)
        }

        else -> {
            FilaLuego(v, null, null)
        }
    }

private const val LETRA_GRANDE = 1.15f
private const val ANCHO_ROTULO_DP = 52
private const val ALTO_BOTON_DP = 44

/**
 * Franja fija encima de los carriles: «AHORA ② Calentar el aceite [EMPEZAR]» y, debajo, hasta dos filas de LUEGO por
 * orden de hora. Entre 40 y 100 dp; con letra grande solo cabe una fila de LUEGO. No se mueve nada: lo que no cabe en
 * una línea se corta con «…». El botón solo sale si el carril que toca no está en [enTarjeta] (si lo está, lo lleva
 * su tarjeta, y el botón no se repite).
 */
@Composable
internal fun FranjaAhora(
    vistas: List<VistaCarril>,
    acciones: AccionesCarril,
    modifier: Modifier = Modifier,
    enTarjeta: Set<String> = emptySet(),
) {
    val escala = LocalDensity.current.fontScale
    val modelo = modeloFranja(vistas, if (escala > LETRA_GRANDE) 1 else MAX_LUEGO) ?: return
    val forma = RoundedCornerShape(12.dp)
    Column(
        modifier
            .fillMaxWidth()
            .testTag("franja-ahora")
            .clip(forma)
            .background(CronoPalette.carta2)
            .border(1.dp, CronoPalette.linea, forma)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        FilaAhora(modelo.ahora, acciones, enTarjeta)
        modelo.luego.forEachIndexed { i, fila -> FilaDeLuego(fila, conRotulo = i == 0, corto = escala > LETRA_GRANDE) }
    }
}

/** Rótulo de la franja («AHORA», «LUEGO»); con [texto] null solo reserva su ancho para alinear las filas. */
@Composable
private fun Rotulo(texto: String?) {
    val ancho = (ANCHO_ROTULO_DP * maxOf(1f, LocalDensity.current.fontScale)).dp
    if (texto == null) {
        Spacer(Modifier.width(ancho))
        return
    }
    Text(
        texto,
        Modifier.width(ancho),
        color = CronoPalette.acento,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
    )
}

@Composable
private fun FilaAhora(
    ahora: Ahora,
    acciones: AccionesCarril,
    enTarjeta: Set<String>,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = ALTO_BOTON_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (ahora) {
            is Ahora.Toca -> {
                val v = ahora.v
                val descripcion =
                    stringResource(R.string.crono_franja_a11y_ahora, v.indiceCarril + 1, v.pantalla.titulo)
                CuerpoAhora(descripcion, v, v.pantalla.titulo, Modifier.weight(1f))
                // Una sola vez: si el carril ya está expandido, el botón es el de su tarjeta.
                if (v.id !in enTarjeta) BotonFranja(v, acciones)
            }

            is Ahora.EnMarcha -> {
                val v = ahora.v
                val reloj = formatoReloj(v.quedanS)
                val descripcion =
                    stringResource(
                        if (ahora.enPausa) R.string.crono_franja_a11y_pausa else R.string.crono_franja_a11y_marcha,
                        v.indiceCarril + 1,
                        v.pantalla.titulo,
                        reloj,
                    )
                CuerpoAhora(descripcion, v, v.pantalla.titulo, Modifier.weight(1f)) {
                    if (ahora.enPausa) {
                        Text(
                            stringResource(R.string.crono_franja_pausa),
                            color = CronoPalette.apagado,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                    Text(
                        reloj,
                        color = if (ahora.enPausa) CronoPalette.apagado else v.colorReloj(),
                        style = MaterialTheme.typography.titleMedium.merge(Tabular),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }

            Ahora.Nada -> {
                val descripcion = stringResource(R.string.crono_franja_a11y_nada)
                CuerpoAhora(descripcion, null, stringResource(R.string.crono_franja_nada), Modifier.weight(1f))
            }
        }
    }
}

/** «AHORA ② Calentar el aceite»: rótulo, insignia (si hay carril) y título en una línea; TalkBack lo lee de una vez. */
@Composable
private fun CuerpoAhora(
    descripcion: String,
    v: VistaCarril?,
    titulo: String,
    modifier: Modifier,
    extra: @Composable () -> Unit = {},
) {
    Row(
        modifier.semantics(mergeDescendants = true) { contentDescription = descripcion },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Rotulo(stringResource(R.string.crono_franja_ahora))
        if (v != null) InsigniaCarril(v, colorDeCarril(v.colorHex, v.indiceCarril), tamano = 22.dp)
        TituloFranja(titulo, Modifier.weight(1f))
        extra()
    }
}

@Composable
private fun TituloFranja(
    texto: String,
    modifier: Modifier = Modifier,
) {
    Text(
        texto,
        modifier,
        color = CronoPalette.texto,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * El botón de la franja, con el mismo estilo que el de la tarjeta ([estiloAccion]): lleno solo si toca ya o ha vencido
 * (rojo si venció), enlace «Empezar antes» si el carril espera su hora y de contorno en el resto.
 */
@Composable
private fun BotonFranja(
    v: VistaCarril,
    acciones: AccionesCarril,
) {
    val estilo = estiloAccion(v)
    if (estilo == EstiloAccion.ESPERA_TEXTO) return
    val enlace = estilo == EstiloAccion.ENLACE
    val descripcion =
        if (enlace) {
            stringResource(R.string.crono_a11y_empezar_antes, v.nombre, v.titulo.orEmpty())
        } else {
            descripcionAccion(v)
        }
    val modifier =
        Modifier
            .heightIn(min = ALTO_BOTON_DP.dp)
            .testTag("franja-accion")
            .semantics { contentDescription = descripcion }
    val relleno = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
    val etiqueta = if (enlace) stringResource(R.string.crono_empezar_antes) else etiquetaAccion(v)
    val contenido: @Composable () -> Unit = {
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (enlace) TextDecoration.Underline else null,
        )
    }
    when (estilo) {
        EstiloAccion.LLENO -> {
            Button(
                onClick = { acciones.principal(v) },
                modifier = modifier,
                contentPadding = relleno,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = if (v.vencido) CronoPalette.urgente else CronoPalette.principal,
                        contentColor = if (v.vencido) CronoPalette.onUrgente else CronoPalette.onPrincipal,
                    ),
            ) { contenido() }
        }

        EstiloAccion.CONTORNO -> {
            OutlinedButton(
                onClick = { acciones.principal(v) },
                modifier = modifier,
                contentPadding = relleno,
                border = BorderStroke(1.5.dp, CronoPalette.principal),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CronoPalette.texto),
            ) { contenido() }
        }

        else -> {
            TextButton(
                onClick = { acciones.principal(v) },
                modifier = modifier,
                contentPadding = relleno,
                colors = ButtonDefaults.textButtonColors(contentColor = CronoPalette.apagado),
            ) { contenido() }
        }
    }
}

/** Cómo se dice la relación de una fila de LUEGO: el texto que se ve y el que lee TalkBack; null si no hay. */
@Composable
private fun textosRelacion(
    fila: FilaLuego,
    corto: Boolean,
): Pair<String, String>? {
    val n = fila.referencia ?: return null
    return when (fila.relacion) {
        Relacion.CUANDO_ACABE -> {
            stringResource(
                if (corto) R.string.crono_franja_tras else R.string.crono_franja_cuando_acabe,
                numeroCarril(n),
            ) to
                stringResource(R.string.crono_franja_a11y_cuando_acabe, n)
        }

        Relacion.A_LA_VEZ -> {
            stringResource(
                if (corto) R.string.crono_franja_con else R.string.crono_a_la_vez_carril,
                numeroCarril(n),
            ) to
                stringResource(R.string.crono_a11y_a_la_vez_carril, n)
        }

        null -> {
            null
        }
    }
}

/** «LUEGO 11:25 ① Rasgar el jamón · a la vez que ②»; el rótulo solo en la primera fila. */
@Composable
private fun FilaDeLuego(
    fila: FilaLuego,
    conRotulo: Boolean,
    corto: Boolean,
) {
    val v = fila.v
    val hora = v.sugeridoMs?.let { formatoHora(it) }
    val relacion = textosRelacion(fila, corto)
    val base =
        if (hora != null) {
            stringResource(R.string.crono_franja_a11y_luego, hora, v.indiceCarril + 1, v.pantalla.titulo)
        } else {
            stringResource(R.string.crono_franja_a11y_luego_sin_hora, v.indiceCarril + 1, v.pantalla.titulo)
        }
    val descripcion = listOfNotNull(base, relacion?.second).joinToString(", ")
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 24.dp)
            .semantics(mergeDescendants = true) { contentDescription = descripcion },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Rotulo(if (conRotulo) stringResource(R.string.crono_franja_luego) else null)
        if (hora != null) {
            Text(
                hora,
                color = CronoPalette.apagado,
                style = MaterialTheme.typography.bodySmall.merge(Tabular),
                maxLines = 1,
            )
        }
        InsigniaCarril(v, colorDeCarril(v.colorHex, v.indiceCarril), tamano = 18.dp)
        Text(
            v.pantalla.titulo,
            Modifier.weight(1f, fill = false),
            color = CronoPalette.texto,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (relacion != null) {
            Text(
                relacion.first,
                Modifier.padding(start = 2.dp),
                color = CronoPalette.acento,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
