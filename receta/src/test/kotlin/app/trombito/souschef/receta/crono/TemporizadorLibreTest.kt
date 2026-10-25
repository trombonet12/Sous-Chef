package app.trombito.souschef.receta.crono

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Temporizador libre (ADR-0016): fuera del plano, mantiene viva la cocinada hasta avisar y avisa como un carril. */
class TemporizadorLibreTest {
    private val t0 = 1_700_000_000_000L
    private val min = 60_000L

    private val receta =
        Receta(
            slug = "x",
            titulo = "X",
            totalMin = 10.0,
            carriles = listOf(Carril("a", "A", "#fff", "fuego")),
            pasos = listOf(Paso(id = "p1", t = "Paso 1", carril = "a", off = 0.0, min = 1.0, s = 60)),
        )

    private fun abierto(ahora: Long = t0) =
        SesionCrono.abrir(SesionCrono.inicial(receta, t0 + 10 * min, ahora), receta, ahora)

    private fun conLibre(
        minutos: Int,
        ahora: Long = t0,
    ) = TemporizadoresLibres.anadir(abierto(ahora), minutos, ahora)

    // ---- añadir ----

    @Test
    fun anadirGuardaElLibreConSuFin() {
        val e = conLibre(10)
        val libre = e.libres.single()
        assertEquals(10, libre.minutos)
        assertEquals(t0, libre.inicioMs)
        assertEquals(t0 + 10 * min, libre.finMs)
        assertFalse(libre.avisado)
        assertNull(libre.etiqueta)
        assertEquals(t0, e.actualizadoMs)
    }

    @Test
    fun anadirNoTocaPlanoFuegosPasosRealesNiDeshacer() {
        val antes = abierto()
        val e = TemporizadoresLibres.anadir(antes, 5, t0 + 1_000)
        assertEquals(antes.carriles, e.carriles)
        assertEquals(antes.pasosReales, e.pasosReales)
        assertEquals(antes.deshacer, e.deshacer)
        assertEquals(antes.ultimoHecho, e.ultimoHecho)
        assertEquals(antes.foco, e.foco)
    }

    @Test
    fun duracionFueraDeRangoSeRechaza() {
        val e = abierto()
        assertEquals(e, TemporizadoresLibres.anadir(e, 0, t0))
        assertEquals(e, TemporizadoresLibres.anadir(e, -3, t0))
        assertEquals(e, TemporizadoresLibres.anadir(e, 181, t0))
        assertEquals(1, TemporizadoresLibres.anadir(e, 1, t0).libres.size)
        assertEquals(1, TemporizadoresLibres.anadir(e, 180, t0).libres.size)
    }

    @Test
    fun comoMuchoTresALaVez() {
        var e = abierto()
        repeat(3) { e = TemporizadoresLibres.anadir(e, 5, t0) }
        assertEquals(3, e.libres.size)
        assertFalse(TemporizadoresLibres.puedeAnadir(e, t0))
        assertEquals(e, TemporizadoresLibres.anadir(e, 5, t0))
    }

    @Test
    fun losIdsSonUnicosAunqueSeAnadanEnElMismoInstante() {
        var e = abierto()
        repeat(3) { e = TemporizadoresLibres.anadir(e, 5, t0) }
        val ids = e.libres.map { it.id }.toSet()
        assertEquals(3, ids.size)
    }

    @Test
    fun losYaAvisadosNoCuentanParaElTopeYSeLimpianAlAnadir() {
        var e = abierto()
        repeat(3) { e = TemporizadoresLibres.anadir(e, 1, t0) }
        val v = AvisosCrono.vencidos(e, receta, t0 + 2 * min)
        assertEquals(3, v.estado.libres.count { it.avisado })
        assertTrue(TemporizadoresLibres.puedeAnadir(v.estado, t0 + 2 * min))
        val nuevo = TemporizadoresLibres.anadir(v.estado, 7, t0 + 2 * min)
        assertEquals(listOf(7), nuevo.libres.map { it.minutos })
    }

    @Test
    fun soloExisteEnUnaCocinadaAbiertaYSinCaducar() {
        val portada = SesionCrono.inicial(receta, t0 + 10 * min, t0)
        assertFalse(TemporizadoresLibres.puedeAnadir(portada, t0))
        assertEquals(portada, TemporizadoresLibres.anadir(portada, 5, t0))
        val e = abierto()
        val tarde = t0 + SesionCrono.CADUCIDAD_MS + 1
        assertFalse(TemporizadoresLibres.puedeAnadir(e, tarde))
    }

    @Test
    fun conLaCocinaTerminadaSigueAdmitiendoLibres() {
        val e = abierto().copy(carriles = mapOf("a" to EstadoCarril(fin = true)))
        assertTrue(TemporizadoresLibres.puedeAnadir(e, t0))
    }

    // ---- quitar / marcar ----

    @Test
    fun quitarEliminaSoloEseLibre() {
        var e = conLibre(5)
        e = TemporizadoresLibres.anadir(e, 10, t0)
        val (a, b) = e.libres
        val sin = TemporizadoresLibres.quitar(e, a.id, t0 + 5_000)
        assertEquals(listOf(b), sin.libres)
        assertEquals(t0 + 5_000, sin.actualizadoMs)
    }

    @Test
    fun quitarUnIdQueNoExisteNoCambiaNada() {
        val e = conLibre(5)
        assertEquals(e, TemporizadoresLibres.quitar(e, "nada", t0 + 5_000))
    }

    @Test
    fun marcarAvisadoSoloMarcaEseLibre() {
        var e = conLibre(5)
        e = TemporizadoresLibres.anadir(e, 10, t0)
        val (a, b) = e.libres
        val marcado = TemporizadoresLibres.marcarAvisado(e, a.id, t0)
        assertTrue(marcado.libres.first { it.id == a.id }.avisado)
        assertFalse(marcado.libres.first { it.id == b.id }.avisado)
    }

    // ---- vencer ----

    @Test
    fun vencidosSonLosQueHanLlegadoAlCeroYNoHanAvisado() {
        var e = conLibre(5)
        e = TemporizadoresLibres.anadir(e, 10, t0)
        assertEquals(emptyList(), TemporizadoresLibres.vencidos(e, t0 + 5 * min - 1))
        assertEquals(listOf(5), TemporizadoresLibres.vencidos(e, t0 + 5 * min).map { it.minutos })
        assertEquals(2, TemporizadoresLibres.vencidos(e, t0 + 10 * min).size)
        val avisado = TemporizadoresLibres.marcarAvisado(e, e.libres.first().id, t0)
        assertEquals(listOf(10), TemporizadoresLibres.vencidos(avisado, t0 + 10 * min).map { it.minutos })
    }

    @Test
    fun quedanSRedondeaHaciaArribaYPuedeSerNegativo() {
        val l = TemporizadorLibre("l", null, 1, t0)
        assertEquals(60, l.quedanS(t0))
        assertEquals(1, l.quedanS(t0 + 59_001))
        assertEquals(0, l.quedanS(t0 + 60_000))
        assertEquals(-5, l.quedanS(t0 + 65_000))
    }

    // ---- cocinada viva ----

    @Test
    fun unLibreSinAvisarMantieneVivaLaCocinadaConTodoTerminado() {
        val terminado = abierto().copy(carriles = mapOf("a" to EstadoCarril(fin = true)))
        assertFalse(SesionCrono.vivo(terminado, t0))
        val conLibre = TemporizadoresLibres.anadir(terminado, 5, t0)
        assertTrue(SesionCrono.vivo(conLibre, t0))
        // Vencido pero sin avisar sigue vivo: la cadena aún tiene que avisarlo.
        assertTrue(SesionCrono.vivo(conLibre, t0 + 6 * min))
    }

    @Test
    fun unLibreYaAvisadoNoMantieneViva() {
        val terminado = abierto().copy(carriles = mapOf("a" to EstadoCarril(fin = true)))
        val e = TemporizadoresLibres.anadir(terminado, 5, t0)
        val avisado = TemporizadoresLibres.marcarAvisado(e, e.libres.single().id, t0)
        assertFalse(SesionCrono.vivo(avisado, t0 + 6 * min))
    }

    @Test
    fun laCaducidadDe12HSeMantieneConUnLibre() {
        val terminado = abierto().copy(carriles = mapOf("a" to EstadoCarril(fin = true)))
        val e = TemporizadoresLibres.anadir(terminado, 180, t0)
        assertTrue(SesionCrono.vivo(e, t0 + SesionCrono.CADUCIDAD_MS))
        assertFalse(SesionCrono.vivo(e, t0 + SesionCrono.CADUCIDAD_MS + 1))
    }

    @Test
    fun conLaCocinaSinArrancarUnLibreNoLaHaceVivaPorqueNoSePuedeCrear() {
        val libre = TemporizadorLibre("l", null, 5, t0)
        val portada = SesionCrono.inicial(receta, t0, t0).copy(libres = listOf(libre))
        assertFalse(SesionCrono.vivo(portada, t0))
    }

    // ---- avisos ----

    @Test
    fun siguienteEventoIncluyeElLibre() {
        val e = conLibre(1)
        val ev = checkNotNull(AvisosCrono.siguienteEvento(e, receta))
        assertEquals(TipoAviso.TEMPORIZADOR, ev.tipo)
        assertEquals(t0 + min, ev.ms)
        assertEquals(TemporizadoresLibres.carrilDe(e.libres.single().id), ev.carril)
    }

    @Test
    fun conSoloUnLibrePendienteEsElUnicoEvento() {
        val terminado = abierto().copy(carriles = mapOf("a" to EstadoCarril(fin = true)))
        val e = TemporizadoresLibres.anadir(terminado, 5, t0)
        assertEquals(TipoAviso.TEMPORIZADOR, AvisosCrono.siguienteEvento(e, receta)?.tipo)
        val avisado = TemporizadoresLibres.marcarAvisado(e, e.libres.single().id, t0)
        assertNull(AvisosCrono.siguienteEvento(avisado, receta))
    }

    @Test
    fun vencidosAvisaElLibreUnaSolaVez() {
        val e = conLibre(10)
        val antes = AvisosCrono.vencidos(e, receta, t0 + 10 * min - 1)
        assertTrue(antes.avisos.none { it.tipo == TipoAviso.TEMPORIZADOR })
        val v = AvisosCrono.vencidos(e, receta, t0 + 10 * min)
        val aviso = v.avisos.single { it.tipo == TipoAviso.TEMPORIZADOR }
        assertEquals("Temporizador: 10 min", aviso.texto)
        assertEquals(t0 + 10 * min, aviso.instanteMs)
        assertFalse(aviso.silencioso)
        val libre = v.estado.libres.single()
        assertTrue(libre.avisado)
        val otra = AvisosCrono.vencidos(v.estado, receta, t0 + 10 * min + 1_000)
        assertTrue(otra.avisos.none { it.tipo == TipoAviso.TEMPORIZADOR })
    }

    @Test
    fun unLibreVencidoHaceMasDe90SegundosSaleSilencioso() {
        val e = conLibre(1)
        val v = AvisosCrono.vencidos(e, receta, t0 + min + AvisosCrono.SILENCIO_MS + 1)
        assertTrue(v.avisos.single { it.tipo == TipoAviso.TEMPORIZADOR }.silencioso)
    }

    @Test
    fun laEtiquetaSaleEnElTexto() {
        val e = TemporizadoresLibres.anadir(abierto(), 3, t0, etiqueta = "huevos")
        val v = AvisosCrono.vencidos(e, receta, t0 + 3 * min)
        assertEquals("Temporizador huevos: 3 min", v.avisos.single { it.tipo == TipoAviso.TEMPORIZADOR }.texto)
    }

    @Test
    fun elAgrupadorDaAlLibreUnaLineaPropiaSinNombreDeCarril() {
        val e = conLibre(10)
        val v = AvisosCrono.vencidos(e, receta, t0 + 10 * min)
        val lineas = AgrupadorAlarmas.lineasDe(v.avisos, mapOf("a" to "A"))
        assertEquals(listOf("Temporizador: 10 min"), lineas.map { it.texto })
        assertEquals(listOf(TemporizadoresLibres.carrilDe(e.libres.single().id)), lineas.map { it.carril })
    }

    // ---- fuera del resumen y del deshacer ----

    @Test
    fun noEntraEnElResumenNiEnPasosReales() {
        val e = conLibre(5)
        val resumen = SesionCrono.resumen(e, receta, t0 + 20 * min)
        assertEquals(emptyList(), resumen.pasosReales)
        assertEquals(1, resumen.carriles.size)
    }

    @Test
    fun hechoYDeshacerNoTocanLosLibres() {
        val e = conLibre(5)
        val hecho = MotorCrono.hecho(e, receta, "a", t0 + 1_000)
        assertEquals(e.libres, hecho.libres)
        assertTrue(hecho.deshacer.keys.all { it in e.carriles })
        val atras = MotorCrono.deshacer(hecho, receta, null, t0 + 2_000)
        assertEquals(e.libres, atras.libres)
    }

    // ---- serialización tolerante ----

    private val formato = Json { ignoreUnknownKeys = true }

    private fun decodificar(json: String) = formato.decodeFromString(EstadoCrono.serializer(), json)

    private val base = """"slug":"x","raciones":2,"servirMs":1,"carriles":{},"actualizadoMs":1"""

    @Test
    fun unEstadoViejoSinLibresEntraConListaVacia() {
        assertEquals(emptyList(), decodificar("{$base}").libres)
    }

    @Test
    fun idaYVueltaConservaLosLibres() {
        val e = conLibre(5)
        val vuelta = decodificar(formato.encodeToString(EstadoCrono.serializer(), e))
        assertEquals(e, vuelta)
    }

    @Test
    fun conLibresBienFormadosSeLeen() {
        val json = """{$base,"libres":[{"id":"l1","minutos":5,"inicioMs":10,"avisado":true}]}"""
        assertEquals(listOf(TemporizadorLibre("l1", null, 5, 10, true)), decodificar(json).libres)
    }

    @Test
    fun conLibresMalFormadosSePierdenLosLibresPeroNoLaCocinada() {
        val malos =
            listOf(
                """"libres":"hola"""",
                """"libres":{"a":1}""",
                """"libres":[{"id":"l1"}]""",
                """"libres":[{"id":"l1","minutos":"cinco","inicioMs":1}]""",
                """"libres":[1,2]""",
                """"libres":null""",
            )
        for (campo in malos) {
            val e = decodificar("""{$base,"foco":"a",$campo}""")
            assertEquals("x", e.slug, campo)
            assertEquals("a", e.foco, campo)
            assertEquals(emptyList(), e.libres, campo)
        }
    }

    @Test
    fun siEstadoCronoNoLlevaLibresNoLosEscribe() {
        val json = formato.encodeToString(EstadoCrono.serializer(), abierto())
        assertFalse("libres" in json)
    }
}
