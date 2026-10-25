package app.trombito.souschef.di

import app.trombito.souschef.data.alarma.AlarmaSistemaAndroid
import app.trombito.souschef.data.alarma.NotificadorAndroid
import app.trombito.souschef.data.aviso.AvisadorAndroid
import app.trombito.souschef.domain.CronoEstadoRepo
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.domain.crono.AlarmaSistema
import app.trombito.souschef.domain.crono.AlmacenCrono
import app.trombito.souschef.domain.crono.Avisador
import app.trombito.souschef.domain.crono.CadenaAlarmas
import app.trombito.souschef.domain.crono.CocinadasVivas
import app.trombito.souschef.domain.crono.Notificador
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

    /** Las consultas de solo lectura de la cadena, para quien no necesita mutar nada (novedades, actualización). */
    @Provides
    fun vivas(cadena: CadenaAlarmas): CocinadasVivas = cadena.vivas
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmaBindingsModule {
    @Binds abstract fun alarmaSistema(impl: AlarmaSistemaAndroid): AlarmaSistema

    @Binds abstract fun notificador(impl: NotificadorAndroid): Notificador

    @Binds abstract fun avisador(impl: AvisadorAndroid): Avisador
}
