package app.souschef.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DomainDependencyRuleTest {
    private val root: File = File("src/main/kotlin/app/souschef")

    private data class Source(
        val relPath: String,
        val imports: List<String>,
    )

    private fun scan(): List<Source> {
        val files = root.walkTopDown().filter { it.isFile && it.extension == "kt" }
        return files
            .map { f ->
                val lines = f.readLines().map { it.trim() }
                Source(
                    relPath = f.relativeTo(root).invariantSeparatorsPath,
                    imports = lines.filter { it.startsWith("import ") }.map { it.removePrefix("import ").trim() },
                )
            }.toList()
    }

    private fun violations(
        under: String?,
        forbidden: List<String>,
        exceptUnder: List<String> = emptyList(),
    ): List<String> =
        scan()
            .filter { under == null || it.relPath.startsWith("$under/") }
            .filter { s -> exceptUnder.none { s.relPath.startsWith("$it/") } }
            .flatMap { s ->
                s.imports
                    .filter { imp -> forbidden.any { imp.startsWith(it) } }
                    .map { imp -> "${s.relPath}: import $imp" }
            }

    private fun assertNone(violations: List<String>) {
        assertTrue("Violaciones de dependencia:\n" + violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun scanFindsSources() {
        val all = scan()
        assertTrue("El escaneo no encontró archivos en ${root.absolutePath}", all.isNotEmpty())
        assertTrue("No hay archivos en domain/", all.any { it.relPath.startsWith("domain/") })
    }

    @Test
    fun domainHasNoFrameworkImports() {
        val forbidden =
            listOf(
                "android.",
                "androidx.",
                "io.github.jan.supabase",
                "io.ktor",
                "dagger",
                "javax.inject",
            )
        assertNone(violations("domain", forbidden))
    }

    @Test
    fun domainDoesNotImportDataNorUi() {
        assertNone(violations("domain", listOf("app.souschef.data", "app.souschef.ui")))
    }

    @Test
    fun uiHasNoSupabaseImports() {
        assertNone(violations("ui", listOf("io.github.jan.supabase", "io.ktor")))
    }
}
