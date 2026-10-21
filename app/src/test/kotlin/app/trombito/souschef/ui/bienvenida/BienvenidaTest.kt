package app.trombito.souschef.ui.bienvenida

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.domain.TemaApp
import app.trombito.souschef.domain.copia.ArchivoCopia
import app.trombito.souschef.domain.copia.GestorCopia
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.receta.leerReceta
import app.trombito.souschef.ui.FakeHistorialRepo
import app.trombito.souschef.ui.FakePreferenciasRepo
import app.trombito.souschef.ui.FakeRecetasRepo
import app.trombito.souschef.ui.ajustes.AjustesAcerca
import app.trombito.souschef.ui.ajustes.AjustesViewModel
import app.trombito.souschef.ui.ajustes.botonConDescripcion
import app.trombito.souschef.ui.comun.EstadoPermisos
import app.trombito.souschef.ui.comun.PermisoApp
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.crono.FakeAjustesRepo
import app.trombito.souschef.ui.crono.LimpiezaCompose
import app.trombito.souschef.ui.crono.RelojFalso
import app.trombito.souschef.ui.theme.SousChefTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Receta real del repositorio (`data/recetas`); los tests de JVM corren con `app/` como directorio de trabajo. */
internal fun recetaReal(slug: String): Receta {
    val archivo = listOf("../data/recetas/$slug.json", "data/recetas/$slug.json").map(::File).first { it.exists() }
    return leerReceta(archivo.readText())
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BienvenidaTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val banco = BancoCompose()
    private val repo = FakeAjustesRepo()
    private val escucha = EscuchaGuion()
    private val habla = HablaGuion()
    private val pollo = recetaReal(SLUG_COCINA)
    private val tacos = recetaReal(SLUG_RACIONES)

    private fun ajustesVm() =
        AjustesViewModel(
            repo,
            GestorCopia(
                FakeHistorialRepo(),
                FakePreferenciasRepo(),
                object : ArchivoCopia {
                    override suspend fun escribir(
                        destino: String,
                        texto: String,
                    ) = true

                    override suspend fun leer(origen: String): String? = null
                },
                RelojFalso(),
            ),
        )

    private fun demoVm() = BienvenidaViewModel(FakeRecetasRepo(listOf(pollo, tacos)), escucha, habla)

    /**
     * La intro sobre [pagina] con los ajustes de ahora. Las elecciones se guardan con el [AjustesViewModel] (como en la
     * app); el efecto en pantalla tras un toque no se comprueba aquí: en Robolectric el contenido no siempre se
     * recompone en los tests que no son el primero de la JVM.
     */
    private fun poner(
        pagina: PaginaIntro,
        permisos: EstadoPermisos = EstadoPermisos(),
    ) {
        val vm = ajustesVm()
        val demo = demoVm().demo.value
        val ajustes = repo.actual.value
        banco.poner {
            SousChefTheme(darkTheme = false) {
                Bienvenida(
                    EstadoIntro(ajustes, demo, permisos = permisos, ahoraMin = 10 * 60),
                    AccionesIntro(AccionesAjustes.de(vm)),
                    paginaInicial = pagina.ordinal,
                )
            }
        }
    }

    /** Textos y descripciones de accesibilidad de todo el árbol, nodo a nodo. */
    private fun todoElTexto(): List<String> =
        banco.nodos().flatMap { n ->
            val textos = n.config.getOrNull(SemanticsProperties.Text).orEmpty()
            val descripciones = n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
            textos.map { it.text } + descripciones
        }

    private fun estadoDe(etiqueta: String): String? =
        banco.conEtiqueta(etiqueta).config.getOrNull(SemanticsProperties.StateDescription)

    private fun hay(etiqueta: String) =
        banco.nodos().any { it.config.getOrNull(SemanticsProperties.TestTag) == etiqueta }

    @Test
    fun `saltar marca la intro vista y termina`() {
        // La intro se entiende ya vista (hay ajustes guardados): al entrar tiene que volver a marcarla como no vista.
        repo.actual.value = Ajustes(introVista = true)
        var terminadas = 0
        val vm = ajustesVm()
        val demo = demoVm()
        banco.poner {
            SousChefTheme(darkTheme = false) { BienvenidaScreen({ terminadas++ }, vm, demo) }
        }
        assertFalse("Al entrar la intro se marca como no vista", repo.actual.value.introVista)

        banco.pulsar(banco.conEtiqueta("intro-saltar"))

        assertTrue(repo.actual.value.introVista)
        assertEquals(1, terminadas)
    }

    @Test
    fun `elegir una opcion en mitad de la intro no la da por vista`() {
        var terminadas = 0
        val vm = ajustesVm()
        val demo = demoVm()
        banco.poner {
            SousChefTheme(darkTheme = false) { BienvenidaScreen({ terminadas++ }, vm, demo) }
        }

        vm.activarVoz(false)

        assertFalse(repo.actual.value.introVista)
        assertEquals(0, terminadas)
    }

    @Test
    fun `la primera pagina no tiene atras y ofrece saltar y siguiente`() {
        poner(PaginaIntro.BIENVENIDA)

        assertFalse(hay("intro-atras"))
        assertTrue(hay("intro-saltar"))
        assertTrue(hay("intro-siguiente"))
        assertTrue(hay("intro-trombito"))
        assertTrue("Paso 1 de 12" in todoElTexto())
    }

    @Test
    fun `la ultima pagina ofrece empezar a cocinar y no saltar`() {
        var terminadas = 0
        banco.poner {
            SousChefTheme(darkTheme = false) {
                Bienvenida(
                    EstadoIntro(),
                    AccionesIntro(terminar = { terminadas++ }),
                    paginaInicial = PaginaIntro.LISTO.ordinal,
                )
            }
        }

        assertFalse(hay("intro-saltar"))
        assertTrue("Empezar a cocinar" in todoElTexto())
        banco.pulsar(banco.conEtiqueta("intro-siguiente"))

        assertEquals(1, terminadas)
    }

    @Test
    fun `elegir gas en la pagina de cocina llama a elegirCocina`() {
        poner(PaginaIntro.COCINA)

        banco.pulsar(banco.conEtiqueta("cocina-tipo-GAS"))

        assertEquals(Cocina(TipoCocina.GAS, Mando.ANALOGICO, null), repo.actual.value.cocina)
    }

    @Test
    fun `la pagina de cocina deja elegir fuegos y horno`() {
        poner(PaginaIntro.COCINA)

        banco.pulsar(banco.botonConDescripcion("Un fuego más"))
        banco.pulsar(banco.conEtiqueta("equipo-horno"))

        assertEquals(4, repo.actual.value.fuegos)
        assertFalse(repo.actual.value.horno)
    }

    @Test
    fun `las paginas con controles no se pasan deslizando`() {
        val conControles = PaginaIntro.entries.filter { it.conControles }

        assertEquals(
            listOf("COCINA", "RACIONES", "HORARIO", "APARIENCIA", "DISPOSICION", "VOZ", "PERMISOS"),
            conControles.map { it.name },
        )
    }

    @Test
    fun `la pagina de permisos pinta los tres estados`() {
        val pedidos = mutableListOf<PermisoApp>()
        banco.poner {
            SousChefTheme(darkTheme = false) {
                PaginaPermisos(
                    EstadoPermisos(notificaciones = true, alarmaExacta = false, pantallaCompleta = true),
                    onPermitir = { pedidos += it },
                )
            }
        }

        assertTrue(hay("permiso-ok-NOTIFICACIONES"))
        assertFalse(hay("permiso-pedir-NOTIFICACIONES"))
        assertTrue(hay("permiso-pedir-ALARMA_EXACTA"))
        assertFalse(hay("permiso-ok-ALARMA_EXACTA"))
        assertTrue(hay("permiso-ok-PANTALLA_COMPLETA"))
        assertEquals(1, banco.textos(banco.raiz()).count { it == "Permitir" })
        assertEquals("Concedido", estadoDe("permiso-NOTIFICACIONES"))
        assertEquals("Falta el permiso", estadoDe("permiso-ALARMA_EXACTA"))
        assertEquals("Concedido", estadoDe("permiso-PANTALLA_COMPLETA"))

        banco.pulsar(banco.conEtiqueta("permiso-pedir-ALARMA_EXACTA"))

        assertEquals(listOf(PermisoApp.ALARMA_EXACTA), pedidos)
    }

    @Test
    fun `sin ningun permiso la pagina ofrece pedir los tres`() {
        poner(
            PaginaIntro.PERMISOS,
            EstadoPermisos(notificaciones = false, alarmaExacta = false, pantallaCompleta = false),
        )

        assertEquals(3, banco.etiquetas("permiso-pedir-").size)
        assertEquals(0, banco.etiquetas("permiso-ok-").size)
    }

    @Test
    fun `la pagina de recetas pinta dos tarjetas reales de la lista dentro del marco`() {
        poner(PaginaIntro.RECETAS)

        assertTrue(hay("marco-demo"))
        val texto = todoElTexto()
        assertTrue(texto.toString(), pollo.titulo in texto)
        assertTrue(texto.toString(), tacos.titulo in texto)
    }

    private fun cocinaEnMarco(disposicion: DisposicionCrono) {
        repo.actual.value = Ajustes(disposicion = disposicion)
        poner(PaginaIntro.DISPOSICION)

        val etiquetas = banco.nodos().mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }
        assertTrue("$disposicion tiene marco", "marco-demo" in etiquetas)
        assertTrue(
            "$disposicion pinta los carriles reales: $etiquetas",
            etiquetas.any { it.startsWith("tarjeta-") || it.startsWith("fila-") || it == "crono-clasico" },
        )
    }

    @Test
    fun `la cocina real cabe en el marco apilada`() = cocinaEnMarco(DisposicionCrono.APILADA)

    @Test
    fun `la cocina real cabe en el marco en foco`() = cocinaEnMarco(DisposicionCrono.FOCO)

    @Test
    fun `la cocina real cabe en el marco en clasica`() = cocinaEnMarco(DisposicionCrono.CLASICA)

    @Test
    fun `la cocina de muestra es la misma en cada vistazo`() {
        val a = cocinaDemo(pollo, Cocina.VITRO_9)
        val b = cocinaDemo(pollo, Cocina.VITRO_9)

        assertEquals(a.ahoraMs, b.ahoraMs)
        assertEquals(a.estado, b.estado)
        assertEquals(AHORA_DEMO_MS + 90_000L, a.ahoraMs)
    }

    @Test
    fun `la lista de ingredientes se ajusta al rango de la receta y lo dice`() {
        repo.actual.value = Ajustes(racionesDefecto = 5)
        poner(PaginaIntro.RACIONES)

        val texto = todoElTexto()
        assertTrue(texto.toString(), "Ingredientes de ${tacos.titulo} para 2 raciones" in texto)
        assertTrue(texto.toString(), "Esta receta va de 1 a 2 raciones" in texto)
    }

    @Test
    fun `las raciones por defecto se guardan al momento`() {
        poner(PaginaIntro.RACIONES)

        banco.pulsar(banco.botonConDescripcion("Una ración más"))

        assertEquals(3, repo.actual.value.racionesDefecto)
    }

    @Test
    fun `la tarjeta del horario sale de la hora de servir`() {
        // 15:30 menos los 136 min de la receta de ejemplo.
        poner(PaginaIntro.HORARIO)
        assertTrue(todoElTexto().toString(), todoElTexto().any { "empieza 13:14" in it })
    }

    @Test
    fun `la tarjeta del horario sigue a otra hora de servir`() {
        repo.actual.value = Ajustes(servirMin = 15 * 60 + 45)
        poner(PaginaIntro.HORARIO)
        assertTrue(todoElTexto().toString(), todoElTexto().any { "empieza 13:29" in it })
    }

    @Test
    fun `la tarjeta del horario ensena cuando acabas si empiezas ya`() {
        repo.actual.value = Ajustes(horaTarjeta = HoraTarjeta.ACABAR)
        poner(PaginaIntro.HORARIO)
        // Son las 10:00 en la prueba: 10:00 + 136 min.
        assertTrue(todoElTexto().toString(), todoElTexto().any { "acabas 12:16" in it })
    }

    @Test
    fun `los mandos del horario se guardan al momento`() {
        poner(PaginaIntro.HORARIO)

        banco.pulsar(banco.botonConDescripcion("Servir 15 minutos más tarde"))
        banco.pulsar(banco.conEtiqueta("hora-ACABAR"))

        assertEquals(15 * 60 + 45, repo.actual.value.servirMin)
        assertEquals(HoraTarjeta.ACABAR, repo.actual.value.horaTarjeta)
    }

    @Test
    fun `la pagina de apariencia deja elegir tema y diseno y pinta tarjetas reales`() {
        poner(PaginaIntro.APARIENCIA)

        banco.pulsar(banco.conEtiqueta("tema-OSCURO"))
        banco.pulsar(banco.conEtiqueta("lista-FRANJA"))

        assertEquals(TemaApp.OSCURO, repo.actual.value.tema)
        assertEquals(DisposicionLista.FRANJA, repo.actual.value.lista)
        assertTrue(pollo.titulo in todoElTexto())
        assertTrue(tacos.titulo in todoElTexto())
    }

    @Test
    fun `la pagina de voz muestra lo oido y la orden entendida`() {
        val vm = demoVm()
        vm.probarVoz("Hola")
        escucha.oir("chef que toca")
        banco.poner {
            SousChefTheme(darkTheme = false) {
                PaginaVoz(EstadoIntro(voz = vm.voz.value), onVoz = {}, onProbar = {})
            }
        }

        val textos = banco.textos(banco.conEtiqueta("intro-voz-resultado"))
        assertTrue(textos.toString(), "Has dicho: «chef que toca»" in textos)
        assertTrue(textos.toString(), "He entendido: una pregunta sobre lo que toca" in textos)
    }

    @Test
    fun `la pagina de voz dice cuando no entiende y cuando no hay reconocedor`() {
        banco.poner {
            SousChefTheme(darkTheme = false) {
                PaginaVoz(EstadoIntro(voz = PruebaVoz.Oido("hola", null)), onVoz = {}, onProbar = {})
            }
        }
        assertTrue("No te he entendido" in banco.textos(banco.conEtiqueta("intro-voz-resultado")))

        val otro = BancoCompose()
        otro.poner {
            SousChefTheme(darkTheme = false) {
                PaginaVoz(EstadoIntro(voz = PruebaVoz.SinReconocedor), onVoz = {}, onProbar = {})
            }
        }
        val textos = otro.textos(otro.conEtiqueta("intro-voz-resultado"))
        assertTrue(textos.toString(), textos.single().startsWith("Este móvil no tiene reconocimiento de voz"))
    }

    @Test
    fun `el resumen final recoge lo elegido`() {
        repo.actual.value = Ajustes(racionesDefecto = 4)
        poner(PaginaIntro.LISTO)

        val textos = banco.textos(banco.conEtiqueta("intro-resumen"))
        assertTrue(textos.toString(), "4 raciones" in textos)
        assertTrue(textos.toString(), "Servir a las 15:30 · cuándo empezar" in textos)
    }

    @Test
    fun `Acerca de ofrece ver la intro otra vez`() {
        var abiertas = 0
        banco.poner {
            SousChefTheme(darkTheme = false) { AjustesAcerca("1.4.0", {}, {}, {}, onIntro = { abiertas++ }) }
        }
        assertTrue("Ver la intro" in todoElTexto())

        banco.pulsar(banco.conEtiqueta("ajustes-intro"))

        assertEquals(1, abiertas)
    }
}
