package app.trombito.souschef.domain.resena

import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.ui.FakeHistorialRepo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResenaTest {
    private fun cocinada(
        n: Int,
        minutos: Int? = 40,
    ) = EntradaHistorial("id$n", "receta$n", "Receta $n", "2026-10-0$n", minutosReales = minutos)

    private class FakeResenaRepo(
        var pedida: Boolean = false,
    ) : ResenaRepo {
        override suspend fun yaPedida() = pedida

        override suspend fun marcarPedida() {
            pedida = true
        }
    }

    private class FakeLanzador(
        private val resultado: ResultadoResena = ResultadoResena.PEDIDA,
    ) : LanzadorResena {
        var llamadas = 0

        override suspend fun lanzar(): ResultadoResena {
            llamadas++
            return resultado
        }
    }

    private fun gestor(
        entradas: List<EntradaHistorial>,
        repo: ResenaRepo = FakeResenaRepo(),
        lanzador: LanzadorResena = FakeLanzador(),
        viva: Boolean = false,
    ) = GestorResena(FakeHistorialRepo(entradas), repo, { viva }, lanzador)

    // --- regla pura ---

    @Test
    fun `solo cuentan las cocinadas con duracion real`() {
        val entradas = listOf(cocinada(1), cocinada(2, minutos = null), cocinada(3, minutos = 0), cocinada(4))
        assertEquals(2, ReglaResena.cerradas(entradas))
    }

    @Test
    fun `con dos cocinadas cerradas no toca`() {
        assertFalse(ReglaResena.debePedir(cerradas = 2, yaPedida = false, hayViva = false, falloEnSesion = false))
    }

    @Test
    fun `con tres cocinadas cerradas toca`() {
        assertTrue(ReglaResena.debePedir(cerradas = 3, yaPedida = false, hayViva = false, falloEnSesion = false))
    }

    @Test
    fun `no toca si ya se pidio, hay cocinada viva o fallo en la sesion`() {
        assertFalse(ReglaResena.debePedir(cerradas = 9, yaPedida = true, hayViva = false, falloEnSesion = false))
        assertFalse(ReglaResena.debePedir(cerradas = 9, yaPedida = false, hayViva = true, falloEnSesion = false))
        assertFalse(ReglaResena.debePedir(cerradas = 9, yaPedida = false, hayViva = false, falloEnSesion = true))
    }

    // --- gestor ---

    @Test
    fun `tras la tercera cocinada la pide y la marca`() =
        runTest {
            val repo = FakeResenaRepo()
            val lanzador = FakeLanzador()
            gestor(listOf(cocinada(1), cocinada(2), cocinada(3)), repo, lanzador).alVolverDelCrono()
            assertEquals(1, lanzador.llamadas)
            assertTrue(repo.pedida)
        }

    @Test
    fun `solo se pide una vez`() =
        runTest {
            val lanzador = FakeLanzador()
            val g = gestor(listOf(cocinada(1), cocinada(2), cocinada(3), cocinada(4)), lanzador = lanzador)
            g.alVolverDelCrono()
            g.alVolverDelCrono()
            assertEquals(1, lanzador.llamadas)
        }

    @Test
    fun `si ya estaba marcada de antes no la pide`() =
        runTest {
            val lanzador = FakeLanzador()
            gestor(listOf(cocinada(1), cocinada(2), cocinada(3)), FakeResenaRepo(pedida = true), lanzador)
                .alVolverDelCrono()
            assertEquals(0, lanzador.llamadas)
        }

    @Test
    fun `con dos cocinadas cerradas y una anotada a mano no la pide`() =
        runTest {
            val lanzador = FakeLanzador()
            gestor(listOf(cocinada(1), cocinada(2), cocinada(3, minutos = null)), lanzador = lanzador)
                .alVolverDelCrono()
            assertEquals(0, lanzador.llamadas)
        }

    @Test
    fun `con una cocinada viva no la pide ni la marca`() =
        runTest {
            val repo = FakeResenaRepo()
            val lanzador = FakeLanzador()
            gestor(listOf(cocinada(1), cocinada(2), cocinada(3)), repo, lanzador, viva = true).alVolverDelCrono()
            assertEquals(0, lanzador.llamadas)
            assertFalse(repo.pedida)
        }

    @Test
    fun `si la API falla no se marca y no se reintenta en la sesion`() =
        runTest {
            val repo = FakeResenaRepo()
            val lanzador = FakeLanzador(ResultadoResena.FALLO)
            val g = gestor(listOf(cocinada(1), cocinada(2), cocinada(3)), repo, lanzador)
            g.alVolverDelCrono()
            g.alVolverDelCrono()
            assertEquals(1, lanzador.llamadas)
            assertFalse(repo.pedida)
        }

    @Test
    fun `sin Play no se marca como pedida`() =
        runTest {
            val repo = FakeResenaRepo()
            val lanzador = FakeLanzador(ResultadoResena.NO_DISPONIBLE)
            val g = gestor(listOf(cocinada(1), cocinada(2), cocinada(3)), repo, lanzador)
            g.alVolverDelCrono()
            g.alVolverDelCrono()
            assertEquals(1, lanzador.llamadas)
            assertFalse(repo.pedida)
        }
}
