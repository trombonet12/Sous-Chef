package app.trombito.souschef.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReglaNovedadesTest {
    private val todas = listOf(novedad("1.6"), novedad("1.5"), novedad("1.4"))

    @Suppress("LongParameterList")
    private fun pendientes(
        actual: String = "1.6.0",
        vista: String? = "1.5",
        introVista: Boolean = true,
        hayViva: Boolean = false,
        entradaDirecta: Boolean = false,
        playPendiente: Boolean = false,
        lista: List<Novedades> = todas,
    ) = novedadesPendientes(actual, vista, introVista, hayViva, entradaDirecta, playPendiente, lista)

    private fun versiones(r: List<Novedades>?) = r?.map { it.version }

    @Test
    fun `instalacion nueva no ensena nada`() {
        assertNull(pendientes(vista = null, introVista = false))
    }

    @Test
    fun `de 1_5 a 1_6 sale solo la 1_6`() {
        assertEquals(listOf("1.6"), versiones(pendientes(vista = "1.5")))
    }

    @Test
    fun `de 1_4 a 1_6 salen la 1_6 y la 1_5 en ese orden`() {
        assertEquals(listOf("1.6", "1.5"), versiones(pendientes(vista = "1.4")))
    }

    @Test
    fun `el orden de entrada no importa`() {
        val r = pendientes(vista = "1.3", lista = listOf(novedad("1.4"), novedad("1.6"), novedad("1.5")))

        assertEquals(listOf("1.6", "1.5", "1.4"), versiones(r))
    }

    @Test
    fun `misma version no sale`() {
        assertNull(pendientes(vista = "1.6"))
    }

    @Test
    fun `un parche no saca novedades`() {
        assertNull(pendientes(actual = "1.6.3", vista = "1.6", lista = listOf(novedad("1.6"))))
    }

    @Test
    fun `un parche de una version nueva si la saca`() {
        assertEquals(listOf("1.6"), versiones(pendientes(actual = "1.6.2", vista = "1.5")))
    }

    @Test
    fun `con una cocinada viva no sale`() {
        assertNull(pendientes(hayViva = true))
    }

    @Test
    fun `con entrada directa no sale`() {
        assertNull(pendientes(entradaDirecta = true))
    }

    @Test
    fun `con la actualizacion de Play pendiente no sale`() {
        assertNull(pendientes(playPendiente = true))
    }

    @Test
    fun `sin marca y con la intro vista salen todas las hasta la actual`() {
        val r = pendientes(vista = null, lista = todas + novedad("1.7"))

        assertEquals(listOf("1.6", "1.5", "1.4"), versiones(r))
    }

    @Test
    fun `marca vacia o corrupta cuenta como sin marca`() {
        assertEquals(listOf("1.6", "1.5", "1.4"), versiones(pendientes(vista = "")))
        assertEquals(listOf("1.6", "1.5", "1.4"), versiones(pendientes(vista = "basura")))
    }

    @Test
    fun `1_10 es mayor que 1_9`() {
        val lista = listOf(novedad("1.10"), novedad("1.9"), novedad("1.2"))

        assertEquals(listOf("1.10"), versiones(pendientes(actual = "1.10.0", vista = "1.9", lista = lista)))
        assertNull(pendientes(actual = "1.9.0", vista = "1.10", lista = lista))
        assertEquals(listOf("1.9", "1.2"), versiones(pendientes(actual = "1.9.1", vista = null, lista = lista)))
    }

    @Test
    fun `una version bajada no ensena nada`() {
        assertNull(pendientes(actual = "1.5.0", vista = "1.6"))
    }

    @Test
    fun `sin entradas aplicables o con version ilegible devuelve null`() {
        assertNull(pendientes(vista = "1.5", lista = listOf(novedad("1.4"))))
        assertNull(pendientes(actual = "", vista = "1.5"))
        assertNull(pendientes(actual = "x.y", vista = "1.5"))
    }

    @Test
    fun `claveNovedades se queda con major_minor`() {
        assertEquals("1.6", claveNovedades("1.6.2-debug"))
        assertEquals("1.10", claveNovedades("1.10"))
        assertNull(claveNovedades(""))
        assertNull(claveNovedades("abc"))
    }
}
