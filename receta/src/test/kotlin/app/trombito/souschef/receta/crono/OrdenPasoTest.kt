package app.trombito.souschef.receta.crono

import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.TipoCocina
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OrdenPasoTest {
    private val fritura =
        OrdenPaso(
            titulo = "1ª fritura (blanqueo)",
            meto = "la fuente: 500 g de patata",
            fuego = 4,
            tapa = "sin tapa",
            segundos = 18 * 60,
            senal = "Tiernas y pálidas, sin color. Si se doran, baja al {3}.",
            saco = "la patata, al plato con papel",
        )

    private val gas = Cocina(TipoCocina.GAS, Mando.ANALOGICO, null)

    // ---- pantalla

    @Test
    fun `pantalla pone el origen de meto detras y saca cada pieza de su dato`() {
        val p = fritura.pantalla()
        assertEquals("500 g de patata · la fuente", p.mete)
        assertEquals("4", p.fuego)
        assertEquals("sin tapa", p.tapa)
        assertEquals("18 min", p.duracion)
        assertEquals("Tiernas y pálidas, sin color. Si se doran, baja al 3.", p.senal)
        assertEquals("la patata, al plato con papel", p.saca)
        assertEquals("fuego 4 · sin tapa · 18 min", p.detalle)
    }

    @Test
    fun `pantalla dice 4 a 5 cuando la potencia cambia respecto al paso anterior`() {
        assertEquals("4 → 5", fritura.copy(fuego = 5, fuegoAnterior = 4).pantalla().fuego)
        assertEquals("5", fritura.copy(fuego = 5, fuegoAnterior = 5).pantalla().fuego)
        assertEquals("5", fritura.copy(fuego = 5, fuegoAnterior = null).pantalla().fuego)
    }

    @Test
    fun `pantalla traduce el fuego a la cocina del usuario y no repite flecha si se leen igual`() {
        val digital12 = Cocina(TipoCocina.INDUCCION, Mando.DIGITAL, 12)
        assertEquals("5 → 7", fritura.copy(fuego = 5, fuegoAnterior = 4).pantalla(digital12).fuego)
        assertEquals("medio-alto", fritura.copy(fuego = 7).pantalla(gas).fuego)
        assertEquals("bajo", fritura.copy(fuego = 3, fuegoAnterior = 2).pantalla(gas).fuego)
        assertEquals("medio-alto", fritura.copy(fuego = 7, fuegoAnterior = 6).pantalla(gas).fuego)
    }

    @Test
    fun `pantalla muestra la tapa siempre que el paso la tenga y las tres formas`() {
        assertEquals("tapada", fritura.copy(tapa = "TAPA PUESTA").pantalla().tapa)
        assertEquals("tapa a medias", fritura.copy(tapa = "TAPA A MEDIAS").pantalla().tapa)
        assertEquals("sin tapa", fritura.copy(tapa = "tapa fuera").pantalla().tapa)
        assertNull(fritura.copy(tapa = null).pantalla().tapa)
        assertNull(fritura.copy(tapa = "algo raro").pantalla().tapa)
    }

    @Test
    fun `pantalla sin senal ni meto ni saca deja esas piezas vacias`() {
        val p = OrdenPaso(titulo = "Esperar", meto = "", senal = " ", saco = null, segundos = 45).pantalla()
        assertNull(p.mete)
        assertNull(p.senal)
        assertNull(p.saca)
        assertNull(p.fuego)
        assertEquals("45 s", p.duracion)
        assertEquals("45 s", p.detalle)
    }

    @Test
    fun `pantalla separa los elementos de meto con mas y cada uno con su origen`() {
        val o = OrdenPaso(titulo = "x", meto = "el bol: 200 g de cebolla + 8 g de ajo picado")
        assertEquals("200 g de cebolla · el bol + 8 g de ajo picado", o.pantalla().mete)
    }

    @Test
    fun `los dos puntos de una cantidad o de una medida no se toman por origen`() {
        assertEquals("2 huevos: M", OrdenPaso(titulo = "x", meto = "2 huevos: M").pantalla().mete)
        assertEquals("los 12 bunyols", OrdenPaso(titulo = "x", meto = "los 12 bunyols").pantalla().mete)
    }

    @Test
    fun `duracion en minutos y segundos`() {
        assertEquals("2 min 30 s", OrdenPaso.duracionCorta(150))
        assertEquals("90 min", OrdenPaso.duracionCorta(5400))
        assertNull(OrdenPaso.duracionCorta(0))
        assertNull(OrdenPaso.duracionCorta(null))
    }

    // ---- una linea

    @Test
    fun `linea es titulo meto y fuego sin origen tapa ni duracion`() {
        assertEquals("1ª fritura (blanqueo) · METE 500 g de patata · fuego 4", fritura.linea())
    }

    @Test
    fun `linea lleva 4 a 5 y traduce la potencia del titulo y del fuego`() {
        val o = OrdenPaso(titulo = "Subir al {5}", fuego = 5, fuegoAnterior = 4)
        assertEquals("Subir al 5 · fuego 4 → 5", o.linea())
        assertEquals("Subir a fuego medio · fuego medio-bajo → medio", o.linea(gas))
    }

    @Test
    fun `linea sin meto ni fuego es solo el titulo`() {
        assertEquals("Pelar y cortar", OrdenPaso(titulo = "Pelar y cortar").linea())
    }

    // ---- voz

    @Test
    fun `voz compone titulo meto fuego tapa y duracion en frases`() {
        assertEquals(
            "Sartén grande: primera fritura (blanqueo). Mete 500 gramos de patata desde la fuente. " +
                "Fuego 4, sin tapa. 18 minutos.",
            fritura.voz("Sartén grande"),
        )
    }

    @Test
    fun `voz usa el decir duracion que se le pase y puede callar la duracion`() {
        assertEquals(
            "Fuego 4, sin tapa. Dieciocho minutos.",
            fritura.detalleHablado(hablarDuracion = { "Dieciocho minutos" })?.substringAfter("la fuente. "),
        )
        assertEquals(
            "Sartén: primera fritura (blanqueo). Mete 500 gramos de patata desde la fuente. Fuego 4, sin tapa.",
            fritura.voz("Sartén", conDuracion = false),
        )
    }

    @Test
    fun `voz sin carril abre con el titulo en mayuscula y dice el cambio de fuego`() {
        val o = OrdenPaso(titulo = "subir al {5}", fuego = 5, fuegoAnterior = 4, segundos = 120)
        assertEquals("Subir al 5. Fuego de 4 a 5. 2 minutos.", o.voz())
    }

    @Test
    fun `voz pasa las unidades a palabra en singular y plural`() {
        val o = OrdenPaso(titulo = "x", meto = "1 g de sal + 1,5 kg de harina + 250 ml de leche + 1 l de agua")
        assertEquals(
            "Mete 1 gramo de sal, 1,5 kilos de harina, 250 mililitros de leche y 1 litro de agua.",
            o.meteHablado(),
        )
    }

    @Test
    fun `voz no toca una letra pegada a una unidad`() {
        assertEquals("Mete 2 gajos de limón.", OrdenPaso(titulo = "x", meto = "2 gajos de limón").meteHablado())
        assertEquals("Mete 3 lonchas.", OrdenPaso(titulo = "x", meto = "3 lonchas").meteHablado())
    }

    @Test
    fun `voz dice los ordinales y deja el titulo tal cual si no hay`() {
        assertEquals("Sartén: segunda fritura.", OrdenPaso(titulo = "2ª fritura").voz("Sartén"))
        assertEquals("Sartén: primer reposo.", OrdenPaso(titulo = "1º reposo").voz("Sartén"))
        assertEquals("Sartén: DOP al fuego.", OrdenPaso(titulo = "DOP al fuego").voz("Sartén"))
    }

    @Test
    fun `voz sin nada que decir mas que el titulo`() {
        assertEquals("Mesa: pelar.", OrdenPaso(titulo = "Pelar").voz("Mesa"))
    }

    @Test
    fun `saca y senal hablados`() {
        assertEquals("Saca la patata, al plato con papel.", fritura.sacaHablado())
        assertEquals("Tiernas y pálidas, sin color. Si se doran, baja al 3.", fritura.senalHablada())
        assertNull(fritura.copy(senal = null).senalHablada())
        assertNull(fritura.copy(saco = "").sacaHablado())
    }

    // ---- avisos de la alarma

    @Test
    fun `aviso de empezar y de hecho anaden lo que se mete y lo que se saca sin traducir la potencia`() {
        val o = fritura.copy(meto = "800 g de tomate + 2 g de sal", saco = "el plato: la patata al {3}")
        assertEquals(
            "Toca empezar: 1ª fritura (blanqueo) · mete 800 gramos de tomate y 2 gramos de sal",
            o.avisoEmpezar(),
        )
        assertEquals("1ª fritura (blanqueo): hecho · saca la patata al {3} desde el plato", o.avisoHecho())
    }

    @Test
    fun `aviso sin meto ni saco queda como antes`() {
        val o = OrdenPaso(titulo = "Baja al {3}")
        assertEquals("Toca empezar: Baja al {3}", o.avisoEmpezar())
        assertEquals("Baja al {3}: hecho", o.avisoHecho())
    }

    // ---- desde el paso

    @Test
    fun `de un paso y delCarril toman la potencia anterior del carril`() {
        fun paso(
            id: String,
            f: Int?,
        ) = Paso(id = id, t = id, carril = "c", off = 0.0, min = 2.0, f = f, meto = "algo", nota = "ojo")

        val pasos = listOf(paso("a", 4), paso("b", null), paso("c", 5))
        assertNull(OrdenPaso.delCarril(pasos, 0).fuegoAnterior)
        assertEquals(4, OrdenPaso.delCarril(pasos, 1).fuegoAnterior)
        val o = OrdenPaso.delCarril(pasos, 2)
        assertEquals(4, o.fuegoAnterior)
        assertEquals("4 → 5", o.pantalla().fuego)
        assertEquals(120, o.segundos)
        assertEquals("ojo", o.senal)
    }
}
