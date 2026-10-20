package app.trombito.souschef.ui.tecnicas

import app.trombito.souschef.ui.comun.TonoPaleta
import app.trombito.souschef.ui.comun.normalizado
import app.trombito.souschef.ui.shell.Icono

/** Categorías conocidas de las técnicas, con el tono y el icono de su cuadradito. Lo desconocido va a [OTRA]. */
enum class CategoriaTecnica(
    val tono: TonoPaleta,
    val icono: Icono,
) {
    ACABADO(TonoPaleta.ROSA, Icono.Acabado),
    ARROZ(TonoPaleta.OLIVA, Icono.Arroz),
    BASE(TonoPaleta.PIEDRA, Icono.Base),
    COCCION(TonoPaleta.AZUL, Icono.Coccion),
    CORTE(TonoPaleta.LILA, Icono.Corte),
    FRITURA(TonoPaleta.TEJA, Icono.Fritura),
    MARINADO(TonoPaleta.VERDE, Icono.Marinado),
    SALSA(TonoPaleta.AMBAR, Icono.Salsa),
    OTRA(TonoPaleta.PIEDRA, Icono.Plato),
    ;

    /** Clave de ruta y de cuadradito: el nombre del enum en minúsculas. */
    val clave: String get() = name.lowercase()

    companion object {
        /** La categoría de la cadena de los datos, sin tildes ni mayúsculas («cocción» es [COCCION]). */
        fun de(categoria: String): CategoriaTecnica {
            val n = categoria.trim().normalizado()
            return entries.firstOrNull { it.clave == n } ?: OTRA
        }

        /** La categoría de una clave de ruta, o null si no existe. */
        fun deClave(clave: String?): CategoriaTecnica? = entries.firstOrNull { it.clave == clave }
    }
}

/** Texto de los datos con la primera letra en mayúscula («cocción» pasa a «Cocción»). */
fun String.conInicialMayuscula(): String = trim().replaceFirstChar { it.uppercase() }
