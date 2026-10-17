package app.souschef.receta

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Los datos reales de `data/` se leen y el formato canónico no los cambia ni un byte. */
class DatosRealesTest {
    private val data = File("../data")

    private fun archivos(dir: String) =
        File(data, dir).listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }

    @Test
    fun todasLasRecetasSeLeen() {
        val recetas = archivos("recetas").map { leerReceta(it.readText()) }
        assertTrue(recetas.size >= 13, "esperaba al menos 13 recetas, hay ${recetas.size}")
        recetas.forEach { r ->
            assertTrue(r.pasos.isNotEmpty(), "${r.slug} sin pasos")
            assertTrue(r.carriles.isNotEmpty(), "${r.slug} sin carriles")
        }
    }

    @Test
    fun todasLasBarrasDelGanttCaben() {
        archivos("recetas").map { leerReceta(it.readText()) }.forEach { r ->
            r.pasos.forEach { p ->
                val (izq, ancho) = Gantt.barra(p, r.totalEfectivo, anchoMinimo = 0.01)
                assertTrue(izq >= 0f && ancho > 0f && izq + ancho <= 1f + 1e-6f, "${r.slug}/${p.id}: $izq + $ancho")
            }
        }
    }

    @Test
    fun todasLasTecnicasSeLeen() {
        val tecnicas = archivos("tecnicas").map { leerTecnica(it.readText()) }
        assertTrue(tecnicas.size >= 17)
        tecnicas.forEach { assertTrue(it.pasos.isNotEmpty(), "${it.slug} sin pasos") }
    }

    @Test
    fun elFormatoCanonicoEsEstable() {
        (archivos("recetas") + archivos("tecnicas")).forEach { f ->
            val texto = f.readText()
            assertEquals(texto, JsonCanonico.formatear(texto), "${f.name} no queda igual al formatear")
        }
    }

    @Test
    fun elSlugCoincideConElArchivo() {
        archivos("recetas").forEach { f -> assertEquals(f.nameWithoutExtension, leerReceta(f.readText()).slug) }
    }
}
