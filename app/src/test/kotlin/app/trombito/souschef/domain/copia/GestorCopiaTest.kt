package app.trombito.souschef.domain.copia

import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.PasoReal
import app.trombito.souschef.domain.Preferencia
import app.trombito.souschef.domain.PreferenciasRepo
import app.trombito.souschef.domain.Valoracion
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakePreferenciasRepo
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.crono.RelojFalso
import app.trombito.souschef.ui.receta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class ArchivosEnMemoria : ArchivoCopia {
    val archivos = mutableMapOf<String, String>()
    var fallar = false

    /** La validación con la que el gestor pidió la última escritura segura. */
    var validacion: ((ByteArray) -> Boolean)? = null

    override suspend fun escribir(
        destino: String,
        texto: String,
    ): Boolean {
        if (!fallar) archivos[destino] = texto
        return !fallar
    }

    override suspend fun leer(origen: String): String? = archivos[origen]

    override suspend fun escribirSeguro(
        destino: String,
        bytes: ByteArray,
        valida: (ByteArray) -> Boolean,
    ): ResultadoEscritura {
        validacion = valida
        return super.escribirSeguro(destino, bytes, valida)
    }
}

class GestorCopiaTest {
    private val curry = EntradaHistorial("curry-2026-10-01", "curry", "Curry", "2026-10-01")
    private val tacos = EntradaHistorial("tacos-2026-10-05", "tacos", "Tacos", "2026-10-05")
    private val archivos = ArchivosEnMemoria()
    private val recetas = FakeRecetasRepo(listOf(receta("curry"), receta("tacos")))

    private fun gestor(
        historial: FakeHistorialRepo,
        prefs: FakePreferenciasRepo = FakePreferenciasRepo(),
    ) = GestorCopia(historial, recetas, prefs, archivos, RelojFalso())

    /** Escribe en `content://malo` una copia con [entradas] tal cual (el exportador no valida) y la importa. */
    private suspend fun importarEntradas(
        entradas: List<EntradaHistorial>,
        destino: FakeHistorialRepo = FakeHistorialRepo(),
        prefs: List<Preferencia> = emptyList(),
    ): ResultadoCopia {
        archivos.archivos["content://malo"] = CopiaHistorial.exportar(entradas, prefs, "2026-10-24")
        return gestor(destino).importar("content://malo")
    }

    @Test
    fun guardarYCargarEnUnaAppVaciaRecuperaElHistorial() =
        runTest {
            val origen = FakeHistorialRepo(listOf(curry, tacos))
            assertEquals(ResultadoCopia.Guardada(2), gestor(origen).exportar("content://copia"))

            val vacio = FakeHistorialRepo()
            val prefs = FakePreferenciasRepo()
            assertEquals(ResultadoCopia.Cargada(2), gestor(vacio, prefs).importar("content://copia"))
            assertEquals(setOf(curry, tacos), vacio.datos.value.toSet())
        }

    @Test
    fun cargarDosVecesNoDuplica() =
        runTest {
            gestor(FakeHistorialRepo(listOf(curry))).exportar("content://copia")
            val destino = FakeHistorialRepo(listOf(curry))
            assertEquals(ResultadoCopia.Cargada(0), gestor(destino).importar("content://copia"))
            assertEquals(listOf(curry), destino.datos.value)
        }

    @Test
    fun cargarAplicaFavoritosSinPisarRaciones() =
        runTest {
            val prefsOrigen = FakePreferenciasRepo()
            prefsOrigen.datos.value = mapOf("curry" to Preferencia("curry", favorito = true, raciones = 6))
            gestor(FakeHistorialRepo(), prefsOrigen).exportar("content://copia")

            val prefs = FakePreferenciasRepo()
            prefs.datos.value = mapOf("curry" to Preferencia("curry", raciones = 3))
            gestor(FakeHistorialRepo(), prefs).importar("content://copia")

            assertEquals(Preferencia("curry", favorito = true, raciones = 3), prefs.datos.value["curry"])
            assertEquals(emptyList<Pair<String, Int>>(), prefs.racionesGuardadas)
        }

    @Test
    fun cargarNoLeeLasPreferenciasFueraDeLaTransaccionDelRepo() =
        runTest {
            val origen = FakePreferenciasRepo()
            origen.datos.value = mapOf("curry" to Preferencia("curry", favorito = true))
            gestor(FakeHistorialRepo(), origen).exportar("content://copia")
            val respaldo = FakePreferenciasRepo()
            // Si el gestor leyera las actuales él mismo (foto vieja) esto fallaría: la fusión es del repositorio.
            val soloImportar =
                object : PreferenciasRepo by respaldo {
                    override fun observarTodas(): Flow<Map<String, Preferencia>> =
                        error("el gestor no debe leer las preferencias al importar")
                }

            val g = GestorCopia(FakeHistorialRepo(), recetas, soloImportar, archivos, RelojFalso())
            assertEquals(ResultadoCopia.Cargada(0), g.importar("content://copia"))
            assertEquals(Preferencia("curry", favorito = true), respaldo.datos.value["curry"])
        }

    @Test
    fun cargarRellenaLaValoracionQueFaltaYNoPisaLaLocal() =
        runTest {
            val sinValorar = curry.copy(valoracion = null)
            val copia =
                listOf(
                    sinValorar.copy(valoracion = Valoracion.REPETIR, previstoMin = 40),
                    tacos.copy(valoracion = Valoracion.NO_REPETIR),
                )
            archivos.archivos["content://v2"] = CopiaHistorial.exportar(copia, emptyList(), "2026-10-24")

            val destino = FakeHistorialRepo(listOf(sinValorar, tacos.copy(valoracion = Valoracion.AJUSTAR)))
            assertEquals(ResultadoCopia.Cargada(0, 0), gestor(destino).importar("content://v2"))
            assertEquals(
                mapOf("curry-2026-10-01" to Valoracion.REPETIR, "tacos-2026-10-05" to Valoracion.AJUSTAR),
                destino.datos.value.associate { it.id to it.valoracion },
            )
        }

    @Test
    fun archivoAjenoOIlegibleSeDiceSinTocarNada() =
        runTest {
            archivos.archivos["content://otro"] = "{}"
            val h = FakeHistorialRepo(listOf(curry))
            assertEquals(ResultadoCopia.NoValida, gestor(h).importar("content://otro"))
            assertEquals(ResultadoCopia.Fallo, gestor(h).importar("content://no-existe"))
            archivos.fallar = true
            assertEquals(ResultadoCopia.Fallo, gestor(h).exportar("content://copia"))
            assertEquals(listOf(curry), h.datos.value)
        }

    @Test
    fun cocinadaDeRecetaRetiradaSeConservaPeroSuPreferenciaNoSeCrea() =
        runTest {
            val destino = FakeHistorialRepo()
            val fantasma = curry.copy(id = "fantasma-2026-10-01", slug = "fantasma", titulo = "Fantasma")
            val prefs =
                listOf(Preferencia("fantasma", favorito = true, raciones = 4), Preferencia("curry", favorito = true))
            archivos.archivos["content://malo"] = CopiaHistorial.exportar(listOf(curry, fantasma), prefs, "2026-10-24")
            val prefsDestino = FakePreferenciasRepo()
            assertEquals(ResultadoCopia.Cargada(2, 1), gestor(destino, prefsDestino).importar("content://malo"))
            assertEquals(setOf(curry, fantasma), destino.datos.value.toSet())
            assertEquals(setOf("curry"), prefsDestino.datos.value.keys)
        }

    @Test
    fun slugConFormatoRaroSeDescarta() =
        runTest {
            val slugs = listOf("../etc", "Curry", "", "a".repeat(65))
            val malos = slugs.mapIndexed { i, s -> curry.copy(id = "x$i", slug = s) }
            assertEquals(ResultadoCopia.Cargada(0, 4), importarEntradas(malos))
        }

    @Test
    fun fechaMalaSeDescarta() =
        runTest {
            val fechas = listOf("2026-13-45", "ayer", "2026-10-1", "")
            val malas = fechas.mapIndexed { i, f -> curry.copy(id = "x$i", fecha = f) }
            val destino = FakeHistorialRepo()
            assertEquals(ResultadoCopia.Cargada(1, 4), importarEntradas(malas + tacos, destino))
            assertEquals(listOf(tacos), destino.datos.value)
        }

    @Test
    fun racionesFueraDeRangoSeDescartan() =
        runTest {
            val entradas =
                listOf(0, 99, -1, 1, 20, null).mapIndexed { i, r -> curry.copy(id = "x$i", raciones = r) }
            val destino = FakeHistorialRepo()
            assertEquals(ResultadoCopia.Cargada(3, 3), importarEntradas(entradas, destino))
            assertEquals(
                setOf(1, 20, null),
                destino.datos.value
                    .map { it.raciones }
                    .toSet(),
            )
        }

    @Test
    fun segundosNegativosOMasDe200PasosSeDescartan() =
        runTest {
            val negativo = curry.copy(id = "neg", pasosReales = listOf(PasoReal("p1", "Picar", -5)))
            val muchos = curry.copy(id = "muchos", pasosReales = List(201) { PasoReal("p$it", "Paso", 10) })
            val bien = curry.copy(id = "bien", pasosReales = List(200) { PasoReal("p$it", "Paso", 0) })
            val destino = FakeHistorialRepo()
            assertEquals(ResultadoCopia.Cargada(1, 2), importarEntradas(listOf(negativo, muchos, bien), destino))
            assertEquals(listOf("bien"), destino.datos.value.map { it.id })
        }

    @Test
    fun notaYTituloSeRecortan() =
        runTest {
            val largo = curry.copy(titulo = "T".repeat(500), nota = "n".repeat(500))
            val destino = FakeHistorialRepo()
            assertEquals(ResultadoCopia.Cargada(1, 0), importarEntradas(listOf(largo), destino))
            assertEquals(
                120,
                destino.datos.value
                    .single()
                    .titulo.length,
            )
            assertEquals(
                160,
                destino.datos.value
                    .single()
                    .nota
                    ?.length,
            )
        }

    @Test
    fun masDe5000EntradasRechazaElArchivoSinTocarNada() =
        runTest {
            val demasiadas = List(5001) { curry.copy(id = "curry-$it") }
            val destino = FakeHistorialRepo(listOf(tacos))
            assertEquals(ResultadoCopia.NoValida, importarEntradas(demasiadas, destino))
            assertEquals(listOf(tacos), destino.datos.value)
            assertEquals(ResultadoCopia.Cargada(5000, 0), importarEntradas(demasiadas.take(5000)))
        }

    @Test
    fun reimportarLaMismaCopiaNoDuplicaNiPisa() =
        runTest {
            val original = curry.copy(nota = "la mía")
            val destino = FakeHistorialRepo(listOf(original))
            val copia = listOf(curry.copy(nota = "otra", titulo = "Cambiado"), tacos)
            archivos.archivos["content://malo"] = CopiaHistorial.exportar(copia, emptyList(), "2026-10-24")

            assertEquals(ResultadoCopia.Cargada(1, 0), gestor(destino).importar("content://malo"))
            assertEquals(ResultadoCopia.Cargada(0, 0), gestor(destino).importar("content://malo"))
            assertEquals(setOf(original, tacos), destino.datos.value.toSet())
            assertEquals(2, destino.datos.value.size)
        }

    @Test
    fun guardarValidaConElMismoLectorQueCargarAntesDeEscribir() =
        runTest {
            gestor(FakeHistorialRepo(listOf(curry))).exportar("content://copia")
            val valida = checkNotNull(archivos.validacion)

            assertTrue(valida(checkNotNull(archivos.archivos["content://copia"]).toByteArray()))
            assertFalse(valida("{}".toByteArray()))
            assertFalse(valida("no es json".toByteArray()))
            assertFalse(valida(ByteArray(0)))
        }

    @Test
    fun contenidoQueNoPasaLaValidacionNoSeEscribeYLaCopiaBuenaQueda() =
        runTest {
            archivos.archivos["content://copia"] = "copia buena anterior"
            val r = archivos.escribirSeguro("content://copia", "basura".toByteArray()) { false }
            assertEquals(ResultadoEscritura.Invalida, r)
            assertEquals("copia buena anterior", archivos.archivos["content://copia"])

            val ok = archivos.escribirSeguro("content://copia", "nueva".toByteArray()) { true }
            assertEquals(ResultadoEscritura.Escrita, ok)
            assertEquals("nueva", archivos.archivos["content://copia"])
        }

    @Test
    fun archivoDemasiadoGrandeSeDiceSinTocarNada() =
        runTest {
            val destino = FakeHistorialRepo(listOf(curry))
            val grande =
                object : ArchivoCopia {
                    override suspend fun escribir(
                        destino: String,
                        texto: String,
                    ) = true

                    override suspend fun leer(origen: String): String? = throw ArchivoDemasiadoGrande()
                }
            val g = GestorCopia(destino, recetas, FakePreferenciasRepo(), grande, RelojFalso())
            assertEquals(ResultadoCopia.Grande, g.importar("content://enorme"))
            assertEquals(listOf(curry), destino.datos.value)
        }
}
