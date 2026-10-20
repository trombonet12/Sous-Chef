package app.trombito.souschef.ui.crono

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Hito
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tags
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.receta.conCocina
import app.trombito.souschef.receta.crono.ClaseEstado
import app.trombito.souschef.receta.crono.EstadoCarril
import app.trombito.souschef.receta.crono.EstadoCrono
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VistaCarrilTest {
    private val ahora = 1_800_000_000_000L
    private val servir = ahora + 15 * 60_000L

    /**
     * SARTÉN (fuego): Sofrito 5 min f5 tapa PUESTA con hitos a 150 s y 240 s; Reposo 10 min f8.
     * MESA: Cortar 2 min (depende de Sofrito); Emplatar 1 min.
     */
    private val receta =
        Receta(
            slug = "r",
            titulo = "R",
            totalMin = 15.0,
            carriles =
                listOf(
                    Carril("f0", nombre = "SARTÉN", color = "#E4572E", tipo = "fuego"),
                    Carril("m", nombre = "MESA", color = "#B0A899", tipo = "mesa"),
                ),
            pasos =
                listOf(
                    Paso(
                        "p1",
                        "Sofrito",
                        "f0",
                        0.0,
                        5.0,
                        f = 5,
                        tapa = "PUESTA",
                        meto = "Cebolla",
                        saco = "Nada",
                        nota = "Remueve",
                        hitos = listOf(Hito(150.0, "Dales la vuelta"), Hito(240.0, "Baja")),
                        p = "Porque sí",
                    ),
                    Paso("p2", "Reposo", "f0", 5.0, 10.0, f = 8),
                    Paso("p3", "Cortar", "m", 0.0, 2.0, dep = listOf("p1")),
                    Paso("p4", "Emplatar", "m", 2.0, 1.0),
                ),
        )

    private fun datos(
        f0: EstadoCarril = EstadoCarril(),
        m: EstadoCarril = EstadoCarril(),
        r: Receta = receta,
    ) = DatosCrono(
        r,
        EstadoCrono("r", 2, servir, mapOf("f0" to f0, "m" to m), actualizadoMs = ahora),
        ahora,
    )

    private fun vistas(
        f0: EstadoCarril = EstadoCarril(),
        m: EstadoCarril = EstadoCarril(),
        r: Receta = receta,
    ) = vistasCarriles(datos(f0, m, r))

    private fun corriendoCon(quedanS: Int) = EstadoCarril(finAtMs = ahora + quedanS * 1000L)

    // ---- vistasCarriles

    @Test
    fun `una vista por carril en el orden de la receta con indice y datos crudos`() {
        val v = vistas()
        assertEquals(listOf("f0", "m"), v.map { it.id })
        assertEquals(listOf(0, 1), v.map { it.indiceCarril })
        assertEquals("SARTÉN", v[0].nombre)
        assertEquals("#E4572E", v[0].colorHex)
        assertEquals("#B0A899", v[1].colorHex)
    }

    @Test
    fun `carril de fuego con potencia tapa y textos del paso`() {
        val f = vistas()[0]
        assertEquals(1, f.paso)
        assertEquals(2, f.pasos)
        assertEquals("Sofrito", f.titulo)
        assertEquals(5, f.fuego)
        assertEquals("PUESTA", f.tapa)
        assertEquals(300, f.duracionS)
        assertEquals("Cebolla", f.meto)
        assertEquals("Nada", f.saco)
        assertEquals("Remueve", f.nota)
    }

    @Test
    fun `carril mesa no tiene fuego aunque el paso lo traiga`() {
        val conF = receta.copy(pasos = receta.pasos.map { if (it.id == "p3") it.copy(f = 3) else it })
        val m = vistas(r = conF)[1]
        assertEquals("Cortar", m.titulo)
        assertNull(m.fuego)
        assertNull(m.tapa)
    }

    @Test
    fun `carril de fuego sin f no tiene potencia`() {
        val sinF = receta.copy(pasos = receta.pasos.map { if (it.id == "p1") it.copy(f = null) else it })
        assertNull(vistas(r = sinF)[0].fuego)
    }

    @Test
    fun `quedan fin sugerido y clase salen de las consultas`() {
        val corre = vistas(f0 = corriendoCon(200))[0]
        assertEquals(200, corre.quedanS)
        assertEquals(ahora + 200_000L, corre.finAtMs)
        assertEquals(ClaseEstado.MARCHA, corre.clase)

        val parado = vistas()[0]
        assertEquals(300, parado.quedanS)
        assertNull(parado.finAtMs)
        assertEquals(ClaseEstado.YA, parado.clase)
        assertEquals(ahora, parado.sugeridoMs)
    }

    @Test
    fun `quedan negativo cuando el reloj paso de cero`() {
        val v = vistas(f0 = corriendoCon(-45))[0]
        assertEquals(-45, v.quedanS)
        assertEquals(ClaseEstado.ATENDER, v.clase)
    }

    // ---- progreso e hitos

    @Test
    fun `progreso es la fraccion consumida`() {
        assertEquals(0f, vistas()[0].progreso, 0.0001f)
        assertEquals(0.5f, vistas(f0 = corriendoCon(150))[0].progreso, 0.0001f)
        assertEquals(1f, vistas(f0 = corriendoCon(-10))[0].progreso, 0.0001f)
    }

    @Test
    fun `hitos con fraccion y avisado y proximo hito el primero sin avisar`() {
        val v = vistas(f0 = corriendoCon(100).copy(hitosAvisados = setOf(0)))[0]
        assertEquals(2, v.hitos.size)
        assertEquals(MarcaHito(0.5f, "Dales la vuelta", true), v.hitos[0])
        assertEquals(0.8f, v.hitos[1].fraccion, 0.0001f)
        assertEquals("Baja", v.hitos[1].texto)
        assertFalse(v.hitos[1].avisado)
        assertEquals(v.hitos[1], v.proximoHito)
    }

    @Test
    fun `sin hitos pendientes el proximo hito es null`() {
        val todos = vistas(f0 = corriendoCon(30).copy(hitosAvisados = setOf(0, 1)))[0]
        assertNull(todos.proximoHito)
        assertTrue(todos.hitos.all { it.avisado })
        assertTrue(vistas()[1].hitos.isEmpty())
        assertNull(vistas()[1].proximoHito)
    }

    @Test
    fun `solo cuentan los hitos validos`() {
        val raros = listOf(Hito(0.0, "Cero"), Hito(300.0, "Final"), Hito(400.0, "Fuera"), Hito(100.0, ""))
        val r = receta.copy(pasos = receta.pasos.map { if (it.id == "p1") it.copy(hitos = raros) else it })
        val v = vistas(r = r)[0]
        assertEquals(listOf("Aviso"), v.hitos.map { it.texto })
    }

    // ---- despues

    @Test
    fun `despues es el siguiente paso con fuego en carril de fuego`() {
        assertEquals("Reposo · fuego 8", vistas()[0].despues)
    }

    @Test
    fun `despues dice el fuego en la cocina del usuario y trae los textos traducidos`() {
        val gas = Cocina(TipoCocina.GAS, Mando.ANALOGICO, null)
        val r =
            receta.copy(
                pasos = receta.pasos.map { if (it.id == "p2") it.copy(t = "Subir al {8}", f = 8) else it },
            )
        val carriles = mapOf("f0" to EstadoCarril(), "m" to EstadoCarril())
        val estado = EstadoCrono("r", 2, servir, carriles, actualizadoMs = ahora)
        val datos = DatosCrono(r, estado, ahora, gas, r.conCocina(gas))
        val v = vistasCarriles(datos)
        assertEquals("Subir a fuego alto · fuego alto", v[0].despues)
        assertEquals(gas, v[0].cocina)
        // El motor sigue con la receta canónica: ids, f y duraciones no cambian.
        val p2 = datos.receta.paso("p2")
        assertEquals(8, p2?.f)
        assertEquals("Subir al {8}", p2?.t)
    }

    @Test
    fun `despues en mesa es solo el titulo`() {
        assertEquals("Emplatar", vistas()[1].despues)
    }

    @Test
    fun `despues es null en el ultimo paso del carril`() {
        assertNull(vistas(f0 = EstadoCarril(i = 1))[0].despues)
        assertNull(vistas(m = EstadoCarril(i = 1))[1].despues)
        assertNull(vistas(f0 = EstadoCarril(i = 1))[0].despuesS)
    }

    @Test
    fun `despues trae la duracion del siguiente paso`() {
        assertEquals(600, vistas()[0].despuesS)
    }

    @Test
    fun `despues en fuego sin potencia en el siguiente es solo el titulo`() {
        val r = receta.copy(pasos = receta.pasos.map { if (it.id == "p2") it.copy(f = null) else it })
        assertEquals("Reposo", vistas(r = r)[0].despues)
    }

    // ---- acciones

    @Test
    fun `parado sin dependencias pendientes es EMPEZAR`() {
        val f = vistas()[0]
        assertEquals(AccionCarril.EMPEZAR, f.accion)
        assertNull(f.bloqueadoPor)
    }

    @Test
    fun `corriendo con tiempo es PAUSAR`() {
        assertEquals(AccionCarril.PAUSAR, vistas(f0 = corriendoCon(1))[0].accion)
    }

    @Test
    fun `corriendo vencido es HECHO tambien justo en cero`() {
        assertEquals(AccionCarril.HECHO, vistas(f0 = corriendoCon(0))[0].accion)
        assertEquals(AccionCarril.HECHO, vistas(f0 = corriendoCon(-30))[0].accion)
        assertTrue(vistas(f0 = corriendoCon(-30))[0].vencido)
    }

    @Test
    fun `pausado es SEGUIR`() {
        val v = vistas(f0 = EstadoCarril(pausado = true, restanteS = 120))[0]
        assertEquals(AccionCarril.SEGUIR, v.accion)
        assertEquals(120, v.quedanS)
        assertNull(v.finAtMs)
    }

    @Test
    fun `con requisitos pendientes y parado es BLOQUEADO con el nombre del bloqueo`() {
        val m = vistas()[1]
        assertEquals(AccionCarril.BLOQUEADO, m.accion)
        assertEquals("Sofrito (SARTÉN)", m.bloqueadoPor)
        assertEquals(ClaseEstado.BLOQ, m.clase)
    }

    @Test
    fun `empezado a pesar del bloqueo deja de estar BLOQUEADO`() {
        val m = vistas(m = EstadoCarril(salto = true))[1]
        assertEquals(AccionCarril.EMPEZAR, m.accion)
        assertNull(m.bloqueadoPor)
    }

    @Test
    fun `corriendo o pausado con requisitos pendientes no esta bloqueado`() {
        assertEquals(AccionCarril.PAUSAR, vistas(m = corriendoCon(60))[1].accion)
        assertEquals(AccionCarril.SEGUIR, vistas(m = EstadoCarril(pausado = true))[1].accion)
    }

    @Test
    fun `cumplido el requisito el carril mesa vuelve a EMPEZAR`() {
        val m = vistas(f0 = EstadoCarril(i = 1))[1]
        assertEquals(AccionCarril.EMPEZAR, m.accion)
        assertNull(m.bloqueadoPor)
    }

    @Test
    fun `carril terminado es TERMINADO sin paso ni titulo`() {
        val m = vistas(m = EstadoCarril(i = 1, fin = true))[1]
        assertEquals(AccionCarril.TERMINADO, m.accion)
        assertEquals(0, m.paso)
        assertEquals(2, m.pasos)
        assertNull(m.titulo)
        assertNull(m.fuego)
        assertNull(m.despues)
        assertNull(m.finAtMs)
        assertNull(m.proximoHito)
        assertTrue(m.hitos.isEmpty())
        assertEquals(0, m.quedanS)
        assertEquals(ClaseEstado.FIN, m.clase)
        assertTrue(m.terminado)
    }

    // ---- tienePorque

    @Test
    fun `tienePorque con p del paso`() {
        assertTrue(vistas()[0].tienePorque)
        assertFalse(vistas()[1].tienePorque)
    }

    @Test
    fun `tienePorque con p en blanco es falso y con tecnicas enlazadas es verdadero`() {
        val enBlanco = receta.copy(pasos = receta.pasos.map { if (it.id == "p1") it.copy(p = "  ") else it })
        assertFalse(vistas(r = enBlanco)[0].tienePorque)
        val conTecnica = enBlanco.copy(tags = Tags(tecnica = listOf("sofrito")))
        assertTrue(vistas(r = conTecnica)[0].tienePorque)
    }

    @Test
    fun `porque es el texto p del paso actual o null`() {
        assertEquals("Porque sí", vistas()[0].porque)
        assertNull(vistas()[1].porque)
        val enBlanco = receta.copy(pasos = receta.pasos.map { if (it.id == "p1") it.copy(p = "  ") else it })
        assertNull(vistas(r = enBlanco)[0].porque)
    }

    // ---- elegirFoco

    private fun v(
        id: String,
        accion: AccionCarril = AccionCarril.EMPEZAR,
        finAtMs: Long? = null,
        clase: ClaseEstado = ClaseEstado.ESPERA,
        quedanS: Int = 60,
    ) = VistaCarril(
        id = id,
        nombre = id.uppercase(),
        colorHex = "#B0A899",
        indiceCarril = 0,
        paso = if (accion == AccionCarril.TERMINADO) 0 else 1,
        pasos = 2,
        titulo = if (accion == AccionCarril.TERMINADO) null else "Paso",
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
        sugeridoMs = null,
        finAtMs = finAtMs,
        tienePorque = false,
    )

    private fun corre(
        id: String,
        finAtMs: Long,
    ) = v(id, AccionCarril.PAUSAR, finAtMs, ClaseEstado.MARCHA)

    private fun vencido(
        id: String,
        finAtMs: Long,
    ) = v(id, AccionCarril.HECHO, finAtMs, ClaseEstado.ATENDER, quedanS = -5)

    private fun terminado(id: String) = v(id, AccionCarril.TERMINADO, clase = ClaseEstado.FIN, quedanS = 0)

    @Test
    fun `foco lista vacia es null`() {
        assertNull(elegirFoco(emptyList(), null))
    }

    @Test
    fun `foco vencido gana a corriendo`() {
        val lista = listOf(corre("a", 1_000), vencido("b", 5_000), v("c", clase = ClaseEstado.YA))
        assertEquals("b", elegirFoco(lista, null))
    }

    @Test
    fun `foco entre varios vencidos el de fin mas antiguo`() {
        val lista = listOf(vencido("a", 9_000), vencido("b", 2_000), vencido("c", 5_000))
        assertEquals("b", elegirFoco(lista, null))
    }

    @Test
    fun `foco corriendo con menor finAt`() {
        val lista = listOf(corre("a", 9_000), corre("b", 2_000), v("c", clase = ClaseEstado.YA))
        assertEquals("b", elegirFoco(lista, null))
    }

    @Test
    fun `foco corriendo gana a YA`() {
        val lista = listOf(v("a", clase = ClaseEstado.YA), corre("b", 9_000))
        assertEquals("b", elegirFoco(lista, null))
    }

    @Test
    fun `foco YA cuando nadie corre`() {
        val lista =
            listOf(v("a", clase = ClaseEstado.ESPERA), v("b", clase = ClaseEstado.YA), v("c", clase = ClaseEstado.YA))
        assertEquals("b", elegirFoco(lista, null))
    }

    @Test
    fun `foco el primero no terminado si nada urgente`() {
        val lista = listOf(terminado("a"), v("b", clase = ClaseEstado.ESPERA), v("c", clase = ClaseEstado.BLOQ))
        assertEquals("b", elegirFoco(lista, null))
    }

    @Test
    fun `foco fijado se respeta sobre corriendo y YA`() {
        val lista = listOf(corre("a", 1_000), v("b", clase = ClaseEstado.YA), v("c", clase = ClaseEstado.ESPERA))
        assertEquals("c", elegirFoco(lista, "c"))
    }

    @Test
    fun `foco un vencido de otro carril gana al fijado`() {
        val lista = listOf(v("a", clase = ClaseEstado.YA), vencido("b", 5_000))
        assertEquals("b", elegirFoco(lista, "a"))
    }

    @Test
    fun `foco fijado terminado se ignora`() {
        val lista = listOf(terminado("a"), corre("b", 3_000), v("c", clase = ClaseEstado.YA))
        assertEquals("b", elegirFoco(lista, "a"))
    }

    @Test
    fun `foco fijado inexistente se ignora`() {
        val lista = listOf(corre("a", 3_000))
        assertEquals("a", elegirFoco(lista, "zzz"))
    }

    @Test
    fun `foco lista toda terminada es null aunque haya fijado`() {
        val lista = listOf(terminado("a"), terminado("b"))
        assertNull(elegirFoco(lista, null))
        assertNull(elegirFoco(lista, "a"))
    }

    @Test
    fun `foco sobre vistas reales con un carril vencido`() {
        val lista = vistas(f0 = corriendoCon(-20), m = EstadoCarril(salto = true))
        assertEquals("f0", elegirFoco(lista, "m"))
    }

    @Test
    fun `desvio cero si se empieza a la hora prevista`() {
        assertEquals(0, desvioMin(0, 300, finPrevistoMs(0, 300, false, null, 0)))
    }

    @Test
    fun `esperar antes de la hora prevista no es ir pronto`() {
        assertEquals(0, desvioMin(600_000, 300, finPrevistoMs(600_000, 300, false, null, 0)))
    }

    @Test
    fun `sin empezar y pasada la hora prevista va tarde`() {
        assertEquals(3, desvioMin(0, 300, finPrevistoMs(0, 300, false, null, 180_000)))
    }

    @Test
    fun `corriendo con el fin antes de lo previsto va pronto`() {
        // previsto: empezar en 0 y acabar en 300 s; corre desde -120 s, así que acaba en 180 s.
        assertEquals(-2, desvioMin(0, 300, finPrevistoMs(0, 180, false, 180_000, 0)))
    }

    @Test
    fun `una prorroga o un cero pasado cuentan como retraso`() {
        assertEquals(1, desvioMin(0, 300, finPrevistoMs(0, 360, false, 360_000, 0)))
        assertEquals(4, desvioMin(0, 300, finPrevistoMs(0, -240, false, 300_000, 540_000)))
    }

    @Test
    fun `en pausa cuenta lo que queda desde ahora`() {
        assertEquals(2, desvioMin(0, 300, finPrevistoMs(0, 120, true, null, 300_000)))
    }

    @Test
    fun `texto del desvio con signo`() {
        assertEquals("+3", textoDesvio(3))
        assertEquals("−2", textoDesvio(-2))
        assertEquals(null, textoDesvio(0))
    }
}
