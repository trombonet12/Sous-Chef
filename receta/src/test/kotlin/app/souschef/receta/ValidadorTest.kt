package app.souschef.receta

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidadorTest {
    private val largo = "x".repeat(130)

    private fun paso(
        id: String,
        carril: String = "a",
        off: Double = 0.0,
        min: Double = 10.0,
        extra: (Paso) -> Paso = { it },
    ): Paso {
        val base = Paso(id, id, carril, off, min, p = largo, saco = "ok", hitos = listOf(Hito(60.0)))
        return extra(base)
    }

    private fun base() =
        Receta(
            slug = "r",
            titulo = "R",
            totalMin = 10.0,
            tags = Tags(base = "arroz", tecnica = listOf("sofrito")),
            carriles = listOf(Carril("a", tipo = "fuego"), Carril("b", tipo = "mesa")),
            pasos = listOf(paso("p1"), paso("p2", carril = "b")),
        )

    private fun reglas(
        r: Receta,
        nivel: Nivel = Nivel.ERROR,
        tecnicas: Set<String>? = null,
    ) = Validador
        .receta(r, tecnicas)
        .filter { it.nivel == nivel }
        .map { it.regla }
        .toSet()

    private fun conPasos(vararg p: Paso) = base().copy(pasos = p.toList())

    @Test
    fun recetaCorrectaNoDaHallazgos() = assertEquals(emptyList(), Validador.receta(base(), setOf("sofrito")))

    @Test
    fun r1TotalMinNoCuadra() = assertTrue(1 in reglas(base().copy(totalMin = 12.0)))

    @Test
    fun r2DepInexistente() =
        assertTrue(2 in reglas(conPasos(paso("p1"), paso("p2", "b") { it.copy(dep = listOf("zz")) })))

    @Test
    fun r3CuatroFuegos() {
        val c = (1..4).map { Carril("f$it", tipo = "fuego") }
        assertTrue(3 in reglas(base().copy(carriles = c, pasos = c.map { paso("p${it.id}", it.id) })))
    }

    @Test
    fun r4FEnCarrilMesa() = assertTrue(4 in reglas(conPasos(paso("p1"), paso("p2", "b") { it.copy(f = 3) })))

    @Test
    fun r5CarrilNoDeclarado() = assertTrue(5 in reglas(conPasos(paso("p1"), paso("p2", "zz"))))

    @Test
    fun r6TagsVacios() = assertTrue(6 in reglas(base().copy(tags = Tags())))

    @Test
    fun r7HitoFueraDeDuracion() =
        assertTrue(7 in reglas(conPasos(paso("p1") { it.copy(hitos = listOf(Hito(600.0))) }, paso("p2", "b"))))

    @Test
    fun r10FuegoLargoSinHitos() =
        assertTrue(10 in reglas(conPasos(paso("p1") { it.copy(hitos = emptyList()) }, paso("p2", "b"))))

    @Test
    fun r11UltimoSinSaco() = assertTrue(11 in reglas(conPasos(paso("p1"), paso("p2", "b") { it.copy(saco = null) })))

    @Test
    fun r16Solape() = assertTrue(16 in reglas(conPasos(paso("p1"), paso("p2", off = 5.0))))

    @Test
    fun r17SIncoherente() = assertTrue(17 in reglas(conPasos(paso("p1") { it.copy(s = 100) }, paso("p2", "b"))))

    @Test
    fun r18EtiquetaHtmlNoPermitida() {
        val mep = listOf(BloqueMep("m", l = listOf("<i>x</i>")))
        assertTrue(18 in reglas(base().copy(mep = mep)))
    }

    @Test
    fun r21PCorto() =
        assertTrue(21 in reglas(conPasos(paso("p1") { it.copy(p = "corto") }, paso("p2", "b")), Nivel.AVISO))

    @Test
    fun r23TecnicaSinFicha() = assertTrue(23 in reglas(base(), Nivel.AVISO, emptySet()))

    private fun conGrupo(
        vararg i: Ingrediente,
        mep: List<String> = emptyList(),
    ) = base().copy(
        grupos = listOf(Grupo("G", i.toList())),
        mep = if (mep.isEmpty()) emptyList() else listOf(BloqueMep("m", l = mep)),
    )

    private fun ing(
        n: String,
        a: Double? = 1.0,
        txt: String? = null,
    ) = Ingrediente(n = n, a = a, u = "g", txt = txt)

    @Test
    fun r8IngredienteAusenteDelMep() =
        assertTrue(8 in reglas(conGrupo(ing("cebolla"), mep = listOf("<b>Bol</b> · 100 g de zanahoria"))))

    @Test
    fun r8PalabraCompletaConPluralYSinAcentos() {
        val r =
            conGrupo(ing("cebolla"), ing("pan"), ing("jamón"), mep = listOf("<b>Bol</b> · 2 cebollas, panes y JAMON"))
        assertTrue(8 !in reglas(r))
    }

    @Test
    fun r8NoCasaPorSubcadena() = assertTrue(8 in reglas(conGrupo(ing("sal"), mep = listOf("250 g de salsa"))))

    @Test
    fun r8PluralEsSoloTrasConsonanteDeLaLista() {
        assertTrue(8 in reglas(conGrupo(ing("tomate"), mep = listOf("3 tomatees"))))
        assertTrue(8 !in reglas(conGrupo(ing("sal"), mep = listOf("sales finas"))))
    }

    @Test
    fun r8NombreLargoCasaPorPalabraNucleo() =
        assertTrue(8 !in reglas(conGrupo(ing("carne picada mixta"), mep = listOf("500 g de carne"))))

    @Test
    fun r8CualquierPalabraClaveNoGenerica() {
        assertTrue(8 !in reglas(conGrupo(ing("semillas de sésamo"), mep = listOf("1 cdta de sésamo"))))
        assertTrue(8 !in reglas(conGrupo(ing("salsa de soja (1 cucharada)"), mep = listOf("15 ml de soja"))))
        assertTrue(8 in reglas(conGrupo(ing("salsa de soja"), mep = listOf("1 vaso de salsa y semillas"))))
    }

    @Test
    fun r8SinMepNoAplica() = assertTrue(8 !in reglas(conGrupo(ing("cebolla"))))

    @Test
    fun r13SinCantidad() = assertTrue(13 in reglas(conGrupo(ing("sal", a = null))))

    @Test
    fun r13TextoVago() {
        assertTrue(13 in reglas(conGrupo(ing("sal", a = null, txt = "al gusto"))))
        assertTrue(13 in reglas(conGrupo(ing("aceite", a = null, txt = "un Chorrito"))))
        assertTrue(13 in reglas(conGrupo(ing("sal", a = null, txt = "c/s"))))
        assertTrue(13 !in reglas(conGrupo(ing("sal", a = null, txt = "½ cdta"))))
    }

    private fun conCortes(
        off: Double,
        textoPaso: String = "Cortar",
        cortes: List<Corte> = listOf(Corte("Cebolla · dados de 4 mm")),
    ) = base().copy(
        cortes = cortes,
        pasos = listOf(paso("p1"), paso("p2", "b", off = off) { it.copy(t = textoPaso) }),
        totalMin = 10.0 + off,
    )

    @Test
    fun r14CortesSinPasoDeMesaEnElMinuto0() = assertTrue(14 in reglas(conCortes(off = 2.0)))

    @Test
    fun r14ConPasoDeMesaEnElMinuto0Pasa() = assertTrue(14 !in reglas(conCortes(off = 0.0)))

    @Test
    fun r15CortaEnMesaSinCortes() {
        val r = conCortes(off = 0.0, cortes = emptyList())
        assertTrue(15 in reglas(r, Nivel.AVISO))
        val sinCorte = conCortes(off = 0.0, textoPaso = "Reposar", cortes = emptyList())
        assertTrue(15 !in reglas(sinCorte, Nivel.AVISO))
    }

    @Test
    fun r15PicanteNoEsPicar() =
        assertTrue(15 !in reglas(conCortes(off = 0.0, textoPaso = "Salsa picante", cortes = emptyList()), Nivel.AVISO))

    @Test
    fun r19CorteSinMedida() {
        val sin = conCortes(off = 0.0, cortes = listOf(Corte("Cebolla · dados", "finos")))
        assertTrue(19 in reglas(sin, Nivel.AVISO))
        assertTrue(19 !in reglas(conCortes(off = 0.0), Nivel.AVISO))
        val enP = conCortes(off = 0.0, cortes = listOf(Corte("Cebolla", "dados de 1,5 cm")))
        assertTrue(19 !in reglas(enP, Nivel.AVISO))
    }

    private fun conPotencias(texto: String) =
        base().copy(
            totalMin = 20.0,
            pasos =
                listOf(
                    paso("p1") { it.copy(f = 7, t = "Sofreír a 7") },
                    paso("p2", off = 10.0) { it.copy(f = 3, t = texto) },
                ),
        )

    @Test
    fun r20CambioDePotenciaSinNombrarla() {
        assertTrue(20 in reglas(conPotencias("Bajar el fuego"), Nivel.AVISO))
        assertTrue(20 !in reglas(conPotencias("Bajar a 3"), Nivel.AVISO))
        assertTrue(20 in reglas(conPotencias("Bajar a 13"), Nivel.AVISO))
    }

    @Test
    fun r20PotenciaCeroAceptaApagar() {
        fun a0(t: String) =
            conPotencias(t).let { r ->
                r.copy(
                    pasos =
                        r.pasos.map {
                            if (it.id ==
                                "p2"
                            ) {
                                it.copy(f = 0)
                            } else {
                                it
                            }
                        },
                )
            }
        listOf(
            "Apagar el fuego",
            "Retira la cazuela",
            "Fuera de la placa",
            "Reposo tapado",
            "Fuera del fuego",
        ).forEach {
            assertTrue(20 !in reglas(a0(it), Nivel.AVISO), it)
        }
        assertTrue(20 in reglas(a0("Seguir"), Nivel.AVISO))
        assertTrue(20 !in reglas(conPotencias("Bajar 3/9"), Nivel.AVISO))
    }

    @Test
    fun r20PotenciaIgualNoAvisa() {
        val r = conPotencias("Seguir").let { it.copy(pasos = it.pasos.map { p -> p.copy(f = 7) }) }
        assertTrue(20 !in reglas(r, Nivel.AVISO))
    }

    @Test
    fun r24VetoEnIngrediente() {
        assertTrue(24 in reglas(conGrupo(ing("champiñones laminados")), Nivel.AVISO))
        assertTrue(24 in reglas(conGrupo(ing("Gambas")), Nivel.AVISO))
        assertTrue(24 !in reglas(conGrupo(ing("cebolla")), Nivel.AVISO))
    }

    @Test
    fun losDatosRealesNoTienenErrores() {
        val data = File("../data")
        val tecnicas = File(data, "tecnicas").listFiles { f -> f.extension == "json" }.orEmpty()
        val existentes = tecnicas.map { it.nameWithoutExtension }.toSet()
        val hallazgos =
            File(data, "recetas").listFiles { f -> f.extension == "json" }.orEmpty().flatMap {
                Validador.receta(leerReceta(it.readText()), existentes)
            } + tecnicas.flatMap { Validador.tecnica(leerTecnica(it.readText())) }
        val e = hallazgos.filter { it.nivel == Nivel.ERROR }
        assertTrue(e.isEmpty(), e.joinToString("\n"))
    }
}
