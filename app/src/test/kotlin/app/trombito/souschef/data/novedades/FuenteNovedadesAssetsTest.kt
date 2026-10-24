package app.trombito.souschef.data.novedades

import app.trombito.souschef.data.seed.FuenteSemilla
import app.trombito.souschef.domain.LineaNovedad
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class FuenteNovedadesAssetsTest {
    private val valido =
        """
        {"version":"1.6","titulo":"Sous Chef 1.6 ya está en tu cocina","destacar":true,
         "lineas":[{"texto":"Seis recetas nuevas","destino":"recetas"},{"texto":"Sin destino"}],"otro":1}
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
        assertEquals(true, n?.destacar)
        assertEquals(listOf(LineaNovedad("Seis recetas nuevas", "recetas"), LineaNovedad("Sin destino")), n?.lineas)
    }

    @Test
    fun `destacar por defecto es false`() {
        val n = FuenteNovedadesAssets.parsear("""{"version":"1.5","titulo":"t","lineas":[{"texto":"a"}]}""")

        assertEquals(false, n?.destacar)
    }

    @Test
    fun `un JSON roto o incompleto da null`() {
        assertNull(FuenteNovedadesAssets.parsear("{ esto no es json"))
        assertNull(FuenteNovedadesAssets.parsear(""))
        assertNull(FuenteNovedadesAssets.parsear("""{"version":"1.6","titulo":"t","lineas":"no"}"""))
        assertNull(FuenteNovedadesAssets.parsear("""{"version":"x","titulo":"t","lineas":[{"texto":"a"}]}"""))
        assertNull(FuenteNovedadesAssets.parsear("""{"version":"1.6","titulo":"","lineas":[{"texto":"a"}]}"""))
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
                        "novedades/1.8.json" to valido,
                        "novedades/LEEME.txt" to "ignorar",
                    ),
                    ilegibles = setOf("novedades/1.8.json"),
                )

            val r = FuenteNovedadesAssets(fuente).todas()

            assertEquals(listOf("1.10", "1.9", "1.5"), r.map { it.version })
        }
}
