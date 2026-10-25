package app.trombito.souschef.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.trombito.souschef.data.db.MetaEntity
import app.trombito.souschef.data.db.SousChefDatabase
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.domain.OrdenRecetas
import app.trombito.souschef.domain.TemaApp
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.TipoCocina
import io.mockk.coVerify
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
class AjustesRepoRoomTest {
    private lateinit var db: SousChefDatabase
    private lateinit var repo: AjustesRepoRoom

    @Before
    fun abrir() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SousChefDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repo = AjustesRepoRoom(db.metaDao())
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun sinDatosDevuelveElDefecto() =
        runBlocking {
            assertEquals(Ajustes(DisposicionCrono.APILADA, true), repo.observar().first())
            assertEquals(DisposicionLista.TINTE, repo.observar().first().lista)
            assertEquals(OrdenRecetas.FAVORITAS, repo.observar().first().orden)
        }

    @Test
    fun fijarYObservar() =
        runBlocking {
            repo.fijarDisposicion(DisposicionCrono.APILADA)
            repo.fijarVoz(false)
            assertEquals(Ajustes(DisposicionCrono.APILADA, false), repo.observar().first())

            repo.fijarDisposicion(DisposicionCrono.FOCO)
            assertEquals(Ajustes(DisposicionCrono.FOCO, false), repo.observar().first())
        }

    @Test
    fun valorCorruptoVuelveAlDefecto() =
        runBlocking {
            db.metaDao().fijar(MetaEntity(AjustesRepoRoom.CLAVE_DISPOSICION, "diagonal"))
            db.metaDao().fijar(MetaEntity(AjustesRepoRoom.CLAVE_VOZ, "quizá"))
            db.metaDao().fijar(MetaEntity(AjustesRepoRoom.CLAVE_LISTA, "mosaico"))
            db.metaDao().fijar(MetaEntity(AjustesRepoRoom.CLAVE_ORDEN, "aleatorio"))
            db.metaDao().fijar(MetaEntity(AjustesRepoRoom.CLAVE_TEMA, "sepia"))
            db.metaDao().fijar(MetaEntity(AjustesRepoRoom.CLAVE_SERVIR, "25:99"))
            db.metaDao().fijar(MetaEntity(AjustesRepoRoom.CLAVE_HORA, "manana"))
            assertEquals(Ajustes(), repo.observar().first())
        }

    @Test
    fun valoresCorruptosDeLaCocinaYLosNuevosAjustesVuelvenAlDefecto() =
        runBlocking {
            val meta = db.metaDao()
            meta.fijar(MetaEntity(AjustesRepoRoom.CLAVE_COCINA_TIPO, "microondas"))
            meta.fijar(MetaEntity(AjustesRepoRoom.CLAVE_COCINA_MANDO, "tactil"))
            meta.fijar(MetaEntity(AjustesRepoRoom.CLAVE_COCINA_MAX, "muchos"))
            meta.fijar(MetaEntity(AjustesRepoRoom.CLAVE_FUEGOS, "tres"))
            meta.fijar(MetaEntity(AjustesRepoRoom.CLAVE_HORNO, "puede"))
            meta.fijar(MetaEntity(AjustesRepoRoom.CLAVE_RACIONES, ""))
            meta.fijar(MetaEntity(AjustesRepoRoom.CLAVE_INTRO, "quizá"))
            meta.fijar(MetaEntity(AjustesRepoRoom.CLAVE_GUIA, "0"))
            // Una intro corrupta no cuenta como vista: vuelve al defecto (false).
            assertEquals(Ajustes(), repo.observar().first())
        }

    @Test
    fun laCocinaGuardadaFueraDeRangoSeAjustaConValida() =
        runBlocking {
            val meta = db.metaDao()
            meta.fijar(MetaEntity(AjustesRepoRoom.CLAVE_COCINA_TIPO, "GAS"))
            meta.fijar(MetaEntity(AjustesRepoRoom.CLAVE_COCINA_MANDO, "ANALOGICO"))
            meta.fijar(MetaEntity(AjustesRepoRoom.CLAVE_COCINA_MAX, "99"))
            assertEquals(Cocina(TipoCocina.GAS, Mando.ANALOGICO, Cocina.ANALOGICO_MAX), repo.observar().first().cocina)

            meta.fijar(MetaEntity(AjustesRepoRoom.CLAVE_COCINA_MANDO, "DIGITAL"))
            meta.fijar(MetaEntity(AjustesRepoRoom.CLAVE_COCINA_MAX, ""))
            assertEquals(Cocina(TipoCocina.GAS, Mando.DIGITAL, Cocina.DIGITAL_MIN), repo.observar().first().cocina)
        }

    @Test
    fun cocinaIdaYVuelta() =
        runBlocking {
            assertEquals(Cocina.VITRO_9, repo.observar().first().cocina)

            val induccion = Cocina(TipoCocina.INDUCCION, Mando.DIGITAL, 14)
            repo.fijarCocina(induccion)
            assertEquals(induccion, repo.observar().first().cocina)

            val gas = Cocina(TipoCocina.GAS, Mando.ANALOGICO, 6)
            repo.fijarCocina(gas)
            assertEquals(gas, repo.observar().first().cocina)

            val sinNumeros = Cocina(TipoCocina.GAS, Mando.ANALOGICO, null)
            repo.fijarCocina(sinNumeros)
            assertEquals(sinNumeros, repo.observar().first().cocina)
            assertEquals("", db.metaDao().valor(AjustesRepoRoom.CLAVE_COCINA_MAX))
        }

    @Test
    fun fijarCocinaEscribeLasTresClavesEnUnaSolaLlamadaAtomica() =
        runBlocking {
            val dao = spyk(db.metaDao())
            val repoEspia = AjustesRepoRoom(dao)
            val gas = Cocina(TipoCocina.GAS, Mando.ANALOGICO, 6)

            repoEspia.fijarCocina(gas)

            val escritas = slot<List<MetaEntity>>()
            coVerify(exactly = 1) { dao.fijarVarias(capture(escritas)) }
            coVerify(exactly = 0) { dao.fijar(any()) }
            assertEquals(
                setOf(
                    AjustesRepoRoom.CLAVE_COCINA_TIPO,
                    AjustesRepoRoom.CLAVE_COCINA_MANDO,
                    AjustesRepoRoom.CLAVE_COCINA_MAX,
                ),
                escritas.captured.map { it.clave }.toSet(),
            )
            assertEquals(gas, repo.observar().first().cocina)
        }

    @Test
    fun observarNuncaEmiteUnaCocinaMezclada() =
        runBlocking {
            val a = Cocina(TipoCocina.INDUCCION, Mando.DIGITAL, 14)
            val b = Cocina(TipoCocina.GAS, Mando.ANALOGICO, 6)
            repo.fijarCocina(a)

            val vistas = CopyOnWriteArrayList<Cocina>()
            val colector = launch(Dispatchers.IO) { repo.observar().collect { vistas.add(it.cocina) } }
            repeat(RONDAS) { i -> repo.fijarCocina(if (i % 2 == 0) b else a) }
            withTimeout(ESPERA_MS) { while (vistas.lastOrNull() != a) delay(PAUSA_MS) }
            colector.cancel()

            assertTrue("Cocinas mezcladas: ${vistas.toSet() - setOf(a, b)}", setOf(a, b).containsAll(vistas))
        }

    @Test
    fun fijarCocinaLaAjustaConValida() =
        runBlocking {
            repo.fijarCocina(Cocina(TipoCocina.INDUCCION, Mando.DIGITAL, 50))
            val tope = Cocina(TipoCocina.INDUCCION, Mando.DIGITAL, Cocina.DIGITAL_MAX)
            assertEquals(tope, repo.observar().first().cocina)
            assertEquals("20", db.metaDao().valor(AjustesRepoRoom.CLAVE_COCINA_MAX))

            repo.fijarCocina(Cocina(TipoCocina.GAS, Mando.ANALOGICO, 1))
            assertEquals(Cocina(TipoCocina.GAS, Mando.ANALOGICO, Cocina.ANALOGICO_MIN), repo.observar().first().cocina)
        }

    @Test
    fun fuegosHornoYRacionesPorDefectoIdaYVuelta() =
        runBlocking {
            val defecto = repo.observar().first()
            assertEquals(3, defecto.fuegos)
            assertEquals(true, defecto.horno)
            assertEquals(2, defecto.racionesDefecto)

            repo.fijarFuegos(4)
            repo.fijarHorno(false)
            repo.fijarRacionesDefecto(5)
            val a = repo.observar().first()
            assertEquals(4, a.fuegos)
            assertEquals(false, a.horno)
            assertEquals(5, a.racionesDefecto)
            assertEquals("4", db.metaDao().valor(AjustesRepoRoom.CLAVE_FUEGOS))
            assertEquals("5", db.metaDao().valor(AjustesRepoRoom.CLAVE_RACIONES))
        }

    @Test
    fun fuegosYRacionesSeAjustanASuRango() =
        runBlocking {
            repo.fijarFuegos(0)
            repo.fijarRacionesDefecto(0)
            assertEquals(1, repo.observar().first().fuegos)
            assertEquals(1, repo.observar().first().racionesDefecto)

            repo.fijarFuegos(99)
            repo.fijarRacionesDefecto(99)
            assertEquals(6, repo.observar().first().fuegos)
            assertEquals(6, repo.observar().first().racionesDefecto)
        }

    @Test
    fun marcasDeIntroYDeGuiaIdaYVuelta() =
        runBlocking {
            assertEquals(false, repo.observar().first().introVista)
            assertEquals(false, repo.observar().first().guiaCronoVista)

            repo.marcarIntroVista(true)
            repo.marcarGuiaCronoVista(true)
            val vistas = repo.observar().first()
            assertEquals(true, vistas.introVista)
            assertEquals(true, vistas.guiaCronoVista)

            repo.marcarIntroVista(false)
            repo.marcarGuiaCronoVista(false)
            val repetir = repo.observar().first()
            assertEquals(false, repetir.introVista)
            assertEquals(false, repetir.guiaCronoVista)
        }

    @Test
    fun novedadesVistasGuardaMajorMinorYAusenteEsNull() =
        runBlocking {
            assertEquals(null, repo.observar().first().novedadesVistas)

            repo.marcarNovedadesVistas("1.6.2")
            assertEquals("1.6", repo.observar().first().novedadesVistas)

            repo.marcarNovedadesVistas("basura") // no se entiende: no cambia nada
            assertEquals("1.6", repo.observar().first().novedadesVistas)

            repo.marcarNovedadesVistas("1.10.0")
            assertEquals("1.10", repo.observar().first().novedadesVistas)
        }

    @Test
    fun terminarIntroGuardaIntroYNovedadesJuntas() =
        runBlocking {
            repo.terminarIntro("1.6.2")
            val a = repo.observar().first()
            assertEquals(true, a.introVista)
            assertEquals("1.6", a.novedadesVistas)
        }

    @Test
    fun terminarIntroConVersionIlegibleSoloMarcaLaIntro() =
        runBlocking {
            repo.marcarNovedadesVistas("1.5")
            repo.terminarIntro("basura")
            val a = repo.observar().first()
            assertEquals(true, a.introVista)
            assertEquals("1.5", a.novedadesVistas)
        }

    @Test
    fun sinMarcaDeIntroPeroConOtrosAjustesLaIntroSeMuestraUnaVez() =
        runBlocking {
            repo.fijarTema(TemaApp.OSCURO)
            assertEquals(false, repo.observar().first().introVista)
        }

    @Test
    fun conLaMarcaDeIntroExplicitaNoSeInfiere() =
        runBlocking {
            repo.fijarTema(TemaApp.OSCURO)
            repo.marcarIntroVista(false)
            assertEquals(false, repo.observar().first().introVista)
        }

    @Test
    fun fijarDisenoYOrdenDeLaLista() =
        runBlocking {
            repo.fijarLista(DisposicionLista.FRANJA)
            repo.fijarOrden(OrdenRecetas.RAPIDA)
            val a = repo.observar().first()
            assertEquals(DisposicionLista.FRANJA, a.lista)
            assertEquals(OrdenRecetas.RAPIDA, a.orden)
            assertEquals(DisposicionCrono.APILADA, a.disposicion)
        }

    @Test
    fun temaHoraDeServirYHoraDeLasTarjetas() =
        runBlocking {
            assertEquals(TemaApp.AUTOMATICO, repo.observar().first().tema)
            assertEquals(15 * 60 + 30, repo.observar().first().servirMin)
            repo.fijarTema(TemaApp.OSCURO)
            repo.fijarServir(14 * 60 + 15)
            repo.fijarHoraTarjeta(HoraTarjeta.ACABAR)
            val a = repo.observar().first()
            assertEquals(TemaApp.OSCURO, a.tema)
            assertEquals(14 * 60 + 15, a.servirMin)
            assertEquals(HoraTarjeta.ACABAR, a.horaTarjeta)
            assertEquals("14:15", db.metaDao().valor(AjustesRepoRoom.CLAVE_SERVIR))
        }

    @Test
    fun horaDeCenarPorDefectoLas2100SeGuardaYUnValorRotoVuelveAlDefecto() =
        runBlocking {
            assertEquals(21 * 60, repo.observar().first().cenarMin)
            repo.fijarCenar(22 * 60 + 30)
            assertEquals(22 * 60 + 30, repo.observar().first().cenarMin)
            assertEquals("22:30", db.metaDao().valor(AjustesRepoRoom.CLAVE_CENAR))
            assertEquals(15 * 60 + 30, repo.observar().first().servirMin)
            db.metaDao().fijar(MetaEntity(AjustesRepoRoom.CLAVE_CENAR, "25:99"))
            assertEquals(21 * 60, repo.observar().first().cenarMin)
        }

    private companion object {
        const val RONDAS = 40
        const val ESPERA_MS = 5_000L
        const val PAUSA_MS = 10L
    }
}
