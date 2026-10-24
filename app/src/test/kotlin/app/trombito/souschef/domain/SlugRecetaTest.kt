package app.trombito.souschef.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlugRecetaTest {
    @Test
    fun `acepta slugs con minusculas, digitos y guiones`() {
        assertTrue(SlugReceta.valido("arroz-con-pollo"))
        assertTrue(SlugReceta.valido("a"))
        assertTrue(SlugReceta.valido("receta-2"))
        assertTrue(SlugReceta.valido("a".repeat(64)))
    }

    @Test
    fun `rechaza nulo, vacio y demasiado largo`() {
        assertFalse(SlugReceta.valido(null))
        assertFalse(SlugReceta.valido(""))
        assertFalse(SlugReceta.valido("a".repeat(65)))
    }

    @Test
    fun `rechaza mayusculas, separadores y caracteres raros`() {
        assertFalse(SlugReceta.valido("Arroz"))
        assertFalse(SlugReceta.valido("../etc"))
        assertFalse(SlugReceta.valido("a/b"))
        assertFalse(SlugReceta.valido("a b"))
        assertFalse(SlugReceta.valido("a_b"))
        assertFalse(SlugReceta.valido("a?x=1"))
        assertFalse(SlugReceta.valido("ñandú"))
    }

    @Test
    fun `rechaza un salto de linea final`() {
        assertFalse(SlugReceta.valido("arroz\n"))
    }
}
