package app.souschef.ui.crono.voz

import app.souschef.domain.voz.Escucha
import app.souschef.domain.voz.Habla
import app.souschef.receta.crono.ClaseEstado
import app.souschef.ui.crono.AccionCarril
import app.souschef.ui.crono.VistaCarril
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class EscuchaFalsa : Escucha {
    override val disponible = true
    val llamadas = mutableListOf<String>()
    var alOir: ((List<String>) -> Unit)? = null

    override fun empezar(alOir: (List<String>) -> Unit) {
        this.alOir = alOir
        llamadas += "empezar"
    }

    override fun pausar() {
        llamadas += "pausar"
    }

    override fun reanudar() {
        llamadas += "reanudar"
    }

    override fun parar() {
        llamadas += "parar"
    }

    fun oir(vararg alternativas: String) = checkNotNull(alOir)(alternativas.toList())
}

private class HablaFalsa(
    private val registro: MutableList<String>,
) : Habla {
    val dichas = mutableListOf<String>()
    private var alAcabar: () -> Unit = {}

    override fun decir(
        texto: String,
        alAcabar: () -> Unit,
    ) {
        dichas += texto
        registro += "decir"
        this.alAcabar = alAcabar
    }

    override fun callar() {
        registro += "callar"
    }

    override fun liberar() {
        registro += "liberar"
    }

    fun terminar() = alAcabar()
}

class ControlVozTest {
    private val escucha = EscuchaFalsa()
    private val habla = HablaFalsa(escucha.llamadas)
    private val mandos = mutableListOf<String>()
    private val ordenes = mutableListOf<String>()
    private var vistas = listOf(sarten(), mesa())
    private var foco: String? = null

    private val control =
        ControlVoz(
            escucha = escucha,
            habla = habla,
            vistas = { vistas },
            foco = { foco },
            mandos =
                MandosVoz(
                    hecho = { mandos += "hecho:$it" },
                    alternar = { mandos += "alternar:$it" },
                    masTreinta = { mandos += "mas30:$it" },
                    empezarIgual = { mandos += "igual:$it" },
                    silenciarAlarma = { mandos += "silenciar" },
                ),
            registro = { ordenes += it },
        ).also {
            it.puedeHablar = true
            it.escuchar()
        }

    private fun vista(
        id: String,
        nombre: String,
        accion: AccionCarril,
        quedanS: Int = 120,
        finAtMs: Long? = null,
    ) = VistaCarril(
        id = id,
        nombre = nombre,
        colorHex = "#E4572E",
        indiceCarril = 0,
        paso = 1,
        pasos = 2,
        titulo = "Sofrito",
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
        clase = ClaseEstado.MARCHA,
        sugeridoMs = null,
        finAtMs = finAtMs,
        tienePorque = false,
    )

    private fun sarten(accion: AccionCarril = AccionCarril.PAUSAR) =
        vista("f0", "SARTÉN GRANDE", accion, finAtMs = 1_000_000L)

    private fun mesa(accion: AccionCarril = AccionCarril.EMPEZAR) = vista("m", "MESA", accion)

    // ---- órdenes que accionan

    @Test
    fun hechoSinCarrilUsaElDelFocoYConfirma() {
        escucha.oir("chef hecho")
        assertEquals(listOf("hecho:f0"), mandos)
        assertEquals(1, habla.dichas.size)
        assertTrue(habla.dichas.single().startsWith("Hecho"))
    }

    @Test
    fun hechoConCarrilNombradoUsaEseCarril() {
        escucha.oir("chef mesa hecho")
        assertEquals(listOf("hecho:m"), mandos)
    }

    @Test
    fun elFocoFijadoGanaSiNadaVencido() {
        foco = "m"
        escucha.oir("chef hecho")
        assertEquals(listOf("hecho:m"), mandos)
    }

    @Test
    fun pausaAlternaSoloSiCorre() {
        escucha.oir("chef pausa")
        assertEquals(listOf("alternar:f0"), mandos)
        assertEquals(listOf("En pausa."), habla.dichas)
    }

    @Test
    fun pausaConElCarrilYaPausadoNoTocaNadaYDiceComoEsta() {
        vistas = listOf(sarten(AccionCarril.SEGUIR), mesa())
        escucha.oir("chef pausa")
        assertTrue(mandos.isEmpty())
        assertEquals("Sartén grande: en pausa, quedan 2 minutos.", habla.dichas.single())
    }

    @Test
    fun seguirAlternaUnCarrilEnPausa() {
        vistas = listOf(sarten(AccionCarril.SEGUIR), mesa())
        escucha.oir("chef sigue")
        assertEquals(listOf("alternar:f0"), mandos)
        assertEquals(listOf("Sigo."), habla.dichas)
    }

    @Test
    fun empezarArrancaElCarrilNombrado() {
        escucha.oir("chef empieza la mesa")
        assertEquals(listOf("alternar:m"), mandos)
        assertEquals(listOf("Vamos."), habla.dichas)
    }

    @Test
    fun empezarUnCarrilBloqueadoEmpiezaIgual() {
        vistas = listOf(sarten(), mesa(AccionCarril.BLOQUEADO))
        escucha.oir("chef empieza mesa")
        assertEquals(listOf("igual:m"), mandos)
    }

    @Test
    fun masTreinta() {
        escucha.oir("chef treinta segundos más")
        assertEquals(listOf("mas30:f0"), mandos)
        assertEquals(listOf("Treinta segundos más."), habla.dichas)
    }

    @Test
    fun paraSilenciaLaAlarma() {
        escucha.oir("chef silencio")
        assertEquals(listOf("silenciar"), mandos)
        assertEquals(listOf("Silencio."), habla.dichas)
    }

    // ---- preguntas

    @Test
    fun cuantoQuedaDiceElTiempoDelCarrilDelFoco() {
        escucha.oir("chef cuánto queda")
        assertEquals(listOf("Sartén grande: quedan 2 minutos."), habla.dichas)
        assertTrue(mandos.isEmpty())
    }

    @Test
    fun queTocaYQueViene() {
        escucha.oir("chef qué toca")
        escucha.oir("chef qué viene")
        assertEquals(
            listOf(RespuestasVoz.queToca(vistas, null), RespuestasVoz.queViene(vistas)),
            habla.dichas,
        )
    }

    @Test
    fun repiteDiceOtraVezLaUltimaFrase() {
        escucha.oir("chef cuánto queda")
        escucha.oir("chef repite")
        assertEquals(2, habla.dichas.size)
        assertEquals(habla.dichas[0], habla.dichas[1])
    }

    @Test
    fun repiteSinNadaDichoNoDiceNada() {
        escucha.oir("chef repite")
        assertTrue(habla.dichas.isEmpty())
    }

    // ---- lo que no es una orden

    @Test
    fun nombrarAlChefSinOrdenDiceNoEntendido() {
        escucha.oir("chef bailemos un vals")
        assertEquals(listOf(RespuestasVoz.NO_ENTENDIDO), habla.dichas)
        assertTrue(mandos.isEmpty())
    }

    @Test
    fun sinChefNoSeHaceNada() {
        escucha.oir("hecho", "cuánto queda")
        assertTrue(habla.dichas.isEmpty())
        assertTrue(mandos.isEmpty())
        assertTrue(ordenes.isEmpty())
    }

    // ---- pausa de la escucha al hablar

    @Test
    fun alHablarPausaLaEscuchaYLaReanudaAlAcabar() {
        escucha.llamadas.clear()
        escucha.oir("chef cuánto queda")
        assertEquals(listOf("pausar", "decir"), escucha.llamadas)
        habla.terminar()
        assertEquals(listOf("pausar", "decir", "reanudar"), escucha.llamadas)
    }

    @Test
    fun siNoPuedeHablarNoDiceNadaPeroSiAcciona() {
        control.puedeHablar = false
        escucha.oir("chef hecho")
        assertEquals(listOf("hecho:f0"), mandos)
        assertTrue(habla.dichas.isEmpty())
    }

    // ---- avisos de la app

    @Test
    fun anunciaLosAvisosEnVozAlta() {
        escucha.llamadas.clear()
        control.anunciar(listOf("SARTÉN GRANDE: Sofrito listo", "MESA: Cortar"))
        assertEquals(listOf("Sartén grande: Sofrito listo. Mesa: Cortar."), habla.dichas)
        assertEquals(listOf("pausar", "decir"), escucha.llamadas)
    }

    @Test
    fun sinAvisosNoAnuncia() {
        control.anunciar(emptyList())
        assertTrue(habla.dichas.isEmpty())
    }

    // ---- registro y ciclo de vida

    @Test
    fun alRegistroSoloLlegaElTipoDeOrden() {
        escucha.oir("chef hecho")
        escucha.oir("chef bailemos")
        assertEquals(listOf("Hecho", "NoEntendido"), ordenes)
    }

    @Test
    fun pararSueltaElMicrofonoYCallaLaVoz() {
        escucha.llamadas.clear()
        control.parar()
        assertEquals(listOf("parar", "callar"), escucha.llamadas)
        control.liberar()
        assertEquals("liberar", escucha.llamadas.last())
    }
}
