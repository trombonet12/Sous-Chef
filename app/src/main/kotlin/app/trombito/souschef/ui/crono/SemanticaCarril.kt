package app.trombito.souschef.ui.crono

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import app.trombito.souschef.R

/** Lo que lee TalkBack en la acción principal: qué hace y de qué carril (y paso) es. */
@Composable
internal fun descripcionAccion(v: VistaCarril): String {
    val paso = v.titulo.orEmpty()
    return when (v.accion) {
        AccionCarril.EMPEZAR -> {
            stringResource(R.string.crono_a11y_empezar, v.nombre, paso)
        }

        AccionCarril.SEGUIR -> {
            stringResource(R.string.crono_a11y_seguir, v.nombre, paso)
        }

        AccionCarril.PAUSAR, AccionCarril.HECHO -> {
            stringResource(R.string.crono_a11y_hecho, v.nombre, paso)
        }

        AccionCarril.BLOQUEADO, AccionCarril.TERMINADO -> {
            stringResource(R.string.crono_a11y_accion_otro, v.nombre, etiquetaAccion(v))
        }
    }
}

/** Un paso vencido no se distingue solo por el color: lo dice «Vencido» y, al pasar a vencido, TalkBack lo anuncia. */
@Composable
internal fun Modifier.avisoVencido(v: VistaCarril): Modifier {
    if (!v.vencido) return this
    val vencido = stringResource(R.string.crono_a11y_vencido)
    return semantics {
        stateDescription = vencido
        liveRegion = LiveRegionMode.Assertive
    }
}
