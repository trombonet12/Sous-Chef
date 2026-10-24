package app.trombito.souschef.ui.novedades

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.trombito.souschef.ui.comun.animacionesReducidas
import app.trombito.souschef.ui.recetas.FamiliaBase
import app.trombito.souschef.ui.recetas.colorBase
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Tope de partículas de la ráfaga. */
internal const val MAX_PARTICULAS = 60

/** Duración de la única ráfaga. */
internal const val DURACION_CONFETI_MS = 3_000

private const val PARTICULAS_ESTATICAS = 14
private const val SEMILLA = 16
private const val RETRASO_MAX = 0.35f
private const val GIRO_MAX = 2.5f
private const val DESVANECER = 0.85f
private const val GRADOS = 360f
private const val CAIDA_EXPONENTE = 1.3f
private const val ALTO_PIEZA = 0.5f
private const val ZONA_ALTA = 0.25f
private const val ALFA_QUIETA = 0.75f

/**
 * Una pieza de confeti. Posiciones y tamaños son fracciones o dp lógicos; el dibujo los escala al lienzo.
 * [giro] son vueltas totales en los 3 s (como mucho 2,5: menos de una vuelta por segundo, sin parpadeo).
 */
@Suppress("LongParameterList") // datos de una pieza
internal class Particula(
    val x: Float,
    val retraso: Float,
    val oscilacion: Float,
    val ciclos: Float,
    val fase: Float,
    val lado: Float,
    val giro: Float,
    val color: Color,
)

/** Genera [n] (como mucho [MAX_PARTICULAS]) partículas repartiendo los [colores]; determinista. */
internal fun generarParticulas(
    n: Int,
    colores: List<Color>,
): List<Particula> {
    if (colores.isEmpty()) return emptyList()
    val azar = Random(SEMILLA)
    return List(n.coerceIn(0, MAX_PARTICULAS)) { i ->
        Particula(
            x = azar.nextFloat(),
            retraso = azar.nextFloat() * RETRASO_MAX,
            oscilacion = 0.01f + azar.nextFloat() * 0.03f,
            ciclos = 0.5f + azar.nextFloat(),
            fase = azar.nextFloat() * 2f * PI.toFloat(),
            lado = 6f + azar.nextFloat() * 5f,
            giro = azar.nextFloat() * GIRO_MAX,
            color = colores[i % colores.size],
        )
    }
}

/**
 * Una ráfaga de confeti desde arriba (no se repite al girar), de [DURACION_CONFETI_MS] ms y hasta
 * [MAX_PARTICULAS] piezas, sin dependencias.
 * Decorativo: sin semántica, sin sonido ni vibración. Con [reducido] (animaciones reducidas o TalkBack) no se anima:
 * unas pocas piezas quietas en la parte alta. Va detrás del contenido y no recibe toques.
 */
@Composable
fun Confeti(
    modifier: Modifier = Modifier,
    reducido: Boolean = animacionesReducidas(),
) {
    val colores = FamiliaBase.entries.map { colorBase(it).acento }
    val cuantas = if (reducido) PARTICULAS_ESTATICAS else MAX_PARTICULAS
    val particulas = remember(colores, cuantas) { generarParticulas(cuantas, colores) }
    // Una sola ráfaga por diálogo: girar la pantalla no la repite.
    val lanzada = rememberSaveable { mutableStateOf(false) }
    val progreso = remember { Animatable(if (reducido || lanzada.value) 1f else 0f) }
    LaunchedEffect(reducido) {
        if (!reducido && !lanzada.value) {
            lanzada.value = true
            progreso.animateTo(1f, tween(DURACION_CONFETI_MS, easing = LinearEasing))
        }
    }
    Canvas(modifier.clearAndSetSemantics {}) {
        if (reducido) {
            dibujarQuietas(particulas)
        } else if (progreso.value < 1f) {
            dibujarRafaga(particulas, progreso.value)
        }
    }
}

private fun DrawScope.dibujarRafaga(
    particulas: List<Particula>,
    t: Float,
) {
    val densidad = density
    particulas.forEach { p ->
        val local = ((t - p.retraso) / (1f - p.retraso)).coerceIn(0f, 1f)
        if (local <= 0f) return@forEach
        val lado = p.lado * densidad
        val y = -lado + local.pow(CAIDA_EXPONENTE) * (size.height + 2f * lado)
        val x = p.x * size.width + sin(p.fase + local * p.ciclos * 2f * PI.toFloat()) * p.oscilacion * size.width
        val alfa = if (local > DESVANECER) (1f - local) / (1f - DESVANECER) else 1f
        pieza(p, Offset(x, y), lado, p.giro * local * GRADOS, alfa)
    }
}

private fun DrawScope.dibujarQuietas(particulas: List<Particula>) {
    val lado = density
    particulas.forEach { p ->
        // Solo el cuarto superior del lienzo.
        val y = p.retraso / RETRASO_MAX * size.height * ZONA_ALTA
        pieza(p, Offset(p.x * size.width, y), p.lado * lado, p.giro * GRADOS, ALFA_QUIETA)
    }
}

private fun DrawScope.pieza(
    p: Particula,
    centro: Offset,
    lado: Float,
    grados: Float,
    alfa: Float,
) {
    rotate(grados, centro) {
        drawRect(
            color = p.color.copy(alpha = alfa),
            topLeft = Offset(centro.x - lado / 2f, centro.y - lado * ALTO_PIEZA / 2f),
            size = Size(lado, lado * ALTO_PIEZA),
        )
    }
}
