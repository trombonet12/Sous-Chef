package app.trombito.souschef.ui.bienvenida

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.ajustes.SelectorCocina
import app.trombito.souschef.ui.ajustes.SelectorEquipo
import app.trombito.souschef.ui.ajustes.SelectorRaciones
import app.trombito.souschef.ui.comun.LocalCocina
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.crono.AccionesCarril
import app.trombito.souschef.ui.crono.CronoApilado
import app.trombito.souschef.ui.crono.CronoFoco
import app.trombito.souschef.ui.crono.DatosCrono
import app.trombito.souschef.ui.crono.clasica.CronoClasico
import app.trombito.souschef.ui.crono.elegirFoco
import app.trombito.souschef.ui.crono.vistasCarriles
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.FichaPalette

private val ALTO_MARCO_COCINA = 500.dp
private val ANCHO_CANTIDAD = 84.dp

/** 3. Qué hace el cronómetro, con la pantalla real de la cocina. */
@Composable
internal fun PaginaCronometro(estado: EstadoIntro) {
    PaginaMarco(PaginaIntro.CRONOMETRO) {
        FilaExplica(Icono.Base, R.string.intro_crono_carriles, R.string.intro_crono_carriles_texto)
        FilaExplica(Icono.Reloj, R.string.intro_crono_alarma, R.string.intro_crono_alarma_texto)
        FilaExplica(Icono.Microfono, R.string.intro_crono_voz, R.string.intro_crono_voz_texto)
        CocinaEnMarco(estado.demo, estado.ajustes, ALTO_MARCO_COCINA)
    }
}

/** 5. Tipo y mando de la cocina, fuegos y horno. */
@Composable
internal fun PaginaCocina(
    ajustes: Ajustes,
    acciones: AccionesAjustes,
) {
    PaginaMarco(PaginaIntro.COCINA) {
        SelectorCocina(ajustes.cocina, acciones.cocina)
        SelectorEquipo(ajustes.fuegos, ajustes.horno, acciones.fuegos, acciones.horno, Modifier.padding(top = 8.dp))
    }
}

/** 6. Raciones por defecto y, debajo, la lista de ingredientes real de una receta con rango 1-2, escalada en vivo. */
@Composable
internal fun PaginaRaciones(
    estado: EstadoIntro,
    acciones: AccionesAjustes,
) {
    PaginaMarco(PaginaIntro.RACIONES) {
        SelectorRaciones(estado.ajustes.racionesDefecto, acciones.raciones)
        estado.demo.raciones?.let { IngredientesEscalados(it, estado.ajustes.racionesDefecto) }
    }
}

/** 9. Disposición del cronómetro y, debajo, la cocina real con la disposición elegida. */
@Composable
internal fun PaginaDisposicion(
    estado: EstadoIntro,
    acciones: AccionesAjustes,
) {
    PaginaMarco(PaginaIntro.DISPOSICION) {
        SelectorDisposicionTexto(estado.ajustes.disposicion, acciones.disposicion)
        CocinaEnMarco(estado.demo, estado.ajustes, ALTO_MARCO_COCINA)
    }
}

/**
 * La lista de ingredientes de [receta] sobre papel, como en la ficha, para las raciones por defecto [defecto] (o la
 * permitida más cercana si la receta no las admite), y la línea con el rango de la receta.
 */
@Composable
private fun IngredientesEscalados(
    receta: Receta,
    defecto: Int,
) {
    val permitidas = Escalado.racionesPermitidas(receta)
    val raciones = Escalado.racionesIniciales(receta, null, defecto)
    val factor = Escalado.factor(raciones, receta.raciones)
    Surface(
        Modifier.fillMaxWidth().testTag("intro-ingredientes"),
        shape = MaterialTheme.shapes.medium,
        color = FichaPalette.papel,
        contentColor = FichaPalette.tinta,
        border = BorderStroke(1.dp, FichaPalette.linea),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(
                    R.string.intro_raciones_ejemplo,
                    receta.titulo,
                    pluralStringResource(R.plurals.raciones_valor, raciones, raciones),
                ),
                Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            Escalado.listaGeneral(receta).forEach { FilaIngrediente(it, factor) }
            if (permitidas.size > 1) {
                Text(
                    stringResource(R.string.ficha_aviso_rango, permitidas.first(), permitidas.last()),
                    Modifier.padding(top = 8.dp).testTag("intro-rango"),
                    style = MaterialTheme.typography.bodySmall,
                    color = FichaPalette.meta,
                )
            }
        }
    }
}

@Composable
private fun FilaIngrediente(
    item: Ingrediente,
    factor: Double,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            Escalado.cantidad(item, factor),
            Modifier.width(ANCHO_CANTIDAD).padding(end = 12.dp),
            style = MaterialTheme.typography.bodyMedium.merge(Tabular),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
        Text(item.n, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}

/** La pantalla de cocina real (los mismos composables que el cronómetro) de la receta de ejemplo, en un marco. */
@Composable
internal fun CocinaEnMarco(
    demo: DatosDemo,
    ajustes: Ajustes,
    alto: Dp,
) {
    val receta = demo.cocina ?: return
    val datos = remember(receta, ajustes.cocina) { cocinaDemo(receta, ajustes.cocina) }
    MarcoDemo(stringResource(R.string.intro_demo_cocina), alto, fondo = CronoPalette.fondo) {
        CompositionLocalProvider(LocalCocina provides ajustes.cocina) {
            Surface(Modifier.fillMaxSize(), color = CronoPalette.fondo, contentColor = CronoPalette.texto) {
                CuerpoCocina(datos, ajustes.disposicion)
            }
        }
    }
}

/** Los carriles en la [disposicion] elegida, con los mandos sin efecto (la cocina de muestra no se toca). */
@Composable
private fun CuerpoCocina(
    datos: DatosCrono,
    disposicion: DisposicionCrono,
) {
    val vistas = vistasCarriles(datos)
    val acciones =
        AccionesCarril(
            hecho = {},
            alternar = {},
            masTreinta = {},
            empezarIgual = {},
            enfocar = {},
            porque = {},
        )
    val cuerpo = Modifier.fillMaxSize().padding(12.dp)
    when (disposicion) {
        DisposicionCrono.FOCO -> CronoFoco(vistas, elegirFoco(vistas, datos.estado.foco), acciones, cuerpo)
        DisposicionCrono.APILADA -> CronoApilado(vistas, acciones, cuerpo)
        DisposicionCrono.CLASICA -> CronoClasico(datos, emptyMap(), acciones, {}, cuerpo)
    }
}
