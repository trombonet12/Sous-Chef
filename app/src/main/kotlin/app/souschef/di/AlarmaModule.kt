package app.souschef.di

import app.souschef.data.alarma.AlarmaSistemaAndroid
import app.souschef.data.alarma.NotificadorAndroid
import app.souschef.domain.CronoEstadoRepo
import app.souschef.domain.RecetasRepo
import app.souschef.domain.Reloj
import app.souschef.domain.crono.AlarmaSistema
import app.souschef.domain.crono.AlmacenCrono
import app.souschef.domain.crono.CadenaAlarmas
import app.souschef.domain.crono.Notificador
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CronoModule {
    @Provides
    @Singleton
    fun almacen(repo: CronoEstadoRepo): AlmacenCrono = AlmacenCrono(repo)

    @Provides
    @Singleton
    fun cadena(
        almacen: AlmacenCrono,
        recetas: RecetasRepo,
        reloj: Reloj,
        sistema: AlarmaSistema,
        notificador: Notificador,
    ): CadenaAlarmas = CadenaAlarmas(almacen, recetas, reloj, sistema, notificador)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmaBindingsModule {
    @Binds abstract fun alarmaSistema(impl: AlarmaSistemaAndroid): AlarmaSistema

    @Binds abstract fun notificador(impl: NotificadorAndroid): Notificador
}
