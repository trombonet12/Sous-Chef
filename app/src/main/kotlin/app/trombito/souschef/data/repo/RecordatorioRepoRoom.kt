package app.trombito.souschef.data.repo

import app.trombito.souschef.data.db.MetaDao
import app.trombito.souschef.data.db.MetaEntity
import app.trombito.souschef.domain.crono.Recordatorio
import app.trombito.souschef.domain.crono.RecordatorioEmpezar
import app.trombito.souschef.domain.crono.RecordatorioRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * El recordatorio «Avísame para empezar» en la tabla `meta` (ADR-0016): «slug|empezarMs|servirMs». Borrar escribe el
 * texto vacío, que cuenta como ausente, igual que un valor ilegible.
 */
class RecordatorioRepoRoom
    @Inject
    constructor(
        private val dao: MetaDao,
    ) : RecordatorioRepo {
        override suspend fun leer(): Recordatorio? = RecordatorioEmpezar.decodificar(dao.valor(CLAVE))

        override suspend fun guardar(r: Recordatorio) = dao.fijar(MetaEntity(CLAVE, RecordatorioEmpezar.codificar(r)))

        override suspend fun borrar() {
            // Sin escribir si no había nada: el receptor lo llama también al reprogramar sin recordatorio.
            if (!dao.valor(CLAVE).isNullOrEmpty()) dao.fijar(MetaEntity(CLAVE, ""))
        }

        override fun observar(): Flow<Recordatorio?> =
            dao.observar(CLAVE).map { RecordatorioEmpezar.decodificar(it) }.distinctUntilChanged()

        companion object {
            const val CLAVE = "crono.recordatorio"
        }
    }
