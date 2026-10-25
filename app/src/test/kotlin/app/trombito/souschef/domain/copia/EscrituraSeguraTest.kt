package app.trombito.souschef.domain.copia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

class EscrituraSeguraTest {
    /** Un «archivo» cuyo modo de apertura es truncar, como `"wt"`: abrir lo vacía. */
    private class ArchivoTruncable(
        var contenido: ByteArray,
    ) {
        var aperturas = 0

        fun salida() =
            SalidaCopia {
                aperturas++
                contenido = byteArrayOf()
                object : ByteArrayOutputStream() {
                    override fun close() {
                        contenido = toByteArray()
                        super.close()
                    }
                }
            }
    }

    private val bien = "copia buena".toByteArray()
    private val nueva = "copia nueva".toByteArray()

    @Test
    fun contenidoInvalidoNoAbreNiTocaElDestino() {
        val archivo = ArchivoTruncable(bien)
        val r = EscrituraSegura.escribir(nueva, { false }, archivo.salida())
        assertEquals(ResultadoEscritura.Invalida, r)
        assertEquals(0, archivo.aperturas)
        assertEquals("copia buena", String(archivo.contenido))
    }

    @Test
    fun siLaValidacionLanzaCuentaComoInvalida() {
        val archivo = ArchivoTruncable(bien)
        val r = EscrituraSegura.escribir(nueva, { error("parser roto") }, archivo.salida())
        assertEquals(ResultadoEscritura.Invalida, r)
        assertEquals(0, archivo.aperturas)
        assertEquals("copia buena", String(archivo.contenido))
    }

    @Test
    fun contenidoValidoSustituyeAlAnterior() {
        val archivo = ArchivoTruncable(bien)
        val r = EscrituraSegura.escribir(nueva, { it.isNotEmpty() }, archivo.salida())
        assertEquals(ResultadoEscritura.Escrita, r)
        assertEquals("copia nueva", String(archivo.contenido))
    }

    @Test
    fun laValidacionRecibeLosBytesQueSeVanAEscribir() {
        var vistos: ByteArray? = null
        val guarda: (ByteArray) -> Boolean = {
            vistos = it.copyOf()
            true
        }
        EscrituraSegura.escribir(nueva, guarda, ArchivoTruncable(bien).salida())
        assertTrue(nueva.contentEquals(vistos))
    }

    @Test
    fun demasiadoGrandeNoValidaNiAbre() {
        val archivo = ArchivoTruncable(bien)
        var validada = false
        val enorme = ByteArray(ArchivoCopia.MAX_BYTES + 1)
        val valida: (ByteArray) -> Boolean = {
            validada = true
            true
        }
        val r = EscrituraSegura.escribir(enorme, valida, archivo.salida())
        assertEquals(ResultadoEscritura.Grande, r)
        assertFalse(validada)
        assertEquals(0, archivo.aperturas)
    }

    @Test
    fun noPoderAbrirEsFallo() {
        assertEquals(ResultadoEscritura.Fallo, EscrituraSegura.escribir(nueva, { true }, { null }))
    }

    @Test
    fun errorDeEscrituraEsFalloTipado() {
        val rota =
            SalidaCopia {
                object : OutputStream() {
                    override fun write(b: Int) = throw IOException("disco lleno")
                }
            }
        assertEquals(ResultadoEscritura.Fallo, EscrituraSegura.escribir(nueva, { true }, rota))
    }

    @Test
    fun uriRechazadoOModoNoSoportadoEsFalloTipadoNoExcepcion() {
        val malUri = SalidaCopia { throw IllegalArgumentException("uri mal formado") }
        val sinModo = SalidaCopia { throw UnsupportedOperationException("modo wt no soportado") }
        assertEquals(ResultadoEscritura.Fallo, EscrituraSegura.escribir(nueva, { true }, malUri))
        assertEquals(ResultadoEscritura.Fallo, EscrituraSegura.escribir(nueva, { true }, sinModo))
    }

    @Test
    fun permisoRetiradoEsSinPermiso() {
        val sinPermiso = SalidaCopia { throw SecurityException("permiso retirado") }
        assertEquals(ResultadoEscritura.SinPermiso, EscrituraSegura.escribir(nueva, { true }, sinPermiso))
    }
}
