package app.trombito.souschef.ui.recetas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.CualHora
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.domain.OrdenRecetas
import app.trombito.souschef.domain.Preferencia
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Grupo
import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.Momento
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tags
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakePreferenciasRepo
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.Rutas
import app.trombito.souschef.ui.crono.FakeAjustesRepo
import app.trombito.souschef.ui.crono.RelojFalso
import app.trombito.souschef.ui.receta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HaceTest {
    private val hoy = LocalDate.of(2026, 10, 14)

    private fun hace(dias: Long) = Hace.de(hoy.minusDays(dias).toString(), hoy)

    @Test
    fun nuncaCocinadaEsSinEstrenarResaltado() {
        val h = Hace.de(null, hoy)
        assertEquals(UnidadHace.SIN_ESTRENAR, h.unidad)
        assertTrue(h.resaltado)
    }

    @Test
    fun hoyAyerYDias() {
        assertEquals(UnidadHace.HOY, hace(0).unidad)
        assertEquals(UnidadHace.AYER, hace(1).unidad)
        assertEquals(Hace(UnidadHace.DIAS, 6), hace(6))
    }

    @Test
    fun semanasMesesYAnos() {
        assertEquals(Hace(UnidadHace.SEMANAS, 1), hace(7))
        assertEquals(Hace(UnidadHace.SEMANAS, 4), hace(29))
        assertEquals(Hace(UnidadHace.MESES, 1, false), hace(30))
        assertEquals(Hace(UnidadHace.MESES, 2, true), hace(60))
        assertEquals(Hace(UnidadHace.ANOS, 1, true), hace(365))
    }

    @Test
    fun resaltaDesdeSesentaDias() {
        assertFalse(hace(59).resaltado)
        assertTrue(hace(60).resaltado)
    }

    @Test
    fun fechaFuturaCuentaComoHoy() {
        assertEquals(UnidadHace.HOY, hace(-3).unidad)
    }
}

class FamiliasTest {
    @Test
    fun cadaBaseVaASuFamiliaYLoDesconocidoAOtra() {
        assertEquals(FamiliaBase.PASTA, FamiliaBase.de("pasta"))
        assertEquals(FamiliaBase.VERDURA, FamiliaBase.de("Ensalada"))
        assertEquals(FamiliaBase.MASAS, FamiliaBase.de("masa de pizza"))
        assertEquals(FamiliaBase.MASAS, FamiliaBase.de("taco"))
        assertEquals(FamiliaBase.MASAS, FamiliaBase.de("hojaldre"))
        assertEquals(FamiliaBase.SALTEADO, FamiliaBase.de("salteado"))
        assertEquals(FamiliaBase.OTRA, FamiliaBase.de("legumbre"))
        assertEquals(FamiliaBase.OTRA, FamiliaBase.de(null))
    }

    @Test
    fun carnesPostresYCremaTienenSuFamiliaYOtraVaLaUltima() {
        assertEquals(FamiliaBase.CARNES, FamiliaBase.de("asado"))
        assertEquals(FamiliaBase.CARNES, FamiliaBase.de("Hamburguesa"))
        assertEquals(FamiliaBase.CARNES, FamiliaBase.de("fritura"))
        assertEquals(FamiliaBase.POSTRES, FamiliaBase.de("postre"))
        assertEquals(FamiliaBase.VERDURA, FamiliaBase.de("crema"))
        assertEquals(10, FamiliaBase.entries.size)
        assertEquals(FamiliaBase.OTRA, FamiliaBase.entries.last())
        assertEquals(
            listOf(FamiliaBase.CARNES, FamiliaBase.POSTRES, FamiliaBase.OTRA),
            FamiliaBase.entries.takeLast(3),
        )
    }

    @Test
    fun cadaFamiliaTieneUnTonoPropioEnClaroYOscuro() {
        val tonos = FamiliaBase.entries.map { ColoresBase.tono(it) }
        assertEquals(tonos.size, tonos.toSet().size)
        FamiliaBase.entries.forEach { assertTrue(ColoresBase.de(it, false) != ColoresBase.de(it, true)) }
    }

    @Test
    fun proteinaPorPalabrasYVegetarianaSiNoHayCarneNiPescado() {
        assertEquals(setOf(FamiliaProteina.POLLO), FamiliaProteina.de("pollo"))
        assertEquals(
            setOf(FamiliaProteina.CARNE, FamiliaProteina.CERDO, FamiliaProteina.POLLO),
            FamiliaProteina.de("carne picada mixta, bacon y pollo"),
        )
        assertEquals(setOf(FamiliaProteina.CERDO, FamiliaProteina.HUEVO), FamiliaProteina.de("huevo y jamón"))
        assertEquals(setOf(FamiliaProteina.PESCADO), FamiliaProteina.de("atún y feta"))
        assertEquals(setOf(FamiliaProteina.PESCADO), FamiliaProteina.de("Salmón"))
        assertEquals(setOf(FamiliaProteina.HUEVO, FamiliaProteina.VEGETARIANA), FamiliaProteina.de("huevos"))
        assertEquals(setOf(FamiliaProteina.VEGETARIANA), FamiliaProteina.de("ninguna"))
        assertEquals(setOf(FamiliaProteina.VEGETARIANA), FamiliaProteina.de(null))
    }

    @Test
    fun tramosDeTiempo() {
        assertEquals(TramoTiempo.RAPIDA, TramoTiempo.de(30.0))
        assertEquals(TramoTiempo.CORTA, TramoTiempo.de(30.5))
        assertEquals(TramoTiempo.CORTA, TramoTiempo.de(44.5))
        assertEquals(TramoTiempo.MEDIA, TramoTiempo.de(45.0))
        assertEquals(TramoTiempo.MEDIA, TramoTiempo.de(90.0))
        assertEquals(TramoTiempo.LARGA, TramoTiempo.de(91.0))
    }
}

class ContextoListaTest {
    private val hoy = LocalDate.of(2026, 10, 14)
    private val r = receta("r").copy(totalMin = 47.0)

    @Test
    fun empezarRestaDeLaHoraDeServirDeAjustes() {
        assertEquals(14 * 60 + 43, ContextoLista(hoy).hora(r))
        assertEquals(13 * 60 + 13, ContextoLista(hoy, servirMin = 14 * 60).hora(r))
    }

    @Test
    fun unaRecetaDeCenaEmpiezaRestandoDeLaHoraDeCenar() {
        val cena = r.copy(tags = r.tags.copy(momento = listOf(Momento.CENA)))
        assertEquals(20 * 60 + 13, ContextoLista(hoy).hora(cena))
        assertEquals(CualHora.CENAR, ContextoLista(hoy).servirDe(cena).cual)
        assertEquals(22 * 60 + 13, ContextoLista(hoy, cenarMin = 23 * 60).hora(cena))
        assertEquals(CualHora.COMER, ContextoLista(hoy).servirDe(r).cual)
    }

    @Test
    fun acabarSumaALaHoraActualYDaLaVueltaAMedianoche() {
        val acabar = ContextoLista(hoy, ahoraMin = 12 * 60, modo = HoraTarjeta.ACABAR)
        assertEquals(12 * 60 + 47, acabar.hora(r))
        assertEquals(30, acabar.copy(ahoraMin = 23 * 60 + 43).hora(r))
    }
}

class RecetasLogicaTest {
    private val hoy = LocalDate.of(2026, 10, 14)

    private fun estado(
        recetas: List<Receta>,
        activos: Set<OpcionFiltro> = emptySet(),
        orden: OrdenRecetas = OrdenRecetas.FAVORITAS,
        prefs: Map<String, Preferencia> = emptyMap(),
        fechas: Map<String, String> = emptyMap(),
    ) = construirEstadoRecetas(recetas, prefs, fechas, FiltroRecetas("", activos, orden), ContextoLista(hoy))

    private fun buscar(
        recetas: List<Receta>,
        consulta: String,
    ) = construirEstadoRecetas(recetas, emptyMap(), emptyMap(), FiltroRecetas(consulta), ContextoLista(hoy))

    private fun RecetasUiState.slugs() = tarjetas.map { it.receta.slug }

    private fun RecetasUiState.chip(o: OpcionFiltro) = secciones.flatMap { it.chips }.firstOrNull { it.opcion == o }

    /** Receta de [min] minutos con [fuegos] carriles de fuego, sin horno. */
    private fun r(
        slug: String,
        base: String? = null,
        proteina: String? = null,
        min: Double = 30.0,
        fuegos: Int = 1,
    ) = receta(slug, base = base).copy(
        totalMin = min,
        tags = Tags(base = base, proteina = proteina),
        carriles = List(fuegos) { Carril("f$it", tipo = "fuego") } + Carril("m", tipo = "mesa"),
    )

    private fun Receta.conHorno() = copy(carriles = carriles + Carril("h", tipo = "horno"))

    private val pollo = r("pollo", "salteado", "pollo", min = 40.0, fuegos = 1)
    private val pasta = r("pasta", "pasta", "carne picada", min = 82.0, fuegos = 2)
    private val lasana = r("lasana", "pasta", "carne picada", min = 131.0, fuegos = 3).conHorno()
    private val tarta = r("tarta", "hojaldre", "ninguna", min = 50.0, fuegos = 1).conHorno()
    private val todas = listOf(pollo, pasta, lasana, tarta)

    @Test
    fun buscaIgnorandoAcentosEnIngredientesYPasos() {
        val con = receta("a", "Tortilla").copy(grupos = listOf(Grupo("g", listOf(Ingrediente("Pimiento rojo")))))
        val otra = receta("b", "Otra")
        assertEquals(listOf("a"), buscar(listOf(con, otra), "PIMIENTO").slugs())
        assertEquals(listOf("a", "b"), buscar(listOf(con, otra), "pícar").slugs().sorted())
        assertEquals(listOf("b"), buscar(listOf(con, otra), "otr").slugs())
    }

    @Test
    fun dentroDeUnaSeccionSumaYEntreSeccionesRestringe() {
        val base = setOf(OpcionFiltro.Base(FamiliaBase.PASTA), OpcionFiltro.Base(FamiliaBase.SALTEADO))
        assertEquals(setOf("pollo", "pasta", "lasana"), estado(todas, activos = base).slugs().toSet())
        val conHorno = base + OpcionFiltro.Equipo(EquipoReceta.CON_HORNO)
        assertEquals(listOf("lasana"), estado(todas, activos = conHorno).slugs())
    }

    @Test
    fun filtroDeMomentoCuentaSoloLasQueLoDeclaranYSeCombinaConElTramoRapido() {
        fun Receta.en(vararg m: Momento) = copy(tags = tags.copy(momento = m.toList()))
        val cenaRapida = r("tortilla", min = 20.0).en(Momento.CENA)
        val cenaLarga = r("guiso", min = 60.0).en(Momento.CENA, Momento.COMIDA)
        val comida = r("arroz", min = 25.0).en(Momento.COMIDA)
        val sinMomento = r("pan", min = 20.0)
        val lista = listOf(cenaRapida, cenaLarga, comida, sinMomento)

        val cenas = estado(lista, activos = setOf(OpcionFiltro.MomentoDia(Momento.CENA)))
        assertEquals(setOf("tortilla", "guiso"), cenas.slugs().toSet())
        assertEquals(2, cenas.chip(OpcionFiltro.MomentoDia(Momento.CENA))?.n)
        assertEquals(2, cenas.chip(OpcionFiltro.MomentoDia(Momento.COMIDA))?.n)
        assertEquals(null, cenas.chip(OpcionFiltro.MomentoDia(Momento.POSTRE)))

        // Cena y «Hasta 30 min» siguen filtrando juntos aunque ya no haya cuadradito ni atajo de «Cenas rápidas».
        val rapidas =
            estado(
                lista,
                activos = setOf(OpcionFiltro.MomentoDia(Momento.CENA), OpcionFiltro.Tiempo(TramoTiempo.RAPIDA)),
            )
        assertEquals(listOf("tortilla"), rapidas.slugs())
        assertEquals(1, rapidas.chip(OpcionFiltro.Tiempo(TramoTiempo.RAPIDA))?.n)
    }

    @Test
    fun filtrosDeTiempoEquipoYProteina() {
        assertEquals(listOf("pollo"), estado(todas, activos = setOf(OpcionFiltro.Tiempo(TramoTiempo.CORTA))).slugs())
        assertEquals(
            setOf("pollo", "tarta"),
            estado(todas, activos = setOf(OpcionFiltro.Equipo(EquipoReceta.UN_FUEGO))).slugs().toSet(),
        )
        assertEquals(
            setOf("pollo", "pasta"),
            estado(todas, activos = setOf(OpcionFiltro.Equipo(EquipoReceta.SIN_HORNO))).slugs().toSet(),
        )
        val vegetariana = setOf(OpcionFiltro.Proteina(FamiliaProteina.VEGETARIANA))
        assertEquals(listOf("tarta"), estado(todas, activos = vegetariana).slugs())
    }

    @Test
    fun mostrarFavoritasSinEstrenarYHaceTiempo() {
        val prefs = mapOf("pasta" to Preferencia("pasta", favorito = true))
        val fechas = mapOf("pollo" to "2026-10-10", "pasta" to "2026-05-01")

        fun con(m: MostrarRecetas) =
            estado(todas, activos = setOf(OpcionFiltro.Mostrar(m)), prefs = prefs, fechas = fechas).slugs().toSet()
        assertEquals(setOf("pasta"), con(MostrarRecetas.FAVORITAS))
        assertEquals(setOf("lasana", "tarta"), con(MostrarRecetas.SIN_ESTRENAR))
        assertEquals(setOf("pasta"), con(MostrarRecetas.HACE_TIEMPO))
    }

    @Test
    fun mostrarQuieroHacerlaFiltraLasMarcadasYCuentaCuantasSon() {
        val prefs =
            mapOf(
                "pasta" to Preferencia("pasta", quiereHacerla = true),
                "tarta" to Preferencia("tarta", favorito = true, quiereHacerla = true),
                "pollo" to Preferencia("pollo", favorito = true),
            )
        val quiero = OpcionFiltro.Mostrar(MostrarRecetas.QUIERO_HACERLA)

        val filtrado = estado(todas, activos = setOf(quiero), prefs = prefs)
        assertEquals(setOf("pasta", "tarta"), filtrado.slugs().toSet())
        assertEquals(2, filtrado.chip(quiero)?.n)
        assertEquals(true, filtrado.chip(quiero)?.activo)

        // Sin marcar ninguna, la opción sigue en la hoja con recuento 0.
        assertEquals(0, estado(todas).chip(quiero)?.n)
        // Se combina con otros filtros de «Mostrar» (O dentro de la sección).
        val favoritas = OpcionFiltro.Mostrar(MostrarRecetas.FAVORITAS)
        assertEquals(
            setOf("pasta", "tarta", "pollo"),
            estado(todas, activos = setOf(quiero, favoritas), prefs = prefs).slugs().toSet(),
        )
    }

    @Test
    fun cadaChipCuentaLoQueQuedariaConElRestoDeFiltros() {
        val e = estado(todas, activos = setOf(OpcionFiltro.Equipo(EquipoReceta.CON_HORNO)))
        // Con horno: lasaña (pasta) y tarta (masas). La sección Base cuenta sobre esas dos.
        assertEquals(1, e.chip(OpcionFiltro.Base(FamiliaBase.PASTA))?.n)
        assertEquals(1, e.chip(OpcionFiltro.Base(FamiliaBase.MASAS))?.n)
        // Salteado no tiene ninguna con horno, pero sigue visible porque existe en el catálogo.
        assertEquals(0, e.chip(OpcionFiltro.Base(FamiliaBase.SALTEADO))?.n)
        // Su propia sección no se restringe a sí misma: «Sin horno» cuenta las dos que no lo usan.
        assertEquals(2, e.chip(OpcionFiltro.Equipo(EquipoReceta.SIN_HORNO))?.n)
        assertEquals(true, e.chip(OpcionFiltro.Equipo(EquipoReceta.CON_HORNO))?.activo)
        // Las familias que no existen en el catálogo no salen; «Mostrar» sale siempre.
        assertEquals(null, e.chip(OpcionFiltro.Base(FamiliaBase.ARROZ)))
        assertEquals(0, e.chip(OpcionFiltro.Mostrar(MostrarRecetas.FAVORITAS))?.n)
    }

    @Test
    fun activosOrdenadosPorSeccionYUnFiltroQueYaNoExisteSeIgnora() {
        val activos =
            setOf(
                OpcionFiltro.Equipo(EquipoReceta.CON_HORNO),
                OpcionFiltro.Tiempo(TramoTiempo.LARGA),
                OpcionFiltro.Base(FamiliaBase.ARROZ),
            )
        val e = estado(todas, activos = activos)
        assertEquals(
            listOf(OpcionFiltro.Tiempo(TramoTiempo.LARGA), OpcionFiltro.Equipo(EquipoReceta.CON_HORNO)),
            e.activos,
        )
        assertEquals(listOf("lasana"), e.slugs())
    }

    @Test
    fun ordenes() {
        val rs =
            listOf(
                r("b", min = 60.0).copy(titulo = "Bravas", actualizada = "2026-09-01T00:00:00Z"),
                r("a", min = 90.0).copy(titulo = "Ávila", actualizada = "2026-01-01T00:00:00Z"),
                r("c", min = 20.0).copy(titulo = "Croquetas", actualizada = "2025-01-01T00:00:00Z"),
            )
        val prefs = mapOf("c" to Preferencia("c", favorito = true))
        val fechas = mapOf("a" to "2026-10-01", "b" to "2026-03-01")

        fun orden(o: OrdenRecetas) = estado(rs, orden = o, prefs = prefs, fechas = fechas).slugs()
        assertEquals(listOf("c", "b", "a"), orden(OrdenRecetas.FAVORITAS))
        assertEquals(listOf("c", "b", "a"), orden(OrdenRecetas.OLVIDADAS))
        assertEquals(listOf("a", "b", "c"), orden(OrdenRecetas.ALFABETICO))
        assertEquals(listOf("c", "b", "a"), orden(OrdenRecetas.RAPIDA))
    }

    @Test
    fun categoriasCuentanTodasLasRecetasEnElOrdenDelEnum() {
        val e = estado(todas + r("otro", "legumbre"))
        assertEquals(
            listOf(
                FamiliaBase.PASTA to 2,
                FamiliaBase.MASAS to 1,
                FamiliaBase.SALTEADO to 1,
                FamiliaBase.OTRA to 1,
            ),
            e.categorias,
        )
        assertEquals(null, e.familiaFija)
        assertTrue(e.secciones.any { it.seccion == SeccionFiltro.BASE })
    }

    @Test
    fun familiaFijaPrefiltraRecuentosYTotalPeroNoLasCategorias() {
        val e =
            construirEstadoRecetas(
                todas,
                emptyMap(),
                emptyMap(),
                FiltroRecetas(familiaFija = FamiliaBase.PASTA),
                ContextoLista(hoy),
            )
        assertEquals(FamiliaBase.PASTA, e.familiaFija)
        assertEquals(setOf("pasta", "lasana"), e.slugs().toSet())
        assertEquals(2, e.totalRecetas)
        assertEquals(2, e.coincidentes)
        assertEquals(3, e.categorias.size)
        assertEquals(1, e.chip(OpcionFiltro.Equipo(EquipoReceta.SIN_HORNO))?.n)
    }

    @Test
    fun familiaFijaQuitaLaSeccionBaseEIgnoraSusFiltros() {
        val filtro =
            FiltroRecetas(activos = setOf(OpcionFiltro.Base(FamiliaBase.SALTEADO)), familiaFija = FamiliaBase.PASTA)
        val e = construirEstadoRecetas(todas, emptyMap(), emptyMap(), filtro, ContextoLista(hoy))
        assertTrue(e.secciones.none { it.seccion == SeccionFiltro.BASE })
        assertTrue(e.activos.isEmpty())
        assertEquals(setOf("pasta", "lasana"), e.slugs().toSet())
    }

    @Test
    fun laTarjetaTraeFamiliaHornoFuegosHoraDeEmpezarYHace() {
        val e = estado(listOf(lasana, pollo), fechas = mapOf("pollo" to "2026-07-01"))
        val l = e.tarjetas.first { it.receta.slug == "lasana" }
        val p = e.tarjetas.first { it.receta.slug == "pollo" }
        assertEquals(FamiliaBase.PASTA, l.familia)
        assertTrue(l.conHorno)
        assertFalse(p.conHorno)
        assertEquals(3, l.fuegos)
        // Servir a las 15:30 menos 131 min = 13:19.
        assertEquals(13 * 60 + 19, l.hora)
        assertEquals(HoraTarjeta.EMPEZAR, l.modoHora)
        assertEquals(Hace(UnidadHace.MESES, 3, true), p.hace)
        assertEquals(UnidadHace.SIN_ESTRENAR, l.hace.unidad)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecetasViewModelTest {
    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun favoritaSubeAlPrincipioYElHistorialDaElHace() =
        runTest(UnconfinedTestDispatcher()) {
            val prefs = FakePreferenciasRepo()
            val historial = FakeHistorialRepo().also { it.ultimas.value = mapOf("b" to "2026-10-13") }
            val reloj =
                Reloj {
                    LocalDate
                        .of(2026, 10, 14)
                        .atStartOfDay()
                        .toInstant(java.time.ZoneOffset.UTC)
                        .toEpochMilli()
                }
            val recetas = FakeRecetasRepo(listOf(receta("a", base = "pasta"), receta("b", base = "arroz")))
            val ajustes = FakeAjustesRepo()
            val vm = RecetasViewModel(SavedStateHandle(), recetas, prefs, historial, ajustes, reloj)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            assertEquals(2, vm.estado.value.tarjetas.size)
            assertEquals(
                UnidadHace.AYER,
                vm.estado.value.tarjetas
                    .first { it.receta.slug == "b" }
                    .hace.unidad,
            )

            vm.alternarFavorita("b", true)
            assertEquals(
                "b",
                vm.estado.value.tarjetas
                    .first()
                    .receta.slug,
            )

            vm.buscar("zzz")
            assertTrue(
                vm.estado.value.tarjetas
                    .isEmpty(),
            )
        }

    @Test
    fun filtrosOrdenYDisenoDeLaLista() =
        runTest(UnconfinedTestDispatcher()) {
            val ajustes = FakeAjustesRepo()
            val recetas = FakeRecetasRepo(listOf(receta("a", base = "pasta"), receta("b", base = "arroz")))
            val vm =
                RecetasViewModel(
                    SavedStateHandle(),
                    recetas,
                    FakePreferenciasRepo(),
                    FakeHistorialRepo(),
                    ajustes,
                    { 0L },
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            vm.alternarFiltro(OpcionFiltro.Base(FamiliaBase.PASTA))
            assertEquals(
                listOf("a"),
                vm.estado.value.tarjetas
                    .map { it.receta.slug },
            )
            vm.alternarFiltro(OpcionFiltro.Base(FamiliaBase.PASTA))
            assertEquals(2, vm.estado.value.tarjetas.size)

            vm.alternarFiltro(OpcionFiltro.Base(FamiliaBase.ARROZ))
            vm.limpiarFiltros()
            assertTrue(
                vm.estado.value.activos
                    .isEmpty(),
            )

            vm.elegirOrden(OrdenRecetas.ALFABETICO)
            assertEquals(OrdenRecetas.ALFABETICO, ajustes.actual.value.orden)
            assertEquals(OrdenRecetas.ALFABETICO, vm.estado.value.orden)
            assertEquals(
                listOf("a", "b"),
                vm.estado.value.tarjetas
                    .map { it.receta.slug },
            )

            ajustes.actual.value = ajustes.actual.value.copy(lista = DisposicionLista.FRANJA)
            assertEquals(DisposicionLista.FRANJA, vm.estado.value.disposicion)
        }

    @Test
    fun laFamiliaDeLaRutaFijaLaLista() =
        runTest(UnconfinedTestDispatcher()) {
            val recetas = FakeRecetasRepo(listOf(receta("a", base = "pasta"), receta("b", base = "arroz")))
            val args = SavedStateHandle(mapOf(Rutas.ARG_FAMILIA to "arroz"))
            val vm =
                RecetasViewModel(args, recetas, FakePreferenciasRepo(), FakeHistorialRepo(), FakeAjustesRepo(), { 0L })
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            assertEquals(FamiliaBase.ARROZ, vm.estado.value.familiaFija)
            assertEquals(
                listOf("b"),
                vm.estado.value.tarjetas
                    .map { it.receta.slug },
            )
            assertEquals(2, vm.estado.value.categorias.size)
        }

    @Test
    fun sinArgumentoOConUnoDesconocidoNoHayFamiliaFija() =
        runTest(UnconfinedTestDispatcher()) {
            val recetas = FakeRecetasRepo(listOf(receta("a", base = "pasta")))
            val args = SavedStateHandle(mapOf(Rutas.ARG_FAMILIA to "nada"))
            val vm =
                RecetasViewModel(args, recetas, FakePreferenciasRepo(), FakeHistorialRepo(), FakeAjustesRepo(), { 0L })
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            assertEquals(null, vm.estado.value.familiaFija)
            assertEquals(1, vm.estado.value.tarjetas.size)
        }

    @Test
    fun laClaveAntiguaCenasRapidasYaNoPoneFiltrosNiFamilia() =
        runTest(UnconfinedTestDispatcher()) {
            fun cena(
                slug: String,
                min: Double,
            ) = receta(slug).let { it.copy(totalMin = min, tags = it.tags.copy(momento = listOf(Momento.CENA))) }

            val recetas = FakeRecetasRepo(listOf(cena("tortilla", 20.0), cena("guiso", 90.0), receta("pan")))
            val args = SavedStateHandle(mapOf(Rutas.ARG_FAMILIA to "cenas-rapidas"))
            val vm =
                RecetasViewModel(args, recetas, FakePreferenciasRepo(), FakeHistorialRepo(), FakeAjustesRepo(), { 0L })
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }

            assertEquals(null, vm.estado.value.familiaFija)
            assertTrue(
                vm.estado.value.activos
                    .isEmpty(),
            )
            assertEquals(3, vm.estado.value.tarjetas.size)
        }

    @Test
    fun enModoAcabasLaHoraAvanzaConElReloj() =
        runTest(UnconfinedTestDispatcher()) {
            val ajustes = FakeAjustesRepo()
            ajustes.actual.value = ajustes.actual.value.copy(horaTarjeta = HoraTarjeta.ACABAR)
            val reloj = RelojFalso()
            val recetas = FakeRecetasRepo(listOf(receta("a")))
            val vm =
                RecetasViewModel(
                    SavedStateHandle(),
                    recetas,
                    FakePreferenciasRepo(),
                    FakeHistorialRepo(),
                    ajustes,
                    reloj,
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.estado.collect {} }
            try {
                val local = { ms: Long -> Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()) }

                fun esperado(ms: Long) = local(ms).let { (it.hour * 60 + it.minute + 30) % (24 * 60) }
                val antes =
                    vm.estado.value.tarjetas
                        .first()
                assertEquals(HoraTarjeta.ACABAR, antes.modoHora)
                assertEquals(esperado(reloj.ms), antes.hora)
                reloj.ms += 5 * 60_000L
                advanceTimeBy(5 * 60_000L + 1)
                assertEquals(
                    esperado(reloj.ms),
                    vm.estado.value.tarjetas
                        .first()
                        .hora,
                )
            } finally {
                vm.viewModelScope.cancel()
            }
        }
}
