package app.trombito.souschef.ui.recetas

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Equipo
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ConLoQueTengoTest {
    private val hoy = LocalDate.of(2026, 10, 25)
    private val conLoQueTengo = OpcionFiltro.Equipo(EquipoReceta.CON_LO_QUE_TENGO)

    private fun receta(
        slug: String,
        horno: Boolean = false,
        equipo: List<Equipo>? = null,
    ): Receta {
        val hornos = if (horno) listOf(Carril("h", tipo = "horno")) else emptyList()
        val carriles = listOf(Carril("f1", tipo = "fuego")) + hornos
        return Receta(
            slug = slug,
            titulo = slug,
            totalMin = 30.0,
            tags = Tags(equipo = equipo),
            carriles = carriles,
            pasos = listOf(Paso("p1", "Hacer", "f1", 0.0, 10.0)),
        )
    }

    private val recetas =
        listOf(
            receta("sencilla"),
            receta("horno", horno = true),
            receta("batida", equipo = listOf(Equipo.BATIDORA)),
            receta("plancha", equipo = listOf(Equipo.PLANCHA)),
        )

    private fun estado(
        tengo: Set<Equipo>,
        activos: Set<OpcionFiltro> = emptySet(),
    ) = construirEstadoRecetas(
        recetas,
        emptyMap(),
        emptyMap(),
        FiltroRecetas(activos = activos),
        ContextoLista(hoy, equipo = tengo),
    )

    private fun RecetasUiState.slugs() = tarjetas.map { it.receta.slug }.toSet()

    @Test
    fun `sin activar la opcion no se oculta nada`() {
        assertEquals(setOf("sencilla", "horno", "batida", "plancha"), estado(emptySet()).slugs())
    }

    @Test
    fun `con todo el equipo todas las recetas caben`() {
        val e = estado(Equipo.entries.toSet(), setOf(conLoQueTengo))
        assertEquals(setOf("sencilla", "horno", "batida", "plancha"), e.slugs())
    }

    @Test
    fun `oculta las recetas que piden algo que no tienes`() {
        val e = estado(setOf(Equipo.BATIDORA), setOf(conLoQueTengo))
        assertEquals(setOf("sencilla", "batida"), e.slugs())
    }

    @Test
    fun `sin nada solo quedan las recetas sin aparatos`() {
        assertEquals(setOf("sencilla"), estado(emptySet(), setOf(conLoQueTengo)).slugs())
    }

    @Test
    fun `el chip vive en la seccion Equipo y cuenta las recetas que caben`() {
        val e = estado(setOf(Equipo.HORNO))
        val seccion = e.secciones.first { it.seccion == SeccionFiltro.EQUIPO }
        val chip = seccion.chips.first { it.opcion == conLoQueTengo }
        assertEquals(2, chip.n)
        assertFalse(chip.activo)
    }

    @Test
    fun `se combina con otras opciones de la seccion con O`() {
        val conHorno = OpcionFiltro.Equipo(EquipoReceta.CON_HORNO)
        val e = estado(emptySet(), setOf(conLoQueTengo, conHorno))
        assertTrue("horno" in e.slugs())
        assertTrue("sencilla" in e.slugs())
        assertFalse("plancha" in e.slugs())
    }
}
