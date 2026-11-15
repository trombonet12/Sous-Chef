package app.trombito.souschef.domain.crono

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.MotorCrono
import app.trombito.souschef.receta.crono.SesionCrono
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.crono.FakeAlarmaSistema
import app.trombito.souschef.ui.crono.FakeCronoEstadoRepo
import app.trombito.souschef.ui.crono.FakeNotificador
import app.trombito.souschef.ui.crono.RelojFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** ADR-0018, escenario 7: la alarma del sistema sigue al cocinero (junto ancla al arranque real del compañero). */
class CadenaAlarmasPrevisionTest {
    private val min = 60_000L
    private val servir = 1_700_000_000_000L
    private val t0 = servir - 37 * min

    @Suppress("LongParameterList")
    private fun paso(
        id: String,
        carril: String,
        off: Double,
        s: Int,
        dep: List<String> = emptyList(),
        junto: String? = null,
    ) = Paso(id = id, t = "Paso $id", carril = carril, off = off, min = s / 60.0, s = s, dep = dep, junto = junto)

    private val receta =
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

    private val repo = FakeCronoEstadoRepo()
    private val reloj = RelojFalso()
    private val sistema = FakeAlarmaSistema()
    private val almacen = AlmacenCrono(repo)
    private val cadena =
        CadenaAlarmas(almacen, FakeRecetasRepo(listOf(receta)), reloj, sistema, FakeNotificador(), PresenciaCrono())

    private fun h8(): EstadoCrono {
        val abrir = t0 - 60 * min
        return SesionCrono
            .abrir(SesionCrono.inicial(receta, servir, abrir), receta, abrir)
            .let { MotorCrono.empezar(it, receta, "mesa", t0) }
            .let { MotorCrono.hecho(it, receta, "mesa", t0 + 5 * min) }
            .let { MotorCrono.empezar(it, receta, "sarten", t0 + 5 * min) }
            .let { MotorCrono.hecho(it, receta, "sarten", t0 + 8 * min) }
            .let { MotorCrono.empezar(it, receta, "sarten", t0 + 8 * min) }
    }

    @Test
    fun laAlarmaDelSistemaSigueAlArranqueRealDelCompanero() =
        runTest {
            val e = h8()
            almacen.fijar(e)
            reloj.ms = t0 + 8 * min
            cadena.reprogramar()
            // Toca el horno (h1, off 12) a T0+12; o1 queda para T0+13, anclado al arranque real de s2 (hoy, T0+20).
            assertEquals(t0 + 12 * min, sistema.programada)

            reloj.ms = t0 + 12 * min
            almacen.fijar(MotorCrono.empezar(e, receta, "horno", t0 + 12 * min))
            cadena.reprogramar()
            assertEquals(t0 + 13 * min, sistema.programada)
        }
}
