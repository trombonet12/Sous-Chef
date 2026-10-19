package app.souschef.data.repo

import app.souschef.data.db.MetaDao
import app.souschef.data.db.MetaEntity
import app.souschef.domain.Ajustes
import app.souschef.domain.AjustesRepo
import app.souschef.domain.DisposicionCrono
import app.souschef.domain.DisposicionLista
import app.souschef.domain.HoraTarjeta
import app.souschef.domain.OrdenRecetas
import app.souschef.domain.TemaApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject

/** Ajustes globales guardados en la tabla `meta`; un valor desconocido o corrupto vuelve al defecto. */
class AjustesRepoRoom
    @Inject
    constructor(
        private val dao: MetaDao,
    ) : AjustesRepo {
        override fun observar(): Flow<Ajustes> =
            combine(CLAVES.map(dao::observar)) { v ->
                val defecto = Ajustes()
                Ajustes(
                    disposicion = enumDe(v[0], defecto.disposicion),
                    vozActiva =
                        when (v[1]) {
                            "true" -> true
                            "false" -> false
                            else -> defecto.vozActiva
                        },
                    lista = enumDe(v[2], defecto.lista),
                    orden = enumDe(v[3], defecto.orden),
                    tema = enumDe(v[4], defecto.tema),
                    servirMin = minutosDe(v[5]) ?: defecto.servirMin,
                    horaTarjeta = enumDe(v[6], defecto.horaTarjeta),
                )
            }.distinctUntilChanged()

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

        companion object {
            const val CLAVE_DISPOSICION = "ajustes.crono.disposicion"
            const val CLAVE_VOZ = "ajustes.voz.activa"
            const val CLAVE_LISTA = "ajustes.recetas.disposicion"
            const val CLAVE_ORDEN = "ajustes.recetas.orden"
            const val CLAVE_TEMA = "ajustes.tema"
            const val CLAVE_SERVIR = "ajustes.servir"
            const val CLAVE_HORA = "ajustes.recetas.hora"
            private const val MINUTOS_DIA = 24 * 60

            /** En el orden en que los lee [observar]. */
            private val CLAVES =
                listOf(
                    CLAVE_DISPOSICION,
                    CLAVE_VOZ,
                    CLAVE_LISTA,
                    CLAVE_ORDEN,
                    CLAVE_TEMA,
                    CLAVE_SERVIR,
                    CLAVE_HORA,
                )

            private inline fun <reified E : Enum<E>> enumDe(
                valor: String?,
                defecto: E,
            ): E = enumValues<E>().firstOrNull { it.name == valor } ?: defecto

            /** «HH:mm» a minutos del día; null si no se entiende. */
            private fun minutosDe(valor: String?): Int? =
                valor?.let { runCatching { LocalTime.parse(it) }.getOrNull() }?.let { it.hour * 60 + it.minute }
        }
    }
