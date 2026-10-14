package app.souschef.data.seed

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Origen de los datos de siembra (rutas relativas a `data/`, p. ej. `recetas/huevos-rotos.json`). */
interface FuenteSemilla {
    /** Rutas completas de los archivos de [directorio], ordenadas. */
    fun listar(directorio: String): List<String>

    fun leer(ruta: String): String
}

/** Lee los assets generados por la tarea Gradle `copiarDatos`. */
class FuenteAssets
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : FuenteSemilla {
        override fun listar(directorio: String): List<String> =
            context.assets
                .list(directorio)
                .orEmpty()
                .sorted()
                .map { "$directorio/$it" }

        override fun leer(ruta: String): String =
            context.assets
                .open(ruta)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
    }
