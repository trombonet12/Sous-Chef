package app.trombito.souschef.ui.crono.voz

import app.trombito.souschef.receta.Grupo
import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientesVozTest {
    private fun receta(vararg grupos: Grupo) = Receta(slug = "t", titulo = "T", raciones = 2, grupos = grupos.toList())

    private fun una(vararg items: Ingrediente) = receta(Grupo("MESA · TODO", items.toList()))

    private fun cantidad(
        r: Receta,
        consulta: String,
        raciones: Int = 2,
    ) = IngredientesVoz.cantidad(r, raciones, consulta)

    private fun paso(
        t: String,
        meto: String? = null,
        nota: String? = null,
    ) = Paso(id = "p", t = t, carril = "c", off = 0.0, min = 1.0, meto = meto, nota = nota)

    private fun delPaso(
        r: Receta,
        p: Paso?,
        raciones: Int = 2,
    ) = IngredientesVoz.delPaso(r, raciones, p)

    // --- cuánto de X ---

    @Test
    fun `una cantidad con su equivalencia casera`() {
        val r = una(Ingrediente("sal (una pizca)", a = 4.0, u = "g"))
        assertEquals("Sal: 4 g, una pizca", cantidad(r, "sal"))
    }

    @Test
    fun `la cantidad se escala a las raciones y la equivalencia se calla`() {
        val r = una(Ingrediente("sal (una pizca)", a = 4.0, u = "g"))
        assertEquals("Sal: 8 g", cantidad(r, "sal", raciones = 4))
    }

    @Test
    fun `el escalado sublineal y el fijo se respetan y sin asterisco`() {
        val sal = una(Ingrediente("sal", a = 4.0, u = "g", esc = "sub"))
        assertEquals("Sal: 6,7 g", cantidad(sal, "sal", raciones = 4))
        val aceite = una(Ingrediente("aceite de girasol, para freír", a = 100.0, u = "ml", esc = "fijo"))
        assertEquals("Aceite de girasol: 100 ml", cantidad(aceite, "aceite", raciones = 4))
    }

    @Test
    fun `un ingrediente sin cantidad numerica dice su texto o que no tiene cantidad fija`() {
        assertEquals("Sal: al gusto", cantidad(una(Ingrediente("sal", txt = "al gusto")), "sal"))
        assertEquals("Sal: sin cantidad fija", cantidad(una(Ingrediente("sal")), "sal"))
    }

    @Test
    fun `sin tildes por palabra y en singular o plural`() {
        val r =
            una(
                Ingrediente("limón (1 pieza)", a = 100.0, u = "g"),
                Ingrediente("pimientos rojos, en tiras", a = 200.0, u = "g"),
                Ingrediente("diente de ajo", a = 5.0, u = "g"),
            )
        assertEquals("Limón: 100 g, 1 pieza", cantidad(r, "limones"))
        assertEquals("Limón: 100 g, 1 pieza", cantidad(r, "limon"))
        assertEquals("Pimientos rojos: 200 g", cantidad(r, "pimiento"))
        assertEquals("Pimientos rojos: 200 g", cantidad(r, "pimientos rojos"))
        assertEquals("Diente de ajo: 5 g", cantidad(r, "dientes de ajo"))
    }

    @Test
    fun `sal no es salsa`() {
        val r = una(Ingrediente("salsa de soja", a = 25.0, u = "ml"), Ingrediente("sal", a = 3.0, u = "g"))
        assertEquals("Sal: 3 g", cantidad(r, "sal"))
        assertEquals("Salsa de soja: 25 ml", cantidad(r, "salsa"))
        assertEquals("No encuentro sal en esta receta", cantidad(una(Ingrediente("salsa de soja", a = 25.0)), "sal"))
    }

    @Test
    fun `gana el nombre exacto y luego el mas parecido`() {
        val r =
            una(
                Ingrediente("pimienta negra", a = 1.0, u = "g"),
                Ingrediente("pimienta", a = 2.0, u = "g"),
            )
        assertEquals("Pimienta: 2 g", cantidad(r, "pimienta"))
        assertEquals("Pimienta negra: 1 g", cantidad(r, "pimienta negra"))
    }

    @Test
    fun `una consulta mas larga que el ingrediente tambien casa si no discrepa`() {
        val r = una(Ingrediente("aceite", a = 20.0, u = "ml"))
        assertEquals("Aceite: 20 ml", cantidad(r, "aceite de oliva"))
        val girasol = una(Ingrediente("aceite de girasol", a = 20.0, u = "ml"))
        assertEquals("No encuentro aceite de oliva en esta receta", cantidad(girasol, "aceite de oliva"))
    }

    @Test
    fun `sin coincidencia lo dice`() {
        assertEquals("No encuentro canela en esta receta", cantidad(una(Ingrediente("sal")), "canela"))
        assertEquals("No encuentro canela en esta receta", cantidad(receta(), "canela"))
    }

    @Test
    fun `varias zonas suman y desglosan`() {
        val r =
            receta(
                Grupo("SARTÉN · EL POLLO", listOf(Ingrediente("aceite de girasol, para sellar", a = 30.0, u = "ml"))),
                Grupo("SARTÉN · LA SALSA", listOf(Ingrediente("aceite de girasol, para la salsa", a = 10.0, u = "ml"))),
            )
        assertEquals(
            "Aceite de girasol: 40 ml en total, 30 ml en el pollo y 10 ml en la salsa",
            cantidad(r, "aceite"),
        )
    }

    @Test
    fun `varias zonas con unidades distintas no suman`() {
        val r =
            receta(
                Grupo("MESA · EL POLLO", listOf(Ingrediente("maicena", a = 15.0, u = "g"))),
                Grupo("OLLA · EL ARROZ", listOf(Ingrediente("maicena", a = 2.0, u = "cucharadas"))),
            )
        assertEquals("Maicena: 15 g en el pollo y 2 cucharadas en el arroz", cantidad(r, "maicena"))
    }

    @Test
    fun `la respuesta cabe en 120 caracteres`() {
        val zonas =
            (1..8).map {
                Grupo("OLLA · LA ZONA NÚMERO MUY LARGA $it", listOf(Ingrediente("sal", a = it.toDouble(), u = "g")))
            }
        val frase = cantidad(receta(*zonas.toTypedArray()), "sal")
        assertTrue(frase, frase.length <= 120)
        assertTrue(frase, frase.startsWith("Sal: 36 g en total"))
    }

    // --- ingredientes de este paso ---

    @Test
    fun `los ingredientes que nombra el texto del paso, escalados`() {
        val r =
            una(
                Ingrediente("ajo picado de 1 mm, para la salsa (2 dientes)", a = 8.0, u = "g"),
                Ingrediente("cebolla", a = 100.0, u = "g"),
                Ingrediente("sal", a = 3.0, u = "g"),
            )
        assertEquals("Lleva ajo picado 8 g y cebolla 100 g.", delPaso(r, paso("Sofreír el ajo y la cebolla")))
        assertEquals("Lleva ajo picado 16 g y cebolla 200 g.", delPaso(r, paso("Sofreír el ajo y la cebolla"), 4))
    }

    @Test
    fun `plural tildes y los otros campos del paso`() {
        val r =
            una(
                Ingrediente("zanahoria, en bastones (1 grande)", a = 150.0, u = "g"),
                Ingrediente("calabacín", a = 200.0, u = "g"),
                Ingrediente("sal", a = 3.0, u = "g"),
            )
        val p = paso("Añade las zanahorias", meto = "calabacin", nota = "Con sal al final")
        assertEquals("Lleva zanahoria 150 g, calabacín 200 g y sal 3 g.", delPaso(r, p))
    }

    @Test
    fun `sal no casa en salsa y un paso sin ingredientes lo dice`() {
        val r = una(Ingrediente("sal", a = 3.0, u = "g"))
        assertEquals("Este paso no nombra ingredientes.", delPaso(r, paso("Añade la salsa")))
    }

    @Test
    fun `un recipiente terminado no tiene paso`() {
        assertEquals("Este recipiente ya ha terminado.", delPaso(una(Ingrediente("sal")), null))
    }

    @Test
    fun `un ingrediente repetido en varias zonas va sin cantidad`() {
        val r =
            receta(
                Grupo("A · UNO", listOf(Ingrediente("aceite de girasol", a = 30.0, u = "ml"))),
                Grupo("B · DOS", listOf(Ingrediente("aceite de girasol", a = 10.0, u = "ml"))),
            )
        assertEquals("Lleva aceite de girasol.", delPaso(r, paso("Calienta el aceite")))
    }

    @Test
    fun `un ingrediente de texto lleva su texto`() {
        val r = una(Ingrediente("sal", txt = "al gusto"))
        assertEquals("Lleva sal al gusto.", delPaso(r, paso("Echa sal")))
    }

    @Test
    fun `dos ingredientes con el mismo nucleo se distinguen por el resto del nombre`() {
        val r =
            una(
                Ingrediente("aceite de girasol", a = 30.0, u = "ml"),
                Ingrediente("aceite de oliva", a = 10.0, u = "ml"),
            )
        assertEquals("Lleva aceite de oliva 10 ml.", delPaso(r, paso("Aliña con aceite de oliva")))
        assertEquals("Lleva aceite de girasol 30 ml y aceite de oliva 10 ml.", delPaso(r, paso("Calienta el aceite")))
    }

    @Test
    fun `una lista larga se trunca a 120 caracteres con y N mas`() {
        val nombres =
            listOf(
                "albahaca",
                "berenjena",
                "calabacin",
                "dátiles",
                "espinacas",
                "fresas",
                "granada",
                "habas",
                "judias",
                "kiwi",
                "lechuga",
                "manzana",
            )
        val r = una(*nombres.map { Ingrediente(it, a = 100.0, u = "g") }.toTypedArray())
        val frase = delPaso(r, paso(nombres.joinToString(" ")))
        assertTrue(frase, frase.length <= 120)
        assertTrue(frase, Regex(" y \\d+ más\\.$").containsMatchIn(frase))
        assertTrue(frase, frase.startsWith("Lleva albahaca 100 g, berenjena 100 g"))
    }
}
