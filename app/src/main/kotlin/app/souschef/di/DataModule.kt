package app.souschef.di

import android.content.Context
import androidx.room.Room
import app.souschef.data.db.MIGRATION_1_2
import app.souschef.data.db.SousChefDatabase
import app.souschef.data.repo.AjustesRepoRoom
import app.souschef.data.repo.CronoEstadoRepoRoom
import app.souschef.data.repo.HistorialRepoRoom
import app.souschef.data.repo.PreferenciasRepoRoom
import app.souschef.data.repo.RecetasRepoRoom
import app.souschef.data.repo.TecnicasRepoRoom
import app.souschef.data.seed.FuenteAssets
import app.souschef.data.seed.FuenteSemilla
import app.souschef.domain.AjustesRepo
import app.souschef.domain.CronoEstadoRepo
import app.souschef.domain.HistorialRepo
import app.souschef.domain.PreferenciasRepo
import app.souschef.domain.RecetasRepo
import app.souschef.domain.Reloj
import app.souschef.domain.TecnicasRepo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): SousChefDatabase =
        Room
            .databaseBuilder(context, SousChefDatabase::class.java, "souschef.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun recetaDao(db: SousChefDatabase) = db.recetaDao()

    @Provides fun tecnicaDao(db: SousChefDatabase) = db.tecnicaDao()

    @Provides fun historialDao(db: SousChefDatabase) = db.historialDao()

    @Provides fun preferenciaDao(db: SousChefDatabase) = db.preferenciaDao()

    @Provides fun cronoEstadoDao(db: SousChefDatabase) = db.cronoEstadoDao()

    @Provides fun metaDao(db: SousChefDatabase) = db.metaDao()

    @Provides fun reloj(): Reloj = Reloj.Sistema
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {
    @Binds abstract fun recetas(impl: RecetasRepoRoom): RecetasRepo

    @Binds abstract fun tecnicas(impl: TecnicasRepoRoom): TecnicasRepo

    @Binds abstract fun historial(impl: HistorialRepoRoom): HistorialRepo

    @Binds abstract fun preferencias(impl: PreferenciasRepoRoom): PreferenciasRepo

    @Binds abstract fun cronoEstado(impl: CronoEstadoRepoRoom): CronoEstadoRepo

    @Binds abstract fun ajustes(impl: AjustesRepoRoom): AjustesRepo

    @Binds abstract fun fuenteSemilla(impl: FuenteAssets): FuenteSemilla
}
