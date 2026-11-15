package app.trombito.souschef.ui.crono

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.crono.EstadoCarril
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.ui.comun.SituacionPaso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiagramaCronoTest {
    private val ahora = 1_800_000_000_000L
    private val servir = ahora + 20 * 60_000L

    /** FUEGO: a (0-5) y b (5-15). MESA: c (0-2). */
    private val receta =
        Receta(
            slug = "r",
            titulo = "R",
            totalMin = 15.0,
            carriles = listOf(Carril("f", "FUEGO", "#E4572E", "fuego"), Carril("m", "MESA", "#B0A899", "mesa")),
            pasos =
                listOf(
                    Paso("a", "A", "f", 0.0, 5.0, f = 5),
                    Paso("b", "B", "f", 5.0, 10.0, f = 8),
                    Paso("c", "C", "m", 0.0, 2.0),
                ),
        )

    private fun datos(
        f: EstadoCarril = EstadoCarril(),
        m: EstadoCarril = EstadoCarril(),
    ) = DatosCrono(receta, EstadoCrono("r", 2, servir, mapOf("f" to f, "m" to m), actualizadoMs = ahora), ahora)

    @Test
    fun `antes de empezar todo esta pendiente y no hay linea ahora`() {
        val vivo = vivoPlan(datos())
        assertEquals(setOf(SituacionPaso.PENDIENTE), vivo.situacion.values.toSet())
        assertNull(vivo.ahoraMin)
    }

    @Test
    fun `un paso en marcha va lleno, el hecho apagado y el resto de contorno`() {
        // El carril de fuego va por su segundo paso (a hecho, b en marcha); la mesa no ha empezado.
        val enMarcha = EstadoCarril(i = 1, finAtMs = ahora + 5 * 60_000L)
        val vivo = vivoPlan(datos(f = enMarcha))
        assertEquals(SituacionPaso.HECHO, vivo.situacion["a"])
        assertEquals(SituacionPaso.EN_MARCHA, vivo.situacion["b"])
        assertEquals(SituacionPaso.PENDIENTE, vivo.situacion["c"])
    }

    @Test
    fun `la linea ahora sigue el avance real y no el reloj`() {
        // b dura 10 min y le quedan 5: va por la mitad, minuto 5 + 5 = 10, sea cual sea la hora de servir.
        val enMarcha = EstadoCarril(i = 1, finAtMs = ahora + 5 * 60_000L)
        assertEquals(10.0, vivoPlan(datos(f = enMarcha)).ahoraMin!!, 1e-9)
        val adelantada = DatosCrono(receta, datos(f = enMarcha).estado.copy(servirMs = ahora + 60 * 60_000L), ahora)
        assertEquals(10.0, vivoPlan(adelantada).ahoraMin!!, 1e-9)
    }

    @Test
    fun `con todo hecho la linea queda al final y el paso vencido no la pasa`() {
        val fin = EstadoCarril(i = 2, fin = true)
        assertEquals(15.0, vivoPlan(datos(f = fin, m = EstadoCarril(i = 1, fin = true))).ahoraMin!!, 1e-9)
        val vencido = EstadoCarril(i = 1, finAtMs = ahora - 60_000L)
        assertEquals(15.0, vivoPlan(datos(f = vencido)).ahoraMin!!, 1e-9)
    }

    @Test
    fun `el inicio del plan es servir menos el total en minutos del dia`() {
        val inicio =
            java.time.Instant
                .ofEpochMilli(servir - 15 * 60_000L)
                .atZone(java.time.ZoneId.systemDefault())
        assertEquals(inicio.hour * 60 + inicio.minute, inicioPlanMin(datos()))
    }

    @Test
    fun `con retraso o adelanto el inicio y el fin siguen al plan real y no a servir menos total`() {
        // «a» arrancó hace 2 min: el plan empezó a las ahora−2 (no a servir−15 = ahora+5) y se acaba a las ahora+13.
        val enMarcha =
            EstadoCarril(finAtMs = ahora + 3 * 60_000L, inicioPasoMs = ahora - 2 * 60_000L)
        val d = datos(f = enMarcha)
        assertEquals(minutosDelDia(ahora - 2 * 60_000L), inicioPlanMin(d))
        val (inicio, fin) = horasDelDiagrama(d)
        assertEquals(formatoHora(ahora - 2 * 60_000L), inicio)
        assertEquals(formatoHora(d.plan.llegadaMs), fin)
        assertEquals(formatoHora(ahora + 13 * 60_000L), fin)
    }

    private fun minutosDelDia(ms: Long): Int {
        val t =
            java.time.Instant
                .ofEpochMilli(ms)
                .atZone(java.time.ZoneId.systemDefault())
        return t.hour * 60 + t.minute
    }
}
