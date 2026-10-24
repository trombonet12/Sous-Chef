package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.ui.crono.clasica.CronoClasico

/**
 * Los carriles en la [disposicion] elegida: lo comparten la cocina en marcha y la cocina de muestra de la intro. El
 * [relleno] se aplica a todas las disposiciones salvo que se dé [rellenoClasica] (la cocina deja la clásica sin
 * margen porque su panel ya lo trae).
 */
@Suppress("LongParameterList")
@Composable
internal fun CuerpoCrono(
    datos: DatosCrono,
    disposicion: DisposicionCrono,
    acciones: AccionesCarril,
    tecnicas: Map<String, String>,
    onTecnica: (String) -> Unit,
    relleno: PaddingValues,
    modifier: Modifier = Modifier,
    rellenoClasica: PaddingValues = relleno,
) {
    when (disposicion) {
        DisposicionCrono.FOCO -> {
            val vistas = vistasCarriles(datos)
            CronoFoco(vistas, elegirFoco(vistas, datos.estado.foco), acciones, modifier.padding(relleno))
        }

        DisposicionCrono.APILADA -> {
            CronoApilado(vistasCarriles(datos), acciones, modifier.padding(relleno))
        }

        DisposicionCrono.CLASICA -> {
            CronoClasico(datos, tecnicas, acciones, onTecnica, modifier.padding(rellenoClasica))
        }
    }
}
