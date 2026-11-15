package app.trombito.souschef.receta.crono

import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Potencia

/**
 * La orden de un paso compuesta con sus datos (ADR-0017, modelo B): METE (`meto`), fuego (`f`), tapa, duración (`s`),
 * SEÑAL (`nota`) y SACA (`saco`). Función pura: sin Android ni reloj. Da la misma orden en tres formas:
 * - [pantalla]: cada pieza por separado, para la tarjeta del cronómetro;
 * - [linea]: una línea para la notificación («1ª fritura · METE 500 g de patata · fuego 4»);
 * - [voz]: frases para el TTS («Sartén grande: primera fritura. Mete 500 gramos de patata. Fuego 4, sin tapa.»).
 *
 * La potencia (`{N}` de los textos y `fuego`) se traduce a la [Cocina] que se pase; con la canónica por defecto sale el
 * número 0-9. Los avisos de la alarma ([avisoEmpezar], [avisoHecho]) no traducen: dejan los `{N}` para quien pinte.
 */
data class OrdenPaso(
    /** Título del paso (`t`). */
    val titulo: String,
    val meto: String? = null,
    /** Potencia canónica 0-9 (`f`). */
    val fuego: Int? = null,
    /** Potencia del paso anterior del carril; si difiere de [fuego] el chip dice «4 → 5». */
    val fuegoAnterior: Int? = null,
    /** Campo `tapa` crudo («sin tapa», «TAPA PUESTA», «tapa fuera», «TAPA A MEDIAS»). */
    val tapa: String? = null,
    val segundos: Int? = null,
    /** La señal (`nota`): cómo sabes que está. Puede faltar. */
    val senal: String? = null,
    val saco: String? = null,
) {
    /** Las piezas de la orden tal como se enseñan en la tarjeta. */
    data class Pantalla(
        val titulo: String,
        /** «500 g de patata · la fuente»; null si el paso no mete nada. */
        val mete: String?,
        /** «4» o «4 → 5»; null sin potencia. */
        val fuego: String?,
        /** «sin tapa», «tapada» o «tapa a medias»; null si el paso no dice nada de la tapa. */
        val tapa: String?,
        /** «18 min». */
        val duracion: String?,
        val senal: String?,
        val saca: String?,
    ) {
        /** «fuego 4 · sin tapa · 18 min»: lo que va junto al reloj antes de empezar. */
        val detalle: String get() = listOfNotNull(fuego?.let { "fuego $it" }, tapa, duracion).joinToString(SEP)
    }

    fun pantalla(cocina: Cocina = Cocina.VITRO_9): Pantalla =
        Pantalla(
            titulo = limpio(titulo, cocina),
            mete = elementos(meto, cocina).takeIf { it.isNotEmpty() }?.joinToString(MAS) { it.pantalla() },
            fuego = fuegoChip(cocina),
            tapa = textoTapa(tapa),
            duracion = duracionCorta(segundos),
            senal = senal?.let { limpio(it, cocina) }?.takeIf { it.isNotEmpty() },
            saca = elementos(saco, cocina).takeIf { it.isNotEmpty() }?.joinToString(MAS) { it.pantalla() },
        )

    /** «1ª fritura · METE 500 g de patata · fuego 4»: sin origen, tapa ni duración, para la notificación. */
    fun linea(cocina: Cocina = Cocina.VITRO_9): String =
        listOfNotNull(
            limpio(titulo, cocina).takeIf { it.isNotEmpty() },
            elementos(meto, cocina).takeIf { it.isNotEmpty() }?.joinToString(MAS) { it.que }?.let { "METE $it" },
            fuegoChip(cocina)?.let { "fuego $it" },
        ).joinToString(SEP)

    /**
     * La orden hablada, una frase por pieza: «Sartén grande: primera fritura. Mete 500 gramos de patata. Fuego 4,
     * sin tapa. Dieciocho minutos.». [carril] ya va dicho («Sartén grande»); [hablarDuracion] dice los segundos (la voz
     * de la app los dice en letra); [conDuracion] es false cuando el paso ya corre y el reloj dice lo que queda.
     */
    fun voz(
        carril: String? = null,
        cocina: Cocina = Cocina.VITRO_9,
        hablarDuracion: (Int) -> String = { duracionLlana(it) },
        conDuracion: Boolean = true,
    ): String =
        listOfNotNull(
            tituloHablado(carril, cocina),
            detalleHablado(cocina, hablarDuracion, conDuracion),
        ).joinToString(" ")

    /** La orden hablada sin el título: lo que se mete, el fuego y la tapa, y la duración. null si no hay nada. */
    fun detalleHablado(
        cocina: Cocina = Cocina.VITRO_9,
        hablarDuracion: (Int) -> String = { duracionLlana(it) },
        conDuracion: Boolean = true,
    ): String? =
        listOfNotNull(
            meteHablado(cocina),
            fuegoYTapaHablados(cocina),
            segundos?.takeIf { conDuracion && it > 0 }?.let { cerrar(hablarDuracion(it)) },
        ).joinToString(" ").takeIf { it.isNotEmpty() }

    /** «Mete 500 gramos de patata desde la fuente.»; null si no mete nada. */
    fun meteHablado(cocina: Cocina = Cocina.VITRO_9): String? = hablado("Mete", meto, cocina)

    /** «Saca la patata, al plato con papel.»; null si no saca nada. */
    fun sacaHablado(cocina: Cocina = Cocina.VITRO_9): String? = hablado("Saca", saco, cocina)

    /** La señal como frase («Tiernas y pálidas, sin color.»); null si falta. */
    fun senalHablada(cocina: Cocina = Cocina.VITRO_9): String? =
        senal?.let { cerrar(limpio(it, cocina)) }?.takeIf { it != "." }

    /** «Toca empezar: 1ª fritura · mete 500 gramos de patata desde la fuente». Sin traducir la potencia. */
    fun avisoEmpezar(): String = listOfNotNull("Toca empezar: $titulo", frase("mete", meto)).joinToString(SEP)

    /** «1ª fritura: hecho · saca la patata, al plato con papel». Sin traducir la potencia; las unidades, en palabra. */
    fun avisoHecho(): String = listOfNotNull("$titulo: hecho", frase("saca", saco)).joinToString(SEP)

    private fun frase(
        verbo: String,
        texto: String?,
    ): String? {
        val partes = elementos(texto, null).map { it.dicho(unidadesEnLetra = true) }
        return if (partes.isEmpty()) null else "$verbo ${unir(partes)}"
    }

    private fun hablado(
        verbo: String,
        texto: String?,
        cocina: Cocina,
    ): String? {
        val partes = elementos(texto, cocina).map { it.dicho(unidadesEnLetra = true) }
        return if (partes.isEmpty()) null else cerrar("$verbo ${unir(partes)}")
    }

    /** «Sartén grande: primera fritura.» (sin carril: «Primera fritura.»); null si no hay nada que decir. */
    fun tituloHablado(
        carril: String?,
        cocina: Cocina = Cocina.VITRO_9,
    ): String? {
        val t = ordinalesEnLetra(limpio(titulo, cocina)).trimEnd('.', ',', ':', ';')
        return when {
            t.isEmpty() -> carril?.let { cerrar(it) }
            carril == null -> cerrar(t.replaceFirstChar { it.titlecase() })
            else -> cerrar("$carril: ${minuscula(t)}")
        }
    }

    private fun fuegoYTapaHablados(cocina: Cocina): String? {
        val potencia =
            fuego?.let {
                val ahora = Potencia.texto(it, cocina)
                val antes = fuegoAnterior?.takeIf { a -> a != it }?.let { a -> Potencia.texto(a, cocina) }
                if (antes != null && antes != ahora) "fuego de $antes a $ahora" else "fuego $ahora"
            }
        val partes = listOfNotNull(potencia, textoTapa(tapa))
        return if (partes.isEmpty()) null else cerrar(partes.joinToString(", ").replaceFirstChar { it.titlecase() })
    }

    private fun fuegoChip(cocina: Cocina): String? {
        val f = fuego ?: return null
        val ahora = Potencia.texto(f, cocina)
        val antes = fuegoAnterior?.takeIf { it != f }?.let { Potencia.texto(it, cocina) }
        return if (antes != null && antes != ahora) "$antes → $ahora" else ahora
    }

    /** Un elemento de `meto` o `saco`: lo que es y, si lo trae («el plato llano: 200 g de bacon»), de dónde. */
    private class Elemento(
        val que: String,
        val origen: String?,
    ) {
        fun pantalla(): String = if (origen == null) que else "$que$SEP$origen"

        fun dicho(unidadesEnLetra: Boolean = false): String {
            val cosa = if (unidadesEnLetra) unidades(que) else que
            return if (origen == null) cosa else "$cosa desde $origen"
        }
    }

    private fun elementos(
        texto: String?,
        cocina: Cocina?,
    ): List<Elemento> =
        texto
            ?.split(MAS)
            .orEmpty()
            .map { limpio(it, cocina) }
            .filter { it.isNotEmpty() }
            .map { e ->
                val corte = e.indexOf(": ")
                val hayOrigen = corte > 0 && corte <= MAX_ORIGEN && !e.first().isDigit() && corte + 2 < e.length
                if (hayOrigen) {
                    Elemento(e.substring(corte + 2).trim(), e.substring(0, corte).trim())
                } else {
                    Elemento(e, null)
                }
            }

    companion object {
        private const val SEP = " · "
        private const val MAS = " + "
        private const val MAX_ORIGEN = 40
        private const val SEG_MINUTO = 60
        private val etiquetas = Regex("<[^>]*>")
        private val espacios = Regex("""\s+""")
        private val cantidad = Regex("""(\d+(?:,\d+)?) ?(kg|g|ml|cl|l)(?![\p{L}\p{N}])""")
        private val ordinal = Regex("""(\d+)([ªº])""")
        private val ordinalesFemeninos =
            listOf("", "primera", "segunda", "tercera", "cuarta", "quinta", "sexta", "séptima", "octava", "novena")
        private val ordinalesMasculinos =
            listOf("", "primer", "segundo", "tercer", "cuarto", "quinto", "sexto", "séptimo", "octavo", "noveno")

        /** La orden de [paso]; [fuegoAnterior] es la potencia del paso anterior de su carril (null si no hay). */
        fun de(
            paso: Paso,
            fuegoAnterior: Int? = null,
        ): OrdenPaso =
            OrdenPaso(
                titulo = paso.t,
                meto = paso.meto,
                fuego = paso.f,
                fuegoAnterior = fuegoAnterior,
                tapa = paso.tapa,
                segundos = paso.segundos,
                senal = paso.nota,
                saco = paso.saco,
            )

        /** La orden del paso [indice] de la cola de un carril: su potencia anterior es la última que hubo antes. */
        fun delCarril(
            pasos: List<Paso>,
            indice: Int,
        ): OrdenPaso = de(pasos[indice], pasos.take(indice).lastOrNull { it.f != null }?.f)

        /** «sin tapa», «tapada» o «tapa a medias»; null si el campo falta o no se reconoce. */
        fun textoTapa(tapa: String?): String? {
            val t = tapa?.uppercase().orEmpty()
            return when {
                t.isBlank() -> null
                "MEDIAS" in t -> "tapa a medias"
                "SIN" in t || "FUERA" in t -> "sin tapa"
                "PUESTA" in t -> "tapada"
                else -> null
            }
        }

        /** «18 min», «45 s», «2 min 30 s»; null si no hay duración. */
        fun duracionCorta(segundos: Int?): String? {
            val s = segundos?.takeIf { it > 0 } ?: return null
            val m = s / SEG_MINUTO
            val r = s % SEG_MINUTO
            return when {
                m == 0 -> "$r s"
                r == 0 -> "$m min"
                else -> "$m min $r s"
            }
        }

        private fun duracionLlana(segundos: Int): String {
            val m = segundos / SEG_MINUTO
            val r = segundos % SEG_MINUTO
            return when {
                m == 0 -> "$r ${if (r == 1) "segundo" else "segundos"}"
                r == 0 -> "$m ${if (m == 1) "minuto" else "minutos"}"
                else -> "$m ${if (m == 1) "minuto" else "minutos"} y $r ${if (r == 1) "segundo" else "segundos"}"
            }.replaceFirstChar { it.titlecase() }
        }

        /** Texto limpio de etiquetas y con la potencia traducida a [cocina] (null = dejar los `{N}`). */
        private fun limpio(
            texto: String,
            cocina: Cocina?,
        ): String {
            val llano = espacios.replace(etiquetas.replace(texto, ""), " ").trim()
            return if (cocina == null) llano else Potencia.reescribir(llano, cocina)
        }

        private fun unidades(texto: String): String =
            cantidad.replace(texto) { m ->
                val n = m.groupValues[1]
                val plural = n != "1"
                val unidad =
                    when (m.groupValues[2]) {
                        "kg" -> if (plural) "kilos" else "kilo"
                        "g" -> if (plural) "gramos" else "gramo"
                        "ml" -> if (plural) "mililitros" else "mililitro"
                        "cl" -> if (plural) "centilitros" else "centilitro"
                        else -> if (plural) "litros" else "litro"
                    }
                "$n $unidad"
            }

        private fun ordinalesEnLetra(texto: String): String =
            ordinal.replace(texto) { m ->
                val n = m.groupValues[1].toInt()
                val lista = if (m.groupValues[2] == "ª") ordinalesFemeninos else ordinalesMasculinos
                lista.getOrNull(n)?.takeIf { it.isNotEmpty() } ?: m.value
            }

        private fun minuscula(texto: String): String =
            if (texto.length > 1 && texto[1].isUpperCase()) texto else texto.replaceFirstChar { it.lowercase() }

        private fun unir(partes: List<String>): String {
            if (partes.size < 2) return partes.joinToString("")
            return partes.dropLast(1).joinToString(", ") + " y " + partes.last()
        }

        private fun cerrar(frase: String): String = if (frase.last() in ".!?") frase else "$frase."
    }
}
