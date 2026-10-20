package app.trombito.souschef.receta

import kotlinx.serialization.Serializable

/**
 * Receta tal como vive en `data/recetas/<slug>.json` y en `sous_chef.recetas.doc` (docs/cocina/esquema-receta.md).
 * Tolerante: los opcionales tienen valor por defecto porque las recetas viejas no los traen todos.
 */
@Serializable
data class Receta(
    val slug: String,
    val titulo: String,
    val descripcion: String = "",
    val raciones: Int = 2,
    val totalMin: Double = 0.0,
    val datos: List<List<String>> = emptyList(),
    val tags: Tags = Tags(),
    val perecederos: List<String> = emptyList(),
    val carriles: List<Carril> = emptyList(),
    val grupos: List<Grupo> = emptyList(),
    val notaEscala: String? = null,
    val escala: Escala? = null,
    val pasos: List<Paso> = emptyList(),
    val lectura: String? = null,
    val mep: List<BloqueMep> = emptyList(),
    val cortes: List<Corte> = emptyList(),
    val porque: List<List<String>> = emptyList(),
    val pie: String? = null,
    val creada: String? = null,
    val actualizada: String? = null,
) {
    fun carril(id: String): Carril? = carriles.firstOrNull { it.id == id }

    fun paso(id: String): Paso? = pasos.firstOrNull { it.id == id }

    /** `totalMin` si es válido; si no, el final más tardío (como hacía la app vieja). */
    val totalEfectivo: Double
        get() = if (totalMin > 0) totalMin else pasos.maxOfOrNull { it.off + it.min } ?: 0.0
}

/** Raciones permitidas de una receta: `min`, `min + paso`, … `max` (regla 26; fijada tras una revisión culinaria). */
@Serializable
data class Escala(
    val min: Int,
    val max: Int,
    val paso: Int = 1,
) {
    /** `min` ≥ 1, `paso` ≥ 1 y `max` ≥ `min`. */
    val valida: Boolean get() = min >= 1 && paso >= 1 && max >= min
}

@Serializable
data class Tags(
    val base: String? = null,
    val tecnica: List<String> = emptyList(),
    val proteina: String? = null,
)

/** Ingrediente de `grupos`:cantidad numérica (`a` + `u`) o textual (`txt`). */
@Serializable
data class Ingrediente(
    val n: String,
    val a: Double? = null,
    val u: String? = null,
    val txt: String? = null,
    val esc: String? = null,
) {
    /** Sal, especias potentes, levadura: escala con exponente 0,75. */
    val sublineal: Boolean get() = esc == "sub"

    /** Aceite de freír, agua de cocción: la cantidad es la profundidad del recipiente y no cambia con las raciones. */
    val fijo: Boolean get() = esc == "fijo"
}

@Serializable
data class Grupo(
    val n: String,
    val items: List<Ingrediente> = emptyList(),
)

enum class TipoCarril { MESA, FUEGO, HORNO }

@Serializable
data class Carril(
    val id: String,
    val nombre: String = id.uppercase(),
    val color: String = "#B0A899",
    val tipo: String = "mesa",
) {
    val tipoCarril: TipoCarril
        get() =
            when (tipo) {
                "fuego" -> TipoCarril.FUEGO
                "horno" -> TipoCarril.HORNO
                else -> TipoCarril.MESA
            }
}

enum class Holgura { FLEXIBLE, AJUSTADO, CRITICO }

@Serializable
data class Hito(
    val en: Double,
    val t: String = "Aviso",
)

@Serializable
data class Paso(
    val id: String,
    val t: String,
    val carril: String,
    val off: Double,
    val min: Double,
    val s: Int? = null,
    val f: Int? = null,
    val tapa: String? = null,
    val holgura: String? = null,
    val dep: List<String> = emptyList(),
    val junto: String? = null,
    val meto: String? = null,
    val saco: String? = null,
    val nota: String? = null,
    val hitos: List<Hito> = emptyList(),
    val sinHitos: String? = null,
    val p: String? = null,
) {
    /** Duración exacta en segundos: `s` manda; si falta, `min`·60. */
    val segundos: Int get() = s ?: Math.round(min * 60).toInt()

    val fin: Double get() = off + min

    val holguraTipo: Holgura
        get() =
            when (holgura) {
                "flexible" -> Holgura.FLEXIBLE
                "critico" -> Holgura.CRITICO
                else -> Holgura.AJUSTADO
            }

    /** Hitos normalizados como en la app vieja: redondeados, dentro del paso y en orden. */
    val hitosValidos: List<Hito>
        get() =
            hitos
                .map { it.copy(en = Math.round(it.en).toDouble(), t = it.t.ifBlank { "Aviso" }) }
                .filter { it.en > 0 && it.en < segundos }
                .sortedBy { it.en }
}

@Serializable
data class BloqueMep(
    val t: String,
    val orden: Boolean = false,
    val porq: String? = null,
    val l: List<String> = emptyList(),
)

@Serializable
data class Corte(
    val t: String,
    val p: String? = null,
    val svg: String? = null,
)

/** Ficha de técnica (`data/tecnicas/<slug>.json`). */
@Serializable
data class Tecnica(
    val slug: String,
    val titulo: String,
    val resumen: String = "",
    val categoria: String = "",
    val pasos: List<PasoTecnica> = emptyList(),
    val consejos: List<String> = emptyList(),
    val cortes: List<Corte> = emptyList(),
    val pie: String? = null,
    val creada: String? = null,
    val actualizada: String? = null,
)

@Serializable
data class PasoTecnica(
    val t: String,
    val p: String = "",
)
