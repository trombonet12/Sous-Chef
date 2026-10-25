package app.trombito.souschef.domain.copia

import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.PasoReal
import app.trombito.souschef.domain.Preferencia
import app.trombito.souschef.domain.Valoracion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CopiaHistorialTest {
    private val curry =
        EntradaHistorial(
            id = "curry-2026-10-01",
            slug = "curry",
            titulo = "Curry",
            fecha = "2026-10-01",
            base = "arroz",
            tecnica = listOf("sofrito"),
            raciones = 2,
            minutosReales = 52,
            nota = "Menos picante",
            pasosReales = listOf(PasoReal("p1", "Sofrito", 610)),
        )
    private val tacos = EntradaHistorial("tacos-2026-10-05", "tacos", "Tacos", "2026-10-05")
    private val slugs = setOf("curry", "tacos")

    @Test
    fun `exportar y leer devuelve lo mismo`() {
        val prefs = listOf(Preferencia("curry", favorito = true, raciones = 4))
        val copia = CopiaHistorial.leer(CopiaHistorial.exportar(listOf(curry, tacos), prefs, "2026-10-18"), slugs)
        assertEquals(Copia(listOf(curry, tacos), prefs), copia)
    }

    @Test
    fun `la copia v2 guarda y lee valoracion, previsto y quiero hacerla`() {
        val valorada = curry.copy(valoracion = Valoracion.NO_REPETIR, previstoMin = 45)
        val prefs = listOf(Preferencia("tacos", quiereHacerla = true))
        val texto = CopiaHistorial.exportar(listOf(valorada), prefs, "2026-10-18")
        assertEquals(2, CopiaHistorial.VERSION)
        assertTrue(""""version": 2""" in texto)
        assertTrue(""""valoracion": "no_repetir"""" in texto)
        assertEquals(Copia(listOf(valorada), prefs), CopiaHistorial.leer(texto, slugs))
    }

    @Test
    fun `una copia v1 se lee con los campos nuevos a null y a false`() {
        val v1 =
            """
            {"formato":"sous-chef-historial","version":1,"fecha":"2026-10-01",
             "historial":[{"id":"curry-2026-10-01","slug":"curry","titulo":"Curry","fecha":"2026-10-01",
               "minutosReales":52,"pasosReales":[{"id":"p1","t":"Sofrito","s":610}]}],
             "preferencias":[{"slug":"curry","favorito":true,"raciones":4}]}
            """.trimIndent()
        val copia = checkNotNull(CopiaHistorial.leer(v1, slugs))
        val e = copia.historial.single()
        assertEquals("curry-2026-10-01", e.id)
        assertNull(e.valoracion)
        assertNull(e.previstoMin)
        assertEquals(listOf(Preferencia("curry", favorito = true, raciones = 4)), copia.preferencias)
        assertEquals(0, copia.descartadas)
    }

    @Test
    fun `una valoracion desconocida o un previsto negativo no tumban la lectura igual`() {
        fun v2(entrada: String) = """{"formato":"sous-chef-historial","version":2,"historial":[$entrada]}"""
        val base = """"id":"x","slug":"curry","titulo":"Curry","fecha":"2026-10-01""""
        val rara = checkNotNull(CopiaHistorial.leer(v2("""{$base,"valoracion":"quizas"}"""), slugs))
        assertNull(rara.historial.single().valoracion)
        val negativo = checkNotNull(CopiaHistorial.leer(v2("""{$base,"previstoMin":-3}"""), slugs))
        assertEquals(Copia(emptyList(), emptyList(), descartadas = 1), negativo)
    }

    @Test
    fun `la copia v2 mantiene los limites de siempre`() {
        val demasiadas = List(5001) { curry.copy(id = "curry-$it", valoracion = Valoracion.REPETIR) }
        assertNull(CopiaHistorial.leer(CopiaHistorial.exportar(demasiadas, emptyList(), "2026-10-18"), slugs))
        val rara = curry.copy(id = "f", slug = "../etc", valoracion = Valoracion.REPETIR)
        val texto = CopiaHistorial.exportar(listOf(rara), emptyList(), "2026-10-18")
        assertEquals(1, checkNotNull(CopiaHistorial.leer(texto, slugs)).descartadas)
    }

    @Test
    fun `una cocinada de una receta retirada del catalogo se conserva pero su preferencia no`() {
        val retirada = curry.copy(id = "retirada-2026-09-01", slug = "retirada", titulo = "Receta retirada")
        val prefs = listOf(Preferencia("retirada", favorito = true), Preferencia("curry", favorito = true))
        val texto = CopiaHistorial.exportar(listOf(retirada, curry), prefs, "2026-10-18")

        val copia = checkNotNull(CopiaHistorial.leer(texto, slugs))

        assertEquals(listOf(retirada, curry), copia.historial)
        assertEquals(listOf(Preferencia("curry", favorito = true)), copia.preferencias)
        assertEquals(1, copia.descartadas)
    }

    @Test
    fun `la valoracion de la copia rellena solo la local que no tiene, y si las dos tienen gana la local`() {
        val local = curry.copy(id = "a", valoracion = null)
        val localValorada = curry.copy(id = "b", valoracion = Valoracion.REPETIR)
        val localSinCopia = curry.copy(id = "c", valoracion = null)
        val copia =
            Copia(
                listOf(
                    curry.copy(id = "a", valoracion = Valoracion.AJUSTAR),
                    curry.copy(id = "a", valoracion = Valoracion.NO_REPETIR), // repetida: vale la primera
                    curry.copy(id = "b", valoracion = Valoracion.NO_REPETIR), // gana la local (REPETIR)
                    curry.copy(id = "d", valoracion = Valoracion.REPETIR), // no existe en local: entra por nuevas()
                    curry.copy(id = "c", valoracion = null), // la copia no tiene: nada que rellenar
                ),
                emptyList(),
            )
        assertEquals(
            mapOf("a" to Valoracion.AJUSTAR),
            CopiaHistorial.valoracionesNuevas(listOf(local, localValorada, localSinCopia), copia),
        )
    }

    @Test
    fun `un quiero hacerla de la copia se suma y nunca quita el actual`() {
        val actual =
            mapOf(
                "curry" to Preferencia("curry", quiereHacerla = true),
                "tacos" to Preferencia("tacos"),
            )
        val copia =
            Copia(
                emptyList(),
                listOf(Preferencia("curry"), Preferencia("tacos", quiereHacerla = true)),
            )
        assertEquals(
            listOf(Preferencia("tacos", quiereHacerla = true)),
            CopiaHistorial.preferenciasNuevas(actual, copia),
        )
    }

    @Test
    fun `un archivo que no es una copia no se lee`() {
        assertNull(CopiaHistorial.leer("no es json", slugs))
        assertNull(CopiaHistorial.leer("""{"formato":"otra-cosa","version":1}""", slugs))
        assertNull(CopiaHistorial.leer("""{"formato":"sous-chef-historial","version":99}""", slugs))
    }

    @Test
    fun `importar solo añade lo que no esta, sin duplicar`() {
        val copia = Copia(listOf(curry, tacos, tacos), emptyList())
        assertEquals(listOf(tacos), CopiaHistorial.nuevas(listOf(curry), copia))
    }

    @Test
    fun `las preferencias suman favoritos y no pisan raciones`() {
        val actual = mapOf("curry" to Preferencia("curry", favorito = false, raciones = 3))
        val copia =
            Copia(
                emptyList(),
                listOf(
                    Preferencia("curry", favorito = true, raciones = 6),
                    Preferencia("tacos", raciones = 4),
                    Preferencia("sopa"),
                ),
            )
        assertEquals(
            listOf(Preferencia("curry", favorito = true, raciones = 3), Preferencia("tacos", raciones = 4)),
            CopiaHistorial.preferenciasNuevas(actual, copia),
        )
    }
}
