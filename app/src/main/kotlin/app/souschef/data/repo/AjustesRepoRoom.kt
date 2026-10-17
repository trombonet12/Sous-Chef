package app.souschef.data.repo

import app.souschef.data.db.MetaDao
import app.souschef.data.db.MetaEntity
import app.souschef.domain.Ajustes
import app.souschef.domain.AjustesRepo
import app.souschef.domain.DisposicionCrono
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
            combine(dao.observar(CLAVE_DISPOSICION), dao.observar(CLAVE_VOZ)) { disposicion, voz ->
                val defecto = Ajustes()
                val disp = DisposicionCrono.entries.firstOrNull { it.name == disposicion }
                Ajustes(
                    disposicion = disp ?: defecto.disposicion,
                    vozActiva =
                        when (voz) {
                            "true" -> true
                            "false" -> false
                            else -> defecto.vozActiva
                        },
                )
            }.distinctUntilChanged()

        override suspend fun fijarDisposicion(d: DisposicionCrono) = dao.fijar(MetaEntity(CLAVE_DISPOSICION, d.name))

        override suspend fun fijarVoz(activa: Boolean) = dao.fijar(MetaEntity(CLAVE_VOZ, activa.toString()))

        companion object {
            const val CLAVE_DISPOSICION = "ajustes.crono.disposicion"
            const val CLAVE_VOZ = "ajustes.voz.activa"
        }
    }
