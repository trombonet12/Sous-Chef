package app.trombito.souschef.data.copia

import app.trombito.souschef.domain.copia.ArchivoCopia
import java.io.ByteArrayOutputStream
import java.io.InputStream

private const val BUFFER = 8192

/** Lee como mucho [ArchivoCopia.MAX_BYTES]; si hay más, null (una copia nuestra nunca pasa de ahí). */
internal fun leerAcotado(entrada: InputStream): ByteArray? {
    val salida = ByteArrayOutputStream()
    val buffer = ByteArray(BUFFER)
    var total = 0
    while (true) {
        val n = entrada.read(buffer)
        if (n < 0) break
        total += n
        if (total > ArchivoCopia.MAX_BYTES) return null
        salida.write(buffer, 0, n)
    }
    return salida.toByteArray()
}
