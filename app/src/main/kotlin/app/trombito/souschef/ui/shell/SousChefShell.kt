package app.trombito.souschef.ui.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import app.trombito.souschef.R
import app.trombito.souschef.ui.Rutas
import app.trombito.souschef.ui.SousChefNavGraph

/** Shell con tres pestañas abajo (Recetas, Técnicas, Historial) y menú superior (Buzón y Ajustes). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SousChefShell(
    navController: NavHostController,
    onBuzon: () -> Unit = { navController.navigate(Rutas.BUZON) { launchSingleTop = true } },
) {
    val entrada by navController.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    var menuAbierto by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            // Sin barras en el cronómetro ni en la intro (que pinta a pantalla completa).
            val sinBarra = rutaActual?.startsWith("crono") == true || rutaActual?.startsWith(Rutas.BIENVENIDA) == true
            if (sinBarra) return@Scaffold
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = { menuAbierto = true }) {
                        Icon(Icono.Mas.vector, contentDescription = stringResource(R.string.menu_mas))
                    }
                    DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.buzon_titulo)) },
                            onClick = {
                                menuAbierto = false
                                onBuzon()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_ajustes)) },
                            onClick = {
                                menuAbierto = false
                                navController.navigate(Rutas.AJUSTES) { launchSingleTop = true }
                            },
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (Pestana.entries.any { it.ruta == rutaActual }) {
                NavigationBar {
                    Pestana.entries.forEach { p ->
                        NavigationBarItem(
                            selected = rutaActual == p.ruta,
                            onClick = {
                                navController.navigate(p.ruta) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(p.icono.vector, contentDescription = null) },
                            label = { Text(stringResource(p.titulo)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        SousChefNavGraph(navController, Modifier.padding(padding))
    }
}
