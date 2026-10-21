package app.trombito.souschef.ui.bienvenida

import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.domain.TemaApp
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.ui.ajustes.AjustesViewModel
import app.trombito.souschef.ui.comun.EstadoPermisos
import app.trombito.souschef.ui.comun.PermisoApp

/** Todo lo que pintan las páginas de la intro: ajustes ya guardados, recetas de ejemplo, prueba de voz y permisos. */
internal class EstadoIntro(
    val ajustes: Ajustes = Ajustes(),
    val demo: DatosDemo = DatosDemo(),
    val voz: PruebaVoz = PruebaVoz.Inactiva,
    val permisos: EstadoPermisos = EstadoPermisos(),
    /** Hora actual en minutos del día, para el ejemplo de «si empiezas ya». */
    val ahoraMin: Int = 0,
)

/** Una elección por ajuste: cada una se guarda al momento. */
@Suppress("LongParameterList") // un callback por ajuste de la intro
internal class AccionesAjustes(
    val cocina: (Cocina) -> Unit = {},
    val fuegos: (Int) -> Unit = {},
    val horno: (Boolean) -> Unit = {},
    val raciones: (Int) -> Unit = {},
    val servir: (Int) -> Unit = {},
    val horaTarjeta: (HoraTarjeta) -> Unit = {},
    val tema: (TemaApp) -> Unit = {},
    val lista: (DisposicionLista) -> Unit = {},
    val disposicion: (DisposicionCrono) -> Unit = {},
    val voz: (Boolean) -> Unit = {},
) {
    companion object {
        fun de(vm: AjustesViewModel) =
            AccionesAjustes(
                cocina = vm::elegirCocina,
                fuegos = vm::ajustarFuegos,
                horno = vm::activarHorno,
                raciones = vm::ajustarRacionesDefecto,
                servir = vm::ajustarServir,
                horaTarjeta = vm::elegirHoraTarjeta,
                tema = vm::elegirTema,
                lista = vm::elegirLista,
                disposicion = vm::elegirDisposicion,
                voz = vm::activarVoz,
            )
    }
}

/** Lo que las páginas pueden pedir además de elegir ajustes. */
internal class AccionesIntro(
    val ajustes: AccionesAjustes = AccionesAjustes(),
    val permitir: (PermisoApp) -> Unit = {},
    val probarVoz: () -> Unit = {},
    /** «Siguiente» en la última página: acaba la intro. */
    val terminar: () -> Unit = {},
    val saltar: () -> Unit = {},
)
