package app.souschef.receta

import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToLong

/** Escalado de cantidades y formato de números como en la ficha vieja (docs/spec/app-legado.md §2.1). */
object Escalado {
    const val RACIONES_MIN = 1
    const val RACIONES_MAX = 20
    private const val EXPONENTE_SUB = 0.75

    fun factor(
        raciones: Int,
        base: Int,
    ): Double = if (base <= 0) 1.0 else raciones.toDouble() / base

    /** Cantidad escalada y formateada: "250 g", "2,5 ml", "½ cdta *". El asterisco marca escalado sublineal. */
    fun cantidad(
        item: Ingrediente,
        f: Double,
    ): String {
        val marca = if (item.sublineal && f != 1.0) " *" else ""
        item.txt?.let { return it + marca }
        val a = item.a ?: return marca.trim()
        val v = a * if (item.sublineal) f.pow(EXPONENTE_SUB) else f
        val u = item.u?.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
        return numero(v) + u + marca
    }

    /** ≥ 10 → entero; si no, un decimal con coma y sin ",0". */
    fun numero(v: Double): String {
        if (v >= 10) return v.roundToLong().toString()
        val r = (v * 10).roundToLong() / 10.0
        return if (r == Math.floor(r)) r.toLong().toString() else r.toString().replace('.', ',')
    }

    /** Minutos redondeados a 0,5 con coma: 2.5 → "2,5", 3.0 → "3". */
    fun minutos(m: Double): String {
        val r = Math.round(m * 2) / 2.0
        return if (r == Math.floor(r)) r.toLong().toString() else r.toString().replace('.', ',')
    }

    /** Texto plano de "Copiar la lista" (docs/spec/app-legado.md §2). */
    fun textoCompra(
        receta: Receta,
        raciones: Int,
    ): String {
        val f = factor(raciones, receta.raciones)
        val cabecera = "Compra · ${receta.titulo} · " + if (raciones == 1) "1 ración" else "$raciones raciones"
        val lineas = receta.compra.map { "${cantidad(it, f).removeSuffix(" *")} ${it.n}".trim() }
        return (listOf(cabecera, "") + lineas).joinToString("\n")
    }
}

/** Eje y barras del gráfico de tiempos (docs/spec/app-legado.md §2.1). */
object Gantt {
    /** Marcas del eje en minutos: 0, salto, 2·salto… y siempre `total` al final. */
    fun marcas(total: Double): List<Double> {
        if (total <= 0) return listOf(0.0)
        val granu = if (total <= 60) 5 else 10
        val salto = ceil(total / 5 / granu).toInt().coerceAtLeast(1) * granu
        val marcas = generateSequence(0.0) { it + salto }.takeWhile { it <= total }.toMutableList()
        if (marcas.last() != total) {
            if (total - marcas.last() < 0.5 * salto && marcas.size > 1) marcas.removeAt(marcas.lastIndex)
            marcas += total
        }
        return marcas
    }

    /** Posición relativa [0,1] de inicio y ancho de un paso sobre `total`. */
    fun barra(
        paso: Paso,
        total: Double,
        anchoMinimo: Double = 0.0,
    ): Pair<Float, Float> {
        if (total <= 0) return 0f to 0f
        val izq = (paso.off / total).coerceIn(0.0, 1.0)
        val ancho = (paso.min / total).coerceIn(anchoMinimo, 1.0 - izq)
        return izq.toFloat() to ancho.toFloat()
    }

    /** Número de carriles de fuego (el horno no cuenta), para el chip "N fuegos". */
    fun fuegos(receta: Receta): Int = receta.carriles.count { it.tipoCarril == TipoCarril.FUEGO }
}
