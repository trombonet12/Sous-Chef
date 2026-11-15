package app.trombito.souschef.receta

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Mini formato de la prosa (ADR-0017): línea en blanco = párrafo, «- » = lista. */
class BloquesTest {
    @Test
    fun `un texto sin saltos es un solo parrafo`() {
        assertEquals(listOf(Bloque.Parrafo("Aquí la patata se cuece.")), Bloques.analizar("Aquí la patata se cuece."))
    }

    @Test
    fun `una linea en blanco separa parrafos`() {
        assertEquals(
            listOf(Bloque.Parrafo("Primero."), Bloque.Parrafo("Segundo."), Bloque.Parrafo("Tercero.")),
            Bloques.analizar("Primero.\n\nSegundo.\n \n\nTercero."),
        )
    }

    @Test
    fun `un salto simple dentro de un parrafo se une con espacio`() {
        assertEquals(listOf(Bloque.Parrafo("una frase partida")), Bloques.analizar("una frase\npartida"))
    }

    @Test
    fun `las lineas seguidas con guion son una sola lista`() {
        assertEquals(
            listOf(
                Bloque.Parrafo("El porqué."),
                Bloque.Lista(listOf("Despegar a los 2 min.", "Pinchar a los 15 min.")),
                Bloque.Parrafo("Si no, sale blanda."),
            ),
            Bloques.analizar("El porqué.\n\n- Despegar a los 2 min.\n- Pinchar a los 15 min.\n\nSi no, sale blanda."),
        )
    }

    @Test
    fun `una lista pegada a un parrafo lo corta sin linea en blanco`() {
        assertEquals(
            listOf(Bloque.Parrafo("Antes:"), Bloque.Lista(listOf("uno", "dos"))),
            Bloques.analizar("Antes:\n- uno\n- dos"),
        )
    }

    @Test
    fun `una linea en blanco entre elementos separa listas`() {
        assertEquals(
            listOf(Bloque.Lista(listOf("uno")), Bloque.Lista(listOf("dos"))),
            Bloques.analizar("- uno\n\n- dos"),
        )
    }

    @Test
    fun `una linea sin guion tras un elemento sigue ese elemento`() {
        assertEquals(
            listOf(Bloque.Lista(listOf("uno que sigue aquí", "dos"))),
            Bloques.analizar("- uno que\nsigue aquí\n- dos"),
        )
    }

    @Test
    fun `un guion sin espacio no abre lista`() {
        assertEquals(listOf(Bloque.Parrafo("-5 °C y - nada")), Bloques.analizar("-5 °C y - nada"))
    }

    @Test
    fun `entiende saltos de linea de Windows y espacios sobrantes`() {
        assertEquals(
            listOf(Bloque.Parrafo("Uno."), Bloque.Lista(listOf("a", "b"))),
            Bloques.analizar("  Uno.  \r\n\r\n  - a  \r\n- b\r\n"),
        )
    }

    @Test
    fun `la negrita pasa tal cual dentro de los bloques`() {
        assertEquals(
            listOf(Bloque.Parrafo("Con <b>aceite</b>."), Bloque.Lista(listOf("<b>Sal</b> al final"))),
            Bloques.analizar("Con <b>aceite</b>.\n\n- <b>Sal</b> al final"),
        )
    }

    @Test
    fun `un texto vacio no da bloques`() {
        assertTrue(Bloques.analizar("").isEmpty())
        assertTrue(Bloques.analizar("  \n\n ").isEmpty())
    }
}
