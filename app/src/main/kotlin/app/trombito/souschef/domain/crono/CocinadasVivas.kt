package app.trombito.souschef.domain.crono

import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.SesionCrono
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Consultas de solo lectura sobre las cocinadas vivas (abiertas, sin caducar y sin terminar; la regla es
 * [SesionCrono.vivo]). Decide las novedades, la entrada al cronómetro y qué alarmas se arman.
 *
 * Sin cerrojo propio ni el de [CadenaAlarmas]: ese protege solo `activas` (la presencia es un `@Volatile` sin
 * cerrojo), que aquí no se toca. Cada lectura pasa por el cerrojo del [AlmacenCrono], que entrega documentos
 * completos; más coherencia no hace falta y pedir el de la cadena bloquearía una consulta de arranque detrás de un
 * `procesar` en curso.
 */
class CocinadasVivas(
    private val almacen: AlmacenCrono,
    private val reloj: Reloj,
) {
    /** Todas las cocinadas vivas ahora. */
    suspend fun todas(): List<EstadoCrono> {
        val ahora = reloj.ahoraMs()
        return almacen.todos().filter { SesionCrono.vivo(it, ahora) }
    }

    /** Hay alguna cocinada viva: no se debe interrumpir con nada ajeno. */
    suspend fun hayViva(): Boolean = todas().isNotEmpty()

    /** Otra cocinada viva distinta de [slug] (solo se cocina una a la vez), o null. */
    suspend fun otraViva(slug: String): EstadoCrono? = todas().firstOrNull { it.slug != slug }

    /** La cocinada de [slug] mientras está viva; null si no lo está. */
    fun viva(slug: String): Flow<EstadoCrono?> =
        almacen.observar(slug).map { e -> e?.takeIf { SesionCrono.vivo(it, reloj.ahoraMs()) } }
}
