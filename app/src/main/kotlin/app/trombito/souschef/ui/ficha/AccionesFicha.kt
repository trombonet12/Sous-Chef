package app.trombito.souschef.ui.ficha

/** Lo que se puede hacer desde la ficha. */
class AccionesFicha(
    val onAtras: () -> Unit,
    val onTecnica: (String) -> Unit,
    val onCocinar: (raciones: Int, servir: String) -> Unit,
    val onRaciones: (Int) -> Unit,
    val onServir: (delta: Int) -> Unit,
    val onFavorita: () -> Unit,
)
