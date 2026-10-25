package app.trombito.souschef.data.copia

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import app.trombito.souschef.domain.copia.ArchivoEnCarpeta
import app.trombito.souschef.domain.copia.CarpetaCopia
import app.trombito.souschef.domain.copia.EscrituraSegura
import app.trombito.souschef.domain.copia.ResultadoCreacion
import app.trombito.souschef.domain.copia.ResultadoEscritura
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject

/**
 * La carpeta de la copia automática (ADR-0015), elegida con `OpenDocumentTree`: documentos nuevos con
 * `DocumentsContract.createDocument`, sin permisos de almacenamiento. Ni la URI ni los nombres llegan al log: solo la
 * clase de la excepción.
 */
class CarpetaCopiaAndroid
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : CarpetaCopia {
        private val resolver get() = context.contentResolver

        override suspend fun adoptar(carpeta: String): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    resolver.takePersistableUriPermission(carpeta.toUri(), PERMISOS)
                    true
                } catch (e: SecurityException) {
                    Log.w(TAG, "No se pudo guardar el permiso de la carpeta: ${e.javaClass.simpleName}")
                    false
                }
            }

        override suspend fun soltar(carpeta: String) {
            withContext(Dispatchers.IO) {
                try {
                    resolver.releasePersistableUriPermission(carpeta.toUri(), PERMISOS)
                } catch (e: SecurityException) {
                    Log.w(TAG, "No se pudo soltar el permiso de la carpeta: ${e.javaClass.simpleName}")
                }
            }
        }

        override suspend fun crear(
            carpeta: String,
            nombre: String,
            bytes: ByteArray,
            valida: (ByteArray) -> Boolean,
        ): ResultadoCreacion =
            withContext(Dispatchers.IO) {
                val raiz = raizValida(carpeta)
                var creado: Uri? = null
                // El documento se crea al abrir la salida, y EscrituraSegura solo la abre si la copia ya se validó.
                val resultado =
                    if (raiz == null) {
                        null
                    } else {
                        EscrituraSegura.escribir(bytes, valida) {
                            creado = DocumentsContract.createDocument(resolver, raiz, MIME_JSON, nombre)
                            creado?.let(::abrirSalida)
                        }
                    }
                val documento = creado
                when {
                    resultado == null -> {
                        ResultadoCreacion.CarpetaNoVale
                    }

                    resultado == ResultadoEscritura.Escrita && documento != null -> {
                        ResultadoCreacion.Creado(documento.toString())
                    }

                    else -> {
                        if (documento != null) borrarUri(documento)
                        Log.w(TAG, "Copia automática no escrita: $resultado")
                        ResultadoCreacion.Rechazada(
                            if (resultado == ResultadoEscritura.Escrita) ResultadoEscritura.Fallo else resultado,
                        )
                    }
                }
            }

        override suspend fun leer(documento: String): ByteArray? =
            withContext(Dispatchers.IO) {
                try {
                    resolver.openInputStream(documento.toUri())?.use { leerAcotado(it) }
                } catch (e: IOException) {
                    Log.w(TAG, "No se pudo releer la copia: ${e.javaClass.simpleName}")
                    null
                } catch (e: SecurityException) {
                    Log.w(TAG, "Sin permiso para releer la copia: ${e.javaClass.simpleName}")
                    null
                } catch (e: IllegalArgumentException) {
                    // Un proveedor que no entiende la URI: igual que no poder leer, la copia nueva se descarta.
                    Log.w(TAG, "No se pudo releer la copia: ${e.javaClass.simpleName}")
                    null
                } catch (e: UnsupportedOperationException) {
                    Log.w(TAG, "No se pudo releer la copia: ${e.javaClass.simpleName}")
                    null
                }
            }

        override suspend fun listar(carpeta: String): List<ArchivoEnCarpeta>? =
            withContext(Dispatchers.IO) {
                try {
                    val arbol = carpeta.toUri()
                    val idArbol = DocumentsContract.getTreeDocumentId(arbol)
                    val hijos = DocumentsContract.buildChildDocumentsUriUsingTree(arbol, idArbol)
                    val columnas =
                        arrayOf(
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_MIME_TYPE,
                        )
                    resolver.query(hijos, columnas, null, null, null)?.use { cursor ->
                        buildList {
                            while (cursor.moveToNext()) {
                                val id = cursor.getString(0) ?: continue
                                val uri = DocumentsContract.buildDocumentUriUsingTree(arbol, id)
                                val carpeta = cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                                add(ArchivoEnCarpeta(uri.toString(), cursor.getString(1).orEmpty(), carpeta))
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    Log.w(TAG, "No se pudo listar la carpeta: ${e.javaClass.simpleName}")
                    null
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "No se pudo listar la carpeta: ${e.javaClass.simpleName}")
                    null
                }
            }

        override suspend fun borrar(documento: String): Boolean =
            withContext(Dispatchers.IO) { borrarUri(documento.toUri()) }

        private fun borrarUri(documento: Uri): Boolean =
            try {
                DocumentsContract.deleteDocument(resolver, documento)
            } catch (e: IOException) {
                Log.w(TAG, "No se pudo borrar un documento: ${e.javaClass.simpleName}")
                false
            } catch (e: SecurityException) {
                Log.w(TAG, "Sin permiso para borrar un documento: ${e.javaClass.simpleName}")
                false
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "No se pudo borrar un documento: ${e.javaClass.simpleName}")
                false
            }

        /**
         * El documento raíz de [carpeta] si todavía vale: el permiso persistente de lectura y escritura sigue ahí y
         * la raíz existe y es una carpeta. null si no (permiso retirado, carpeta borrada, proveedor desinstalado).
         */
        private fun raizValida(carpeta: String): Uri? =
            try {
                val arbol = carpeta.toUri()
                val concedido =
                    resolver.persistedUriPermissions.any {
                        it.uri == arbol && it.isReadPermission && it.isWritePermission
                    }
                val idArbol = DocumentsContract.getTreeDocumentId(arbol)
                DocumentsContract.buildDocumentUriUsingTree(arbol, idArbol).takeIf { concedido && esCarpeta(it) }
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "La carpeta no es un árbol de documentos: ${e.javaClass.simpleName}")
                null
            }

        private fun esCarpeta(documento: Uri): Boolean =
            try {
                val columnas = arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE)
                resolver.query(documento, columnas, null, null, null)?.use {
                    it.moveToFirst() && it.getString(0) == DocumentsContract.Document.MIME_TYPE_DIR
                } ?: false
            } catch (e: SecurityException) {
                Log.w(TAG, "Sin acceso a la carpeta: ${e.javaClass.simpleName}")
                false
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Carpeta no consultable: ${e.javaClass.simpleName}")
                false
            }

        /** Con descriptor de archivo (para `sync`) si el proveedor lo da; si no, el flujo normal. */
        private fun abrirSalida(documento: Uri): OutputStream? {
            val descriptor =
                try {
                    resolver.openFileDescriptor(documento, "w")
                } catch (_: IOException) {
                    null
                } catch (_: UnsupportedOperationException) {
                    null
                }
            return if (descriptor != null) FlujoSincronizado(descriptor) else resolver.openOutputStream(documento, "w")
        }

        private companion object {
            const val TAG = "SousChef"
            const val MIME_JSON = "application/json"
            const val PERMISOS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
    }
