package app.souschef.ui.ficha

import android.content.Context
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

/** WebViews con una impresión en curso: la referencia fuerte evita que se recojan antes de terminar. */
private val enCurso = mutableSetOf<WebView>()

/** Abre el diálogo de impresión del sistema (también "Guardar como PDF") con la ficha en [html]. */
fun imprimirFicha(
    context: Context,
    nombre: String,
    html: String,
) {
    val app = context.applicationContext
    val impresion = app.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val web = WebView(app)
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
