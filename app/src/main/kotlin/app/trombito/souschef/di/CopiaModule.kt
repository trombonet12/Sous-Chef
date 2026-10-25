package app.trombito.souschef.di

import android.util.Log
import app.trombito.souschef.data.repo.HistorialRepoRoom
import app.trombito.souschef.domain.HistorialRepo
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.domain.copia.CarpetaCopia
import app.trombito.souschef.domain.copia.CopiaAutoRepo
import app.trombito.souschef.domain.copia.CopiaAutomatica
import app.trombito.souschef.domain.copia.GestorCopia
import app.trombito.souschef.domain.copia.HistorialConCopiaAutomatica
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

/** La copia automática del historial en una carpeta (ADR-0015). */
@Module
@InstallIn(SingletonComponent::class)
object CopiaModule {
    /**
     * El historial que usa toda la app. Envuelve al de Room para que cada cocinada cerrada en firme pida la copia
     * automática (ADR-0015). [copia] es `Lazy` porque la copia lee el historial: rompe el ciclo.
     */
    @Provides
    fun historial(
        room: HistorialRepoRoom,
        copia: Lazy<CopiaAutomatica>,
    ): HistorialRepo = HistorialConCopiaAutomatica(room) { copia.get().solicitar() }

    @Provides
    @Singleton
    fun copiaAutomatica(
        gestor: GestorCopia,
        carpeta: CarpetaCopia,
        estado: CopiaAutoRepo,
        reloj: Reloj,
        @ApplicationScope ambito: CoroutineScope,
    ): CopiaAutomatica =
        CopiaAutomatica(gestor, carpeta, estado, reloj, ambito) {
            // Solo la clase de la excepción: nada de URIs ni de mensajes en el log (ADR-0015).
            Log.w("SousChef", "Copia automática fallida: $it")
        }
}
