package app.trombito.souschef.receta

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToLong

/** Escalado de cantidades y formato de números como en la ficha vieja (docs/spec/app-legado.md §2.1). */
object Escalado {
    const val RACIONES_MIN = 1
    const val RACIONES_MAX = 20
    private const val EXPONENTE_SUB = 0.75
    private val ESPACIOS = Regex("\\s+")

    /**
     * Raciones con las que se puede abrir la receta: de `escala.min` a `escala.max` de `paso` en `paso`; sin escala (o
     * con una inválida, que el validador ya rechaza), de [RACIONES_MIN] a [RACIONES_MAX].
     */
    fun racionesPermitidas(r: Receta): List<Int> {
        val e = r.escala?.takeIf { it.valida }
        return if (e == null) (RACIONES_MIN..RACIONES_MAX).toList() else (e.min..e.max step e.paso).toList()
    }

    /**
     * Raciones con las que abrir la receta: la [guardada] si está permitida; si no, la permitida más cercana al
     * [defecto] global (en empate, la menor).
     */
    fun racionesIniciales(
        r: Receta,
        guardada: Int?,
        defecto: Int,
    ): Int {
        val permitidas = racionesPermitidas(r)
        if (guardada != null && guardada in permitidas) return guardada
        return permitidas.minWithOrNull(compareBy({ abs(it - defecto) }, { it })) ?: defecto
    }

    fun factor(
        raciones: Int,
        base: Int,
    ): Double = if (base <= 0) 1.0 else raciones.toDouble() / base

    /**
     * Cantidad escalada y formateada: "250 g", "2,5 ml", "½ cdta *". El asterisco marca escalado sublineal; un
     * ingrediente `fijo` no cambia con las raciones.
     */
    fun cantidad(
        item: Ingrediente,
        f: Double,
    ): String {
        val marca = if (item.sublineal && f != 1.0) " *" else ""
        val a = item.a
        return when {
            item.txt != null -> {
                item.txt + marca
            }

            a == null -> {
                marca.trim()
            }

            else -> {
                val v =
                    when {
                        item.fijo -> a
                        item.sublineal -> a * f.pow(EXPONENTE_SUB)
                        else -> a * f
                    }
                val u =
                    item.u
                        ?.takeIf { it.isNotBlank() }
                        ?.let { " $it" }
                        .orEmpty()
                numero(v) + u + marca
            }
        }
    }

    /**
     * Ingredientes de todas las zonas juntos, en orden de primera aparición. Se suman los que coinciden en nombre base
     * (sin la equivalencia entre paréntesis ni lo que va tras la coma, sin mayúsculas ni espacios de más), unidad y
     * modo de escalado; los de texto salen una vez. Si se juntan varios, la línea lleva el nombre base y, entre
     * paréntesis, la suma de sus equivalencias caseras cuando todas son cucharadas soperas y cucharaditas
     * ([MedidaCasera]); si no, sin paréntesis. Un ingrediente que no se repite conserva su nombre tal cual.
     */
    fun listaGeneral(receta: Receta): List<Ingrediente> {
        val juntos = LinkedHashMap<List<String?>, Ingrediente>()
        val equivalencias = HashMap<List<String?>, MutableList<String?>>()
        receta.grupos.flatMap { it.items }.forEach { i ->
            val clave =
                listOf(normal(base(i.n)), i.u?.trim()?.lowercase(), i.txt?.trim(), i.esc, (i.a == null).toString())
            val previo = juntos[clave]
            juntos[clave] =
                if (previo == null) {
                    i.copy(n = i.n.trim())
                } else {
                    previo.copy(n = base(previo.n), a = previo.a?.let { a -> a + (i.a ?: 0.0) })
                }
            equivalencias.getOrPut(clave) { mutableListOf() } += MedidaCasera.equivalenciaDe(i.n)
        }
        return juntos.map { (clave, i) ->
            val equivalencia = equivalencias.getValue(clave)
            val suma = if (equivalencia.size > 1 && i.a != null) sumaCasera(equivalencia) else null
            if (suma == null) i else i.copy(n = "${i.n} ($suma)")
        }
    }

    private fun sumaCasera(equivalencias: List<String?>): String? =
        if (equivalencias.any { it == null }) null else MedidaCasera.sumar(equivalencias.filterNotNull())

    private fun base(nombre: String) = nombre.substringBefore('(').substringBefore(',').trim()

    private fun normal(nombre: String) = nombre.trim().lowercase().replace(ESPACIOS, " ")

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

    /**
     * Posición relativa [0,1] de inicio y ancho de un paso sobre `total`. Una barra ensanchada a [anchoMinimo] que no
     * cabe al final se corre a la izquierda en vez de salirse.
     */
    fun barra(
        paso: Paso,
        total: Double,
        anchoMinimo: Double = 0.0,
    ): Pair<Float, Float> {
        if (total <= 0) return 0f to 0f
        val ancho = maxOf(paso.min / total, anchoMinimo).coerceIn(0.0, 1.0)
        val izq = (paso.off / total).coerceIn(0.0, 1.0 - ancho)
        return izq.toFloat() to ancho.toFloat()
    }

    /** Número de carriles de fuego (el horno no cuenta), para el chip "N fuegos". */
    fun fuegos(receta: Receta): Int = receta.carriles.count { it.tipoCarril == TipoCarril.FUEGO }
}
