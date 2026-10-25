package app.trombito.souschef.ui.resena

import app.trombito.souschef.ui.Rutas
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SalidaDelCronoTest {
    @Test
    fun `del cronometro a la ficha o a recetas es salida`() {
        assertTrue(salidaDelCrono(Rutas.CRONO, Rutas.FICHA))
        assertTrue(salidaDelCrono(Rutas.CRONO, "recetas"))
    }

    @Test
    fun `dentro del cronometro o desde otras pantallas no es salida`() {
        assertFalse(salidaDelCrono(Rutas.CRONO, Rutas.CRONO))
        assertFalse(salidaDelCrono(Rutas.CRONO, Rutas.TECNICA))
        assertFalse(salidaDelCrono(Rutas.TECNICA, Rutas.CRONO))
        assertFalse(salidaDelCrono(Rutas.AJUSTES, Rutas.FICHA))
        assertFalse(salidaDelCrono(null, Rutas.FICHA))
    }
}
