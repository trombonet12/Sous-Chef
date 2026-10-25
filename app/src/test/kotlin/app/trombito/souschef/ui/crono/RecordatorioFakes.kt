package app.trombito.souschef.ui.crono

import app.trombito.souschef.domain.crono.AlarmaRecordatorio
import app.trombito.souschef.domain.crono.NotificadorRecordatorio
import app.trombito.souschef.domain.crono.Recordatorio
import app.trombito.souschef.domain.crono.RecordatorioRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeRecordatorioRepo(
    inicial: Recordatorio? = null,
) : RecordatorioRepo {
    val dato = MutableStateFlow(inicial)

    override suspend fun leer(): Recordatorio? = dato.value

    override suspend fun guardar(r: Recordatorio) {
        dato.value = r
    }

    override suspend fun borrar() {
        dato.value = null
    }

    override fun observar(): Flow<Recordatorio?> = dato
}

class FakeAlarmaRecordatorio : AlarmaRecordatorio {
    var programada: Long? = null
    var programaciones = 0

    override fun programar(ms: Long) {
        programada = ms
        programaciones++
    }

    override fun cancelar() {
        programada = null
    }
}

class FakeNotificadorRecordatorio : NotificadorRecordatorio {
    /** Slug, título y hora de servir de cada aviso publicado, en orden. */
    val avisos = mutableListOf<Triple<String, String, Long>>()

    override fun avisar(
        slug: String,
        titulo: String,
        servirMs: Long,
    ) {
        avisos += Triple(slug, titulo, servirMs)
    }
}
