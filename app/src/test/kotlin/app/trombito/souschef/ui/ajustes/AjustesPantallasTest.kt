package app.trombito.souschef.ui.ajustes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.domain.TemaApp
import app.trombito.souschef.ui.Rutas
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.crono.LimpiezaCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AjustesPantallasTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val banco = BancoCompose()

    @Test
    fun `el menu muestra los resumenes y la version`() {
        banco.poner { MaterialTheme { AjustesMenu(Ajustes(), "1.2.3", {}, {}) } }

        val textos = banco.textos(banco.raiz())
        assertTrue(textos.toString(), "Tema automático · lista Tinte" in textos)
        assertTrue(textos.toString(), "Servir a las 15:30 · cuándo empezar" in textos)
        assertTrue(textos.toString(), "Apilada · voz activada" in textos)
        assertTrue(textos.toString(), "Guardar o cargar una copia" in textos)
        assertTrue(textos.toString(), "Sous Chef 1.2.3" in textos)
    }

    @Test
    fun `el menu tiene la fila Acerca de y el pie con version y autor`() {
        banco.poner { MaterialTheme { AjustesMenu(Ajustes(), "1.3.0", {}, {}) } }

        val textos = banco.textos(banco.raiz())
        assertTrue(textos.toString(), "Acerca de" in textos)
        assertTrue(textos.toString(), "Versión 1.3.0 · contacto" in textos)
        assertTrue(textos.toString(), "Sous Chef 1.3.0" in textos)
        assertTrue(textos.toString(), "Trombonet12© and Co. Interactive Systems Solutions" in textos)
        assertTrue(textos.toString(), "trombonet12@trombonet12.es" !in textos)
        banco.conEtiqueta("ajustes-version")
        banco.conEtiqueta("ajustes-autor")
    }

    @Test
    fun `tocar Acerca de abre su ruta`() {
        val abiertas = mutableListOf<String>()
        banco.poner { MaterialTheme { AjustesMenu(Ajustes(), "1", {}, { abiertas += it }) } }

        banco.pulsar(banco.conEtiqueta("ajustes-${Rutas.AJUSTES_ACERCA}"))

        assertEquals(listOf(Rutas.AJUSTES_ACERCA), abiertas)
    }

    @Test
    fun `acerca muestra version, autor y las dos filas de contacto`() {
        banco.poner { MaterialTheme { AjustesAcerca("1.3.0", {}, {}, {}) } }

        val textos = banco.textos(banco.raiz())
        assertTrue(textos.toString(), "Sous Chef" in textos)
        assertTrue(textos.toString(), "Versión 1.3.0" in textos)
        assertTrue(textos.toString(), "Trombonet12© and Co. Interactive Systems Solutions" in textos)
        assertTrue(textos.toString(), "Contacto" in textos)
        assertTrue(textos.toString(), "Escribir un correo" in textos)
        assertTrue(textos.toString(), "trombonet12@trombonet12.es" in textos)
        assertTrue(textos.toString(), "Visitar la web" in textos)
        assertTrue(textos.toString(), "www.trombonet12.es" in textos)
    }

    @Test
    fun `tocar correo y web en acerca avisa a los callbacks`() {
        val tocados = mutableListOf<String>()
        banco.poner {
            MaterialTheme {
                AjustesAcerca("1", {}, onCorreo = { tocados += "correo" }, onWeb = { tocados += "web" })
            }
        }

        banco.pulsar(banco.conEtiqueta("ajustes-contacto"))
        banco.pulsar(banco.conEtiqueta("ajustes-web"))

        assertEquals(listOf("correo", "web"), tocados)
    }

    @Test
    fun `el resumen del horario cambia con la hora de las tarjetas`() {
        val ajustes = Ajustes(horaTarjeta = HoraTarjeta.ACABAR, servirMin = 14 * 60, vozActiva = false)
        banco.poner { MaterialTheme { AjustesMenu(ajustes, "1", {}, {}) } }

        val textos = banco.textos(banco.raiz())
        assertTrue(textos.toString(), "Servir a las 14:00 · si empiezas ya" in textos)
        assertTrue(textos.toString(), "Apilada · voz desactivada" in textos)
    }

    @Test
    fun `tocar un apartado del menu abre su ruta`() {
        val abiertas = mutableListOf<String>()
        banco.poner { MaterialTheme { AjustesMenu(Ajustes(), "1", {}, { abiertas += it }) } }

        banco.pulsar(banco.conEtiqueta("ajustes-${Rutas.AJUSTES_HORARIO}"))
        banco.pulsar(banco.conEtiqueta("ajustes-${Rutas.AJUSTES_HISTORIAL}"))

        assertEquals(listOf(Rutas.AJUSTES_HORARIO, Rutas.AJUSTES_HISTORIAL), abiertas)
    }

    @Test
    fun `apariencia elige tema y diseno de lista`() {
        val temas = mutableListOf<TemaApp>()
        val listas = mutableListOf<DisposicionLista>()
        banco.poner { MaterialTheme { AjustesApariencia(Ajustes(), {}, { temas += it }, { listas += it }) } }

        assertEquals(listOf("tema-AUTOMATICO", "tema-CLARO", "tema-OSCURO"), banco.etiquetas("tema-"))
        val auto = banco.conEtiqueta("tema-AUTOMATICO").config.getOrNull(SemanticsProperties.Selected)
        assertEquals(true, auto)

        banco.pulsar(banco.conEtiqueta("tema-OSCURO"))
        banco.pulsar(banco.conEtiqueta("lista-CLASICA"))

        assertTrue(banco.textos(banco.raiz()).toString(), "Diseño de las listas" in banco.textos(banco.raiz()))
        assertEquals(listOf(TemaApp.OSCURO), temas)
        assertEquals(listOf(DisposicionLista.CLASICA), listas)
    }

    @Test
    fun `horario ajusta la hora de servir y elige la hora de las tarjetas`() {
        val pasos = mutableListOf<Int>()
        val horas = mutableListOf<HoraTarjeta>()
        banco.poner {
            MaterialTheme { AjustesHorario(Ajustes(), 10 * 60, {}, { pasos += it }, { horas += it }) }
        }

        val textos = banco.textos(banco.raiz())
        assertTrue(textos.toString(), "15:30" in textos)
        assertTrue(textos.toString(), "Empieza a las 14:43 para servir a las 15:30" in textos)
        assertTrue(textos.toString(), "Acabas a las 10:47 (ahora + lo que dura)" in textos)

        banco.pulsar(botonConDescripcion("Servir 15 minutos más tarde"))
        banco.pulsar(botonConDescripcion("Servir 15 minutos antes"))
        banco.pulsar(banco.conEtiqueta("hora-ACABAR"))

        assertEquals(listOf(15, -15), pasos)
        assertEquals(listOf(HoraTarjeta.ACABAR), horas)
    }

    private fun botonConDescripcion(descripcion: String): SemanticsNode {
        fun contiene(n: SemanticsNode): Boolean =
            n.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(descripcion) == true ||
                n.children.any(::contiene)

        fun pulsables(n: SemanticsNode): List<SemanticsNode> =
            listOfNotNull(n.takeIf { it.config.contains(SemanticsActions.OnClick) }) +
                n.children.flatMap(::pulsables)
        return checkNotNull(pulsables(banco.raiz()).firstOrNull(::contiene)) { "No hay botón $descripcion" }
    }
}
