package app.trombito.souschef.ui.recetas

import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.receta.Receta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueMeDaTiempoTest {
    private fun r(
        slug: String,
        total: Double,
    ) = Receta(slug = slug, titulo = slug, totalMin = total)

    private fun hm(
        h: Int,
        m: Int,
    ) = h * 60 + m

    private fun tarjetas(vararg recetas: Pair<String, Double>) =
        recetas.map { (s, t) ->
            TarjetaReceta(r(s, t), favorita = false, fuegos = 0, ultimaFecha = null, hace = Hace(UnidadHace.HOY))
        }

    @Test
    fun conLaHoraDeServirPorDelanteLlegaLaQueCabeYNoLaQueNo() {
        // Son las 14:00 y se sirve a las 15:30: quedan 90 min.
        val recetas = listOf(r("corta", 47.0), r("justa", 90.0), r("larga", 91.0))
        val l = QueMeDaTiempo.evaluar(recetas, hm(14, 0), hm(15, 30))
        assertEquals(listOf(true, true, false), l.map { it.llega })
    }

    @Test
    fun siLlegaDaLaHoraDeEmpezarYNoTieneHoraDeAcabar() {
        val l = QueMeDaTiempo.evaluar(listOf(r("a", 47.0)), hm(14, 0), hm(15, 30)).single()
        assertEquals(hm(14, 43), l.empiezaMin)
        assertNull(l.acabariaMin)
        assertEquals(47, l.minutos)
    }

    @Test
    fun siNoLlegaDaLaHoraEnQueAcabariaSiEmpezaraAhora() {
        val l = QueMeDaTiempo.evaluar(listOf(r("a", 120.5)), hm(14, 0), hm(15, 30)).single()
        assertFalse(l.llega)
        assertEquals(hm(16, 1), l.acabariaMin)
        assertEquals(hm(13, 29), l.empiezaMin)
    }

    @Test
    fun laDuracionSeRedondeaHaciaArribaComoEnLaFicha() {
        val l = QueMeDaTiempo.evaluar(listOf(r("a", 90.2)), hm(14, 0), hm(15, 30)).single()
        assertFalse(l.llega)
        assertEquals(91, l.minutos)
    }

    @Test
    fun conLaHoraDeServirIgualAAhoraElMargenEsCero() {
        assertEquals(0, QueMeDaTiempo.margen(hm(15, 30), hm(15, 30)))
    }

    @Test
    fun servirYaPasadoSeTomaMananaSoloSiFaltanMenosDe12Horas() {
        // 23:00 -> 02:00 de mañana: faltan 3 h.
        assertEquals(180, QueMeDaTiempo.margen(hm(23, 0), hm(2, 0)))
        // 15:45 -> 03:00 de mañana: faltan 11 h 15.
        assertEquals(11 * 60 + 15, QueMeDaTiempo.margen(hm(15, 45), hm(3, 0)))
        // 15:30 -> 03:30: faltan justo 12 h, ya no cuenta.
        assertNull(QueMeDaTiempo.margen(hm(15, 30), hm(3, 30)))
    }

    @Test
    fun servirYaPasadoConMasDe12HorasPorDelanteNoLlegaNinguna() {
        // Son las 20:00 y se sirve a las 15:30: mañana quedan 19 h 30.
        assertNull(QueMeDaTiempo.margen(hm(20, 0), hm(15, 30)))
        val l = QueMeDaTiempo.evaluar(listOf(r("a", 5.0), r("b", 30.0)), hm(20, 0), hm(15, 30))
        assertTrue(l.none { it.llega })
        assertEquals(hm(20, 5), l[0].acabariaMin)
    }

    @Test
    fun servirDeMananaDaMargenYLlegan() {
        val l = QueMeDaTiempo.evaluar(listOf(r("a", 120.0), r("b", 200.0)), hm(23, 0), hm(2, 0))
        assertEquals(listOf(true, false), l.map { it.llega })
        assertEquals(0, l[0].empiezaMin)
    }

    @Test
    fun conservaElOrdenDeEntrada() {
        val l = QueMeDaTiempo.evaluar(listOf(r("z", 10.0), r("a", 10.0)), 0, 60)
        assertEquals(listOf("z", "a"), l.map { it.slug })
    }

    @Test
    fun aplicarTiempoOrdenaPrimeroLasQueLleganDeLaMasCortaYDespuesLasDemasEnSuOrden() {
        val entrada = tarjetas("larga" to 200.0, "media" to 60.0, "corta" to 20.0, "otra" to 300.0)
        val salida = aplicarTiempo(entrada, ModoTiempo(hm(15, 30)), hm(14, 0))
        assertEquals(listOf("corta", "media", "larga", "otra"), salida.map { it.receta.slug })
        assertEquals(listOf(true, true, false, false), salida.map { it.llegada?.llega })
        // Las que llegan enseñan cuándo empezar; las demás, cuándo acabarían.
        assertEquals(HoraTarjeta.EMPEZAR, salida[0].modoHora)
        assertEquals(hm(15, 10), salida[0].hora)
        assertEquals(HoraTarjeta.ACABAR, salida[2].modoHora)
        assertEquals(hm(17, 20), salida[2].hora)
    }

    @Test
    fun soloLlegoQuitaLasQueNoLleganYSinModoNoCambiaNada() {
        val entrada = tarjetas("a" to 20.0, "b" to 500.0)
        val soloLlego = aplicarTiempo(entrada, ModoTiempo(hm(15, 30), soloLlego = true), hm(14, 0))
        assertEquals(listOf("a"), soloLlego.map { it.receta.slug })
        assertEquals(entrada, aplicarTiempo(entrada, null, hm(14, 0)))
    }
}
