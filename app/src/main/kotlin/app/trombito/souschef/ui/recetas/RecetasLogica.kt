package app.trombito.souschef.ui.recetas

import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.domain.OrdenRecetas
import app.trombito.souschef.domain.Preferencia
import app.trombito.souschef.receta.Gantt
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.comun.normalizado
import app.trombito.souschef.ui.ficha.Servir
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

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
    val familia: FamiliaBase = FamiliaBase.de(receta.tags.base),
    val conHorno: Boolean = false,
    /** Minutos del día: cuándo empezar (EMPEZAR) o cuándo acabas si empiezas ya (ACABAR). */
    val hora: Int = Servir.inicio(Servir.DEFECTO, receta),
    val modoHora: HoraTarjeta = HoraTarjeta.EMPEZAR,
)

/** Cuándo se mira la lista y qué hora enseña cada tarjeta. */
data class ContextoLista(
    val hoy: LocalDate,
    /** Hora actual en minutos del día, en la zona del móvil. */
    val ahoraMin: Int = 0,
    val servirMin: Int = Servir.DEFECTO,
    val modo: HoraTarjeta = HoraTarjeta.EMPEZAR,
) {
    fun hora(r: Receta): Int =
        when (modo) {
            HoraTarjeta.EMPEZAR -> Servir.inicio(servirMin, r)
            HoraTarjeta.ACABAR -> Servir.ajustar(ahoraMin, ceil(r.totalEfectivo).toInt())
        }
}

/** Un chip de la hoja con cuántas recetas quedarían si se marcara (con el resto de filtros puestos). */
data class ChipFiltro(
    val opcion: OpcionFiltro,
    val n: Int,
    val activo: Boolean,
)

data class GrupoFiltro(
    val seccion: SeccionFiltro,
    val chips: List<ChipFiltro>,
)

data class RecetasUiState(
    val cargando: Boolean = true,
    val consulta: String = "",
    /** Filtros marcados que siguen existiendo, en el orden de la hoja. */
    val activos: List<OpcionFiltro> = emptyList(),
    val secciones: List<GrupoFiltro> = emptyList(),
    val orden: OrdenRecetas = OrdenRecetas.FAVORITAS,
    val disposicion: DisposicionLista = DisposicionLista.TINTE,
    /** Recetas que pasan el buscador (sin los filtros de la hoja). */
    val coincidentes: Int = 0,
    val totalRecetas: Int = 0,
    val tarjetas: List<TarjetaReceta> = emptyList(),
)

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

private val POR_TITULO = compareBy<TarjetaReceta> { it.receta.titulo.normalizado() }

private fun comparador(orden: OrdenRecetas): Comparator<TarjetaReceta> =
    when (orden) {
        // Favoritas primero y luego `actualizada` descendente.
        OrdenRecetas.FAVORITAS -> {
            compareByDescending<TarjetaReceta> { it.favorita }.thenByDescending { it.receta.actualizada.orEmpty() }
        }

        // Sin estrenar primero y luego la que hace más que no se cocina.
        OrdenRecetas.OLVIDADAS -> {
            compareBy<TarjetaReceta> { it.ultimaFecha != null }.thenBy { it.ultimaFecha.orEmpty() }.then(POR_TITULO)
        }

        OrdenRecetas.ALFABETICO -> {
            POR_TITULO
        }

        OrdenRecetas.RAPIDA -> {
            compareBy<TarjetaReceta> { it.receta.totalEfectivo }.then(POR_TITULO)
        }
    }

/** Lo que el usuario ha escrito, marcado y elegido en la lista. */
data class FiltroRecetas(
    val consulta: String = "",
    val activos: Set<OpcionFiltro> = emptySet(),
    val orden: OrdenRecetas = OrdenRecetas.FAVORITAS,
)

fun construirEstadoRecetas(
    recetas: List<Receta>,
    preferencias: Map<String, Preferencia>,
    ultimasFechas: Map<String, String>,
    filtro: FiltroRecetas,
    contexto: ContextoLista,
): RecetasUiState {
    val porTexto =
        recetas.filter { coincideTexto(it, filtro.consulta) }.map { r ->
            val fecha = ultimasFechas[r.slug]
            val favorita = preferencias[r.slug]?.favorito == true
            val fuegos = Gantt.fuegos(r)
            val hace = Hace.de(fecha, contexto.hoy)
            val rasgos = RasgosReceta.de(r, favorita, fuegos, hace)
            val tarjeta =
                TarjetaReceta(
                    receta = r,
                    favorita = favorita,
                    fuegos = fuegos,
                    ultimaFecha = fecha,
                    hace = hace,
                    familia = rasgos.familia,
                    conHorno = EquipoReceta.CON_HORNO in rasgos.equipo,
                    hora = contexto.hora(r),
                    modoHora = contexto.modo,
                )
            tarjeta to rasgos
        }
    // Una opción existe si alguna receta del buscador la cumple (las de «Mostrar» siempre, aunque cuenten 0, para que
    // «Favoritas» no desaparezca); las marcadas que ya no existen se ignoran.
    val existentes =
        OpcionFiltro.TODAS.filter { o -> o is OpcionFiltro.Mostrar || porTexto.any { it.second.cumple(o) } }
    val activos = existentes.filter { it in filtro.activos }.sortedWith(OpcionFiltro.ORDEN)
    val secciones =
        existentes
            .groupBy { it.seccion }
            .map { (seccion, opciones) ->
                GrupoFiltro(
                    seccion,
                    opciones.map { o ->
                        val n =
                            porTexto.count { (_, rasgos) ->
                                rasgos.cumple(o) &&
                                    rasgos.pasa(activos, salvo = seccion)
                            }
                        ChipFiltro(o, n, o in activos)
                    },
                )
            }.sortedBy { it.seccion.ordinal }
    val tarjetas =
        porTexto
            .filter { (_, rasgos) -> rasgos.pasa(activos) }
            .map { it.first }
            .sortedWith(comparador(filtro.orden))
    return RecetasUiState(
        cargando = false,
        consulta = filtro.consulta,
        activos = activos,
        secciones = secciones,
        orden = filtro.orden,
        coincidentes = porTexto.size,
        totalRecetas = recetas.size,
        tarjetas = tarjetas,
    )
}
