package app.trombito.souschef.data.repo

import app.trombito.souschef.data.db.MetaDao
import app.trombito.souschef.data.db.MetaEntity
import app.trombito.souschef.domain.copia.CopiaAutoRepo
import app.trombito.souschef.domain.copia.EstadoCopiaAuto
import app.trombito.souschef.domain.copia.FalloCopiaAuto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * El estado de la copia automática en la tabla `meta` (ADR-0015): carpeta (URI del árbol SAF, privada), última copia
 * automática y manual, y último fallo. El texto vacío cuenta como ausente. Lee todas las claves en una sola consulta.
 */
class CopiaAutoRepoRoom
    @Inject
    constructor(
        private val dao: MetaDao,
    ) : CopiaAutoRepo {
        override fun observar(): Flow<EstadoCopiaAuto> =
            dao
                .observarVarias(EstadoCopiaAuto.CLAVES)
                .map { filas -> EstadoCopiaAuto.decodificar(filas.associate { it.clave to it.valor }) }
                .distinctUntilChanged()

        override suspend fun leer(): EstadoCopiaAuto = observar().first()

        override suspend fun fijarCarpeta(carpeta: String?) =
            dao.fijarVarias(
                listOf(
                    MetaEntity(EstadoCopiaAuto.CLAVE_CARPETA, carpeta.orEmpty()),
                    MetaEntity(EstadoCopiaAuto.CLAVE_FALLO, ""),
                ),
            )

        override suspend fun registrarCopia(ms: Long) =
            dao.fijarVarias(
                listOf(
                    MetaEntity(EstadoCopiaAuto.CLAVE_ULTIMA_AUTO, ms.toString()),
                    MetaEntity(EstadoCopiaAuto.CLAVE_FALLO, ""),
                ),
            )

        override suspend fun registrarFallo(fallo: FalloCopiaAuto) =
            dao.fijar(MetaEntity(EstadoCopiaAuto.CLAVE_FALLO, fallo.name))

        override suspend fun registrarManual(ms: Long) =
            dao.fijar(MetaEntity(EstadoCopiaAuto.CLAVE_ULTIMA_MANUAL, ms.toString()))
    }
