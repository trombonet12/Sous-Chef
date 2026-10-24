package app.trombito.souschef.tools

import app.trombito.souschef.receta.CamposDeTexto
import app.trombito.souschef.receta.Despensa
import app.trombito.souschef.receta.Hallazgo
import app.trombito.souschef.receta.JsonCanonico
import app.trombito.souschef.receta.MarcaPotencia
import app.trombito.souschef.receta.Nivel
import app.trombito.souschef.receta.Validador
import app.trombito.souschef.receta.leerReceta
import app.trombito.souschef.receta.leerTecnica
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.system.exitProcess

private const val SALIDA_FALLO = 1
private const val SALIDA_USO = 2

fun main(args: Array<String>) {
    val resto = args.drop(1)
    val codigo =
        when (args.firstOrNull()) {
            "validar" -> validar(resto)
            "formatear" -> formatear(resto)
            "potencias" -> potencias(resto)
            "historial" -> historial(resto, ::raiz)
            else -> uso()
        }
    exitProcess(codigo)
}

/** Raíz del repo: el cwd o su ancestro más cercano que contenga `data/recetas`. */
private fun raiz(): File? =
    generateSequence(File("").absoluteFile) { it.parentFile }
        .firstOrNull { File(it, "data/recetas").isDirectory }

private fun jsons(dir: File): List<File> = dir.listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }

private fun ilegible(
    f: File,
    e: Throwable,
) = Hallazgo(f.nameWithoutExtension, 0, Nivel.ERROR, "JSON ilegible: ${e.message?.lineSequence()?.first()}")

private fun validar(args: List<String>): Int {
    val estricto = "--estricto" in args
    val slugs = args.filterNot { it.startsWith("--") }.toSet()
    val raiz = raiz() ?: return errorEntorno()

    fun elegidos(dir: String) = jsons(File(raiz, dir)).filter { slugs.isEmpty() || it.nameWithoutExtension in slugs }
    val tecnicasExistentes = jsons(File(raiz, "data/tecnicas")).map { it.nameWithoutExtension }.toSet()
    val semilla = File(raiz, "data/despensa-semilla.md")
    val despensa = if (semilla.isFile) Despensa.parsearSemilla(semilla.readText()) else emptySet()
    println("despensa: semilla data/despensa-semilla.md (${despensa.size} entradas; sin Supabase)")
    val hallazgos = mutableListOf<Hallazgo>()
    for (f in elegidos("data/recetas")) {
        runCatching { leerReceta(f.readText()) }
            .onSuccess { hallazgos += Validador.receta(it, tecnicasExistentes) + Despensa.comprobar(it, despensa) }
            .onFailure { hallazgos += ilegible(f, it) }
    }
    for (f in elegidos("data/tecnicas")) {
        runCatching { leerTecnica(f.readText()) }
            .onSuccess { hallazgos += Validador.tecnica(it) }
            .onFailure { hallazgos += ilegible(f, it) }
    }
    hallazgos.forEach { println("%s  R%d  %s  %s".format(it.slug, it.regla, it.nivel, it.texto)) }
    val errores = hallazgos.count { it.nivel == Nivel.ERROR }
    val avisos = hallazgos.size - errores
    println("validar: $errores errores, $avisos avisos${if (estricto) " (estricto)" else ""}")
    return if (errores > 0 || (estricto && avisos > 0)) SALIDA_FALLO else 0
}

private fun formatear(args: List<String>): Int {
    val comprobar = "--comprobar" in args
    val slugs = args.filterNot { it.startsWith("--") }.toSet()
    val raiz = raiz() ?: return errorEntorno()
    var cambiados = 0
    for (dir in listOf("data/recetas", "data/tecnicas")) {
        for (f in jsons(File(raiz, dir)).filter { slugs.isEmpty() || it.nameWithoutExtension in slugs }) {
            val antes = f.readText()
            val despues = JsonCanonico.formatear(antes)
            if (antes == despues) continue
            cambiados++
            println(if (comprobar) "cambiaría: $dir/${f.name}" else "formateado: $dir/${f.name}")
            if (!comprobar) f.writeText(despues)
        }
    }
    println("formatear: $cambiados archivos ${if (comprobar) "por formatear" else "reescritos"}")
    return if (comprobar && cambiados > 0) SALIDA_FALLO else 0
}

/** `sc potencias [--marcar] [slug…]`: lista (o marca como `{N}`) los números de potencia del texto libre. */
private fun potencias(args: List<String>): Int {
    System.setOut(PrintStream(FileOutputStream(FileDescriptor.out), true, "UTF-8")) // el contexto lleva acentos
    val marcar = "--marcar" in args
    val slugs = args.filterNot { it.startsWith("--") }.toSet()
    val raiz = raiz() ?: return errorEntorno()
    var total = 0
    var dudosos = 0
    var archivos = 0
    for ((dir, esReceta) in listOf("data/recetas" to true, "data/tecnicas" to false)) {
        for (f in jsons(File(raiz, dir)).filter { slugs.isEmpty() || it.nameWithoutExtension in slugs }) {
            val slug = f.nameWithoutExtension
            val doc = Json.parseToJsonElement(f.readText()) as JsonObject
            var cambiosArchivo = 0
            val nuevo =
                CamposDeTexto.transformar(doc, esReceta) { campo, texto ->
                    val r = MarcaPotencia.marcar(texto)
                    val lista = if (marcar) r.cambios else MarcaPotencia.candidatos(texto)
                    lista.forEach { println("%s  %s  «%s»".format(slug, campo, linea(it.contexto))) }
                    r.dudosos.forEach { println("%s  %s  DUDOSO «%s»".format(slug, campo, linea(it.contexto))) }
                    cambiosArchivo += lista.size
                    dudosos += r.dudosos.size
                    if (marcar) r.texto else texto
                }
            total += cambiosArchivo
            if (marcar && cambiosArchivo > 0) {
                f.writeText(JsonCanonico.escribir(nuevo) + "\n")
                archivos++
            }
        }
    }
    val verbo = if (marcar) "marcados" else "candidatos"
    println("potencias: $total $verbo, $dudosos dudosos${if (marcar) ", $archivos archivos reescritos" else ""}")
    return 0
}

private fun linea(s: String) = s.replace('\n', ' ')

private fun errorEntorno(): Int {
    System.err.println("sc: no encuentro data/recetas desde el directorio actual")
    return SALIDA_USO
}

private fun uso(): Int {
    System.err.println("sc: subcomando desconocido o ausente. Ver docs/herramientas.md")
    return SALIDA_USO
}
