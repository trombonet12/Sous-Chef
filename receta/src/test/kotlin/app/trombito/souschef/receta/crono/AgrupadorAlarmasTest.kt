package app.trombito.souschef.receta.crono

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AgrupadorAlarmasTest {
    private val nombres = mapOf("a" to "SARTÉN", "b" to "OLLA", "c" to "HORNO")

    private fun aviso(
        carril: String,
        tipo: TipoAviso,
        texto: String,
        instante: Long = 0L,
        silencioso: Boolean = false,
    ) = Aviso(tipo, carril, "p", texto, instante, silencioso)

    private fun l(
        carril: String,
        texto: String,
    ) = LineaAlarma(carril, texto)

    // --- lineasDe ---

    @Test
    fun `lineasDe una linea por carril en orden de aparicion con el nombre delante`() {
        val avisos =
            listOf(
                aviso("b", TipoAviso.FIN, "Hervir: hecho", 10),
                aviso("a", TipoAviso.HITO, "Dar la vuelta", 20),
            )
        assertEquals(
            listOf(l("b", "OLLA: Hervir: hecho"), l("a", "SARTÉN: Dar la vuelta")),
            AgrupadorAlarmas.lineasDe(avisos, nombres),
        )
    }

    @Test
    fun `lineasDe une FIN y TOCA_EMPEZAR del mismo carril con punto medio`() {
        val avisos =
            listOf(
                aviso("a", TipoAviso.FIN, "Freír: hecho", 100),
                aviso("b", TipoAviso.HITO, "Remover", 150),
                aviso("a", TipoAviso.TOCA_EMPEZAR, "Toca empezar: Dorar", 100),
            )
        assertEquals(
            listOf(
                l("a", "SARTÉN: Freír: hecho · Toca empezar: Dorar"),
                l("b", "OLLA: Remover"),
            ),
            AgrupadorAlarmas.lineasDe(avisos, nombres),
        )
    }

    @Test
    fun `lineasDe une los textos de un carril en orden temporal aunque lleguen desordenados`() {
        val avisos =
            listOf(
                aviso("a", TipoAviso.TOCA_EMPEZAR, "segundo", 200),
                aviso("a", TipoAviso.FIN, "primero", 100),
            )
        assertEquals(
            listOf(l("a", "SARTÉN: primero · segundo")),
            AgrupadorAlarmas.lineasDe(avisos, nombres),
        )
    }

    @Test
    fun `lineasDe ignora silenciosos`() {
        val avisos =
            listOf(
                aviso("a", TipoAviso.FIN, "viejo", 1, silencioso = true),
                aviso("b", TipoAviso.FIN, "vivo", 2),
            )
        assertEquals(listOf(l("b", "OLLA: vivo")), AgrupadorAlarmas.lineasDe(avisos, nombres))
    }

    @Test
    fun `lineasDe ignora tipos que no suenan`() {
        val avisos = TipoAviso.entries.filter { it !in sonoros }.map { aviso("a", it, "no suena") }
        assertTrue(avisos.all { it.tipo !in sonoros })
        assertEquals(emptyList(), AgrupadorAlarmas.lineasDe(avisos, nombres))
    }

    @Test
    fun `lineasDe sin nombre conocido usa el id del carril`() {
        assertEquals(
            listOf(l("z", "z: x")),
            AgrupadorAlarmas.lineasDe(listOf(aviso("z", TipoAviso.FIN, "x")), nombres),
        )
    }

    @Test
    fun `lineasDe sin avisos devuelve vacio`() {
        assertEquals(emptyList(), AgrupadorAlarmas.lineasDe(emptyList(), nombres))
    }

    // --- combinar ---

    @Test
    fun `combinar sin activa crea alarma nueva y suena`() {
        val r = AgrupadorAlarmas.combinar(null, listOf(l("a", "x")), 1_000L)
        assertEquals(AlarmaActiva(1_000L, listOf(l("a", "x"))), r.alarma)
        assertTrue(r.sonar)
    }

    @Test
    fun `combinar a 59999 ms fusiona sin sonar y mantiene el inicio`() {
        val activa = AlarmaActiva(1_000L, listOf(l("a", "x")))
        val r = AgrupadorAlarmas.combinar(activa, listOf(l("b", "y")), 1_000L + 59_999L)
        assertFalse(r.sonar)
        assertEquals(AlarmaActiva(1_000L, listOf(l("a", "x"), l("b", "y"))), r.alarma)
    }

    @Test
    fun `combinar a 60000 ms caduca y empieza alarma nueva que suena`() {
        val activa = AlarmaActiva(1_000L, listOf(l("a", "x")))
        val r = AgrupadorAlarmas.combinar(activa, listOf(l("b", "y")), 1_000L + 60_000L)
        assertTrue(r.sonar)
        assertEquals(AlarmaActiva(61_000L, listOf(l("b", "y"))), r.alarma)
    }

    @Test
    fun `combinar con la ventana pasada de sobra empieza alarma nueva`() {
        val activa = AlarmaActiva(0L, listOf(l("a", "x")))
        val r = AgrupadorAlarmas.combinar(activa, listOf(l("a", "z")), 500_000L)
        assertTrue(r.sonar)
        assertEquals(AlarmaActiva(500_000L, listOf(l("a", "z"))), r.alarma)
    }

    @Test
    fun `combinar sustituye la linea del mismo carril sin cambiar su posicion`() {
        val activa = AlarmaActiva(0L, listOf(l("a", "vieja"), l("b", "y")))
        val r = AgrupadorAlarmas.combinar(activa, listOf(l("a", "nueva")), 10_000L)
        assertFalse(r.sonar)
        assertEquals(listOf(l("a", "nueva"), l("b", "y")), r.alarma.lineas)
        assertEquals(0L, r.alarma.inicioMs)
    }

    @Test
    fun `combinar anade carriles nuevos al final en el orden recibido`() {
        val activa = AlarmaActiva(0L, listOf(l("a", "x")))
        val r = AgrupadorAlarmas.combinar(activa, listOf(l("c", "z"), l("b", "y"), l("a", "x2")), 5_000L)
        assertEquals(listOf(l("a", "x2"), l("c", "z"), l("b", "y")), r.alarma.lineas)
    }

    @Test
    fun `combinar con nuevas vacias y activa viva la devuelve igual sin sonar`() {
        val activa = AlarmaActiva(0L, listOf(l("a", "x")))
        val r = AgrupadorAlarmas.combinar(activa, emptyList(), 5_000L)
        assertEquals(activa, r.alarma)
        assertFalse(r.sonar)
    }

    @Test
    fun `combinar con nuevas vacias y activa caducada tambien la devuelve igual sin sonar`() {
        val activa = AlarmaActiva(0L, listOf(l("a", "x")))
        val r = AgrupadorAlarmas.combinar(activa, emptyList(), 90_000L)
        assertEquals(activa, r.alarma)
        assertFalse(r.sonar)
    }

    @Test
    fun `combinar con nuevas vacias y sin activa da alarma vacia sin sonar`() {
        val r = AgrupadorAlarmas.combinar(null, emptyList(), 7_000L)
        assertEquals(AlarmaActiva(7_000L, emptyList()), r.alarma)
        assertFalse(r.sonar)
    }

    @Test
    fun `combinar no muta la activa original`() {
        val activa = AlarmaActiva(0L, listOf(l("a", "x")))
        AgrupadorAlarmas.combinar(activa, listOf(l("b", "y")), 1_000L)
        assertEquals(AlarmaActiva(0L, listOf(l("a", "x"))), activa)
    }

    // --- quitar ---

    @Test
    fun `quitar de null devuelve null`() {
        assertNull(AgrupadorAlarmas.quitar(null, listOf("a")))
    }

    @Test
    fun `quitar elimina solo las lineas de esos carriles y conserva inicio y orden`() {
        val activa = AlarmaActiva(42L, listOf(l("a", "x"), l("b", "y"), l("c", "z")))
        assertEquals(
            AlarmaActiva(42L, listOf(l("a", "x"), l("c", "z"))),
            AgrupadorAlarmas.quitar(activa, listOf("b")),
        )
    }

    @Test
    fun `quitar hasta vaciar devuelve null`() {
        val activa = AlarmaActiva(0L, listOf(l("a", "x"), l("b", "y")))
        val tras1 = AgrupadorAlarmas.quitar(activa, setOf("a"))
        assertEquals(AlarmaActiva(0L, listOf(l("b", "y"))), tras1)
        assertNull(AgrupadorAlarmas.quitar(tras1, setOf("b")))
    }

    @Test
    fun `quitar varios carriles a la vez`() {
        val activa = AlarmaActiva(0L, listOf(l("a", "x"), l("b", "y")))
        assertNull(AgrupadorAlarmas.quitar(activa, listOf("a", "b")))
    }

    @Test
    fun `quitar carriles que no estan deja la alarma igual`() {
        val activa = AlarmaActiva(0L, listOf(l("a", "x")))
        assertEquals(activa, AgrupadorAlarmas.quitar(activa, listOf("q")))
    }

    @Test
    fun `quitar con lista vacia deja la alarma igual`() {
        val activa = AlarmaActiva(0L, listOf(l("a", "x")))
        assertEquals(activa, AgrupadorAlarmas.quitar(activa, emptyList()))
    }

    @Test
    fun `ventana de agrupacion es de 60 segundos`() {
        assertEquals(60_000L, AgrupadorAlarmas.VENTANA_MS)
    }

    private companion object {
        val sonoros = setOf(TipoAviso.FIN, TipoAviso.HITO, TipoAviso.TOCA_EMPEZAR)
    }
}
