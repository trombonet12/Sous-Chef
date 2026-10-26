package app.trombito.souschef.domain

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Equipo
import app.trombito.souschef.receta.Momento
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PropuestasTest {
    private val hoy = LocalDate.of(2026, 10, 25)
    private val manana = LocalDateTime.of(2026, 10, 25, 10, 0)
    private val ajustes = Ajustes()

    @Suppress("LongParameterList") // cada parámetro es un rasgo de la receta que miran las propuestas
    private fun receta(
        slug: String,
        base: String? = null,
        total: Double = 60.0,
        momento: List<Momento>? = null,
        creada: String? = null,
        horno: Boolean = false,
    ): Receta {
        val hornos = if (horno) listOf(Carril("h", tipo = "horno")) else emptyList()
        return Receta(
            slug = slug,
            titulo = slug.replaceFirstChar { it.uppercase() },
            totalMin = total,
            tags = Tags(base = base, momento = momento),
            creada = creada,
            carriles = listOf(Carril("f1", tipo = "fuego")) + hornos,
            pasos = listOf(Paso("p1", "Hacer", "f1", 0.0, total)),
        )
    }

    private fun cocinada(
        slug: String,
        diasAtras: Long,
        base: String? = null,
        valoracion: Valoracion? = null,
        hora: String = "120000",
    ): EntradaHistorial {
        val fecha = hoy.minusDays(diasAtras).toString()
        return EntradaHistorial("$slug-$fecha-$hora", slug, slug, fecha, base = base, valoracion = valoracion)
    }

    @Suppress("LongParameterList") // cada parámetro es una entrada de la función pura
    private fun slugs(
        recetas: List<Receta>,
        historial: List<EntradaHistorial> = emptyList(),
        preferencias: Map<String, Preferencia> = emptyMap(),
        ajustes: Ajustes = this.ajustes,
        ahora: LocalDateTime = manana,
        desplazamiento: Int = 0,
    ) = propuestas(recetas, historial, preferencias, ajustes, ahora, ahora.toLocalDate(), desplazamiento)
        .map { it.receta.slug }

    @Test
    fun `como mucho tres propuestas`() {
        val recetas = (1..8).map { receta("r$it", base = "b$it") }
        assertEquals(3, slugs(recetas).size)
    }

    @Test
    fun `sin recetas no hay propuestas`() {
        assertTrue(slugs(emptyList()).isEmpty())
    }

    @Test
    fun `solo las que llegan a la hora con cinco minutos de margen`() {
        // A las 10:00 y sirviendo a las 15:30: 335 min empiezan a las 09:55 (justo el margen); 336, a las 09:54.
        val justa = receta("justa", total = 335.0)
        val tarde = receta("tarde", total = 336.0)
        assertEquals(listOf("justa"), slugs(listOf(justa, tarde)))
    }

    @Test
    fun `a las siete de la tarde ninguna de comida llega`() {
        val recetas = listOf(receta("paella"), receta("tortilla"))
        assertTrue(slugs(recetas, ahora = LocalDateTime.of(2026, 10, 25, 19, 0)).isEmpty())
    }

    @Test
    fun `las de cena llegan a la hora de cenar y salen como llega para cenar`() {
        val cena = receta("crema", total = 40.0, momento = listOf(Momento.CENA))
        val ahora = LocalDateTime.of(2026, 10, 25, 19, 0)
        val p = propuestas(listOf(cena), emptyList(), emptyMap(), ajustes, ahora, hoy).single()
        assertEquals(21 * 60 - 40, p.empiezaMin)
        assertEquals(MotivoPropuesta.SinEstrenar, p.motivo)
        // Cocinada hace 15 días y de cena: el motivo es la hora.
        val repetida = propuestas(listOf(cena), listOf(cocinada("crema", 15)), emptyMap(), ajustes, ahora, hoy).single()
        assertEquals(MotivoPropuesta.LlegaParaCenar, repetida.motivo)
        // A las 21:30 ya no llega.
        assertTrue(slugs(listOf(cena), ahora = LocalDateTime.of(2026, 10, 25, 21, 30)).isEmpty())
    }

    @Test
    fun `fuera lo cocinado en los ultimos catorce dias`() {
        val recetas = listOf(receta("a", "x"), receta("b", "x"), receta("c", "x"), receta("d", "x"))
        val historial =
            listOf(
                cocinada("a", 13, base = "otra"),
                cocinada("b", 14, base = "otra2"),
                cocinada("c", 0, base = "otra3"),
            )
        assertEquals(setOf("b", "d"), slugs(recetas, historial).toSet())
    }

    @Test
    fun `no repite la base de las dos ultimas cocinadas`() {
        val recetas =
            listOf(receta("p", "pasta"), receta("a", "arroz"), receta("c", "carnes"), receta("v", "verdura"))
        val historial =
            listOf(
                cocinada("cena1", 20, base = "pasta"),
                cocinada("cena2", 30, base = "arroz"),
                cocinada("cena3", 40, base = "carnes"),
            )
        assertEquals(setOf("c", "v"), slugs(recetas, historial).toSet())
    }

    @Test
    fun `la base de una cocinada sin base se toma de su receta`() {
        val recetas = listOf(receta("p", "pasta"), receta("a", "arroz"), receta("cocinada", "carnes"))
        val historial = listOf(cocinada("cocinada", 20))
        assertEquals(setOf("p", "a"), slugs(recetas, historial).toSet())
    }

    @Test
    fun `la base sin dato no se excluye`() {
        val recetas = listOf(receta("sin"), receta("p", "pasta"))
        val historial = listOf(cocinada("x", 20, base = "pasta"))
        assertEquals(setOf("sin"), slugs(recetas, historial).toSet())
    }

    @Test
    fun `fuera las valoradas no repetir en su ultima cocinada`() {
        val recetas = listOf(receta("mala", "a"), receta("redimida", "b"), receta("buena", "c"))
        val historial =
            listOf(
                cocinada("mala", 30, base = "z1", valoracion = Valoracion.NO_REPETIR),
                cocinada("redimida", 60, base = "z2", valoracion = Valoracion.NO_REPETIR),
                cocinada("redimida", 30, base = "z3", valoracion = Valoracion.REPETIR),
                cocinada("buena", 30, base = "z4", valoracion = Valoracion.AJUSTAR),
            )
        assertEquals(setOf("redimida", "buena"), slugs(recetas, historial).toSet())
    }

    @Test
    fun `fuera las que no caben con el equipo`() {
        val recetas = listOf(receta("asado", "a", horno = true), receta("sarten", "b"))
        val sinHorno = ajustes.copy(equipo = Equipo.entries.toSet() - Equipo.HORNO)
        assertEquals(listOf("sarten"), slugs(recetas, ajustes = sinHorno))
    }

    @Test
    fun `primero las que quiere hacer, luego las nuevas y despues el resto`() {
        val recetas =
            listOf(
                receta("normal1", "a"),
                receta("nueva", "b", creada = "2026-10-20T00:00:00Z"),
                receta("normal2", "c"),
                receta("quiero", "d"),
            )
        val prefs = mapOf("quiero" to Preferencia("quiero", quiereHacerla = true))
        val resultado = propuestas(recetas, emptyList(), prefs, ajustes, manana, hoy)
        assertEquals(listOf("quiero", "nueva"), resultado.take(2).map { it.receta.slug })
        assertEquals(MotivoPropuesta.QuieresHacerla, resultado[0].motivo)
        assertEquals(MotivoPropuesta.SinEstrenar, resultado[1].motivo)
    }

    @Test
    fun `el motivo de una cocinada hace tiempo es cuantos dias`() {
        // Con otra base en las dos últimas: si no, su propia base (la de la receta) bloquearía la propuesta.
        val historial = listOf(cocinada("a", 21, base = "otra"))
        val p = propuestas(listOf(receta("a", "x")), historial, emptyMap(), ajustes, manana, hoy)
        assertEquals(MotivoPropuesta.HaceTiempo(21), p.single().motivo)
    }

    @Test
    fun `la hora de empezar es servir menos la duracion`() {
        val p = propuestas(listOf(receta("a", total = 90.0)), emptyList(), emptyMap(), ajustes, manana, hoy).single()
        assertEquals(15 * 60 + 30 - 90, p.empiezaMin)
    }

    @Test
    fun `la misma fecha da siempre las mismas propuestas`() {
        val recetas = (1..12).map { receta("r$it", base = "b$it") }
        assertEquals(slugs(recetas), slugs(recetas))
    }

    @Test
    fun `la semilla desempata de forma distinta cada dia`() {
        val recetas = (1..12).map { receta("r$it", base = "b$it") }
        val ordenes =
            (0L..20L)
                .map { d ->
                    val dia = hoy.plusDays(d)
                    candidatas(recetas, emptyList(), emptyMap(), ajustes, manana, dia).map { it.receta.slug }
                }.toSet()
        assertTrue(ordenes.size > 1)
        ordenes.forEach { assertEquals(recetas.map { it.slug }.toSet(), it.toSet()) }
    }

    @Test
    fun `otra desplaza la ventana de tres y da la vuelta`() {
        val recetas = (1..7).map { receta("r$it", base = "b$it") }
        val todas = candidatas(recetas, emptyList(), emptyMap(), ajustes, manana, hoy).map { it.receta.slug }
        assertEquals(7, todas.size)
        assertEquals(todas.subList(0, 3), slugs(recetas, desplazamiento = 0))
        assertEquals(todas.subList(3, 6), slugs(recetas, desplazamiento = 1))
        assertEquals(listOf(todas[6], todas[0], todas[1]), slugs(recetas, desplazamiento = 2))
        assertEquals(todas.subList(0, 3), slugs(recetas, desplazamiento = 7))
    }

    @Test
    fun `con tres candidatas o menos otra no cambia nada`() {
        val recetas = (1..3).map { receta("r$it", base = "b$it") }
        assertEquals(slugs(recetas), slugs(recetas, desplazamiento = 4))
        assertFalse(slugs(recetas, desplazamiento = 4).isEmpty())
    }
}
