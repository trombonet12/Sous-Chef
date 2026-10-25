package app.trombito.souschef.receta

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EquipoTest {
    @Test
    fun recetaSinEquipoSigueLeyendose() {
        val r = leerReceta("""{"slug":"r","titulo":"R","tags":{"base":"arroz"}}""")
        assertNull(r.tags.equipo)
        assertEquals(emptySet(), equipoDe(r))
    }

    @Test
    fun equipoSeLeeEnMinusculas() {
        val r = leerReceta("""{"slug":"r","titulo":"R","tags":{"equipo":["batidora","microondas","plancha"]}}""")
        assertEquals(listOf(Equipo.BATIDORA, Equipo.MICROONDAS, Equipo.PLANCHA), r.tags.equipo)
    }

    @Test
    fun equipoSeEscribeEnMinusculasYNuloSeOmite() {
        val receta = Receta("r", "R", tags = Tags(equipo = listOf(Equipo.BATIDORA)))
        val con = RecetaJson.encodeToString(Receta.serializer(), receta)
        assertTrue("\"equipo\":[\"batidora\"]" in con, con)
        val sin = RecetaJson.encodeToString(Receta.serializer(), Receta("r", "R"))
        assertTrue("equipo" !in sin, sin)
    }

    @Test
    fun elHornoSeDerivaDeLosCarriles() {
        val r = Receta("r", "R", carriles = listOf(Carril("a", tipo = "fuego"), Carril("h", tipo = "horno")))
        assertEquals(setOf(Equipo.HORNO), equipoDe(r))
    }

    @Test
    fun equipoDeJuntaLoDerivadoYLoDeclarado() {
        val r =
            Receta(
                "r",
                "R",
                tags = Tags(equipo = listOf(Equipo.PLANCHA, Equipo.BATIDORA)),
                carriles = listOf(Carril("h", tipo = "horno")),
            )
        assertEquals(setOf(Equipo.HORNO, Equipo.PLANCHA, Equipo.BATIDORA), equipoDe(r))
    }

    @Test
    fun hornoDeclaradoYDerivadoNoSeDuplica() {
        val r =
            Receta("r", "R", tags = Tags(equipo = listOf(Equipo.HORNO)), carriles = listOf(Carril("h", tipo = "horno")))
        assertEquals(setOf(Equipo.HORNO), equipoDe(r))
    }

    @Test
    fun formatoCanonicoOrdenaLasClavesDeTags() {
        val f = JsonCanonico.formatear("""{"tags":{"proteina":"x","equipo":["batidora"],"base":"y"}}""")
        val orden = listOf("base", "equipo", "proteina").map { f.indexOf("\"$it\"") }
        assertEquals(orden.sorted(), orden, f)
    }
}
