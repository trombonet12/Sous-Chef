package app.trombito.souschef.tools

import app.trombito.souschef.receta.PasoMedido
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.desvios
import app.trombito.souschef.receta.formatoDelta
import app.trombito.souschef.receta.leerReceta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.math.abs

private const val FORMATO_COPIA = "sous-chef-historial"
private const val SALIDA_FALLO_HIST = 1
private const val SALIDA_USO_HIST = 2

private val jsonCopia = Json { ignoreUnknownKeys = true }

@Serializable
private data class CopiaLeida(
    val formato: String = "",
    val historial: List<EntradaLeida> = emptyList(),
)

@Serializable
private data class EntradaLeida(
    val slug: String,
    val pasosReales: List<PasoLeido> = emptyList(),
)

@Serializable
private data class PasoLeido(
    val id: String,
    @SerialName("s") val segundos: Int,
)

/**
 * Por cada receta cocinada en la copia (o solo [slug]): una línea de cabecera y, debajo, cada paso con la mediana de su
 * desvío (real menos previsto) y en cuántas cocinadas se midió, de mayor a menor |mediana|.
 * Lanza [IllegalArgumentException] si el texto no es una copia del historial de Sous Chef.
 */
fun informeHistorial(
    copiaJson: String,
    recetas: Map<String, Receta>,
    slug: String? = null,
): List<String> {
    val copia =
        try {
            jsonCopia.decodeFromString(CopiaLeida.serializer(), copiaJson)
        } catch (e: kotlinx.serialization.SerializationException) {
            throw IllegalArgumentException("no es una copia del historial: ${e.message?.lineSequence()?.first()}", e)
        }
    require(copia.formato == FORMATO_COPIA) { "no es una copia del historial de Sous Chef" }
    return copia.historial
        .filter { slug == null || it.slug == slug }
        .groupBy { it.slug }
        .toSortedMap()
        .flatMap { (s, entradas) -> bloque(s, entradas, recetas[s]) }
}

private fun cocinadas(n: Int) = if (n == 1) "1 cocinada" else "$n cocinadas"

private fun bloque(
    slug: String,
    entradas: List<EntradaLeida>,
    receta: Receta?,
): List<String> =
    if (receta == null) {
        listOf("$slug · sin receta en data/recetas · ${cocinadas(entradas.size)}")
    } else {
        listOf("$slug · ${receta.titulo} · ${cocinadas(entradas.size)}") + filasPasos(entradas, receta)
    }

private fun filasPasos(
    entradas: List<EntradaLeida>,
    receta: Receta,
): List<String> {
    val porPaso =
        entradas
            .flatMap { e -> desvios(receta, e.pasosReales.map { PasoMedido(it.id, it.segundos) }) }
            .groupBy({ it.id }, { it.deltaS })
    val filas =
        receta.pasos
            .filter { it.id in porPaso }
            .map { Triple(it.t, mediana(porPaso.getValue(it.id)), porPaso.getValue(it.id).size) }
            .sortedByDescending { abs(it.second) }
    return filas.map { (t, m, n) -> "  ${formatoDelta(m)} · $n× · $t" }.ifEmpty { listOf("  sin tiempos por paso") }
}

/** Mediana entera; con cantidad par, la media de los dos centrales. */
private fun mediana(valores: List<Int>): Int {
    val v = valores.sorted()
    val mitad = v.size / 2
    return if (v.size % 2 == 1) v[mitad] else (v[mitad - 1] + v[mitad]) / 2
}

private const val USO_HISTORIAL = "uso: sc historial <copia.json> [slug]"

/** Mensaje de error de uso o de entorno, o null si los argumentos valen. */
private fun errorHistorial(
    posicionales: List<String>,
    archivo: File?,
    dir: File?,
): String? =
    when {
        posicionales.isEmpty() || posicionales.size > 2 -> USO_HISTORIAL
        archivo?.isFile != true -> "sc: no encuentro ${posicionales[0]}"
        dir == null -> "sc: no encuentro data/recetas desde el directorio actual"
        else -> null
    }

/** `sc historial <copia.json> [slug]`: desvío mediano de cada paso según la copia exportada por la app. */
fun historial(
    args: List<String>,
    raiz: () -> File?,
): Int {
    val posicionales = args.filterNot { it.startsWith("-") }
    val archivo = posicionales.firstOrNull()?.let { File(it) }
    val dir = raiz()?.let { File(it, "data/recetas") }
    val error = errorHistorial(posicionales, archivo, dir)
    return if ("--help" in args || "-h" in args) {
        System.err.println(USO_HISTORIAL)
        0
    } else if (error != null || archivo == null || dir == null) {
        System.err.println(error)
        SALIDA_USO_HIST
    } else {
        imprimirHistorial(archivo, dir, posicionales.getOrNull(1))
    }
}

private fun imprimirHistorial(
    archivo: File,
    dir: File,
    slug: String?,
): Int {
    System.setOut(PrintStream(FileOutputStream(FileDescriptor.out), true, "UTF-8")) // títulos con acentos
    val recetas =
        dir
            .listFiles { f -> f.extension == "json" }
            .orEmpty()
            .mapNotNull { f -> runCatching { leerReceta(f.readText()) }.getOrNull() }
            .associateBy { it.slug }
    return try {
        val lineas = informeHistorial(archivo.readText(), recetas, slug)
        if (lineas.isEmpty()) println("historial: sin cocinadas que mostrar") else lineas.forEach(::println)
        0
    } catch (e: IllegalArgumentException) {
        System.err.println("sc: ${e.message}")
        SALIDA_FALLO_HIST
    }
}
