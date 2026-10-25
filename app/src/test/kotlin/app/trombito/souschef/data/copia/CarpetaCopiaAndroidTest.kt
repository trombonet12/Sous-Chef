package app.trombito.souschef.data.copia

import android.content.ContentResolver
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/** La relectura de la copia nueva: cualquier fallo del proveedor es «no se pudo leer», nunca una excepción. */
@RunWith(RobolectricTestRunner::class)
class CarpetaCopiaAndroidTest {
    private fun carpetaCon(fallo: () -> Throwable): CarpetaCopiaAndroid {
        val resolver = mockk<ContentResolver>()
        every { resolver.openInputStream(any()) } answers { throw fallo() }
        val contexto = mockk<Context>()
        every { contexto.contentResolver } returns resolver
        return CarpetaCopiaAndroid(contexto)
    }

    @Test
    fun unProveedorQueNoEntiendeLaUriDevuelveNulo() =
        runTest {
            assertNull(carpetaCon { IllegalArgumentException("uri") }.leer("content://carpeta/doc"))
        }

    @Test
    fun unProveedorSinLecturaDevuelveNulo() =
        runTest {
            assertNull(carpetaCon { UnsupportedOperationException("sin lectura") }.leer("content://carpeta/doc"))
        }

    @Test
    fun lasExcepcionesYaContempladasSiguenDevolviendoNulo() =
        runTest {
            assertNull(carpetaCon { IOException("e/s") }.leer("content://carpeta/doc"))
            assertNull(carpetaCon { SecurityException("permiso") }.leer("content://carpeta/doc"))
        }
}
