package app.trombito.souschef.receta.crono

import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import kotlin.test.Test
import kotlin.test.assertEquals

/** Los textos de «Toca empezar» y «hecho» llevan lo que se mete y lo que se saca (ADR-0017). */
class AvisosOrdenTest {
    private val servir = 1_700_000_000_000L

    private fun receta(vararg pasos: Paso) =
        Receta(
            slug = "s",
            titulo = "S",
            totalMin = 10.0,
            carriles = listOf(Carril("a", "A", "#fff", "fuego")),
            pasos = pasos.toList(),
        )

    private fun paso(
        meto: String? = null,
        saco: String? = null,
    ) = Paso(id = "x", t = "Baja al {3}", carril = "a", off = 0.0, min = 1.0, s = 60, meto = meto, saco = saco)

    @Test
    fun `toca empezar anade lo que se mete`() {
        val r = receta(paso(meto = "la fuente: 500 g de patata"))
        val e = SesionCrono.inicial(r, servir, 0L)
        val avisos = AvisosCrono.vencidos(e, r, servir).avisos
        assertEquals(
            listOf("Toca empezar: Baja al {3} · mete 500 gramos de patata"),
            avisos.filter { it.tipo == TipoAviso.TOCA_EMPEZAR }.map { it.texto },
        )
    }

    @Test
    fun `toca empezar sin meto queda como antes`() {
        val r = receta(paso())
        val e = SesionCrono.inicial(r, servir, 0L)
        assertEquals(
            listOf("Toca empezar: Baja al {3}"),
            AvisosCrono.vencidos(e, r, servir).avisos.map { it.texto },
        )
    }

    @Test
    fun `hecho anade lo que se saca`() {
        val r = receta(paso(saco = "al plato con papel"))
        val e = MotorCrono.empezar(SesionCrono.inicial(r, servir, 0L), r, "a", servir)
        assertEquals(
            listOf("Baja al {3}: hecho · saca al plato con papel"),
            AvisosCrono.vencidos(e, r, servir + 60_000L).avisos.map { it.texto },
        )
    }

    @Test
    fun `hecho sin saco queda como antes`() {
        val r = receta(paso())
        val e = MotorCrono.empezar(SesionCrono.inicial(r, servir, 0L), r, "a", servir)
        assertEquals(
            listOf("Baja al {3}: hecho"),
            AvisosCrono.vencidos(e, r, servir + 60_000L).avisos.map { it.texto },
        )
    }
}
