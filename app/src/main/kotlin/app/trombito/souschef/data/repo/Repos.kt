package app.trombito.souschef.data.repo

import androidx.room.withTransaction
import app.trombito.souschef.data.db.CronoEstadoDao
import app.trombito.souschef.data.db.CronoEstadoEntity
import app.trombito.souschef.data.db.HistorialDao
import app.trombito.souschef.data.db.PreferenciaDao
import app.trombito.souschef.data.db.PreferenciaEntity
import app.trombito.souschef.data.db.RecetaDao
import app.trombito.souschef.data.db.SousChefDatabase
import app.trombito.souschef.data.db.TecnicaDao
import app.trombito.souschef.domain.CronoEstado
import app.trombito.souschef.domain.CronoEstadoRepo
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.HistorialRepo
import app.trombito.souschef.domain.Preferencia
import app.trombito.souschef.domain.PreferenciasRepo
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.domain.TecnicasRepo
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tecnica
import app.trombito.souschef.receta.leerReceta
import app.trombito.souschef.receta.leerTecnica
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecetasRepoRoom
    @Inject
    constructor(
        private val dao: RecetaDao,
    ) : RecetasRepo {
        override fun observarTodas(): Flow<List<Receta>> = dao.observarTodas().map { l -> l.map { leerReceta(it.doc) } }

        override fun observar(slug: String): Flow<Receta?> =
            dao.observar(slug).map { e -> e?.let { leerReceta(it.doc) } }

        override suspend fun obtener(slug: String): Receta? = dao.obtener(slug)?.let { leerReceta(it.doc) }
    }

class TecnicasRepoRoom
    @Inject
    constructor(
        private val dao: TecnicaDao,
    ) : TecnicasRepo {
        override fun observarTodas(): Flow<List<Tecnica>> =
            dao.observarTodas().map { l ->
                l.map { leerTecnica(it.doc) }
            }

        override fun observar(slug: String): Flow<Tecnica?> =
            dao.observar(slug).map { e -> e?.let { leerTecnica(it.doc) } }

        override suspend fun obtener(slug: String): Tecnica? = dao.obtener(slug)?.let { leerTecnica(it.doc) }
    }

class HistorialRepoRoom
    @Inject
    constructor(
        private val dao: HistorialDao,
    ) : HistorialRepo {
        override fun observarTodo(): Flow<List<EntradaHistorial>> =
            dao.observarTodo().map { l ->
                l.map { it.aDominio() }
            }

        override fun observarUltimaFechaPorSlug(): Flow<Map<String, String>> =
            dao.observarUltimaFecha().map { l -> l.associate { it.slug to it.fecha } }

        override suspend fun agregar(entrada: EntradaHistorial) = dao.insertar(entrada.aEntidad())

        override suspend fun importar(entradas: List<EntradaHistorial>): Int =
            dao.insertarNuevas(entradas.map { it.aEntidad() }).count { it != -1L }

        override suspend fun borrar(id: String) = dao.borrar(id)
    }

class PreferenciasRepoRoom
    @Inject
    constructor(
        private val db: SousChefDatabase,
        private val dao: PreferenciaDao,
    ) : PreferenciasRepo {
        override fun observarTodas(): Flow<Map<String, Preferencia>> =
            dao.observarTodas().map { l -> l.associate { it.slug to Preferencia(it.slug, it.favorito, it.raciones) } }

        override fun observar(slug: String): Flow<Preferencia?> =
            dao.observar(slug).map { e -> e?.let { Preferencia(it.slug, it.favorito, it.raciones) } }

        override suspend fun fijarFavorito(
            slug: String,
            favorito: Boolean,
        ) = db.withTransaction {
            val actual = dao.obtener(slug) ?: PreferenciaEntity(slug, false, null)
            dao.insertar(actual.copy(favorito = favorito))
        }

        override suspend fun fijarRaciones(
            slug: String,
            raciones: Int,
        ) = db.withTransaction {
            val actual = dao.obtener(slug) ?: PreferenciaEntity(slug, false, null)
            dao.insertar(actual.copy(raciones = raciones))
        }
    }

class CronoEstadoRepoRoom
    @Inject
    constructor(
        private val dao: CronoEstadoDao,
        private val reloj: Reloj,
    ) : CronoEstadoRepo {
        override fun observar(slug: String): Flow<CronoEstado?> =
            dao.observar(slug).map { e ->
                e?.let { CronoEstado(it.slug, it.json, it.actualizadoMs) }
            }

        override suspend fun obtener(slug: String): CronoEstado? =
            dao.obtener(slug)?.let {
                CronoEstado(it.slug, it.json, it.actualizadoMs)
            }

        override suspend fun todos(): List<CronoEstado> =
            dao.todos().map { CronoEstado(it.slug, it.json, it.actualizadoMs) }

        override suspend fun guardar(
            slug: String,
            json: String,
        ) = dao.guardar(CronoEstadoEntity(slug, json, reloj.ahoraMs()))

        override suspend fun borrar(slug: String) = dao.borrar(slug)
    }
