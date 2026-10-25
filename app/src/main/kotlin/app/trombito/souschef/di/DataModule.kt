package app.trombito.souschef.di

import android.content.Context
import androidx.room.Room
import app.trombito.souschef.data.copia.ArchivoCopiaAndroid
import app.trombito.souschef.data.db.MIGRATION_1_2
import app.trombito.souschef.data.db.MIGRATION_2_3
import app.trombito.souschef.data.db.SousChefDatabase
import app.trombito.souschef.data.repo.AjustesRepoRoom
import app.trombito.souschef.data.repo.CronoEstadoRepoRoom
import app.trombito.souschef.data.repo.HistorialRepoRoom
import app.trombito.souschef.data.repo.PreferenciasRepoRoom
import app.trombito.souschef.data.repo.PropuestasRepoRoom
import app.trombito.souschef.data.repo.RecetasRepoRoom
import app.trombito.souschef.data.repo.TecnicasRepoRoom
import app.trombito.souschef.data.seed.FuenteAssets
import app.trombito.souschef.data.seed.FuenteSemilla
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.CronoEstadoRepo
import app.trombito.souschef.domain.HistorialRepo
import app.trombito.souschef.domain.PreferenciasRepo
import app.trombito.souschef.domain.PropuestasRepo
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.domain.TecnicasRepo
import app.trombito.souschef.domain.copia.ArchivoCopia
import app.trombito.souschef.domain.copia.GestorCopia
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides fun recetaDao(db: SousChefDatabase) = db.recetaDao()

    @Provides fun tecnicaDao(db: SousChefDatabase) = db.tecnicaDao()

    @Provides fun historialDao(db: SousChefDatabase) = db.historialDao()

    @Provides fun preferenciaDao(db: SousChefDatabase) = db.preferenciaDao()

    @Provides fun cronoEstadoDao(db: SousChefDatabase) = db.cronoEstadoDao()

    @Provides fun metaDao(db: SousChefDatabase) = db.metaDao()

    @Provides fun reloj(): Reloj = Reloj.Sistema

    @Provides
    fun gestorCopia(
        historial: HistorialRepo,
        recetas: RecetasRepo,
        preferencias: PreferenciasRepo,
        archivo: ArchivoCopia,
        reloj: Reloj,
    ): GestorCopia = GestorCopia(historial, recetas, preferencias, archivo, reloj)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {
    @Binds abstract fun recetas(impl: RecetasRepoRoom): RecetasRepo

    @Binds abstract fun tecnicas(impl: TecnicasRepoRoom): TecnicasRepo

    @Binds abstract fun historial(impl: HistorialRepoRoom): HistorialRepo

    @Binds abstract fun preferencias(impl: PreferenciasRepoRoom): PreferenciasRepo

    @Binds abstract fun propuestas(impl: PropuestasRepoRoom): PropuestasRepo

    @Binds abstract fun cronoEstado(impl: CronoEstadoRepoRoom): CronoEstadoRepo

    @Binds abstract fun ajustes(impl: AjustesRepoRoom): AjustesRepo

    @Binds abstract fun fuenteSemilla(impl: FuenteAssets): FuenteSemilla

    @Binds abstract fun archivoCopia(impl: ArchivoCopiaAndroid): ArchivoCopia
}
