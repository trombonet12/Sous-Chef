package app.trombito.souschef.receta

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Reglas 30-36 (legibilidad, docs/analisis/legibilidad-crono-y-textos.md § 6.4): solo corren con `--legibilidad`. */
class ValidadorLegibilidadTest {
    private val p0 = "Parrafo de porque con palabras distintas de cualquier senal. ".repeat(3).trim()

    private fun paso(
        id: String = "p1",
        off: Double = 0.0,
        f: Int? = null,
        extra: (Paso) -> Paso = { it },
    ) = extra(Paso(id, "Sofreir cebolla", "a", off, 10.0, f = f, p = p0))

    private fun base(vararg pasos: Paso) =
        Receta(
            slug = "r",
            titulo = "R",
            totalMin = 10.0,
            carriles = listOf(Carril("a", tipo = "fuego")),
            pasos = pasos.toList().ifEmpty { listOf(paso()) },
        )

    private fun hallazgos(r: Receta) = Validador.legibilidad(r)

    private fun reglas(r: Receta) = hallazgos(r).map { it.regla }.toSet()

    private fun conPaso(extra: (Paso) -> Paso) = base(paso(extra = extra))

    @Test
    fun recetaLimpiaNoDaHallazgos() = assertEquals(emptyList(), hallazgos(base()))

    @Test
    fun laValidacionNormalNoEjecutaLasReglasDeLegibilidad() {
        val r = conPaso { it.copy(t = "x".repeat(80), nota = "y".repeat(200)) }
        assertTrue(Validador.receta(r).none { it.regla in 30..36 })
    }

    @Test
    fun notaAusenteOVaciaEsValida() {
        assertEquals(emptyList(), hallazgos(conPaso { it.copy(nota = null) }))
        assertEquals(emptyList(), hallazgos(conPaso { it.copy(nota = "") }))
    }

    // Regla 30
    @Test
    fun r30TituloDe36EsValidoYDe37No() {
        assertTrue(30 !in reglas(conPaso { it.copy(t = "x".repeat(36)) }))
        assertTrue(30 in reglas(conPaso { it.copy(t = "x".repeat(37)) }))
    }

    @Test
    fun r30LaMarcaDePotenciaCuentaUnCaracterYLaEtiquetaCero() {
        assertTrue(30 !in reglas(conPaso { it.copy(t = "Subir al {5} " + "x".repeat(22)) }))
        assertTrue(30 !in reglas(conPaso { it.copy(t = "<b>" + "x".repeat(36) + "</b>") }))
        assertTrue(30 in reglas(conPaso { it.copy(t = "{5}".repeat(37)) }))
    }

    @Test
    fun r30DuracionEscritaEnElTitulo() {
        assertTrue(30 in reglas(conPaso { it.copy(t = "Hervir 10 min") }))
        assertTrue(30 in reglas(conPaso { it.copy(t = "Dorar 30 s") }))
        assertTrue(30 in reglas(conPaso { it.copy(t = "Dorar 2 minutos") }))
        assertTrue(30 !in reglas(conPaso { it.copy(t = "Dorar 2 sartenes") }))
        assertTrue(30 !in reglas(conPaso { it.copy(t = "Subir al {5}") }))
    }

    // Regla 31
    @Test
    fun r31NotaDe80EsValidaYDe81No() {
        assertTrue(31 !in reglas(conPaso { it.copy(nota = "x".repeat(80)) }))
        assertTrue(31 in reglas(conPaso { it.copy(nota = "x".repeat(81)) }))
    }

    @Test
    fun r31MaximoDosFrases() {
        assertTrue(31 !in reglas(conPaso { it.copy(nota = "Dora. Punto: grasa clara.") }))
        assertTrue(31 in reglas(conPaso { it.copy(nota = "Uno. Dos. Tres.") }))
        assertTrue(31 !in reglas(conPaso { it.copy(nota = "Echa 1.5 dientes y mira") }))
    }

    @Test
    fun r31SinMinutoDeReloj() {
        assertTrue(31 in reglas(conPaso { it.copy(nota = "Al minuto 3 dale la vuelta") }))
        assertTrue(31 !in reglas(conPaso { it.copy(nota = "Dale la vuelta cuando dore") }))
    }

    @Test
    fun r31SinLaPotenciaDeF() {
        assertTrue(31 in reglas(base(paso(f = 4) { it.copy(nota = "Mantén el {4} y mira") })))
        assertTrue(31 !in reglas(base(paso(f = 4) { it.copy(nota = "Baja al {2} si humea") })))
        assertTrue(31 !in reglas(conPaso { it.copy(nota = "Mantén el {4} y mira") }))
    }

    // Regla 32
    @Test
    fun r32ElementosDeMetoYSacoDe36() {
        val ok = "x".repeat(36)
        assertTrue(32 !in reglas(conPaso { it.copy(meto = "$ok + $ok", saco = ok) }))
        assertTrue(32 in reglas(conPaso { it.copy(meto = "x".repeat(37)) }))
        assertTrue(32 in reglas(conPaso { it.copy(saco = "$ok + ${"x".repeat(37)}") }))
    }

    @Test
    fun r32LaLongitudEsPorElementoNoDelTotal() {
        val largo = "500 g de patata + 200 g de cebolla + 300 g de pimiento + 100 g de jamón"
        assertTrue(32 !in reglas(conPaso { it.copy(meto = largo) }))
    }

    @Test
    fun r32SinMedidaDeCorte() {
        assertTrue(32 in reglas(conPaso { it.copy(meto = "cebolla en 5 mm") }))
        assertTrue(32 in reglas(conPaso { it.copy(saco = "patata en dados de 2cm") }))
        assertTrue(32 !in reglas(conPaso { it.copy(meto = "300 g de cebolla") }))
    }

    // Regla 33
    @Test
    fun r33HitoDe44EsValidoYDe45No() {
        assertTrue(33 !in reglas(conPaso { it.copy(hitos = listOf(Hito(60.0, "x".repeat(44)))) }))
        assertTrue(33 in reglas(conPaso { it.copy(hitos = listOf(Hito(60.0, "x".repeat(45)))) }))
    }

    // Regla 34
    @Test
    fun r34PDeMasDe600() {
        val parrafos = List(3) { "x".repeat(200) }.joinToString("\n\n") // 604 con separadores
        assertTrue(34 in reglas(conPaso { it.copy(p = parrafos) }))
        assertTrue(34 !in reglas(conPaso { it.copy(p = List(3) { "x".repeat(190) }.joinToString("\n\n")) }))
    }

    @Test
    fun r34ParrafoDeMasDe240() {
        assertTrue(34 !in reglas(conPaso { it.copy(p = "x".repeat(240)) }))
        assertTrue(34 in reglas(conPaso { it.copy(p = "x".repeat(241)) }))
        assertTrue(34 in reglas(conPaso { it.copy(p = "x".repeat(100) + "\n\n" + "y".repeat(241)) }))
    }

    @Test
    fun r34ComoMuchoCuatroParrafos() {
        val cuatro = List(4) { "x".repeat(50) }.joinToString("\n\n")
        val cinco = List(5) { "x".repeat(50) }.joinToString("\n\n")
        assertTrue(34 !in reglas(conPaso { it.copy(p = cuatro) }))
        assertTrue(34 in reglas(conPaso { it.copy(p = cinco) }))
    }

    @Test
    fun r34UnSaltoSencilloNoSeparaParrafos() {
        val dosLineas = "x".repeat(150) + "\n" + "y".repeat(150)
        assertTrue(34 in reglas(conPaso { it.copy(p = dosLineas) }))
    }

    @Test
    fun r34Descripcion() {
        assertTrue(34 !in reglas(base().copy(descripcion = "x".repeat(200))))
        assertTrue(34 in reglas(base().copy(descripcion = "x".repeat(201))))
    }

    @Test
    fun r34LecturaDe800YConSalto() {
        val conSalto = "x".repeat(300) + "\n\n" + "y".repeat(300)
        assertEquals(emptyList(), hallazgos(base().copy(lectura = conSalto)))
        assertTrue(34 in reglas(base().copy(lectura = "x".repeat(300))))
        assertTrue(34 in reglas(base().copy(lectura = "x".repeat(500) + "\n" + "y".repeat(301))))
        assertTrue(34 !in reglas(base().copy(lectura = null)))
    }

    @Test
    fun r34PorqueYPorqDeMep() {
        assertTrue(34 !in reglas(base().copy(porque = listOf(listOf("Titulo", "x".repeat(280))))))
        assertTrue(34 in reglas(base().copy(porque = listOf(listOf("Titulo", "x".repeat(281))))))
        assertTrue(34 !in reglas(base().copy(mep = listOf(BloqueMep("Corte", porq = "x".repeat(280))))))
        assertTrue(34 in reglas(base().copy(mep = listOf(BloqueMep("Corte", porq = "x".repeat(281))))))
    }

    // Regla 35
    @Test
    fun r35OrdenDeOffNoDecreciente() {
        val ok = base(paso("p1", off = 0.0), paso("p2", off = 3.0), paso("p3", off = 3.0))
        assertEquals(emptyList(), hallazgos(ok))
        val mal = base(paso("p1", off = 0.0), paso("p2", off = 5.0), paso("p3", off = 3.0))
        val h = hallazgos(mal).single { it.regla == 35 }
        assertEquals(Nivel.ERROR, h.nivel)
        assertTrue("p3" in h.texto)
    }

    // Regla 36
    @Test
    fun r36FraseDeLaNotaRepetidaEnP() {
        val p = "Dora la cebolla hasta que quede transparente y la grasa salga clara del fondo del recipiente."
        val repetida = "Cebolla transparente y grasa clara del fondo"
        val h = hallazgos(conPaso { it.copy(p = p, nota = repetida) })
        assertEquals(setOf(36), h.map { it.regla }.toSet())
        assertEquals(Nivel.AVISO, h.single().nivel)
    }

    @Test
    fun r36FraseNuevaNoAvisa() {
        val p = "Dora la cebolla hasta que quede transparente."
        assertTrue(36 !in reglas(conPaso { it.copy(p = p, nota = "Punto: chirrido, no silbido") }))
    }

    @Test
    fun r36SeEvaluaFraseAFrase() {
        val p = "Dora la cebolla hasta que quede transparente y la grasa salga clara del fondo."
        val nota = "Punto: chirrido, no silbido. Cebolla transparente y grasa clara del fondo."
        assertTrue(36 in reglas(conPaso { it.copy(p = p, nota = nota) }))
    }

    @Test
    fun r36LasFrasesCortasNoCuentan() {
        val p = "Dora la cebolla hasta que quede transparente."
        assertTrue(36 !in reglas(conPaso { it.copy(p = p, nota = "Cebolla transparente") }))
    }

    @Test
    fun r36IgnoraAcentosPluralesYMarcas() {
        val p = "Las cebollas deben quedar transparentes antes de añadir el tomate triturado a {4}."
        val nota = "Cebolla transparente, tomate triturado, añadir"
        assertTrue(36 in reglas(conPaso { it.copy(p = p, nota = nota) }))
    }

    @Test
    fun r36ElUmbralEsExactamenteElOchentaPorCiento() {
        val p = "cebolla transparente grasa clara"
        assertTrue(36 in reglas(conPaso { it.copy(p = p, nota = "Cebolla transparente grasa clara fondo") }))
        assertTrue(36 !in reglas(conPaso { it.copy(p = p, nota = "Cebolla transparente grasa fondo humo") }))
    }
}
