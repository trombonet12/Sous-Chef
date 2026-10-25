package app.trombito.souschef.domain.copia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

private val AHORA = Instant.parse("2026-10-25T15:30:00Z").toEpochMilli()
private const val DIA_MS = 24 * 60 * 60 * 1000L

class NombresCopiaAutoTest {
    @Test
    fun elNombreLlevaFechaYHoraLocales() {
        assertEquals("souschef-historial-20261025-1530.json", NombresCopiaAuto.nombre(AHORA, ZoneOffset.UTC))
        // 25-oct-2026 el reloj ya ha vuelto a la hora de invierno (CET, UTC+1).
        val madrid = ZoneId.of("Europe/Madrid")
        assertEquals("souschef-historial-20261025-1630.json", NombresCopiaAuto.nombre(AHORA, madrid))
    }

    @Test
    fun elNombreGeneradoEsReconocidoComoAutomatico() {
        assertTrue(NombresCopiaAuto.esAutomatica(NombresCopiaAuto.nombre(AHORA, ZoneOffset.UTC)))
    }

    @Test
    fun reconoceElSufijoQueAnadeUnProveedorSiYaExistia() {
        assertTrue(NombresCopiaAuto.esAutomatica("souschef-historial-20261025-1530 (1).json"))
        assertTrue(NombresCopiaAuto.esAutomatica("souschef-historial-20261025-1530(2).json"))
    }

    @Test
    fun noReconoceLasCopiasManualesNiOtrosArchivos() {
        listOf(
            "sous-chef-historial-2026-10-25.json",
            "souschef-historial-20261025-1530.json.bak",
            "souschef-historial-20261025-1530",
            "souschef-historial-2026.json",
            "souschef-historial-20261025-1530-extra.json",
            "mi-souschef-historial-20261025-1530.json",
            "notas.json",
            "",
        ).forEach { assertFalse("«$it» no es automática", NombresCopiaAuto.esAutomatica(it)) }
    }
}

class RotacionCopiasTest {
    private fun auto(
        sello: String,
        sufijo: String = "",
    ) = ArchivoEnCarpeta("id-$sello$sufijo", "souschef-historial-$sello$sufijo.json")

    private fun nombres(l: List<ArchivoEnCarpeta>) = l.map { it.nombre.removePrefix("souschef-historial-") }

    @Test
    fun conMenosDeCincoNoSeBorraNada() {
        val existentes = listOf(auto("20261020-0900"), auto("20261021-0900"), auto("20261022-0900"))
        assertEquals(emptyList<ArchivoEnCarpeta>(), RotacionCopias.aBorrar(existentes, "nuevo"))
    }

    @Test
    fun conLaNuevaSeQuedanLasCincoMasRecientes() {
        val nueva = auto("20261025-1530")
        val existentes =
            listOf("20261019", "20261020", "20261021", "20261022", "20261023")
                .map { auto("$it-0900") } + auto("20261018-0900") + nueva
        val borrar = RotacionCopias.aBorrar(existentes, nueva.id)
        // 7 automáticas: sobran las 2 más viejas, de la más vieja a la más reciente.
        assertEquals(listOf("20261018-0900.json", "20261019-0900.json"), nombres(borrar))
    }

    @Test
    fun ordenaPorElSelloDelNombreNoPorElOrdenDeLaLista() {
        val existentes =
            listOf("20261022", "20261018", "20261024", "20261020", "20261023", "20261021").map { auto("$it-1200") }
        // Ninguna es la nueva: compiten las 6 y quedan las 4 más recientes (la nueva ocuparía la quinta plaza).
        val borrar = RotacionCopias.aBorrar(existentes, "nuevo")
        assertEquals(listOf("20261018-1200.json", "20261020-1200.json"), nombres(borrar))
    }

    @Test
    fun elSufijoDeUnProveedorCuentaComoMasReciente() {
        val base = auto("20261025-1530")
        val repetida = auto("20261025-1530", " (1)")
        val otras = listOf("20261021", "20261022", "20261023").map { auto("$it-0900") }
        val nueva = auto("20261026-0800")
        // 3 + base + repetida + nueva = 6 → sobra una: la más vieja (21), no la repetida ni la base.
        val borrar = RotacionCopias.aBorrar(otras + base + repetida + nueva, nueva.id)
        assertEquals(listOf("20261021-0900.json"), nombres(borrar))
        // A igual sello, la «(1)» es la más reciente: con una sola plaza para las otras, sobrevive ella y no la base.
        val porSufijo = RotacionCopias.aBorrar(otras + base + repetida, "ninguna", conservar = 2)
        assertEquals(
            listOf("20261021-0900.json", "20261022-0900.json", "20261023-0900.json", "20261025-1530.json"),
            nombres(porSufijo),
        )
    }

    @Test
    fun nuncaTocaLasCopiasManualesNiOtrosArchivos() {
        val manual = ArchivoEnCarpeta("m", "sous-chef-historial-2026-01-01.json")
        val otro = ArchivoEnCarpeta("o", "recetas.txt")
        val autos = (10..16).map { auto("202610$it-0900") }
        val borrar = RotacionCopias.aBorrar(listOf(manual, otro) + autos, "nuevo")
        assertEquals(3, borrar.size)
        assertTrue(borrar.none { it == manual || it == otro })
    }

    @Test
    fun unaSubcarpetaConNombreDeCopiaNuncaEntraEnLaRotacion() {
        val subcarpeta = ArchivoEnCarpeta("dir", "souschef-historial-20200101-0000.json", esCarpeta = true)
        val autos = (10..15).map { auto("202610$it-0900") }
        val borrar = RotacionCopias.aBorrar(listOf(subcarpeta) + autos, "nuevo")
        // Solo compiten las 6 copias de verdad: sobran 2 y la subcarpeta, la más vieja por nombre, no se toca.
        assertEquals(listOf("20261010-0900.json", "20261011-0900.json"), nombres(borrar))
        assertTrue(borrar.none { it.esCarpeta })
    }

    @Test
    fun laNuevaNuncaSeBorraAunqueElRelojHayaRetrocedido() {
        val nueva = auto("20200101-0000")
        val existentes = (10..14).map { auto("202610$it-0900") } + nueva
        val borrar = RotacionCopias.aBorrar(existentes, nueva.id)
        assertEquals(listOf("20261010-0900.json"), nombres(borrar))
        assertTrue(borrar.none { it.id == nueva.id })
    }

    @Test
    fun conservarUnaSoloDejaLaNueva() {
        val nueva = auto("20261025-1530")
        val existentes = listOf(auto("20261020-0900"), auto("20261021-0900"), nueva)
        val borrar = RotacionCopias.aBorrar(existentes, nueva.id, conservar = 1)
        assertEquals(2, borrar.size)
        assertFalse(borrar.contains(nueva))
    }
}

class AvisoCopiaAutoTest {
    private val activa = EstadoCopiaAuto(carpeta = "content://carpeta", ultimaAutoMs = AHORA - DIA_MS)

    @Test
    fun sinCarpetaNoSeAvisaAunqueQuedeUnFalloViejo() {
        assertFalse(AvisoCopiaAuto.debeAvisar(EstadoCopiaAuto(fallo = FalloCopiaAuto.CARPETA_NO_VALE), AHORA))
    }

    @Test
    fun sinFalloNoSeAvisa() {
        assertFalse(AvisoCopiaAuto.debeAvisar(activa, AHORA))
    }

    @Test
    fun laCarpetaQueNoValeAvisaEnElActo() {
        val e = activa.copy(fallo = FalloCopiaAuto.CARPETA_NO_VALE)
        assertTrue(AvisoCopiaAuto.debeAvisar(e, AHORA))
    }

    @Test
    fun unFalloPasajeroDeEscrituraNoAvisaSiLaUltimaCopiaEsReciente() {
        val e = activa.copy(fallo = FalloCopiaAuto.NO_SE_PUDO)
        assertFalse(AvisoCopiaAuto.debeAvisar(e, AHORA))
        assertFalse(AvisoCopiaAuto.debeAvisar(e, AHORA + 5 * DIA_MS))
    }

    @Test
    fun unFalloDeEscrituraAvisaAlLlegarALaTolerancia() {
        val e = activa.copy(ultimaAutoMs = AHORA, fallo = FalloCopiaAuto.NO_SE_PUDO)
        assertFalse(AvisoCopiaAuto.debeAvisar(e, AHORA + 7 * DIA_MS - 1))
        assertTrue(AvisoCopiaAuto.debeAvisar(e, AHORA + 7 * DIA_MS))
    }

    @Test
    fun unFalloDeEscrituraSinNingunaCopiaAutomaticaAvisa() {
        val e = EstadoCopiaAuto(carpeta = "content://carpeta", fallo = FalloCopiaAuto.NO_SE_PUDO)
        assertTrue(AvisoCopiaAuto.debeAvisar(e, AHORA))
    }

    @Test
    fun unaCopiaManualNoSilenciaElFalloDeLaAutomatica() {
        val e = EstadoCopiaAuto("content://c", ultimaManualMs = AHORA, fallo = FalloCopiaAuto.NO_SE_PUDO)
        assertTrue(AvisoCopiaAuto.debeAvisar(e, AHORA))
    }
}

class UltimaCopiaTest {
    private val utc = ZoneOffset.UTC

    @Test
    fun nuncaSiNoHayCopia() {
        assertNull(UltimaCopia.dias(null, AHORA, utc))
    }

    @Test
    fun hoyEsCero() {
        assertEquals(0L, UltimaCopia.dias(AHORA - 3_600_000, AHORA, utc))
    }

    @Test
    fun cuentaDiasDeCalendarioNoDeVeinticuatroHoras() {
        val ayerNoche = Instant.parse("2026-10-24T23:59:00Z").toEpochMilli()
        val hoyMadrugada = Instant.parse("2026-10-25T00:01:00Z").toEpochMilli()
        assertEquals(1L, UltimaCopia.dias(ayerNoche, hoyMadrugada, utc))
    }

    @Test
    fun haceNDias() {
        assertEquals(12L, UltimaCopia.dias(AHORA - 12 * DIA_MS, AHORA, utc))
    }

    @Test
    fun unRelojAdelantadoNuncaDaNegativo() {
        assertEquals(0L, UltimaCopia.dias(AHORA + 3 * DIA_MS, AHORA, utc))
    }
}

class EstadoCopiaAutoTest {
    @Test
    fun sinFilasEsUnEstadoVacio() {
        val e = EstadoCopiaAuto.decodificar(emptyMap())
        assertEquals(EstadoCopiaAuto(), e)
        assertFalse(e.activa)
        assertNull(e.ultimaCopiaMs)
    }

    @Test
    fun leeTodasLasClaves() {
        val e =
            EstadoCopiaAuto.decodificar(
                mapOf(
                    EstadoCopiaAuto.CLAVE_CARPETA to "content://carpeta",
                    EstadoCopiaAuto.CLAVE_ULTIMA_AUTO to "1000",
                    EstadoCopiaAuto.CLAVE_ULTIMA_MANUAL to "2000",
                    EstadoCopiaAuto.CLAVE_FALLO to "CARPETA_NO_VALE",
                ),
            )
        assertEquals(EstadoCopiaAuto("content://carpeta", 1000, 2000, FalloCopiaAuto.CARPETA_NO_VALE), e)
        assertTrue(e.activa)
        assertEquals(2000L, e.ultimaCopiaMs)
    }

    @Test
    fun vacioIlegibleODesconocidoCuentaComoAusente() {
        val e =
            EstadoCopiaAuto.decodificar(
                mapOf(
                    EstadoCopiaAuto.CLAVE_CARPETA to "  ",
                    EstadoCopiaAuto.CLAVE_ULTIMA_AUTO to "ayer",
                    EstadoCopiaAuto.CLAVE_ULTIMA_MANUAL to "-5",
                    EstadoCopiaAuto.CLAVE_FALLO to "EXPLOTO",
                ),
            )
        assertEquals(EstadoCopiaAuto(), e)
    }

    @Test
    fun laUltimaCopiaEsLaMasRecienteDeLasDos() {
        assertEquals(500L, EstadoCopiaAuto(ultimaAutoMs = 500, ultimaManualMs = 100).ultimaCopiaMs)
        assertEquals(900L, EstadoCopiaAuto(ultimaAutoMs = 500, ultimaManualMs = 900).ultimaCopiaMs)
        assertEquals(100L, EstadoCopiaAuto(ultimaManualMs = 100).ultimaCopiaMs)
    }

    @Test
    fun laUriNuncaSaleEnToString() {
        val texto = EstadoCopiaAuto(carpeta = "content://com.android.externalstorage/tree/secreto%3ADocs").toString()
        assertFalse(texto.contains("secreto"))
        assertFalse(texto.contains("content://"))
    }
}
