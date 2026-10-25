package app.trombito.souschef.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import app.trombito.souschef.domain.crono.CocinadasVivas
import app.trombito.souschef.ui.shell.Pestana
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Accesos directos estáticos del icono (F17, `res/xml/shortcuts.xml`). Cada uno abre `MainActivity` con un intent
 * **explícito** y el extra [EXTRA_ATAJO]; no hay intent-filter público ni BROWSABLE, así que ninguna web ni app los
 * lanza por un enlace (solo el lanzador del móvil o quien conozca el componente, que solo consigue navegar).
 */
const val EXTRA_ATAJO = "atajo"

/** Clave de una sola vez en el estado de la entrada Recetas: pide encender «¿Qué me da tiempo?». */
const val CLAVE_ATAJO_TIEMPO = "atajo_que_me_da_tiempo"

/** Los atajos del manifest; [clave] es el valor del extra y debe coincidir con `shortcuts.xml`. */
enum class Atajo(
    val clave: String,
) {
    CENAS_RAPIDAS("cenas_rapidas"),
    QUE_ME_DA_TIEMPO("que_me_da_tiempo"),
    VOLVER_COCINA("volver_cocina"),
    ;

    companion object {
        /** El atajo de una [clave], o null si no es de ninguno (un extra ajeno o vacío no hace nada). */
        fun de(clave: String?): Atajo? = entries.firstOrNull { it.clave == clave }
    }
}

/** El atajo con el que se abrió o se reabrió la actividad, si lo hay. */
fun atajoDe(intent: Intent?): Atajo? = Atajo.de(intent?.getStringExtra(EXTRA_ATAJO))

/** A dónde lleva un atajo: [ruta] y si, además, hay que encender «¿Qué me da tiempo?» en Recetas. */
data class DestinoAtajo(
    val ruta: String,
    val modoTiempo: Boolean = false,
)

/**
 * Destino de [atajo]. Los dos de Recetas son una entrada normal a Recetas (pestaña o lista de Cenas rápidas); «Volver a
 * la cocina» entra al cronómetro de la cocinada viva [slugViva] y, si no hay ninguna, a Recetas.
 */
fun destinoDeAtajo(
    atajo: Atajo,
    slugViva: String?,
): DestinoAtajo =
    when (atajo) {
        Atajo.CENAS_RAPIDAS -> {
            DestinoAtajo(Rutas.recetasFamilia(Rutas.CENAS_RAPIDAS))
        }

        Atajo.QUE_ME_DA_TIEMPO -> {
            DestinoAtajo(Pestana.Recetas.ruta, modoTiempo = true)
        }

        Atajo.VOLVER_COCINA -> {
            if (slugViva != null) DestinoAtajo(Rutas.cronoViva(slugViva)) else DestinoAtajo(Pestana.Recetas.ruta)
        }
    }

/** Qué cocinada hay viva, para «Volver a la cocina». */
@HiltViewModel
class AtajosViewModel
    @Inject
    constructor(
        private val vivas: CocinadasVivas,
    ) : ViewModel() {
        suspend fun slugViva(): String? = vivas.todas().firstOrNull()?.slug
    }

/**
 * Lleva a su destino el [atajo] pendiente (clave del extra) en cuanto el grafo está montado y [alAplicar] lo da por
 * hecho. Con la intro sin ver no se hace nada: el atajo se descarta y se queda la intro. El destino de Recetas deja la
 * pestaña Recetas como base de la pila, igual que una entrada normal.
 */
@Composable
fun AplicarAtajo(
    navController: NavHostController,
    atajo: String?,
    alAplicar: () -> Unit,
) {
    val viewModel = hiltViewModel<AtajosViewModel>()
    AplicarAtajo(navController, atajo, viewModel::slugViva, alAplicar)
}

/** Lo mismo con la consulta de la cocinada viva a mano (los tests no necesitan Hilt). */
@Composable
internal fun AplicarAtajo(
    navController: NavHostController,
    atajo: String?,
    slugViva: suspend () -> String?,
    alAplicar: () -> Unit,
) {
    val entrada by navController.currentBackStackEntryAsState()
    val ruta = entrada?.destination?.route
    LaunchedEffect(atajo, ruta == null) {
        val pedido = Atajo.de(atajo)
        if (ruta == null || atajo == null) return@LaunchedEffect
        if (pedido != null && !ruta.startsWith(Rutas.BIENVENIDA)) {
            val viva = if (pedido == Atajo.VOLVER_COCINA) slugViva() else null
            navController.abrir(destinoDeAtajo(pedido, viva))
        }
        // Lo último: marcar el atajo como hecho cambia la clave y reinicia este efecto.
        alAplicar()
    }
}

private fun NavHostController.abrir(destino: DestinoAtajo) {
    navigate(destino.ruta) {
        popUpTo(Pestana.Recetas.ruta)
        launchSingleTop = true
    }
    if (destino.modoTiempo) getBackStackEntry(Pestana.Recetas.ruta).savedStateHandle[CLAVE_ATAJO_TIEMPO] = true
}
