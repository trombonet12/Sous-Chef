package app.trombito.souschef.ui.crono

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** El porqué se pinta en párrafos: línea en blanco separa, «- » abre una viñeta. */
class CronoPorqueTest {
    @Test
    fun `un texto sin saltos es un solo parrafo`() {
        assertEquals(
            listOf(BloqueTexto.Parrafo("Aquí la patata se cuece.")),
            bloquesDeTexto("Aquí la patata se cuece."),
        )
    }

    @Test
    fun `una linea en blanco separa parrafos`() {
        val bloques = bloquesDeTexto("Primero.\n\nSegundo.\n \n\nTercero.")
        assertEquals(
            listOf(BloqueTexto.Parrafo("Primero."), BloqueTexto.Parrafo("Segundo."), BloqueTexto.Parrafo("Tercero.")),
            bloques,
        )
    }

    @Test
    fun `las lineas que empiezan por guion son vinetas`() {
        val bloques =
            bloquesDeTexto("El porqué.\n\n- Despegar a los 2 min.\n- Pinchar a los 15 min.\n\nSi no, sale blanda.")
        assertEquals(
            listOf(
                BloqueTexto.Parrafo("El porqué."),
                BloqueTexto.Vineta("Despegar a los 2 min."),
                BloqueTexto.Vineta("Pinchar a los 15 min."),
                BloqueTexto.Parrafo("Si no, sale blanda."),
            ),
            bloques,
        )
    }

    @Test
    fun `un salto simple dentro de un parrafo no lo parte`() {
        assertEquals(listOf(BloqueTexto.Parrafo("una frase partida")), bloquesDeTexto("una frase\npartida"))
    }

    @Test
    fun `un texto vacio no da bloques`() {
        assertTrue(bloquesDeTexto("  \n\n ").isEmpty())
    }
}
