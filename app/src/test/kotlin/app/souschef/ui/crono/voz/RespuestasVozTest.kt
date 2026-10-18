package app.souschef.ui.crono.voz

import app.souschef.receta.crono.ClaseEstado
import app.souschef.receta.voz.OrdenVoz
import app.souschef.ui.crono.AccionCarril
import app.souschef.ui.crono.PasoProximo
import app.souschef.ui.crono.VistaCarril
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RespuestasVozTest {
    private fun vista(
        id: String = "f0",
        nombre: String = "SARTÉN GRANDE",
        accion: AccionCarril = AccionCarril.PAUSAR,
        quedanS: Int = 460,
        clase: ClaseEstado = ClaseEstado.MARCHA,
    ) = VistaCarril(
        id = id,
        nombre = nombre,
        colorHex = "#E4572E",
        indiceCarril = 0,
        paso = if (accion == AccionCarril.TERMINADO) 0 else 1,
        pasos = 2,
        titulo = if (accion == AccionCarril.TERMINADO) null else "Sofrito",
        fuego = null,
        tapa = null,
        quedanS = quedanS,
        duracionS = 600,
        progreso = 0.5f,
        hitos = emptyList(),
        proximoHito = null,
        meto = null,
        saco = null,
        nota = null,
        despues = null,
        accion = accion,
        bloqueadoPor = null,
        clase = clase,
        sugeridoMs = null,
        finAtMs = null,
        tienePorque = false,
    )

    private fun corriendo(
        quedanS: Int,
        id: String = "f0",
        nombre: String = "SARTÉN GRANDE",
        despues: String? = null,
        titulo: String = "Sofrito",
    ) = vista(
        id = id,
        nombre = nombre,
        quedanS = quedanS,
        accion = if (quedanS > 0) AccionCarril.PAUSAR else AccionCarril.HECHO,
    ).copy(despues = despues, titulo = titulo, finAtMs = 1_000_000L + quedanS * 1000L)

    // ---- cuantoQueda

    @Test
    fun `cuantoQueda dice minutos y segundos con el nombre en minuscula con inicial`() {
        assertEquals("Sartén grande: quedan 7 minutos y 40 segundos.", RespuestasVoz.cuantoQueda(corriendo(460)))
    }

    @Test
    fun `cuantoQueda singular y plural`() {
        assertEquals("Sartén grande: queda 1 segundo.", RespuestasVoz.cuantoQueda(corriendo(1)))
        assertEquals("Sartén grande: quedan 2 segundos.", RespuestasVoz.cuantoQueda(corriendo(2)))
        assertEquals("Sartén grande: queda 1 minuto.", RespuestasVoz.cuantoQueda(corriendo(60)))
        assertEquals("Sartén grande: quedan 2 minutos.", RespuestasVoz.cuantoQueda(corriendo(120)))
        assertEquals(
            "Sartén grande: quedan 1 minuto y 1 segundo.",
            RespuestasVoz.cuantoQueda(corriendo(61)),
        )
        assertEquals(
            "Sartén grande: quedan 2 minutos y 1 segundo.",
            RespuestasVoz.cuantoQueda(corriendo(121)),
        )
    }

    @Test
    fun `cuantoQueda borde 59 y 60 segundos`() {
        assertEquals("Sartén grande: quedan 59 segundos.", RespuestasVoz.cuantoQueda(corriendo(59)))
        assertEquals("Sartén grande: queda 1 minuto.", RespuestasVoz.cuantoQueda(corriendo(60)))
    }

    @Test
    fun `cuantoQueda borde 10 minutos y por encima solo minutos redondeados`() {
        assertEquals(
            "Sartén grande: quedan 9 minutos y 59 segundos.",
            RespuestasVoz.cuantoQueda(corriendo(599)),
        )
        assertEquals("Sartén grande: quedan 10 minutos.", RespuestasVoz.cuantoQueda(corriendo(600)))
        assertEquals("Sartén grande: quedan 10 minutos.", RespuestasVoz.cuantoQueda(corriendo(629)))
        assertEquals("Sartén grande: quedan 11 minutos.", RespuestasVoz.cuantoQueda(corriendo(630)))
        assertEquals("Sartén grande: quedan 45 minutos.", RespuestasVoz.cuantoQueda(corriendo(2700)))
    }

    @Test
    fun `cuantoQueda vencido pide decir hecho`() {
        assertEquals("Sartén grande: ya está, di hecho.", RespuestasVoz.cuantoQueda(corriendo(0)))
        assertEquals("Sartén grande: ya está, di hecho.", RespuestasVoz.cuantoQueda(corriendo(-30)))
    }

    @Test
    fun `cuantoQueda sin empezar`() {
        val v = vista(accion = AccionCarril.EMPEZAR, clase = ClaseEstado.YA)
        assertEquals("Sartén grande: sin empezar.", RespuestasVoz.cuantoQueda(v))
        val b = vista(accion = AccionCarril.BLOQUEADO, clase = ClaseEstado.BLOQ)
        assertEquals("Sartén grande: sin empezar.", RespuestasVoz.cuantoQueda(b))
    }

    @Test
    fun `cuantoQueda en pausa con el tiempo que queda`() {
        val v = vista(accion = AccionCarril.SEGUIR, quedanS = 180, clase = ClaseEstado.PAUSA)
        assertEquals("Sartén grande: en pausa, quedan 3 minutos.", RespuestasVoz.cuantoQueda(v))
    }

    @Test
    fun `cuantoQueda carril terminado`() {
        val v = vista(accion = AccionCarril.TERMINADO, quedanS = 0, clase = ClaseEstado.FIN)
        assertEquals("Sartén grande: terminado.", RespuestasVoz.cuantoQueda(v))
    }

    @Test
    fun `cuantoQueda respeta nombres de varias palabras y acentos`() {
        val v = corriendo(90, nombre = "OLLA DE ARROZ")
        assertEquals("Olla de arroz: quedan 1 minuto y 30 segundos.", RespuestasVoz.cuantoQueda(v))
        assertEquals("Ñoquis: queda 1 segundo.", RespuestasVoz.cuantoQueda(corriendo(1, nombre = "ÑOQUIS")))
    }

    // ---- queToca

    @Test
    fun `queToca da titulo fuego y tapa del carril mas urgente`() {
        val v = corriendo(300).copy(fuego = 8, tapa = "SIN TAPA", titulo = "Sellar")
        assertEquals("Sartén grande: Sellar. Fuego 8, destapada.", RespuestasVoz.queToca(listOf(v), null))
    }

    @Test
    fun `queToca traduce los tres valores de tapa`() {
        val base = corriendo(300).copy(fuego = 3)
        assertEquals(
            "Sartén grande: Sofrito. Fuego 3, tapada.",
            RespuestasVoz.queToca(listOf(base.copy(tapa = "PUESTA")), null),
        )
        assertEquals(
            "Sartén grande: Sofrito. Fuego 3, a medias.",
            RespuestasVoz.queToca(listOf(base.copy(tapa = "A MEDIAS")), null),
        )
        assertEquals(
            "Sartén grande: Sofrito. Fuego 3, destapada.",
            RespuestasVoz.queToca(listOf(base.copy(tapa = "FUERA")), null),
        )
    }

    @Test
    fun `queToca sin fuego ni tapa omite los detalles`() {
        assertEquals("Sartén grande: Sofrito.", RespuestasVoz.queToca(listOf(corriendo(300)), null))
        val soloTapa = corriendo(300).copy(tapa = "PUESTA")
        assertEquals("Sartén grande: Sofrito. Tapada.", RespuestasVoz.queToca(listOf(soloTapa), null))
        val soloFuego = corriendo(300).copy(fuego = 5, tapa = "algo raro")
        assertEquals("Sartén grande: Sofrito. Fuego 5.", RespuestasVoz.queToca(listOf(soloFuego), null))
    }

    @Test
    fun `queToca elige primero el vencido y luego el fijado`() {
        val a = corriendo(300, id = "a", nombre = "A", titulo = "Pasos A")
        val b = corriendo(0, id = "b", nombre = "B", titulo = "Pasos B")
        assertEquals("B: Pasos B. Ya está, di hecho.", RespuestasVoz.queToca(listOf(a, b), "a"))
        val c = corriendo(100, id = "c", nombre = "C", titulo = "Pasos C")
        assertEquals("A: Pasos A.", RespuestasVoz.queToca(listOf(a, c), "a"))
        assertEquals("C: Pasos C.", RespuestasVoz.queToca(listOf(a, c), null))
    }

    @Test
    fun `queToca indica empezar cuando el carril aun no ha empezado`() {
        val v = vista(accion = AccionCarril.EMPEZAR, clase = ClaseEstado.YA)
        assertEquals("Toca empezar Sofrito en Sartén grande.", RespuestasVoz.queToca(listOf(v), null))
    }

    @Test
    fun `queToca de un carril bloqueado dice a quien espera`() {
        val v =
            vista(accion = AccionCarril.BLOQUEADO, clase = ClaseEstado.BLOQ)
                .copy(bloqueadoPor = "Escurrir (MESA)")
        assertEquals("Sartén grande: espera a Escurrir (MESA).", RespuestasVoz.queToca(listOf(v), null))
    }

    @Test
    fun `queToca todo terminado y lista vacia`() {
        val fin = vista(accion = AccionCarril.TERMINADO, clase = ClaseEstado.FIN)
        assertEquals("Todo terminado.", RespuestasVoz.queToca(listOf(fin, fin.copy(id = "m")), null))
        assertEquals("Todo terminado.", RespuestasVoz.queToca(emptyList(), null))
    }

    // ---- queViene

    @Test
    fun `queViene da el proximo fin con lo que sigue`() {
        val a = corriendo(120, id = "a", nombre = "SARTÉN", despues = "Reposo · fuego 8")
        val b = corriendo(600, id = "b", nombre = "MESA", despues = "Emplatar")
        assertEquals(
            "En 2 minutos, Sartén: sigue Reposo, fuego 8.",
            RespuestasVoz.queViene(listOf(b, a)),
        )
    }

    @Test
    fun `queViene sin paso siguiente dice fin del paso`() {
        val a = corriendo(45, despues = null, titulo = "Reposo")
        assertEquals("En 45 segundos, Sartén grande: fin de Reposo.", RespuestasVoz.queViene(listOf(a)))
    }

    @Test
    fun `queViene con un fin vencido dice ahora`() {
        val a = corriendo(-5, despues = "Emplatar")
        assertEquals("Ahora, Sartén grande: sigue Emplatar.", RespuestasVoz.queViene(listOf(a)))
    }

    @Test
    fun `queViene ignora pausados sin empezar y terminados`() {
        val pausa = vista(id = "p", accion = AccionCarril.SEGUIR, quedanS = 30, clase = ClaseEstado.PAUSA)
        val espera = vista(id = "e", accion = AccionCarril.EMPEZAR)
        val fin = vista(id = "f", accion = AccionCarril.TERMINADO)
        assertEquals("Nada en marcha.", RespuestasVoz.queViene(listOf(pausa, espera, fin)))
        assertEquals("Nada en marcha.", RespuestasVoz.queViene(emptyList()))
    }

    // ---- anuncio

    @Test
    fun `anuncio une los avisos y normaliza el nombre en mayusculas`() {
        val texto =
            RespuestasVoz.anuncio(
                listOf("SARTÉN GRANDE: primera fritura hecha.", "MESA: toca escurrir."),
            )
        assertEquals("Sartén grande: primera fritura hecha. Mesa: toca escurrir.", texto)
    }

    @Test
    fun `anuncio cierra frases sin punto y separa varios avisos de un carril`() {
        assertEquals(
            "Sartén grande: Primera fritura: hecho. Dales la vuelta.",
            RespuestasVoz.anuncio(listOf("SARTÉN GRANDE: Primera fritura: hecho · Dales la vuelta")),
        )
    }

    @Test
    fun `anuncio descarta lineas vacias y con lista vacia no dice nada`() {
        assertEquals("Mesa: toca escurrir.", RespuestasVoz.anuncio(listOf("", "  ", "MESA: toca escurrir")))
        assertEquals("", RespuestasVoz.anuncio(emptyList()))
    }

    @Test
    fun `anuncio respeta lo que no es un nombre en mayusculas`() {
        assertEquals("Toca escurrir la pasta.", RespuestasVoz.anuncio(listOf("Toca escurrir la pasta")))
    }

    // ---- confirmacion

    @Test
    fun `confirmacion de hecho anuncia el siguiente paso`() {
        val v = corriendo(0, despues = "Reposo · fuego 8")
        assertEquals("Hecho. Ahora, Reposo, fuego 8.", RespuestasVoz.confirmacion(OrdenVoz.Hecho(null), v))
    }

    @Test
    fun `confirmacion de hecho sin siguiente o sin vista`() {
        assertEquals(
            "Hecho. Sartén grande terminado.",
            RespuestasVoz.confirmacion(OrdenVoz.Hecho("f0"), corriendo(0)),
        )
        assertEquals("Hecho.", RespuestasVoz.confirmacion(OrdenVoz.Hecho(null), null))
    }

    @Test
    fun `confirmacion de las ordenes cortas`() {
        val v = corriendo(100)
        assertEquals("En pausa.", RespuestasVoz.confirmacion(OrdenVoz.Pausa(null), v))
        assertEquals("Treinta segundos más.", RespuestasVoz.confirmacion(OrdenVoz.Prorrogar(30, null), v))
        assertEquals("Sigo.", RespuestasVoz.confirmacion(OrdenVoz.Seguir(null), v))
        assertEquals("Silencio.", RespuestasVoz.confirmacion(OrdenVoz.Para, null))
        assertEquals("Vamos.", RespuestasVoz.confirmacion(OrdenVoz.Empezar(null), v))
    }

    @Test
    fun `confirmacion de prorrogar dice la duracion en letra`() {
        fun dice(segundos: Int) = RespuestasVoz.confirmacion(OrdenVoz.Prorrogar(segundos, null), corriendo(100))
        assertEquals("Treinta segundos más.", dice(30))
        assertEquals("Dos minutos más.", dice(120))
        assertEquals("Un minuto y medio más.", dice(90))
        assertEquals("Un segundo más.", dice(1))
        assertEquals("Un minuto más.", dice(60))
        assertEquals("Veintiún segundos más.", dice(21))
        assertEquals("Cuarenta y cinco segundos más.", dice(45))
        assertEquals("Dos minutos y medio más.", dice(150))
        assertEquals("Un minuto y veinte segundos más.", dice(80))
        assertEquals("Veintiún minutos más.", dice(1260))
        assertEquals("Treinta minutos más.", dice(1800))
    }

    @Test
    fun `confirmacion de cuanto queda reutiliza la respuesta o no entiende sin carril`() {
        assertEquals(
            "Sartén grande: quedan 1 minuto y 40 segundos.",
            RespuestasVoz.confirmacion(OrdenVoz.CuantoQueda(null), corriendo(100)),
        )
        assertEquals(RespuestasVoz.NO_ENTENDIDO, RespuestasVoz.confirmacion(OrdenVoz.CuantoQueda(null), null))
    }

    // ---- porQue

    @Test
    fun `porQue lee el texto del paso sin etiquetas`() {
        val v = corriendo(100).copy(porque = "Sella a <b>fuego fuerte</b>  para dorar")
        assertEquals("Sella a fuego fuerte para dorar.", RespuestasVoz.porQue(v))
    }

    @Test
    fun `porQue sin texto dice que no hay explicacion`() {
        assertEquals("Este paso no tiene explicación.", RespuestasVoz.porQue(corriendo(100)))
        assertEquals("Este paso no tiene explicación.", RespuestasVoz.porQue(corriendo(100).copy(porque = " <b></b> ")))
    }

    @Test
    fun `porQue recorta por palabras a 300 caracteres`() {
        val v = corriendo(100).copy(porque = "palabra ".repeat(100).trim())
        val frase = RespuestasVoz.porQue(v)
        assertTrue(frase, frase.length <= 300)
        assertTrue(frase, frase.endsWith("palabra."))
    }

    // ---- info

    @Test
    fun `info junta titulo nota fuego tapa y tiempo`() {
        val v = corriendo(120, nombre = "SARTÉN").copy(nota = "Remueve", fuego = 7, tapa = "PUESTA")
        assertEquals("Sartén: Sofrito. Remueve. Fuego 7, tapada. Quedan 2 minutos.", RespuestasVoz.info(v))
    }

    @Test
    fun `info sin nota ni fuego y segun el estado del carril`() {
        assertEquals("Sartén grande: Sofrito. Queda 1 minuto.", RespuestasVoz.info(corriendo(60)))
        assertEquals("Sartén grande: Sofrito. Ya está, di hecho.", RespuestasVoz.info(corriendo(0)))
        val pausa = vista(accion = AccionCarril.SEGUIR, quedanS = 180, clase = ClaseEstado.PAUSA)
        assertEquals("Sartén grande: Sofrito. En pausa, quedan 3 minutos.", RespuestasVoz.info(pausa))
        val sin = vista(accion = AccionCarril.EMPEZAR, clase = ClaseEstado.YA)
        assertEquals("Sartén grande: Sofrito. Sin empezar.", RespuestasVoz.info(sin))
        val fin = vista(accion = AccionCarril.TERMINADO, quedanS = 0, clase = ClaseEstado.FIN)
        assertEquals("Sartén grande: terminado.", RespuestasVoz.info(fin))
    }

    @Test
    fun `info no pasa de 200 caracteres y conserva el tiempo`() {
        val largo = "Remueve con calma ".repeat(30).trim()
        val v = corriendo(120, titulo = largo).copy(nota = largo, fuego = 5, tapa = "PUESTA")
        val frase = RespuestasVoz.info(v)
        assertTrue(frase, frase.length <= 200)
        assertTrue(frase, frase.endsWith("Fuego 5, tapada. Quedan 2 minutos."))
    }

    @Test
    fun `no entendido es la frase fija`() {
        assertEquals("No te he entendido.", RespuestasVoz.NO_ENTENDIDO)
    }

    // ---- reglas generales

    @Test
    fun `ninguna respuesta pasa de 120 caracteres aunque el titulo sea larguisimo`() {
        val largo = "Sofrito muy lento de cebolla ".repeat(8).trim()
        val v = corriendo(300, titulo = largo).copy(fuego = 5, tapa = "PUESTA", despues = largo)
        val respuestas =
            listOf(
                RespuestasVoz.queToca(listOf(v), null),
                RespuestasVoz.queViene(listOf(v)),
                RespuestasVoz.confirmacion(OrdenVoz.Hecho(null), v),
                RespuestasVoz.queToca(listOf(v.copy(accion = AccionCarril.EMPEZAR, finAtMs = null)), null),
            )
        respuestas.forEach { assertTrue(it, it.length <= 120) }
    }

    @Test
    fun `ninguna respuesta lleva emojis ni el separador visual`() {
        val v = corriendo(300, despues = "Reposo · fuego 8").copy(fuego = 5, tapa = "PUESTA")
        listOf(
            RespuestasVoz.cuantoQueda(v),
            RespuestasVoz.queToca(listOf(v), null),
            RespuestasVoz.queViene(listOf(v)),
            RespuestasVoz.confirmacion(OrdenVoz.Hecho(null), v),
        ).forEach { frase ->
            assertFalse(frase, frase.any { it.code > 0x024F })
            assertFalse(frase, "·" in frase)
        }
    }

    // ---- proximosPasos

    @Test
    fun `proximos pasos de un carril con sus duraciones`() {
        val v =
            corriendo(200, titulo = "Sofrito").copy(
                proximos = listOf(PasoProximo("Añadir el tomate", 300), PasoProximo("Reducir", 600)),
            )
        assertEquals(
            "Sartén grande, después de sofrito: añadir el tomate, 5 minutos; luego reducir, 10 minutos.",
            ProximosPasosVoz.deCarril(v),
        )
    }

    @Test
    fun `proximos pasos de un carril en su ultimo paso`() {
        assertEquals("Sartén grande: sofrito es el último paso.", ProximosPasosVoz.deCarril(corriendo(200)))
    }

    @Test
    fun `proximos pasos en general por hora prevista`() {
        val sarten =
            corriendo(200).copy(sugeridoMs = 2_000L, proximos = listOf(PasoProximo("Reducir", 600)))
        val olla =
            corriendo(400, id = "o", nombre = "OLLA", titulo = "Hervir").copy(sugeridoMs = 1_000L)
        val mesa = vista(id = "m", nombre = "MESA", accion = AccionCarril.TERMINADO)
        assertEquals(
            "Olla: hervir. Sartén grande: sofrito, luego reducir.",
            ProximosPasosVoz.general(listOf(sarten, olla, mesa)),
        )
    }
}
