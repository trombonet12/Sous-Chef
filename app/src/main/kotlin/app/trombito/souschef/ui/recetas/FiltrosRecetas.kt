package app.trombito.souschef.ui.recetas

import app.trombito.souschef.receta.Momento
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.TipoCarril
import app.trombito.souschef.ui.comun.normalizado

/** Las bases libres de `tags.base` agrupadas en diez familias, cada una con su color en la lista. */
enum class FamiliaBase {
    PASTA,
    ARROZ,
    PATATA,
    PESCADO,
    VERDURA,
    MASAS,
    SALTEADO,
    CARNES,
    POSTRES,
    OTRA,
    ;

    companion object {
        private val POR_BASE =
            mapOf(
                "pasta" to PASTA,
                "arroz" to ARROZ,
                "patata" to PATATA,
                "pescado" to PESCADO,
                "verdura" to VERDURA,
                "ensalada" to VERDURA,
                "crema" to VERDURA,
                "masa de pizza" to MASAS,
                "hojaldre" to MASAS,
                "taco" to MASAS,
                "quesadilla" to MASAS,
                "wrap" to MASAS,
                "pan" to MASAS,
                // Las claves van sin acentos: `de` normaliza la base ("buñuelo" -> "bunuelo").
                "bunuelo" to MASAS,
                "salteado" to SALTEADO,
                "asado" to CARNES,
                "hamburguesa" to CARNES,
                "fritura" to CARNES,
                "guiso" to CARNES,
                "postre" to POSTRES,
            )

        fun de(base: String?): FamiliaBase = base?.trim()?.normalizado()?.let { POR_BASE[it] } ?: OTRA
    }
}

/** Familias de proteína sacadas de `tags.proteina` por palabras; una receta puede tener varias. */
enum class FamiliaProteina {
    POLLO,
    CARNE,
    CERDO,
    PESCADO,
    HUEVO,

    /** Ni carne ni pescado (puede llevar huevo o queso). */
    VEGETARIANA,
    ;

    companion object {
        private val RAICES =
            listOf(
                POLLO to listOf("pollo", "pechuga"),
                CARNE to listOf("carne", "ternera"),
                CERDO to listOf("bacon", "jamon", "cerdo", "chorizo"),
                PESCADO to listOf("pescado", "salmon", "emperador", "atun", "merluza", "bacalao"),
                HUEVO to listOf("huevo"),
            )
        private val CARNICAS = setOf(POLLO, CARNE, CERDO, PESCADO)
        private val PALABRA = Regex("[a-z]+")

        fun de(proteina: String?): Set<FamiliaProteina> {
            val palabras = PALABRA.findAll(proteina.orEmpty().normalizado()).map { it.value }.toList()
            val familias =
                RAICES
                    .filter { (_, raices) -> raices.any { r -> palabras.any { it.startsWith(r) } } }
                    .map { it.first }
                    .toMutableSet()
            if (familias.none { it in CARNICAS }) familias += VEGETARIANA
            return familias
        }
    }
}

/** Tramos de duración total: 30 min o menos, de 30 a 45 (sin llegar), de 45 a 90 y más de 90. */
enum class TramoTiempo {
    RAPIDA,
    CORTA,
    MEDIA,
    LARGA,
    ;

    companion object {
        /** Hasta aquí (incluido) una receta es rápida; también el límite de las «Cenas rápidas». */
        const val RAPIDA_HASTA = 30.0
        private const val CORTA_HASTA = 45.0
        private const val MEDIA_HASTA = 90.0

        fun de(minutos: Double): TramoTiempo =
            when {
                minutos <= RAPIDA_HASTA -> RAPIDA
                minutos < CORTA_HASTA -> CORTA
                minutos <= MEDIA_HASTA -> MEDIA
                else -> LARGA
            }
    }
}

/** «Con lo que tengo»: la receta no pide ningún aparato que el usuario no tenga (Ajustes → Tu equipo). */
enum class EquipoReceta { SIN_HORNO, CON_HORNO, UN_FUEGO, CON_LO_QUE_TENGO }

/**
 * «Quiero hacerla»: la marcada con el marcador de la ficha. «Hace tiempo»: cocinada hace [Hace.DIAS_RESALTE] días o más
 * (la que sale resaltada en la tarjeta).
 */
enum class MostrarRecetas { FAVORITAS, QUIERO_HACERLA, NUEVAS, SIN_ESTRENAR, HACE_TIEMPO }

/** Secciones de la hoja de filtros, en su orden: dentro de una se suman opciones (O) y entre ellas se restringe (Y). */
enum class SeccionFiltro { MOSTRAR, TIEMPO, MOMENTO, BASE, PROTEINA, EQUIPO }

/** Un chip de la hoja de filtros. */
sealed interface OpcionFiltro {
    val seccion: SeccionFiltro
    val valor: Enum<*>

    data class Mostrar(
        override val valor: MostrarRecetas,
    ) : OpcionFiltro {
        override val seccion get() = SeccionFiltro.MOSTRAR
    }

    data class Tiempo(
        override val valor: TramoTiempo,
    ) : OpcionFiltro {
        override val seccion get() = SeccionFiltro.TIEMPO
    }

    /** `tags.momento` de la receta; las que no lo tienen no cumplen ninguno. */
    data class MomentoDia(
        override val valor: Momento,
    ) : OpcionFiltro {
        override val seccion get() = SeccionFiltro.MOMENTO
    }

    data class Base(
        override val valor: FamiliaBase,
    ) : OpcionFiltro {
        override val seccion get() = SeccionFiltro.BASE
    }

    data class Proteina(
        override val valor: FamiliaProteina,
    ) : OpcionFiltro {
        override val seccion get() = SeccionFiltro.PROTEINA
    }

    data class Equipo(
        override val valor: EquipoReceta,
    ) : OpcionFiltro {
        override val seccion get() = SeccionFiltro.EQUIPO
    }

    companion object {
        /** Todas las opciones posibles, en el orden de la hoja. */
        val TODAS: List<OpcionFiltro> =
            MostrarRecetas.entries.map(::Mostrar) +
                TramoTiempo.entries.map(::Tiempo) +
                Momento.entries.map(::MomentoDia) +
                FamiliaBase.entries.map(::Base) +
                FamiliaProteina.entries.map(::Proteina) +
                EquipoReceta.entries.map(::Equipo)

        val ORDEN: Comparator<OpcionFiltro> = compareBy({ it.seccion.ordinal }, { it.valor.ordinal })
    }
}

/** Lo que una receta es a efectos de filtro, calculado una vez por receta. */
data class RasgosReceta(
    val familia: FamiliaBase,
    val proteinas: Set<FamiliaProteina>,
    val tramo: TramoTiempo,
    /** `tags.momento` tal cual; vacío si la receta no lo declara. */
    val momentos: Set<Momento>,
    val equipo: Set<EquipoReceta>,
    val mostrar: Set<MostrarRecetas>,
) {
    fun cumple(o: OpcionFiltro): Boolean =
        when (o) {
            is OpcionFiltro.Mostrar -> o.valor in mostrar
            is OpcionFiltro.Tiempo -> o.valor == tramo
            is OpcionFiltro.MomentoDia -> o.valor in momentos
            is OpcionFiltro.Base -> o.valor == familia
            is OpcionFiltro.Proteina -> o.valor in proteinas
            is OpcionFiltro.Equipo -> o.valor in equipo
        }

    /** Pasa todas las secciones activas salvo [salvo]: O dentro de cada sección, Y entre secciones. */
    fun pasa(
        activos: Collection<OpcionFiltro>,
        salvo: SeccionFiltro? = null,
    ): Boolean =
        activos
            .filter { it.seccion != salvo }
            .groupBy { it.seccion }
            .values
            .all { opciones -> opciones.any(::cumple) }

    companion object {
        @Suppress("LongParameterList") // un dato por cada opción de «Mostrar»
        fun de(
            receta: Receta,
            favorita: Boolean,
            fuegos: Int,
            hace: Hace,
            nueva: Boolean = false,
            quiereHacerla: Boolean = false,
            cabeEnMiEquipo: Boolean = true,
        ): RasgosReceta {
            val conHorno = receta.carriles.any { it.tipoCarril == TipoCarril.HORNO }
            val momentos = receta.tags.momento ?: emptyList()
            return RasgosReceta(
                familia = FamiliaBase.de(receta.tags.base),
                proteinas = FamiliaProteina.de(receta.tags.proteina),
                tramo = TramoTiempo.de(receta.totalEfectivo),
                momentos = momentos.toSet(),
                equipo =
                    buildSet {
                        add(if (conHorno) EquipoReceta.CON_HORNO else EquipoReceta.SIN_HORNO)
                        if (fuegos == 1) add(EquipoReceta.UN_FUEGO)
                        if (cabeEnMiEquipo) add(EquipoReceta.CON_LO_QUE_TENGO)
                    },
                mostrar =
                    buildSet {
                        if (favorita) add(MostrarRecetas.FAVORITAS)
                        if (quiereHacerla) add(MostrarRecetas.QUIERO_HACERLA)
                        if (nueva) add(MostrarRecetas.NUEVAS)
                        if (hace.unidad == UnidadHace.SIN_ESTRENAR) {
                            add(MostrarRecetas.SIN_ESTRENAR)
                        } else if (hace.resaltado) {
                            add(MostrarRecetas.HACE_TIEMPO)
                        }
                    },
            )
        }
    }
}
