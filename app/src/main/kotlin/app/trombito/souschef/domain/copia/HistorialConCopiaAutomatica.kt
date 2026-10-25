package app.trombito.souschef.domain.copia

import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.HistorialRepo

/**
 * El historial de siempre, que además avisa ([alCerrar]) cuando una cocinada queda guardada en firme (ADR-0015).
 *
 * `agregar` es el punto común de las dos vías que cierran una cocinada: el cronómetro, que solo lo llama al vencer los
 * 12 s de «Deshacer», y «Anotar que la he cocinado» de la ficha. Un decorador evita tocar los dos ViewModels y que
 * una tercera vía futura se olvide de la copia. Lo que no escribe una cocinada nueva (valorar, notas, borrar,
 * importar) no dispara nada: la siguiente copia lleva el historial entero.
 */
class HistorialConCopiaAutomatica(
    private val base: HistorialRepo,
    private val alCerrar: () -> Unit,
) : HistorialRepo by base {
    override suspend fun agregar(entrada: EntradaHistorial) {
        base.agregar(entrada)
        alCerrar()
    }
}
