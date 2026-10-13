package app.souschef.receta

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Lectura tolerante de recetas, técnicas e historial: claves desconocidas no rompen la app. */
val RecetaJson: Json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = false
        coerceInputValues = true
    }

fun leerReceta(texto: String): Receta = RecetaJson.decodeFromString(Receta.serializer(), texto)

fun leerTecnica(texto: String): Tecnica = RecetaJson.decodeFromString(Tecnica.serializer(), texto)

/**
 * Formato canónico de los JSON de `data/`: igual que `json.dumps(d, ensure_ascii=False, indent=2, sort_keys=True)`
 * de Python, más `\n` final. Los números se escriben tal como se leyeron (2.5 sigue siendo 2.5, 5 sigue siendo 5),
 * así que formatear un archivo ya canónico no lo cambia.
 */
object JsonCanonico {
    fun formatear(texto: String): String = escribir(Json.parseToJsonElement(texto)) + "\n"

    fun escribir(e: JsonElement): String = StringBuilder().also { escribe(e, it, 0) }.toString()

    private fun escribe(
        e: JsonElement,
        sb: StringBuilder,
        nivel: Int,
    ) {
        when (e) {
            is JsonNull -> sb.append("null")
            is JsonPrimitive -> if (e.isString) cadena(e.content, sb) else sb.append(e.content)
            is JsonArray ->
                if (e.isEmpty()) {
                    sb.append("[]")
                } else {
                    sb.append("[\n")
                    e.forEachIndexed { i, v ->
                        sangra(sb, nivel + 1)
                        escribe(v, sb, nivel + 1)
                        sb.append(if (i < e.size - 1) ",\n" else "\n")
                    }
                    sangra(sb, nivel)
                    sb.append("]")
                }
            is JsonObject ->
                if (e.isEmpty()) {
                    sb.append("{}")
                } else {
                    sb.append("{\n")
                    val claves = e.keys.sorted()
                    claves.forEachIndexed { i, k ->
                        sangra(sb, nivel + 1)
                        cadena(k, sb)
                        sb.append(": ")
                        escribe(e.getValue(k), sb, nivel + 1)
                        sb.append(if (i < claves.size - 1) ",\n" else "\n")
                    }
                    sangra(sb, nivel)
                    sb.append("}")
                }
        }
    }

    private fun sangra(
        sb: StringBuilder,
        nivel: Int,
    ) {
        repeat(nivel * 2) { sb.append(' ') }
    }

    // Escapes de Python con ensure_ascii=False: comillas, barra invertida y controles; el resto literal.
    private fun cadena(
        s: String,
        sb: StringBuilder,
    ) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else ->
                    if (c < ' ' || c == '\u007F') {
                        sb.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                    } else {
                        sb.append(c)
                    }
            }
        }
        sb.append('"')
    }
}
