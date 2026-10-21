package app.trombito.souschef.ui.bienvenida

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trombito.souschef.R
import app.trombito.souschef.ui.ajustes.AjustesViewModel
import app.trombito.souschef.ui.comun.rememberPedirMicrofono
import app.trombito.souschef.ui.comun.rememberPermisosApp
import app.trombito.souschef.ui.comun.tieneMicrofono
import kotlinx.coroutines.launch
import java.time.LocalTime

private val BOTON_MIN = 48.dp
private val PUNTO = 8.dp
private val PUNTO_ACTUAL = 10.dp

/**
 * Intro guiada: se enseña en el primer arranque y desde Ajustes. Al entrar la marca como no vista (para que elegir un
 * ajuste no la dé por vista) y al acabar o saltar la marca como vista y llama a [onTerminar]. Cada elección se guarda
 * al momento con [ajustesVm].
 */
@Composable
fun BienvenidaScreen(
    onTerminar: () -> Unit,
    ajustesVm: AjustesViewModel = hiltViewModel(),
    demoVm: BienvenidaViewModel = hiltViewModel(),
) {
    val ajustes by ajustesVm.estado.collectAsStateWithLifecycle()
    val demo by demoVm.demo.collectAsStateWithLifecycle()
    val voz by demoVm.voz.collectAsStateWithLifecycle()
    val permisos = rememberPermisosApp()
    val contexto = LocalContext.current
    val ahoraMin = remember { LocalTime.now().let { it.hour * 60 + it.minute } }
    val frase = stringResource(R.string.intro_voz_frase)
    var explicarMicrofono by remember { mutableStateOf(false) }
    val pedirMicrofono = rememberPedirMicrofono { if (it) demoVm.probarVoz(frase) else demoVm.sinPermiso() }
    LaunchedEffect(Unit) { ajustesVm.marcarIntroVista(false) }
    val terminar = {
        demoVm.marcarIntroVista()
        onTerminar()
    }
    val acciones =
        AccionesIntro(
            ajustes = AccionesAjustes.de(ajustesVm),
            permitir = permisos::permitir,
            probarVoz = { if (tieneMicrofono(contexto)) demoVm.probarVoz(frase) else explicarMicrofono = true },
            terminar = terminar,
            saltar = terminar,
        )
    Bienvenida(
        EstadoIntro(ajustes, demo, voz, permisos.estado, ahoraMin),
        acciones,
        alCambiarPagina = { if (it != PaginaIntro.VOZ) demoVm.pararVoz() },
    )
    if (explicarMicrofono) {
        DialogoMicrofono(
            onContinuar = {
                explicarMicrofono = false
                pedirMicrofono()
            },
            onAhoraNo = { explicarMicrofono = false },
        )
    }
}

/** La intro sin ViewModels: estado y acciones llegan hechos. [paginaInicial] es para las pruebas. */
@Composable
internal fun Bienvenida(
    estado: EstadoIntro,
    acciones: AccionesIntro,
    paginaInicial: Int = 0,
    alCambiarPagina: (PaginaIntro) -> Unit = {},
) {
    val paginas = PaginaIntro.entries
    val pager = rememberPagerState(initialPage = paginaInicial) { paginas.size }
    val alcance = rememberCoroutineScope()
    val actual = pager.currentPage
    val pagina = paginas[actual]
    val ultima = actual == paginas.lastIndex
    LaunchedEffect(pagina) { alCambiarPagina(pagina) }
    val ir: (Int) -> Unit = { destino -> alcance.launch { pager.animateScrollToPage(destino) } }
    BackHandler(enabled = actual > 0) { ir(actual - 1) }
    Column(Modifier.fillMaxSize().testTag("intro")) {
        BarraSuperior(visible = !ultima, onSaltar = acciones.saltar)
        HorizontalPager(
            pager,
            Modifier.weight(1f).fillMaxWidth(),
            userScrollEnabled = !pagina.conControles,
            key = { paginas[it].name },
        ) { Pagina(paginas[it], estado, acciones) }
        Puntos(actual, paginas.size)
        BotonesNavegacion(
            hayAtras = actual > 0,
            ultima = ultima,
            onAtras = { ir(actual - 1) },
            onSiguiente = { if (ultima) acciones.terminar() else ir(actual + 1) },
        )
    }
}

@Composable
private fun Pagina(
    pagina: PaginaIntro,
    estado: EstadoIntro,
    acciones: AccionesIntro,
) {
    when (pagina) {
        PaginaIntro.BIENVENIDA -> PaginaBienvenida()
        PaginaIntro.RECETAS -> PaginaRecetas(estado)
        PaginaIntro.CRONOMETRO -> PaginaCronometro(estado)
        PaginaIntro.COCINA -> PaginaCocina(estado.ajustes, acciones.ajustes)
        PaginaIntro.RACIONES -> PaginaRaciones(estado, acciones.ajustes)
        PaginaIntro.HORARIO -> PaginaHorario(estado, acciones.ajustes)
        PaginaIntro.APARIENCIA -> PaginaApariencia(estado, acciones.ajustes)
        PaginaIntro.DISPOSICION -> PaginaDisposicion(estado, acciones.ajustes)
        PaginaIntro.VOZ -> PaginaVoz(estado, acciones.ajustes.voz, acciones.probarVoz)
        PaginaIntro.PERMISOS -> PaginaPermisos(estado.permisos, acciones.permitir)
        PaginaIntro.LISTO -> PaginaListo(estado.ajustes)
    }
}

/** «Saltar» arriba a la derecha; en la última página no hace falta, pero el hueco se conserva. */
@Composable
private fun BarraSuperior(
    visible: Boolean,
    onSaltar: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = BOTON_MIN).padding(horizontal = 8.dp), Arrangement.End) {
        if (visible) {
            TextButton(onClick = onSaltar, modifier = Modifier.heightIn(min = BOTON_MIN).testTag("intro-saltar")) {
                Text(stringResource(R.string.intro_saltar))
            }
        }
    }
}

/** Puntos de progreso; para TalkBack son un solo elemento («Paso 3 de 11»). */
@Composable
private fun Puntos(
    actual: Int,
    total: Int,
) {
    val descripcion = stringResource(R.string.intro_progreso, actual + 1, total)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("intro-puntos")
            .clearAndSetSemantics { contentDescription = descripcion },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) {
            val lleno = it == actual
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (lleno) PUNTO_ACTUAL else PUNTO)
                    .clip(CircleShape)
                    .background(
                        if (lleno) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

@Composable
private fun BotonesNavegacion(
    hayAtras: Boolean,
    ultima: Boolean,
    onAtras: () -> Unit,
    onSiguiente: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hayAtras) {
            TextButton(onClick = onAtras, modifier = Modifier.heightIn(min = BOTON_MIN).testTag("intro-atras")) {
                Text(stringResource(R.string.intro_atras))
            }
        } else {
            Spacer(Modifier.size(1.dp))
        }
        Button(onClick = onSiguiente, modifier = Modifier.heightIn(min = BOTON_MIN).testTag("intro-siguiente")) {
            Text(stringResource(if (ultima) R.string.intro_empezar else R.string.intro_siguiente))
        }
    }
}

/** Por qué se pide el micrófono, antes del diálogo del sistema. */
@Composable
private fun DialogoMicrofono(
    onContinuar: () -> Unit,
    onAhoraNo: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onAhoraNo,
        title = { Text(stringResource(R.string.intro_mic_titulo)) },
        text = { Text(stringResource(R.string.intro_mic_texto)) },
        confirmButton = {
            TextButton(onClick = onContinuar, modifier = Modifier.heightIn(min = BOTON_MIN).testTag("intro-mic-ok")) {
                Text(stringResource(R.string.intro_mic_continuar))
            }
        },
        dismissButton = {
            TextButton(onClick = onAhoraNo, modifier = Modifier.heightIn(min = BOTON_MIN)) {
                Text(stringResource(R.string.intro_mic_ahora_no))
            }
        },
    )
}
