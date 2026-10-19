package app.souschef.data.repo

import app.souschef.data.db.MetaDao
import app.souschef.data.db.MetaEntity
import app.souschef.domain.Ajustes
import app.souschef.domain.AjustesRepo
import app.souschef.domain.DisposicionCrono
import app.souschef.domain.DisposicionLista
import app.souschef.domain.OrdenRecetas
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/** Ajustes globales guardados en la tabla `meta`; un valor desconocido o corrupto vuelve al defecto. */
class AjustesRepoRoom
    @Inject
    constructor(
        private val dao: MetaDao,
    ) : AjustesRepo {
        override fun observar(): Flow<Ajustes> =
            combine(
                dao.observar(CLAVE_DISPOSICION),
                dao.observar(CLAVE_VOZ),
                dao.observar(CLAVE_LISTA),
                dao.observar(CLAVE_ORDEN),
            ) { disposicion, voz, lista, orden ->
                val defecto = Ajustes()
                Ajustes(
                    disposicion =
                        DisposicionCrono.entries.firstOrNull {
                            it.name == disposicion
                        } ?: defecto.disposicion,
                    vozActiva =
                        when (voz) {
                            "true" -> true
                            "false" -> false
                            else -> defecto.vozActiva
                        },
                    lista = DisposicionLista.entries.firstOrNull { it.name == lista } ?: defecto.lista,
                    orden = OrdenRecetas.entries.firstOrNull { it.name == orden } ?: defecto.orden,
                )
            }.distinctUntilChanged()

        override suspend fun fijarDisposicion(d: DisposicionCrono) = dao.fijar(MetaEntity(CLAVE_DISPOSICION, d.name))

        override suspend fun fijarVoz(activa: Boolean) = dao.fijar(MetaEntity(CLAVE_VOZ, activa.toString()))

        override suspend fun fijarLista(d: DisposicionLista) = dao.fijar(MetaEntity(CLAVE_LISTA, d.name))

        override suspend fun fijarOrden(o: OrdenRecetas) = dao.fijar(MetaEntity(CLAVE_ORDEN, o.name))

        companion object {
            const val CLAVE_DISPOSICION = "ajustes.crono.disposicion"
            const val CLAVE_VOZ = "ajustes.voz.activa"
            const val CLAVE_LISTA = "ajustes.recetas.disposicion"
            const val CLAVE_ORDEN = "ajustes.recetas.orden"
        }
    }
