package app.trombito.souschef.domain

import app.trombito.souschef.receta.Equipo
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.equipoDe

/** Los aparatos que pide [receta] y no están en [equipo] (horno de los carriles, el resto de `tags.equipo`). */
fun equipoQueFalta(
    receta: Receta,
    equipo: Set<Equipo>,
): Set<Equipo> = equipoDe(receta) - equipo

/** La receta se puede hacer con lo que el usuario tiene: no pide ningún aparato que le falte. */
fun cabeConMiEquipo(
    receta: Receta,
    equipo: Set<Equipo>,
): Boolean = equipoQueFalta(receta, equipo).isEmpty()
