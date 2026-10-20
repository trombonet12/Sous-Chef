package app.trombito.souschef.domain.actualizacion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionNuevaTest {
    @Test
    fun `un tag mayor es mas nuevo aunque lleve v delante`() {
        assertTrue(VersionNueva.esMasNueva("v0.7.3", "0.7.2"))
        assertTrue(VersionNueva.esMasNueva("v0.8.0", "0.7.9"))
        assertTrue(VersionNueva.esMasNueva("v1.0.0", "0.9.9"))
    }

    @Test
    fun `compara por numeros, no por texto`() {
        assertTrue(VersionNueva.esMasNueva("v0.10.0", "0.9.0"))
        assertFalse(VersionNueva.esMasNueva("v0.9.0", "0.10.0"))
    }

    @Test
    fun `la misma version o una anterior no es nueva`() {
        assertFalse(VersionNueva.esMasNueva("v0.7.2", "0.7.2"))
        assertFalse(VersionNueva.esMasNueva("v0.7.1", "0.7.2"))
    }

    @Test
    fun `faltan componentes cuentan como cero`() {
        assertFalse(VersionNueva.esMasNueva("v0.8", "0.8.0"))
        assertTrue(VersionNueva.esMasNueva("v0.8.1", "0.8"))
    }

    @Test
    fun `un tag ilegible nunca avisa`() {
        assertFalse(VersionNueva.esMasNueva("nightly", "0.7.2"))
        assertFalse(VersionNueva.esMasNueva("", "0.7.2"))
    }

    @Test
    fun `lee tag y url de la respuesta de la API`() {
        val json =
            """{"url":"x","html_url":"https://github.com/trombonet12/Sous-Chef/releases/tag/v0.7.3",""" +
                """"id":1,"tag_name": "v0.7.3","name":"Sous Chef 0.7.3","assets":[{"name":"a.apk"}]}"""
        val r = VersionNueva.leerRelease(json)
        assertEquals("v0.7.3", r?.tag)
        assertEquals("https://github.com/trombonet12/Sous-Chef/releases/tag/v0.7.3", r?.url)
    }

    @Test
    fun `una respuesta sin tag no es una release`() {
        assertNull(VersionNueva.leerRelease("""{"message":"Not Found"}"""))
    }

    @Test
    fun `avisa una sola vez por version`() {
        assertTrue(VersionNueva.debeAvisar("v0.7.3", instalada = "0.7.2", yaAvisada = null))
        assertTrue(VersionNueva.debeAvisar("v0.7.4", instalada = "0.7.2", yaAvisada = "v0.7.3"))
        assertFalse(VersionNueva.debeAvisar("v0.7.3", instalada = "0.7.2", yaAvisada = "v0.7.3"))
        assertFalse(VersionNueva.debeAvisar("v0.7.2", instalada = "0.7.2", yaAvisada = null))
    }

    @Test
    fun `desde Google Play no se avisa porque actualiza la tienda`() {
        assertTrue(VersionNueva.actualizaLaTienda("com.android.vending"))
        assertFalse(VersionNueva.actualizaLaTienda(null))
        assertFalse(VersionNueva.actualizaLaTienda("dev.imranr.obtainium"))
        assertFalse(VersionNueva.actualizaLaTienda("com.google.android.packageinstaller"))
    }

    @Test
    fun `al abrir solo comprueba si pasaron 6 horas`() {
        val seisHoras = 6 * 3_600_000L
        assertTrue(VersionNueva.tocaComprobar(ahoraMs = seisHoras, ultimaMs = 0L))
        assertFalse(VersionNueva.tocaComprobar(ahoraMs = seisHoras - 1, ultimaMs = 0L))
        assertTrue(VersionNueva.tocaComprobar(ahoraMs = 1_000L, ultimaMs = 5_000L))
    }
}
