package app.trombito.souschef.domain

import app.trombito.souschef.receta.Cocina
import java.time.LocalDate

private const val LONGITUD_FECHA = 10

/**
 * Cifras agregadas de uso que el usuario puede adjuntar al correo del Buzón (F6). Solo números y ajustes: por
 * construcción no lleva notas, títulos de cocinadas ni slugs ni identificadores.
 */
data class ResumenUso(
    val version: String,
    /** Cocinadas del historial, también las anotadas sin cronómetro. */
    val cocinadas: Int,
    /** De ellas, las del último mes (desde hace un mes hasta hoy, ambos incluidos). */
    val cocinadasUltimoMes: Int,
    val recetasDistintas: Int,
    val repetir: Int,
    val ajustar: Int,
    val noRepetir: Int,
    val disposicion: DisposicionCrono,
    val cocina: Cocina,
    val vozActiva: Boolean,
    val quieroHacerla: Int,
)

/**
 * Resumen de uso a partir de lo que la app ya tiene. Función pura: [hoy] llega de fuera y [quieroHacerla] es el número
 * de recetas marcadas «Quiero hacerla». Una fecha que no se entiende cuenta en el total pero no en el último mes.
 */
fun resumenDeUso(
    entradas: List<EntradaHistorial>,
    quieroHacerla: Int,
    ajustes: Ajustes,
    version: String,
    hoy: LocalDate,
): ResumenUso {
    val desde = hoy.minusMonths(1)
    val delMes =
        entradas.count { e ->
            val fecha = runCatching { LocalDate.parse(e.fecha.take(LONGITUD_FECHA)) }.getOrNull()
            fecha != null && !fecha.isBefore(desde) && !fecha.isAfter(hoy)
        }
    return ResumenUso(
        version = version,
        cocinadas = entradas.size,
        cocinadasUltimoMes = delMes,
        recetasDistintas = entradas.map { it.slug }.toSet().size,
        repetir = entradas.count { it.valoracion == Valoracion.REPETIR },
        ajustar = entradas.count { it.valoracion == Valoracion.AJUSTAR },
        noRepetir = entradas.count { it.valoracion == Valoracion.NO_REPETIR },
        disposicion = ajustes.disposicion,
        cocina = ajustes.cocina.valida(),
        vozActiva = ajustes.vozActiva,
        quieroHacerla = quieroHacerla,
    )
}
