package app.trombito.souschef.receta

/**
 * Topes de legibilidad (reglas 30-36, docs/analisis/legibilidad-crono-y-textos.md § 6.3). De partida: se calibran
 * cocinando el piloto. Único sitio donde viven los números.
 */
object TopesLegibilidad {
    const val T_MAX = 36
    const val NOTA_MAX = 80
    const val NOTA_FRASES_MAX = 2
    const val ELEMENTO_ORDEN_MAX = 36
    const val HITO_MAX = 44
    const val P_MAX = 600
    const val P_PARRAFO_MAX = 240
    const val P_PARRAFOS_MAX = 4
    const val DESCRIPCION_MAX = 200
    const val LECTURA_MAX = 800
    const val PORQUE_MAX = 280

    /** Regla 36: fracción de palabras de contenido de una frase de la nota que ya están en `p`. */
    const val REPETICION_MIN = 0.8

    /** Regla 36: una frase con menos palabras de contenido que esto no se compara (el porcentaje no dice nada). */
    const val PALABRAS_CONTENIDO_MIN = 3

    /** Regla 36: longitud mínima de una palabra de contenido (deja fuera artículos, preposiciones y nexos). */
    const val LARGO_PALABRA_CONTENIDO = 4
}
