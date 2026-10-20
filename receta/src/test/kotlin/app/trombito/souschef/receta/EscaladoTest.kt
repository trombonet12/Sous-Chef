package app.trombito.souschef.receta

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EscaladoTest {
    @Test
    fun linealDobla() {
        assertEquals("500 g", Escalado.cantidad(Ingrediente(n = "carne", a = 250.0, u = "g"), 2.0))
    }

    @Test
    fun menosDeDiezLlevaUnDecimalConComa() {
        assertEquals("2,5 cdta", Escalado.cantidad(Ingrediente(n = "sal", a = 5.0, u = "cdta"), 0.5))
        assertEquals("3 uds", Escalado.cantidad(Ingrediente(n = "huevos", a = 1.5, u = "uds"), 2.0))
    }

    @Test
    fun sublinealUsaExponente075YMarca() {
        // 4 g · 2^0,75 = 6,73 → "6,7 g *"
        assertEquals("6,7 g *", Escalado.cantidad(Ingrediente(n = "sal", a = 4.0, u = "g", esc = "sub"), 2.0))
        assertEquals("4 g", Escalado.cantidad(Ingrediente(n = "sal", a = 4.0, u = "g", esc = "sub"), 1.0))
    }

    @Test
    fun fijoNoEscalaNiMarca() {
        assertEquals("300 ml", Escalado.cantidad(Ingrediente(n = "aceite", a = 300.0, u = "ml", esc = "fijo"), 2.0))
        assertEquals("300 ml", Escalado.cantidad(Ingrediente(n = "aceite", a = 300.0, u = "ml", esc = "fijo"), 0.5))
        assertEquals("1 vaso", Escalado.cantidad(Ingrediente(n = "aceite", txt = "1 vaso", esc = "fijo"), 3.0))
    }

    @Test
    fun listaGeneralNoSumaFijoConNoFijo() {
        val r =
            Receta(
                slug = "x",
                titulo = "x",
                grupos =
                    listOf(
                        Grupo("A", listOf(Ingrediente("aceite", 300.0, "ml", esc = "fijo"))),
                        Grupo(
                            "B",
                            listOf(Ingrediente("aceite", 20.0, "ml"), Ingrediente("aceite", 200.0, "ml", esc = "fijo")),
                        ),
                    ),
            )
        val lista = Escalado.listaGeneral(r)
        assertEquals(listOf(500.0, 20.0), lista.map { it.a })
        assertEquals(listOf("fijo", null), lista.map { it.esc })
    }

    @Test
    fun textoNoEscala() {
        assertEquals("½ cdta *", Escalado.cantidad(Ingrediente(n = "pimienta", txt = "½ cdta", esc = "sub"), 2.0))
        assertEquals("1 pizca", Escalado.cantidad(Ingrediente(n = "x", txt = "1 pizca"), 3.0))
    }

    @Test
    fun listaGeneralSumaElMismoIngredienteDeVariasZonas() {
        val r =
            Receta(
                slug = "x",
                titulo = "x",
                grupos =
                    listOf(
                        Grupo("Sartén", listOf(Ingrediente("Agua", 100.0, "ml"), Ingrediente("Sal", txt = "1 pizca"))),
                        Grupo("Olla", listOf(Ingrediente("arroz", 150.0, "g"), Ingrediente(" agua ", 270.0, "ml"))),
                    ),
            )
        val lista = Escalado.listaGeneral(r)
        assertEquals(listOf("Agua", "Sal", "arroz"), lista.map { it.n })
        assertEquals(370.0, lista[0].a)
    }

    @Test
    fun listaGeneralSeparaUnidadesDistintasYNoRepiteTextos() {
        val r =
            Receta(
                slug = "x",
                titulo = "x",
                grupos =
                    listOf(
                        Grupo("A", listOf(Ingrediente("aceite", 15.0, "ml"), Ingrediente("sal", txt = "al gusto"))),
                        Grupo("B", listOf(Ingrediente("Aceite", 1.0, "cda"), Ingrediente("sal", txt = "al gusto"))),
                    ),
            )
        val lista = Escalado.listaGeneral(r)
        assertEquals(3, lista.size)
        assertEquals(listOf("ml", null, "cda"), lista.map { it.u })
    }

    @Test
    fun listaGeneralJuntaPorNombreBaseSinEquivalenciasNiColetillas() {
        val r =
            Receta(
                slug = "x",
                titulo = "x",
                grupos =
                    listOf(
                        Grupo(
                            "A",
                            listOf(
                                Ingrediente("harina (5 cucharadas soperas)", 50.0, "g"),
                                Ingrediente("sal (⅓ de cucharadita)", 2.0, "g", esc = "sub"),
                            ),
                        ),
                        Grupo(
                            "B",
                            listOf(
                                Ingrediente("harina (2 cucharadas soperas)", 20.0, "g"),
                                Ingrediente("sal, al salir de la sartén (una pizca)", 1.0, "g", esc = "sub"),
                            ),
                        ),
                        Grupo("C", listOf(Ingrediente("cebolla, en aros de 1 cm (1 cebolla grande)", 250.0, "g"))),
                    ),
            )
        val lista = Escalado.listaGeneral(r)
        assertEquals(listOf("harina", "sal", "cebolla, en aros de 1 cm (1 cebolla grande)"), lista.map { it.n })
        assertEquals(listOf(70.0, 3.0, 250.0), lista.map { it.a })
    }

    private fun conEscala(escala: Escala?) = Receta(slug = "x", titulo = "x", escala = escala)

    @Test
    fun racionesPermitidasSinEscalaEsElRangoGlobal() {
        assertEquals((1..20).toList(), Escalado.racionesPermitidas(conEscala(null)))
    }

    @Test
    fun racionesPermitidasConEscalaVaDeMinAMaxDePasoEnPaso() {
        assertEquals(listOf(2, 3, 4), Escalado.racionesPermitidas(conEscala(Escala(2, 4))))
        assertEquals(listOf(2, 4, 6), Escalado.racionesPermitidas(conEscala(Escala(2, 6, 2))))
        assertEquals(listOf(2, 4, 6), Escalado.racionesPermitidas(conEscala(Escala(2, 7, 2))))
    }

    @Test
    fun racionesPermitidasConEscalaInvalidaCaeAlRangoGlobal() {
        assertEquals((1..20).toList(), Escalado.racionesPermitidas(conEscala(Escala(1, 4, 0))))
        assertEquals((1..20).toList(), Escalado.racionesPermitidas(conEscala(Escala(5, 2))))
    }

    @Test
    fun racionesInicialesUsaLaGuardadaSiEstaPermitida() {
        assertEquals(3, Escalado.racionesIniciales(conEscala(Escala(2, 4)), guardada = 3, defecto = 2))
        assertEquals(7, Escalado.racionesIniciales(conEscala(null), guardada = 7, defecto = 2))
    }

    @Test
    fun racionesInicialesSinGuardadaValidaVaAlMasCercanoAlDefecto() {
        val r = conEscala(Escala(2, 6, 2))
        assertEquals(2, Escalado.racionesIniciales(r, guardada = null, defecto = 1))
        assertEquals(6, Escalado.racionesIniciales(r, guardada = null, defecto = 9))
        assertEquals(4, Escalado.racionesIniciales(r, guardada = 3, defecto = 4))
        assertEquals(4, Escalado.racionesIniciales(r, guardada = 20, defecto = 5), "5 equidista de 4 y 6: el menor")
        assertEquals(2, Escalado.racionesIniciales(conEscala(null), guardada = null, defecto = 2))
    }

    @Test
    fun minutosAMedios() {
        assertEquals("2,5", Escalado.minutos(2.5))
        assertEquals("3", Escalado.minutos(3.0))
        assertEquals("114,5", Escalado.minutos(114.5))
    }

    @Test
    fun ejeDelGantt() {
        // 34 min: granu 5, salto 10; 30 se quita por estar a menos de medio salto de 34.
        assertEquals(listOf(0.0, 10.0, 20.0, 34.0), Gantt.marcas(34.0))
        // 82 min: granu 10, salto 20.
        assertEquals(listOf(0.0, 20.0, 40.0, 60.0, 82.0), Gantt.marcas(82.0))
        assertEquals(listOf(0.0, 10.0, 20.0, 30.0, 40.0, 50.0), Gantt.marcas(50.0))
    }

    @Test
    fun barraMinimaAlFinalNoSePasa() {
        // pollo-almendras: el último paso (1 min) acaba justo en el 136; con ancho mínimo 1 % no cabía y reventaba.
        val paso = Paso(id = "fin", t = "Fin", carril = "c", off = 135.0, min = 1.0)
        val (izq, ancho) = Gantt.barra(paso, 136.0, anchoMinimo = 0.01)
        assertEquals(0.01f, ancho, 1e-6f)
        assertTrue(izq >= 0f && izq + ancho <= 1f + 1e-6f, "izq=$izq ancho=$ancho")
    }

    @Test
    fun barraNormalNoCambia() {
        val (izq, ancho) = Gantt.barra(Paso(id = "a", t = "A", carril = "c", off = 10.0, min = 20.0), 100.0, 0.01)
        assertEquals(0.1f, izq, 1e-6f)
        assertEquals(0.2f, ancho, 1e-6f)
    }
}
