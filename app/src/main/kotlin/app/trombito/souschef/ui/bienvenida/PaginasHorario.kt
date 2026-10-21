package app.trombito.souschef.ui.bienvenida

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.ui.ajustes.SelectorLista
import app.trombito.souschef.ui.ajustes.SelectorTema
import app.trombito.souschef.ui.ajustes.TituloApartado
import app.trombito.souschef.ui.comun.PasosMasMenos
import app.trombito.souschef.ui.ficha.Servir
import app.trombito.souschef.ui.recetas.TarjetaLista

/** Duración de ejemplo de los detalles de «Hora en las tarjetas». */
private const val DURACION_EJEMPLO_MIN = 47

/** 7. Hora de servir por defecto, qué hora enseñan las tarjetas y una tarjeta real que cambia en vivo. */
@Composable
internal fun PaginaHorario(
    estado: EstadoIntro,
    acciones: AccionesAjustes,
) {
    val ajustes = estado.ajustes
    PaginaMarco(PaginaIntro.HORARIO) {
        TituloApartado(stringResource(R.string.ajustes_servir_titulo))
        PasosMasMenos(
            valor = Servir.formato(ajustes.servirMin),
            descMenos = stringResource(R.string.ficha_servir_menos),
            descMas = stringResource(R.string.ficha_servir_mas),
            onMenos = { acciones.servir(-Servir.PASO) },
            onMas = { acciones.servir(Servir.PASO) },
        )
        Text(
            stringResource(R.string.ajustes_servir_nota),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TituloApartado(stringResource(R.string.ajustes_tarjetas_titulo))
        OpcionesHora(ajustes, estado.ahoraMin, acciones.horaTarjeta)
        estado.demo.cocina?.let {
            TarjetaLista(
                tarjetaDemo(it, ajustes, estado.ahoraMin),
                ajustes.lista,
                onFavorita = {},
                onAbrir = {},
                modifier = Modifier.fillMaxWidth().testTag("intro-tarjeta-horario"),
            )
        }
    }
}

/** 8. Tema y diseño de las listas y, debajo, dos tarjetas reales a tamaño completo con la elección. */
@Composable
internal fun PaginaApariencia(
    estado: EstadoIntro,
    acciones: AccionesAjustes,
) {
    val ajustes = estado.ajustes
    PaginaMarco(PaginaIntro.APARIENCIA) {
        TituloApartado(stringResource(R.string.ajustes_tema))
        SelectorTema(ajustes.tema, acciones.tema)
        TituloApartado(stringResource(R.string.ajustes_diseno_listas))
        SelectorLista(ajustes.lista, acciones.lista)
        Column(Modifier.testTag("intro-tarjetas"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.demo.tarjetas.forEach {
                TarjetaLista(
                    tarjetaDemo(it, ajustes, estado.ahoraMin),
                    ajustes.lista,
                    onFavorita = {},
                    onAbrir = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** «Cuándo empezar» y «Si empiezas ya», con la hora de ejemplo según la de servir y la de ahora. */
@Composable
private fun OpcionesHora(
    ajustes: Ajustes,
    ahoraMin: Int,
    onElegir: (HoraTarjeta) -> Unit,
) {
    val servir = Servir.formato(ajustes.servirMin)
    val empieza = Servir.formato(Servir.ajustar(ajustes.servirMin, -DURACION_EJEMPLO_MIN))
    val acaba = Servir.formato(Servir.ajustar(ahoraMin, DURACION_EJEMPLO_MIN))
    Column(Modifier.fillMaxWidth().selectableGroup()) {
        FilaHora(
            HoraTarjeta.EMPEZAR,
            ajustes.horaTarjeta,
            stringResource(R.string.ajustes_tarjeta_empezar),
            stringResource(R.string.ajustes_tarjeta_empezar_detalle, empieza, servir),
            onElegir,
        )
        FilaHora(
            HoraTarjeta.ACABAR,
            ajustes.horaTarjeta,
            stringResource(R.string.ajustes_tarjeta_acabar),
            stringResource(R.string.ajustes_tarjeta_acabar_detalle, acaba),
            onElegir,
        )
    }
}

@Composable
private fun FilaHora(
    opcion: HoraTarjeta,
    elegida: HoraTarjeta,
    titulo: String,
    detalle: String,
    onElegir: (HoraTarjeta) -> Unit,
) {
    val marcada = opcion == elegida
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("hora-${opcion.name}")
            .selectable(selected = marcada, role = Role.RadioButton, onClick = { onElegir(opcion) }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = marcada, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (marcada) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
