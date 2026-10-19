package app.souschef.ui.crono

import app.souschef.domain.Ajustes
import app.souschef.domain.AjustesRepo
import app.souschef.domain.CronoEstado
import app.souschef.domain.CronoEstadoRepo
import app.souschef.domain.DisposicionCrono
import app.souschef.domain.DisposicionLista
import app.souschef.domain.HoraTarjeta
import app.souschef.domain.OrdenRecetas
import app.souschef.domain.Reloj
import app.souschef.domain.TemaApp
import app.souschef.domain.crono.AlarmaSistema
import app.souschef.domain.crono.CarrilNotificacion
import app.souschef.domain.crono.Notificador
import app.souschef.domain.crono.ResumenCocina
import app.souschef.domain.voz.Escucha
import app.souschef.domain.voz.Habla
import app.souschef.receta.Carril
import app.souschef.receta.Paso
import app.souschef.receta.Receta
import app.souschef.receta.crono.AlarmaActiva
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

class FakeAjustesRepo : AjustesRepo {
    val actual = MutableStateFlow(Ajustes())

    override fun observar(): Flow<Ajustes> = actual

    override suspend fun fijarDisposicion(d: DisposicionCrono) {
        actual.value = actual.value.copy(disposicion = d)
    }

    override suspend fun fijarVoz(activa: Boolean) {
        actual.value = actual.value.copy(vozActiva = activa)
    }

    override suspend fun fijarLista(d: DisposicionLista) {
        actual.value = actual.value.copy(lista = d)
    }

    override suspend fun fijarOrden(o: OrdenRecetas) {
        actual.value = actual.value.copy(orden = o)
    }

    override suspend fun fijarTema(t: TemaApp) {
        actual.value = actual.value.copy(tema = t)
    }

    override suspend fun fijarServir(minutos: Int) {
        actual.value = actual.value.copy(servirMin = minutos)
    }

    override suspend fun fijarHoraTarjeta(h: HoraTarjeta) {
        actual.value = actual.value.copy(horaTarjeta = h)
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

/** Una llamada a [Notificador.alarma]. */
data class AlarmaPublicada(
    val slug: String,
    val titulo: String,
    val alarma: AlarmaActiva,
    val sonar: Boolean,
    val urgente: String?,
)

class FakeNotificador : Notificador {
    /** Lo que hay ahora en la bandeja fija, por cocinada. */
    val cocinas = mutableMapOf<String, ResumenCocina>()
    val carrilesPorSlug: Map<String, List<CarrilNotificacion>> get() = cocinas.mapValues { it.value.carriles }

    /** Historial de alarmas publicadas o actualizadas, en orden. */
    val alarmas = mutableListOf<AlarmaPublicada>()

    /** La alarma que hay ahora en la bandeja, por cocinada. */
    val alarmaActual = mutableMapOf<String, AlarmaPublicada>()
    val alarmasQuitadas = mutableListOf<String>()
    val cocinasQuitadas = mutableListOf<String>()
    val limpiados = mutableListOf<String>()

    override fun cocina(resumen: ResumenCocina) {
        cocinas[resumen.slug] = resumen
    }

    override fun alarma(
        slug: String,
        titulo: String,
        alarma: AlarmaActiva,
        sonar: Boolean,
        urgente: String?,
    ) {
        val publicada = AlarmaPublicada(slug, titulo, alarma, sonar, urgente)
        alarmas += publicada
        alarmaActual[slug] = publicada
    }

    override fun quitarAlarma(slug: String) {
        alarmasQuitadas += slug
        alarmaActual.remove(slug)
    }

    override fun quitarCocina(slug: String) {
        cocinasQuitadas += slug
        cocinas.remove(slug)
    }

    override fun limpiar(slug: String) {
        limpiados += slug
        cocinas.remove(slug)
        alarmaActual.remove(slug)
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

class FakeEscucha(
    override val disponible: Boolean = true,
) : Escucha {
    var empezadas = 0
    var paradas = 0
    private var alDetenerse: (Boolean) -> Unit = {}

    override fun empezar(
        alOir: (List<String>) -> Unit,
        alDetenerse: (Boolean) -> Unit,
    ) {
        empezadas++
        this.alDetenerse = alDetenerse
    }

    /** Simula que el bucle se detiene solo (permiso o idioma). */
    fun detenerseSolo(porPermiso: Boolean) = alDetenerse(porPermiso)

    override fun pausar() = Unit

    override fun reanudar() = Unit

    override fun parar() {
        paradas++
    }
}

class FakeHabla : Habla {
    val dichas = mutableListOf<String>()
    var liberada = 0

    override fun decir(
        texto: String,
        alAcabar: () -> Unit,
    ) {
        dichas += texto
        alAcabar()
    }

    override fun callar() = Unit

    override fun liberar() {
        liberada++
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
