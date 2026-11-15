package app.trombito.souschef.ui.ficha

import android.content.Context
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.trombito.souschef.R
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.shell.Icono

/**
 * Botón de la barra de la ficha: ofrece las dos salidas de la impresión («Hoja de cocina» y «Ficha completa») en un
 * menú. [servirMin] (minutos desde medianoche) pone en el papel la hora de empezar, la de servir y la de cada paso.
 */
@Composable
fun BotonImprimirFicha(
    receta: Receta,
    raciones: Int,
    cocina: Cocina,
    servirMin: Int?,
) {
    val contexto = LocalContext.current
    var abierto by remember { mutableStateOf(false) }
    val nombreHoja = stringResource(R.string.impr_pdf_hoja, receta.titulo)
    val nombreCompleta = stringResource(R.string.ficha_pdf_nombre, receta.titulo)
    val imprimir = { tipo: TipoFicha ->
        abierto = false
        val html =
            fichaHtml(
                receta,
                raciones,
                TextosFicha.desde(contexto, cocina),
                tipo,
                servirMin,
                TextosImpresion.desde(contexto),
            )
        imprimirFicha(contexto, if (tipo == TipoFicha.HOJA) nombreHoja else nombreCompleta, html)
    }
    Box {
        IconButton(onClick = { abierto = true }, modifier = Modifier.testTag("imprimir")) {
            Icon(Icono.Imprimir.vector, contentDescription = stringResource(R.string.ficha_imprimir))
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            OpcionImpresion(R.string.impr_hoja, R.string.impr_hoja_desc, "imprimir-hoja") { imprimir(TipoFicha.HOJA) }
            OpcionImpresion(R.string.impr_completa, R.string.impr_completa_desc, "imprimir-completa") {
                imprimir(TipoFicha.COMPLETA)
            }
        }
    }
}

@Composable
private fun OpcionImpresion(
    titulo: Int,
    descripcion: Int,
    etiqueta: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(stringResource(titulo), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(descripcion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        onClick = onClick,
        modifier = Modifier.testTag(etiqueta),
    )
}

/** WebViews con una impresión en curso: la referencia fuerte evita que se recojan antes de terminar. */
private val enCurso = mutableSetOf<WebView>()

/**
 * Abre el diálogo de impresión del sistema (también "Guardar como PDF") con la ficha en [html]. [context] tiene que
 * ser el de la Activity: `PrintManager.print` lanza IllegalStateException con el de la aplicación.
 */
fun imprimirFicha(
    context: Context,
    nombre: String,
    html: String,
) {
    val impresion = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val web = WebView(context)
    enCurso += web
    web.webViewClient =
        object : WebViewClient() {
            override fun onPageFinished(
                view: WebView,
                url: String?,
            ) {
                val adaptador = view.createPrintDocumentAdapter(nombre)
                impresion.print(
                    nombre,
                    LiberaAlTerminar(adaptador) { liberar(view) },
                    PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build(),
                )
            }
        }
    web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
}

private fun liberar(web: WebView) {
    enCurso -= web
    web.destroy()
}

private class LiberaAlTerminar(
    private val base: PrintDocumentAdapter,
    private val alTerminar: () -> Unit,
) : PrintDocumentAdapter() {
    override fun onStart() = base.onStart()

    override fun onLayout(
        anteriores: PrintAttributes?,
        nuevos: PrintAttributes?,
        senal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: android.os.Bundle?,
    ) = base.onLayout(anteriores, nuevos, senal, callback, extras)

    override fun onWrite(
        paginas: Array<out PageRange>?,
        destino: ParcelFileDescriptor?,
        senal: CancellationSignal?,
        callback: WriteResultCallback?,
    ) = base.onWrite(paginas, destino, senal, callback)

    override fun onFinish() {
        base.onFinish()
        alTerminar()
    }
}
