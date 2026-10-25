package app.trombito.souschef.data.repo

import app.trombito.souschef.data.db.MetaDao
import app.trombito.souschef.data.db.MetaEntity
import app.trombito.souschef.domain.ContadorPropuestas
import app.trombito.souschef.domain.OtraDelDia
import app.trombito.souschef.domain.PropuestasRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** Contadores de «¿Qué cocino hoy?» en la tabla `meta`; un valor ilegible cuenta como ausente. */
class PropuestasRepoRoom
    @Inject
    constructor(
        private val dao: MetaDao,
    ) : PropuestasRepo {
        override fun observarOtra(): Flow<OtraDelDia?> =
            dao.observar(CLAVE_OTRA).map { ContadorPropuestas.otraDe(it) }.distinctUntilChanged()

        override suspend fun otra(hoy: LocalDate) {
            val nuevo = ContadorPropuestas.siguienteOtra(dao.valor(CLAVE_OTRA), hoy)
            dao.fijar(MetaEntity(CLAVE_OTRA, nuevo))
        }

        override suspend fun mostrar(
            hoy: LocalDate,
            slugs: List<String>,
        ) {
            val actual = dao.valor(CLAVE_MOSTRADAS)
            val nuevo = ContadorPropuestas.mostrar(actual, hoy, slugs)
            // Solo se escribe si cambia algo: la lista se vuelve a calcular cada vez que cambia el estado.
            if (nuevo != actual) dao.fijar(MetaEntity(CLAVE_MOSTRADAS, nuevo))
        }

        override fun observarCocinadas(): Flow<Int> =
            dao.observar(CLAVE_COCINADAS).map { it?.toIntOrNull() ?: 0 }.distinctUntilChanged()

        companion object {
            /** «yyyy-MM-dd|N»: pulsaciones de «Otra» del día; otro día = vuelve a empezar. */
            const val CLAVE_OTRA = "recetas.propuestas.otra"

            /** «yyyy-MM-dd|slug,slug»: lo que se ha propuesto ese día. */
            const val CLAVE_MOSTRADAS = "recetas.propuestas.mostradas"

            /** Veces que se cocinó una receta propuesta ese día. */
            const val CLAVE_COCINADAS = "recetas.propuestas.cocinadas"
        }
    }

/**
 * Si [slug], cocinada en [fecha], estaba entre las propuestas de ese día, suma 1 a las cocinadas propuestas. Se llama
 * dentro de la transacción de guardar la cocinada.
 */
internal suspend fun MetaDao.contarSiEraPropuesta(
    slug: String,
    fecha: String,
) {
    if (!ContadorPropuestas.eraPropuesta(valor(PropuestasRepoRoom.CLAVE_MOSTRADAS), slug, fecha)) return
    val n = valor(PropuestasRepoRoom.CLAVE_COCINADAS)?.toIntOrNull() ?: 0
    fijar(MetaEntity(PropuestasRepoRoom.CLAVE_COCINADAS, (n + 1).toString()))
}
