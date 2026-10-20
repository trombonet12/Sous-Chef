package app.trombito.souschef.ui.shell

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
    Libro(
        "M5 4.5A1.5 1.5 0 0 1 6.5 3H19v15H6.5A1.5 1.5 0 0 0 5 19.5",
        "M5 4.5v15A1.5 1.5 0 0 0 6.5 21H19v-3",
        "M9 7h6",
    ),
    Tarro("M8 3h8v3H8z", "M7 6h10l1 3v10a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V9z", "M9 13h6"),
    Reloj("M12 21a9 9 0 1 0 0-18a9 9 0 0 0 0 18z", "M12 7v5l3 2"),
    Mas(
        "F:M12 6.5a1.5 1.5 0 1 0 0-3a1.5 1.5 0 0 0 0 3z",
        "F:M12 13.5a1.5 1.5 0 1 0 0-3a1.5 1.5 0 0 0 0 3z",
        "F:M12 20.5a1.5 1.5 0 1 0 0-3a1.5 1.5 0 0 0 0 3z",
    ),
    Libreta("M6 3h11a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H6z", "M9 8h6", "M9 12h6", "M9 16h4"),
    Atras("M15 5l-7 7 7 7"),
    Buscar("M10.5 17a6.5 6.5 0 1 0 0-13a6.5 6.5 0 0 0 0 13z", "M15.5 15.5L20 20"),
    Estrella("M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z"),
    EstrellaLlena("F:M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z"),
    Imprimir(
        "M7 9V3h10v6",
        "M7 18H5a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2",
        "M7 14h10v7H7z",
    ),
    Copiar("M9 9h11v11H9z", "M5 15H4V4h11v1"),
    Carrito(
        "M3 4h2l2.4 11h10.2L20 7H6.2",
        "F:M9 20a1.5 1.5 0 1 0 0-3a1.5 1.5 0 0 0 0 3z",
        "F:M17 20a1.5 1.5 0 1 0 0-3a1.5 1.5 0 0 0 0 3z",
    ),
    Crono("M12 21a8 8 0 1 0 0-16a8 8 0 0 0 0 16z", "M12 9v4", "M10 2h4", "M18.5 6.5l1.5-1.5"),
    Anadir("M12 5v14", "M5 12h14"),
    Deshacer("M9 14L4 9l5-5", "M4 9h10.5a5.5 5.5 0 0 1 0 11H11"),
    Menos("M5 12h14"),
    Borrar("M4 7h16", "M9 7V4h6v3", "M6 7l1 13h10l1-13"),
    Cerrar("M6 6l12 12", "M18 6L6 18"),
    Microfono("M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3z", "M6 11a6 6 0 0 0 12 0", "M12 17v4", "M9 21h6"),
    MicrofonoMudo(
        "M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3z",
        "M6 11a6 6 0 0 0 12 0",
        "M12 17v4",
        "M9 21h6",
        "M4 4l16 16",
    ),

    /** Tres deslizadores: abre la hoja de filtros de la lista. */
    Filtros(
        "M4 6h9",
        "M17 6h3",
        "M15 8a2 2 0 1 0 0-4a2 2 0 0 0 0 4z",
        "M4 12h3",
        "M11 12h9",
        "M9 14a2 2 0 1 0 0-4a2 2 0 0 0 0 4z",
        "M4 18h11",
        "M19 18h1",
        "M17 20a2 2 0 1 0 0-4a2 2 0 0 0 0 4z",
    ),

    /** Chevron a la derecha: fila que abre otra pantalla. */
    Chevron("M9 5l7 7-7 7"),

    /** Círculo medio relleno: apariencia (claro/oscuro). */
    Apariencia("M12 21a9 9 0 1 0 0-18a9 9 0 0 0 0 18z", "F:M12 3a9 9 0 0 1 0 18z"),

    /** Tenedor y espagueti: categoría de pasta. */
    Pasta(
        "M6 3v5a2 2 0 0 0 4 0V3",
        "M8 3v5",
        "M8 10v11",
        "M14 7c1-1.5 2-1.5 3 0s2 1.5 3 0",
        "M14 12c1-1.5 2-1.5 3 0s2 1.5 3 0",
        "M14 17c1-1.5 2-1.5 3 0s2 1.5 3 0",
    ),

    /** Cuenco con granos: categoría de arroz. */
    Arroz(
        "M4 11h16a8 8 0 0 1-16 0z",
        "F:M8 8a1 1 0 1 0 0-2a1 1 0 0 0 0 2z",
        "F:M12 7a1 1 0 1 0 0-2a1 1 0 0 0 0 2z",
        "F:M16 8a1 1 0 1 0 0-2a1 1 0 0 0 0 2z",
    ),

    /** Patata con sus ojos: categoría de patata. */
    Patata(
        "M5 14c0-5 4-9 9-9 3 0 5 2 5 5 0 5-4 9-9 9-3 0-5-2-5-5z",
        "M10 11h.01",
        "M14 15h.01",
        "M15 9h.01",
    ),

    /** Pez de perfil: categoría de pescado. */
    Pescado(
        "M3 12c2.5-4 6-5.5 10-5c2 .3 4 2 5 5c-1 3-3 4.7-5 5c-4 .5-7.5-1-10-5z",
        "M18 12l3.5-3.5v7z",
        "M8 11h.01",
    ),

    /** Hoja con nervio: categoría de verdura. */
    Verdura("M5 19c0-9 5-14 15-15c0 10-5 15-15 15z", "M4 20L13 11"),

    /** Porción de pizza con rodajas: categoría de masas. */
    Masas(
        "M12 21L4 6c5-3 11-3 16 0z",
        "F:M10 11a1 1 0 1 0 0-2a1 1 0 0 0 0 2z",
        "F:M14 12a1 1 0 1 0 0-2a1 1 0 0 0 0 2z",
        "F:M12 16a1 1 0 1 0 0-2a1 1 0 0 0 0 2z",
    ),

    /** Sartén con mango y trozos en el aire: categoría de salteado. */
    Salteado(
        "M3 11h14v3a5 5 0 0 1-5 5H8a5 5 0 0 1-5-5z",
        "M17 12h5",
        "M8 8V6",
        "M12 8V4",
    ),

    /** Plato visto desde arriba: categoría genérica u otra. */
    Plato("M12 21a9 9 0 1 0 0-18a9 9 0 0 0 0 18z", "M12 17a5 5 0 1 0 0-10a5 5 0 0 0 0 10z"),

    /** Destellos: categoría de acabado. */
    Acabado(
        "M10 4l1.8 5.2L17 11l-5.2 1.8L10 18l-1.8-5.2L3 11l5.2-1.8z",
        "M18 3v4",
        "M16 5h4",
        "M19 15v4",
        "M17 17h4",
    ),

    /** Olla con tapa: categoría de base. */
    Base("M4 9h16v8a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3z", "M5 6h14", "M12 6V4"),

    /** Olla con vapor: categoría de cocción. */
    Coccion(
        "M4 12h16v5a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3z",
        "M8 9c-1-1.5 1-2.5 0-4",
        "M12 9c-1-1.5 1-2.5 0-4",
        "M16 9c-1-1.5 1-2.5 0-4",
    ),

    /** Cuchillo de cocinero: categoría de corte. */
    Corte("M3 21c0-7 4-12 11-14l3 3c-2 7-7 11-14 11z", "M15.5 8.5L20 4"),

    /** Sartén con burbujas de aceite: categoría de fritura. */
    Fritura(
        "M3 13h14v2a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4z",
        "M17 14h5",
        "M7 9h.01",
        "M11 6h.01",
        "M13 10h.01",
    ),

    /** Cuenco con gotas encima: categoría de marinado. */
    Marinado(
        "M4 13h16a8 7 0 0 1-16 0z",
        "M8 3c1.5 2 2.5 3.5 2.5 5a2.5 2.5 0 0 1-5 0c0-1.5 1-3 2.5-5z",
        "M16 5c1 1.5 1.75 2.5 1.75 3.5a1.75 1.75 0 0 1-3.5 0c0-1 .75-2 1.75-3.5z",
    ),

    /** Cazo con mango largo: categoría de salsa. */
    Salsa("M8 21a5 5 0 1 0 0-10a5 5 0 0 0 0 10z", "M11.5 12.5L21 3"),
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
