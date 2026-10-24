package app.trombito.souschef.di

import app.trombito.souschef.data.novedades.FuenteNovedadesAssets
import app.trombito.souschef.data.novedades.VersionAppAndroid
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.ComprobarNovedades
import app.trombito.souschef.domain.FuenteNovedades
import app.trombito.souschef.domain.VersionApp
import app.trombito.souschef.domain.crono.CadenaAlarmas
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NovedadesBindsModule {
    @Binds abstract fun fuenteNovedades(impl: FuenteNovedadesAssets): FuenteNovedades

    @Binds abstract fun versionApp(impl: VersionAppAndroid): VersionApp
}

@Module
@InstallIn(SingletonComponent::class)
object NovedadesModule {
    @Provides
    fun comprobarNovedades(
        ajustes: AjustesRepo,
        fuente: FuenteNovedades,
        version: VersionApp,
        cadena: CadenaAlarmas,
    ): ComprobarNovedades = ComprobarNovedades(ajustes, fuente, version) { cadena.hayViva() }
}
