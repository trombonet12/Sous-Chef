package app.trombito.souschef.domain.crono

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RecordatorioEmpezarTest {
    private val zona = ZoneId.of("Europe/Madrid")

    private fun ms(
        hora: Int,
        minuto: Int,
        segundo: Int = 0,
    ): Long =
        LocalDate
            .of(2026, 10, 14)
            .atTime(hora, minuto, segundo)
            .atZone(zona)
            .toInstant()
            .toEpochMilli()

    private val ahora = ms(12, 0)

    private fun min(
        hora: Int,
        minuto: Int,
    ) = hora * 60 + minuto

    @Test
    fun laHoraDeEmpezarEsLaDeHoyEnPuntoYEnLaZonaDelMovil() {
        assertEquals(ms(14, 43), RecordatorioEmpezar.proximoMs(min(14, 43), ahora, zona))
    }

    @Test
    fun siLaHoraYaPasoONoHayMargenNoHayRecordatorio() {
        assertNull(RecordatorioEmpezar.proximoMs(min(11, 59), ahora, zona))
        assertNull(RecordatorioEmpezar.proximoMs(min(12, 0), ahora, zona))
        // Las 12:00 con segundos: ya pasaron.
        assertNull(RecordatorioEmpezar.proximoMs(min(12, 0), ms(12, 0, 20), zona))
        assertEquals(ms(12, 1), RecordatorioEmpezar.proximoMs(min(12, 1), ms(12, 0, 20), zona))
    }

    @Test
    fun elRecordatorioLlevaLaHoraDeServirQueSigueALaDeEmpezar() {
        val r = RecordatorioEmpezar.crear("sopa", min(14, 43), min(15, 30), ahora, zona)
        assertEquals(Recordatorio("sopa", ms(14, 43), ms(15, 30)), r)
    }

    @Test
    fun laHoraDeServirCruzaLaMedianoche() {
        val r = RecordatorioEmpezar.crear("sopa", min(23, 30), min(0, 15), ahora, zona)
        assertEquals(ms(23, 30), r?.empezarMs)
        assertEquals(ms(23, 30) + 45 * 60_000L, r?.servirMs)
    }

    @Test
    fun noSeCreaSiLaHoraDeEmpezarYaPaso() {
        assertNull(RecordatorioEmpezar.crear("sopa", min(9, 0), min(15, 30), ahora, zona))
    }

    private val guardado = Recordatorio("sopa", ms(14, 43), ms(15, 30))

    @Test
    fun alReleerSinRecordatorioNoHayNada() {
        assertEquals(AccionRecordatorio.Nada, RecordatorioEmpezar.alReleer(null, ahora))
    }

    @Test
    fun alReleerAntesDeLaHoraSeVuelveAArmar() {
        assertEquals(AccionRecordatorio.Armar(ms(14, 43)), RecordatorioEmpezar.alReleer(guardado, ahora))
        assertEquals(AccionRecordatorio.Armar(ms(14, 43)), RecordatorioEmpezar.alReleer(guardado, ms(14, 42, 59)))
    }

    @Test
    fun trasUnReinicioSiLaHoraPasoHaceDiezMinutosOMenosAvisa() {
        assertEquals(AccionRecordatorio.Avisar, RecordatorioEmpezar.alReleer(guardado, ms(14, 43)))
        assertEquals(AccionRecordatorio.Avisar, RecordatorioEmpezar.alReleer(guardado, ms(14, 50)))
        assertEquals(AccionRecordatorio.Avisar, RecordatorioEmpezar.alReleer(guardado, ms(14, 53)))
    }

    @Test
    fun trasUnReinicioSiLaHoraPasoHaceMasDeDiezMinutosSeBorraSinAvisar() {
        assertEquals(AccionRecordatorio.Borrar, RecordatorioEmpezar.alReleer(guardado, ms(14, 53, 1)))
        assertEquals(AccionRecordatorio.Borrar, RecordatorioEmpezar.alReleer(guardado, ms(18, 0)))
    }

    @Test
    fun seGuardaComoTextoYSeLeeIgual() {
        val texto = RecordatorioEmpezar.codificar(guardado)
        assertEquals("sopa|${guardado.empezarMs}|${guardado.servirMs}", texto)
        assertEquals(guardado, RecordatorioEmpezar.decodificar(texto))
    }

    @Test
    fun unTextoAusenteVacioOMalFormadoCuentaComoSinRecordatorio() {
        assertEquals("", RecordatorioEmpezar.codificar(null))
        listOf(null, "", "sopa", "sopa|1", "sopa|a|2", "sopa|1|b", "|1|2", "sopa|1|2|3").forEach {
            assertNull(it, RecordatorioEmpezar.decodificar(it))
        }
    }

    @Test
    fun elChipOfreceLaHoraQueEnsenaLaFicha() {
        val chip = RecordatorioEmpezar.chip("sopa", min(14, 43), null, hayViva = false, ahoraMs = ahora, zona = zona)
        assertEquals(ChipAviso(min(14, 43), puesto = false), chip)
    }

    @Test
    fun elChipNoSaleSiLaHoraYaPasoHayCocinadaVivaOFaltaLaReceta() {
        assertNull(RecordatorioEmpezar.chip("sopa", min(9, 0), null, hayViva = false, ahoraMs = ahora, zona = zona))
        assertNull(RecordatorioEmpezar.chip("sopa", min(14, 43), null, hayViva = true, ahoraMs = ahora, zona = zona))
        assertNull(RecordatorioEmpezar.chip("sopa", null, null, hayViva = false, ahoraMs = ahora, zona = zona))
    }

    @Test
    fun elChipDeUnAvisoPuestoEnEstaRecetaSePuedeQuitarSiempreConSuHora() {
        val chip = RecordatorioEmpezar.chip("sopa", min(9, 0), guardado, hayViva = true, ahoraMs = ahora, zona = zona)
        assertEquals(ChipAviso(min(14, 43), puesto = true), chip)
    }

    @Test
    fun elAvisoPuestoDeOtraRecetaNoImpideOfrecerEsta() {
        val otra = guardado.copy(slug = "arroz")
        val chip = RecordatorioEmpezar.chip("sopa", min(14, 0), otra, hayViva = false, ahoraMs = ahora, zona = zona)
        assertEquals(ChipAviso(min(14, 0), puesto = false), chip)
    }
}
