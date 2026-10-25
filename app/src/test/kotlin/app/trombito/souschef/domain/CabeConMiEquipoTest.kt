package app.trombito.souschef.domain

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Equipo
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CabeConMiEquipoTest {
    private val todo = Equipo.entries.toSet()

    private fun receta(
        horno: Boolean = false,
        declarado: List<Equipo>? = null,
    ): Receta {
        val hornos = if (horno) listOf(Carril("h", tipo = "horno")) else emptyList()
        val carriles = listOf(Carril("f1", tipo = "fuego")) + hornos
        return Receta(
            slug = "r",
            titulo = "R",
            totalMin = 20.0,
            tags = Tags(equipo = declarado),
            carriles = carriles,
            pasos = listOf(Paso("p1", "Hacer", "f1", 0.0, 10.0)),
        )
    }

    @Test
    fun `una receta sin aparatos cabe siempre`() {
        assertTrue(cabeConMiEquipo(receta(), emptySet()))
        assertTrue(cabeConMiEquipo(receta(), todo))
    }

    @Test
    fun `con todo el equipo cabe cualquier receta`() {
        val r = receta(horno = true, declarado = listOf(Equipo.BATIDORA, Equipo.MICROONDAS, Equipo.PLANCHA))
        assertTrue(cabeConMiEquipo(r, todo))
    }

    @Test
    fun `el horno sale de los carriles aunque no se declare`() {
        val r = receta(horno = true)
        assertFalse(cabeConMiEquipo(r, todo - Equipo.HORNO))
        assertTrue(cabeConMiEquipo(r, setOf(Equipo.HORNO)))
    }

    @Test
    fun `no cabe si falta un solo aparato`() {
        val r = receta(declarado = listOf(Equipo.BATIDORA, Equipo.PLANCHA))
        assertFalse(cabeConMiEquipo(r, setOf(Equipo.BATIDORA)))
        assertFalse(cabeConMiEquipo(r, emptySet()))
        assertTrue(cabeConMiEquipo(r, setOf(Equipo.BATIDORA, Equipo.PLANCHA)))
    }

    @Test
    fun `equipoQueFalta devuelve solo lo que pide y no hay`() {
        val r = receta(horno = true, declarado = listOf(Equipo.MICROONDAS, Equipo.PLANCHA))
        assertEquals(setOf(Equipo.HORNO, Equipo.MICROONDAS), equipoQueFalta(r, setOf(Equipo.PLANCHA)))
        assertEquals(emptySet<Equipo>(), equipoQueFalta(r, todo))
    }
}
