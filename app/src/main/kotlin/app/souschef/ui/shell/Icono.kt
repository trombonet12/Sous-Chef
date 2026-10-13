package app.souschef.ui.shell

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Iconos de trazo propios (24 dp, línea de 2 dp), sin la dependencia material-icons. Cada uno es una lista de
 * trazos SVG; los rellenos se marcan con el prefijo "F:".
 */
enum class Icono(
    private vararg val trazos: String,
) {
    Libro("M5 4.5A1.5 1.5 0 0 1 6.5 3H19v15H6.5A1.5 1.5 0 0 0 5 19.5", "M5 4.5v15A1.5 1.5 0 0 0 6.5 21H19v-3", "M9 7h6"),
    Tarro("M8 3h8v3H8z", "M7 6h10l1 3v10a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V9z", "M9 13h6"),
    Reloj("M12 21a9 9 0 1 0 0-18a9 9 0 0 0 0 18z", "M12 7v5l3 2"),
    Mas("F:M12 6.5a1.5 1.5 0 1 0 0-3a1.5 1.5 0 0 0 0 3z", "F:M12 13.5a1.5 1.5 0 1 0 0-3a1.5 1.5 0 0 0 0 3z", "F:M12 20.5a1.5 1.5 0 1 0 0-3a1.5 1.5 0 0 0 0 3z"),
    Libreta("M6 3h11a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H6z", "M9 8h6", "M9 12h6", "M9 16h4"),
    Atras("M15 5l-7 7 7 7"),
    Buscar("M10.5 17a6.5 6.5 0 1 0 0-13a6.5 6.5 0 0 0 0 13z", "M15.5 15.5L20 20"),
    Estrella("M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z"),
    EstrellaLlena("F:M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z"),
    Imprimir("M7 9V3h10v6", "M7 18H5a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2", "M7 14h10v7H7z"),
    Copiar("M9 9h11v11H9z", "M5 15H4V4h11v1"),
    Carrito("M3 4h2l2.4 11h10.2L20 7H6.2", "F:M9 20a1.5 1.5 0 1 0 0-3a1.5 1.5 0 0 0 0 3z", "F:M17 20a1.5 1.5 0 1 0 0-3a1.5 1.5 0 0 0 0 3z"),
    Crono("M12 21a8 8 0 1 0 0-16a8 8 0 0 0 0 16z", "M12 9v4", "M10 2h4", "M18.5 6.5l1.5-1.5"),
    Anadir("M12 5v14", "M5 12h14"),
    Borrar("M4 7h16", "M9 7V4h6v3", "M6 7l1 13h10l1-13"),
    Cerrar("M6 6l12 12", "M18 6L6 18"),
    ;

    val vector: ImageVector by lazy {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        trazos.forEach { t ->
            val relleno = t.startsWith("F:")
            val nodos = PathParser().parsePathString(t.removePrefix("F:")).toNodes()
            if (relleno) {
                b.addPath(nodos, fill = SolidColor(Color.Black))
            } else {
                b.addPath(
                    nodos,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }
        b.build()
    }
}
