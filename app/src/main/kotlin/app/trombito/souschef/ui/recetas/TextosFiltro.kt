package app.trombito.souschef.ui.recetas

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.trombito.souschef.R
import app.trombito.souschef.domain.OrdenRecetas
import app.trombito.souschef.receta.Momento

/** Textos de las secciones, opciones y órdenes de la hoja de filtros. */
fun SeccionFiltro.titulo(): Int =
    when (this) {
        SeccionFiltro.MOSTRAR -> R.string.filtros_seccion_mostrar
        SeccionFiltro.TIEMPO -> R.string.filtros_seccion_tiempo
        SeccionFiltro.MOMENTO -> R.string.filtros_seccion_momento
        SeccionFiltro.BASE -> R.string.filtros_seccion_base
        SeccionFiltro.PROTEINA -> R.string.filtros_seccion_proteina
        SeccionFiltro.EQUIPO -> R.string.filtros_seccion_equipo
    }

@Composable
fun textoOrden(o: OrdenRecetas): String =
    stringResource(
        when (o) {
            OrdenRecetas.FAVORITAS -> R.string.orden_favoritas
            OrdenRecetas.OLVIDADAS -> R.string.orden_olvidadas
            OrdenRecetas.ALFABETICO -> R.string.orden_alfabetico
            OrdenRecetas.RAPIDA -> R.string.orden_rapida
        },
    )

@Composable
fun textoOpcion(o: OpcionFiltro): String =
    stringResource(
        when (o) {
            is OpcionFiltro.Mostrar -> o.valor.texto()
            is OpcionFiltro.Tiempo -> o.valor.texto()
            is OpcionFiltro.MomentoDia -> o.valor.texto()
            is OpcionFiltro.Base -> o.valor.texto()
            is OpcionFiltro.Proteina -> o.valor.texto()
            is OpcionFiltro.Equipo -> o.valor.texto()
        },
    )

private fun MostrarRecetas.texto(): Int =
    when (this) {
        MostrarRecetas.FAVORITAS -> R.string.filtro_mostrar_favoritas
        MostrarRecetas.QUIERO_HACERLA -> R.string.filtro_mostrar_quiero_hacerla
        MostrarRecetas.NUEVAS -> R.string.filtro_mostrar_nuevas
        MostrarRecetas.SIN_ESTRENAR -> R.string.filtro_mostrar_sin_estrenar
        MostrarRecetas.HACE_TIEMPO -> R.string.filtro_mostrar_hace_tiempo
    }

private fun TramoTiempo.texto(): Int =
    when (this) {
        TramoTiempo.RAPIDA -> R.string.filtro_tiempo_rapida
        TramoTiempo.CORTA -> R.string.filtro_tiempo_corta
        TramoTiempo.MEDIA -> R.string.filtro_tiempo_media
        TramoTiempo.LARGA -> R.string.filtro_tiempo_larga
    }

private fun Momento.texto(): Int =
    when (this) {
        Momento.COMIDA -> R.string.filtro_momento_comida
        Momento.CENA -> R.string.filtro_momento_cena
        Momento.DESAYUNO -> R.string.filtro_momento_desayuno
        Momento.MERIENDA -> R.string.filtro_momento_merienda
        Momento.POSTRE -> R.string.filtro_momento_postre
    }

private fun FamiliaBase.texto(): Int =
    when (this) {
        FamiliaBase.PASTA -> R.string.filtro_base_pasta
        FamiliaBase.ARROZ -> R.string.filtro_base_arroz
        FamiliaBase.PATATA -> R.string.filtro_base_patata
        FamiliaBase.PESCADO -> R.string.filtro_base_pescado
        FamiliaBase.VERDURA -> R.string.filtro_base_verdura
        FamiliaBase.MASAS -> R.string.filtro_base_masas
        FamiliaBase.SALTEADO -> R.string.filtro_base_salteado
        FamiliaBase.CARNES -> R.string.filtro_base_carnes
        FamiliaBase.POSTRES -> R.string.filtro_base_postres
        FamiliaBase.OTRA -> R.string.filtro_base_otra
    }

private fun FamiliaProteina.texto(): Int =
    when (this) {
        FamiliaProteina.POLLO -> R.string.filtro_proteina_pollo
        FamiliaProteina.CARNE -> R.string.filtro_proteina_carne
        FamiliaProteina.CERDO -> R.string.filtro_proteina_cerdo
        FamiliaProteina.PESCADO -> R.string.filtro_proteina_pescado
        FamiliaProteina.HUEVO -> R.string.filtro_proteina_huevo
        FamiliaProteina.VEGETARIANA -> R.string.filtro_proteina_vegetariana
    }

private fun EquipoReceta.texto(): Int =
    when (this) {
        EquipoReceta.SIN_HORNO -> R.string.filtro_equipo_sin_horno
        EquipoReceta.CON_HORNO -> R.string.filtro_equipo_con_horno
        EquipoReceta.UN_FUEGO -> R.string.filtro_equipo_un_fuego
        EquipoReceta.CON_LO_QUE_TENGO -> R.string.filtro_equipo_con_lo_que_tengo
    }
