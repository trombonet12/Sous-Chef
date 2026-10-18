package app.souschef.data.repo

import androidx.room.withTransaction
import app.souschef.data.db.CronoEstadoDao
import app.souschef.data.db.CronoEstadoEntity
import app.souschef.data.db.HistorialDao
import app.souschef.data.db.PreferenciaDao
import app.souschef.data.db.PreferenciaEntity
import app.souschef.data.db.RecetaDao
import app.souschef.data.db.SousChefDatabase
import app.souschef.data.db.TecnicaDao
import app.souschef.domain.CronoEstado
import app.souschef.domain.CronoEstadoRepo
import app.souschef.domain.EntradaHistorial
import app.souschef.domain.HistorialRepo
import app.souschef.domain.Preferencia
import app.souschef.domain.PreferenciasRepo
import app.souschef.domain.RecetasRepo
import app.souschef.domain.Reloj
import app.souschef.domain.TecnicasRepo
import app.souschef.receta.Receta
import app.souschef.receta.Tecnica
import app.souschef.receta.leerReceta
import app.souschef.receta.leerTecnica
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
