package app.trombito.souschef.ui.crono.guia

import app.trombito.souschef.ui.crono.CronoUi
import app.trombito.souschef.ui.crono.enCocina

/** La guía sale al entrar en la cocina en marcha (no en la portada ni en el cierre) si aún no se ha visto. */
internal fun mostrarGuia(ui: CronoUi): Boolean = enCocina(ui) && !ui.guiaVista

/** Índice (en [GuiaIds.ORDEN]) del primer punto a la vista desde [desde], o null si no queda ninguno. */
internal fun primerPuntoDesde(
    visibles: Set<String>,
    desde: Int,
): Int? = (desde.coerceAtLeast(0) until GuiaIds.ORDEN.size).firstOrNull { GuiaIds.ORDEN[it] in visibles }

/** Hay otro punto a la vista después del [indice]. */
internal fun hayMasPuntos(
    visibles: Set<String>,
    indice: Int,
): Boolean = primerPuntoDesde(visibles, indice + 1) != null

/** Posición (1-based) del punto [indice] entre los que están a la vista. */
internal fun numeroDePunto(
    visibles: Set<String>,
    indice: Int,
): Int = GuiaIds.ORDEN.take(indice).count { it in visibles } + 1
