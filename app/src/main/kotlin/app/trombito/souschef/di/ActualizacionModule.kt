package app.trombito.souschef.di

import android.content.Context
import app.trombito.souschef.data.actualizacion.ActualizacionPlay
import app.trombito.souschef.domain.crono.CadenaAlarmas
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ActualizacionModule {
    @Provides
    @Singleton
    fun appUpdateManager(
        @ApplicationContext contexto: Context,
    ): AppUpdateManager = AppUpdateManagerFactory.create(contexto)

    @Provides
    @Singleton
    fun actualizacionPlay(
        @ApplicationContext contexto: Context,
        manager: AppUpdateManager,
        cadena: CadenaAlarmas,
    ): ActualizacionPlay =
        ActualizacionPlay(manager, { ActualizacionPlay.instaladorDe(contexto) }, { cadena.hayViva() })
}
