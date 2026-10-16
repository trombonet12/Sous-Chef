package app.souschef.domain.crono

import app.souschef.receta.Receta
import app.souschef.receta.crono.ConsultasCrono
import app.souschef.receta.crono.EstadoCrono
import app.souschef.receta.crono.SesionCrono

enum class SituacionCarril { CORRIENDO, VENCIDO, PAUSA, SIN_ARRANCAR }

/**
 * Lo que enseña la notificación de un carril: "<nombre> · paso i/n", el texto del paso, el progreso (pasos hechos
 * sobre el total) y, si el paso está corriendo con tiempo por delante, el instante en que acaba ([finAtMs], cuenta
 * atrás nativa).
 */
data class CarrilNotificacion(
    val slug: String,
    val tituloReceta: String,
    val carril: String,
    val nombre: String,
    /** Color del carril, "#RRGGBB". */
    val color: String,
    /** Paso actual, base 1. */
    val indice: Int,
    val total: Int,
    val texto: String,
    val situacion: SituacionCarril,
    val finAtMs: Long?,
    /** Segundos que quedan del paso (0 si no corre con tiempo por delante). */
    val quedanS: Int = 0,
) {
    val titulo: String get() = "$nombre · paso $indice/$total"

    /** Pasos ya hechos (el actual no cuenta). */
    val hechos: Int get() = indice - 1

    /** Progreso sobre [tramos]: [PASO] unidades por paso hecho. Los pendientes quedan difuminados. */
    val progreso: Int get() = hechos * PASO

    /** Tiempo corto para el chip de la barra de estado: cuenta atrás "m:ss" o, sin reloj, el nombre del carril. */
    val cortoCritico: String get() = if (finAtMs != null) reloj(quedanS) else nombre

    /** Texto de la notificación: el paso y, si hace falta, en qué situación está. */
    val detalle: String
        get() =
            when (situacion) {
                SituacionCarril.CORRIENDO -> texto
                SituacionCarril.VENCIDO -> "$texto · ¡listo! pulsa Hecho"
                SituacionCarril.PAUSA -> "$texto · en pausa"
                SituacionCarril.SIN_ARRANCAR -> "$texto · sin arrancar"
            }

    val id: Int get() = IdsNotificacion.carril(carril)

    companion object {
        /** Longitud de cada tramo de la barra de progreso. */
        const val PASO = 100

        private const val SEGUNDOS_HORA = 3600
        private const val SEGUNDOS_MINUTO = 60

        /** "m:ss" o "h:mm:ss". */
        fun reloj(segundos: Int): String {
            val s = maxOf(0, segundos)
            val h = s / SEGUNDOS_HORA
            val m = s % SEGUNDOS_HORA / SEGUNDOS_MINUTO
            val resto = s % SEGUNDOS_MINUTO
            return if (h > 0) "%d:%02d:%02d".format(h, m, resto) else "%d:%02d".format(m, resto)
        }

        /**
         * Carril que se promociona a Live Update: el que acaba antes (menor `finAtMs` entre los que corren) o, si
         * ninguno tiene reloj, el primero. null si no hay carriles.
         */
        fun promocionado(carriles: List<CarrilNotificacion>): String? =
            (
                carriles.filter { it.finAtMs != null }.minByOrNull { it.finAtMs ?: Long.MAX_VALUE }
                    ?: carriles.firstOrNull()
            )?.carril
    }
}

object ConstructorCarriles {
    /** Un modelo por carril sin terminar; vacío si la cocinada no está abierta, caducó o ya terminó. */
    fun construir(
        receta: Receta,
        e: EstadoCrono,
        ahoraMs: Long,
    ): List<CarrilNotificacion> {
        val abierta = e.inicioMs != null && !SesionCrono.caducado(e, ahoraMs) && !ConsultasCrono.todoTerminado(e)
        if (!abierta) return emptyList()
        return ConsultasCrono.carriles(receta).mapNotNull { c ->
            val ec = e.carriles[c.id]?.takeUnless { it.fin } ?: return@mapNotNull null
            val pasos = receta.pasos.filter { it.carril == c.id }
            val paso = pasos.getOrNull(ec.i) ?: return@mapNotNull null
            val quedan = ConsultasCrono.quedan(e, receta, c.id, ahoraMs)
            val situacion =
                when {
                    ec.corriendo && quedan <= 0 -> SituacionCarril.VENCIDO
                    ec.corriendo -> SituacionCarril.CORRIENDO
                    ec.pausado -> SituacionCarril.PAUSA
                    else -> SituacionCarril.SIN_ARRANCAR
                }
            CarrilNotificacion(
                slug = e.slug,
                tituloReceta = receta.titulo,
                carril = c.id,
                nombre = c.nombre,
                color = c.color,
                indice = ec.i + 1,
                total = pasos.size,
                texto = paso.t,
                situacion = situacion,
                finAtMs = ec.finAtMs.takeIf { situacion == SituacionCarril.CORRIENDO },
                quedanS = if (situacion == SituacionCarril.CORRIENDO) quedan else 0,
            )
        }
    }
}
