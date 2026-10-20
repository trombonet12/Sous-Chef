package app.trombito.souschef.ui.crono

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trombito.souschef.MainActivity
import app.trombito.souschef.R
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.crono.CadenaAlarmas
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.SousChefTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

/**
 * Pantalla de alarma al estilo reloj: se muestra sobre el bloqueo, enciende la pantalla, lista lo que pide atención
 * (las líneas de la alarma única) y permite parar el sonido o dar el paso por hecho. La abre el full-screen intent,
 * que solo se pide con el móvil bloqueado; el sonido es de la notificación, no de aquí.
 */
@AndroidEntryPoint
class AlarmaActivity : ComponentActivity() {
    @Inject
    lateinit var cadena: CadenaAlarmas

    @Inject
    lateinit var ajustesRepo: AjustesRepo

    private var datos by mutableStateOf<DatosAlarma?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        datos = leer(intent)
        setContent {
            val ajustes by ajustesRepo.observar().collectAsStateWithLifecycle(initialValue = Ajustes())
            SousChefTheme(darkTheme = ajustes.tema.oscuro(isSystemInDarkTheme())) {
                Surface(Modifier.fillMaxSize(), color = CronoPalette.fondo, contentColor = CronoPalette.texto) {
                    datos?.let { d -> PantallaAlarma(d, ::parar, ::hechoYSiguiente, ::abrirCocina) }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        datos = leer(intent)
    }

    private fun leer(intent: Intent): DatosAlarma? {
        val slug = intent.getStringExtra(EXTRA_SLUG) ?: return null
        return DatosAlarma(
            slug = slug,
            titulo = intent.getStringExtra(EXTRA_TITULO).orEmpty(),
            lineas = intent.getStringArrayExtra(EXTRA_LINEAS)?.toList().orEmpty(),
            instanteMs = intent.getLongExtra(EXTRA_INSTANTE, System.currentTimeMillis()),
            conHecho = intent.getBooleanExtra(EXTRA_HECHO, false),
        )
    }

    // Ámbito propio: la actividad se cierra enseguida y no debe cancelar el trabajo. El sonido es el de la
    // notificación: quitarla (silenciar) lo corta.
    private fun enSegundoPlano(trabajo: suspend () -> Unit) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { trabajo() }
    }

    private fun parar() {
        val d = datos
        if (d != null) enSegundoPlano { cadena.silenciar(d.slug) }
        finish()
    }

    private fun hechoYSiguiente() {
        val d = datos ?: return
        enSegundoPlano {
            cadena.silenciar(d.slug)
            cadena.siguiente(d.slug)
        }
        finish()
    }

    private fun abrirCocina() {
        val d = datos ?: return
        enSegundoPlano { cadena.silenciar(d.slug) }
        startActivity(
            Intent(Intent.ACTION_VIEW, "souschef://crono/${d.slug}".toUri(), this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    companion object {
        const val EXTRA_SLUG = "slug"
        const val EXTRA_TITULO = "titulo"
        const val EXTRA_LINEAS = "lineas"
        const val EXTRA_INSTANTE = "instante"
        const val EXTRA_HECHO = "hecho"
    }
}

private data class DatosAlarma(
    val slug: String,
    val titulo: String,
    /** Una línea por carril que pide algo: «SARTÉN GRANDE: Primera fritura: hecho». */
    val lineas: List<String>,
    val instanteMs: Long,
    /** Hay un paso vencido al que dar por hecho. */
    val conHecho: Boolean,
)

@Composable
private fun PantallaAlarma(
    d: DatosAlarma,
    onParar: () -> Unit,
    onSiguiente: () -> Unit,
    onAbrir: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            d.titulo,
            style = MaterialTheme.typography.titleMedium,
            color = CronoPalette.tenue,
            textAlign = TextAlign.Center,
        )
        Text(
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(d.instanteMs)),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
        )
        d.lineas.forEach { linea ->
            Text(
                linea,
                style = MaterialTheme.typography.headlineSmall,
                color = CronoPalette.acento,
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = onParar,
            Modifier.fillMaxWidth().heightIn(min = 72.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CronoPalette.alerta),
        ) { Text(stringResource(R.string.crono_alarma_parar), style = MaterialTheme.typography.titleLarge) }
        if (d.conHecho) {
            OutlinedButton(onClick = onSiguiente, Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                Text(
                    stringResource(R.string.crono_alarma_hecho_siguiente),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        TextButton(onClick = onAbrir, Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.crono_alarma_abrir))
        }
    }
}
