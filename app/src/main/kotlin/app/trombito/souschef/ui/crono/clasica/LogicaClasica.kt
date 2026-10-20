package app.trombito.souschef.ui.crono.clasica

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.trombito.souschef.R
import app.trombito.souschef.receta.crono.ClaseEstado
import app.trombito.souschef.receta.crono.VistaEstado
import app.trombito.souschef.ui.crono.DatosCrono
import app.trombito.souschef.ui.crono.formatoHora

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
            val de = v.bloqueadoPor.firstOrNull()
            stringResource(R.string.crono_est_bloq, de?.let { "${it.first} (${datos.nombre(it.second)})" }.orEmpty())
        }

        ClaseEstado.ESPERA -> {
            stringResource(R.string.crono_est_espera, formatoHora(v.sugeridoMs ?: datos.ahoraMs))
        }

        ClaseEstado.YA -> {
            stringResource(R.string.crono_est_ya)
        }
    }
