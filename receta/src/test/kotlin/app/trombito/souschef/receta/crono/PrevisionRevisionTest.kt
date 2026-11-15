package app.trombito.souschef.receta.crono

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regresión de la revisión de arquitectura del ADR-0018: avisos «en el acto» tras un mando, `avisado` solo con hora
 * firme, `arrancada` con estados viejos y fin firme de un paso que justo toca.
 */
class PrevisionRevisionTest {
    private val s0 = 1_700_000_000_000L
    private val min = 60_000L

    private fun m(n: Int): Long = n * min

    @Suppress("LongParameterList")
    private fun paso(
        id: String,
        carril: String,
        off: Double,
        s: Int,
        junto: String? = null,
    ) = Paso(id = id, t = "T-$id", carril = carril, off = off, min = s / 60.0, s = s, junto = junto)

    private fun receta(
        total: Double,
        vararg pasos: Paso,
    ) = Receta(
        slug = "x",
        titulo = "X",
        totalMin = total,
        carriles = listOf(Carril("mesa", "MESA", "#fff", "mesa"), Carril("horno", "HORNO", "#fff", "horno")),
        pasos = pasos.toList(),
    )

    private fun nueva(
        r: Receta,
        t0: Long,
    ): EstadoCrono = SesionCrono.abrir(SesionCrono.inicial(r, t0 + m(r.totalMin.toInt()), t0 - m(60)), r, t0 - m(60))

    // ---- 1 · «Toca empezar» que nace vencido por un mando suena ----

    private val rA = receta(10.0, paso("m1", "mesa", 0.0, 600), paso("h1", "horno", 5.0, 300))

    @Test
    fun hechoSinArrancarHaceSonarLaRaizQueNaceVencida() {
        val t0 = s0
        val ahora = t0 + m(10)
        val e = nueva(rA, t0).let { MotorCrono.hecho(it, rA, "mesa", ahora) }
        // A = t0 (hecho − dur): h1 = t0+5, firme y con 5 min de retraso.
        assertEquals(t0 + m(5), ConsultasCrono.previstos(e, rA, ahora).getValue("h1").inicioMs)
        val aviso = AvisosCrono.vencidos(e, rA, ahora).avisos.single { it.carril == "horno" }
        assertEquals(TipoAviso.TOCA_EMPEZAR, aviso.tipo)
        assertFalse(aviso.silencioso, "tras un Hecho el aviso debe sonar en el acto")
    }

    @Test
    fun unTocaVencidoPorDozeSigueSilencioso() {
        val t0 = s0
        val e = nueva(rA, t0).let { MotorCrono.empezar(it, rA, "mesa", t0) }
        val aviso = AvisosCrono.vencidos(e, rA, t0 + m(8)).avisos.single { it.carril == "horno" }
        assertEquals(t0 + m(5), aviso.instanteMs)
        assertTrue(aviso.silencioso)
    }

    // ---- 2 · `avisado` solo si la hora es firme ----

    private val rB =
        receta(
            15.0,
            paso("a1", "mesa", 0.0, 600),
            paso("a2", "mesa", 10.0, 300),
            paso("b1", "horno", 0.0, 300),
            paso("b2", "horno", 5.0, 300, junto = "a2"),
        )

    @Test
    fun hechoNoDaPorAvisadoUnaHoraFlotanteDeUnJuntoConCompaneroSinArrancar() {
        val t0 = s0
        val e =
            nueva(rB, t0)
                .let { MotorCrono.empezar(it, rB, "mesa", t0) }
                .let { MotorCrono.empezar(it, rB, "horno", t0) }
                .let { MotorCrono.pausar(it, rB, "mesa", t0 + m(9)) }
                .let { MotorCrono.hecho(it, rB, "horno", t0 + m(10)) }
        val b2 = ConsultasCrono.previstos(e, rB, t0 + m(10)).getValue("b2")
        assertTrue(b2.inicioMs <= t0 + m(10))
        assertFalse(b2.firme)
        assertFalse(e.carriles.getValue("horno").avisado, "no avisado: su hora se mueve con el compañero")
        // Al arrancar el compañero (a2 hecho a1), la hora vuelve a ser firme y su aviso puede sonar.
        val tras = MotorCrono.reanudar(e, rB, "mesa", t0 + m(10))
        assertFalse(tras.carriles.getValue("horno").avisado)
    }

    private val rC =
        receta(10.0, paso("a1", "mesa", 5.0, 300), paso("b1", "horno", 0.0, 600, junto = "a1"))

    @Test
    fun abrirNoDaPorAvisadaUnaHoraFlotante() {
        val t0 = s0
        val ahora = t0 + m(10)
        val e = SesionCrono.abrir(SesionCrono.inicial(rC, t0 + m(10), ahora), rC, ahora)
        val b1 = ConsultasCrono.previstos(e, rC, ahora).getValue("b1")
        assertTrue(b1.inicioMs <= ahora)
        assertFalse(b1.firme)
        assertFalse(e.carriles.getValue("horno").avisado)
    }

    // ---- 3 · `arrancada` con estados viejos ----

    @Test
    fun arrancadaReconoceProgresoDeUnEstadoViejoSinMapas() {
        val base = nueva(rA, s0)
        assertFalse(ConsultasCrono.arrancada(base))
        assertTrue(ConsultasCrono.arrancada(base.copy(pasosReales = mapOf("m1" to 30))))
        val mesa = base.carriles.getValue("mesa")
        listOf(
            mesa.copy(finAtMs = s0 + min),
            mesa.copy(pausado = true, restanteS = 10),
            mesa.copy(i = 1),
            mesa.copy(fin = true),
        ).forEach { ec ->
            assertTrue(ConsultasCrono.arrancada(base.copy(carriles = base.carriles + ("mesa" to ec))), "$ec")
        }
    }

    // ---- 4 · un paso que justo toca no es fin firme ----

    private val rD =
        receta(
            15.0,
            paso("s1", "mesa", 0.0, 600),
            paso("s2", "mesa", 10.0, 300),
            paso("o1", "horno", 15.0, 300, junto = "s2"),
        )

    @Test
    fun unPasoPendienteQueTocaAhoraNoArmaAlarmasFirmesEnSusJunto() {
        val t0 = s0
        val ahora = t0 + m(10)
        val e =
            nueva(rD, t0)
                .let { MotorCrono.empezar(it, rD, "mesa", t0) }
                .let { MotorCrono.hecho(it, rD, "mesa", ahora) }
        val previstos = ConsultasCrono.previstos(e, rD, ahora)
        assertEquals(ahora, previstos.getValue("s2").inicioMs)
        assertFalse(previstos.getValue("o1").firme)
        assertNull(AvisosCrono.siguienteEventoFuera(e, rD, "mesa", ahora))
        // En cuanto s2 arranca, su junto vuelve a ser firme y sí arma el aviso.
        val arrancado = MotorCrono.empezar(e, rD, "mesa", ahora)
        val ev = checkNotNull(AvisosCrono.siguienteEventoFuera(arrancado, rD, "mesa", ahora))
        assertEquals(TipoAviso.TOCA_EMPEZAR, ev.tipo)
        assertEquals(t0 + m(15), ev.ms)
    }
}
