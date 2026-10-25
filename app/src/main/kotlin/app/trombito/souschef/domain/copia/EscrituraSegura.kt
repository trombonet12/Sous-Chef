package app.trombito.souschef.domain.copia

import java.io.IOException
import java.io.OutputStream

/** Resultado tipado de escribir una copia: nunca un fallo silencioso. */
sealed interface ResultadoEscritura {
    /** Escrita entera y cerrada. */
    data object Escrita : ResultadoEscritura

    /** El contenido no pasó la validación previa: no se ha abierto ni tocado el destino. */
    data object Invalida : ResultadoEscritura

    /** El contenido pasa de [ArchivoCopia.MAX_BYTES]: no se ha tocado el destino. */
    data object Grande : ResultadoEscritura

    /** El permiso del destino se retiró (SAF) entre elegirlo y escribirlo. */
    data object SinPermiso : ResultadoEscritura

    /** No se pudo abrir o escribir el destino. */
    data object Fallo : ResultadoEscritura
}

/** Cómo abrir el destino para escribir (reemplazando su contenido); null si no se puede abrir. */
fun interface SalidaCopia {
    fun abrir(): OutputStream?
}

/**
 * Escritura de una copia sin destruir la buena anterior: abrir el destino en modo «truncar» (`"wt"`) vacía el
 * archivo, así que el contenido se valida entero en memoria ANTES de abrirlo. Sirve igual para un documento ya
 * elegido y para uno nuevo dentro de un árbol SAF (el [SalidaCopia] lo crea al abrir, y solo si la validación pasó).
 */
object EscrituraSegura {
    /**
     * Bloqueante: llamar desde un hilo de IO. [valida] decide si [bytes] es una copia que se pueda volver a leer.
     * Orden: tamaño, validación, y solo entonces [salida].
     */
    fun escribir(
        bytes: ByteArray,
        valida: (ByteArray) -> Boolean,
        salida: SalidaCopia,
    ): ResultadoEscritura =
        when {
            bytes.size > ArchivoCopia.MAX_BYTES -> ResultadoEscritura.Grande
            !runCatching { valida(bytes) }.getOrDefault(false) -> ResultadoEscritura.Invalida
            else -> volcar(bytes, salida)
        }

    private fun volcar(
        bytes: ByteArray,
        salida: SalidaCopia,
    ): ResultadoEscritura =
        try {
            val flujo = salida.abrir()
            if (flujo == null) {
                ResultadoEscritura.Fallo
            } else {
                flujo.use {
                    it.write(bytes)
                    it.flush()
                }
                ResultadoEscritura.Escrita
            }
        } catch (_: IOException) {
            ResultadoEscritura.Fallo
        } catch (_: SecurityException) {
            ResultadoEscritura.SinPermiso
        } catch (_: IllegalArgumentException) {
            // El proveedor SAF rechaza un URI mal formado o ya revocado.
            ResultadoEscritura.Fallo
        } catch (_: UnsupportedOperationException) {
            // El proveedor no admite abrir el documento en modo escritura.
            ResultadoEscritura.Fallo
        }
}
