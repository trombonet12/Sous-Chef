package app.trombito.souschef.domain

import kotlinx.coroutines.flow.first

/**
 * Caso de uso de «Novedades tras actualizar». La UI lo llama al llegar al destino Recetas y, al mostrar el aviso,
 * [marcarVistas] (se marca **al mostrarse**, para que no se repita si el proceso muere).
 */
class ComprobarNovedades(
    private val ajustes: AjustesRepo,
    private val fuente: FuenteNovedades,
    private val version: VersionApp,
    private val hayViva: suspend () -> Boolean,
) {
    /**
     * Las novedades que toca enseñar ahora, o `null`. [entradaDirecta]: se llegó por `souschef://crono` o por la
     * alarma. [playPendiente]: hay una actualización de Play por aplicar (hoy la app no lo sabe: queda en false).
     */
    suspend operator fun invoke(
        entradaDirecta: Boolean,
        playPendiente: Boolean = false,
    ): List<Novedades>? {
        val a = ajustes.observar().first()
        // Barato primero: sin intro vista no se lee nada más.
        if (!a.introVista || entradaDirecta || playPendiente) return null
        return novedadesPendientes(
            actual = version.nombre,
            vista = a.novedadesVistas,
            introVista = a.introVista,
            hayViva = hayViva(),
            entradaDirecta = entradaDirecta,
            playPendiente = playPendiente,
            todas = fuente.todas(),
        )
    }

    /** Guarda `major.minor` de [version] (la de la app, normalmente) como vista. */
    suspend fun marcarVistas(version: String = this.version.nombre) {
        ajustes.marcarNovedadesVistas(version)
    }
}
