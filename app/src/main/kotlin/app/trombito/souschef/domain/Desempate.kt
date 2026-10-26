package app.trombito.souschef.domain

import java.time.LocalDate

/**
 * Clave de desempate de [slug] para el día [semilla]. `"$slug|$fecha".hashCode()` no sirve: para slugs de igual
 * longitud la fecha suma lo mismo a todos y el orden relativo no cambia de un día a otro. Se mezcla el hash del slug
 * con el día y se pasa por el finalizador de MurmurHash3, que reparte los bits y reordena de verdad cada día.
 */
internal fun desempate(
    slug: String,
    semilla: LocalDate,
): Int {
    var h = slug.hashCode() xor (semilla.toEpochDay().toInt() * MEZCLA_DIA)
    h = h xor (h ushr MURMUR_PRIMER_DESPLAZAMIENTO)
    h *= MURMUR_PRIMERA_CONSTANTE
    h = h xor (h ushr MURMUR_SEGUNDO_DESPLAZAMIENTO)
    h *= MURMUR_SEGUNDA_CONSTANTE
    return h xor (h ushr MURMUR_PRIMER_DESPLAZAMIENTO)
}

private const val MEZCLA_DIA = -1640531535 // 0x9E3779B1: impar, reparte los días consecutivos
private const val MURMUR_PRIMER_DESPLAZAMIENTO = 16
private const val MURMUR_SEGUNDO_DESPLAZAMIENTO = 13
private const val MURMUR_PRIMERA_CONSTANTE = -2048144789 // 0x85EBCA6B
private const val MURMUR_SEGUNDA_CONSTANTE = -1028477387 // 0xC2B2AE35
