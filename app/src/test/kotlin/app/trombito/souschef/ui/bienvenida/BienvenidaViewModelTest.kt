package app.trombito.souschef.ui.bienvenida

import app.trombito.souschef.domain.VersionFija
import app.trombito.souschef.domain.voz.Escucha
import app.trombito.souschef.domain.voz.Habla
import app.trombito.souschef.receta.voz.OrdenVoz
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.crono.FakeAjustesRepo
import app.trombito.souschef.ui.crono.recetaCrono
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Escucha que deja decidir al test qué se oye y cuándo. */
class EscuchaGuion(
    override val disponible: Boolean = true,
) : Escucha {
    var empezadas = 0
    var paradas = 0
    private var alOir: (List<String>) -> Unit = {}

    override fun empezar(
        alOir: (List<String>) -> Unit,
        alDetenerse: (Boolean) -> Unit,
    ) {
        empezadas++
        this.alOir = alOir
    }

    fun oir(vararg alternativas: String) = alOir(alternativas.toList())

    override fun pausar() = Unit

    override fun reanudar() = Unit

    override fun parar() {
        paradas++
    }
}

/** Habla que acaba al instante y guarda lo que dijo. */
class HablaGuion : Habla {
    val dichas = mutableListOf<String>()
    var liberada = 0

    override fun decir(
        texto: String,
        alAcabar: () -> Unit,
    ) {
        dichas += texto
        alAcabar()
    }

    override fun callar() = Unit

    override fun liberar() {
        liberada++
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BienvenidaViewModelTest {
    private val escucha = EscuchaGuion()
    private val habla = HablaGuion()
    private val ajustes = FakeAjustesRepo()

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    private fun vm(
        escucha: EscuchaGuion = this.escucha,
        recetas: FakeRecetasRepo = FakeRecetasRepo(listOf(recetaCrono(SLUG_COCINA), recetaCrono(SLUG_RACIONES))),
    ) = BienvenidaViewModel(
        recetas,
        escucha,
        habla,
        ajustes,
        VersionFija("1.6.0"),
        CoroutineScope(Dispatchers.Unconfined),
    )

    @Test
    fun `carga las recetas de ejemplo del repositorio`() {
        val demo = vm().demo.value

        assertEquals(SLUG_COCINA, demo.cocina?.slug)
        assertEquals(SLUG_RACIONES, demo.raciones?.slug)
        assertEquals(listOf("f0", "m"), demo.carrilesVoz.map { it.id })
    }

    @Test
    fun `sin las recetas los ejemplos quedan vacios`() {
        val demo = vm(recetas = FakeRecetasRepo()).demo.value

        assertEquals(emptyList<Any>(), demo.tarjetas)
    }

    @Test
    fun `probar dice la frase, escucha una vez y entiende la orden`() {
        val vm = vm()

        vm.probarVoz("Hola, soy el Chef. Dime: qué tengo que hacer")

        assertEquals(listOf("Hola, soy el Chef. Dime: qué tengo que hacer"), habla.dichas)
        assertEquals(PruebaVoz.Escuchando, vm.voz.value)
        assertEquals(1, escucha.empezadas)

        escucha.oir("Chef, qué toca")

        val oido = vm.voz.value as PruebaVoz.Oido
        assertEquals("Chef, qué toca", oido.frase)
        assertEquals(OrdenVoz.QueToca(null), oido.orden)
        // Escucha una sola vez: tras oír se suelta el micrófono.
        assertTrue(escucha.paradas >= 1)
    }

    @Test
    fun `lo que no es una orden se dice que no se ha entendido`() {
        val vm = vm()
        vm.probarVoz("hola")

        escucha.oir("buenos dias a todos")

        assertEquals(PruebaVoz.Oido("buenos dias a todos", null), vm.voz.value)
    }

    @Test
    fun `sin reconocedor lo dice y no escucha`() {
        val sin = EscuchaGuion(disponible = false)
        val vm = vm(escucha = sin)

        vm.probarVoz("hola")

        assertEquals(PruebaVoz.SinReconocedor, vm.voz.value)
        assertEquals(0, sin.empezadas)
        assertEquals(emptyList<String>(), habla.dichas)
    }

    @Test
    fun `sin permiso queda anotado`() {
        val vm = vm()

        vm.sinPermiso()

        assertEquals(PruebaVoz.SinPermiso, vm.voz.value)
    }

    @Test
    fun `si no se oye nada en diez segundos se deja de escuchar`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = vm()
            vm.probarVoz("hola")
            assertEquals(PruebaVoz.Escuchando, vm.voz.value)

            advanceTimeBy(10_001)

            assertEquals(PruebaVoz.NadaOido, vm.voz.value)
        }

    @Test
    fun `parar deja la prueba como al principio`() {
        val vm = vm()
        vm.probarVoz("hola")

        vm.pararVoz()

        assertEquals(PruebaVoz.Inactiva, vm.voz.value)
    }

    @Test
    fun `al acabar la intro la version actual queda como vista`() {
        vm().marcarIntroVista()

        assertTrue(ajustes.actual.value.introVista)
        assertEquals("1.6", ajustes.actual.value.novedadesVistas)
    }
}
