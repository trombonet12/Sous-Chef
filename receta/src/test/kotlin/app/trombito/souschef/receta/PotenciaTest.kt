package app.trombito.souschef.receta

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PotenciaTest {
    private val analogico6 = Cocina(TipoCocina.GAS, Mando.ANALOGICO, 6)
    private val digital12 = Cocina(TipoCocina.INDUCCION, Mando.DIGITAL, 12)
    private val digital14 = Cocina(TipoCocina.INDUCCION, Mando.DIGITAL, 14)
    private val palabras = Cocina(TipoCocina.GAS, Mando.ANALOGICO, null)

    private fun textos(c: Cocina) = (1..9).map { Potencia.texto(it, c) }

    @Test
    fun canonicaEsVitroDigital9() {
        assertTrue(Cocina.VITRO_9.esCanonica)
        assertTrue(!Cocina(max = 12).esCanonica)
        assertTrue(!Cocina(TipoCocina.GAS).esCanonica)
        assertEquals((1..9).map { it.toString() }, textos(Cocina.VITRO_9))
    }

    @Test
    fun validaAjustaLoInvalido() {
        assertEquals(20, Cocina(max = 30).valida().max)
        assertEquals(9, Cocina(max = 5).valida().max)
        assertEquals(9, Cocina(max = null).valida().max)
        assertEquals(4, Cocina(mando = Mando.ANALOGICO, max = 2).valida().max)
        assertEquals(12, Cocina(mando = Mando.ANALOGICO, max = 20).valida().max)
        assertEquals(null, Cocina(mando = Mando.ANALOGICO, max = null).valida().max)
        assertEquals(Cocina.VITRO_9, Cocina.VITRO_9.valida())
    }

    @Test
    fun ceroEsApagado() {
        assertEquals(NivelPotencia.Apagado, Potencia.nivel(0, Cocina.VITRO_9))
        assertEquals(NivelPotencia.Apagado, Potencia.nivel(0, palabras))
        assertEquals("apagado", Potencia.texto(0, analogico6))
    }

    @Test
    fun analogicoEscala6RedondeaAMedios() {
        assertEquals(listOf("1", "1,5", "2", "2,5", "3,5", "4", "4,5", "5,5", "6"), textos(analogico6))
        assertEquals(NivelPotencia.Numero(3.5), Potencia.nivel(5, analogico6))
    }

    @Test
    fun analogicoEscala6NoColisionaEntreNiveles() {
        assertEquals(9, textos(analogico6).toSet().size)
    }

    @Test
    fun digitalEscala12() {
        assertEquals(listOf("1", "3", "4", "5", "7", "8", "9", "11", "12"), textos(digital12))
        assertEquals(NivelPotencia.Numero(12.0), Potencia.nivel(9, digital12))
    }

    @Test
    fun digitalEscala14() {
        assertEquals(listOf("2", "3", "5", "6", "8", "9", "11", "12", "14"), textos(digital14))
    }

    @Test
    fun digitalEscala9ConservaLosNumeros() {
        assertEquals(NivelPotencia.Numero(1.0), Potencia.nivel(1, Cocina.VITRO_9))
        assertEquals(NivelPotencia.Numero(9.0), Potencia.nivel(9, Cocina.VITRO_9))
    }

    @Test
    fun sinNumerosUsaPalabras() {
        assertEquals(
            listOf("mínimo", "bajo", "bajo", "medio-bajo", "medio", "medio-alto", "medio-alto", "alto", "máximo"),
            textos(palabras),
        )
        assertEquals(NivelPotencia.Palabra("medio"), Potencia.nivel(5, palabras))
    }

    @Test
    fun vitro9DejaElTextoSinLlaves() {
        assertEquals("baja del 4 al 5", Potencia.reescribir("baja del {4} al {5}", Cocina.VITRO_9))
        assertEquals("Al 7, dora; luego al 0.", Potencia.reescribir("Al {7}, dora; luego al {0}.", Cocina.VITRO_9))
    }

    @Test
    fun cocinaNumericaTraduceElNumero() {
        assertEquals("del 2,5 al 3,5", Potencia.reescribir("del {4} al {5}", analogico6))
        assertEquals("sube a 9 y luego a 0", Potencia.reescribir("sube a {7} y luego a {0}", digital12))
    }

    @Test
    fun sinTokensDevuelveLaMismaInstancia() {
        val t = "Sofríe sin prisa, al fuego 7 que ya tiene."
        assertSame(t, Potencia.reescribir(t, palabras))
        assertSame(t, Potencia.reescribir(t, Cocina.VITRO_9))
    }

    @Test
    fun gramaticaRango() {
        assertEquals(
            "baja de fuego medio-bajo a medio",
            Potencia.reescribir("baja del {4} al {5}", palabras),
        )
        assertEquals("De fuego bajo a alto.", Potencia.reescribir("Del {2} al {8}.", palabras))
        assertEquals("deja a fuego medio-alto", Potencia.reescribir("deja del {6} al {7}", palabras))
    }

    @Test
    fun gramaticaAl() {
        assertEquals(
            "Cocina a fuego medio-alto dos minutos",
            Potencia.reescribir("Cocina al {7} dos minutos", palabras),
        )
        assertEquals("Pasa a fuego bajo y tapa", Potencia.reescribir("Pasa a {3} y tapa", palabras))
    }

    @Test
    fun gramaticaFuegoNoSeDuplica() {
        assertEquals("Pon fuego medio-alto.", Potencia.reescribir("Pon fuego {7}.", palabras))
        assertEquals("Pon fuego medio-alto.", Potencia.reescribir("Pon fuego al {7}.", palabras))
        assertEquals("Déjalo a fuego medio-alto", Potencia.reescribir("Déjalo a fuego {7}", palabras))
        assertEquals("Déjalo a fuego medio-alto", Potencia.reescribir("Déjalo a fuego al {7}", palabras))
    }

    @Test
    fun gramaticaPotenciaSeConvierteEnFuego() {
        assertEquals("Sube a fuego medio-alto", Potencia.reescribir("Sube a potencia {7}", palabras))
        assertEquals("Con fuego medio-alto, dora", Potencia.reescribir("Con potencia {7}, dora", palabras))
    }

    @Test
    fun gramaticaApagado() {
        assertEquals("Retira y déjalo apagado", Potencia.reescribir("Retira y déjalo al {0}", palabras))
        assertEquals("Retira y déjalo apagado", Potencia.reescribir("Retira y déjalo a fuego {0}", palabras))
        assertEquals("Deja apagado.", Potencia.reescribir("Deja {0}.", palabras))
    }

    @Test
    fun gramaticaSueltoEsFuego() {
        assertEquals("Sube fuego alto.", Potencia.reescribir("Sube {8}.", palabras))
    }

    @Test
    fun respetaMayusculaInicial() {
        assertEquals("A fuego medio-alto, dora.", Potencia.reescribir("Al {7}, dora.", palabras))
        assertEquals("Hierve. Fuego medio un minuto", Potencia.reescribir("Hierve. {5} un minuto", palabras))
        assertEquals("Fuego medio-alto", Potencia.reescribir("{7}", palabras))
        assertEquals("Apagado.", Potencia.reescribir("{0}.", palabras))
    }

    @Test
    fun variosTokensEnUnTexto() {
        assertEquals(
            "Al fuego: a fuego alto 1 minuto y después de fuego bajo a medio.",
            Potencia.reescribir("Al fuego: al {8} 1 minuto y después del {2} al {5}.", palabras),
        )
    }

    @Test
    fun recetaConCocinaNoCambiaIdsNiF() {
        val r =
            Receta(
                slug = "x",
                titulo = "x al {7}",
                descripcion = "Sofrito al {7}.",
                notaEscala = "Nota {3}",
                lectura = "Lectura {5}",
                pie = "Pie {9}",
                porque = listOf(listOf("Por qué", "Porque al {6}")),
                cortes = listOf(Corte("Corte", p = "Corte al {4}")),
                mep = listOf(BloqueMep("t {7}", porq = "mep {2}", l = listOf("<b>A</b> · al {8}"))),
                pasos =
                    listOf(
                        Paso(
                            id = "p1",
                            t = "Sofríe al {7}",
                            carril = "f1",
                            off = 0.0,
                            min = 5.0,
                            f = 7,
                            nota = "nota del {4} al {5}",
                            p = "p al {1}",
                            meto = "meto {3}",
                            saco = "saco {0}",
                            sinHitos = "sin hitos {2}",
                            hitos = listOf(Hito(30.0, "Baja al {3}")),
                        ),
                    ),
            )
        val c = r.conCocina(palabras)
        assertEquals("p1", c.pasos[0].id)
        assertEquals(7, c.pasos[0].f)
        assertEquals("x al {7}", c.titulo)
        assertEquals("Sofrito a fuego medio-alto.", c.descripcion)
        assertEquals("Nota fuego bajo", c.notaEscala)
        assertEquals("Lectura fuego medio", c.lectura)
        assertEquals("Pie fuego máximo", c.pie)
        assertEquals(listOf("Por qué", "Porque a fuego medio-alto"), c.porque[0])
        assertEquals("Corte a fuego medio-bajo", c.cortes[0].p)
        assertEquals("mep fuego bajo", c.mep[0].porq)
        assertEquals("<b>A</b> · a fuego alto", c.mep[0].l[0])
        val p = c.pasos[0]
        assertEquals("Sofríe a fuego medio-alto", p.t)
        assertEquals("nota de fuego medio-bajo a medio", p.nota)
        assertEquals("p a fuego mínimo", p.p)
        assertEquals("meto fuego bajo", p.meto)
        assertEquals("saco apagado", p.saco)
        assertEquals("sin hitos fuego bajo", p.sinHitos)
        assertEquals("Baja a fuego bajo", p.hitos[0].t)
        assertEquals(30.0, p.hitos[0].en)
    }

    @Test
    fun recetaConCocinaCanonicaSoloQuitaLlaves() {
        val r =
            Receta(
                slug = "x",
                titulo = "x",
                descripcion = "baja del {4} al {5}",
                pasos = listOf(Paso(id = "p1", t = "Sofríe al {7}", carril = "f1", off = 0.0, min = 5.0, f = 7)),
            )
        val c = r.conCocina(Cocina.VITRO_9)
        assertEquals("baja del 4 al 5", c.descripcion)
        assertEquals("Sofríe al 7", c.pasos[0].t)
        assertEquals(7, c.pasos[0].f)
    }

    @Test
    fun recetaSinTokensDevuelveLaMismaInstancia() {
        val r = Receta(slug = "x", titulo = "x", descripcion = "nada")
        assertSame(r, r.conCocina(palabras))
    }

    @Test
    fun tecnicaConCocina() {
        val t =
            Tecnica(
                slug = "sellar",
                titulo = "Sellar",
                resumen = "Sella al {8}.",
                pasos = listOf(PasoTecnica("Calienta al {8}", p = "porque {8}")),
                consejos = listOf("No bajes del {8} al {5}"),
                cortes = listOf(Corte("c", p = "corte {1}")),
                pie = "Pie al {0}",
            )
        val c = t.conCocina(palabras)
        assertEquals("sellar", c.slug)
        assertEquals("Sella a fuego alto.", c.resumen)
        assertEquals("Calienta a fuego alto", c.pasos[0].t)
        assertEquals("porque fuego alto", c.pasos[0].p)
        assertEquals(listOf("No bajes de fuego alto a medio"), c.consejos)
        assertEquals("corte fuego mínimo", c.cortes[0].p)
        assertEquals("Pie apagado", c.pie)
    }
}
