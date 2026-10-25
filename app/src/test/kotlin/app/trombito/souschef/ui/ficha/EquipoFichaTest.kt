package app.trombito.souschef.ui.ficha

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Equipo
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tags
import org.junit.Assert.assertEquals
import org.junit.Test

class EquipoFichaTest {
    private fun receta(
        horno: Boolean,
        equipo: List<Equipo>?,
    ): Receta {
        val hornos = if (horno) listOf(Carril("h", tipo = "horno")) else emptyList()
        val carriles = listOf(Carril("f1", tipo = "fuego")) + hornos
        return Receta(
            slug = "r",
            titulo = "R",
            totalMin = 30.0,
            tags = Tags(equipo = equipo),
            carriles = carriles,
            pasos = listOf(Paso("p1", "Hacer", "f1", 0.0, 10.0)),
        )
    }

    @Test
    fun `una receta sin aparatos no pide nada`() {
        assertEquals(emptyList<AparatoPedido>(), aparatosPedidos(receta(false, null), emptySet()))
    }

    @Test
    fun `pide el horno de los carriles y lo declarado, en el orden del enum`() {
        val r = receta(true, listOf(Equipo.PLANCHA, Equipo.BATIDORA))
        val pedidos = aparatosPedidos(r, Equipo.entries.toSet())
        assertEquals(listOf(Equipo.HORNO, Equipo.BATIDORA, Equipo.PLANCHA), pedidos.map { it.aparato })
        assertEquals(listOf(false, false, false), pedidos.map { it.falta })
    }

    @Test
    fun `marca como falta lo que el usuario no tiene`() {
        val r = receta(true, listOf(Equipo.MICROONDAS))
        val pedidos = aparatosPedidos(r, setOf(Equipo.HORNO))
        assertEquals(
            listOf(AparatoPedido(Equipo.HORNO, false), AparatoPedido(Equipo.MICROONDAS, true)),
            pedidos,
        )
    }

    @Test
    fun `el estado de la ficha tiene todo el equipo por defecto`() {
        assertEquals(Equipo.entries.toSet(), FichaUiState().equipo)
    }
}
