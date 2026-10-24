package app.trombito.souschef.domain

/**
 * Qué novedades enseñar al llegar a Recetas; `null` si no toca. Si toca, de la más nueva a la más vieja.
 *
 * - Instalación nueva (`introVista` false), cocinada viva, entrada directa (deep link o alarma) o actualización
 *   de Play pendiente: nada.
 * - Sin marca [vista] (viene de una versión anterior a las novedades): todas las de versión <= [actual].
 * - [vista] anterior a [actual] (`major.minor`): las de versión > [vista] y <= [actual].
 * - Misma versión o [vista] posterior (bajada de versión): nada. Los parches no cambian `major.minor`.
 */
@Suppress("LongParameterList")
fun novedadesPendientes(
    actual: String,
    vista: String?,
    introVista: Boolean,
    hayViva: Boolean,
    entradaDirecta: Boolean,
    playPendiente: Boolean,
    todas: List<Novedades>,
): List<Novedades>? {
    val tope = VersionMenor.de(actual)
    val suelo = VersionMenor.de(vista)
    val bloqueada = listOf(!introVista, hayViva, entradaDirecta, playPendiente).any { it }
    val yaVista = suelo != null && tope != null && suelo >= tope
    if (bloqueada || tope == null || yaVista) return null
    return todas
        .mapNotNull { n -> VersionMenor.de(n.version)?.let { it to n } }
        .filter { (v, _) -> v <= tope && (suelo == null || v > suelo) }
        .sortedByDescending { (v, _) -> v }
        .map { (_, n) -> n }
        .ifEmpty { null }
}
