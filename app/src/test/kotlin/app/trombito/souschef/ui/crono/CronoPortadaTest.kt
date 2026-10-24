package app.trombito.souschef.ui.crono

import androidx.compose.material3.MaterialTheme
import app.trombito.souschef.domain.NotaCocinada
import app.trombito.souschef.receta.BloqueMep
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.receta.conCocina
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.SesionCrono
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CronoPortadaTest {
    @get:Rule
    val limpieza = LimpiezaCompose()

    private val canonica =
        recetaCrono().copy(
            mep =
                listOf(
                    BloqueMep("Antes de encender", l = listOf("Saca el aceite", "Calienta la sartén al {7}")),
                    BloqueMep("Mesa", l = listOf("Pica la cebolla")),
                ),
        )

    private val gas = Cocina(TipoCocina.GAS, Mando.ANALOGICO, null)
    private val digital12 = Cocina(TipoCocina.INDUCCION, Mando.DIGITAL, 12)

    private fun bloques(c: Cocina) = bloquesPortada(canonica, canonica.conCocina(c), 2)

    @Test
    fun losIdsDeLasCasillasSonLosMismosEnCualquierCocinaYLosDeAntesDelMarcado() {
        val vitro = bloques(Cocina.VITRO_9).flatMap { b -> b.casillas.map { it.id } }
        assertEquals(vitro, bloques(gas).flatMap { b -> b.casillas.map { it.id } })
        assertEquals(vitro, bloques(digital12).flatMap { b -> b.casillas.map { it.id } })
        // Es el id que tenía la línea antes de que la potencia se marcara con llaves.
        assertEquals(SesionCrono.idCasilla("Calienta la sartén al 7"), vitro[1])
    }

    @Test
    fun elTextoSeTraduceAlaCocinaPeroLaCasillaMarcadaSigueMarcada() {
        val marcada = bloques(Cocina.VITRO_9)[0].casillas[1].id
        val estado =
            EstadoCrono("r", 2, 0L, emptyMap(), marcados = setOf(marcada), actualizadoMs = 0L)
        val textos = listOf(Cocina.VITRO_9, gas, digital12).map { c -> bloques(c)[0].casillas[1] }
        assertEquals(
            listOf("Calienta la sartén al 7", "Calienta la sartén a fuego medio-alto", "Calienta la sartén al 9"),
            textos.map { it.texto },
        )
        assertTrue(textos.all { it.id in estado.marcados })
    }

    @Test
    fun sinLlavesElTextoQuedaIgualYLosBloquesConservanSuTitulo() {
        val b = bloques(gas)
        assertEquals(listOf("Antes de encender", "Mesa"), b.map { it.titulo })
        assertEquals("Pica la cebolla", b[1].casillas.single().texto)
    }

    private fun pintar(notas: List<NotaCocinada>): BancoCompose {
        val estado = EstadoCrono("r", 2, 0L, emptyMap(), actualizadoMs = 0L)
        val acciones = AccionesPortada({ _, _ -> }, { _, _ -> }, {}, {}, {})
        val banco = BancoCompose()
        banco.poner { MaterialTheme { PortadaCuerpo(canonica, canonica, estado, notas, acciones) } }
        return banco
    }

    @Test
    fun laPortadaMuestraLaUltimaVezSiHayNotas() {
        val banco = pintar(listOf(NotaCocinada("2026-10-12", "Menos sal")))
        assertTrue("ultima-vez" in banco.etiquetas("ultima-vez"))
        assertTrue(banco.textos(banco.raiz()).contains("Menos sal"))
    }

    @Test
    fun laPortadaNoMuestraLaUltimaVezSinNotas() {
        assertFalse("ultima-vez" in pintar(emptyList()).etiquetas("ultima-vez"))
    }
}
