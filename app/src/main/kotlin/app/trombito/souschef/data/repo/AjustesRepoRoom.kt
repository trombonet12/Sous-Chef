package app.trombito.souschef.data.repo

import app.trombito.souschef.data.db.MetaDao
import app.trombito.souschef.data.db.MetaEntity
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.FUEGOS_MAX
import app.trombito.souschef.domain.FUEGOS_MIN
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.domain.OrdenRecetas
import app.trombito.souschef.domain.RACIONES_MAX
import app.trombito.souschef.domain.RACIONES_MIN
import app.trombito.souschef.domain.TemaApp
import app.trombito.souschef.receta.Cocina
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject

/** Ajustes globales guardados en la tabla `meta`; un valor desconocido o corrupto vuelve al defecto. */
@Suppress("TooManyFunctions") // un setter por ajuste
class AjustesRepoRoom
    @Inject
    constructor(
        private val dao: MetaDao,
    ) : AjustesRepo {
        override fun observar(): Flow<Ajustes> = combine(CLAVES.map(dao::observar)) { leer(it) }.distinctUntilChanged()

        private fun leer(v: Array<String?>): Ajustes {
            val defecto = Ajustes()
            return Ajustes(
                disposicion = enumDe(v[I_DISPOSICION], defecto.disposicion),
                vozActiva = booleanoDe(v[I_VOZ], defecto.vozActiva),
                lista = enumDe(v[I_LISTA], defecto.lista),
                orden = enumDe(v[I_ORDEN], defecto.orden),
                tema = enumDe(v[I_TEMA], defecto.tema),
                servirMin = minutosDe(v[I_SERVIR]) ?: defecto.servirMin,
                horaTarjeta = enumDe(v[I_HORA], defecto.horaTarjeta),
                cocina = cocinaDe(v[I_COCINA_TIPO], v[I_COCINA_MANDO], v[I_COCINA_MAX]),
                fuegos = enteroDe(v[I_FUEGOS], FUEGOS_MIN..FUEGOS_MAX, defecto.fuegos),
                horno = booleanoDe(v[I_HORNO], defecto.horno),
                racionesDefecto = enteroDe(v[I_RACIONES], RACIONES_MIN..RACIONES_MAX, defecto.racionesDefecto),
                introVista = booleanoDe(v[I_INTRO], defecto.introVista),
                guiaCronoVista = booleanoDe(v[I_GUIA], defecto.guiaCronoVista),
            )
        }

        override suspend fun fijarDisposicion(d: DisposicionCrono) = dao.fijar(MetaEntity(CLAVE_DISPOSICION, d.name))

        override suspend fun fijarVoz(activa: Boolean) = dao.fijar(MetaEntity(CLAVE_VOZ, activa.toString()))

        override suspend fun fijarLista(d: DisposicionLista) = dao.fijar(MetaEntity(CLAVE_LISTA, d.name))

        override suspend fun fijarOrden(o: OrdenRecetas) = dao.fijar(MetaEntity(CLAVE_ORDEN, o.name))

        override suspend fun fijarTema(t: TemaApp) = dao.fijar(MetaEntity(CLAVE_TEMA, t.name))

        override suspend fun fijarServir(minutos: Int) {
            val m = minutos.mod(MINUTOS_DIA)
            dao.fijar(MetaEntity(CLAVE_SERVIR, String.format(Locale.ROOT, "%02d:%02d", m / 60, m % 60)))
        }

        override suspend fun fijarHoraTarjeta(h: HoraTarjeta) = dao.fijar(MetaEntity(CLAVE_HORA, h.name))

        override suspend fun fijarCocina(c: Cocina) {
            val v = c.valida()
            dao.fijar(MetaEntity(CLAVE_COCINA_TIPO, v.tipo.name))
            dao.fijar(MetaEntity(CLAVE_COCINA_MANDO, v.mando.name))
            dao.fijar(MetaEntity(CLAVE_COCINA_MAX, v.max?.toString().orEmpty()))
        }

        override suspend fun fijarFuegos(n: Int) =
            dao.fijar(MetaEntity(CLAVE_FUEGOS, n.coerceIn(FUEGOS_MIN, FUEGOS_MAX).toString()))

        override suspend fun fijarHorno(tiene: Boolean) = dao.fijar(MetaEntity(CLAVE_HORNO, tiene.toString()))

        override suspend fun fijarRacionesDefecto(n: Int) =
            dao.fijar(MetaEntity(CLAVE_RACIONES, n.coerceIn(RACIONES_MIN, RACIONES_MAX).toString()))

        override suspend fun marcarIntroVista(vista: Boolean) = dao.fijar(MetaEntity(CLAVE_INTRO, vista.toString()))

        override suspend fun marcarGuiaCronoVista(vista: Boolean) = dao.fijar(MetaEntity(CLAVE_GUIA, vista.toString()))

        companion object {
            const val CLAVE_DISPOSICION = "ajustes.crono.disposicion"
            const val CLAVE_VOZ = "ajustes.voz.activa"
            const val CLAVE_LISTA = "ajustes.recetas.disposicion"
            const val CLAVE_ORDEN = "ajustes.recetas.orden"
            const val CLAVE_TEMA = "ajustes.tema"
            const val CLAVE_SERVIR = "ajustes.servir"
            const val CLAVE_HORA = "ajustes.recetas.hora"
            const val CLAVE_COCINA_TIPO = "ajustes.cocina.tipo"
            const val CLAVE_COCINA_MANDO = "ajustes.cocina.mando"

            /** "" = mando sin números. */
            const val CLAVE_COCINA_MAX = "ajustes.cocina.max"
            const val CLAVE_FUEGOS = "ajustes.cocina.fuegos"
            const val CLAVE_HORNO = "ajustes.cocina.horno"
            const val CLAVE_RACIONES = "ajustes.raciones.defecto"
            const val CLAVE_INTRO = "ajustes.intro.vista"
            const val CLAVE_GUIA = "ajustes.guia.crono.vista"
            private const val MINUTOS_DIA = 24 * 60

            /** En el orden en que los lee [observar]; los índices `I_*` son sus posiciones. */
            private val CLAVES =
                listOf(
                    CLAVE_DISPOSICION,
                    CLAVE_VOZ,
                    CLAVE_LISTA,
                    CLAVE_ORDEN,
                    CLAVE_TEMA,
                    CLAVE_SERVIR,
                    CLAVE_HORA,
                    CLAVE_COCINA_TIPO,
                    CLAVE_COCINA_MANDO,
                    CLAVE_COCINA_MAX,
                    CLAVE_FUEGOS,
                    CLAVE_HORNO,
                    CLAVE_RACIONES,
                    CLAVE_INTRO,
                    CLAVE_GUIA,
                )
            private const val I_DISPOSICION = 0
            private const val I_VOZ = 1
            private const val I_LISTA = 2
            private const val I_ORDEN = 3
            private const val I_TEMA = 4
            private const val I_SERVIR = 5
            private const val I_HORA = 6
            private const val I_COCINA_TIPO = 7
            private const val I_COCINA_MANDO = 8
            private const val I_COCINA_MAX = 9
            private const val I_FUEGOS = 10
            private const val I_HORNO = 11
            private const val I_RACIONES = 12
            private const val I_INTRO = 13
            private const val I_GUIA = 14

            private fun booleanoDe(
                valor: String?,
                defecto: Boolean,
            ): Boolean =
                when (valor) {
                    "true" -> true
                    "false" -> false
                    else -> defecto
                }

            /** Entero en [rango] (si se sale, se ajusta); no numérico o ausente, el defecto. */
            private fun enteroDe(
                valor: String?,
                rango: IntRange,
                defecto: Int,
            ): Int = valor?.toIntOrNull()?.coerceIn(rango.first, rango.last) ?: defecto

            /** Siempre pasa por `valida()`. `max` "" = sin números; ausente o no numérico, el defecto (9). */
            private fun cocinaDe(
                tipo: String?,
                mando: String?,
                max: String?,
            ): Cocina {
                val defecto = Cocina.VITRO_9
                return Cocina(
                    tipo = enumDe(tipo, defecto.tipo),
                    mando = enumDe(mando, defecto.mando),
                    max = if (max == "") null else max?.toIntOrNull() ?: defecto.max,
                ).valida()
            }

            private inline fun <reified E : Enum<E>> enumDe(
                valor: String?,
                defecto: E,
            ): E = enumValues<E>().firstOrNull { it.name == valor } ?: defecto

            /** «HH:mm» a minutos del día; null si no se entiende. */
            private fun minutosDe(valor: String?): Int? =
                valor?.let { runCatching { LocalTime.parse(it) }.getOrNull() }?.let { it.hour * 60 + it.minute }
        }
    }
