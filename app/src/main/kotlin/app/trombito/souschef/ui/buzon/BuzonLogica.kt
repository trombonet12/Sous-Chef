package app.trombito.souschef.ui.buzon

import androidx.annotation.StringRes
import app.trombito.souschef.R

/** Qué quiere contar el usuario. [etiqueta] es el texto del selector y del asunto del correo. */
enum class TipoBuzon(
    @param:StringRes val etiqueta: Int,
) {
    RECETA(R.string.buzon_tipo_receta),
    FALLO(R.string.buzon_tipo_fallo),
    SUGERENCIA(R.string.buzon_tipo_sugerencia),
}

/** Tiempo máximo de una receta pedida. */
enum class TiempoMaximo(
    @param:StringRes val etiqueta: Int,
) {
    MENOS_45(R.string.filtro_tiempo_menos_45),
    DE_45_A_90(R.string.filtro_tiempo_media),
    MAS_90(R.string.filtro_tiempo_larga),
}

/** Secciones de la app entre las que elegir al contar un fallo. */
enum class SeccionApp(
    @param:StringRes val etiqueta: Int,
) {
    RECETAS(R.string.buzon_pantalla_recetas),
    FICHA(R.string.buzon_pantalla_ficha),
    CRONOMETRO(R.string.buzon_pantalla_crono),
    TECNICAS(R.string.buzon_pantalla_tecnicas),
    HISTORIAL(R.string.buzon_pantalla_historial),
    AJUSTES(R.string.buzon_pantalla_ajustes),
    OTRA(R.string.buzon_pantalla_otra),
}

/** Lo que el usuario ha escrito. Cada tipo usa sus campos; el resto se ignora al construir el correo. */
data class FormularioBuzon(
    val tipo: TipoBuzon = TipoBuzon.RECETA,
    // Pedir una receta
    val plato: String = "",
    val ingredientes: String = "",
    val tiempo: TiempoMaximo? = null,
    val raciones: String = "",
    // Contar un fallo
    val queOcurrio: String = "",
    val queHacias: String = "",
    val pantalla: SeccionApp? = null,
    // Sugerencia
    val texto: String = "",
) {
    /** Se puede enviar cuando el campo principal del tipo elegido tiene algo escrito. */
    val valido: Boolean
        get() =
            when (tipo) {
                TipoBuzon.RECETA -> plato.isNotBlank()
                TipoBuzon.FALLO -> queOcurrio.isNotBlank()
                TipoBuzon.SUGERENCIA -> texto.isNotBlank()
            }
}

/** Datos de la app y del móvil que viajan al pie del correo. */
data class DatosApp(
    val version: String,
    val modelo: String,
    val androidRelease: String,
    val sdk: Int,
    val cocina: String,
)

/** Asunto y cuerpo ya montados. */
data class Correo(
    val asunto: String,
    val cuerpo: String,
) {
    /** El mismo texto para pegarlo a mano. */
    val textoPlano: String get() = "$asunto\n\n$cuerpo"
}

/**
 * Monta el correo del [formulario]: asunto `[Sous Chef] <tipo>`, los campos rellenados del tipo elegido (los vacíos se
 * omiten) y el bloque «— Datos —». Función pura: los textos llegan por [texto] (`getString` de recursos).
 */
fun construirCorreo(
    formulario: FormularioBuzon,
    datos: DatosApp,
    texto: (Int) -> String,
): Correo {
    val campos: List<Pair<Int, String?>> =
        when (formulario.tipo) {
            TipoBuzon.RECETA -> {
                listOf(
                    R.string.buzon_correo_plato to formulario.plato,
                    R.string.buzon_correo_ingredientes to formulario.ingredientes,
                    R.string.buzon_correo_tiempo to formulario.tiempo?.let { texto(it.etiqueta) },
                    R.string.buzon_correo_raciones to formulario.raciones,
                )
            }

            TipoBuzon.FALLO -> {
                listOf(
                    R.string.buzon_correo_que_paso to formulario.queOcurrio,
                    R.string.buzon_correo_que_hacias to formulario.queHacias,
                    R.string.buzon_correo_pantalla to formulario.pantalla?.let { texto(it.etiqueta) },
                )
            }

            TipoBuzon.SUGERENCIA -> {
                listOf(R.string.buzon_correo_sugerencia to formulario.texto)
            }
        }
    val rellenos =
        campos.mapNotNull { (id, valor) ->
            valor?.trim()?.takeIf { it.isNotEmpty() }?.let { linea(texto(id), it) }
        }
    val pie =
        listOf(
            texto(R.string.buzon_correo_datos),
            linea(texto(R.string.buzon_correo_version), datos.version),
            linea(texto(R.string.buzon_correo_dispositivo), datos.modelo),
            linea(texto(R.string.buzon_correo_android), "${datos.androidRelease} (SDK ${datos.sdk})"),
            linea(texto(R.string.buzon_correo_cocina), datos.cocina),
        )
    val cuerpo = (rellenos + "" + pie).joinToString("\n")
    return Correo(asunto = asunto(formulario.tipo, texto), cuerpo = cuerpo)
}

/** `[Sous Chef] Pedir una receta`: la marca es fija para poder filtrar el correo. */
fun asunto(
    tipo: TipoBuzon,
    texto: (Int) -> String,
): String = "[Sous Chef] ${texto(tipo.etiqueta)}"

private fun linea(
    etiqueta: String,
    valor: String,
): String = if ('\n' in valor) "$etiqueta:\n$valor" else "$etiqueta: $valor"
