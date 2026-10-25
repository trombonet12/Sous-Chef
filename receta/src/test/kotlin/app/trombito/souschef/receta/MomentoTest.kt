package app.trombito.souschef.receta

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MomentoTest {
    @Test
    fun recetaSinMomentoSigueLeyendose() {
        val r = leerReceta("""{"slug":"r","titulo":"R","tags":{"base":"arroz"}}""")
        assertNull(r.tags.momento)
    }

    @Test
    fun momentoSeLeeEnMinusculas() {
        val r = leerReceta("""{"slug":"r","titulo":"R","tags":{"momento":["comida","cena","postre"]}}""")
        assertEquals(listOf(Momento.COMIDA, Momento.CENA, Momento.POSTRE), r.tags.momento)
    }

    @Test
    fun momentoSeEscribeEnMinusculasYNuloSeOmite() {
        val receta = Receta("r", "R", tags = Tags(momento = listOf(Momento.MERIENDA)))
        val con = RecetaJson.encodeToString(Receta.serializer(), receta)
        assertTrue("\"momento\":[\"merienda\"]" in con, con)
        val sin = RecetaJson.encodeToString(Receta.serializer(), Receta("r", "R"))
        assertTrue("momento" !in sin, sin)
    }

    @Test
    fun formatoCanonicoOrdenaLasClavesDeTags() {
        val f = JsonCanonico.formatear("""{"tags":{"proteina":"x","momento":["cena"],"base":"y"}}""")
        val orden = listOf("base", "momento", "proteina").map { f.indexOf("\"$it\"") }
        assertEquals(orden.sorted(), orden, f)
    }
}
