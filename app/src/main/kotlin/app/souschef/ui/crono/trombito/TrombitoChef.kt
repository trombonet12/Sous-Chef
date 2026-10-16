package app.souschef.ui.crono.trombito

import android.content.ContentResolver
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.souschef.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

private const val VIEWPORT = 108f
private const val BOB_PERIOD_MS = 2600.0
private const val BOB_AMPLITUD = 2.2f
private const val BLINK_PERIOD_MS = 4000L
private const val BLINK_DURATION_MS = 160L
private const val OJO_ABIERTO_MIN = 0.08f

/**
 * Trombito con gorro de chef, dibujado con Canvas (mismo dibujo que el icono de la app, viewBox 108). Se mece
 * suavemente y parpadea; con las animaciones del sistema desactivadas (ANIMATOR_DURATION_SCALE = 0) queda quieto.
 */
@Composable
fun TrombitoChef(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val animar = remember { !animacionesDesactivadas(context.contentResolver) }
    var tiempoMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(animar) {
        if (animar) {
            val inicio = withFrameMillis { it }
            while (true) withFrameMillis { tiempoMs = it - inicio }
        }
    }
    val descripcion = stringResource(R.string.trombito_descripcion)
    Canvas(modifier.semantics { contentDescription = descripcion }) { dibujaTrombito(tiempoMs, animar) }
}

private fun animacionesDesactivadas(resolver: ContentResolver): Boolean =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

private fun trazo(data: String): Path = PathParser().parsePathString(data).toPath()

private object Partes {
    val varaU by lazy { trazo("M46,47H27A7,7 0 0 0 27,61H46") }
    val varaBarra by lazy { trazo("M42,44V64") }
    val campana by lazy { trazo("M30,48C46,48 54,36 66,26.5L66,81.5C54,72 46,60 30,60Z") }
    val cuerpo by lazy { trazo("M49,54a19,28 0 1,0 38,0a19,28 0 1,0 -38,0Z") }
    val cara by lazy { trazo("M52.2,54a15.8,24.6 0 1,0 31.6,0a15.8,24.6 0 1,0 -31.6,0Z") }
    val mejillaI by lazy { trazo("M52.5,63a3,3 0 1,0 6,0a3,3 0 1,0 -6,0Z") }
    val mejillaD by lazy { trazo("M77.5,63a3,3 0 1,0 6,0a3,3 0 1,0 -6,0Z") }
    val lenteI by lazy { trazo("M52.4,50a7.6,7.6 0 1,0 15.2,0a7.6,7.6 0 1,0 -15.2,0Z") }
    val lenteD by lazy { trazo("M68.4,50a7.6,7.6 0 1,0 15.2,0a7.6,7.6 0 1,0 -15.2,0Z") }
    val pupilaI by lazy { trazo("M57.4,51a3.4,3.4 0 1,0 6.8,0a3.4,3.4 0 1,0 -6.8,0Z") }
    val pupilaD by lazy { trazo("M73.4,51a3.4,3.4 0 1,0 6.8,0a3.4,3.4 0 1,0 -6.8,0Z") }
    val brilloI by lazy { trazo("M60.8,49.8a1.2,1.2 0 1,0 2.4,0a1.2,1.2 0 1,0 -2.4,0Z") }
    val brilloD by lazy { trazo("M76.8,49.8a1.2,1.2 0 1,0 2.4,0a1.2,1.2 0 1,0 -2.4,0Z") }
    val boca by lazy { trazo("M62,64Q68,70 74,64") }
    val gorroCopa by lazy {
        trazo("M52,30L52,20A7,7 0 0 1 59,13A9.5,9.5 0 0 1 77,13A7,7 0 0 1 84,20L84,30Z")
    }
    val gorroPliegues by lazy { trazo("M62,19V28M68,16.5V28M74,19V28") }
    val gorroBanda by lazy {
        trazo("M53,27H83A2,2 0 0 1 85,29V32A2,2 0 0 1 83,34H53A2,2 0 0 1 51,32V29A2,2 0 0 1 53,27Z")
    }
}

private val LatonOscuro = Color(0xFFB97A0C)
private val LatonClaro = Color(0xFFF5C033)
private val CampanaOro = Color(0xFFE8A92A)
private val BordeCuerpo = Color(0xFFC98A12)
private val CaraAmarilla = Color(0xFFF9D768)
private val Mejilla = Color(0xFFF6A58A)
private val GafasRojas = Color(0xFFD8232A)
private val Tinta = Color(0xFF2B1B0E)
private val BocaMarron = Color(0xFF6B3F08)
private val GorroBlanco = Color(0xFFFFFFFF)
private val GorroBorde = Color(0xFFCBBE9F)
private val GorroPliegue = Color(0xFFE3D8BF)

private fun trazoRedondo(ancho: Float) = Stroke(width = ancho, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** 0 = ojos abiertos, 1 = cerrados (parpadeo breve cada [BLINK_PERIOD_MS]). */
private fun parpadeo(tiempoMs: Long): Float {
    val fase = (tiempoMs + BLINK_PERIOD_MS / 2) % BLINK_PERIOD_MS
    if (fase >= BLINK_DURATION_MS) return 0f
    val mitad = BLINK_DURATION_MS / 2f
    return 1f - abs(fase - mitad) / mitad
}

private fun DrawScope.dibujaTrombito(
    tiempoMs: Long,
    animar: Boolean,
) {
    val unidad = min(size.width, size.height) / VIEWPORT
    val origenX = (size.width - VIEWPORT * unidad) / 2f
    val origenY = (size.height - VIEWPORT * unidad) / 2f
    val balanceo = if (animar) sin(2.0 * PI * tiempoMs / BOB_PERIOD_MS).toFloat() * BOB_AMPLITUD else 0f
    val ojoAbierto = if (animar) (1f - parpadeo(tiempoMs)).coerceAtLeast(OJO_ABIERTO_MIN) else 1f
    translate(origenX, origenY) {
        scale(unidad, unidad, pivot = Offset.Zero) {
            translate(0f, balanceo) {
                dibujaCuerpo(ojoAbierto)
                dibujaGorro()
            }
        }
    }
}

private fun DrawScope.dibujaCuerpo(ojoAbierto: Float) {
    drawPath(Partes.varaU, LatonOscuro, style = trazoRedondo(5.4f))
    drawPath(Partes.varaU, LatonClaro, style = trazoRedondo(2f))
    drawPath(Partes.varaBarra, LatonOscuro, style = trazoRedondo(3.4f))
    drawPath(Partes.campana, CampanaOro)
    drawPath(Partes.cuerpo, BordeCuerpo)
    drawPath(Partes.cara, CaraAmarilla)
    drawPath(Partes.mejillaI, Mejilla)
    drawPath(Partes.mejillaD, Mejilla)
    for (lente in listOf(Partes.lenteI, Partes.lenteD)) {
        drawPath(lente, Color.White)
        drawPath(lente, GafasRojas, style = Stroke(width = 2.8f))
    }
    scale(1f, ojoAbierto, pivot = Offset(0f, 51f)) {
        drawPath(Partes.pupilaI, Tinta)
        drawPath(Partes.pupilaD, Tinta)
        drawPath(Partes.brilloI, Color.White)
        drawPath(Partes.brilloD, Color.White)
    }
    drawPath(Partes.boca, BocaMarron, style = trazoRedondo(2.4f))
}

/** Gorro de chef blanco: copa abombada con pliegues y banda sobre la campana. */
private fun DrawScope.dibujaGorro() {
    drawPath(Partes.gorroCopa, GorroBlanco)
    drawPath(Partes.gorroCopa, GorroBorde, style = Stroke(width = 1.6f, join = StrokeJoin.Round))
    drawPath(Partes.gorroPliegues, GorroPliegue, style = trazoRedondo(1.3f))
    drawPath(Partes.gorroBanda, GorroBlanco)
    drawPath(Partes.gorroBanda, GorroBorde, style = Stroke(width = 1.6f))
}
