package app.trombito.souschef.ui.crono

import app.trombito.souschef.receta.crono.ClaseEstado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Qué dice la línea de la hora prevista y qué estilo lleva la acción principal en cada estado de arranque. */
class HoraPrevistaTest {
    private fun v(
        clase: ClaseEstado,
        accion: AccionCarril = AccionCarril.EMPEZAR,
        faltanMin: Int? = null,
        bloqueadoPorN: Int? = null,
    ) = VistaCarril(
        id = "a",
        nombre = "A",
        colorHex = "#B0A899",
        indiceCarril = 0,
        paso = if (accion == AccionCarril.TERMINADO) 0 else 1,
        pasos = 2,
        titulo = "Paso",
        fuego = null,
        tapa = null,
        quedanS = 60,
        duracionS = 60,
        progreso = 0f,
        hitos = emptyList(),
        proximoHito = null,
        meto = null,
        saco = null,
        nota = null,
        despues = null,
        accion = accion,
        bloqueadoPor = null,
        clase = clase,
        sugeridoMs = 1_000L,
        finAtMs = null,
        tienePorque = false,
        faltanMin = faltanMin,
        bloqueadoPorN = bloqueadoPorN,
    )

    // ---- previstaDe

    @Test
    fun `en ESPERA dice en cuantos minutos`() {
        assertEquals(Prevista(TipoPrevista.EN_MIN, minutos = 22), previstaDe(v(ClaseEstado.ESPERA, faltanMin = 22)))
    }

    @Test
    fun `en ESPERA sin dato de minutos no dice nada`() {
        assertNull(previstaDe(v(ClaseEstado.ESPERA)))
    }

    @Test
    fun `en BLOQ dice a que carril espera`() {
        val p = previstaDe(v(ClaseEstado.BLOQ, AccionCarril.BLOQUEADO, bloqueadoPorN = 2))
        assertEquals(Prevista(TipoPrevista.ESPERA_A, carril = 2), p)
    }

    @Test
    fun `en BLOQ sin numero dice solo que espera`() {
        assertEquals(Prevista(TipoPrevista.ESPERA_A, carril = null), previstaDe(v(ClaseEstado.BLOQ)))
    }

    @Test
    fun `en YA dice ahora`() {
        assertEquals(Prevista(TipoPrevista.AHORA), previstaDe(v(ClaseEstado.YA)))
    }

    @Test
    fun `en marcha pausa vencido y terminado no dice nada`() {
        listOf(ClaseEstado.MARCHA, ClaseEstado.PAUSA, ClaseEstado.ATENDER, ClaseEstado.FIN).forEach {
            assertNull(it.name, previstaDe(v(it, AccionCarril.PAUSAR)))
        }
    }

    // ---- numeroCarril

    @Test
    fun `el numero de carril se escribe encerrado en un circulo hasta el veinte`() {
        assertEquals("①", numeroCarril(1))
        assertEquals("②", numeroCarril(2))
        assertEquals("⑳", numeroCarril(20))
        assertEquals("21", numeroCarril(21))
    }

    // ---- estiloAccion

    @Test
    fun `solo YA lleva el boton lleno de empezar`() {
        assertEquals(EstiloAccion.LLENO, estiloAccion(v(ClaseEstado.YA)))
    }

    @Test
    fun `en ESPERA empezar es un enlace discreto`() {
        assertEquals(EstiloAccion.ENLACE, estiloAccion(v(ClaseEstado.ESPERA)))
    }

    @Test
    fun `un carril bloqueado no lleva boton`() {
        assertEquals(EstiloAccion.ESPERA_TEXTO, estiloAccion(v(ClaseEstado.BLOQ, AccionCarril.BLOQUEADO)))
    }

    @Test
    fun `hecho es de contorno mientras corre y lleno al llegar a cero`() {
        assertEquals(EstiloAccion.CONTORNO, estiloAccion(v(ClaseEstado.MARCHA, AccionCarril.PAUSAR)))
        assertEquals(EstiloAccion.LLENO, estiloAccion(v(ClaseEstado.ATENDER, AccionCarril.HECHO)))
    }

    @Test
    fun `seguir tras una pausa es de contorno`() {
        assertEquals(EstiloAccion.CONTORNO, estiloAccion(v(ClaseEstado.PAUSA, AccionCarril.SEGUIR)))
    }
}
