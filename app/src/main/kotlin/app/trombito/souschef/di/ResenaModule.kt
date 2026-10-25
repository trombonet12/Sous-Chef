package app.trombito.souschef.di

import android.content.Context
import app.trombito.souschef.data.actualizacion.ActualizacionPlay
import app.trombito.souschef.data.db.MetaDao
import app.trombito.souschef.data.repo.ResenaRepoRoom
import app.trombito.souschef.data.resena.ActividadActual
import app.trombito.souschef.data.resena.ResenaPlay
import app.trombito.souschef.domain.HistorialRepo
import app.trombito.souschef.domain.crono.CocinadasVivas
import app.trombito.souschef.domain.resena.GestorResena
import app.trombito.souschef.domain.resena.LanzadorResena
import app.trombito.souschef.domain.resena.ResenaRepo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** In-App Review (F14): pide la reseña una sola vez, solo si la app viene de Play. */
@Module
@InstallIn(SingletonComponent::class)
object ResenaModule {
    @Provides
    fun resenaRepo(dao: MetaDao): ResenaRepo = ResenaRepoRoom(dao)

    @Provides
    @Singleton
    fun reviewManager(
        @ApplicationContext contexto: Context,
    ): ReviewManager = ReviewManagerFactory.create(contexto)

    @Provides
    @Singleton
    fun lanzadorResena(
        @ApplicationContext contexto: Context,
        manager: ReviewManager,
        actividad: ActividadActual,
    ): LanzadorResena = ResenaPlay(manager, { ActualizacionPlay.instaladorDe(contexto) }, actividad::actual)

    @Provides
    @Singleton
    fun gestorResena(
        historial: HistorialRepo,
        repo: ResenaRepo,
        vivas: CocinadasVivas,
        lanzador: LanzadorResena,
    ): GestorResena = GestorResena(historial, repo, { vivas.hayViva() }, lanzador)
}
