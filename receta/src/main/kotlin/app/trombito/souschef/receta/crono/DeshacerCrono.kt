package app.trombito.souschef.receta.crono

/** Olvida el deshacer de [carril]; «deshaz» sin carril pasa al Hecho anterior que aún se pueda deshacer. */
internal fun EstadoCrono.sinDeshacer(carril: String): EstadoCrono {
    if (carril !in deshacer) return this
    val resto = deshacer - carril
    return copy(deshacer = resto, ultimoHecho = resto.keys.lastOrNull())
}
