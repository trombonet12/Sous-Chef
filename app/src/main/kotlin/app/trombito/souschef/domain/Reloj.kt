package app.trombito.souschef.domain

/** Reloj de pared inyectable: el cronómetro y la sync nunca leen la hora directamente (tests deterministas). */
fun interface Reloj {
    fun ahoraMs(): Long

    companion object {
        val Sistema = Reloj { System.currentTimeMillis() }
    }
}
