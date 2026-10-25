package app.trombito.souschef.data.copia

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import app.trombito.souschef.domain.copia.ArchivoCopia
import app.trombito.souschef.domain.copia.ArchivoDemasiadoGrande
import app.trombito.souschef.domain.copia.EscrituraSegura
import app.trombito.souschef.domain.copia.ResultadoEscritura
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

/**
 * Lee y escribe la copia en el archivo que el usuario elige con el selector del sistema (Storage Access Framework):
 * vive fuera de los datos de la app, así que sobrevive a «borrar datos» y a reinstalar. Sin permisos nuevos.
 */
class ArchivoCopiaAndroid
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ArchivoCopia {
        override suspend fun escribir(
            destino: String,
            texto: String,
        ): Boolean = escribirSeguro(destino, texto.toByteArray(Charsets.UTF_8)) { true } == ResultadoEscritura.Escrita

        /**
         * El modo `"wt"` vacía el archivo al abrirlo, así que antes se valida el contenido entero en memoria
         * ([EscrituraSegura]): un contenido inválido deja intacta la copia buena anterior.
         */
        override suspend fun escribirSeguro(
            destino: String,
            bytes: ByteArray,
            valida: (ByteArray) -> Boolean,
        ): ResultadoEscritura =
            withContext(Dispatchers.IO) {
                EscrituraSegura
                    .escribir(bytes, valida) { context.contentResolver.openOutputStream(destino.toUri(), "wt") }
                    .also { if (it != ResultadoEscritura.Escrita) Log.w(TAG, "Copia no guardada: $it") }
            }

        override suspend fun leer(origen: String): String? =
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(origen.toUri())?.use { leerAcotado(it) }
                } catch (e: IOException) {
                    Log.w(TAG, "No se pudo leer la copia: ${e.javaClass.simpleName}")
                    null
                } catch (e: SecurityException) {
                    Log.w(TAG, "Sin permiso para leer la copia: ${e.javaClass.simpleName}")
                    null
                }
            }

        /** Lee como mucho [ArchivoCopia.MAX_BYTES]; si hay más, [ArchivoDemasiadoGrande] sin cargarlo entero. */
        private fun leerAcotado(entrada: InputStream): String {
            val salida = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER)
            var total = 0
            while (true) {
                val n = entrada.read(buffer)
                if (n < 0) break
                total += n
                if (total > ArchivoCopia.MAX_BYTES) throw ArchivoDemasiadoGrande()
                salida.write(buffer, 0, n)
            }
            return salida.toString(Charsets.UTF_8.name())
        }

        private companion object {
            const val TAG = "SousChef"
            const val BUFFER = 8192
        }
    }
