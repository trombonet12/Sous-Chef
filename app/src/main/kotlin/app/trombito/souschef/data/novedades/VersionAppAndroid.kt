package app.trombito.souschef.data.novedades

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import app.trombito.souschef.domain.VersionApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** `versionName` del paquete (el proyecto no genera BuildConfig, así que se lee del PackageManager). */
class VersionAppAndroid
    @Inject
    constructor(
        @ApplicationContext private val contexto: Context,
    ) : VersionApp {
        override val nombre: String by lazy { leer(contexto) }

        companion object {
            /** Vacío si no se puede leer. */
            @Suppress("TooGenericExceptionCaught", "SwallowedException")
            fun leer(contexto: Context): String =
                try {
                    val gestor = contexto.packageManager
                    val info =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gestor.getPackageInfo(contexto.packageName, PackageManager.PackageInfoFlags.of(0))
                        } else {
                            @Suppress("DEPRECATION")
                            gestor.getPackageInfo(contexto.packageName, 0)
                        }
                    info.versionName.orEmpty()
                } catch (e: Exception) {
                    ""
                }
        }
    }
