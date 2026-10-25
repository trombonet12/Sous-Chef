package app.trombito.souschef.ui.comun

import androidx.compose.material3.MaterialTheme
import app.trombito.souschef.domain.NotaCocinada
import app.trombito.souschef.ui.crono.BancoCompose
import app.trombito.souschef.ui.crono.LimpiezaCompose
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** La tarjeta «La última vez» en la JVM: vacía, con una nota y con varias. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-xxhdpi")
class UltimaVezTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val tres =
        listOf(
            NotaCocinada("2026-10-12", "Menos sal"),
            NotaCocinada("2026-09-30", "Más tiempo de horno"),
            NotaCocinada("2026-08-03", "Cebolla más fina"),
        )

    private val hoy = LocalDate.of(2026, 11, 2)

    private fun pintar(notas: List<NotaCocinada>): BancoCompose {
        val banco = BancoCompose()
        banco.poner { MaterialTheme { UltimaVez(notas, ColoresUltimaVez.Papel, hoy = hoy) } }
        return banco
    }

    private fun BancoCompose.todosLosTextos() = textos(raiz())

    @Test
    fun `sin notas no sale nada`() {
        val banco = pintar(emptyList())
        assertTrue(banco.etiquetas("ultima-vez").isEmpty())
        assertTrue(banco.todosLosTextos().isEmpty())
    }

    @Test
    fun `con una nota sale la nota y su fecha sin boton`() {
        val banco = pintar(tres.take(1))
        val textos = banco.todosLosTextos()
        assertTrue(textos.toString(), textos.contains("La última vez"))
        assertTrue(textos.toString(), textos.contains("Menos sal"))
        assertTrue(textos.toString(), textos.contains("hace 3 semanas · 12 oct"))
        assertTrue("ultima-vez" in banco.etiquetas("ultima-vez"))
        assertTrue(banco.etiquetas("ultima-vez-mas").isEmpty())
    }

    @Test
    fun `con tres sale Ver 2 mas y al tocarlo aparecen las otras`() {
        val banco = pintar(tres)
        assertTrue(banco.todosLosTextos().contains("Menos sal"))
        assertFalse(banco.todosLosTextos().contains("Más tiempo de horno"))
        assertTrue(banco.todosLosTextos().contains("Ver 2 más"))

        banco.pulsar(banco.conEtiqueta("ultima-vez-mas"))

        val textos = banco.todosLosTextos()
        assertTrue(textos.toString(), textos.contains("Más tiempo de horno"))
        assertTrue(textos.toString(), textos.contains("hace 1 mes · 30 sep"))
        assertTrue(textos.toString(), textos.contains("Cebolla más fina"))
        assertTrue(textos.toString(), textos.contains("hace 3 meses · 3 ago"))
        assertTrue(textos.toString(), textos.contains("Ver menos"))

        banco.pulsar(banco.conEtiqueta("ultima-vez-mas"))
        assertFalse(banco.todosLosTextos().contains("Cebolla más fina"))
    }
}
