package app.trombito.souschef.ui.crono

import app.trombito.souschef.receta.crono.ClaseEstado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Qué dice la franja «Ahora / Luego»: el modelo puro, sin Compose. */
class FranjaAhoraTest {
    @Suppress("LongParameterList") // un carril de prueba con los pocos datos que cambian entre casos
    private fun carril(
        id: String,
        indice: Int,
        accion: AccionCarril,
        clase: ClaseEstado,
        sugeridoMs: Long? = null,
        quedanS: Int = 60,
        finAtMs: Long? = null,
        bloqueadoPorN: Int? = null,
        juntoN: Int? = null,
    ) = VistaCarril(
        id = id,
        nombre = id.uppercase(),
        colorHex = "#B0A899",
        indiceCarril = indice,
        paso = if (accion == AccionCarril.TERMINADO) 0 else 1,
        pasos = 2,
        titulo = if (accion == AccionCarril.TERMINADO) null else "Paso $id",
        fuego = null,
        tapa = null,
        quedanS = quedanS,
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
        sugeridoMs = sugeridoMs,
        finAtMs = finAtMs,
        tienePorque = false,
        bloqueadoPorN = bloqueadoPorN,
        juntoN = juntoN,
    )

    private fun ya(
        id: String,
        indice: Int,
        hora: Long,
    ) = carril(id, indice, AccionCarril.EMPEZAR, ClaseEstado.YA, hora)

    private fun espera(
        id: String,
        indice: Int,
        hora: Long,
        juntoN: Int? = null,
    ) = carril(id, indice, AccionCarril.EMPEZAR, ClaseEstado.ESPERA, hora, juntoN = juntoN)

    private fun bloqueado(
        id: String,
        indice: Int,
        hora: Long,
        por: Int,
    ) = carril(id, indice, AccionCarril.BLOQUEADO, ClaseEstado.BLOQ, hora, bloqueadoPorN = por)

    private fun marcha(
        id: String,
        indice: Int,
        quedanS: Int,
    ) = carril(id, indice, AccionCarril.PAUSAR, ClaseEstado.MARCHA, quedanS = quedanS, finAtMs = 1_000L + quedanS)

    private fun vencido(
        id: String,
        indice: Int,
        finAtMs: Long,
    ) = carril(id, indice, AccionCarril.HECHO, ClaseEstado.ATENDER, quedanS = -5, finAtMs = finAtMs)

    private fun terminado(
        id: String,
        indice: Int,
    ) = carril(id, indice, AccionCarril.TERMINADO, ClaseEstado.FIN, quedanS = 0)

    private fun Ahora.carril(): String? =
        when (this) {
            is Ahora.Toca -> v.id
            is Ahora.EnMarcha -> v.id
            Ahora.Nada -> null
        }

    @Test
    fun `sin carriles vivos no hay franja`() {
        assertNull(modeloFranja(emptyList()))
        assertNull(modeloFranja(listOf(terminado("a", 0), terminado("b", 1))))
    }

    @Test
    fun `ahora es el vencido mas antiguo aunque otro toque ya`() {
        val m =
            modeloFranja(
                listOf(ya("a", 0, 100), vencido("b", 1, finAtMs = 9_000), vencido("c", 2, finAtMs = 2_000)),
            )
        val ahora = m?.ahora as Ahora.Toca
        assertEquals("c", ahora.v.id)
        assertTrue(ahora.lleno)
    }

    @Test
    fun `ahora es el que toca ya de hora mas temprana con boton lleno`() {
        val m = modeloFranja(listOf(marcha("a", 0, 30), ya("b", 1, 900), ya("c", 2, 300), espera("d", 3, 100)))
        val ahora = m?.ahora as Ahora.Toca
        assertEquals("c", ahora.v.id)
        assertTrue(ahora.lleno)
    }

    @Test
    fun `sin nada en marcha ni YA ahora es el primero de la cola con dependencias cumplidas y boton de contorno`() {
        val m = modeloFranja(listOf(bloqueado("a", 0, 100, por = 2), espera("b", 1, 900), espera("c", 2, 300)))
        val ahora = m?.ahora as Ahora.Toca
        assertEquals("c", ahora.v.id)
        assertEquals(false, ahora.lleno)
    }

    @Test
    fun `con algo en marcha y nada que toque ahora es el que acaba antes`() {
        val m = modeloFranja(listOf(marcha("a", 0, 300), marcha("b", 1, 40), espera("c", 2, 5_000)))
        assertEquals("b", (m?.ahora as Ahora.EnMarcha).v.id)
    }

    @Test
    fun `solo bloqueados y nada en marcha es nada que empezar`() {
        val m = modeloFranja(listOf(bloqueado("a", 0, 100, por = 2), bloqueado("b", 1, 200, por = 1)))
        assertEquals(Ahora.Nada, m?.ahora)
        assertEquals(listOf("a", "b"), m?.luego?.map { it.v.id })
    }

    @Test
    fun `luego deja fuera el de ahora, va por hora y no pasa de dos`() {
        val m =
            modeloFranja(
                listOf(
                    espera("d", 3, 700),
                    ya("a", 0, 100),
                    espera("c", 2, 500),
                    espera("b", 1, 300),
                    marcha("e", 4, 10),
                    terminado("f", 5),
                ),
            )
        assertEquals("a", m?.ahora?.carril())
        assertEquals(listOf("b", "c"), m?.luego?.map { it.v.id })
    }

    @Test
    fun `con letra grande luego se queda en una fila`() {
        val m = modeloFranja(listOf(ya("a", 0, 1), espera("b", 1, 2), espera("c", 2, 3)), maxLuego = 1)
        assertEquals(listOf("b"), m?.luego?.map { it.v.id })
    }

    @Test
    fun `luego dice cuando acabe del bloqueo y a la vez que del junto`() {
        val m =
            modeloFranja(
                listOf(
                    ya("a", 0, 100),
                    bloqueado("b", 1, 300, por = 1),
                    espera("c", 2, 500, juntoN = 1),
                ),
            )
        val luego = m?.luego.orEmpty()
        assertEquals(FilaLuego(luego[0].v, Relacion.CUANDO_ACABE, 1), luego[0])
        assertEquals(FilaLuego(luego[1].v, Relacion.A_LA_VEZ, 1), luego[1])
    }

    @Test
    fun `sin nada pendiente luego queda vacio`() {
        val m = modeloFranja(listOf(marcha("a", 0, 30), terminado("b", 1)))
        assertTrue(m?.luego.orEmpty().isEmpty())
    }

    @Test
    fun `luego no repite la potencia si el siguiente paso no la cambia`() {
        assertEquals("Reposo", luegoSinPotencia("Reposo · fuego 5", fuego = 5, fuegoSiguiente = 5))
        assertEquals("Reposo · fuego 8", luegoSinPotencia("Reposo · fuego 8", fuego = 5, fuegoSiguiente = 8))
        assertEquals("Subir al 5", luegoSinPotencia("Subir al 5 · fuego 5", fuego = 4, fuegoSiguiente = 5))
        assertEquals("Emplatar", luegoSinPotencia("Emplatar", fuego = null, fuegoSiguiente = null))
    }

    // ---- reparto de Carriles: tarjeta expandida o fila

    @Test
    fun `con sitio de sobra todos los carriles vivos son tarjeta`() {
        val v = listOf(ya("a", 0, 1), espera("b", 1, 2), marcha("c", 2, 30), terminado("d", 3))
        assertEquals(setOf("a", "b", "c"), carrilesEnTarjeta(v, alturaDp = 900f, escala = 1f))
    }

    @Test
    fun `con cuatro carriles por empezar y poco sitio quedan dos tarjetas las que mas urgen`() {
        val v = listOf(espera("a", 0, 40), ya("b", 1, 5), bloqueado("c", 2, 10, por = 2), espera("d", 3, 20))
        assertEquals(setOf("b", "d"), carrilesEnTarjeta(v, alturaDp = 780f, escala = 1f))
    }

    @Test
    fun `con letra grande caben menos tarjetas y el vencido nunca pasa a fila antes que lo que espera`() {
        val v = listOf(espera("a", 0, 40), vencido("b", 1, 9_000), marcha("c", 2, 30), espera("d", 3, 20))
        val grande = carrilesEnTarjeta(v, alturaDp = 780f, escala = 1.3f)
        assertTrue("b" in grande)
        assertTrue("a" !in grande)
        assertTrue(grande.size < carrilesEnTarjeta(v, alturaDp = 780f, escala = 1f).size + 1)
    }

    @Test
    fun `siempre queda al menos una tarjeta`() {
        val v = listOf(espera("a", 0, 40), espera("b", 1, 20))
        assertEquals(setOf("b"), carrilesEnTarjeta(v, alturaDp = 100f, escala = 1.3f))
    }

    @Test
    fun `el carril tocado se expande antes que los que esperan o corren pero no antes que lo que toca ya`() {
        val v = listOf(marcha("a", 0, 30), espera("b", 1, 40), ya("c", 2, 5))
        // Sitio para dos tarjetas por empezar y una fila: toca ya (c) y, entre b y a, el tocado.
        val alto = 108f + 18f + 200f + 200f + 80f
        assertEquals(setOf("c", "b"), carrilesEnTarjeta(v, alturaDp = alto, escala = 1f, tocado = "b"))
        assertEquals(setOf("c", "a"), carrilesEnTarjeta(v, alturaDp = alto, escala = 1f))
    }

    @Test
    fun `entre dos vencidos la tarjeta es la del mas antiguo que es el de la franja`() {
        val v = listOf(vencido("a", 0, 90_000), vencido("b", 1, 5_000))
        assertEquals(setOf("b"), carrilesEnTarjeta(v, alturaDp = 108f + 12f + 150f + 80f, escala = 1f))
    }
}
