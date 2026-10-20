package app.trombito.souschef.ui.crono.clasica

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.trombito.souschef.R
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.NivelPotencia
import app.trombito.souschef.receta.Potencia
import app.trombito.souschef.receta.crono.ClaseEstado
import app.trombito.souschef.receta.crono.VistaEstado
import app.trombito.souschef.ui.comun.alNivel
import app.trombito.souschef.ui.crono.DatosCrono
import app.trombito.souschef.ui.crono.formatoHora
import kotlin.math.ceil

/** Nadie ha arrancado nada todavía (cocina abierta, sin pasos en marcha ni hechos). */
internal val DatosCrono.sinEmpezar: Boolean
    get() = estado.carriles.values.all { !it.corriendo && !it.pausado && !it.fin && it.i == 0 }

/** Texto de estado de un carril ("empieza ya", "a las 14:05", "espera a ..."). */
@Composable
internal fun textoEstado(
    datos: DatosCrono,
    v: VistaEstado,
): String =
    when (v.clase) {
        ClaseEstado.FIN -> {
            stringResource(R.string.crono_est_fin)
        }

        ClaseEstado.ATENDER -> {
            stringResource(R.string.crono_est_atender)
        }

        ClaseEstado.PAUSA -> {
            stringResource(R.string.crono_est_pausa)
        }

        ClaseEstado.MARCHA -> {
            stringResource(R.string.crono_est_marcha)
        }

        ClaseEstado.BLOQ -> {
            val de = v.bloqueadoPor.firstOrNull()?.let { "${datos.texto(it.first)} (${datos.nombre(it.second)})" }
            stringResource(R.string.crono_est_bloq, de.orEmpty())
        }

        ClaseEstado.ESPERA -> {
            stringResource(R.string.crono_est_espera, formatoHora(v.sugeridoMs ?: datos.ahoraMs))
        }

        ClaseEstado.YA -> {
            stringResource(R.string.crono_est_ya)
        }
    }

/**
 * Barra de potencia del panel: [segmentos] casillas, [encendidos] pintadas y el [texto] grande (número o palabra).
 * Con números hay tantos segmentos como el máximo del mando (más alto del mando; en analógico los medios redondean
 * hacia arriba); sin números, uno por palabra (7).
 */
internal data class BarraPotencia(
    val segmentos: Int,
    val encendidos: Int,
    val texto: String,
    val esPalabra: Boolean,
)

private const val F_MAXIMA = 9

/** La barra de la potencia canónica [f] (0-9) en la [cocina] del usuario. */
internal fun barraPotencia(
    f: Int,
    cocina: Cocina,
): BarraPotencia {
    val g = f.coerceIn(0, F_MAXIMA)
    val max = cocina.valida().max
    val palabras = if (max == null) (1..F_MAXIMA).map { Potencia.texto(it, cocina) }.distinct() else emptyList()
    val segmentos = max ?: palabras.size
    return when (val nivel = Potencia.nivel(g, cocina)) {
        is NivelPotencia.Numero -> {
            val encendidos = ceil(nivel.valor).toInt().coerceIn(0, segmentos)
            BarraPotencia(segmentos, encendidos, Potencia.texto(g, cocina), false)
        }

        is NivelPotencia.Palabra -> {
            BarraPotencia(segmentos, palabras.indexOf(nivel.texto) + 1, nivel.texto, true)
        }

        NivelPotencia.Apagado -> {
            BarraPotencia(segmentos, 0, if (max == null) Potencia.texto(0, cocina) else "0", max == null)
        }
    }
}

/**
 * «al 7» o «a fuego medio-alto» para ir tras otro texto ("Después aquí: Reposo · 10 min al 7"): `alNivel` abre
 * con mayúscula si la frase empieza ahí, y aquí siempre va a mitad de línea.
 */
internal fun Cocina.despuesNivel(f: Int): String = alNivel(f).replaceFirstChar { it.lowercaseChar() }
