package app.trombito.souschef.data.repo

import app.trombito.souschef.data.db.MetaDao
import app.trombito.souschef.data.db.MetaEntity
import app.trombito.souschef.domain.resena.ReglaResena
import app.trombito.souschef.domain.resena.ResenaRepo
import javax.inject.Inject

/** «Ya se pidió la reseña» en la tabla `meta`; cualquier valor presente cuenta como pedida. */
class ResenaRepoRoom
    @Inject
    constructor(
        private val dao: MetaDao,
    ) : ResenaRepo {
        override suspend fun yaPedida(): Boolean = dao.valor(ReglaResena.CLAVE_PEDIDA) != null

        override suspend fun marcarPedida() = dao.fijar(MetaEntity(ReglaResena.CLAVE_PEDIDA, "1"))
    }
