package app.trombito.souschef.tools

import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HistorialTest {
    private val lentejas =
        Receta(
            slug = "lentejas",
            titulo = "Lentejas con verdura",
            pasos =
                listOf(
                    Paso(id = "a", t = "Sudar la verdura al {5}", carril = "f1", off = 0.0, min = 10.0),
                    Paso(id = "b", t = "Cocer las lentejas", carril = "f2", off = 0.0, min = 20.0),
                ),
        )
    private val recetas = mapOf("lentejas" to lentejas)

    private fun entrada(
        id: String,
        slug: String,
        pasos: String,
    ) = """{"id":"$id","slug":"$slug","titulo":"x","fecha":"2026-10-01","pasosReales":[$pasos]}"""

    private fun copia(vararg entradas: String) =
        """{"formato":"sous-chef-historial","version":1,"historial":[${entradas.joinToString(",")}]}"""

    private fun paso(
        id: String,
        s: Int,
    ) = """{"id":"$id","t":"x","s":$s}"""

    @Test
    fun listaPasosPorMedianaDeDesvioDeMayorAMenor() {
        val json =
            copia(
                entrada("1", "lentejas", paso("a", 780) + "," + paso("b", 1200)),
                entrada("2", "lentejas", paso("a", 840) + "," + paso("b", 1260)),
                entrada("3", "lentejas", paso("a", 600) + "," + paso("b", 1140)),
            )
        // a: +180, +240, 0 -> mediana +180 s; b: 0, +60, -60 -> mediana 0
        val esperado =
            listOf(
                "lentejas · Lentejas con verdura · 3 cocinadas",
                "  +3 min · 3× · Sudar la verdura al {5}",
                "  0 s · 3× · Cocer las lentejas",
            )
        assertEquals(esperado, informeHistorial(json, recetas))
    }

    @Test
    fun medianaDeCantidadParEsLaMediaDeLosDosCentrales() {
        val json =
            copia(
                entrada("1", "lentejas", paso("a", 660)),
                entrada("2", "lentejas", paso("a", 900)),
            )
        // deltas +60 y +300 -> mediana +180 s
        assertEquals("  +3 min · 2× · Sudar la verdura al {5}", informeHistorial(json, recetas)[1])
    }

    @Test
    fun cuentaPorPasoSoloLasCocinadasQueLoMidieron() {
        val json =
            copia(
                entrada("1", "lentejas", paso("a", 660)),
                entrada("2", "lentejas", paso("b", 1200)),
            )
        val lineas = informeHistorial(json, recetas)
        assertEquals("lentejas · Lentejas con verdura · 2 cocinadas", lineas[0])
        assertEquals("  +1 min · 1× · Sudar la verdura al {5}", lineas[1])
        assertEquals("  0 s · 1× · Cocer las lentejas", lineas[2])
    }

    @Test
    fun filtraPorSlugEIndicaLasRecetasDesconocidas() {
        val json =
            copia(
                entrada("1", "lentejas", paso("a", 660)),
                entrada("2", "fantasma", paso("a", 660)),
            )
        assertEquals(2, informeHistorial(json, recetas, "lentejas").size)
        assertEquals(emptyList(), informeHistorial(json, recetas, "otra"))
        assertEquals(
            listOf("fantasma · sin receta en data/recetas · 1 cocinada"),
            informeHistorial(json, recetas, "fantasma"),
        )
    }

    @Test
    fun cocinadaSinTiemposPorPasoSeIndica() {
        val json = copia(entrada("1", "lentejas", ""))
        assertEquals(
            listOf("lentejas · Lentejas con verdura · 1 cocinada", "  sin tiempos por paso"),
            informeHistorial(json, recetas),
        )
    }

    @Test
    fun rechazaArchivosQueNoSonUnaCopia() {
        assertFailsWith<IllegalArgumentException> { informeHistorial("""{"formato":"otro","historial":[]}""", recetas) }
        assertFailsWith<IllegalArgumentException> { informeHistorial("no es json", recetas) }
    }
}
