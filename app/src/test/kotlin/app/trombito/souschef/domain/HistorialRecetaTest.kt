package app.trombito.souschef.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HistorialRecetaTest {
    private fun e(
        fecha: String,
        minutos: Int? = null,
        slug: String = "sopa",
        previsto: Int? = null,
        valoracion: Valoracion? = null,
    ) = EntradaHistorial(
        id = "$slug-$fecha",
        slug = slug,
        titulo = slug,
        fecha = fecha,
        minutosReales = minutos,
        previstoMin = previsto,
        valoracion = valoracion,
    )

    @Test
    fun `una receta que nunca se cocino no tiene resumen`() {
        assertNull(resumenDeReceta(listOf(e("2026-10-01", 40, slug = "otra")), "sopa", 30.0))
        assertNull(resumenDeReceta(emptyList(), "sopa", 30.0))
    }

    @Test
    fun `cuenta las veces y toma la ultima fecha`() {
        val r = resumenDeReceta(listOf(e("2026-09-01", 40), e("2026-10-05", 44), e("2026-08-01", 50)), "sopa", 30.0)
        assertEquals(3, r?.veces)
        assertEquals("2026-10-05", r?.ultimaFecha)
    }

    @Test
    fun `con una sola cocinada con duracion no hay sueles tardar`() {
        val r = resumenDeReceta(listOf(e("2026-10-05", 44)), "sopa", 30.0)
        assertNull(r?.tardaMin)
        assertEquals(1, r?.veces)
    }

    @Test
    fun `con dos o mas cocinadas con duracion sale la mediana de las tres mas recientes`() {
        val dos = resumenDeReceta(listOf(e("2026-10-01", 60), e("2026-10-02", 80)), "sopa", 30.0)
        assertEquals(70, dos?.tardaMin)
        // Las tres más recientes son 80, 60 y 100: la vieja (10) se queda fuera.
        val cuatro =
            resumenDeReceta(
                listOf(e("2026-09-01", 10), e("2026-10-01", 60), e("2026-10-02", 80), e("2026-10-03", 100)),
                "sopa",
                30.0,
            )
        assertEquals(80, cuatro?.tardaMin)
    }

    @Test
    fun `una cocinada anotada sin duracion cuenta para las veces y la fecha pero no para el ritmo`() {
        val r = resumenDeReceta(listOf(e("2026-10-09"), e("2026-10-01", 60), e("2026-10-02", 80)), "sopa", 30.0)
        assertEquals(3, r?.veces)
        assertEquals("2026-10-09", r?.ultimaFecha)
        assertEquals(70, r?.tardaMin)
        // Con una sola con duración y otra anotada, tampoco hay ritmo.
        assertNull(resumenDeReceta(listOf(e("2026-10-09"), e("2026-10-01", 60)), "sopa", 30.0)?.tardaMin)
    }

    @Test
    fun `las duraciones fuera de rango no cuentan`() {
        val r = resumenDeReceta(listOf(e("2026-10-01", 0), e("2026-10-02", 900), e("2026-10-03", 50)), "sopa", 30.0)
        assertNull(r?.tardaMin)
    }

    @Test
    fun `el plan es el previsto guardado mas reciente o el total de la receta redondeado hacia arriba`() {
        val guardado = resumenDeReceta(listOf(e("2026-10-01", 60, previsto = 55), e("2026-10-02", 62)), "sopa", 30.0)
        assertEquals(55, guardado?.planMin)
        val mixto =
            resumenDeReceta(
                listOf(e("2026-10-01", 60, previsto = 55), e("2026-10-02", 62, previsto = 58)),
                "sopa",
                30.0,
            )
        assertEquals(58, mixto?.planMin)
        val sin = resumenDeReceta(listOf(e("2026-10-01", 60), e("2026-10-02", 62)), "sopa", 47.2)
        assertEquals(48, sin?.planMin)
    }

    @Test
    fun `la valoracion es la mas reciente que exista`() {
        val r =
            resumenDeReceta(
                listOf(
                    e("2026-10-01", valoracion = Valoracion.NO_REPETIR),
                    e("2026-10-02", valoracion = Valoracion.REPETIR),
                    e("2026-10-03"),
                ),
                "sopa",
                30.0,
            )
        assertEquals(Valoracion.REPETIR, r?.valoracion)
        assertNull(resumenDeReceta(listOf(e("2026-10-03")), "sopa", 30.0)?.valoracion)
    }

    @Test
    fun `entre dos del mismo dia manda el id mayor`() {
        val r =
            resumenDeReceta(
                listOf(
                    e("2026-10-03", valoracion = Valoracion.AJUSTAR).copy(id = "sopa-2026-10-03-120000"),
                    e("2026-10-03", valoracion = Valoracion.REPETIR).copy(id = "sopa-2026-10-03-210000"),
                ),
                "sopa",
                30.0,
            )
        assertEquals(Valoracion.REPETIR, r?.valoracion)
    }
}

class ResumenMesTest {
    private val hoy = LocalDate.of(2026, 10, 24)

    private fun e(
        slug: String,
        fecha: String,
    ) = EntradaHistorial(id = "$slug-$fecha", slug = slug, titulo = slug, fecha = fecha)

    @Test
    fun `cuenta las cocinadas del mes, las recetas distintas y las que siguen sin estrenar`() {
        val entradas =
            listOf(
                e("sopa", "2026-10-20"),
                e("sopa", "2026-10-02"),
                e("arroz", "2026-10-10"),
                e("pasta", "2026-09-30"),
                e("curry", "2025-10-15"),
            )
        val r = resumenDelMes(entradas, setOf("sopa", "arroz", "pasta", "curry", "flan", "tarta"), hoy)
        assertEquals(ResumenMes(cocinadas = 3, recetasDistintas = 2, sinEstrenar = 2, totalRecetas = 6), r)
    }

    @Test
    fun `sin nada cocinado este mes salen ceros y todo sin estrenar`() {
        val r = resumenDelMes(emptyList(), setOf("a", "b"), hoy)
        assertEquals(ResumenMes(0, 0, 2, 2), r)
    }

    @Test
    fun `una fecha ilegible no cuenta en el mes pero su receta ya esta estrenada`() {
        val r = resumenDelMes(listOf(e("sopa", "ayer"), e("sopa", "2026-10-01T21:15:00")), setOf("sopa", "flan"), hoy)
        assertEquals(ResumenMes(cocinadas = 1, recetasDistintas = 1, sinEstrenar = 1, totalRecetas = 2), r)
    }

    @Test
    fun `una cocinada de una receta retirada cuenta como cocinada y no cambia los sin estrenar`() {
        val r = resumenDelMes(listOf(e("vieja", "2026-10-02")), setOf("sopa"), hoy)
        assertEquals(1, r.cocinadas)
        assertEquals(1, r.sinEstrenar)
        assertEquals(1, r.totalRecetas)
    }
}
