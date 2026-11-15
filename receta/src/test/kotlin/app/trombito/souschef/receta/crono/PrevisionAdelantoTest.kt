package app.trombito.souschef.receta.crono

import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.leerReceta
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Auditoría final H1 (ADR-0018, anexo «hueco de cola»): la cola de un paso con `dep` o `junto` solo pide el carril
 * libre; el hueco de la receta no hace de suelo cuando sus enlaces terminan antes. Sin desvíos, nada cambia.
 */
class PrevisionAdelantoTest {
    private val servir = 1_700_000_000_000L
    private val min = 60_000L

    private fun receta(slug: String): Receta = leerReceta(File("../data/recetas/$slug.json").readText())

    private fun origen(r: Receta): Long = servir - Plano.medioMinutoMs(r.totalEfectivo)

    /** Un paso cocinado de verdad: su carril, y su arranque y su Hecho en minutos desde `O_plan`. */
    private data class Tramo(
        val carril: String,
        val desde: Int,
        val hasta: Int,
    )

    /** Aplica los tramos en orden de reloj (a igual hora, los Hecho antes que los arranques). */
    private fun cocinar(
        r: Receta,
        vararg tramos: Tramo,
    ): EstadoCrono {
        val o = origen(r)
        val eventos =
            tramos
                .flatMap { listOf(Triple(it.desde, 1, it.carril), Triple(it.hasta, 0, it.carril)) }
                .sortedWith(compareBy({ it.first }, { it.second }))
        return eventos.fold(SesionCrono.inicial(r, servir, o - 60 * min)) { e, (t, tipo, carril) ->
            val ms = o + t * min
            if (tipo == 0) MotorCrono.hecho(e, r, carril, ms) else MotorCrono.empezar(e, r, carril, ms)
        }
    }

    @Test
    fun arrozALaCubanaMontaEnCuantoAcabanSartenYCazuela() {
        val r = receta("arroz-a-la-cubana-con-bacon")
        // Todo a su hora salvo el arroz (4 min antes) y el punto de la sartén (4 min antes): a O+60 ambos hechos.
        val e =
            cocinar(
                r,
                Tramo("mesa", 0, 4),
                Tramo("mesa", 4, 10),
                Tramo("sarten", 4, 11),
                Tramo("sarten", 11, 12),
                Tramo("sarten", 12, 22),
                Tramo("sarten", 22, 23),
                Tramo("sarten", 23, 24),
                Tramo("sarten", 24, 28),
                Tramo("sarten", 28, 40),
                Tramo("sarten", 40, 56),
                Tramo("sarten", 56, 58),
                Tramo("sarten", 58, 60),
                Tramo("cazuela", 40, 48),
                Tramo("cazuela", 48, 56),
                Tramo("cazuela", 56, 60),
            )
        val ahora = origen(r) + 60 * min
        assertTrue(e.carriles.getValue("sarten").fin && e.carriles.getValue("cazuela").fin)
        val montar = ConsultasCrono.previstos(e, r, ahora).getValue("alinar-el-arroz-y-montar")
        // Antes: cebolla-y-ajo hecha a O+10 + hueco de 54 min = O+64, aunque sus dos `dep` acabaron a O+60.
        assertEquals(ahora, montar.inicioMs)
        assertTrue(montar.firme)
    }

    @Test
    fun huevosRotosElReposoSigueALaFrituraQueAcabaAntes() {
        val r = receta("huevos-rotos")
        // 1ª fritura hecha 5 min antes (O+24 en vez de O+29); el jamón, a su hora (hecho a O+17).
        val e =
            cocinar(
                r,
                Tramo("mesa", 0, 6),
                Tramo("sarten", 6, 11),
                Tramo("sarten", 11, 24),
                Tramo("mesa", 12, 17),
            )
        val ahora = origen(r) + 24 * min
        val previstos = ConsultasCrono.previstos(e, r, ahora)
        // Antes: rasgar-jamon hecho a O+17 + hueco de 12 min = O+29.
        assertEquals(ahora, previstos.getValue("reposo-patata").inicioMs)
        // Y lo que va `junto` al reposo se adelanta con él.
        assertEquals(ahora, previstos.getValue("subir-al-5").inicioMs)
    }

    @Test
    fun unJuntoConservaSuDesfaseAunqueElCompaneroAcabeAntes() {
        val r = receta("huevos-rotos")
        val e = cocinar(r, Tramo("mesa", 0, 6), Tramo("sarten", 6, 11), Tramo("sarten", 11, 24))
        // calentar-sarten-pequena va `junto` a la 1ª fritura (arrancada a O+11): su desfase de 17 min no se toca.
        val ahora = origen(r) + 24 * min
        val pequena = ConsultasCrono.previstos(e, r, ahora).getValue("calentar-sarten-pequena")
        assertEquals(origen(r) + 28 * min, pequena.inicioMs)
    }

    /**
     * Invariante principal con la cocina en marcha: cocinando cada paso de las 33 recetas a su hora (arranque a
     * `O_plan + off`, Hecho a `O_plan + finPlan`), tras cada mando todo paso pendiente sigue previsto a `O_plan + off`.
     */
    @Test
    fun sinDesviosConLaCocinaEnMarchaElPrevistoEsLaHoraDeHoy() {
        val recetas = File("../data/recetas").listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }
        assertTrue(recetas.size >= 33, "esperaba las 33 recetas, hay ${recetas.size}")
        for (f in recetas) comprobarSinDesvios(leerReceta(f.readText()))
    }

    private fun comprobarSinDesvios(r: Receta) {
        val o = origen(r)
        val eventos =
            r.pasos
                .flatMap { p ->
                    val ini = o + Plano.medioMinutoMs(p.off)
                    listOf(Triple(ini, 1, p), Triple(ini + p.segundos * 1000L, 0, p))
                }.sortedWith(compareBy({ it.first }, { it.second }))
        var e = SesionCrono.inicial(r, servir, o - 60 * min)
        val arrancados = HashSet<String>()
        for ((ms, tipo, p) in eventos) {
            e =
                if (tipo == 0) {
                    MotorCrono.hecho(e, r, p.carril, ms)
                } else {
                    MotorCrono.empezar(e, r, p.carril, ms)
                }
            if (tipo == 1) arrancados += p.id
            assertTrue(tipo == 0 || e.inicioRealMs[p.id] == ms, "${r.slug}/${p.id} no arrancó a su hora")
            val previstos = ConsultasCrono.previstos(e, r, ms)
            for (q in r.pasos.filterNot { it.id in arrancados }) {
                assertEquals(o + Plano.medioMinutoMs(q.off), previstos.getValue(q.id).inicioMs, "${r.slug}/${q.id}")
            }
        }
    }
}
