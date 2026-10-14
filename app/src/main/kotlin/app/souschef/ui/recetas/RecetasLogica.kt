package app.souschef.ui.recetas

import app.souschef.domain.Preferencia
import app.souschef.receta.Gantt
import app.souschef.receta.Receta
import app.souschef.ui.comun.contarPor
import app.souschef.ui.comun.normalizado
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class UnidadHace { SIN_ESTRENAR, HOY, AYER, DIAS, SEMANAS, MESES, ANOS }

/** Chip "hace X" (docs/spec/app-legado.md §1): días entre medianoches UTC, resaltado desde 60 días. */
data class Hace(
    val unidad: UnidadHace,
    val cantidad: Int = 0,
    val resaltado: Boolean = false,
) {
    companion object {
        const val DIAS_RESALTE = 60
        private const val DIAS_SEMANA = 7
        private const val DIAS_MES = 30
        private const val DIAS_ANO = 365

        private val SIN_ESTRENAR = Hace(UnidadHace.SIN_ESTRENAR, 0, resaltado = true)

        fun de(
            ultimaFecha: String?,
            hoy: LocalDate,
        ): Hace {
            val fecha = ultimaFecha?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
            return fecha?.let { desde(ChronoUnit.DAYS.between(it, hoy).toInt()) } ?: SIN_ESTRENAR
        }

        private fun desde(dias: Int): Hace {
            val resaltado = dias >= DIAS_RESALTE
            return when {
                dias <= 0 -> Hace(UnidadHace.HOY)
                dias == 1 -> Hace(UnidadHace.AYER)
                dias < DIAS_SEMANA -> Hace(UnidadHace.DIAS, dias)
                dias < DIAS_MES -> Hace(UnidadHace.SEMANAS, dias / DIAS_SEMANA)
                dias < DIAS_ANO -> Hace(UnidadHace.MESES, dias / DIAS_MES, resaltado)
                else -> Hace(UnidadHace.ANOS, dias / DIAS_ANO, resaltado)
            }
        }
    }
}

data class TarjetaReceta(
    val receta: Receta,
    val favorita: Boolean,
    val fuegos: Int,
    val ultimaFecha: String?,
    val hace: Hace,
)

data class ChipBase(
    val base: String,
    val n: Int,
)

data class RecetasUiState(
    val cargando: Boolean = true,
    val consulta: String = "",
    val baseActiva: String? = null,
    val bases: List<ChipBase> = emptyList(),
    val coincidentes: Int = 0,
    val totalRecetas: Int = 0,
    val tarjetas: List<TarjetaReceta> = emptyList(),
) {
    /** Los chips solo salen si hay más de una base entre las que pasan el texto. */
    val mostrarChips: Boolean get() = bases.size > 1
}

/** Texto sobre el que busca: titulo, descripcion, base, ingredientes de los grupos y títulos de los pasos. */
fun textoBuscable(r: Receta): String =
    buildList {
        add(r.titulo)
        add(r.descripcion)
        r.tags.base?.let { add(it) }
        r.grupos.forEach { g -> g.items.forEach { add(it.n) } }
        r.pasos.forEach { add(it.t) }
    }.joinToString(" ").normalizado()

fun coincideTexto(
    receta: Receta,
    consulta: String,
): Boolean {
    val q = consulta.trim().normalizado()
    return q.isEmpty() || textoBuscable(receta).contains(q)
}

/** Favoritas primero y luego `actualizada` descendente. */
private val ORDEN_TARJETAS =
    compareByDescending<TarjetaReceta> { it.favorita }.thenByDescending { it.receta.actualizada.orEmpty() }

/** Lo que el usuario ha escrito o elegido en la lista. */
data class FiltroRecetas(
    val consulta: String = "",
    val base: String? = null,
)

fun construirEstadoRecetas(
    recetas: List<Receta>,
    preferencias: Map<String, Preferencia>,
    ultimasFechas: Map<String, String>,
    filtro: FiltroRecetas,
    hoy: LocalDate,
): RecetasUiState {
    val consulta = filtro.consulta
    val baseElegida = filtro.base
    val porTexto = recetas.filter { coincideTexto(it, consulta) }
    val bases = porTexto.contarPor { it.tags.base }.map { ChipBase(it.first, it.second) }
    val baseActiva = baseElegida?.takeIf { b -> bases.any { it.base == b } }
    val tarjetas =
        porTexto
            .filter { baseActiva == null || it.tags.base == baseActiva }
            .map { r ->
                val fecha = ultimasFechas[r.slug]
                TarjetaReceta(
                    receta = r,
                    favorita = preferencias[r.slug]?.favorito == true,
                    fuegos = Gantt.fuegos(r),
                    ultimaFecha = fecha,
                    hace = Hace.de(fecha, hoy),
                )
            }.sortedWith(ORDEN_TARJETAS)
    return RecetasUiState(
        cargando = false,
        consulta = consulta,
        baseActiva = baseActiva,
        bases = bases,
        coincidentes = porTexto.size,
        totalRecetas = recetas.size,
        tarjetas = tarjetas,
    )
}
