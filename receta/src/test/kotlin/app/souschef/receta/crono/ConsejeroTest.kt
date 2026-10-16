package app.souschef.receta.crono

import app.souschef.receta.BloqueMep
import app.souschef.receta.Carril
import app.souschef.receta.Paso
import app.souschef.receta.Receta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConsejeroTest {
    private val servir = 1_700_000_000_000L
    private val inicio = servir - 10 * 60_000L

    private fun receta(mep: List<BloqueMep> = emptyList()) =
        Receta(
            slug = "t",
            titulo = "T",
            totalMin = 10.0,
            carriles = listOf(Carril("a", "Sartén", "#fff", "fuego"), Carril("b", "Olla", "#fff", "fuego")),
            mep = mep,
            pasos =
                listOf(
                    Paso(id = "x", t = "Sofreír cebolla, a fuego medio", carril = "a", off = 0.0, min = 5.0, s = 300),
                    Paso(id = "z", t = "Hervir agua", carril = "b", off = 3.0, min = 5.0, s = 300),
                ),
        )

    private fun abierto(
        r: Receta,
        ahora: Long,
    ) = SesionCrono.abrir(SesionCrono.inicial(r, servir, ahora), r, ahora)

    @Test
    fun avisaCuandoFaltaMenosDeUnMinuto() {
        val r = receta()
        val ahora = inicio - 30_000L
        val c = Consejero.consejo(r, abierto(r, ahora), ahora)
        assertEquals(Consejo("En nada toca: Sartén · Sofreír cebolla", TipoConsejo.PRONTO), c)
    }

    @Test
    fun avisaDeOtroCarrilDentroDeCincoMinutos() {
        val r = receta()
        val ahora = inicio - 90_000L
        val e = abierto(r, ahora)
        assertEquals("a", e.foco)
        assertEquals(Consejo("En 5 min: Olla · Hervir agua", TipoConsejo.OTRO_CARRIL), Consejero.consejo(r, e, ahora))
    }

    @Test
    fun sinPrisaSugiereElPrimerMepSinMarcarCortado() {
        val larga = "Pica la <b>cebolla</b>, muy fina " + "x".repeat(80)
        val r = receta(mep = listOf(BloqueMep("Corte", l = listOf(larga, "Lava el arroz"))))
        val ahora = inicio - 10 * 60_000L
        val e = abierto(r, ahora)
        assertEquals(Consejo("Ve adelantando: Pica la cebolla", TipoConsejo.ADELANTA), Consejero.consejo(r, e, ahora))
        val marcado = SesionCrono.marcar(e, SesionCrono.idCasilla(larga), true, ahora)
        assertEquals(
            Consejo("Ve adelantando: Lava el arroz", TipoConsejo.ADELANTA),
            Consejero.consejo(r, marcado, ahora),
        )
    }

    @Test
    fun sinNadaQueDecirDevuelveNull() {
        val r = receta()
        val ahora = inicio - 10 * 60_000L
        assertNull(Consejero.consejo(r, abierto(r, ahora), ahora))
    }
}
