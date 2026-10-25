package app.trombito.souschef.ui.crono.voz

import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Grupo
import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.MedidaCasera
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta

/**
 * Respuestas de voz sobre los ingredientes de la receta (F12): «cuánto de X» e «ingredientes de este paso». Puro: sin
 * Android, sin reloj. Las cantidades salen de [Escalado], igual que en la ficha y en la hoja de ingredientes.
 */
object IngredientesVoz {
    const val RECIPIENTE_TERMINADO = "Este recipiente ya ha terminado."
    const val PASO_SIN_INGREDIENTES = "Este paso no nombra ingredientes."

    private const val MAX_FRASE = 120

    private class Hallado(
        val zona: Grupo,
        val item: Ingrediente,
        val rango: Int,
    ) {
        val clave: String get() = NombresIngrediente.clave(item.n)
    }

    /**
     * La cantidad escalada del ingrediente que mejor coincide con [consulta] («Sal: 4 g, una pizca»). Con la receta en
     * su base se añade la equivalencia casera; si está en varias zonas, la suma y el desglose por zona.
     */
    fun cantidad(
        receta: Receta,
        raciones: Int,
        consulta: String,
    ): String {
        val pedido = CoincidenciaNombres.tokens(consulta)
        val hallados =
            receta.grupos
                .flatMap { g -> g.items.map { g to it } }
                .mapNotNull { (g, i) ->
                    val nombre = CoincidenciaNombres.tokens(NombresIngrediente.base(i.n))
                    CoincidenciaNombres.rango(pedido, nombre)?.let { Hallado(g, i, it) }
                }
        val mejor = hallados.minByOrNull { it.rango }
        return if (mejor == null) {
            limitar("No encuentro $consulta en esta receta")
        } else {
            val iguales = hallados.filter { it.rango == mejor.rango && it.clave == mejor.clave }
            limitar(respuesta(receta, Escalado.factor(raciones, receta.raciones), iguales))
        }
    }

    private fun respuesta(
        receta: Receta,
        f: Double,
        iguales: List<Hallado>,
    ): String {
        val nombre = nombreHablado(NombresIngrediente.corto(iguales.first().item.n))
        if (iguales.size == 1) return "$nombre: ${unaZona(iguales.single().item, f)}"
        val porZona = iguales.map { "${texto(it.item, f).ifEmpty { "sin cantidad" }} ${zona(it.zona)}" }
        val general = Escalado.listaGeneral(receta.copy(grupos = iguales.map { Grupo(it.zona.n, listOf(it.item)) }))
        val total = general.singleOrNull()?.takeIf { it.a != null }?.let { texto(it, f) }
        return "$nombre: " + total?.let { "$it en total, " }.orEmpty() + unir(porZona)
    }

    /**
     * Los ingredientes que nombra el paso (título, meto, saco, junto y nota) con su cantidad escalada: el modelo de la
     * receta no relaciona pasos con ingredientes, así que se buscan sus nombres en el texto. Cabe en 120 caracteres
     * («y N más» si no entran todos).
     */
    fun delPaso(
        receta: Receta,
        raciones: Int,
        paso: Paso?,
    ): String {
        val lineas = if (paso == null) emptyList() else lineasDelPaso(receta, raciones, paso)
        return when {
            paso == null -> RECIPIENTE_TERMINADO
            lineas.isEmpty() -> PASO_SIN_INGREDIENTES
            else -> ajustar(lineas)
        }
    }

    private fun lineasDelPaso(
        receta: Receta,
        raciones: Int,
        paso: Paso,
    ): List<String> {
        val textoPaso = listOfNotNull(paso.t, paso.meto, paso.saco, paso.junto, paso.nota).joinToString(" ")
        val nombrados = receta.grupos.flatMap { it.items }.filter { NombresIngrediente.nombradoEn(textoPaso, it.n) }
        val f = Escalado.factor(raciones, receta.raciones)
        return NombresIngrediente
            .desempatar(nombrados, CoincidenciaNombres.tokens(textoPaso))
            .groupBy { NombresIngrediente.clave(it.n) }
            .values
            .map { grupo ->
                val nombre = NombresIngrediente.corto(grupo.first().n).lowercase()
                // Repetido en varias zonas, la cantidad del paso es ambigua: solo el nombre.
                val cantidad = if (grupo.size == 1) texto(grupo.single(), f) else ""
                "$nombre $cantidad".trim()
            }
    }

    /** Las líneas que caben en 120 caracteres, con «y N más» si no entran todas. */
    private fun ajustar(lineas: List<String>): String =
        (lineas.size downTo 1)
            .map { armar(lineas.take(it), lineas.size - it) }
            .firstOrNull { it.length <= MAX_FRASE }
            ?: limitar(armar(lineas.take(1), lineas.size - 1))

    private fun armar(
        mostradas: List<String>,
        resto: Int,
    ): String =
        if (resto == 0) {
            "Lleva ${unir(mostradas)}."
        } else {
            "Lleva ${mostradas.joinToString(", ")} y $resto más."
        }

    private fun unir(partes: List<String>): String =
        if (partes.size < 2) {
            partes.joinToString("")
        } else {
            val anteriores = partes.dropLast(1).joinToString(", ")
            "$anteriores y ${partes.last()}"
        }

    /** «4 g, una pizca»: la cantidad y, con la receta en su base, la equivalencia casera (que no se escala). */
    private fun unaZona(
        i: Ingrediente,
        f: Double,
    ): String {
        val cantidad = texto(i, f).ifEmpty { "sin cantidad fija" }
        val equivalencia = if (f == 1.0 && i.a != null && i.txt == null) MedidaCasera.equivalenciaDe(i.n) else null
        return equivalencia?.let { "$cantidad, $it" } ?: cantidad
    }

    /** La cantidad como se dice: sin la marca «*» del escalado sublineal. */
    private fun texto(
        i: Ingrediente,
        f: Double,
    ): String = Escalado.cantidad(i, f).removeSuffix("*").trim()

    /** «SARTÉN · LA SALSA» -> «en la salsa». */
    private fun zona(g: Grupo): String {
        val nombre = g.n.substringAfter('·', g.n)
        return "en ${nombre.trim().lowercase()}"
    }
}
