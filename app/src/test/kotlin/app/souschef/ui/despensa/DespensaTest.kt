package app.souschef.ui.despensa

import app.souschef.domain.CompraRepo
import app.souschef.domain.DespensaRepo
import app.souschef.domain.EstadoDespensa
import app.souschef.domain.ItemCompra
import app.souschef.domain.ItemDespensa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeDespensaRepo(
    inicial: List<ItemDespensa> = emptyList(),
) : DespensaRepo {
    val datos = MutableStateFlow(inicial)
    private var siguiente = 100L

    override fun observar(): Flow<List<ItemDespensa>> = datos

    override suspend fun agregar(
        nombre: String,
        categoria: String,
        estado: EstadoDespensa,
    ): Long {
        val id = siguiente++
        datos.value = datos.value + ItemDespensa(id, nombre, categoria, estado)
        return id
    }

    override suspend fun cambiarEstado(
        id: Long,
        estado: EstadoDespensa,
    ) {
        datos.value = datos.value.map { if (it.id == id) it.copy(estado = estado) else it }
    }

    override suspend fun actualizar(item: ItemDespensa) {
        datos.value = datos.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun borrar(id: Long) {
        datos.value = datos.value.filterNot { it.id == id }
    }
}

class FakeCompraListaRepo(
    inicial: List<ItemCompra> = emptyList(),
) : CompraRepo {
    val datos = MutableStateFlow(inicial)
    private var siguiente = 1L

    override fun observar(): Flow<List<ItemCompra>> = datos

    override suspend fun agregar(
        texto: String,
        zona: String,
        origen: String?,
    ): Long {
        val id = siguiente++
        datos.value = datos.value + ItemCompra(id, texto, zona, origen, orden = datos.value.size)
        return id
    }

    override suspend fun agregarVarios(
        textos: List<String>,
        origen: String?,
    ) {
        textos.forEach { agregar(it, "", origen) }
    }

    override suspend fun marcar(
        id: Long,
        marcado: Boolean,
    ) {
        datos.value = datos.value.map { if (it.id == id) it.copy(marcado = marcado) else it }
    }

    override suspend fun borrar(id: Long) {
        datos.value = datos.value.filterNot { it.id == id }
    }

    override suspend fun borrarMarcados() {
        datos.value = datos.value.filterNot { it.marcado }
    }

    override suspend fun reordenar(idsEnOrden: List<Long>) = Unit
}

private fun d(
    id: Long,
    nombre: String,
    categoria: String,
    estado: EstadoDespensa = EstadoDespensa.HAY,
) = ItemDespensa(id, nombre, categoria, estado)

class DespensaLogicaTest {
    @Test
    fun elCicloNoTocaLosVetados() {
        assertEquals(EstadoDespensa.POCO, siguienteEstado(EstadoDespensa.HAY))
        assertEquals(EstadoDespensa.FALTA, siguienteEstado(EstadoDespensa.POCO))
        assertEquals(EstadoDespensa.HAY, siguienteEstado(EstadoDespensa.FALTA))
        assertEquals(EstadoDespensa.VETADO, siguienteEstado(EstadoDespensa.VETADO))
    }

    @Test
    fun agrupaPorCategoriaConVetadosAlFinal() {
        val items =
            listOf(
                d(1, "Sal", "Especias"),
                d(2, "Coliflor", "Verduras", EstadoDespensa.VETADO),
                d(3, "Arroz", "Cereales"),
                d(4, "Pimienta", "Especias"),
            )
        val grupos = agruparDespensa(items, "", null)
        assertEquals(listOf("Cereales", "Especias", ""), grupos.map { it.categoria })
        assertTrue(grupos.last().esVetados)
        assertEquals(listOf("Pimienta", "Sal"), grupos[1].items.map { it.nombre })
    }

    @Test
    fun buscaYFiltraPorEstado() {
        val items = listOf(d(1, "Sal", "Especias"), d(2, "Salmón", "Pescado", EstadoDespensa.FALTA))
        assertEquals(2, agruparDespensa(items, "sal", null).sumOf { it.items.size })
        assertEquals(
            listOf("Salmón"),
            agruparDespensa(items, "sal", EstadoDespensa.FALTA).flatMap { g ->
                g.items.map { it.nombre }
            },
        )
    }

    @Test
    fun textoDeCopiarAgrupaPorZonaOOrigenYOmiteMarcados() {
        val items =
            listOf(
                ItemCompra(1, "Leche", zona = "Lácteos", orden = 0),
                ItemCompra(2, "Sal", origen = "despensa", orden = 1),
                ItemCompra(3, "Pan", orden = 2),
                ItemCompra(4, "Queso", zona = "Lácteos", marcado = true, orden = 3),
                ItemCompra(5, "Yogur", zona = "Lácteos", orden = 4),
            )
        val esperado = "Lácteos:\n- Leche\n- Yogur\n\ndespensa:\n- Sal\n\n- Pan"
        assertEquals(esperado, textoCompra(agruparCompra(items)))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DespensaViewModelTest {
    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun tocarCiclaEstadosYIgnoraVetados() =
        runTest(UnconfinedTestDispatcher()) {
            val repo =
                FakeDespensaRepo(listOf(d(1, "Sal", "Especias"), d(2, "Coliflor", "Verduras", EstadoDespensa.VETADO)))
            val vm = DespensaViewModel(repo, FakeCompraListaRepo())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            val sal = repo.datos.value[0]
            vm.ciclarEstado(sal)
            assertEquals(EstadoDespensa.POCO, repo.datos.value[0].estado)
            vm.ciclarEstado(repo.datos.value[0])
            assertEquals(EstadoDespensa.FALTA, repo.datos.value[0].estado)
            vm.ciclarEstado(repo.datos.value[0])
            assertEquals(EstadoDespensa.HAY, repo.datos.value[0].estado)

            vm.ciclarEstado(repo.datos.value[1])
            assertEquals(EstadoDespensa.VETADO, repo.datos.value[1].estado)
            assertTrue(
                vm.estado.value.grupos
                    .last()
                    .esVetados,
            )
        }

    @Test
    fun pasarACompraNoDuplicaTextos() =
        runTest(UnconfinedTestDispatcher()) {
            val repo =
                FakeDespensaRepo(
                    listOf(
                        d(1, "Sal", "Especias", EstadoDespensa.FALTA),
                        d(2, "Aceite", "Despensa", EstadoDespensa.POCO),
                        d(3, "Arroz", "Cereales"),
                        d(4, "Coliflor", "Verduras", EstadoDespensa.VETADO),
                    ),
                )
            val compra = FakeCompraListaRepo(listOf(ItemCompra(9, " sal ")))
            val vm = DespensaViewModel(repo, compra)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            vm.pasarACompra()
            assertEquals(listOf(" sal ", "Aceite"), compra.datos.value.map { it.texto })
            assertEquals(1, vm.estado.value.avisoPasados)

            vm.pasarACompra()
            assertEquals(2, compra.datos.value.size)
            assertEquals(0, vm.estado.value.avisoPasados)
        }

    @Test
    fun estadoExponeTextoParaCopiar() =
        runTest(UnconfinedTestDispatcher()) {
            val compra = FakeCompraListaRepo()
            val vm = DespensaViewModel(FakeDespensaRepo(), compra)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            vm.agregarCompra("  Pan ")
            vm.agregarCompra("Leche")
            vm.marcar(compra.datos.value[0].id, true)
            assertEquals("- Leche", vm.estado.value.textoCompra)
            vm.borrarMarcados()
            assertEquals(listOf("Leche"), compra.datos.value.map { it.texto })
        }
}
