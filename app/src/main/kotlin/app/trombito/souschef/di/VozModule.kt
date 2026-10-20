package app.trombito.souschef.di

import android.content.Context
import app.trombito.souschef.data.voz.EscuchaAndroid
import app.trombito.souschef.data.voz.HablaAndroid
import app.trombito.souschef.domain.voz.Escucha
import app.trombito.souschef.domain.voz.Habla
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/**
 * Voz del cronómetro (ADR-0008). Sin scope a propósito: cada inyección crea una instancia nueva, que el ViewModel del
 * cronómetro posee y libera (`parar()` / `liberar()`). Un singleton retendría el micrófono más allá de la pantalla.
 */
@Module
@InstallIn(SingletonComponent::class)
object VozModule {
    @Provides
    fun escucha(
        @ApplicationContext contexto: Context,
    ): Escucha = EscuchaAndroid(contexto)

    @Provides
    fun habla(
        @ApplicationContext contexto: Context,
    ): Habla = HablaAndroid(contexto)
}
