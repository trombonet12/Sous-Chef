package app.souschef.ui.crono

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.souschef.R
import app.souschef.receta.Carril
import app.souschef.receta.Paso
import app.souschef.receta.Receta
import app.souschef.receta.crono.ClaseEstado
import app.souschef.receta.crono.ConsultasCrono
import app.souschef.receta.crono.EstadoCarril
import app.souschef.receta.crono.EstadoCrono
import app.souschef.receta.crono.PlanCrono
import app.souschef.receta.crono.VistaEstado

/** Lo que las piezas de la cocina necesitan para pintarse: receta, estado y reloj en un solo paquete. */
class DatosCrono(
    val receta: Receta,
    val estado: EstadoCrono,
    val ahoraMs: Long,
) {
    val carriles: List<Carril> = ConsultasCrono.carriles(receta)
    val plan: PlanCrono by lazy { ConsultasCrono.plan(estado, receta, ahoraMs) }

    fun carril(id: String): Carril? = carriles.firstOrNull { it.id == id }

    fun nombre(id: String): String = carril(id)?.nombre ?: id

    fun ec(id: String): EstadoCarril? = estado.carriles[id]

    fun paso(id: String): Paso? = pasoActual(receta, estado, id)

    fun quedan(id: String): Int = ConsultasCrono.quedan(estado, receta, id, ahoraMs)

    fun vista(id: String): VistaEstado = ConsultasCrono.vista(estado, receta, id, ahoraMs)

    fun cola(id: String): List<Paso> = colaDe(receta, id)
}

/** Texto de estado de un carril ("empieza ya", "a las 14:05", "espera a ..."). */
@Composable
fun textoEstado(
    datos: DatosCrono,
    v: VistaEstado,
    mostrarTocaba: Boolean = false,
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
            val base = stringResource(R.string.crono_est_ya)
            val sugerido = v.sugeridoMs
            if (mostrarTocaba && sugerido != null) {
                base + " · " + stringResource(R.string.crono_est_tocaba, formatoHora(sugerido))
            } else {
                base
            }
        }
    }
