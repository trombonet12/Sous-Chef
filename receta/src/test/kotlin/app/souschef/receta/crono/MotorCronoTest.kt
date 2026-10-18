package app.souschef.receta.crono

import app.souschef.receta.Carril
import app.souschef.receta.Hito
import app.souschef.receta.Paso
import app.souschef.receta.Receta
import app.souschef.receta.RecetaJson
import app.souschef.receta.leerReceta
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Motor puro del cronómetro: ensalada de ñoquis real (dos carriles, dependencias, hitos) y casos sintéticos. */
class MotorCronoTest {
    private val ensalada =
        leerReceta(File("../data/recetas/ensalada-de-noquis-con-atun-y-feta.json").readText())

    private val servir = 1_700_000_000_000L

    private fun min(n: Double): Long = Math.round(n * 60_000)

    private fun min(n: Int): Long = n * 60_000L

    private fun seg(n: Int): Long = n * 1_000L

    // Ensalada: total 19 min. mesa empieza en servir-19 min; sarten (sésamo, off 5) en servir-14 min.
    private val inicioMesa get() = servir - min(19)
    private val inicioSarten get() = servir - min(14)

    private fun nuevo(r: Receta = ensalada) = SesionCrono.inicial(r, servir, ahoraMs = 0L)

    /** Cocina abierta a la hora del sartén: ambos carriles "ya" y sin avisos pendientes de empezar. */
    private fun abierto(
        r: Receta = ensalada,
        ahora: Long = inicioSarten,
    ) = SesionCrono.abrir(SesionCrono.inicial(r, servir, ahora), r, ahora)

    private fun sintetica(
        totalMin: Double,
        vararg pasos: Paso,
        carriles: List<Carril> = listOf(Carril("a", "A", "#fff", "fuego")),
    ) = Receta(slug = "sint", titulo = "Sintética", totalMin = totalMin, carriles = carriles, pasos = pasos.toList())

    @Suppress("LongParameterList")
    private fun paso(
        id: String,
        s: Int,
        off: Double,
        carril: String = "a",
        dep: List<String> = emptyList(),
        hitos: List<Hito> = emptyList(),
    ) = Paso(id = id, t = "Paso $id", carril = carril, off = off, min = s / 60.0, s = s, dep = dep, hitos = hitos)

    // ---- Plan y sugerido ----

    @Test
    fun carrilesYSugeridoDeLaEnsalada() {
        assertEquals(listOf("mesa", "sarten"), ConsultasCrono.carriles(ensalada).map { it.id })
        val e = nuevo()
        assertEquals(inicioMesa, ConsultasCrono.sugerido(e, ensalada, "mesa"))
        assertEquals(inicioSarten, ConsultasCrono.sugerido(e, ensalada, "sarten"))
    }

    @Test
    fun sugeridoNoTruncaMediosMinutos() {
        val r = sintetica(10.5, paso("x", 60, 3.0), paso("y", 60, 3.7))
        val e = SesionCrono.inicial(r, servir, 0)
        // 10,5 − 3 = 7,5 min (truncar daría 7); la cola no importa, solo el paso actual.
        assertEquals(servir - 450_000L, ConsultasCrono.sugerido(e, r, "a"))
        val tras = MotorCrono.hecho(e, r, "a", 0)
        // off 3,7 se redondea a 3,5 → 10,5 − 3,5 = 7 min.
        assertEquals(servir - min(7), ConsultasCrono.sugerido(tras, r, "a"))
    }

    @Test
    fun carrilNoDeclaradoEntraGrisYCarrilSinPasosSeQuita() {
        val r =
            sintetica(
                2.0,
                paso("x", 60, 0.0, carril = "nuevo"),
                carriles = listOf(Carril("a", "A"), Carril("a", "A"), Carril("vacio")),
            )
        val cs = ConsultasCrono.carriles(r)
        assertEquals(listOf("nuevo"), cs.map { it.id })
        assertEquals("#B0A899", cs.single().color)
        assertEquals("mesa", cs.single().tipo)
    }

    // ---- Mandos ----

    @Test
    fun empezarFijaElFinEnRelojDePared() {
        val e = MotorCrono.empezar(abierto(), ensalada, "sarten", 1_000L)
        val ec = e.carriles.getValue("sarten")
        assertEquals(1_000L + seg(180), ec.finAtMs)
        assertTrue(ec.salto)
        assertEquals(180, ConsultasCrono.quedan(e, ensalada, "sarten", 1_000L))
        assertEquals(80, ConsultasCrono.quedan(e, ensalada, "sarten", 1_000L + seg(100)))
        // Se redondea hacia arriba: a 100,5 s quedan 79,5 → 80.
        assertEquals(80, ConsultasCrono.quedan(e, ensalada, "sarten", 1_000L + 100_500L))
        // Pasado de cero: negativo, nunca avanza solo.
        assertEquals(-20, ConsultasCrono.quedan(e, ensalada, "sarten", 1_000L + seg(200)))
        assertEquals(0, e.carriles.getValue("sarten").i)
    }

    @Test
    fun pausarYReanudarConservanLoQueQueda() {
        val t0 = inicioSarten
        val e = MotorCrono.empezar(abierto(), ensalada, "sarten", t0)
        val pausa = MotorCrono.pausar(e, ensalada, "sarten", t0 + seg(50))
        val ec = pausa.carriles.getValue("sarten")
        assertTrue(ec.pausado)
        assertNull(ec.finAtMs)
        assertEquals(130, ec.restanteS)
        // En pausa el reloj no avanza.
        assertEquals(130, ConsultasCrono.quedan(pausa, ensalada, "sarten", t0 + seg(500)))
        val sigue = MotorCrono.reanudar(pausa, ensalada, "sarten", t0 + seg(500))
        assertEquals(t0 + seg(500) + seg(130), sigue.carriles.getValue("sarten").finAtMs)
        assertFalse(sigue.carriles.getValue("sarten").pausado)
    }

    @Test
    fun pausarSinArrancarOReanudarSinPausaNoHaceNada() {
        val e = abierto()
        assertEquals(e, MotorCrono.pausar(e, ensalada, "sarten", 5L))
        assertEquals(e, MotorCrono.reanudar(e, ensalada, "sarten", 5L))
    }

    @Test
    fun masTreintaCorriendoParadoYVencido() {
        val t0 = inicioSarten
        val parado = MotorCrono.masTreinta(abierto(), ensalada, "sarten", t0)
        assertEquals(210, parado.carriles.getValue("sarten").restanteS)

        val e = MotorCrono.empezar(abierto(), ensalada, "sarten", t0)
        val mas = MotorCrono.masTreinta(e, ensalada, "sarten", t0 + seg(10))
        assertEquals(t0 + seg(210), mas.carriles.getValue("sarten").finAtMs)

        // Vencido: sigue valiendo y rearma aFin si vuelve a quedar tiempo.
        val vencido = AvisosCrono.vencidos(e, ensalada, t0 + seg(185)).estado
        assertTrue(vencido.carriles.getValue("sarten").aFin)
        val rearmado = MotorCrono.masTreinta(vencido, ensalada, "sarten", t0 + seg(185))
        val ec = rearmado.carriles.getValue("sarten")
        assertFalse(ec.aFin)
        // Muy vencido (-20 s tras +30): aFin no se rearma.
        val muy = AvisosCrono.vencidos(e, ensalada, t0 + seg(250)).estado
        val sigueVencido = MotorCrono.masTreinta(muy, ensalada, "sarten", t0 + seg(250)).carriles.getValue("sarten")
        assertTrue(sigueVencido.aFin)
    }

    @Test
    fun hechoGuardaSegundosRealesYAvanza() {
        val t0 = inicioSarten
        val e = MotorCrono.hecho(abierto(), ensalada, "sarten", t0 + 100_400L)
        assertEquals(100, e.pasosReales["sesamo"])
        val ec = e.carriles.getValue("sarten")
        assertEquals(1, ec.i)
        assertNull(ec.finAtMs)
        assertTrue(ec.hitosAvisados.isEmpty())
        assertEquals(t0 + 100_400L, ec.inicioPasoMs)

        val redondeo = MotorCrono.hecho(abierto(), ensalada, "sarten", t0 + 100_500L)
        assertEquals(101, redondeo.pasosReales["sesamo"])
        val minimo = MotorCrono.hecho(abierto(), ensalada, "sarten", t0)
        assertEquals(1, minimo.pasosReales["sesamo"], "mínimo 1 s")
    }

    @Test
    fun hechoRecalculaAvisadoSegunSugerido() {
        val pronto = MotorCrono.hecho(abierto(ensalada, servir - min(60)), ensalada, "mesa", servir - min(60))
        assertFalse(pronto.carriles.getValue("mesa").avisado)
        val tarde = MotorCrono.hecho(abierto(), ensalada, "mesa", servir)
        assertTrue(tarde.carriles.getValue("mesa").avisado)
    }

    @Test
    fun ultimoHechoTerminaElCarrilYMueveElFoco() {
        var e = abierto()
        repeat(4) { e = MotorCrono.hecho(e, ensalada, "mesa", inicioSarten + seg(it + 1)) }
        assertTrue(e.carriles.getValue("mesa").fin)
        assertEquals("sarten", e.foco)
        assertEquals(0, ConsultasCrono.quedan(e, ensalada, "mesa", 0))
        assertEquals(ClaseEstado.FIN, ConsultasCrono.vista(e, ensalada, "mesa", 0).clase)
        // Un mando sobre un carril terminado no hace nada.
        assertEquals(e, MotorCrono.empezar(e, ensalada, "mesa", 99L))
        assertEquals(e, MotorCrono.hecho(e, ensalada, "mesa", 99L))
        assertFalse(ConsultasCrono.todoTerminado(e))
    }

    @Test
    fun deshacerVuelveAlPasoAnteriorConSuRelojComoSiNoSeHubieraTocado() {
        val t0 = inicioSarten
        val corriendo = MotorCrono.empezar(abierto(), ensalada, "sarten", t0)
        val antes = corriendo.carriles.getValue("sarten")
        val tocado = MotorCrono.hecho(corriendo, ensalada, "sarten", t0 + seg(100))

        val e = MotorCrono.deshacer(tocado, ensalada, "sarten", t0 + seg(110))

        assertEquals(antes, e.carriles.getValue("sarten"))
        assertNull(e.pasosReales["sesamo"])
        assertTrue(e.deshacer.isEmpty())
        assertNull(e.ultimoHecho)
        assertEquals(t0 + seg(110), e.actualizadoMs)
    }

    @Test
    fun deshacerTrasElUltimoPasoReabreElCarril() {
        var e = abierto()
        repeat(4) { e = MotorCrono.hecho(e, ensalada, "mesa", inicioSarten + seg(it + 1)) }
        assertTrue(e.carriles.getValue("mesa").fin)

        val r = MotorCrono.deshacer(e, ensalada, "mesa", inicioSarten + seg(10))

        val mesa = r.carriles.getValue("mesa")
        assertFalse(mesa.fin)
        assertEquals(3, mesa.i)
    }

    @Test
    fun deshacerSinCarrilDeshaceElUltimoHechoYSoloUnNivel() {
        val t0 = inicioSarten
        var e = MotorCrono.hecho(abierto(), ensalada, "mesa", t0 + seg(1))
        e = MotorCrono.hecho(e, ensalada, "sarten", t0 + seg(2))
        assertEquals("sarten", e.ultimoHecho)

        val r = MotorCrono.deshacer(e, ensalada, null, t0 + seg(3))

        assertEquals(0, r.carriles.getValue("sarten").i)
        assertEquals(1, r.carriles.getValue("mesa").i)
        // Un segundo deshacer sobre el mismo carril ya no tiene nada que deshacer.
        assertEquals(r, MotorCrono.deshacer(r, ensalada, "sarten", t0 + seg(4)))
    }

    @Test
    fun sinNadaQueDeshacerNoCambiaNada() {
        val e = abierto()
        assertEquals(e, MotorCrono.deshacer(e, ensalada, "mesa", inicioSarten))
        assertEquals(e, MotorCrono.deshacer(e, ensalada, null, inicioSarten))
    }

    @Test
    fun dependenciasBloqueanYEmpezarIgualSalta() {
        var e = abierto()
        repeat(3) { e = MotorCrono.hecho(e, ensalada, "mesa", servir) }
        val t = servir
        val vista = ConsultasCrono.vista(e, ensalada, "mesa", t)
        assertEquals(ClaseEstado.BLOQ, vista.clase)
        assertEquals(1, vista.bloqueadoPor.size)
        assertEquals("sarten", vista.bloqueadoPor.single().second)

        assertEquals(e, MotorCrono.empezar(e, ensalada, "mesa", t), "bloqueado: Play no hace nada")
        val igual = MotorCrono.empezarIgual(e, ensalada, "mesa", t)
        val ec = igual.carriles.getValue("mesa")
        assertTrue(ec.salto)
        assertEquals(t + seg(120), ec.finAtMs)
        assertEquals(ClaseEstado.MARCHA, ConsultasCrono.vista(igual, ensalada, "mesa", t).clase)

        // Con la dependencia resuelta, ya no está bloqueado.
        repeat(5) { e = MotorCrono.hecho(e, ensalada, "sarten", t) }
        assertNotEquals(ClaseEstado.BLOQ, ConsultasCrono.vista(e, ensalada, "mesa", t).clase)
        assertTrue(
            MotorCrono
                .empezar(e, ensalada, "mesa", t)
                .carriles
                .getValue("mesa")
                .corriendo,
        )
    }

    @Test
    fun reiniciarVuelveALaPortadaConservandoServirYRaciones() {
        val e = MotorCrono.hecho(abierto(), ensalada, "mesa", servir)
        val r = SesionCrono.reiniciar(e, ensalada, 7L)
        assertTrue(r.pasosReales.isEmpty())
        assertNull(r.inicioMs)
        assertEquals(servir, r.servirMs)
        assertEquals(e.raciones, r.raciones)
        assertEquals(0, r.carriles.getValue("mesa").i)
        assertEquals(7L, r.actualizadoMs)
    }

    // ---- Estado / rango / foco ----

    @Test
    fun clasesDeEstadoSegunLaTabla() {
        val t0 = inicioSarten
        // sarten empieza a servir-14: antes de 3 min de margen → ESPERA; dentro → YA.
        val lejos = SesionCrono.inicial(ensalada, servir, 0)
        assertEquals(ClaseEstado.ESPERA, ConsultasCrono.vista(lejos, ensalada, "sarten", t0 - min(4)).clase)
        assertEquals(ClaseEstado.YA, ConsultasCrono.vista(lejos, ensalada, "sarten", t0 - min(3)).clase)
        assertEquals(ClaseEstado.YA, ConsultasCrono.vista(lejos, ensalada, "sarten", t0 + min(2)).clase)

        val marcha = MotorCrono.empezar(abierto(), ensalada, "sarten", t0)
        assertEquals(ClaseEstado.MARCHA, ConsultasCrono.vista(marcha, ensalada, "sarten", t0 + seg(10)).clase)
        assertEquals(ClaseEstado.ATENDER, ConsultasCrono.vista(marcha, ensalada, "sarten", t0 + seg(180)).clase)
        val pausa = MotorCrono.pausar(marcha, ensalada, "sarten", t0 + seg(10))
        assertEquals(ClaseEstado.PAUSA, ConsultasCrono.vista(pausa, ensalada, "sarten", t0 + seg(10)).clase)
    }

    @Test
    fun rangoYOrdenSegunLaSpec() {
        val rangos = ClaseEstado.entries.associate { it to it.rango }
        assertEquals(
            mapOf(
                ClaseEstado.ATENDER to 0,
                ClaseEstado.YA to 1,
                ClaseEstado.MARCHA to 2,
                ClaseEstado.PAUSA to 3,
                ClaseEstado.ESPERA to 4,
                ClaseEstado.BLOQ to 5,
                ClaseEstado.FIN to 9,
            ),
            rangos,
        )
        assertEquals(listOf(0, 1, 3, 4, 5, 6, 9), ClaseEstado.entries.map { it.orden }.sorted())
    }

    @Test
    fun siguienteAccionableElige_porRangoYLuegoPorHora() {
        val e = SesionCrono.inicial(ensalada, servir, 0)
        // Ambos en espera: gana el de hora sugerida más temprana (mesa).
        assertEquals("mesa", ConsultasCrono.siguienteAccionable(e, ensalada, servir - min(40)))
        // Un carril corriendo y vencido (atender, rango 0) gana a uno que "ya" toca.
        val t0 = inicioSarten
        val c = MotorCrono.empezar(abierto(), ensalada, "sarten", t0)
        assertEquals("sarten", ConsultasCrono.siguienteAccionable(c, ensalada, t0 + seg(200)))
        // Bloqueado nunca se enfoca solo.
        var b = abierto()
        repeat(3) { b = MotorCrono.hecho(b, ensalada, "mesa", servir) }
        repeat(5) { b = MotorCrono.hecho(b, ensalada, "sarten", servir) }
        val soloMesaBloqueada = sintetica(2.0, paso("p", 60, 0.0), paso("q", 60, 1.0, carril = "b", dep = listOf("p")))
        val sb = SesionCrono.inicial(soloMesaBloqueada, servir, 0)
        assertEquals("a", ConsultasCrono.siguienteAccionable(sb, soloMesaBloqueada, servir))
    }

    @Test
    fun abrirMarcaAvisadosYFijaInicio() {
        val ahora = servir - min(10)
        val e = SesionCrono.abrir(SesionCrono.inicial(ensalada, servir, 0), ensalada, ahora)
        assertEquals(ahora, e.inicioMs)
        assertTrue(e.carriles.values.all { it.avisado && it.inicioPasoMs == ahora })
        // Abrir de nuevo no cambia el inicio.
        assertEquals(ahora, SesionCrono.abrir(e, ensalada, ahora + 5).inicioMs)
        // Con tiempo de sobra no se marca nada como avisado.
        val pronto = SesionCrono.abrir(SesionCrono.inicial(ensalada, servir, 0), ensalada, servir - min(60))
        assertTrue(pronto.carriles.values.none { it.avisado })
    }

    // ---- Plan, retraso, paralelo ----

    @Test
    fun enHoraNoEstaTarde() {
        val e = abierto(ensalada, inicioMesa)
        val plan = ConsultasCrono.plan(e, ensalada, inicioMesa)
        assertEquals(0, plan.retrasoMin)
        assertEquals(servir - min(7), plan.llegadaMs) // sarten: 720 s desde servir-19
        assertEquals(-7, plan.difFinMin)
        assertFalse(plan.tarde)
        assertNull(plan.vencido)
    }

    @Test
    fun porDetrasDaRetrasoYListasDeHolgura() {
        val ahora = inicioSarten // mesa tocaba 5 min antes
        val e = abierto(ensalada, ahora)
        val plan = ConsultasCrono.plan(e, ensalada, ahora)
        assertEquals(5, plan.retrasoMin)
        assertTrue(plan.tarde)
        assertEquals(5, plan.minutosPorDetras)
        val flexibles =
            ensalada.pasos
                .filter { it.holgura == "flexible" }
                .map { it.t }
                .distinct()
        assertEquals(flexibles.toSet(), plan.aguantan.toSet())
        val criticos =
            ensalada.pasos
                .filter { it.holgura == "critico" }
                .map { it.t }
                .distinct()
        assertEquals(criticos.toSet(), plan.noEsperan.toSet())
    }

    @Test
    fun planSenalaElCarrilVencido() {
        val t0 = inicioSarten
        val e = MotorCrono.empezar(abierto(), ensalada, "sarten", t0)
        assertEquals("sarten", ConsultasCrono.plan(e, ensalada, t0 + seg(190)).vencido)
        assertNull(ConsultasCrono.plan(e, ensalada, t0 + seg(10)).vencido)
    }

    @Test
    fun paraleloListaLoQueTocaEnDoceMinutos() {
        val lejos = SesionCrono.inicial(ensalada, servir, 0)
        // Desde mesa a servir-19: sarten sugerido a servir-14 → 5 min.
        val p = ConsultasCrono.paralelo(lejos, ensalada, "mesa", inicioMesa)
        assertEquals(1, p.size)
        assertEquals(5, p.single().minutos)
        assertEquals(EtiquetaParalelo.EN_MIN, p.single().etiqueta)
        // A 16 min queda fuera de la ventana de 12.
        assertTrue(ConsultasCrono.paralelo(lejos, ensalada, "mesa", servir - min(30)).isEmpty())
        // Sarten ya pasado de hora → AHORA.
        assertEquals(
            EtiquetaParalelo.AHORA,
            ConsultasCrono.paralelo(lejos, ensalada, "mesa", inicioSarten).single().etiqueta,
        )
        // Si el otro corre con tiempo, se salta; vencido → VENCIDO.
        val corre = MotorCrono.empezar(abierto(), ensalada, "sarten", inicioSarten)
        assertTrue(ConsultasCrono.paralelo(corre, ensalada, "mesa", inicioSarten + seg(10)).isEmpty())
        assertEquals(
            EtiquetaParalelo.VENCIDO,
            ConsultasCrono.paralelo(corre, ensalada, "mesa", inicioSarten + seg(200)).single().etiqueta,
        )
        // En pausa → PAUSA.
        val pausa = MotorCrono.pausar(corre, ensalada, "sarten", inicioSarten + seg(10))
        assertEquals(
            EtiquetaParalelo.PAUSA,
            ConsultasCrono.paralelo(pausa, ensalada, "mesa", inicioSarten + seg(20)).single().etiqueta,
        )
    }

    @Test
    fun paraleloOrdenaPorMinutosYSaltaBloqueados() {
        val r =
            sintetica(
                20.0,
                paso("m", 60, 0.0),
                paso("n", 60, 8.0, carril = "b"),
                paso("o", 60, 4.0, carril = "c"),
                paso("p", 60, 5.0, carril = "d", dep = listOf("m")),
                carriles = listOf(Carril("a"), Carril("b"), Carril("c"), Carril("d")),
            )
        val e = SesionCrono.inicial(r, servir, 0)
        val res = ConsultasCrono.paralelo(e, r, "a", servir - min(20))
        assertEquals(listOf("c", "b"), res.map { it.carril })
        assertEquals(listOf(4, 8), res.map { it.minutos })
    }

    // ---- Avisos ----

    @Test
    fun hitosDiezSegundosYCeroSinDuplicar() {
        val t0 = inicioSarten
        var e = MotorCrono.empezar(abierto(), ensalada, "sarten", t0)
        assertTrue(AvisosCrono.vencidos(e, ensalada, t0 + seg(119)).avisos.isEmpty())

        val hito = AvisosCrono.vencidos(e, ensalada, t0 + seg(120))
        assertEquals(listOf(TipoAviso.HITO), hito.avisos.map { it.tipo })
        assertEquals("sarten", hito.avisos.single().carril)
        assertTrue(
            hito.avisos
                .single()
                .texto
                .startsWith("A partir de aquí"),
        )
        assertFalse(hito.avisos.single().silencioso)
        e = hito.estado
        assertEquals(setOf(0), e.carriles.getValue("sarten").hitosAvisados)
        assertTrue(AvisosCrono.vencidos(e, ensalada, t0 + seg(121)).avisos.isEmpty(), "sin duplicar")

        assertTrue(AvisosCrono.vencidos(e, ensalada, t0 + seg(170)).avisos.isEmpty(), "ya no hay aviso de 10 s")
        val fin = AvisosCrono.vencidos(e, ensalada, t0 + seg(180))
        assertEquals(listOf(TipoAviso.FIN), fin.avisos.map { it.tipo })
        assertEquals(ensalada.paso("sesamo")!!.t, fin.avisos.single().paso)
        assertTrue(AvisosCrono.vencidos(fin.estado, ensalada, t0 + seg(190)).avisos.isEmpty())
        assertTrue(
            fin.estado.carriles
                .getValue("sarten")
                .aFin,
        )
    }

    @Test
    fun avisosAtrasadosSonSilenciososPasados90Segundos() {
        val t0 = inicioSarten
        val e = MotorCrono.empezar(abierto(), ensalada, "sarten", t0)
        // Vuelta de segundo plano 220 s después: el hito (hace 100 s) y el cero (hace 40 s).
        val v = AvisosCrono.vencidos(e, ensalada, t0 + seg(220))
        assertEquals(listOf(TipoAviso.HITO, TipoAviso.FIN), v.avisos.map { it.tipo }, "no hay aviso de 10 s")
        assertEquals(listOf(true, false), v.avisos.map { it.silencioso })
        val c = v.estado.carriles.getValue("sarten")
        assertTrue(c.aFin)
        // Más de 90 s después del cero: todo silencioso.
        val tarde = AvisosCrono.vencidos(e, ensalada, t0 + seg(400))
        assertTrue(tarde.avisos.all { it.silencioso })
        // Exactamente 90 s todavía suena.
        val limite = AvisosCrono.vencidos(e, ensalada, t0 + seg(270))
        assertFalse(limite.avisos.last().silencioso)
    }

    @Test
    fun enPausaNoHayAvisos() {
        val t0 = inicioSarten
        val e =
            MotorCrono.pausar(
                MotorCrono.empezar(abierto(), ensalada, "sarten", t0),
                ensalada,
                "sarten",
                t0 + seg(10),
            )
        val v = AvisosCrono.vencidos(e, ensalada, t0 + seg(10_000))
        assertTrue(v.avisos.none { it.carril == "sarten" })
        assertNull(AvisosCrono.siguienteEvento(e, ensalada)?.takeIf { it.carril == "sarten" })
    }

    @Test
    fun tocaEmpezarUnaVezYSilenciosoSiVaMuyTarde() {
        val e = SesionCrono.inicial(ensalada, servir, 0)
        val v = AvisosCrono.vencidos(e, ensalada, inicioMesa + seg(1))
        assertEquals(listOf(TipoAviso.TOCA_EMPEZAR), v.avisos.map { it.tipo })
        assertEquals("mesa", v.avisos.single().carril)
        assertFalse(v.avisos.single().silencioso)
        assertTrue(AvisosCrono.vencidos(v.estado, ensalada, inicioMesa + seg(2)).avisos.none { it.carril == "mesa" })

        val muyTarde = AvisosCrono.vencidos(e, ensalada, inicioMesa + seg(91))
        assertTrue(muyTarde.avisos.single { it.carril == "mesa" }.silencioso)
    }

    @Test
    fun tocaEmpezarNoSuenaBloqueadoYSeRearma() {
        val r =
            sintetica(
                10.0,
                paso("p", 60, 0.0),
                paso("q", 60, 1.0, carril = "b", dep = listOf("p")),
                carriles = listOf(Carril("a"), Carril("b")),
            )
        val e0 = SesionCrono.inicial(r, servir, 0)
        // Sólo avisa p; q está bloqueado.
        val v = AvisosCrono.vencidos(e0, r, servir)
        assertEquals(listOf("a"), v.avisos.map { it.carril })
        // Forzamos un avisado=true en el bloqueado: se rearma.
        val sucio = v.estado.copy(carriles = v.estado.carriles + ("b" to EstadoCarril(avisado = true)))
        val rearmado = AvisosCrono.vencidos(sucio, r, servir).estado
        assertFalse(rearmado.carriles.getValue("b").avisado)
        // Al terminar p (hecho) q deja de estar bloqueado y avisa.
        val libre = MotorCrono.hecho(rearmado, r, "a", servir + seg(5))
        val v2 = AvisosCrono.vencidos(libre, r, servir + seg(6))
        assertEquals(listOf("b"), v2.avisos.map { it.carril })
    }

    @Test
    fun siguienteEventoParaLaCadenaDeAlarmas() {
        val e0 = SesionCrono.inicial(ensalada, servir, 0)
        val primero = AvisosCrono.siguienteEvento(e0, ensalada)!!
        assertEquals(TipoAviso.TOCA_EMPEZAR, primero.tipo)
        assertEquals(inicioMesa, primero.ms)

        // Con ambos avisados y el sartén corriendo, el siguiente es su hito.
        val t0 = inicioSarten
        val e = MotorCrono.empezar(abierto(), ensalada, "sarten", t0)
        val sig = AvisosCrono.siguienteEvento(e, ensalada)!!
        assertEquals(TipoAviso.HITO, sig.tipo)
        assertEquals("sarten", sig.carril)
        assertEquals(t0 + seg(120), sig.ms)
        val sig2 = AvisosCrono.siguienteEvento(AvisosCrono.vencidos(e, ensalada, t0 + seg(120)).estado, ensalada)!!
        assertEquals(TipoAviso.FIN, sig2.tipo)
        assertEquals(t0 + seg(180), sig2.ms)
        // Todo terminado: nada que programar.
        var fin = e
        repeat(4) { fin = MotorCrono.hecho(fin, ensalada, "mesa", t0) }
        repeat(5) { fin = MotorCrono.hecho(fin, ensalada, "sarten", t0) }
        assertNull(AvisosCrono.siguienteEvento(fin, ensalada))
    }

    @Test
    fun masTreintaDesplazaElHito() {
        val t0 = inicioSarten
        val e = MotorCrono.masTreinta(MotorCrono.empezar(abierto(), ensalada, "sarten", t0), ensalada, "sarten", t0)
        assertEquals(t0 + seg(150), AvisosCrono.siguienteEvento(e, ensalada)!!.ms)
    }

    @Test
    fun prorrogarSumaSegundosArbitrariosYDesplazaElHito() {
        val t0 = inicioSarten
        val parado = MotorCrono.prorrogar(abierto(), ensalada, "sarten", 120, t0)
        assertEquals(300, parado.carriles.getValue("sarten").restanteS)

        val e = MotorCrono.prorrogar(MotorCrono.empezar(abierto(), ensalada, "sarten", t0), ensalada, "sarten", 120, t0)
        assertEquals(t0 + seg(300), e.carriles.getValue("sarten").finAtMs)
        assertEquals(t0 + seg(240), AvisosCrono.siguienteEvento(e, ensalada)!!.ms)
    }

    @Test
    fun masTreintaEsProrrogarTreinta() {
        val t0 = inicioSarten
        assertEquals(
            MotorCrono.prorrogar(abierto(), ensalada, "sarten", 30, t0),
            MotorCrono.masTreinta(abierto(), ensalada, "sarten", t0),
        )
    }

    @Test
    fun hitosFueraDeRangoNoAvisan() {
        val r = sintetica(5.0, paso("x", 60, 0.0, hitos = listOf(Hito(0.0, "cero"), Hito(60.0, "fin"), Hito(30.4, ""))))
        var e = SesionCrono.abrir(SesionCrono.inicial(r, servir, 0), r, servir)
        e = MotorCrono.empezar(e, r, "a", servir)
        val v = AvisosCrono.vencidos(e, r, servir + seg(31))
        assertEquals(listOf("Aviso"), v.avisos.filter { it.tipo == TipoAviso.HITO }.map { it.texto })
    }

    // ---- Caducidad y persistencia ----

    @Test
    fun caducaTrasDoceHoras() {
        val e = nuevo().copy(actualizadoMs = 1_000L)
        val doce = 12 * 3_600_000L
        assertFalse(SesionCrono.caducado(e, 1_000L + doce))
        assertTrue(SesionCrono.caducado(e, 1_000L + doce + 1))
        assertEquals(e, SesionCrono.vigente(e, ensalada, 1_000L + doce))
        // Caducado: se descarta entero.
        assertNull(SesionCrono.vigente(e, ensalada, 1_000L + doce + 1))
        assertNull(SesionCrono.vigente(null, ensalada, 0))
        assertNull(SesionCrono.vigente(e.copy(slug = "otra"), ensalada, 1_000L))
    }

    @Test
    fun cadaMandoActualizaElSello() {
        val e = abierto()
        assertEquals(42L, MotorCrono.empezar(e, ensalada, "sarten", 42L).actualizadoMs)
        assertEquals(43L, MotorCrono.hecho(e, ensalada, "mesa", 43L).actualizadoMs)
        assertEquals(44L, SesionCrono.marcar(e, "x", true, 44L).actualizadoMs)
    }

    @Test
    fun elEstadoSeSerializaIntegro() {
        val t0 = inicioSarten
        var e = MotorCrono.empezar(abierto(), ensalada, "sarten", t0)
        e = MotorCrono.hecho(e, ensalada, "mesa", t0 + seg(30))
        e = SesionCrono.marcar(e, SesionCrono.idCasilla("2 tomates"), true, t0 + seg(31))
        e = AvisosCrono.vencidos(e, ensalada, t0 + seg(125)).estado
        val json = RecetaJson.encodeToString(EstadoCrono.serializer(), e)
        assertEquals(e, RecetaJson.decodeFromString(EstadoCrono.serializer(), json))
        assertTrue(json.contains("pasosReales"))
    }

    @Test
    fun idsDeChecklistEstablesPorContenido() {
        val a = SesionCrono.idCasilla("200 g de ñoquis")
        assertEquals(a, SesionCrono.idCasilla("  200 g de ñoquis "))
        assertNotEquals(a, SesionCrono.idCasilla("200 g de atún"))
        assertTrue(a.startsWith("mep-") && a.length == 12)
        val e = SesionCrono.marcar(nuevo(), a, true, 1L)
        assertTrue(a in e.marcados)
        assertFalse(a in SesionCrono.marcar(e, a, false, 2L).marcados)
        // Las líneas reales dan ids sin colisiones.
        val lineas = ensalada.mep.flatMap { it.l }
        assertEquals(
            lineas.distinct().size,
            lineas
                .distinct()
                .map { SesionCrono.idCasilla(it) }
                .distinct()
                .size,
        )
    }

    // ---- Cierre ----

    @Test
    fun resumenConDuracionRealYDesvios() {
        val r = sintetica(3.0, paso("uno", 60, 0.0), paso("dos", 120, 1.0))
        val inicio = servir - min(4)
        var e = SesionCrono.abrir(SesionCrono.inicial(r, servir, 0), r, inicio)
        e = MotorCrono.hecho(e, r, "a", inicio + seg(90))
        e = MotorCrono.hecho(e, r, "a", inicio + seg(190))
        assertTrue(ConsultasCrono.todoTerminado(e))
        val res = SesionCrono.resumen(e, r, inicio + seg(190))
        assertEquals(190L, res.duracionRealS)
        assertEquals(3, res.minutosReales)
        assertEquals(inicio + seg(190), res.servidoMs)
        assertEquals(servir, res.previstoMs)
        assertEquals(listOf(90, 100), res.pasosReales.map { it.segundos })
        assertEquals(listOf("uno", "dos"), res.desvios.map { it.id })
        assertEquals(listOf(30, -20), res.desvios.map { it.difS })
        val c = res.carriles.single()
        assertEquals(2, c.pasos)
        assertEquals(3, c.minutos)
        assertTrue(c.deFuego)
    }

    @Test
    fun resumenSinInicioOFueraDeRangoNoDaMinutos() {
        val r = sintetica(1.0, paso("uno", 60, 0.0))
        val e = MotorCrono.hecho(SesionCrono.inicial(r, servir, 0), r, "a", 5L)
        assertNull(SesionCrono.resumen(e, r, 10L).duracionRealS)
        assertNull(SesionCrono.resumen(e, r, 10L).minutosReales)
        val abierto = e.copy(inicioMs = 0L)
        assertNull(SesionCrono.resumen(abierto, r, 10_000L).minutosReales, "0 min no vale")
        assertNull(SesionCrono.resumen(abierto, r, 13 * 3_600_000L).minutosReales, "más de 720 min no vale")
        assertEquals(60, SesionCrono.resumen(abierto, r, 3_600_000L).minutosReales)
    }

    @Test
    fun cocinadaCompletaDeLaEnsalada() {
        val inicio = inicioMesa
        var e = SesionCrono.abrir(SesionCrono.inicial(ensalada, servir, 0), ensalada, inicio)
        var t = inicio
        for (carril in listOf("mesa", "sarten", "mesa").distinct()) {
            val n = ensalada.pasos.count { it.carril == carril }
            repeat(n) {
                t += seg(60)
                e = MotorCrono.hecho(e, ensalada, carril, t)
            }
        }
        assertTrue(ConsultasCrono.todoTerminado(e))
        assertEquals(ensalada.pasos.map { it.id }.toSet(), e.pasosReales.keys)
        val res = SesionCrono.resumen(e, ensalada, t)
        assertEquals(8 * 60L, res.duracionRealS)
        assertEquals(8, res.minutosReales)
        assertEquals(listOf(4, 4), res.carriles.map { it.pasos })
        assertEquals(listOf(false, true), res.carriles.map { it.deFuego })
    }
}
