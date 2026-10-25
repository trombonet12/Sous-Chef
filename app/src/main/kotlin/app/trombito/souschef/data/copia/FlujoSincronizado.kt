package app.trombito.souschef.data.copia

import android.os.ParcelFileDescriptor
import java.io.IOException
import java.io.OutputStream

/**
 * Salida a un documento SAF que, al cerrar, hace `flush` y `FileDescriptor.sync()`. Si el proveedor no da un
 * descriptor sincronizable (una tubería, una nube) el `sync` falla y se sigue (ADR-0015); un error al vaciar o al
 * cerrar sí se propaga, y la escritura cuenta como fallida.
 */
internal class FlujoSincronizado(
    private val descriptor: ParcelFileDescriptor,
) : OutputStream() {
    private val flujo = ParcelFileDescriptor.AutoCloseOutputStream(descriptor)

    override fun write(b: Int) = flujo.write(b)

    override fun write(
        b: ByteArray,
        off: Int,
        len: Int,
    ) = flujo.write(b, off, len)

    override fun flush() = flujo.flush()

    override fun close() {
        try {
            flujo.flush()
            sincronizar()
        } finally {
            flujo.close()
        }
    }

    private fun sincronizar() {
        try {
            descriptor.fileDescriptor.sync()
        } catch (_: IOException) {
            // El proveedor no admite sync (SyncFailedException es una IOException): la copia se relee después.
        }
    }
}
