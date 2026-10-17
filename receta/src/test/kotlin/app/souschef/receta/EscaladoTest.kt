package app.souschef.receta

import kotlin.test.Test
import kotlin.test.assertEquals

class EscaladoTest {
    @Test
    fun linealDobla() {
        assertEquals("500 g", Escalado.cantidad(Ingrediente(n = "carne", a = 250.0, u = "g"), 2.0))
    }

    @Test
    fun menosDeDiezLlevaUnDecimalConComa() {
        assertEquals("2,5 cdta", Escalado.cantidad(Ingrediente(n = "sal", a = 5.0, u = "cdta"), 0.5))
        assertEquals("3 uds", Escalado.cantidad(Ingrediente(n = "huevos", a = 1.5, u = "uds"), 2.0))
    }

    @Test
    fun sublinealUsaExponente075YMarca() {
        // 4 g · 2^0,75 = 6,73 → "6,7 g *"
        assertEquals("6,7 g *", Escalado.cantidad(Ingrediente(n = "sal", a = 4.0, u = "g", esc = "sub"), 2.0))
        assertEquals("4 g", Escalado.cantidad(Ingrediente(n = "sal", a = 4.0, u = "g", esc = "sub"), 1.0))
    }

    @Test
    fun textoNoEscala() {
        assertEquals("½ cdta *", Escalado.cantidad(Ingrediente(n = "pimienta", txt = "½ cdta", esc = "sub"), 2.0))
        assertEquals("1 pizca", Escalado.cantidad(Ingrediente(n = "x", txt = "1 pizca"), 3.0))
    }

    @Test
    fun minutosAMedios() {
        assertEquals("2,5", Escalado.minutos(2.5))
        assertEquals("3", Escalado.minutos(3.0))
        assertEquals("114,5", Escalado.minutos(114.5))
    }

    @Test
    fun ejeDelGantt() {
        // 34 min: granu 5, salto 10; 30 se quita por estar a menos de medio salto de 34.
        assertEquals(listOf(0.0, 10.0, 20.0, 34.0), Gantt.marcas(34.0))
        // 82 min: granu 10, salto 20.
        assertEquals(listOf(0.0, 20.0, 40.0, 60.0, 82.0), Gantt.marcas(82.0))
        assertEquals(listOf(0.0, 10.0, 20.0, 30.0, 40.0, 50.0), Gantt.marcas(50.0))
    }
}
