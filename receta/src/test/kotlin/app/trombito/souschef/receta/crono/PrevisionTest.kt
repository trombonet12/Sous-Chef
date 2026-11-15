package app.trombito.souschef.receta.crono

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.RecetaJson
import app.trombito.souschef.receta.Validador
import app.trombito.souschef.receta.leerReceta
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ADR-0018 «El plan sigue al cocinero»: los 25 escenarios. Receta sintética R (total 37 min) y huevos rotos real.
 * `S` = hora de servir; `T0 = S − 37 min`.
 */
@Suppress("TooManyFunctions")
class PrevisionTest {
    private val s0 = 1_700_000_000_000L
    private val min = 60_000L
    private val t0 = s0 - 37 * min

    private fun m(n: Int): Long = n * min

    @Suppress("LongParameterList")
    private fun paso(
        id: String,
        carril: String,
        off: Double,
        s: Int,
        dep: List<String> = emptyList(),
        junto: String? = null,
    ) = Paso(id = id, t = "T-$id", carril = carril, off = off, min = s / 60.0, s = s, dep = dep, junto = junto)

    private val r =
        Receta(
            slug = "r",
            titulo = "R",
            totalMin = 37.0,
            carriles =
                listOf(
                    Carril("mesa", "MESA", "#fff", "mesa"),
                    Carril("sarten", "SARTEN", "#fff", "fuego"),
                    Carril("olla", "OLLA", "#fff", "fuego"),
                    Carril("horno", "HORNO", "#fff", "horno"),
                ),
            pasos =
                listOf(
                    paso("m1", "mesa", 0.0, 600),
                    paso("s1", "sarten", 10.0, 300, dep = listOf("m1")),
                    paso("s2", "sarten", 15.0, 900),
                    paso("o1", "olla", 20.0, 300, junto = "s2"),
                    paso("o2", "olla", 25.0, 600),
                    paso("h1", "horno", 12.0, 600),
                    paso("h2", "horno", 30.0, 300),
                    paso("m2", "mesa", 35.0, 120, dep = listOf("s2", "o2")),
                ),
        )

    /** Cocina abierta una hora antes: nada arrancado, nada avisado. */
    private fun nueva(servir: Long = s0): EstadoCrono =
        SesionCrono.abrir(SesionCrono.inicial(r, servir, t0 - m(60)), r, t0 - m(60))

    private fun EstadoCrono.empieza(
        carril: String,
        t: Long,
    ) = MotorCrono.empezar(this, r, carril, t)

    private fun EstadoCrono.hecho(
        carril: String,
        t: Long,
    ) = MotorCrono.hecho(this, r, carril, t)

    private fun EstadoCrono.previstos(ahora: Long) = ConsultasCrono.previstos(this, r, ahora)

    private fun EstadoCrono.previsto(
        id: String,
        ahora: Long,
    ): PasoPrevisto = checkNotNull(previstos(ahora)[id]) { "sin previsto de $id" }

    private fun EstadoCrono.inicio(
        id: String,
        ahora: Long,
    ) = previsto(id, ahora).inicioMs

    private fun EstadoCrono.llegada(ahora: Long) = ConsultasCrono.plan(this, r, ahora).llegadaMs

    private fun EstadoCrono.evento(ahora: Long) = AvisosCrono.siguienteEvento(this, r, ahora)

    private fun EstadoCrono.eventoFuera(
        excepto: String,
        ahora: Long,
    ) = AvisosCrono.siguienteEventoFuera(this, r, excepto, ahora)

    /** Estado base B (ahora `T0+23`). */
    private fun estadoB(): EstadoCrono =
        nueva()
            .empieza("mesa", t0)
            .hecho("mesa", t0 + m(10))
            .empieza("sarten", t0 + m(10))
            .hecho("sarten", t0 + m(15))
            .empieza("sarten", t0 + m(15))
            .empieza("horno", t0 + m(12))
            .hecho("horno", t0 + m(22))
            .empieza("olla", t0 + m(20))

    private val ahoraB get() = t0 + m(23)

    // ---- 1 · invariante sin desvíos ----

    @Test
    fun e01InvarianteSinDesviosEnLas33Recetas() {
        val recetas = File("../data/recetas").listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }
        assertTrue(recetas.size >= 33, "esperaba las 33 recetas, hay ${recetas.size}")
        for (f in recetas) {
            val receta = leerReceta(f.readText())
            val totalMs = Plano.medioMinutoMs(receta.totalEfectivo)
            val servir = s0
            val ahora = servir - totalMs - m(60)
            val e = SesionCrono.inicial(receta, servir, ahora)
            val previstos = ConsultasCrono.previstos(e, receta, ahora)
            for (p in receta.pasos) {
                val esperado = servir - totalMs + Plano.medioMinutoMs(p.off)
                val real = previstos.getValue(p.id)
                assertEquals(esperado, real.inicioMs, "${receta.slug}/${p.id}")
                assertTrue(real.firme, "${receta.slug}/${p.id} debería ser firme")
            }
            val llegada = servir - totalMs + receta.pasos.maxOf { Plano.medioMinutoMs(it.off) + it.segundos * 1000L }
            assertEquals(llegada, ConsultasCrono.plan(e, receta, ahora).llegadaMs, receta.slug)
        }
    }

    // ---- 2 · abrir antes sin arrancar ----

    @Test
    fun e02AbrirAntesSinArrancarNoAdelantaLaLlegada() {
        val ahora = t0 - m(10)
        val e = SesionCrono.abrir(SesionCrono.inicial(r, s0, ahora), r, ahora)
        for (p in r.pasos) assertEquals(t0 + Plano.medioMinutoMs(p.off), e.inicio(p.id, ahora), p.id)
        val ev = checkNotNull(e.evento(ahora))
        assertEquals(TipoAviso.TOCA_EMPEZAR, ev.tipo)
        assertEquals("T-m1", ev.paso)
        assertEquals(t0, ev.ms)
        assertEquals(s0, e.llegada(ahora))
    }

    // ---- 3 · arranque adelantado ----

    @Test
    fun e03ArranqueAdelantadoMueveTodoYSoloM1Avisa() {
        val ahora = t0 - m(10)
        val e = nueva().empieza("mesa", ahora)
        assertEquals(t0 + m(2), e.inicio("h1", ahora))
        assertEquals(t0, e.inicio("s1", ahora))
        assertEquals(t0 + m(5), e.inicio("s2", ahora))
        assertEquals(t0 + m(10), e.inicio("o1", ahora))
        assertTrue(e.previsto("o1", ahora).firme)
        assertEquals(t0 + m(20), e.inicio("h2", ahora))
        assertEquals(t0 + m(25), e.inicio("m2", ahora))
        assertEquals(s0 - m(10), e.llegada(ahora))
        val ev = checkNotNull(e.evento(ahora))
        assertEquals(TipoAviso.FIN, ev.tipo)
        assertEquals("T-m1", ev.paso)
        assertEquals(t0, ev.ms)
        // s1 está bloqueado: no hay «toca empezar» suyo.
        assertTrue(AvisosCrono.vencidos(e, r, t0 + m(1)).avisos.none { it.carril == "sarten" })
    }

    // ---- 4 · hecho adelantado ----

    @Test
    fun e04HechoAdelantadoAvisaAlOtroCarrilEnElActo() {
        val ahora = t0 + m(5)
        val e = nueva().empieza("mesa", t0).hecho("mesa", ahora)
        assertEquals(t0 + m(5), e.inicio("s1", ahora))
        assertTrue(e.previsto("s1", ahora).firme)
        assertEquals(t0 + m(15), e.inicio("o1", ahora))
        assertEquals(t0 + m(30), e.inicio("m2", ahora))
        assertEquals(t0 + m(12), e.inicio("h1", ahora))
        assertEquals(t0 + m(30), e.inicio("h2", ahora))
        assertEquals(t0 + m(35), e.llegada(ahora))
        val avisos = AvisosCrono.vencidos(e, r, ahora).avisos
        val s1 = avisos.single()
        assertEquals("sarten", s1.carril)
        assertEquals(TipoAviso.TOCA_EMPEZAR, s1.tipo)
        assertFalse(s1.silencioso)
    }

    // ---- 5 · prórroga ----

    @Test
    fun e05ProrrogaMueveLosDependientesLoMismo() {
        val antes = estadoB()
        var e = antes
        repeat(2) { e = MotorCrono.masTreinta(e, r, "sarten", ahoraB) }
        assertEquals(t0 + m(31), checkNotNull(e.carriles["sarten"]?.finAtMs) - 0L)
        assertEquals(t0 + m(36), e.inicio("m2", ahoraB))
        assertEquals(s0 + m(1), e.llegada(ahoraB))
        for (id in listOf("o1", "o2", "h2")) {
            assertEquals(antes.previsto(id, ahoraB), e.previsto(id, ahoraB), id)
        }
    }

    // ---- 6 · pausa ----

    private fun estadoPausa(): EstadoCrono =
        MotorCrono
            .pausar(estadoB(), r, "sarten", t0 + m(23))
            .hecho("olla", t0 + m(25))
            .empieza("olla", t0 + m(25))

    @Test
    fun e06PausaHaceFlotanteAlDependienteYALaLlegada() {
        val e = estadoPausa()
        assertEquals(420, e.carriles.getValue("sarten").restanteS)
        val a26 = t0 + m(26)
        assertEquals(t0 + m(38), e.inicio("m2", a26))
        assertFalse(e.previsto("m2", a26).firme)
        assertEquals(t0 + m(40), e.llegada(a26))
        val a27 = t0 + m(27)
        assertEquals(t0 + m(39), e.inicio("m2", a27))
        assertEquals(t0 + m(41), e.llegada(a27))
        assertEquals(e.previsto("o2", a26), e.previsto("o2", a27))
        assertEquals(e.previsto("h2", a26), e.previsto("h2", a27))
    }

    // ---- 7 · junto ancla al arranque ----

    private fun estadoH8() =
        nueva()
            .empieza("mesa", t0)
            .hecho("mesa", t0 + m(5))
            .empieza("sarten", t0 + m(5))
            .hecho("sarten", t0 + m(8))
            .empieza("sarten", t0 + m(8))

    @Test
    fun e07JuntoAnclaAlArranqueDelCompanero() {
        val ahora = t0 + m(8)
        val e = estadoH8()
        assertEquals(t0 + m(13), e.inicio("o1", ahora))
        assertTrue(e.previsto("o1", ahora).firme)
        val ev = checkNotNull(e.eventoFuera("horno", ahora))
        assertEquals(TipoAviso.TOCA_EMPEZAR, ev.tipo)
        assertEquals("T-o1", ev.paso)
        assertEquals(t0 + m(13), ev.ms)
    }

    // ---- 8 · junto a un compañero sin arrancar ----

    @Test
    fun e08JuntoAUnCompaneroSinArrancarSigueSuCadena() {
        val ahora = t0 + m(1)
        val e = nueva().empieza("mesa", t0)
        assertEquals(t0 + m(20), e.inicio("o1", ahora))
        assertTrue(e.previsto("o1", ahora).firme)
        val mas = MotorCrono.masTreinta(e, r, "mesa", ahora)
        assertEquals(t0 + m(15) + 30_000L, mas.inicio("s2", ahora))
        assertEquals(t0 + m(20) + 30_000L, mas.inicio("o1", ahora))
        assertTrue(mas.previsto("o1", ahora).firme)
    }

    // ---- 9 · compañero atrasado ----

    private fun estadoCompaneroAtrasado() =
        nueva()
            .empieza("mesa", t0)
            .hecho("mesa", t0 + m(10))
            .empieza("sarten", t0 + m(10))
            .hecho("sarten", t0 + m(15))
            .empieza("horno", t0 + m(12))

    @Test
    fun e09CompaneroAtrasadoDejaLaHoraSinFirmar() {
        val ahora = t0 + m(18)
        val e = estadoCompaneroAtrasado()
        assertEquals(t0 + m(15), e.inicio("s2", ahora))
        assertEquals(t0 + m(23), e.inicio("o1", ahora))
        assertFalse(e.previsto("o1", ahora).firme)
        assertNull(e.eventoFuera("horno", ahora))

        val tras = e.empieza("sarten", t0 + m(19))
        val a19 = t0 + m(19)
        assertEquals(t0 + m(24), tras.inicio("o1", a19))
        assertTrue(tras.previsto("o1", a19).firme)
        val ev = checkNotNull(tras.eventoFuera("horno", a19))
        assertEquals("T-o1", ev.paso)
        assertEquals(t0 + m(24), ev.ms)
    }

    // ---- 10 · raíz con off > 0 sin arrancar la cocina ----

    @Test
    fun e10RaizConOffMayorQueCeroSigueAAhoraHastaArrancar() {
        val ahora = t0 + m(3)
        val e = nueva()
        assertEquals(t0, e.inicio("m1", ahora))
        assertEquals(ClaseEstado.YA, ConsultasCrono.vista(e, r, "mesa", ahora).clase)
        assertEquals(t0 + m(15), e.inicio("h1", ahora))
        assertFalse(e.previsto("h1", ahora).firme)
        assertTrue(AvisosCrono.vencidos(e, r, ahora).avisos.none { it.carril == "horno" })
        assertTrue(e.evento(ahora)?.carril != "horno")

        val a4 = t0 + m(4)
        val tras = e.empieza("mesa", a4)
        assertEquals(t0 + m(16), tras.inicio("h1", a4))
        assertTrue(tras.previsto("h1", a4).firme)
        val ev = checkNotNull(tras.evento(a4))
        assertEquals(TipoAviso.FIN, ev.tipo)
        assertEquals("T-m1", ev.paso)
        assertEquals(t0 + m(14), ev.ms)
    }

    // ---- 11 · hecho sin empezar ----

    @Test
    fun e11HechoSinEmpezarFijaElArranqueDeLaCocina() {
        val ahora = t0 + m(12)
        val e = nueva().hecho("mesa", ahora)
        assertTrue(e.inicioRealMs.isEmpty())
        assertEquals(t0 + m(14), e.inicio("h1", ahora))
        assertEquals(t0 + m(12), e.inicio("s1", ahora))
        val avisos = AvisosCrono.vencidos(e, r, ahora).avisos
        assertTrue(avisos.any { it.carril == "sarten" && it.tipo == TipoAviso.TOCA_EMPEZAR })
    }

    // ---- 12 · «Empezar igual» ----

    @Test
    fun e12EmpezarIgualAnotaElArranqueYFijaLaLlegada() {
        val e = MotorCrono.empezarIgual(estadoB(), r, "mesa", ahoraB)
        val ec = e.carriles.getValue("mesa")
        assertTrue(ec.salto)
        assertEquals(t0 + m(25), ec.finAtMs)
        assertEquals(t0 + m(23), e.inicioRealMs["m2"])
        assertEquals(t0 + m(35), e.llegada(ahoraB))
    }

    // ---- 13 · deshacer ----

    @Test
    fun e13DeshacerDevuelveExactamenteLasHorasDeAntes() {
        val ahora = t0 + m(5)
        val antes = nueva().empieza("mesa", t0)
        val hecho = antes.hecho("mesa", ahora)
        val deshecho = MotorCrono.deshacer(hecho, r, "mesa", ahora)
        assertFalse("m1" in deshecho.finRealMs)
        assertFalse("m1" in deshecho.pasosReales)
        assertTrue(deshecho.carriles.getValue("mesa").corriendo)
        assertEquals(antes.previstos(ahora), deshecho.previstos(ahora))
    }

    // ---- 14 · idempotencia ----

    @Test
    fun e14Idempotencia() {
        val ahora = t0 + m(5)
        val e = nueva().empieza("mesa", t0).hecho("mesa", ahora)
        val primera = AvisosCrono.vencidos(e, r, ahora)
        assertTrue(primera.avisos.isNotEmpty())
        assertTrue(AvisosCrono.vencidos(primera.estado, r, ahora).avisos.isEmpty())
        assertEquals(e.evento(ahora), e.evento(ahora))
        assertEquals(e.previstos(ahora), e.previstos(ahora))
    }

    // ---- 15 · sin despertares en falso ----

    @Test
    fun e15LasHorasFlotantesNoArmanAlarma() {
        val e = estadoPausa()
        for (ahora in listOf(t0 + m(26), t0 + m(27), t0 + m(28))) {
            val ev = checkNotNull(e.evento(ahora))
            assertEquals(TipoAviso.TOCA_EMPEZAR, ev.tipo)
            assertEquals("T-h2", ev.paso)
            assertEquals(t0 + m(30), ev.ms)
        }
    }

    @Test
    fun e15NingunTocaDevueltoTieneHoraNoFirme() {
        val estados =
            listOf(
                nueva(),
                estadoB(),
                estadoPausa(),
                estadoH8(),
                estadoCompaneroAtrasado(),
                nueva().empieza("mesa", t0 - m(10)),
            )
        for (e in estados) {
            for (medio in -40..90) comprobarTocasFirmes(e, t0 + medio * 30_000L)
        }
    }

    private fun comprobarTocasFirmes(
        e: EstadoCrono,
        ahora: Long,
    ) {
        val eventos = listOfNotNull(e.evento(ahora), e.eventoFuera("horno", ahora), e.eventoFuera("mesa", ahora))
        for (ev in eventos.filter { it.tipo == TipoAviso.TOCA_EMPEZAR }) {
            val id = ev.paso.removePrefix("T-")
            assertTrue(e.previsto(id, ahora).firme, "TOCA de $id con hora no firme a ${ahora - t0}")
        }
    }

    // ---- 16 · misma cola, con y sin hueco ----

    @Test
    fun e16MismaColaConYSinHueco() {
        val a20 = t0 + m(20)
        val conHueco = nueva().empieza("horno", t0 + m(12)).hecho("horno", a20)
        assertEquals(t0 + m(28), conHueco.inicio("h2", a20))
        assertFalse(conHueco.carriles.getValue("horno").avisado)
        val ev = checkNotNull(conHueco.eventoFuera("mesa", a20))
        assertEquals("T-h2", ev.paso)
        assertEquals(t0 + m(28), ev.ms)

        val a16 = t0 + m(16)
        val sinHueco =
            nueva()
                .empieza("mesa", t0)
                .hecho("mesa", t0 + m(10))
                .empieza("sarten", t0 + m(10))
                .hecho("sarten", a16)
        assertEquals(t0 + m(16), sinHueco.inicio("s2", a16))
        assertTrue(sinHueco.carriles.getValue("sarten").avisado)
        assertTrue(AvisosCrono.vencidos(sinHueco, r, a16).avisos.none { it.carril == "sarten" })
    }

    // ---- 17 · llegada sin arrancar ----

    @Test
    fun e17LlegadaSinArrancarEsLaHoraDeServir() {
        val ahora = t0 - m(60)
        assertEquals(s0, nueva().llegada(ahora))
    }

    // ---- 18 · hora de servir ----

    @Test
    fun e18HoraDeServirSoloMuevePlanesSinArrancar() {
        val ahora = t0 - m(30)
        val e = nueva()
        val movido = SesionCrono.servir(e, s0 + m(15), ahora)
        for (p in r.pasos) assertEquals(e.inicio(p.id, ahora) + m(15), movido.inicio(p.id, ahora), p.id)
        assertEquals(t0 + m(15), movido.evento(ahora)?.ms)

        val a1 = t0 + m(1)
        val arrancado = e.empieza("mesa", t0)
        val servido = SesionCrono.servir(arrancado, s0 + m(15), a1)
        assertEquals(arrancado.previstos(a1), servido.previstos(a1))
        assertEquals(arrancado.llegada(a1), servido.llegada(a1))
        assertTrue(ConsultasCrono.arrancada(servido))
        assertFalse(ConsultasCrono.arrancada(e))
    }

    // ---- 19 · estado viejo ----

    @Test
    fun e19EstadoViejoSinMapasUsaLosRespaldos() {
        val b = estadoB()
        val json = RecetaJson.encodeToJsonElement(EstadoCrono.serializer(), b) as JsonObject
        assertTrue("inicioRealMs" in json && "finRealMs" in json)
        val viejo = JsonObject(json - "inicioRealMs" - "finRealMs")
        val e = RecetaJson.decodeFromJsonElement(EstadoCrono.serializer(), viejo)
        assertTrue(e.inicioRealMs.isEmpty() && e.finRealMs.isEmpty())
        // s2 en marcha: inicio = finAt − dur; s1 hecho: fin = inicioPasoMs de s2 → mismas horas que con los mapas.
        assertEquals(b.previstos(ahoraB), e.previstos(ahoraB))
        assertEquals(t0 + m(15), e.previsto("s2", ahoraB).inicioMs)
        assertEquals(t0 + m(30), e.previsto("s2", ahoraB).finMs)
    }

    @Test
    fun e19UnMapaMalFormadoSeSaltaPeroNoPierdeLaCocinada() {
        val b = estadoB()
        val json = RecetaJson.encodeToJsonElement(EstadoCrono.serializer(), b) as JsonObject
        val roto = JsonObject(json + ("inicioRealMs" to JsonPrimitive("roto")))
        val e = RecetaJson.decodeFromJsonElement(EstadoCrono.serializer(), roto)
        assertTrue(e.inicioRealMs.isEmpty())
        assertEquals(b.finRealMs, e.finRealMs)
        assertEquals(b.carriles, e.carriles)
        val valores = JsonObject(json + ("finRealMs" to JsonObject(mapOf("m1" to JsonPrimitive("x")))))
        val f = RecetaJson.decodeFromJsonElement(EstadoCrono.serializer(), valores)
        assertTrue(f.finRealMs.isEmpty())
        assertEquals(b.inicioRealMs, f.inicioRealMs)
    }

    @Test
    fun e19LosMapasSeSerializanYSeLeenIguales() {
        val b = estadoB()
        val texto = RecetaJson.encodeToString(EstadoCrono.serializer(), b)
        assertEquals(b, RecetaJson.decodeFromString(EstadoCrono.serializer(), texto))
    }

    // ---- 20 · reanudar no pisa el arranque ----

    @Test
    fun e20ReanudarNoPisaElArranque() {
        val b = estadoB()
        val pausa = MotorCrono.pausar(b, r, "sarten", ahoraB)
        val sigue = MotorCrono.reanudar(pausa, r, "sarten", t0 + m(24))
        assertEquals(t0 + m(15), sigue.inicioRealMs["s2"])
        assertEquals(b.previsto("o1", t0 + m(24)), sigue.previsto("o1", t0 + m(24)))
    }

    // ---- 21 · ciclo, por defensa ----

    @Test
    fun e21CicloDeJuntoTerminaIgnorandoLosJunto() {
        val ciclica =
            Receta(
                slug = "ciclo",
                titulo = "Ciclo",
                totalMin = 10.0,
                carriles = listOf(Carril("a", "A", "#fff", "fuego"), Carril("b", "B", "#fff", "fuego")),
                pasos =
                    listOf(
                        paso("a1", "a", 0.0, 600, junto = "b1"),
                        paso("b1", "b", 0.0, 600, junto = "a1"),
                    ),
            )
        val e = SesionCrono.inicial(ciclica, s0, 0L)
        val previstos = ConsultasCrono.previstos(e, ciclica, 0L)
        assertEquals(setOf("a1", "b1"), previstos.keys)
        assertTrue(previstos.values.all { it.inicioMs == s0 - 10 * min })
        assertTrue(Validador.receta(ciclica).any { it.regla == 2 && it.texto.contains("ciclo") })
    }

    // ---- 22 · huevos rotos real (H8) ----

    @Test
    fun e22HuevosRotosRealSigueAlFuegoPequeno() {
        val huevos = leerReceta(File("../data/recetas/huevos-rotos.json").readText())

        fun hora(
            h: Int,
            mm: Int,
        ): Long = s0 - ((12 * 60) - (h * 60 + mm)) * min
        var e = SesionCrono.inicial(huevos, s0, hora(10, 0))
        e = MotorCrono.empezar(e, huevos, "mesa", hora(10, 54))
        e = MotorCrono.hecho(e, huevos, "mesa", hora(11, 0))
        e = MotorCrono.empezar(e, huevos, "sarten", hora(11, 0))
        e = MotorCrono.hecho(e, huevos, "sarten", hora(11, 5))
        e = MotorCrono.empezar(e, huevos, "sarten", hora(11, 5))
        val previstos = ConsultasCrono.previstos(e, huevos, hora(11, 5))
        val pequena = previstos.getValue("calentar-sarten-pequena")
        assertEquals(hora(11, 22), pequena.inicioMs)
        assertTrue(pequena.firme)
        assertEquals(hora(11, 6), previstos.getValue("rasgar-jamon-lavar-padron").inicioMs)
    }

    // ---- 23 · validador ----

    @Test
    fun e23LasRecetasRealesPasanLaReglaDosAmpliada() {
        val recetas = File("../data/recetas").listFiles { f -> f.extension == "json" }.orEmpty()
        for (f in recetas) {
            val receta = leerReceta(f.readText())
            val hallazgos = Validador.receta(receta, null).filter { it.regla == 2 }
            assertEquals(emptyList(), hallazgos, receta.slug)
        }
    }

    // ---- 24 · clases de vista ----

    @Test
    fun e24ClasesDeVistaConLaPrevision() {
        val a = t0 - m(10)
        val adelantado = nueva().empieza("mesa", a)
        val horno = ConsultasCrono.vista(adelantado, r, "horno", a)
        assertEquals(ClaseEstado.ESPERA, horno.clase)
        assertEquals(t0 + m(2), horno.sugeridoMs)
        assertEquals(ClaseEstado.BLOQ, ConsultasCrono.vista(adelantado, r, "sarten", a).clase)

        val ahora = t0 + m(18)
        assertEquals(ClaseEstado.YA, ConsultasCrono.vista(estadoCompaneroAtrasado(), r, "sarten", ahora).clase)
    }

    // ---- 25 · temporizadores libres ----

    @Test
    fun e25UnLibreNoAlteraLasHorasYSuenaComoSiempre() {
        val b = estadoB()
        val conLibre = TemporizadoresLibres.anadir(b, 1, ahoraB)
        assertEquals(1, conLibre.libres.size)
        assertEquals(b.previstos(ahoraB), conLibre.previstos(ahoraB))
        assertEquals(b.llegada(ahoraB), conLibre.llegada(ahoraB))
        val ev = checkNotNull(conLibre.evento(ahoraB))
        assertEquals(TipoAviso.TEMPORIZADOR, ev.tipo)
        assertEquals(ahoraB + m(1), ev.ms)
        assertNotNull(conLibre.libres.single())
    }
}
