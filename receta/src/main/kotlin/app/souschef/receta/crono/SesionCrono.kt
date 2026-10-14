package app.souschef.receta.crono

import app.souschef.receta.Receta
import kotlin.math.abs

/** Ciclo de vida de la cocinada: creación, apertura, caducidad, checklist y cierre (§4.1, §4.5, §4.13). */
object SesionCrono {
    const val CADUCIDAD_MS = 12 * 3_600_000L
    private const val MAX_MIN_REALES = 720
    private const val FNV_BASE = -0x7ee3623b
    private const val FNV_PRIMO = 0x01000193

    fun inicial(
        receta: Receta,
        servirMs: Long,
        ahoraMs: Long,
        raciones: Int = receta.raciones,
    ): EstadoCrono {
        val carriles = Plano(receta).carriles
        return EstadoCrono(
            slug = receta.slug,
            raciones = raciones,
            servirMs = servirMs,
            carriles = carriles.associate { it.id to EstadoCarril() },
            foco = carriles.firstOrNull()?.id,
            actualizadoMs = ahoraMs,
        )
    }

    /** REINICIAR: borra todo el progreso y vuelve a la portada, conservando raciones y hora de servir. */
    fun reiniciar(
        e: EstadoCrono,
        receta: Receta,
        ahoraMs: Long,
    ): EstadoCrono = inicial(receta, e.servirMs, ahoraMs, e.raciones)

    /**
     * "Abrir la cocina": fija el inicio, arranca el cronometraje de cada paso y marca como avisados los carriles
     * cuya hora ya pasó (para que no suene todo a la vez). Elige el foco.
     */
    fun abrir(
        e: EstadoCrono,
        receta: Receta,
        ahoraMs: Long,
    ): EstadoCrono {
        val plano = Plano(receta)
        val carriles =
            e.carriles.mapValues { (id, ec) ->
                val p = plano.pasoActual(e, id)
                if (p == null) {
                    ec
                } else {
                    ec.copy(
                        inicioPasoMs = ec.inicioPasoMs ?: ahoraMs,
                        avisado = ec.avisado || plano.sugeridoMs(e, p) <= ahoraMs,
                    )
                }
            }
        val abierto = e.copy(carriles = carriles, inicioMs = e.inicioMs ?: ahoraMs, actualizadoMs = ahoraMs)
        val preferido =
            abierto.foco?.takeIf {
                ConsultasCrono.vista(plano, abierto, it, ahoraMs).clase.rango <= ClaseEstado.ESPERA.rango
            }
        val foco = preferido ?: ConsultasCrono.siguienteAccionable(abierto, receta, ahoraMs) ?: abierto.foco
        return abierto.copy(foco = foco)
    }

    fun servir(
        e: EstadoCrono,
        servirMs: Long,
        ahoraMs: Long,
    ): EstadoCrono = e.copy(servirMs = servirMs, actualizadoMs = ahoraMs)

    fun enfocar(
        e: EstadoCrono,
        carril: String,
        ahoraMs: Long,
    ): EstadoCrono = if (carril in e.carriles) e.copy(foco = carril, actualizadoMs = ahoraMs) else e

    fun marcar(
        e: EstadoCrono,
        idCasilla: String,
        marcada: Boolean,
        ahoraMs: Long,
    ): EstadoCrono =
        e.copy(
            marcados = if (marcada) e.marcados + idCasilla else e.marcados - idCasilla,
            actualizadoMs = ahoraMs,
        )

    /** Id estable por contenido (FNV-1a de la línea), no por posición: reordenar la receta no desmarca. */
    fun idCasilla(linea: String): String {
        var h = FNV_BASE
        for (b in linea.trim().toByteArray(Charsets.UTF_8)) {
            h = (h xor (b.toInt() and 0xFF)) * FNV_PRIMO
        }
        return "mep-" + Integer.toHexString(h).padStart(8, '0')
    }

    /** Pasadas 12 h desde la última actualización el estado se descarta entero. */
    fun caducado(
        e: EstadoCrono,
        ahoraMs: Long,
    ): Boolean = ahoraMs - e.actualizadoMs > CADUCIDAD_MS

    /** El estado si sigue vigente (y es de esta receta); si no, null: no se restaura nada. */
    fun vigente(
        e: EstadoCrono?,
        receta: Receta,
        ahoraMs: Long,
    ): EstadoCrono? = e?.takeIf { it.slug == receta.slug && !caducado(it, ahoraMs) }

    /** Datos del cierre (§4.13). `ahoraMs` es el instante de servir. */
    fun resumen(
        e: EstadoCrono,
        receta: Receta,
        ahoraMs: Long,
    ): ResumenCrono {
        val plano = Plano(receta)
        val dur = e.inicioMs?.let { redondeaS(ahoraMs - it) }
        val minutos = dur?.let { Math.floorDiv(it + 30, 60L).toInt() }?.takeIf { it in 1..MAX_MIN_REALES }
        val reales =
            receta.pasos
                .filter { it.id in e.pasosReales }
                .map { PasoReal(it.id, it.t, e.pasosReales.getValue(it.id)) }
        val desvios =
            reales
                .map { r -> Desvio(r.id, r.t, receta.paso(r.id)?.segundos ?: r.segundos, r.segundos) }
                .filter { it.difS != 0 }
                .sortedByDescending { abs(it.difS) }
        val carriles =
            plano.carriles.map { c ->
                val ps = plano.colas.getValue(c.id)
                val s = ps.sumOf { e.pasosReales[it.id] ?: 0 }
                ResumenCarril(c.nombre, ps.size, Math.floorDiv(s + 30, 60), c.tipo == "fuego")
            }
        return ResumenCrono(dur, minutos, ahoraMs, e.servirMs, reales, desvios, carriles)
    }
}
