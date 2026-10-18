package app.souschef.receta.crono

import app.souschef.receta.Receta
import app.souschef.receta.leerReceta
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Deshacer el último Hecho de un carril (toque sin querer), sobre la ensalada de ñoquis real. */
class MotorCronoDeshacerTest {
    private val ensalada =
        leerReceta(File("../data/recetas/ensalada-de-noquis-con-atun-y-feta.json").readText())

    private val servir = 1_700_000_000_000L

    private fun seg(n: Int): Long = n * 1_000L

    // Ensalada: total 19 min; el sartén (sésamo, off 5) empieza en servir-14 min.
    private val inicioSarten get() = servir - 14 * 60_000L

    private fun abierto(
        r: Receta = ensalada,
        ahora: Long = inicioSarten,
    ) = SesionCrono.abrir(SesionCrono.inicial(r, servir, ahora), r, ahora)

    @Test
    fun deshacerVuelveAlPasoAnteriorConSuRelojComoSiNoSeHubieraTocado() {
        val t0 = inicioSarten
        val corriendo = MotorCrono.empezar(abierto(), ensalada, "sarten", t0)
        val antes = corriendo.carriles.getValue("sarten")
        val tocado = MotorCrono.hecho(corriendo, ensalada, "sarten", t0 + seg(100))

        val e = MotorCrono.deshacer(tocado, ensalada, "sarten", t0 + seg(110))

        assertEquals(antes, e.carriles.getValue("sarten"))
        assertNull(e.pasosReales["sesamo"])
        assertTrue(e.deshacer.isEmpty())
        assertNull(e.ultimoHecho)
        assertEquals(t0 + seg(110), e.actualizadoMs)
    }

    @Test
    fun deshacerTrasElUltimoPasoReabreElCarril() {
        var e = abierto()
        repeat(4) { e = MotorCrono.hecho(e, ensalada, "mesa", inicioSarten + seg(it + 1)) }
        assertTrue(e.carriles.getValue("mesa").fin)

        val r = MotorCrono.deshacer(e, ensalada, "mesa", inicioSarten + seg(10))

        val mesa = r.carriles.getValue("mesa")
        assertFalse(mesa.fin)
        assertEquals(3, mesa.i)
    }

    @Test
    fun deshacerSinCarrilDeshaceElUltimoHechoYSoloUnNivel() {
        val t0 = inicioSarten
        var e = MotorCrono.hecho(abierto(), ensalada, "mesa", t0 + seg(1))
        e = MotorCrono.hecho(e, ensalada, "sarten", t0 + seg(2))
        assertEquals("sarten", e.ultimoHecho)

        val r = MotorCrono.deshacer(e, ensalada, null, t0 + seg(3))

        assertEquals(0, r.carriles.getValue("sarten").i)
        assertEquals(1, r.carriles.getValue("mesa").i)
        // Un segundo deshacer sobre el mismo carril ya no tiene nada que deshacer.
        assertEquals(r, MotorCrono.deshacer(r, ensalada, "sarten", t0 + seg(4)))
    }

    @Test
    fun tocarElCarrilTrasElHechoConfirmaElHechoYYaNoSeDeshace() {
        val t0 = inicioSarten
        val tocado = MotorCrono.hecho(abierto(), ensalada, "mesa", t0 + seg(1))
        val empezado = MotorCrono.empezar(tocado, ensalada, "mesa", t0 + seg(2))
        assertTrue("mesa" !in empezado.deshacer)
        assertNull(empezado.ultimoHecho)
        assertEquals(empezado, MotorCrono.deshacer(empezado, ensalada, "mesa", t0 + seg(3)))
    }

    @Test
    fun deshazSinCarrilVaDeshaciendoLosHechoDelMasRecienteAlMasViejo() {
        val t0 = inicioSarten
        var e = MotorCrono.hecho(abierto(), ensalada, "mesa", t0 + seg(1))
        e = MotorCrono.hecho(e, ensalada, "sarten", t0 + seg(2))

        val una = MotorCrono.deshacer(e, ensalada, null, t0 + seg(3))
        assertEquals("mesa", una.ultimoHecho)
        val dos = MotorCrono.deshacer(una, ensalada, null, t0 + seg(4))

        assertEquals(0, dos.carriles.getValue("mesa").i)
        assertEquals(0, dos.carriles.getValue("sarten").i)
        assertNull(dos.ultimoHecho)
    }

    @Test
    fun sinNadaQueDeshacerNoCambiaNada() {
        val e = abierto()
        assertEquals(e, MotorCrono.deshacer(e, ensalada, "mesa", inicioSarten))
        assertEquals(e, MotorCrono.deshacer(e, ensalada, null, inicioSarten))
    }
}
