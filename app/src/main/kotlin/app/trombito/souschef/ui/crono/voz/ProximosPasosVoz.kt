package app.trombito.souschef.ui.crono.voz

import app.trombito.souschef.ui.crono.VistaCarril

/** «Próximos pasos» hablados: los de un carril con su duración, o qué toca y qué sigue en cada carril vivo. */
object ProximosPasosVoz {
    /** Los pasos que le quedan a un carril, con su duración; si es el último, se dice. */
    fun deCarril(v: VistaCarril): String {
        val nombre = nombreHablado(v.nombre)
        val actual = minuscula(hablar(v.titulo.orEmpty()))
        return when {
            v.terminado -> {
                RespuestasVoz.cuantoQueda(v)
            }

            v.proximos.isEmpty() -> {
                limitar("$nombre: $actual es el último paso.")
            }

            else -> {
                val pasos =
                    v.proximos.joinToString("; luego ") { "${minuscula(hablar(it.titulo))}, ${tiempo(it.segundos)}" }
                limitar("$nombre, después de $actual: $pasos.", MAX_INFO)
            }
        }
    }

    /** Qué toca en cada carril vivo y lo que viene detrás, por hora prevista. */
    fun general(vistas: List<VistaCarril>): String {
        val vivos = vistas.filter { !it.terminado }.sortedBy { it.sugeridoMs ?: Long.MAX_VALUE }
        if (vivos.isEmpty()) return "Todo terminado."
        val frase =
            vivos.joinToString(" ") { v ->
                val luego =
                    v.proximos
                        .firstOrNull()
                        ?.let { ", luego ${minuscula(hablar(it.titulo))}" }
                        .orEmpty()
                "${nombreHablado(v.nombre)}: ${minuscula(hablar(v.titulo.orEmpty()))}$luego."
            }
        return limitar(frase, MAX_INFO)
    }

    private fun minuscula(texto: String): String = texto.replaceFirstChar { it.lowercase() }
}
