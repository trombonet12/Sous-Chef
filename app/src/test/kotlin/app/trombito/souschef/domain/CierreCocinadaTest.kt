package app.trombito.souschef.domain

import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tags
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.PasoReal
import app.trombito.souschef.receta.crono.ResumenCrono
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import app.trombito.souschef.domain.PasoReal as PasoRealHistorial

class CierreCocinadaTest {
    private val zona = ZoneId.of("Europe/Madrid")
    private val servidoMs = Instant.parse("2026-03-07T14:30:05Z").toEpochMilli()

    private val receta =
        Receta(
            slug = "ensalada",
            titulo = "Ensalada",
            tags = Tags(base = "pasta", tecnica = listOf("sofrito", "salteado")),
        )

    private val estado =
        EstadoCrono(slug = "ensalada", raciones = 3, servirMs = servidoMs, carriles = emptyMap(), actualizadoMs = 0L)

    private fun resumen(vararg pasos: PasoReal) =
        ResumenCrono(
            duracionRealS = 1_500L,
            minutosReales = 25,
            servidoMs = servidoMs,
            previstoMs = servidoMs,
            pasosReales = pasos.toList(),
            desvios = emptyList(),
            carriles = emptyList(),
        )

    private fun entrada(
        nota: String,
        resumen: ResumenCrono = resumen(),
    ) = entradaDe(receta, estado, resumen, nota, zona)

    @Test
    fun elIdEsSlugMasFechaYHoraDeServirEnLaZona() {
        val e = entrada("")
        assertEquals("ensalada-2026-03-07-153005", e.id)
        assertEquals("2026-03-07", e.fecha)
        assertEquals("ensalada", e.slug)
        assertEquals("Ensalada", e.titulo)
    }

    @Test
    fun copiaBaseTecnicaRacionesYMinutosReales() {
        val e = entrada("")
        assertEquals("pasta", e.base)
        assertEquals(listOf("sofrito", "salteado"), e.tecnica)
        assertEquals(3, e.raciones)
        assertEquals(25, e.minutosReales)
    }

    @Test
    fun laNotaSeRecortaYSeLimitaAMaxNota() {
        val larga = "  " + "a".repeat(MAX_NOTA + 40) + "  "
        assertEquals("a".repeat(MAX_NOTA), entrada(larga).nota)
        assertEquals("poca sal", entrada("  poca sal \n").nota)
    }

    @Test
    fun laNotaEnBlancoSeGuardaComoNula() {
        assertNull(entrada("   \n ").nota)
        assertNull(entrada("").nota)
    }

    @Test
    fun losPasosRealesSeGuardanCanonicosConLaPotenciaEnLlaves() {
        val r = resumen(PasoReal("p1", "Sofreír a fuego {5}", 300), PasoReal("p2", "Reposar", 60))
        val e = entrada("", r)
        assertEquals(
            listOf(PasoRealHistorial("p1", "Sofreír a fuego {5}", 300), PasoRealHistorial("p2", "Reposar", 60)),
            e.pasosReales,
        )
    }
}
