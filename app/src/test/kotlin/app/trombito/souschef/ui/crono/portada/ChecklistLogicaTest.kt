package app.trombito.souschef.ui.crono.portada

import app.trombito.souschef.ui.crono.BloqueCasillas
import app.trombito.souschef.ui.crono.Casilla
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistLogicaTest {
    private val bloques =
        listOf(
            BloqueCasillas("A", listOf(Casilla("1", "x"), Casilla("2", "y"), Casilla("3", "z"))),
            BloqueCasillas("B", listOf(Casilla("4", "w"))),
            BloqueCasillas("C", emptyList()),
        )

    @Test
    fun elProgresoIgnoraMarcasQueYaNoSonDeNingunaCasilla() {
        val p = progresoTotal(bloques, setOf("1", "4", "viejo"))
        assertEquals(Progreso(2, 4), p)
        assertEquals(0.5f, p.fraccion, 0f)
        assertFalse(p.completo)
    }

    @Test
    fun unBloqueVacioNuncaEstaCompleto() {
        assertFalse(progreso(emptyList(), emptySet()).completo)
        assertEquals(0f, progreso(emptyList(), emptySet()).fraccion, 0f)
        assertTrue(progreso(bloques[1].casillas, setOf("4")).completo)
    }

    @Test
    fun laCabeceraEsElTituloYElDetalleYElMinutoSeSeparan() {
        val v = vistaCasilla("<b>Cebolla</b> · picada fina · minuto 4.")
        assertEquals(VistaCasilla("Cebolla", "picada fina · minuto 4.", 4), v)
    }

    @Test
    fun sinCabeceraElTituloSonLasPrimerasPalabrasYUnaLineaCortaNoTieneDetalle() {
        assertEquals(VistaCasilla("Pica la cebolla", null, null), vistaCasilla("Pica la cebolla"))
        val larga = vistaCasilla("Saca el aceite y la sal de la despensa y ponlos junto al fuego")
        assertEquals("Saca el aceite y la…", larga.titulo)
        assertEquals("Saca el aceite y la sal de la despensa y ponlos junto al fuego", larga.detalle)
        assertNull(larga.minuto)
    }

    @Test
    fun cabeceraSinDetalleNoTieneInfo() {
        assertNull(vistaCasilla("<b>Sal</b>").detalle)
    }
}
