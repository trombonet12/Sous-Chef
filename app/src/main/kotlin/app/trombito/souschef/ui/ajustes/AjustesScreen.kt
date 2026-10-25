package app.trombito.souschef.ui.ajustes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trombito.souschef.R
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.domain.TemaApp
import app.trombito.souschef.ui.Rutas
import app.trombito.souschef.ui.comun.BarraTitulo
import app.trombito.souschef.ui.ficha.Servir
import app.trombito.souschef.ui.shell.Icono

/** Marco común de las pantallas de Ajustes: barra con atrás y contenido que se desplaza. */
@Composable
internal fun PantallaAjustes(
    titulo: String,
    onAtras: () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        BarraTitulo(titulo, onAtras)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = contenido,
        )
    }
}

/**
 * Menú de Ajustes: seis apartados, cada uno con su pantalla, y el Buzón. [onAbrir] recibe la ruta del apartado;
 * [onBuzon] abre el Buzón.
 */
@Composable
fun AjustesScreen(
    onAtras: () -> Unit,
    onAbrir: (String) -> Unit,
    onBuzon: () -> Unit = {},
    viewModel: AjustesViewModel = hiltViewModel(),
    copiaViewModel: CopiaAutoViewModel = hiltViewModel(),
) {
    val ajustes by viewModel.estado.collectAsStateWithLifecycle()
    val avisoCopia by copiaViewModel.aviso.collectAsStateWithLifecycle()
    AjustesMenu(ajustes, viewModel.version, onAtras, onAbrir, onBuzon, copiaFalla = avisoCopia != null)
}

/** Menú sin ViewModel: estado y versión llegan hechos. */
@Suppress("LongParameterList") // pantalla presentacional: cada parámetro es una pieza que se ve
@Composable
internal fun AjustesMenu(
    ajustes: Ajustes,
    version: String,
    onAtras: () -> Unit,
    onAbrir: (String) -> Unit,
    onBuzon: () -> Unit = {},
    copiaFalla: Boolean = false,
) {
    PantallaAjustes(stringResource(R.string.ajustes_titulo), onAtras) {
        FilaApartado(Icono.Base, R.string.ajustes_cocina, Rutas.AJUSTES_COCINA, onAbrir) { resumenCocina(ajustes) }
        FilaApartado(Icono.Apariencia, R.string.ajustes_apariencia, Rutas.AJUSTES_APARIENCIA, onAbrir) {
            val tema = nombreTema(ajustes.tema).lowercase()
            stringResource(R.string.ajustes_resumen_apariencia, tema, nombreLista(ajustes.lista))
        }
        FilaApartado(Icono.Reloj, R.string.ajustes_horario, Rutas.AJUSTES_HORARIO, onAbrir) {
            val servir = Servir.formato(ajustes.servirMin)
            when (ajustes.horaTarjeta) {
                HoraTarjeta.EMPEZAR -> stringResource(R.string.ajustes_resumen_horario_empezar, servir)
                HoraTarjeta.ACABAR -> stringResource(R.string.ajustes_resumen_horario_acabar, servir)
            }
        }
        FilaApartado(Icono.Crono, R.string.ajustes_crono, Rutas.AJUSTES_CRONO, onAbrir) {
            val voz =
                stringResource(
                    if (ajustes.vozActiva) {
                        R.string.ajustes_resumen_voz_activada
                    } else {
                        R.string.ajustes_resumen_voz_desactivada
                    },
                )
            stringResource(R.string.ajustes_resumen_crono, nombreDisposicion(ajustes.disposicion), voz)
        }
        FilaApartado(Icono.Libreta, R.string.ajustes_historial, Rutas.AJUSTES_HISTORIAL, onAbrir) {
            stringResource(
                if (copiaFalla) R.string.ajustes_resumen_historial_aviso else R.string.ajustes_resumen_historial,
            )
        }
        FilaAjuste(
            icono = Icono.Correo,
            titulo = stringResource(R.string.buzon_titulo),
            resumen = stringResource(R.string.ajustes_resumen_buzon),
            final = Icono.Chevron,
            etiqueta = "ajustes-buzon",
            onClick = onBuzon,
        )
        FilaApartado(Icono.Info, R.string.ajustes_acerca, Rutas.AJUSTES_ACERCA, onAbrir) {
            stringResource(R.string.ajustes_resumen_acerca, version)
        }
        Text(
            stringResource(R.string.ajustes_version, version),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("ajustes-version"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.ajustes_autor),
            modifier = Modifier.fillMaxWidth().testTag("ajustes-autor"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FilaApartado(
    icono: Icono,
    titulo: Int,
    ruta: String,
    onAbrir: (String) -> Unit,
    resumen: @Composable () -> String,
) {
    FilaAjuste(
        icono = icono,
        titulo = stringResource(titulo),
        resumen = resumen(),
        final = Icono.Chevron,
        etiqueta = "ajustes-$ruta",
        onClick = { onAbrir(ruta) },
    )
}

/** Fila común de Ajustes: icono, título con resumen y un icono final (chevron o enlace externo). */
@Suppress("LongParameterList") // fila presentacional: cada parámetro es una pieza visible
@Composable
internal fun FilaAjuste(
    icono: Icono,
    titulo: String,
    resumen: String,
    final: Icono,
    etiqueta: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag(etiqueta)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icono.vector, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(
                resumen,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(final.vector, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun nombreTema(tema: TemaApp): String =
    stringResource(
        when (tema) {
            TemaApp.AUTOMATICO -> R.string.ajustes_tema_automatico
            TemaApp.CLARO -> R.string.ajustes_tema_claro
            TemaApp.OSCURO -> R.string.ajustes_tema_oscuro
        },
    )

@Composable
private fun nombreLista(lista: DisposicionLista): String =
    stringResource(
        when (lista) {
            DisposicionLista.TINTE -> R.string.ajustes_lista_tinte
            DisposicionLista.FRANJA -> R.string.ajustes_lista_franja
            DisposicionLista.CLASICA -> R.string.ajustes_lista_clasica
        },
    )

@Composable
private fun nombreDisposicion(d: DisposicionCrono): String =
    stringResource(
        when (d) {
            DisposicionCrono.APILADA -> R.string.ajustes_resumen_apilada
            DisposicionCrono.FOCO -> R.string.ajustes_resumen_foco
            DisposicionCrono.CLASICA -> R.string.ajustes_resumen_clasica
        },
    )
