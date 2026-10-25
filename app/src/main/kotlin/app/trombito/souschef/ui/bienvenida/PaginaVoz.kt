package app.trombito.souschef.ui.bienvenida

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.voz.OrdenVoz
import app.trombito.souschef.ui.crono.voz.GruposOrdenesVoz

/** 10. Interruptor de la voz y botón «Probar»: el chef habla, se escucha una vez y se enseña lo entendido. */
@Composable
internal fun PaginaVoz(
    estado: EstadoIntro,
    onVoz: (Boolean) -> Unit,
    onProbar: () -> Unit,
) {
    PaginaMarco(PaginaIntro.VOZ) {
        Text(stringResource(R.string.intro_voz_cabecera), style = MaterialTheme.typography.titleMedium)
        OrdenesDisponibles()
        Row(
            Modifier
                .fillMaxWidth()
                .testTag("intro-voz-interruptor")
                .toggleable(estado.ajustes.vozActiva, role = Role.Switch, onValueChange = onVoz)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ajustes_voz), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.ajustes_voz_detalle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = estado.ajustes.vozActiva, onCheckedChange = null)
        }
        Button(onClick = onProbar, modifier = Modifier.heightIn(min = 48.dp).testTag("intro-voz-probar")) {
            Text(stringResource(R.string.intro_voz_probar))
        }
        ResultadoVoz(estado.voz)
    }
}

/** Las órdenes que entiende el intérprete, agrupadas y con un ejemplo dicho de cada una. */
@Composable
private fun OrdenesDisponibles() {
    Column(Modifier.fillMaxWidth().testTag("intro-voz-ordenes"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.intro_voz_ordenes_titulo),
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        GruposOrdenesVoz(MaterialTheme.colorScheme.primary)
    }
}

/** Lo que pasa en la prueba, anunciado a TalkBack al cambiar. */
@Composable
private fun ResultadoVoz(voz: PruebaVoz) {
    Column(
        Modifier
            .fillMaxWidth()
            .testTag("intro-voz-resultado")
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (voz) {
            is PruebaVoz.Oido -> {
                Text(stringResource(R.string.intro_voz_oido, voz.frase), style = MaterialTheme.typography.bodyLarge)
                val entendido =
                    voz.orden?.let { stringResource(R.string.intro_voz_entendido, stringResource(it.descripcion())) }
                Text(
                    entendido ?: stringResource(R.string.intro_voz_no_entendido),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            else -> {
                Text(stringResource(voz.mensaje()), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@StringRes
private fun PruebaVoz.mensaje(): Int =
    when (this) {
        PruebaVoz.Inactiva -> R.string.intro_voz_pista
        PruebaVoz.Hablando -> R.string.intro_voz_hablando
        PruebaVoz.Escuchando -> R.string.intro_voz_escuchando
        PruebaVoz.SinReconocedor -> R.string.intro_voz_sin_reconocedor
        PruebaVoz.SinPermiso -> R.string.intro_voz_sin_permiso
        PruebaVoz.NadaOido -> R.string.intro_voz_nada
        is PruebaVoz.Oido -> R.string.intro_voz_pista
    }

/** Qué hace la orden, en palabras. */
@StringRes
internal fun OrdenVoz.descripcion(): Int =
    when (this) {
        is OrdenVoz.Hecho -> R.string.intro_orden_hecho

        is OrdenVoz.Pausa -> R.string.intro_orden_pausa

        is OrdenVoz.Seguir -> R.string.intro_orden_seguir

        is OrdenVoz.Empezar -> R.string.intro_orden_empezar

        is OrdenVoz.Prorrogar -> R.string.intro_orden_prorrogar

        is OrdenVoz.CuantoQueda, is OrdenVoz.QueToca, is OrdenVoz.QueViene, is OrdenVoz.ProximosPasos,
        -> R.string.intro_orden_consulta

        is OrdenVoz.PorQue, is OrdenVoz.Info -> R.string.intro_orden_explica

        OrdenVoz.Para -> R.string.intro_orden_para

        OrdenVoz.Repite -> R.string.intro_orden_repite

        is OrdenVoz.Deshacer -> R.string.intro_orden_deshacer
    }
