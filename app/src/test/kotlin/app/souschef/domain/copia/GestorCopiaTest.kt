package app.souschef.domain.copia

import app.souschef.domain.EntradaHistorial
import app.souschef.domain.Preferencia
import app.souschef.ui.FakeHistorialRepo
import app.souschef.ui.FakePreferenciasRepo
import app.souschef.ui.crono.RelojFalso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class ArchivosEnMemoria : ArchivoCopia {
    val archivos = mutableMapOf<String, String>()
    var fallar = false

    override suspend fun escribir(
        destino: String,
        texto: String,
    ): Boolean {
        if (!fallar) archivos[destino] = texto
        return !fallar
    }

    override suspend fun leer(origen: String): String? = archivos[origen]
}

class GestorCopiaTest {
    private val curry = EntradaHistorial("curry-2026-10-01", "curry", "Curry", "2026-10-01")
    private val tacos = EntradaHistorial("tacos-2026-10-05", "tacos", "Tacos", "2026-10-05")
    private val archivos = ArchivosEnMemoria()

    private fun gestor(
        historial: FakeHistorialRepo,
        prefs: FakePreferenciasRepo = FakePreferenciasRepo(),
    ) = GestorCopia(historial, prefs, archivos, RelojFalso())

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
}
