package app.souschef.receta.voz

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InterpreteVozTest {
    private val carriles =
        listOf(
            CarrilVoz("sg", "SARTÉN GRANDE"),
            CarrilVoz("sp", "SARTÉN PEQUEÑA"),
            CarrilVoz("olla", "OLLA"),
            CarrilVoz("mesa", "MESA"),
            CarrilVoz("horno", "HORNO"),
            CarrilVoz("cazo", "CAZO"),
        )

    private fun i(texto: String) = InterpreteVoz.interpretar(texto, carriles)

    // --- Órdenes sin carril ---

    @Test
    fun `hecho sin carril`() = assertEquals(OrdenVoz.Hecho(null), i("chef hecho"))

    @Test
    fun `listo siguiente y ya esta son Hecho`() {
        assertEquals(OrdenVoz.Hecho(null), i("Chef, listo"))
        assertEquals(OrdenVoz.Hecho(null), i("chef siguiente"))
        assertEquals(OrdenVoz.Hecho(null), i("chef, ya está"))
    }

    @Test
    fun `pausa en sus variantes`() {
        assertEquals(OrdenVoz.Pausa(null), i("chef pausa"))
        assertEquals(OrdenVoz.Pausa(null), i("chef para el tiempo"))
        assertEquals(OrdenVoz.Pausa(null), i("chef espera"))
    }

    @Test
    fun `seguir en sus variantes`() {
        assertEquals(OrdenVoz.Seguir(null), i("chef sigue"))
        assertEquals(OrdenVoz.Seguir(null), i("chef continúa"))
        assertEquals(OrdenVoz.Seguir(null), i("chef reanuda"))
    }

    @Test
    fun `empezar en sus variantes`() {
        assertEquals(OrdenVoz.Empezar(null), i("chef empieza"))
        assertEquals(OrdenVoz.Empezar(null), i("chef arranca"))
        assertEquals(OrdenVoz.Empezar(null), i("chef, vamos"))
    }

    @Test
    fun `mas treinta con palabra, cifra y poco mas`() {
        assertEquals(OrdenVoz.MasTreinta(null), i("chef treinta segundos más"))
        assertEquals(OrdenVoz.MasTreinta(null), i("chef 30 más"))
        assertEquals(OrdenVoz.MasTreinta(null), i("chef 30 segundos más"))
        assertEquals(OrdenVoz.MasTreinta(null), i("chef un poco más"))
    }

    @Test
    fun `cuanto queda y cuanto falta`() {
        assertEquals(OrdenVoz.CuantoQueda(null), i("chef cuánto queda"))
        assertEquals(OrdenVoz.CuantoQueda(null), i("chef, ¿cuánto falta?"))
    }

    @Test
    fun `que toca y que hago`() {
        assertEquals(OrdenVoz.QueToca, i("chef qué toca"))
        assertEquals(OrdenVoz.QueToca, i("chef, ¿qué hago?"))
    }

    @Test
    fun `que viene, y luego y que sigue`() {
        assertEquals(OrdenVoz.QueViene, i("chef qué viene"))
        assertEquals(OrdenVoz.QueViene, i("chef y luego"))
        assertEquals(OrdenVoz.QueViene, i("chef qué sigue"))
    }

    @Test
    fun `para calla silencio y apaga la alarma`() {
        assertEquals(OrdenVoz.Para, i("chef para"))
        assertEquals(OrdenVoz.Para, i("chef calla"))
        assertEquals(OrdenVoz.Para, i("chef silencio"))
        assertEquals(OrdenVoz.Para, i("chef apaga la alarma"))
    }

    @Test
    fun `repite`() {
        assertEquals(OrdenVoz.Repite, i("chef repite"))
        assertEquals(OrdenVoz.Repite, i("chef otra vez"))
        assertEquals(OrdenVoz.Repite, i("chef, ¿qué has dicho?"))
    }

    // --- Carriles ---

    @Test
    fun `carril por nombre de una palabra`() {
        assertEquals(OrdenVoz.Hecho("olla"), i("chef hecho la olla"))
        assertEquals(OrdenVoz.Pausa("horno"), i("chef pausa el horno"))
        assertEquals(OrdenVoz.Seguir("mesa"), i("chef sigue la mesa"))
    }

    @Test
    fun `carril por adjetivo`() {
        assertEquals(OrdenVoz.Hecho("sg"), i("chef listo la grande"))
        assertEquals(OrdenVoz.Empezar("sp"), i("chef empieza la pequeña"))
    }

    @Test
    fun `carril por nombre completo con dos palabras`() {
        assertEquals(OrdenVoz.MasTreinta("sg"), i("chef 30 más la sartén grande"))
        assertEquals(OrdenVoz.CuantoQueda("sp"), i("chef cuánto queda de la sartén pequeña"))
    }

    @Test
    fun `carril ambiguo da null en el carril pero conserva la orden`() {
        assertEquals(OrdenVoz.Hecho(null), i("chef hecho la sartén"))
    }

    @Test
    fun `carril inexistente da null en el carril`() {
        assertEquals(OrdenVoz.Pausa(null), i("chef pausa la freidora"))
    }

    @Test
    fun `hecho para la sarten no se confunde con Para`() {
        assertEquals(OrdenVoz.Hecho("sg"), i("chef hecho para la sartén grande"))
    }

    @Test
    fun `para el tiempo de un carril es Pausa`() {
        assertEquals(OrdenVoz.Pausa("olla"), i("chef para el tiempo de la olla"))
    }

    @Test
    fun `sin carriles disponibles el carril es null`() {
        assertEquals(OrdenVoz.Hecho(null), InterpreteVoz.interpretar("chef hecho la olla", emptyList()))
    }

    // --- Palabra clave y errores del reconocedor ---

    @Test
    fun `chef mal reconocido`() {
        assertEquals(OrdenVoz.Hecho(null), i("chéf hecho"))
        assertEquals(OrdenVoz.Hecho(null), i("jef hecho"))
        assertEquals(OrdenVoz.Hecho(null), i("shef hecho"))
        assertEquals(OrdenVoz.Hecho(null), i("CHEF HECHO"))
    }

    @Test
    fun `solo se analiza lo que sigue a la ultima chef`() {
        assertEquals(OrdenVoz.Pausa(null), i("chef hecho chef pausa"))
        assertNull(i("chef hecho chef"))
    }

    @Test
    fun `numero en palabra y cifra equivalen`() {
        assertEquals(i("chef treinta más"), i("chef 30 más"))
    }

    // --- Negativos ---

    @Test
    fun `sin chef no hay orden`() {
        assertNull(i("hecho"))
        assertNull(i("pausa la olla"))
        assertNull(i("qué toca ahora"))
    }

    @Test
    fun `chef solo no es orden`() {
        assertNull(i("chef"))
        assertNull(i("  Chef.  "))
    }

    @Test
    fun `ruido tras chef no es orden`() {
        assertNull(i("chef cuéntame un chiste"))
        assertNull(i("chef qué tiempo hace mañana"))
        assertNull(i(""))
    }

    @Test
    fun `chef dentro de otra palabra no cuenta`() {
        assertNull(i("el chefcito hecho"))
    }

    // --- Alternativas del reconocedor ---

    @Test
    fun `alternativas devuelve la primera que da orden`() {
        val alternativas = listOf("hecho", "chef cuéntame algo", "chef listo la olla", "chef pausa")
        assertEquals(OrdenVoz.Hecho("olla"), InterpreteVoz.interpretar(alternativas, carriles))
    }

    @Test
    fun `alternativas sin ninguna orden da null`() {
        assertNull(InterpreteVoz.interpretar(listOf("hola", "chef"), carriles))
        assertNull(InterpreteVoz.interpretar(emptyList(), carriles))
    }
}
