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
