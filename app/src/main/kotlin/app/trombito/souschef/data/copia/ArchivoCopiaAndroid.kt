package app.trombito.souschef.data.copia

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import app.trombito.souschef.domain.copia.ArchivoCopia
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
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
        ): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    val salida = context.contentResolver.openOutputStream(destino.toUri(), "wt")
                    salida?.bufferedWriter(Charsets.UTF_8)?.use { it.write(texto) }
                    salida != null
                } catch (e: IOException) {
                    Log.w(TAG, "No se pudo guardar la copia", e)
                    false
                } catch (e: SecurityException) {
                    // Permiso del archivo retirado entre elegirlo y escribirlo.
                    Log.w(TAG, "Sin permiso para guardar la copia", e)
                    false
                }
            }

        override suspend fun leer(origen: String): String? =
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(origen.toUri())?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    }
                }.getOrNull()
            }

        private companion object {
            const val TAG = "SousChef"
        }
    }
