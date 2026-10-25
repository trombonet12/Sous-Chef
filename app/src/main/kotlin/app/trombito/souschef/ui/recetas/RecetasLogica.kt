package app.trombito.souschef.ui.recetas

import app.trombito.souschef.domain.CENAR_DEFECTO_MIN
import app.trombito.souschef.domain.CualHora
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.FUEGOS_DEFECTO
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.domain.HoraUsada
import app.trombito.souschef.domain.OrdenRecetas
import app.trombito.souschef.domain.Preferencia
import app.trombito.souschef.domain.cabeConMiEquipo
import app.trombito.souschef.domain.desvioHabitualMin
import app.trombito.souschef.domain.esNueva
import app.trombito.souschef.domain.horaServir
import app.trombito.souschef.receta.Equipo
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
    /** Fuegos que pide la receta si son más de los de la cocina del usuario; 0 si le bastan. */
    val pideFuegos: Int = 0,
    /** La receta usa el horno y el usuario no tiene. */
    val pideHorno: Boolean = false,
    /** Si llega a servir a la hora elegida en «¿Qué me da tiempo?»; null con el modo apagado. */
    val llegada: Llegada? = null,
    /** Sin cocinar y creada hace menos de 30 días (`esNueva`). */
    val nueva: Boolean = false,
    /** La [hora] ya cuenta el ritmo de Joan en esta receta (`desvioHabitualMin`); la tarjeta lo marca. */
    val conRitmo: Boolean = false,
    /** Hora de servir en que se basa la [hora] (con EMPEZAR) y si es la de comer o la de cenar; la tarjeta lo dice. */
    val usada: HoraUsada = HoraUsada(Servir.DEFECTO, CualHora.COMER),
)

/** Cuándo se mira la lista, qué hora enseña cada tarjeta y qué equipo tiene el usuario. */
data class ContextoLista(
    val hoy: LocalDate,
    /** Hora actual en minutos del día, en la zona del móvil. */
    val ahoraMin: Int = 0,
    val servirMin: Int = Servir.DEFECTO,
    val modo: HoraTarjeta = HoraTarjeta.EMPEZAR,
    /** Fuegos de la cocina del usuario (Ajustes). */
    val fuegos: Int = FUEGOS_DEFECTO,
    /** El usuario tiene horno (Ajustes). */
    val horno: Boolean = true,
    /** Desvío habitual en minutos por slug (positivo = suele tardar más); solo las recetas con ritmo. */
    val ritmos: Map<String, Int> = emptyMap(),
    /** Aparatos que tiene el usuario (Ajustes → Tu equipo), para «Con lo que tengo». */
    val equipo: Set<Equipo> = Equipo.entries.toSet(),
    /** Hora de cenar de Ajustes, para las recetas de cena. */
    val cenarMin: Int = CENAR_DEFECTO_MIN,
) {
    /** Cuál de las horas de servir (comer o cenar) le toca a [r] ahora. */
    fun servirDe(r: Receta) = horaServir(r.tags.momento, ahoraMin, servirMin, cenarMin)

    /** Hora de la tarjeta: con ritmo, empezar antes (o después) lo que Joan suele tardar de más (o de menos). */
    fun hora(r: Receta): Int {
        val ritmo = ritmos[r.slug] ?: 0
        return when (modo) {
            HoraTarjeta.EMPEZAR -> Servir.ajustar(Servir.inicio(servirDe(r).minutos, r), -ritmo)
            HoraTarjeta.ACABAR -> Servir.ajustar(ahoraMin, ceil(r.totalEfectivo).toInt() + ritmo)
        }
    }
}

/**
 * Desvío habitual de cada receta con ritmo, en una sola pasada por el historial (sin consultas por tarjeta). Las
 * recetas sin ritmo (pocas cocinadas o desvío pequeño) no salen en el mapa.
 */
fun ritmosPorSlug(
    recetas: List<Receta>,
    historial: List<EntradaHistorial>,
): Map<String, Int> {
    if (historial.isEmpty()) return emptyMap()
    val porSlug = historial.groupBy { it.slug }
    return buildMap {
        recetas.forEach { r ->
            val entradas = porSlug[r.slug] ?: return@forEach
            desvioHabitualMin(entradas, r.slug, ceil(r.totalEfectivo).toInt())?.let { put(r.slug, it) }
        }
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
    /** Fuegos de la cocina del usuario, de Ajustes. */
    val fuegos: Int = FUEGOS_DEFECTO,
    /** El usuario tiene horno, de Ajustes. */
    val horno: Boolean = true,
    /** Recetas que pasan el buscador (sin los filtros de la hoja). */
    val coincidentes: Int = 0,
    val totalRecetas: Int = 0,
    val tarjetas: List<TarjetaReceta> = emptyList(),
    /** Familias con al menos una receta (sobre todas las recetas), con su recuento, en el orden del enum. */
    val categorias: List<Pair<FamiliaBase, Int>> = emptyList(),
    /** Si la lista es la de una sola familia (se abre desde un cuadradito de la pestaña). */
    val familiaFija: FamiliaBase? = null,
    /** El modo «¿Qué me da tiempo?» si está puesto. */
    val tiempo: ModoTiempo? = null,
    /** Con el modo puesto, la hora de servir ya pasó y no hay margen: no llega ninguna. */
    val tiempoVencido: Boolean = false,
    /** Cuántas recetas son cenas rápidas (sobre todas las recetas): el cuadradito de arriba se oculta con 0. */
    val cenasRapidas: Int = 0,
    /** Si la lista es la de «Cenas rápidas» (se abre desde su cuadradito): lleva barra de título y atrás. */
    val listaCenasRapidas: Boolean = false,
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
    /** Si no es null, solo cuentan las recetas de esta familia y el filtro de Base no existe. */
    val familiaFija: FamiliaBase? = null,
    /** Modo «¿Qué me da tiempo?»; null = apagado. No se guarda. */
    val tiempo: ModoTiempo? = null,
    /** La lista se abrió desde el cuadradito «Cenas rápidas» (los filtros de cena y rápida ya vienen puestos). */
    val cenasRapidas: Boolean = false,
)

/** Las familias con al menos una receta y cuántas tiene cada una, en el orden del enum. */
private fun categoriasDe(recetas: List<Receta>): List<Pair<FamiliaBase, Int>> =
    recetas
        .groupingBy { FamiliaBase.de(it.tags.base) }
        .eachCount()
        .toList()
        .sortedBy { it.first.ordinal }

private fun soloFamilia(
    recetas: List<Receta>,
    familia: FamiliaBase?,
): List<Receta> = if (familia == null) recetas else recetas.filter { FamiliaBase.de(it.tags.base) == familia }

private fun tarjetaYRasgos(
    r: Receta,
    preferencias: Map<String, Preferencia>,
    ultimasFechas: Map<String, String>,
    contexto: ContextoLista,
): Pair<TarjetaReceta, RasgosReceta> {
    val fecha = ultimasFechas[r.slug]
    val favorita = preferencias[r.slug]?.favorito == true
    val fuegos = Gantt.fuegos(r)
    val hace = Hace.de(fecha, contexto.hoy)
    val nueva = esNueva(r, fecha, contexto.hoy)
    val quiereHacerla = preferencias[r.slug]?.quiereHacerla == true
    val rasgos =
        RasgosReceta.de(r, favorita, fuegos, hace, nueva, quiereHacerla, cabeConMiEquipo(r, contexto.equipo))
    val conHorno = EquipoReceta.CON_HORNO in rasgos.equipo
    val tarjeta =
        TarjetaReceta(
            receta = r,
            favorita = favorita,
            fuegos = fuegos,
            ultimaFecha = fecha,
            hace = hace,
            familia = rasgos.familia,
            conHorno = conHorno,
            hora = contexto.hora(r),
            modoHora = contexto.modo,
            pideFuegos = fuegos.takeIf { it > contexto.fuegos } ?: 0,
            pideHorno = conHorno && !contexto.horno,
            nueva = nueva,
            conRitmo = r.slug in contexto.ritmos,
            usada = contexto.servirDe(r),
        )
    return tarjeta to rasgos
}

fun construirEstadoRecetas(
    todasLasRecetas: List<Receta>,
    preferencias: Map<String, Preferencia>,
    ultimasFechas: Map<String, String>,
    filtro: FiltroRecetas,
    contexto: ContextoLista,
): RecetasUiState {
    val familiaFija = filtro.familiaFija
    val recetas = soloFamilia(todasLasRecetas, familiaFija)
    val porTexto =
        recetas
            .filter { coincideTexto(it, filtro.consulta) }
            .map { tarjetaYRasgos(it, preferencias, ultimasFechas, contexto) }
    // Una opción existe si alguna receta del buscador la cumple (las de «Mostrar» siempre, aunque cuenten 0, para que
    // «Favoritas» no desaparezca); las marcadas que ya no existen se ignoran.
    val existentes =
        OpcionFiltro.TODAS.filter { o ->
            when {
                familiaFija != null && o is OpcionFiltro.Base -> false
                else -> o is OpcionFiltro.Mostrar || porTexto.any { it.second.cumple(o) }
            }
        }
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
            .let { aplicarTiempo(it, filtro.tiempo, contexto.ahoraMin) }
    return RecetasUiState(
        cargando = false,
        consulta = filtro.consulta,
        activos = activos,
        secciones = secciones,
        orden = filtro.orden,
        coincidentes = porTexto.size,
        totalRecetas = recetas.size,
        tarjetas = tarjetas,
        fuegos = contexto.fuegos,
        horno = contexto.horno,
        categorias = categoriasDe(todasLasRecetas),
        familiaFija = familiaFija,
        cenasRapidas = todasLasRecetas.count(::esCenaRapida),
        listaCenasRapidas = filtro.cenasRapidas,
        tiempo = filtro.tiempo,
        tiempoVencido = filtro.tiempo?.let { QueMeDaTiempo.margen(contexto.ahoraMin, it.servirMin) == null } == true,
    )
}
