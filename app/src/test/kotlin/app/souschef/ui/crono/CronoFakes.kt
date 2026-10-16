package app.souschef.ui.crono

import app.souschef.domain.CronoEstado
import app.souschef.domain.CronoEstadoRepo
import app.souschef.domain.Reloj
import app.souschef.domain.crono.AlarmaSistema
import app.souschef.domain.crono.AvisoNotificacion
import app.souschef.domain.crono.CarrilNotificacion
import app.souschef.domain.crono.Notificador
import app.souschef.receta.Carril
import app.souschef.receta.Paso
import app.souschef.receta.Receta
import app.souschef.receta.crono.TipoAviso
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

/** Hoy a las 12:00 en la zona del sistema: fecha fija para que los tests no dependan del día. */
val T0: Long =
    LocalDate
        .of(2026, 10, 14)
        .atTime(12, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

class RelojFalso(
    var ms: Long = T0,
) : Reloj {
    override fun ahoraMs(): Long = ms
}

class FakeCronoEstadoRepo : CronoEstadoRepo {
    val datos = MutableStateFlow<Map<String, CronoEstado>>(emptyMap())

    override fun observar(slug: String): Flow<CronoEstado?> = datos.map { it[slug] }

    override suspend fun obtener(slug: String): CronoEstado? = datos.value[slug]

    override suspend fun todos(): List<CronoEstado> = datos.value.values.toList()

    override suspend fun guardar(
        slug: String,
        json: String,
    ) {
        datos.value = datos.value + (slug to CronoEstado(slug, json, 0L))
    }

    override suspend fun borrar(slug: String) {
        datos.value = datos.value - slug
    }
}

class FakeAlarmaSistema : AlarmaSistema {
    var programada: Long? = null
    var cancelaciones = 0

    override fun programar(ms: Long) {
        programada = ms
    }

    override fun cancelar() {
        programada = null
        cancelaciones++
    }
}

class FakeNotificador : Notificador {
    val publicados = mutableListOf<AvisoNotificacion>()
    val carrilesPorSlug = mutableMapOf<String, List<CarrilNotificacion>>()
    val canceladas = mutableListOf<Pair<String, Set<String>>>()
    val limpiados = mutableListOf<String>()

    override fun carriles(
        slug: String,
        carriles: List<CarrilNotificacion>,
    ) {
        if (carriles.isEmpty()) carrilesPorSlug.remove(slug) else carrilesPorSlug[slug] = carriles
    }

    override fun cancelarAvisos(
        slug: String,
        carriles: Collection<String>,
    ) {
        canceladas += slug to carriles.toSet()
    }

    val silenciados = mutableListOf<String>()

    override fun silenciar(slug: String) {
        silenciados += slug
    }

    override fun limpiar(slug: String) {
        limpiados += slug
        carrilesPorSlug.remove(slug)
    }

    override fun publicar(avisos: List<AvisoNotificacion>) {
        publicados += avisos
    }
}

class FakeAvisador : Avisador {
    val sonados = mutableListOf<TipoAviso>()
    var cierres = 0

    override fun sonar(tipo: TipoAviso) {
        sonados += tipo
    }

    override fun resumen() = Unit

    override fun cierre() {
        cierres++
    }
}

/** Fuego f0 con dos pasos (5 + 10 min) y una mesa m con uno (2 min); 15 min en total. */
fun recetaCrono(slug: String = "r") =
    Receta(
        slug = slug,
        titulo = "Receta $slug",
        totalMin = 15.0,
        carriles = listOf(Carril("f0", tipo = "fuego"), Carril("m", tipo = "mesa")),
        pasos =
            listOf(
                Paso("p1", "Sofrito", "f0", 0.0, 5.0, f = 5),
                Paso("p2", "Hervir", "f0", 5.0, 10.0, f = 9),
                Paso("p3", "Cortar", "m", 0.0, 2.0),
            ),
    )
