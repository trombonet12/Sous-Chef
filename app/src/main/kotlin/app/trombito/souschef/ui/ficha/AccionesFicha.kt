package app.trombito.souschef.ui.ficha

import java.time.LocalDateTime

/** Lo que se puede hacer desde la ficha. */
@Suppress("LongParameterList") // un callback por acción de la ficha
class AccionesFicha(
    val onAtras: () -> Unit,
    val onTecnica: (String) -> Unit,
    val onCocinar: (raciones: Int, servir: String) -> Unit,
    val onRaciones: (Int) -> Unit,
    val onServir: (delta: Int) -> Unit,
    val onFavorita: () -> Unit,
    val onQuiereHacerla: () -> Unit = {},
    /** Anota una cocinada hecha sin cronómetro: momento, raciones y nota. */
    val onAnotar: (momento: LocalDateTime, raciones: Int, nota: String) -> Unit = { _, _, _ -> },
)
