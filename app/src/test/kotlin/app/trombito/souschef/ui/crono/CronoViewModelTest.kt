package app.trombito.souschef.ui.crono

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.domain.crono.AlmacenCrono
import app.trombito.souschef.domain.crono.CadenaAlarmas
import app.trombito.souschef.receta.BloqueMep
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.SesionCrono
import app.trombito.souschef.receta.crono.TipoAviso
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.FakeTecnicasRepo
import app.trombito.souschef.ui.Rutas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CronoViewModelTest {
    private val creados = mutableListOf<CronoViewModel>()
    private val repo = FakeCronoEstadoRepo()
    private val historial = FakeHistorialRepo()
    private val reloj = RelojFalso()
    private val sistema = FakeAlarmaSistema()
    private val avisador = FakeAvisador()
    private val ajustes = FakeAjustesRepo()
    private val escucha = FakeEscucha()
    private val habla = FakeHabla()
    private val almacen = AlmacenCrono(repo)
    private val recetas = FakeRecetasRepo(listOf(recetaCrono()))
    private val notificador = FakeNotificador()
    private val cadena = CadenaAlarmas(almacen, recetas, reloj, sistema, notificador)

    @Before
    fun antes() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    private fun TestScope.crear(
        raciones: Int = 0,
        servir: String = "12:15",
    ): CronoViewModel {
        val args = mapOf(Rutas.ARG_SLUG to "r", Rutas.ARG_RACIONES to raciones, Rutas.ARG_SERVIR to servir)
        return CronoViewModel(
            SavedStateHandle(args),
            recetas,
            FakeTecnicasRepo(),
            historial,
            cadena,
            reloj,
            avisador,
            ajustes,
            escucha,
            habla,
        ).also {
            creados += it
            runCurrent()
        }
    }

    /** El bucle de tic es infinito: hay que parar cada ViewModel o `runTest` no termina. */
    private fun prueba(bloque: suspend TestScope.() -> Unit) =
        runTest {
            try {
                bloque()
            } finally {
                creados.forEach { it.viewModelScope.cancel() }
            }
        }

    private suspend fun guardado(): EstadoCrono =
        Json.decodeFromString(EstadoCrono.serializer(), checkNotNull(repo.obtener("r")).json)

    private fun TestScope.siguienteTic() {
        advanceTimeBy(1_001)
        runCurrent()
    }

    @Test
    fun sinHoraEnLaRutaUsaLaHoraDeServirDeAjustes() =
        prueba {
            ajustes.actual.value = ajustes.actual.value.copy(servirMin = 12 * 60 + 45)
            crear(servir = "")
            assertEquals(T0 + 45 * 60_000L, guardado().servirMs)
        }

    @Test
    fun alEntrarCreaYPersisteLaPortadaConLasRacionesYLaHoraDeServir() =
        prueba {
            val vm = crear(raciones = 4)
            val e = guardado()
            assertNull(e.inicioMs)
            assertEquals(4, e.raciones)
            assertEquals(T0 + 15 * 60_000L, e.servirMs)
            assertEquals(e, vm.ui.value.estado)
            assertNull(sistema.programada)
        }

    @Test
    fun abrirLaCocinaFijaElInicioYLosMandosSePersistenYArmanLaAlarma() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            assertEquals(T0, guardado().inicioMs)

            vm.alternar("f0")
            runCurrent()
            val f0 = guardado().carriles.getValue("f0")
            assertEquals(T0 + 300_000L, f0.finAtMs)
            // Próximo evento: el fin del paso (ya no hay aviso de 10 s).
            assertEquals(T0 + 300_000L, sistema.programada)

            vm.masTreinta("f0")
            runCurrent()
            assertEquals(T0 + 330_000L, guardado().carriles.getValue("f0").finAtMs)
            assertEquals(T0 + 330_000L, sistema.programada)
        }

    @Test
    fun prorrogarSumaLosSegundosPedidosAlCarril() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.alternar("f0")
            runCurrent()
            val fin0 = guardado().carriles.getValue("f0").finAtMs
            vm.prorrogar("f0", 120)
            runCurrent()
            assertEquals(fin0?.plus(120_000L), guardado().carriles.getValue("f0").finAtMs)
        }

    @Test
    fun elDobleToqueDeHechoSoloCuentaUnaVezHastaElSiguienteTic() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.hecho("f0")
            vm.hecho("f0")
            runCurrent()
            assertEquals(1, guardado().carriles.getValue("f0").i)
            siguienteTic()
            vm.hecho("f0")
            runCurrent()
            assertTrue(guardado().carriles.getValue("f0").fin)
        }

    @Test
    fun tresMasTreintaSeguidosSinTicSumanNoventaSegundos() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.alternar("f0")
            runCurrent()
            val fin0 = guardado().carriles.getValue("f0").finAtMs
            vm.masTreinta("f0")
            vm.masTreinta("f0")
            vm.masTreinta("f0")
            runCurrent()
            assertEquals(fin0?.plus(90_000L), guardado().carriles.getValue("f0").finAtMs)
        }

    @Test
    fun sinLaCocinaALaVistaElTicNoConsumeLosAvisosQueDebeEmitirLaCadena() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.alternar("f0")
            runCurrent()
            vm.alVer()
            runCurrent()
            vm.enSegundoPlano()
            runCurrent()
            assertNull(cadena.presencia.visible)

            reloj.ms = T0 + 301_000L
            siguienteTic()

            assertTrue(avisador.sonados.isEmpty())
            assertFalse(guardado().carriles.getValue("f0").aFin)
            assertEquals(T0 + 301_000L, vm.ui.value.ahoraMs)
            // Lo consume la cadena de alarmas: la alarma del sistema, no un pitido.
            cadena.procesar()
            assertTrue(guardado().carriles.getValue("f0").aFin)
            assertTrue(notificador.alarmaActual.containsKey("r"))
        }

    @Test
    fun enPrimerPlanoElTicSuenaElFinYLoMarca() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.alVer()
            runCurrent()
            vm.alternar("f0")
            runCurrent()
            reloj.ms = T0 + 301_000L
            siguienteTic()
            assertEquals(listOf(TipoAviso.FIN), avisador.sonados)
            assertTrue(guardado().carriles.getValue("f0").aFin)
            assertEquals(T0 + 301_000L, vm.ui.value.ahoraMs)
        }

    @Test
    fun alTerminarTodoElCierreEscribeElHistorialYBorraElEstado() =
        prueba {
            val vm = crear(raciones = 3)
            vm.abrirCocina()
            runCurrent()
            listOf("f0", "m").forEach {
                vm.hecho(it)
                runCurrent()
                siguienteTic()
            }
            vm.hecho("f0")
            runCurrent()
            assertNotNull(vm.ui.value.resumen)
            assertEquals(1, avisador.cierres)

            vm.cerrar("  menos sal  ")
            runCurrent()
            assertTrue(vm.ui.value.cierrePendiente)
            assertTrue(historial.datos.value.isEmpty())
            assertNotNull(repo.obtener("r"))
            assertEquals(listOf("r"), notificador.limpiados)
            advanceTimeBy(VENTANA_DESHACER_MS + 100)
            runCurrent()
            val entrada = historial.datos.value.single()
            assertEquals("r", entrada.slug)
            assertEquals("Receta r", entrada.titulo)
            assertEquals(3, entrada.raciones)
            assertEquals("menos sal", entrada.nota)
            assertEquals(setOf("p1", "p2", "p3"), entrada.pasosReales.map { it.id }.toSet())
            assertTrue(entrada.id.startsWith("r-2026-10-14-"))
            assertEquals("2026-10-14", entrada.fecha)
            assertNull(repo.obtener("r"))
            assertTrue(vm.ui.value.salida)
            assertNull(sistema.programada)
        }

    @Test
    fun deshacerElCierreNoEscribeNadaYVuelveAlResumen() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            listOf("m", "f0", "f0").forEach {
                vm.hecho(it)
                runCurrent()
                siguienteTic()
            }
            vm.cerrar("")
            runCurrent()
            vm.deshacerCierre()
            advanceTimeBy(VENTANA_DESHACER_MS + 100)
            runCurrent()
            assertFalse(vm.ui.value.cierrePendiente)
            assertFalse(vm.ui.value.salida)
            assertTrue(historial.datos.value.isEmpty())
            assertNotNull(repo.obtener("r"))
            assertNotNull(vm.ui.value.resumen)
        }

    @Test
    fun salirDuranteLaVentanaConfirmaElCierreAlMomento() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            listOf("m", "f0", "f0").forEach {
                vm.hecho(it)
                runCurrent()
                siguienteTic()
            }
            vm.cerrar("")
            runCurrent()
            vm.enSegundoPlano()
            runCurrent()
            assertEquals(1, historial.datos.value.size)
            assertNull(repo.obtener("r"))
        }

    @Test
    fun conLaCocinaEnPantallaNoHayNotificacionesYAlSalirReapareceLaFija() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            assertTrue(notificador.cocinas.containsKey("r"))
            vm.alVer()
            runCurrent()
            assertEquals("r", cadena.presencia.visible)
            assertFalse(notificador.cocinas.containsKey("r"))
            // Con la cocina visible un repintado (p. ej. tras un mando) tampoco la vuelve a pintar.
            vm.alternar("f0")
            runCurrent()
            assertFalse(notificador.cocinas.containsKey("r"))
            assertTrue(notificador.alarmas.isEmpty())
            vm.enSegundoPlano()
            runCurrent()
            assertNull(cadena.presencia.visible)
            assertTrue(notificador.cocinas.containsKey("r"))
        }

    @Test
    fun alVerSeCallaLaAlarmaQueSonaba() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.alternar("f0")
            runCurrent()
            vm.enSegundoPlano()
            runCurrent()
            reloj.ms = T0 + 301_000L
            cadena.procesar()
            assertTrue(notificador.alarmaActual.containsKey("r"))
            vm.alVer()
            runCurrent()
            assertFalse(notificador.alarmaActual.containsKey("r"))
        }

    @Test
    fun hechoCancelaLosAvisosDelCarrilYSeRepintan() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            assertEquals(
                setOf("f0", "m"),
                notificador.carrilesPorSlug
                    .getValue("r")
                    .map { it.carril }
                    .toSet(),
            )
            vm.hecho("f0")
            runCurrent()
            // Sin alarma en memoria (tras morir el proceso) se quita por si seguía sonando; la fija sigue pintada.
            assertEquals(listOf("r"), notificador.alarmasQuitadas)
            assertEquals(
                setOf("f0", "m"),
                notificador.carrilesPorSlug
                    .getValue("r")
                    .map { it.carril }
                    .toSet(),
            )
        }

    @Test
    fun reiniciarNecesitaDosToquesYVuelveALaPortada() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.hecho("m")
            runCurrent()
            vm.reiniciar()
            assertTrue(vm.ui.value.reiniciarArmado)
            assertTrue(guardado().carriles.getValue("m").fin)

            vm.reiniciar()
            runCurrent()
            assertFalse(vm.ui.value.reiniciarArmado)
            val e = guardado()
            assertNull(e.inicioMs)
            assertFalse(e.carriles.getValue("m").fin)
        }

    @Test
    fun unaCocinadaEnMarchaSeReanudaSinPisarlaConLaHoraDeLaFicha() =
        prueba {
            val primero = crear()
            primero.abrirCocina()
            runCurrent()
            val vm = crear(servir = "18:00")
            assertEquals(
                T0 + 15 * 60_000L,
                vm.ui.value.estado
                    ?.servirMs,
            )
            assertEquals(
                T0,
                vm.ui.value.estado
                    ?.inicioMs,
            )
        }

    @Test
    fun laDisposicionDeLaCocinaSigueAlosAjustes() =
        prueba {
            val vm = crear()
            assertEquals(DisposicionCrono.APILADA, vm.ui.value.disposicion)

            ajustes.fijarDisposicion(DisposicionCrono.FOCO)
            runCurrent()

            assertEquals(DisposicionCrono.FOCO, vm.ui.value.disposicion)
        }

    @Test
    fun deshacerVuelveAlPasoAnteriorYLoGuarda() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            val carril = guardado().carriles.keys.first()
            vm.hecho(carril)
            runCurrent()
            assertEquals(1, guardado().carriles.getValue(carril).i)

            vm.deshacer(carril)
            runCurrent()

            assertEquals(0, guardado().carriles.getValue(carril).i)
            assertTrue(guardado().deshacer.isEmpty())
        }

    @Test
    fun elegirLaInterfazDesdeLaCocinaLaGuardaEnAjustesYLaAplica() =
        prueba {
            val vm = crear()

            vm.elegirDisposicion(DisposicionCrono.CLASICA)
            runCurrent()

            assertEquals(DisposicionCrono.CLASICA, ajustes.actual.value.disposicion)
            assertEquals(DisposicionCrono.CLASICA, vm.ui.value.disposicion)
        }

    @Test
    fun hechoDelCarrilFijadoSueltaElFocoYSaltaAlSiguienteUrgente() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.alternar("m")
            vm.enfocar("f0")
            runCurrent()
            assertEquals("f0", guardado().foco)

            vm.hecho("f0")
            runCurrent()

            assertEquals("m", guardado().foco)
        }

    @Test
    fun hechoDeUnCarrilQueNoEraElFijadoNoMueveElFoco() =
        prueba {
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            vm.alternar("m")
            vm.enfocar("f0")
            runCurrent()

            vm.hecho("m")
            runCurrent()

            assertEquals("f0", guardado().foco)
        }

    @Test
    fun laHoraDeServirEsDeHoyEnLaZonaLocal() {
        val ms = servirDeHoy("15:30", T0, ZoneId.systemDefault())
        assertEquals(3 * 60 * 60_000L + 30 * 60_000L, ms - T0)
    }

    // ---- voz (ADR-0008)

    private fun TestScope.cocinaAbierta(): CronoViewModel {
        val vm = crear()
        vm.abrirCocina()
        runCurrent()
        return vm
    }

    @Test
    fun conLaVozDesactivadaEnAjustesNoSeEmpiezaAEscuchar() =
        prueba {
            ajustes.actual.value = Ajustes(vozActiva = false)
            val vm = cocinaAbierta()
            vm.vozIniciar(permiso = true)
            runCurrent()
            assertEquals(0, escucha.empezadas)
            assertEquals(EstadoVoz.APAGADA, vm.ui.value.voz)
        }

    @Test
    fun conVozActivaPermisoYCocinaAbiertaEscuchaYAlParaDeVerseSePara() =
        prueba {
            val vm = cocinaAbierta()
            vm.vozIniciar(permiso = true)
            runCurrent()
            assertEquals(1, escucha.empezadas)
            assertEquals(EstadoVoz.ESCUCHANDO, vm.ui.value.voz)

            vm.vozParar()
            assertEquals(1, escucha.paradas)
            assertEquals(EstadoVoz.APAGADA, vm.ui.value.voz)
        }

    @Test
    fun siElBucleSePareSinPermisoLaCabeceraPasaASinPermisoYNoSeReintenta() =
        prueba {
            val vm = cocinaAbierta()
            vm.vozIniciar(permiso = true)
            runCurrent()
            assertEquals(EstadoVoz.ESCUCHANDO, vm.ui.value.voz)

            escucha.detenerseSolo(porPermiso = true)

            assertEquals(EstadoVoz.SIN_PERMISO, vm.ui.value.voz)
            siguienteTic()
            assertEquals(1, escucha.empezadas)

            vm.vozIniciar(permiso = true)
            assertEquals(2, escucha.empezadas)
            assertEquals(EstadoVoz.ESCUCHANDO, vm.ui.value.voz)
        }

    @Test
    fun siElBucleSePareSoloPorElIdiomaLaVozQuedaApagadaYNoSeReintentaEnCadaTic() =
        prueba {
            val vm = cocinaAbierta()
            vm.vozIniciar(permiso = true)
            runCurrent()

            escucha.detenerseSolo(porPermiso = false)

            assertEquals(EstadoVoz.APAGADA, vm.ui.value.voz)
            repeat(3) { siguienteTic() }
            assertEquals(1, escucha.empezadas)
            assertEquals(EstadoVoz.APAGADA, vm.ui.value.voz)

            // Al volver a verse la pantalla se prueba de nuevo.
            vm.vozIniciar(permiso = true)
            assertEquals(2, escucha.empezadas)
            assertEquals(EstadoVoz.ESCUCHANDO, vm.ui.value.voz)
        }

    @Test
    fun enLaPortadaTodaviaNoSeEscucha() =
        prueba {
            val vm = crear()
            vm.vozIniciar(permiso = true)
            runCurrent()
            assertEquals(0, escucha.empezadas)
        }

    @Test
    fun sinPermisoNoEscuchaYLoDice() =
        prueba {
            val vm = cocinaAbierta()
            vm.vozIniciar(permiso = false)
            runCurrent()
            assertEquals(0, escucha.empezadas)
            assertEquals(EstadoVoz.SIN_PERMISO, vm.ui.value.voz)

            vm.vozIniciar(permiso = true)
            assertEquals(1, escucha.empezadas)
            assertEquals(EstadoVoz.ESCUCHANDO, vm.ui.value.voz)
        }

    @Test
    fun sinReconocedorLaVozQuedaApagada() =
        prueba {
            val sinReconocedor = FakeEscucha(disponible = false)
            val args = mapOf(Rutas.ARG_SLUG to "r", Rutas.ARG_RACIONES to 0, Rutas.ARG_SERVIR to "12:15")
            val vm =
                CronoViewModel(
                    SavedStateHandle(args),
                    recetas,
                    FakeTecnicasRepo(),
                    historial,
                    cadena,
                    reloj,
                    avisador,
                    ajustes,
                    sinReconocedor,
                    habla,
                ).also { creados += it }
            runCurrent()
            vm.abrirCocina()
            runCurrent()
            vm.vozIniciar(permiso = true)
            assertEquals(0, sinReconocedor.empezadas)
            assertEquals(EstadoVoz.APAGADA, vm.ui.value.voz)
        }

    @Test
    fun silenciarVozParaLaEscuchaYReactivarLaVuelveAPonerSinPersistirNada() =
        prueba {
            val vm = cocinaAbierta()
            vm.vozIniciar(permiso = true)
            runCurrent()

            vm.silenciarVoz()
            assertTrue(vm.ui.value.vozSilenciada)
            assertEquals(1, escucha.paradas)
            assertEquals(EstadoVoz.APAGADA, vm.ui.value.voz)

            vm.silenciarVoz()
            assertFalse(vm.ui.value.vozSilenciada)
            assertEquals(2, escucha.empezadas)
            assertEquals(EstadoVoz.ESCUCHANDO, vm.ui.value.voz)
            assertTrue(ajustes.actual.value.vozActiva)
        }

    @Test
    fun laUiRecibeLaRecetaTraducidaYElMotorSigueConLaCanonicaAlCambiarDeCocina() =
        prueba {
            val gas = Cocina(TipoCocina.GAS, Mando.ANALOGICO, null)
            val canonica =
                recetaCrono().copy(
                    mep = listOf(BloqueMep("Antes", l = listOf("Aceite al {7}"))),
                    pasos = recetaCrono().pasos.map { if (it.id == "p1") it.copy(t = "Baja al {3}") else it },
                )
            recetas.datos.value = listOf(canonica)
            ajustes.fijarCocina(gas)
            val vm = crear()
            assertEquals(gas, vm.ui.value.cocina)
            assertEquals("Baja al {3}", vm.textoCanonico())
            assertEquals("Baja a fuego bajo", vm.textoTraducido())

            val id = SesionCrono.idCasilla(canonica.mep[0].l[0])
            vm.marcar(id, true)
            runCurrent()
            val antes = guardado()
            assertTrue(id in antes.marcados)

            ajustes.fijarCocina(Cocina.VITRO_9)
            runCurrent()
            assertEquals("Baja al 3", vm.textoTraducido())
            assertEquals("Baja al {3}", vm.textoCanonico())
            assertEquals(antes.marcados, guardado().marcados)
        }

    @Test
    fun conInduccionDigital12LaUiMuestraElNumeroProporcional() =
        prueba {
            ajustes.fijarCocina(Cocina(TipoCocina.INDUCCION, Mando.DIGITAL, 12))
            recetas.datos.value =
                listOf(
                    recetaCrono().copy(
                        pasos = recetaCrono().pasos.map { if (it.id == "p1") it.copy(t = "Sube al {9}") else it },
                    ),
                )
            val vm = crear()
            vm.abrirCocina()
            runCurrent()
            assertEquals("Sube al {9}", vm.textoCanonico())
            assertEquals("Sube al 12", vm.textoTraducido())
        }

    private fun Receta?.textoP1(): String? = this?.let { it.pasos[0].t }

    private fun CronoViewModel.textoCanonico(): String? = ui.value.receta.textoP1()

    private fun CronoViewModel.textoTraducido(): String? = ui.value.recetaUi.textoP1()
}
