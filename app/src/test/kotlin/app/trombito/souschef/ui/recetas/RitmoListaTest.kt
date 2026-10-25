package app.trombito.souschef.ui.recetas

import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.ui.receta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** El ritmo de Joan en la lista: hora de la tarjeta ajustada y marca «con tu ritmo» (v1.7, U1). */
class RitmoListaTest {
    private val hoy = LocalDate.of(2026, 10, 14)
    private val r = receta("r").copy(totalMin = 47.0)
    private val otra = receta("otra").copy(totalMin = 47.0)

    private fun cocinada(
        id: Int,
        slug: String,
        minutos: Int?,
    ) = EntradaHistorial("$slug-$id", slug, slug, "2026-10-%02d".format(id), minutosReales = minutos)

    @Test
    fun conRitmoPositivoSeEmpiezaAntes() {
        val contexto = ContextoLista(hoy, ritmos = mapOf("r" to 8))
        assertEquals(14 * 60 + 35, contexto.hora(r))
        assertEquals(14 * 60 + 43, contexto.hora(otra))
    }

    @Test
    fun conRitmoNegativoSeEmpiezaDespues() {
        assertEquals(14 * 60 + 48, ContextoLista(hoy, ritmos = mapOf("r" to -5)).hora(r))
    }

    @Test
    fun enModoAcabarElRitmoAlargaLaHoraDeAcabar() {
        val contexto = ContextoLista(hoy, ahoraMin = 12 * 60, modo = HoraTarjeta.ACABAR, ritmos = mapOf("r" to 8))
        assertEquals(12 * 60 + 55, contexto.hora(r))
    }

    @Test
    fun ritmosPorSlugSoloIncluyeLasRecetasConRitmo() {
        val historial =
            listOf(
                cocinada(1, "r", 55),
                cocinada(2, "r", 55),
                cocinada(3, "otra", 60),
            )
        assertEquals(mapOf("r" to 8), ritmosPorSlug(listOf(r, otra, receta("nueva")), historial))
        assertTrue(ritmosPorSlug(listOf(r), emptyList()).isEmpty())
    }

    @Test
    fun laTarjetaTraeLaHoraConRitmoYSuMarca() {
        val estado =
            construirEstadoRecetas(
                listOf(r, otra),
                emptyMap(),
                emptyMap(),
                FiltroRecetas(),
                ContextoLista(hoy, ritmos = mapOf("r" to 8)),
            )
        val con = estado.tarjetas.first { it.receta.slug == "r" }
        val sin = estado.tarjetas.first { it.receta.slug == "otra" }
        assertTrue(con.conRitmo)
        assertEquals(14 * 60 + 35, con.hora)
        assertFalse(sin.conRitmo)
        assertEquals(14 * 60 + 43, sin.hora)
    }
}
