package app.trombito.souschef.receta

import kotlin.test.Test
import kotlin.test.assertEquals

class DesvioTest {
    private val receta =
        Receta(
            slug = "lentejas",
            titulo = "Lentejas con verdura",
            raciones = 2,
            totalMin = 30.0,
            pasos =
                listOf(
                    Paso(id = "a", t = "Sudar la verdura al {5}", carril = "f1", off = 0.0, min = 10.0),
                    Paso(id = "b", t = "Cocer las lentejas", carril = "f2", off = 0.0, min = 20.0),
                    Paso(id = "c", t = "Reposar", carril = "mesa", off = 20.0, min = 1.0, s = 90),
                ),
        )

    @Test
    fun desviosCalculaDeltaPorPasoEnOrdenDeReceta() {
        val r = desvios(receta, listOf(PasoMedido("b", 1260), PasoMedido("a", 780)))
        assertEquals(
            listOf(
                DesvioPaso("a", "Sudar la verdura al {5}", 600, 780, 180),
                DesvioPaso("b", "Cocer las lentejas", 1200, 1260, 60),
            ),
            r,
        )
    }

    @Test
    fun desviosIgnoraPasosSinMedirYIdsDesconocidos() {
        val r = desvios(receta, listOf(PasoMedido("zzz", 10), PasoMedido("c", 60)))
        assertEquals(listOf(DesvioPaso("c", "Reposar", 90, 60, -30)), r)
    }

    @Test
    fun desviosSinRealesEsVacio() {
        assertEquals(emptyList(), desvios(receta, emptyList()))
    }

    @Test
    fun formatoDeltaUsaSegundosPorDebajoDeUnMinutoYMinutosRedondeados() {
        assertEquals("+45 s", formatoDelta(45))
        assertEquals("-30 s", formatoDelta(-30))
        assertEquals("0 s", formatoDelta(0))
        assertEquals("+3 min", formatoDelta(180))
        assertEquals("-2 min", formatoDelta(-110))
        assertEquals("+1 min", formatoDelta(60))
    }

    @Test
    fun textoInformeOrdenaPorDeltaAbsolutoYConservaLlaves() {
        val d =
            listOf(
                DesvioPaso("c", "Reposar", 90, 60, -30),
                DesvioPaso("a", "Sudar la verdura al {5}", 600, 780, 180),
                DesvioPaso("b", "Cocer las lentejas", 1200, 1000, -200),
            )
        val esperado =
            """
            Lentejas con verdura
            2026-10-24 · 2 raciones
            Total: previsto 30 min · real 33 min (+3 min)

            -3 min · Cocer las lentejas
            +3 min · Sudar la verdura al {5}
            -30 s · Reposar
            """.trimIndent()
        assertEquals(esperado, textoInforme(receta, "2026-10-24", 2, 33, d))
    }

    @Test
    fun textoInformeSinMinutosRealesNiDesviosYUnaRacion() {
        val esperado =
            """
            Lentejas con verdura
            2026-10-24 · 1 ración
            Total: previsto 30 min
            """.trimIndent()
        assertEquals(esperado, textoInforme(receta, "2026-10-24", 1, null, emptyList()))
    }
}
