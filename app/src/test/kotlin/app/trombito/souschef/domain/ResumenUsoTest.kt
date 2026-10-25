package app.trombito.souschef.domain

import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.TipoCocina
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class ResumenUsoTest {
    private val hoy = LocalDate.of(2026, 10, 25)

    private fun e(
        fecha: String,
        slug: String = "sopa",
        valoracion: Valoracion? = null,
        nota: String? = null,
    ) = EntradaHistorial(
        id = "$slug-$fecha",
        slug = slug,
        titulo = "Título de $slug",
        fecha = fecha,
        nota = nota,
        valoracion = valoracion,
    )

    private fun resumen(
        entradas: List<EntradaHistorial>,
        quiero: Int = 0,
        ajustes: Ajustes = Ajustes(),
    ) = resumenDeUso(entradas, quiero, ajustes, "1.7.0", hoy)

    @Test
    fun `sin historial todo son ceros y los ajustes llegan tal cual`() {
        val r = resumen(emptyList())
        assertEquals(0, r.cocinadas)
        assertEquals(0, r.cocinadasUltimoMes)
        assertEquals(0, r.recetasDistintas)
        assertEquals(Triple(0, 0, 0), Triple(r.repetir, r.ajustar, r.noRepetir))
        assertEquals("1.7.0", r.version)
        assertEquals(DisposicionCrono.APILADA, r.disposicion)
        assertEquals(true, r.vozActiva)
    }

    @Test
    fun `cuenta cocinadas totales y recetas distintas`() {
        val r = resumen(listOf(e("2026-10-01"), e("2026-09-01"), e("2026-08-01", "tortilla")))
        assertEquals(3, r.cocinadas)
        assertEquals(2, r.recetasDistintas)
    }

    @Test
    fun `el ultimo mes incluye el borde de hace un mes y hoy y deja fuera lo anterior y el futuro`() {
        val r =
            resumen(
                listOf(
                    e("2026-10-25"),
                    e("2026-09-25"),
                    e("2026-09-24"),
                    e("2026-10-26"),
                    e("2026-10-10"),
                    e("2026-10-20T10:30:00"),
                ),
            )
        assertEquals(6, r.cocinadas)
        assertEquals(4, r.cocinadasUltimoMes)
    }

    @Test
    fun `una fecha ilegible cuenta en el total pero no en el mes`() {
        val r = resumen(listOf(e("ayer"), e("2026-10-20")))
        assertEquals(2, r.cocinadas)
        assertEquals(1, r.cocinadasUltimoMes)
    }

    @Test
    fun `cuenta las valoraciones sin contar las que no se valoraron`() {
        val r =
            resumen(
                listOf(
                    e("2026-10-01", valoracion = Valoracion.REPETIR),
                    e("2026-10-02", valoracion = Valoracion.REPETIR),
                    e("2026-10-03", valoracion = Valoracion.AJUSTAR),
                    e("2026-10-04", valoracion = Valoracion.NO_REPETIR),
                    e("2026-10-05"),
                ),
            )
        assertEquals(Triple(2, 1, 1), Triple(r.repetir, r.ajustar, r.noRepetir))
    }

    @Test
    fun `lleva la cocina validada, la disposicion, la voz y las que quiere hacer`() {
        val ajustes =
            Ajustes(
                disposicion = DisposicionCrono.CLASICA,
                vozActiva = false,
                cocina = Cocina(tipo = TipoCocina.GAS, max = null),
            )
        val r = resumen(emptyList(), quiero = 4, ajustes = ajustes)
        assertEquals(DisposicionCrono.CLASICA, r.disposicion)
        assertEquals(TipoCocina.GAS, r.cocina.tipo)
        assertFalse(r.vozActiva)
        assertEquals(4, r.quieroHacerla)
    }

    @Test
    fun `el resumen no guarda notas, titulos ni slugs`() {
        val r =
            resumen(
                listOf(e("2026-10-01", "lasana-secreta", Valoracion.REPETIR, nota = "NOTA-PRIVADA con la suegra")),
            )
        val texto = r.toString()
        assertFalse(texto, texto.contains("NOTA-PRIVADA"))
        assertFalse(texto, texto.contains("lasana-secreta"))
        assertFalse(texto, texto.contains("Título de"))
    }
}
