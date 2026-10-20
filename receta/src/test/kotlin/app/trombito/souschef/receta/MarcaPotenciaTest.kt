package app.trombito.souschef.receta

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarcaPotenciaTest {
    private fun marca(t: String) = MarcaPotencia.marcar(t).texto

    private fun igual(
        esperado: String,
        original: String,
    ) = assertEquals(esperado, marca(original))

    @Test
    fun `marca baja al N sube al N y enciende al N`() {
        igual("Baja al {5} y tapa.", "Baja al 5 y tapa.")
        igual("Sube al {8} y aparta.", "Sube al 8 y aparta.")
        igual("enciendes al {6}: la grasa", "enciendes al 6: la grasa")
        igual("Un minuto al {7} hasta que brille", "Un minuto al 7 hasta que brille")
    }

    @Test
    fun `marca del N al M los dos`() {
        igual("baja del {8} al {7} y ahí se queda", "baja del 8 al 7 y ahí se queda")
        igual("(del {4} al {5} y del {5} al {8})", "(del 4 al 5 y del 5 al 8)")
    }

    @Test
    fun `marca fuego potencia y posicion`() {
        igual("A fuego {8} con el aceite", "A fuego 8 con el aceite")
        igual("agua ya hirviendo con sal, potencia {8}", "agua ya hirviendo con sal, potencia 8")
        igual("a potencia {1}", "a potencia 1")
        igual("en la posición {2} del horno", "en la posición 2 del horno")
    }

    @Test
    fun `marca rangos de potencia`() {
        igual("potencia {6}-{7}", "potencia 6-7")
        igual("Calentar sin prisa, potencia {4}-{5}", "Calentar sin prisa, potencia 4-5")
    }

    @Test
    fun `marca entre el N y el M`() {
        igual("entre el {3} y el {4}", "entre el 3 y el 4")
        igual("entre el minuto 4 y el 10", "entre el minuto 4 y el 10")
    }

    @Test
    fun `no marca unidades ni contadores`() {
        for (t in listOf(
            "al 5 min",
            "al 2 minutos",
            "al 3 s",
            "al 3 h",
            "al 50%",
            "al 2 °C",
            "al 3 g",
            "al 4 ml",
            "al 2 veces",
            "la tanda al 3 tandas",
            "al 2 pasos",
            "al 2 raciones",
            "al 3 huevos",
            "al 2 dientes",
            "al 1 cucharada",
            "al 2 vasos",
            "al 15",
            "al 2,5 de",
            "al 1.5 kg",
            "al 1/2",
            "al 2º",
        )) {
            assertEquals(t, marca(t), t)
        }
    }

    @Test
    fun `un numero seguido de de es dudoso y no se marca`() {
        val r = MarcaPotencia.marcar("Baja del 1 al 3 de los fuegos")
        assertEquals("Baja del 1 al 3 de los fuegos", r.texto)
        assertTrue(r.cambios.isEmpty())
        assertEquals(listOf(1, 3), r.dudosos.map { it.numero })
        assertEquals("Sube al 8 de golpe", MarcaPotencia.marcar("Sube al 8 de golpe").texto)
    }

    @Test
    fun `del N suelto es dudoso`() {
        val r = MarcaPotencia.marcar("por encima del 7 se quema")
        assertEquals("por encima del 7 se quema", r.texto)
        assertEquals(listOf(7), r.dudosos.map { it.numero })
    }

    @Test
    fun `sube a N y manten el N son dudosos`() {
        val r = MarcaPotencia.marcar("Sube a 9 y espera. Mantén el 8 hasta que hierva. Baja a 3 min")
        assertTrue(r.cambios.isEmpty())
        assertEquals(listOf(9, 8), r.dudosos.map { it.numero })
    }

    @Test
    fun `la escala en si es dudosa`() {
        val r = MarcaPotencia.marcar("Vitrocerámica, potencias del 0 al 9.")
        assertEquals("Vitrocerámica, potencias del 0 al 9.", r.texto)
        assertEquals(listOf(0, 9), r.dudosos.map { it.numero })
        assertTrue(r.cambios.isEmpty())
    }

    @Test
    fun `no marca numeros mayores ni decimales`() {
        igual("al 10 y al 25,5", "al 10 y al 25,5")
        igual("a fuego 12", "a fuego 12")
    }

    @Test
    fun `es idempotente y respeta lo ya marcado`() {
        val ya = "Baja al {3} y del {4} al {5}, potencia {6}-{7}"
        val r = MarcaPotencia.marcar(ya)
        assertEquals(ya, r.texto)
        assertTrue(r.cambios.isEmpty())
        val una = marca("Baja al 3 y del 4 al 5, potencia 6-7")
        assertEquals(una, marca(una))
    }

    @Test
    fun `completa un par a medio marcar`() {
        igual("del {4} al {5}", "del {4} al 5")
    }

    @Test
    fun `candidatos devuelve posicion numero y contexto sin llaves`() {
        val c = MarcaPotencia.candidatos("Baja al 3 y tapa. Ya al {5}.")
        assertEquals(1, c.size)
        assertEquals(3, c[0].numero)
        assertEquals(8, c[0].posicion)
        assertEquals("Baja al 3 y tapa. Ya al {5}.", c[0].contexto)
    }

    @Test
    fun `el contexto se limita a 40 caracteres a cada lado`() {
        val relleno = "x".repeat(60)
        val c = MarcaPotencia.candidatos("$relleno baja al 3 $relleno").single()
        assertEquals(40 + 1 + 40, c.contexto.length)
    }

    @Test
    fun `CamposDeTexto solo toca los campos de texto libre`() {
        val doc =
            Json.parseToJsonElement(
                """{"slug":"al 3","titulo":"fuego 4","descripcion":"baja al 2",
                |"pasos":[{"id":"p1","t":"sube al 8","carril":"al 5","hitos":[{"en":3,"t":"baja al 4"}]}],
                |"porque":[["a","fuego 7","fuego 9"]],"mep":[{"t":"x","l":["al 6"]}]}
                """.trimMargin(),
            ) as JsonObject
        val vistos = mutableListOf<String>()
        val nuevo =
            CamposDeTexto.transformar(doc, true) { campo, texto ->
                vistos += campo
                MarcaPotencia.marcar(texto).texto
            }
        val s = nuevo.toString()
        assertTrue("\"slug\":\"al 3\"" in s && "\"titulo\":\"fuego 4\"" in s && "\"carril\":\"al 5\"" in s, s)
        assertTrue("baja al {2}" in s && "sube al {8}" in s && "baja al {4}" in s && "fuego {7}" in s, s)
        assertTrue("fuego 9" in s && "al {6}" in s, s)
        assertTrue("pasos[p1].t" in vistos && "pasos[p1].hitos[0].t" in vistos && "porque[0][1]" in vistos, "$vistos")
    }
}
