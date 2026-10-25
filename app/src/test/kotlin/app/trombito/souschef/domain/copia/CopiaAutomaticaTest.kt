package app.trombito.souschef.domain.copia

import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.HistorialRepo
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakePreferenciasRepo
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.crono.RelojFalso
import app.trombito.souschef.ui.receta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneOffset
import kotlin.coroutines.EmptyCoroutineContext

private val T_CIERRE = Instant.parse("2026-10-25T15:30:00Z").toEpochMilli()
private const val NOMBRE_NUEVO = "souschef-historial-20261025-1530.json"

/** Estado en memoria con la misma semántica que `CopiaAutoRepoRoom`. */
private class EstadoEnMemoria : CopiaAutoRepo {
    val datos = MutableStateFlow(EstadoCopiaAuto())

    override fun observar(): Flow<EstadoCopiaAuto> = datos

    override suspend fun leer(): EstadoCopiaAuto = datos.value

    override suspend fun fijarCarpeta(carpeta: String?) {
        datos.value = datos.value.copy(carpeta = carpeta, fallo = null)
    }

    override suspend fun registrarCopia(ms: Long) {
        datos.value = datos.value.copy(ultimaAutoMs = ms, fallo = null)
    }

    override suspend fun registrarFallo(fallo: FalloCopiaAuto) {
        datos.value = datos.value.copy(fallo = fallo)
    }

    override suspend fun registrarManual(ms: Long) {
        datos.value = datos.value.copy(ultimaManualMs = ms)
    }
}

/**
 * Una carpeta SAF en memoria. La escritura pasa por la `EscrituraSegura` de verdad, así que también se comprueba que
 * no se crea nada si la copia no es válida. Cada paso puede fallar.
 */
private class CarpetaEnMemoria : CarpetaCopia {
    /** id -> (nombre, contenido). */
    val archivos = linkedMapOf<String, Pair<String, ByteArray>>()
    val soltadas = mutableListOf<String>()
    val borrados = mutableListOf<String>()
    var creaciones = 0

    var vale = true
    var adoptable = true
    var motivoRechazo: ResultadoEscritura? = null
    var releeNulo = false
    var releeCorrupto = false
    var noLista = false
    var noBorra = emptySet<String>()

    fun poner(nombre: String): String {
        val id = "doc-$nombre"
        archivos[id] = nombre to "viejo".toByteArray()
        return id
    }

    val nombres get() = archivos.values.map { it.first }.sorted()

    fun contenidoUnico(): ByteArray = archivos.values.single().second

    override suspend fun adoptar(carpeta: String): Boolean = adoptable

    override suspend fun soltar(carpeta: String) {
        soltadas += carpeta
    }

    override suspend fun crear(
        carpeta: String,
        nombre: String,
        bytes: ByteArray,
        valida: (ByteArray) -> Boolean,
    ): ResultadoCreacion {
        if (!vale) return ResultadoCreacion.CarpetaNoVale
        val flujo = ByteArrayOutputStream()
        var id: String? = null
        val resultado =
            motivoRechazo
                ?: EscrituraSegura.escribir(bytes, valida) {
                    // Un proveedor que repite el nombre lo renombra con « (1)».
                    var candidato = nombre
                    var n = 0
                    while (archivos.values.any { it.first == candidato }) {
                        n++
                        candidato = nombre.removeSuffix(".json") + " ($n).json"
                    }
                    val nuevoId = "doc-$candidato"
                    id = nuevoId
                    creaciones++
                    archivos[nuevoId] = candidato to ByteArray(0)
                    flujo
                }
        val creado = id
        return when {
            resultado == ResultadoEscritura.Escrita && creado != null -> {
                archivos[creado] = archivos.getValue(creado).first to flujo.toByteArray()
                ResultadoCreacion.Creado(creado)
            }

            else -> {
                creado?.let { archivos.remove(it) }
                ResultadoCreacion.Rechazada(resultado)
            }
        }
    }

    override suspend fun leer(documento: String): ByteArray? =
        when {
            releeNulo -> null
            releeCorrupto -> "esto no es una copia".toByteArray()
            else -> archivos[documento]?.second
        }

    override suspend fun listar(carpeta: String): List<ArchivoEnCarpeta>? =
        if (noLista) null else archivos.map { (id, v) -> ArchivoEnCarpeta(id, v.first) }

    override suspend fun borrar(documento: String): Boolean {
        borrados += documento
        return documento !in noBorra && archivos.remove(documento) != null
    }
}

/** Para `GestorCopia`: aquí solo se usa `preparar` y `exportar`. */
private class ArchivoFalso(
    var fallar: Boolean = false,
) : ArchivoCopia {
    override suspend fun escribir(
        destino: String,
        texto: String,
    ): Boolean = !fallar

    override suspend fun leer(origen: String): String? = null
}

@OptIn(ExperimentalCoroutinesApi::class)
class CopiaAutomaticaTest {
    private val curry = EntradaHistorial("curry-2026-10-01", "curry", "Curry", "2026-10-01")
    private val tacos = EntradaHistorial("tacos-2026-10-05", "tacos", "Tacos", "2026-10-05")
    private val historial = FakeHistorialRepo(listOf(curry, tacos))
    private val recetas = FakeRecetasRepo(listOf(receta("curry"), receta("tacos")))
    private val reloj = RelojFalso(T_CIERRE)
    private val estado = EstadoEnMemoria()
    private val carpeta = CarpetaEnMemoria()
    private val errores = mutableListOf<String>()

    private fun gestor(repo: HistorialRepo = historial) =
        GestorCopia(repo, recetas, FakePreferenciasRepo(), ArchivoFalso(), reloj, estado)

    private fun copia(
        scope: CoroutineScope = CoroutineScope(SupervisorJob()),
        repo: HistorialRepo = historial,
    ) = CopiaAutomatica(
        gestor(repo),
        carpeta,
        estado,
        reloj,
        scope,
        contexto = EmptyCoroutineContext,
        zona = ZoneOffset.UTC,
        alFallar = { errores += it },
    )

    private fun conCarpeta() {
        estado.datos.value = EstadoCopiaAuto(carpeta = "content://carpeta")
    }

    private fun TestScope.ambito() = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

    // --- La secuencia escribir -> releer -> rotar ---------------------------------------------------------------

    @Test
    fun sinCarpetaNoSeHaceNada() =
        runTest {
            assertEquals(ResultadoAuto.SinCarpeta, copia().copiarAhora())
            assertEquals(0, carpeta.creaciones)
        }

    @Test
    fun laCopiaBuenaSeEscribeConElNombreDelMinutoYSeRegistra() =
        runTest {
            conCarpeta()
            assertEquals(ResultadoAuto.Hecha(0), copia().copiarAhora())
            assertEquals(listOf(NOMBRE_NUEVO), carpeta.nombres)
            // Lleva el historial entero y se puede volver a leer con el lector de «Cargar copia».
            val bytes = carpeta.contenidoUnico()
            val leida = CopiaHistorial.leer(String(bytes, Charsets.UTF_8), setOf("curry", "tacos"))
            val entradas = leida?.historial.orEmpty()
            val ids = entradas.map { it.id }.toSet()
            assertEquals(setOf("curry-2026-10-01", "tacos-2026-10-05"), ids)
            assertEquals(T_CIERRE, estado.datos.value.ultimaAutoMs)
            assertNull(estado.datos.value.fallo)
        }

    @Test
    fun rotaSoloDespuesDeRellenarYDejaCincoSinTocarManualesNiOtros() =
        runTest {
            conCarpeta()
            (10..15).forEach { carpeta.poner("souschef-historial-202610$it-0900.json") }
            carpeta.poner("sous-chef-historial-2026-10-01.json")
            carpeta.poner("fotos.zip")

            assertEquals(ResultadoAuto.Hecha(2), copia().copiarAhora())

            assertEquals(
                listOf(
                    "fotos.zip",
                    "sous-chef-historial-2026-10-01.json",
                    "souschef-historial-20261012-0900.json",
                    "souschef-historial-20261013-0900.json",
                    "souschef-historial-20261014-0900.json",
                    "souschef-historial-20261015-0900.json",
                    NOMBRE_NUEVO,
                ),
                carpeta.nombres,
            )
        }

    @Test
    fun dosCierresEnElMismoMinutoNoSePisanYLaRotacionCuentaLosRenombrados() =
        runTest {
            conCarpeta()
            val c = copia()
            repeat(6) { c.copiarAhora() }
            // Seis copias en el mismo minuto: el proveedor las llama « (1)»… « (5)» y se quedan 5.
            assertEquals(6, carpeta.creaciones)
            assertEquals(5, carpeta.archivos.size)
            assertTrue(carpeta.nombres.all { NombresCopiaAuto.esAutomatica(it) })
            assertTrue(carpeta.nombres.contains("souschef-historial-20261025-1530 (5).json"))
            assertFalse(carpeta.nombres.contains(NOMBRE_NUEVO))
        }

    @Test
    fun siLaCarpetaNoValeSeGuardaElFalloYNoSeRota() =
        runTest {
            conCarpeta()
            (10..15).forEach { carpeta.poner("souschef-historial-202610$it-0900.json") }
            carpeta.vale = false

            assertEquals(ResultadoAuto.Fallida(FalloCopiaAuto.CARPETA_NO_VALE), copia().copiarAhora())

            assertEquals(FalloCopiaAuto.CARPETA_NO_VALE, estado.datos.value.fallo)
            assertNull(estado.datos.value.ultimaAutoMs)
            assertEquals(6, carpeta.archivos.size)
            assertTrue(carpeta.borrados.isEmpty())
        }

    @Test
    fun siRetiranElPermisoALaMitadEsCarpetaQueNoVale() =
        runTest {
            conCarpeta()
            carpeta.motivoRechazo = ResultadoEscritura.SinPermiso
            assertEquals(ResultadoAuto.Fallida(FalloCopiaAuto.CARPETA_NO_VALE), copia().copiarAhora())
            assertEquals(FalloCopiaAuto.CARPETA_NO_VALE, estado.datos.value.fallo)
        }

    @Test
    fun siLaEscrituraFallaOEsGrandeNoSeRotaYQuedanLasAnteriores() =
        runTest {
            conCarpeta()
            (10..15).forEach { carpeta.poner("souschef-historial-202610$it-0900.json") }
            listOf(ResultadoEscritura.Fallo, ResultadoEscritura.Grande, ResultadoEscritura.Invalida).forEach { motivo ->
                carpeta.motivoRechazo = motivo
                assertEquals(ResultadoAuto.Fallida(FalloCopiaAuto.NO_SE_PUDO), copia().copiarAhora())
                assertEquals(FalloCopiaAuto.NO_SE_PUDO, estado.datos.value.fallo)
                assertEquals(6, carpeta.archivos.size)
            }
            assertTrue(carpeta.borrados.isEmpty())
        }

    @Test
    fun siLaRelecturaNoSePuedeSeBorraLaNuevaYNoSeRota() =
        runTest {
            conCarpeta()
            (10..15).forEach { carpeta.poner("souschef-historial-202610$it-0900.json") }
            carpeta.releeNulo = true

            assertEquals(ResultadoAuto.Fallida(FalloCopiaAuto.NO_SE_PUDO), copia().copiarAhora())

            assertEquals(6, carpeta.archivos.size)
            assertFalse(carpeta.nombres.contains(NOMBRE_NUEVO))
            assertEquals(listOf("doc-$NOMBRE_NUEVO"), carpeta.borrados)
            assertNull(estado.datos.value.ultimaAutoMs)
        }

    @Test
    fun siLaRelecturaNoEsUnaCopiaValidaSeBorraLaNuevaYNoSeRota() =
        runTest {
            conCarpeta()
            carpeta.poner("souschef-historial-20261010-0900.json")
            carpeta.releeCorrupto = true

            assertEquals(ResultadoAuto.Fallida(FalloCopiaAuto.NO_SE_PUDO), copia().copiarAhora())

            assertEquals(listOf("souschef-historial-20261010-0900.json"), carpeta.nombres)
            assertEquals(FalloCopiaAuto.NO_SE_PUDO, estado.datos.value.fallo)
        }

    @Test
    fun siNoSePuedeListarLaCopiaNuevaSigueSiendoBuena() =
        runTest {
            conCarpeta()
            (10..15).forEach { carpeta.poner("souschef-historial-202610$it-0900.json") }
            carpeta.noLista = true

            assertEquals(ResultadoAuto.Hecha(0), copia().copiarAhora())

            assertEquals(7, carpeta.archivos.size)
            assertEquals(T_CIERRE, estado.datos.value.ultimaAutoMs)
        }

    @Test
    fun siUnaCopiaViejaNoSeBorraSeCuentanSoloLasBorradas() =
        runTest {
            conCarpeta()
            (10..15).forEach { carpeta.poner("souschef-historial-202610$it-0900.json") }
            carpeta.noBorra = setOf("doc-souschef-historial-20261010-0900.json")

            assertEquals(ResultadoAuto.Hecha(1), copia().copiarAhora())

            assertEquals(T_CIERRE, estado.datos.value.ultimaAutoMs)
            assertNull(estado.datos.value.fallo)
        }

    @Test
    fun unaCopiaBuenaLimpiaElFalloAnterior() =
        runTest {
            estado.datos.value = EstadoCopiaAuto("content://carpeta", fallo = FalloCopiaAuto.NO_SE_PUDO)
            assertEquals(ResultadoAuto.Hecha(0), copia().copiarAhora())
            assertNull(estado.datos.value.fallo)
        }

    @Test
    fun unaCopiaQueNoSePuedeVolverALeerNoCreaNingunArchivo() =
        runTest {
            conCarpeta()
            // Más entradas de las que admite una copia (5000): el lector de «Cargar copia» la rechazaría, así que
            // EscrituraSegura ni abre el destino. Si además pasara de 2 MB, sería «Grande»: mismo efecto.
            val demasiadas =
                (1..CopiaHistorial.MAX_ENTRADAS + 1).map { EntradaHistorial("r-$it", "curry", "Curry", "2026-10-01") }

            val resultado = copia(repo = FakeHistorialRepo(demasiadas)).copiarAhora()

            assertEquals(ResultadoAuto.Fallida(FalloCopiaAuto.NO_SE_PUDO), resultado)
            assertEquals(0, carpeta.creaciones)
            assertTrue(carpeta.archivos.isEmpty())
        }

    // --- Elegir y desactivar ---------------------------------------------------------------------------------------

    @Test
    fun elegirCarpetaSueltaLaAnteriorYPideUnaPrimeraCopia() =
        runTest {
            conCarpeta()
            estado.datos.value = estado.datos.value.copy(fallo = FalloCopiaAuto.CARPETA_NO_VALE)
            val c = copia(scope = ambito())

            assertTrue(c.elegirCarpeta("content://otra"))
            advanceUntilIdle()

            assertEquals("content://otra", estado.datos.value.carpeta)
            assertEquals(listOf("content://carpeta"), carpeta.soltadas)
            // La primera copia comprueba enseguida que la nueva carpeta funciona.
            assertEquals(listOf(NOMBRE_NUEVO), carpeta.nombres)
            assertNull(estado.datos.value.fallo)
        }

    @Test
    fun elegirLaMismaCarpetaNoSueltaSuPermiso() =
        runTest {
            conCarpeta()
            assertTrue(copia(scope = ambito()).elegirCarpeta("content://carpeta"))
            assertTrue(carpeta.soltadas.isEmpty())
        }

    @Test
    fun siElSistemaNoDaElPermisoNoCambiaNada() =
        runTest {
            conCarpeta()
            carpeta.adoptable = false
            assertFalse(copia(scope = ambito()).elegirCarpeta("content://otra"))
            assertEquals("content://carpeta", estado.datos.value.carpeta)
            assertTrue(carpeta.soltadas.isEmpty())
        }

    @Test
    fun desactivarSueltaElPermisoYOlvidaCarpetaYFallo() =
        runTest {
            estado.datos.value =
                EstadoCopiaAuto("content://carpeta", ultimaAutoMs = 5, fallo = FalloCopiaAuto.CARPETA_NO_VALE)
            copia().desactivar()
            assertEquals(listOf("content://carpeta"), carpeta.soltadas)
            assertEquals(EstadoCopiaAuto(ultimaAutoMs = 5), estado.datos.value)
        }

    // --- Dónde corre ------------------------------------------------------------------------------------------------

    @Test
    fun variasPeticionesSeguidasSeJuntanEnUnaSolaCopia() =
        runTest {
            conCarpeta()
            val c = copia(scope = ambito())

            val primera = c.solicitar()
            assertNotNull(primera)
            assertNull(c.solicitar())
            assertNull(c.solicitar())
            advanceUntilIdle()
            assertEquals(1, carpeta.creaciones)

            // Pasada la copia, una petición nueva vuelve a copiar.
            assertNotNull(c.solicitar())
            advanceUntilIdle()
            assertEquals(2, carpeta.creaciones)
        }

    @Test
    fun unaExcepcionInesperadaNoTumbaLaAppSeAvisaSoloConSuClaseYSeSigueCopiando() =
        runTest {
            conCarpeta()
            var romper = true
            val intermitente =
                object : HistorialRepo by historial {
                    override fun observarTodo(): Flow<List<EntradaHistorial>> =
                        if (romper) flow { error("contenido-privado content://carpeta") } else historial.observarTodo()
                }
            val c = copia(scope = ambito(), repo = intermitente)

            c.solicitar()
            advanceUntilIdle()
            assertEquals(listOf("IllegalStateException"), errores)
            assertEquals(0, carpeta.creaciones)

            // La petición pendiente no se queda atascada: la siguiente copia de la misma instancia sí sale.
            romper = false
            assertNotNull(c.solicitar())
            advanceUntilIdle()
            assertEquals(1, carpeta.creaciones)
        }

    // --- Copia manual en el mismo estado ---------------------------------------------------------------------------

    @Test
    fun laCopiaManualGuardadaCuentaParaUltimaCopia() =
        runTest {
            assertEquals(ResultadoCopia.Guardada(2), gestor().exportar("content://manual"))
            assertEquals(T_CIERRE, estado.datos.value.ultimaManualMs)
            assertEquals(T_CIERRE, estado.datos.value.ultimaCopiaMs)
        }

    @Test
    fun laCopiaManualQueFallaNoSeRegistra() =
        runTest {
            val g = GestorCopia(historial, recetas, FakePreferenciasRepo(), ArchivoFalso(fallar = true), reloj, estado)
            // ArchivoFalso devuelve false al escribir: ResultadoEscritura.Fallo.
            assertEquals(ResultadoCopia.Fallo, g.exportar("content://manual"))
            assertNull(estado.datos.value.ultimaManualMs)
        }

    @Test
    fun prepararDaLosMismosBytesValidosQueLaCopiaManual() =
        runTest {
            val preparada = gestor().preparar()
            assertEquals(2, preparada.cocinadas)
            assertTrue(preparada.valida(preparada.bytes))
            assertFalse(preparada.valida("{}".toByteArray()))
        }
}

class HistorialConCopiaAutomaticaTest {
    private val curry = EntradaHistorial("curry-2026-10-01", "curry", "Curry", "2026-10-01")
    private var avisos = 0
    private val base = FakeHistorialRepo()
    private val repo = HistorialConCopiaAutomatica(base) { avisos++ }

    @Test
    fun agregarGuardaYDespuesPideLaCopia() =
        runTest {
            repo.agregar(curry)
            assertEquals(listOf(curry), base.datos.value)
            assertEquals(1, avisos)
        }

    @Test
    fun siAgregarFallaNoSePideLaCopia() =
        runTest {
            val roto =
                object : HistorialRepo by base {
                    override suspend fun agregar(entrada: EntradaHistorial) {
                        error("sin espacio")
                    }
                }
            val envuelto = HistorialConCopiaAutomatica(roto) { avisos++ }
            runCatching { envuelto.agregar(curry) }
            assertEquals(0, avisos)
        }

    @Test
    fun lasDemasOperacionesNoDisparanLaCopia() =
        runTest {
            base.agregar(curry)
            repo.editarNota(curry.id, "con más sal")
            repo.valorar(curry.id, null)
            repo.importar(emptyList())
            repo.borrar(curry.id)
            assertEquals(0, avisos)
        }
}
