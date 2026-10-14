package app.souschef.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Scope que vive lo que el proceso; para guardados pendientes al cerrar una pantalla. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AmbitoModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun ambitoAplicacion(): CoroutineScope = CoroutineScope(SupervisorJob())
}
