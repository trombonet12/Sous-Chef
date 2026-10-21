package app.trombito.souschef.ui.bienvenida

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.domain.TemaApp
import app.trombito.souschef.ui.ajustes.resumenCocina
import app.trombito.souschef.ui.ficha.Servir
import app.trombito.souschef.ui.recetas.TarjetaLista
import app.trombito.souschef.ui.shell.Icono

/** Lado del círculo con la mascota de la bienvenida. */
private val LADO_MASCOTA_GRANDE = 176.dp

/** El adaptive icon mide 108 dp y su zona segura 72: se amplía para que la mascota llene el círculo. */
private val LADO_ICONO_COMPLETO = LADO_MASCOTA_GRANDE * 108 / 72

private val ALTO_MARCO_LISTA = 330.dp

/** 1. Qué es Sous Chef, con Trombito. */
@Composable
internal fun PaginaBienvenida() {
    PaginaMarco(
        PaginaIntro.BIENVENIDA,
        arriba = {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(LADO_MASCOTA_GRANDE)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .testTag("intro-trombito"),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.requiredSize(LADO_ICONO_COMPLETO),
                    )
                }
            }
        },
    ) {}
}

/** 2. Lista, familias, filtros y ficha; dos tarjetas reales con el diseño y la hora de ahora. */
@Composable
internal fun PaginaRecetas(estado: EstadoIntro) {
    PaginaMarco(PaginaIntro.RECETAS) {
        val recetas = estado.demo.tarjetas
        if (recetas.isNotEmpty()) {
            MarcoDemo(stringResource(R.string.intro_demo_lista), ALTO_MARCO_LISTA) {
                Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recetas.forEach {
                        TarjetaLista(
                            tarjetaDemo(it, estado.ajustes, estado.ahoraMin),
                            estado.ajustes.lista,
                            onFavorita = {},
                            onAbrir = {},
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/** 4. Historial, técnicas, la copia del historial y el Buzón. */
@Composable
internal fun PaginaHistorial() {
    PaginaMarco(PaginaIntro.HISTORIAL) {
        FilaExplica(Icono.Reloj, R.string.intro_hist_historial, R.string.intro_hist_historial_texto)
        FilaExplica(Icono.Libreta, R.string.intro_hist_tecnicas, R.string.intro_hist_tecnicas_texto)
        FilaExplica(Icono.Copiar, R.string.intro_hist_copia, R.string.intro_hist_copia_texto)
        FilaExplica(Icono.Correo, R.string.intro_hist_buzon, R.string.intro_hist_buzon_texto)
    }
}

/** 12. Resumen de lo elegido. «Empezar a cocinar» es el botón de abajo. */
@Composable
internal fun PaginaListo(ajustes: Ajustes) {
    PaginaMarco(PaginaIntro.LISTO) {
        Column(Modifier.testTag("intro-resumen"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FilaResumen(R.string.intro_res_cocina, resumenCocina(ajustes))
            FilaResumen(
                R.string.intro_res_raciones,
                pluralStringResource(R.plurals.raciones_valor, ajustes.racionesDefecto, ajustes.racionesDefecto),
            )
            FilaResumen(R.string.intro_res_horario, resumenHorario(ajustes))
            FilaResumen(R.string.intro_res_apariencia, resumenApariencia(ajustes))
            FilaResumen(R.string.intro_res_crono, resumenCrono(ajustes))
        }
    }
}

@Composable
private fun FilaResumen(
    titulo: Int,
    valor: String,
) {
    Column {
        Text(
            stringResource(titulo),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun resumenHorario(ajustes: Ajustes): String {
    val servir = Servir.formato(ajustes.servirMin)
    return when (ajustes.horaTarjeta) {
        HoraTarjeta.EMPEZAR -> stringResource(R.string.ajustes_resumen_horario_empezar, servir)
        HoraTarjeta.ACABAR -> stringResource(R.string.ajustes_resumen_horario_acabar, servir)
    }
}

@Composable
private fun resumenApariencia(ajustes: Ajustes): String {
    val tema =
        stringResource(
            when (ajustes.tema) {
                TemaApp.AUTOMATICO -> R.string.ajustes_tema_automatico
                TemaApp.CLARO -> R.string.ajustes_tema_claro
                TemaApp.OSCURO -> R.string.ajustes_tema_oscuro
            },
        )
    val lista =
        stringResource(
            when (ajustes.lista) {
                DisposicionLista.TINTE -> R.string.ajustes_lista_tinte
                DisposicionLista.FRANJA -> R.string.ajustes_lista_franja
                DisposicionLista.CLASICA -> R.string.ajustes_lista_clasica
            },
        )
    return stringResource(R.string.ajustes_resumen_apariencia, tema.lowercase(), lista)
}

@Composable
private fun resumenCrono(ajustes: Ajustes): String {
    val disposicion =
        stringResource(
            when (ajustes.disposicion) {
                DisposicionCrono.APILADA -> R.string.ajustes_resumen_apilada
                DisposicionCrono.FOCO -> R.string.ajustes_resumen_foco
                DisposicionCrono.CLASICA -> R.string.ajustes_resumen_clasica
            },
        )
    val voz =
        stringResource(
            if (ajustes.vozActiva) R.string.ajustes_resumen_voz_activada else R.string.ajustes_resumen_voz_desactivada,
        )
    return stringResource(R.string.ajustes_resumen_crono, disposicion, voz)
}
