package app.trombito.souschef.data.novedades

import app.trombito.souschef.data.seed.FuenteSemilla
import app.trombito.souschef.domain.LineaNovedad
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class FuenteNovedadesAssetsTest {
    private val valido =
        """
        {"version":"1.6","titulo":"Sous Chef 1.6 ya está en tu cocina",
         "lineas":[{"texto":"Seis recetas nuevas","destino":"recetas"},{"texto":"Sin destino"},{"texto":"Otra"}],"otro":1}
        """.trimIndent()

    private class Fuente(
        val archivos: Map<String, String>,
        val ilegibles: Set<String> = emptySet(),
    ) : FuenteSemilla {
        override fun listar(directorio: String) = archivos.keys.filter { it.startsWith("$directorio/") }.sorted()

        override fun leer(ruta: String): String {
            if (ruta in ilegibles) throw IOException("no se puede leer")
            return archivos.getValue(ruta)
        }
    }

    @Test
    fun `parsea un JSON valido, con destino opcional y claves desconocidas`() {
        val n = FuenteNovedadesAssets.parsear(valido)

        assertEquals("1.6", n?.version)
        assertEquals("Sous Chef 1.6 ya está en tu cocina", n?.titulo)
        assertEquals(
            listOf(LineaNovedad("Seis recetas nuevas", "recetas"), LineaNovedad("Sin destino"), LineaNovedad("Otra")),
            n?.lineas,
        )
    }

    private fun json(
        version: String = "1.6",
        titulo: String = "t",
        textos: List<String> = listOf("a", "b", "c"),
    ) = """{"version":"$version","titulo":"$titulo","lineas":[${textos.joinToString(",") { """{"texto":"$it"}""" }}]}"""

    @Test
    fun `exige de 3 a 5 lineas de 90 caracteres como mucho`() {
        assertNull(FuenteNovedadesAssets.parsear(json(textos = listOf("a", "b"))))
        assertEquals(3, FuenteNovedadesAssets.parsear(json(textos = listOf("a", "b", "c")))?.lineas?.size)
        assertEquals(5, FuenteNovedadesAssets.parsear(json(textos = List(5) { "x" }))?.lineas?.size)
        assertNull(FuenteNovedadesAssets.parsear(json(textos = List(6) { "x" })))
        val justa = FuenteNovedadesAssets.parsear(json(textos = listOf("x".repeat(90), "b", "c")))
        val primera = justa?.lineas?.firstOrNull()
        assertEquals(90, primera?.texto?.length)
        assertNull(FuenteNovedadesAssets.parsear(json(textos = listOf("x".repeat(91), "b", "c"))))
    }

    @Test
    fun `la version debe ser major_minor e igual al nombre del archivo`() {
        assertNull(FuenteNovedadesAssets.parsear(json(version = "1.6.1")))
        assertEquals("1.6", FuenteNovedadesAssets.parsear(json(), "1.6")?.version)
        assertNull(FuenteNovedadesAssets.parsear(json(), "1.5"))
    }

    @Test
    fun `un JSON roto o incompleto da null`() {
        assertNull(FuenteNovedadesAssets.parsear("{ esto no es json"))
        assertNull(FuenteNovedadesAssets.parsear(""))
        assertNull(FuenteNovedadesAssets.parsear("""{"version":"1.6","titulo":"t","lineas":"no"}"""))
        assertNull(FuenteNovedadesAssets.parsear(json(version = "x")))
        assertNull(FuenteNovedadesAssets.parsear(json(titulo = "")))
        assertNull(FuenteNovedadesAssets.parsear("""{"version":"1.6","titulo":"t","lineas":[]}"""))
    }

    @Test
    fun `todas ordena de la mas nueva a la mas vieja e ignora lo roto o ilegible`() =
        runBlocking {
            val fuente =
                Fuente(
                    mapOf(
                        "novedades/1.9.json" to valido.replace("1.6", "1.9"),
                        "novedades/1.10.json" to valido.replace("1.6", "1.10"),
                        "novedades/1.5.json" to valido.replace("1.6", "1.5"),
                        "novedades/1.7.json" to "{ roto",
                        "novedades/1.8.json" to valido.replace("1.6", "1.8"),
                        "novedades/1.4.json" to valido, // la version no coincide con el nombre
                        "novedades/LEEME.txt" to "ignorar",
                    ),
                    ilegibles = setOf("novedades/1.8.json"),
                )

            val r = FuenteNovedadesAssets(fuente).todas()

            assertEquals(listOf("1.10", "1.9", "1.5"), r.map { it.version })
        }

    // Los tests de JVM corren con `app/` como directorio de trabajo; `comprobarNovedades` aplica las mismas reglas.
    @Test
    fun `las novedades empaquetadas en distribution pasan el parseo de la app`() {
        val carpeta = listOf("../distribution/novedades", "distribution/novedades").map(::File).first { it.isDirectory }
        val jsons = carpeta.listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }
        assertTrue("No hay JSON de novedades en ${carpeta.absolutePath}", jsons.isNotEmpty())
        jsons.forEach { f ->
            val n = FuenteNovedadesAssets.parsear(f.readText(Charsets.UTF_8), f.nameWithoutExtension)
            assertNotNull("${f.name} lo rechaza la app (versión, título, 3 a 5 líneas de 90 como mucho)", n)
        }
    }
}
