package app.trombito.souschef.ui.comun

import androidx.compose.runtime.staticCompositionLocalOf
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Potencia

/** Cocina del usuario para pintar la potencia (ADR-0014). La da la raíz de cada pantalla desde sus Ajustes. */
val LocalCocina = staticCompositionLocalOf { Cocina.VITRO_9 }

/** «7», «3,5» o «medio-alto», según la cocina; `f` está en la escala canónica 0-9. */
fun Cocina.nivel(f: Int): String = Potencia.texto(f.coerceIn(0, F_MAX), this)

/** «al 7» o «a fuego medio-alto»: la misma gramática que el texto de las recetas. */
fun Cocina.alNivel(f: Int): String = Potencia.reescribir("al {${f.coerceIn(0, F_MAX)}}", this)

private const val F_MAX = 9
