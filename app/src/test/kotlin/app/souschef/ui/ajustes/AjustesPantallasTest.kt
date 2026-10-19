package app.souschef.ui.ajustes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.souschef.domain.Ajustes
import app.souschef.domain.DisposicionLista
import app.souschef.domain.HoraTarjeta
import app.souschef.domain.TemaApp
import app.souschef.ui.Rutas
import app.souschef.ui.crono.BancoCompose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AjustesPantallasTest {
    private val banco = BancoCompose()

    @Test
    fun `el menu muestra los cuatro resumenes y la version`() {
        banco.poner { MaterialTheme { AjustesMenu(Ajustes(), "1.2.3", {}, {}) } }

        val textos = banco.textos(banco.raiz())
        assertTrue(textos.toString(), "Tema automático · lista Tinte" in textos)
        assertTrue(textos.toString(), "Servir a las 15:30 · cuándo empezar" in textos)
        assertTrue(textos.toString(), "Apilada · voz activada" in textos)
        assertTrue(textos.toString(), "Guardar o cargar una copia" in textos)
        assertTrue(textos.toString(), "Sous Chef 1.2.3" in textos)
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
