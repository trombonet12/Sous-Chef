package app.trombito.souschef.receta.voz

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InterpreteVozTest {
    private val carriles =
        listOf(
            CarrilVoz("sg", "SARTÉN GRANDE", 1),
            CarrilVoz("sp", "SARTÉN PEQUEÑA", 2),
            CarrilVoz("olla", "OLLA", 3),
            CarrilVoz("mesa", "MESA", 4),
            CarrilVoz("horno", "HORNO", 5),
            CarrilVoz("cazo", "CAZO", 6),
        )

    private fun i(texto: String) = InterpreteVoz.interpretar(texto, carriles)

    // --- Órdenes sin carril ---

    @Test
    fun `hecho sin carril`() = assertEquals(OrdenVoz.Hecho(null), i("chef hecho"))

    @Test
    fun `listo siguiente y ya esta son Hecho`() {
        assertEquals(OrdenVoz.Hecho(null), i("Chef, listo"))
        assertEquals(OrdenVoz.Hecho(null), i("chef siguiente"))
        assertEquals(OrdenVoz.Hecho(null), i("chef, ya está"))
    }

    @Test
    fun `pausa en sus variantes`() {
        assertEquals(OrdenVoz.Pausa(null), i("chef pausa"))
        assertEquals(OrdenVoz.Pausa(null), i("chef para el tiempo"))
        assertEquals(OrdenVoz.Pausa(null), i("chef espera"))
    }

    @Test
    fun `seguir en sus variantes`() {
        assertEquals(OrdenVoz.Seguir(null), i("chef sigue"))
        assertEquals(OrdenVoz.Seguir(null), i("chef continúa"))
        assertEquals(OrdenVoz.Seguir(null), i("chef reanuda"))
    }

    @Test
    fun `empezar en sus variantes`() {
        assertEquals(OrdenVoz.Empezar(null), i("chef empieza"))
        assertEquals(OrdenVoz.Empezar(null), i("chef arranca"))
        assertEquals(OrdenVoz.Empezar(null), i("chef, vamos"))
    }

    @Test
    fun `prorrogar 30 con palabra, cifra y poco mas`() {
        assertEquals(OrdenVoz.Prorrogar(30, null), i("chef treinta segundos más"))
        assertEquals(OrdenVoz.Prorrogar(30, null), i("chef 30 más"))
        assertEquals(OrdenVoz.Prorrogar(30, null), i("chef 30 segundos más"))
        assertEquals(OrdenVoz.Prorrogar(30, null), i("chef un poco más"))
    }

    @Test
    fun `cuanto queda y cuanto falta`() {
        assertEquals(OrdenVoz.CuantoQueda(null), i("chef cuánto queda"))
        assertEquals(OrdenVoz.CuantoQueda(null), i("chef, ¿cuánto falta?"))
    }

    @Test
    fun `que toca y que hago`() {
        assertEquals(OrdenVoz.QueToca(null), i("chef qué toca"))
        assertEquals(OrdenVoz.QueToca(null), i("chef, ¿qué hago?"))
    }

    @Test
    fun `que viene, y luego y que sigue`() {
        assertEquals(OrdenVoz.QueViene, i("chef qué viene"))
        assertEquals(OrdenVoz.QueViene, i("chef y luego"))
        assertEquals(OrdenVoz.QueViene, i("chef qué sigue"))
    }

    @Test
    fun `para calla silencio y apaga la alarma`() {
        assertEquals(OrdenVoz.Para, i("chef para"))
        assertEquals(OrdenVoz.Para, i("chef calla"))
        assertEquals(OrdenVoz.Para, i("chef silencio"))
        assertEquals(OrdenVoz.Para, i("chef apaga la alarma"))
    }

    @Test
    fun `repite`() {
        assertEquals(OrdenVoz.Repite, i("chef repite"))
        assertEquals(OrdenVoz.Repite, i("chef otra vez"))
        assertEquals(OrdenVoz.Repite, i("chef, ¿qué has dicho?"))
    }

    // --- Carriles ---

    @Test
    fun `carril por nombre de una palabra`() {
        assertEquals(OrdenVoz.Hecho("olla"), i("chef hecho la olla"))
        assertEquals(OrdenVoz.Pausa("horno"), i("chef pausa el horno"))
        assertEquals(OrdenVoz.Seguir("mesa"), i("chef sigue la mesa"))
    }

    @Test
    fun `carril por adjetivo`() {
        assertEquals(OrdenVoz.Hecho("sg"), i("chef listo la grande"))
        assertEquals(OrdenVoz.Empezar("sp"), i("chef empieza la pequeña"))
    }

    @Test
    fun `carril por nombre completo con dos palabras`() {
        assertEquals(OrdenVoz.Prorrogar(30, "sg"), i("chef 30 más la sartén grande"))
        assertEquals(OrdenVoz.CuantoQueda("sp"), i("chef cuánto queda de la sartén pequeña"))
    }

    @Test
    fun `carril ambiguo da null en el carril pero conserva la orden`() {
        assertEquals(OrdenVoz.Hecho(null), i("chef hecho la sartén"))
    }

    @Test
    fun `carril inexistente da null en el carril`() {
        assertEquals(OrdenVoz.Pausa(null), i("chef pausa la freidora"))
    }

    @Test
    fun `hecho para la sarten no se confunde con Para`() {
        assertEquals(OrdenVoz.Hecho("sg"), i("chef hecho para la sartén grande"))
    }

    @Test
    fun `para el tiempo de un carril es Pausa`() {
        assertEquals(OrdenVoz.Pausa("olla"), i("chef para el tiempo de la olla"))
    }

    @Test
    fun `sin carriles disponibles el carril es null`() {
        assertEquals(OrdenVoz.Hecho(null), InterpreteVoz.interpretar("chef hecho la olla", emptyList()))
    }

    // --- Gramática ampliada (carriles numerados: 1 MESA, 2 SARTÉN, 3 OLLA) ---

    private val numerados =
        listOf(
            CarrilVoz("mesa", "MESA", 1),
            CarrilVoz("sarten", "SARTÉN", 2),
            CarrilVoz("olla", "OLLA", 3),
        )

    private fun n(texto: String) = InterpreteVoz.interpretar(texto, numerados)

    @Test
    fun `cantidad sola es prorrogar`() {
        assertEquals(OrdenVoz.Prorrogar(30, null), n("chef 30 segundos"))
        assertEquals(OrdenVoz.Prorrogar(30, null), n("chef treinta segundos más"))
        assertEquals(OrdenVoz.Prorrogar(45, null), n("chef cuarenta y cinco segundos"))
        assertEquals(OrdenVoz.Prorrogar(60, null), n("chef un minuto"))
        assertEquals(OrdenVoz.Prorrogar(10, null), n("chef 10 seg"))
    }

    @Test
    fun `prorrogar con verbos y cantidad`() {
        assertEquals(OrdenVoz.Prorrogar(60, null), n("chef añade un minuto"))
        assertEquals(OrdenVoz.Prorrogar(60, null), n("chef anade un minuto"))
        assertEquals(OrdenVoz.Prorrogar(120, null), n("chef suma dos minutos"))
        assertEquals(OrdenVoz.Prorrogar(300, null), n("chef pon cinco min"))
        assertEquals(OrdenVoz.Prorrogar(45, null), n("chef más 45 segundos"))
    }

    @Test
    fun `dale con cantidad prorroga y sin cantidad sigue`() {
        assertEquals(OrdenVoz.Prorrogar(30, null), n("chef dale 30 segundos"))
        assertEquals(OrdenVoz.Seguir(null), n("chef dale"))
        assertEquals(OrdenVoz.Seguir(null), n("chef quita la pausa"))
        assertEquals(OrdenVoz.Seguir("mesa"), n("chef quita la pausa del 1"))
    }

    @Test
    fun `minutos medios y tope`() {
        assertEquals(OrdenVoz.Prorrogar(30, null), n("chef medio minuto más"))
        assertEquals(OrdenVoz.Prorrogar(90, "olla"), n("chef un minuto y medio más a la olla"))
        assertEquals(OrdenVoz.Prorrogar(90, null), n("chef minuto y medio más"))
        assertEquals(OrdenVoz.Prorrogar(150, null), n("chef 2 minutos y medio más"))
        assertEquals(OrdenVoz.Prorrogar(1800, null), n("chef 90 minutos más"))
        assertEquals(OrdenVoz.Prorrogar(1800, null), n("chef noventa y nueve minutos más"))
    }

    @Test
    fun `numeros en palabras de cero a noventa y nueve`() {
        assertEquals(OrdenVoz.Prorrogar(15, null), n("chef quince segundos"))
        assertEquals(OrdenVoz.Prorrogar(16, null), n("chef dieciséis segundos"))
        assertEquals(OrdenVoz.Prorrogar(25, null), n("chef veinticinco segundos"))
        assertEquals(OrdenVoz.Prorrogar(21, null), n("chef veintiún segundos"))
        assertEquals(OrdenVoz.Prorrogar(35, null), n("chef treinta y cinco segundos"))
        assertEquals(OrdenVoz.Prorrogar(180, null), n("chef tres minutos más"))
    }

    @Test
    fun `prorrogar con carril por numero`() {
        assertEquals(OrdenVoz.Prorrogar(120, "sarten"), n("chef 2 minutos más al 2"))
        assertEquals(OrdenVoz.Prorrogar(30, "mesa"), n("chef 30 segundos más en el 1"))
        assertEquals(OrdenVoz.Prorrogar(120, "olla"), n("chef dos minutos más al tres"))
    }

    @Test
    fun `por que`() {
        assertEquals(OrdenVoz.PorQue(null), n("chef por qué"))
        assertEquals(OrdenVoz.PorQue("mesa"), n("chef por qué del 1"))
        assertEquals(OrdenVoz.PorQue(null), n("chef porque"))
        assertEquals(OrdenVoz.PorQue(null), n("chef para qué"))
        assertEquals(OrdenVoz.PorQue("olla"), n("chef explícame la olla"))
        assertEquals(OrdenVoz.PorQue(null), n("chef explica"))
    }

    @Test
    fun `que toca en sus variantes`() {
        assertEquals(OrdenVoz.QueToca(null), n("chef qué tengo que hacer ahora"))
        assertEquals(OrdenVoz.QueToca("mesa"), n("chef qué tengo que hacer del 1"))
        assertEquals(OrdenVoz.QueToca(null), n("chef qué hay que hacer"))
        assertEquals(OrdenVoz.QueToca(null), n("chef qué me toca"))
        assertEquals(OrdenVoz.QueToca(null), n("chef y ahora qué"))
        assertEquals(OrdenVoz.QueToca(null), n("chef qué debo hacer"))
        assertEquals(OrdenVoz.QueToca(null), n("chef ahora qué"))
        assertEquals(OrdenVoz.QueToca("olla"), n("chef qué hago con la olla"))
    }

    @Test
    fun `info`() {
        assertEquals(OrdenVoz.Info("sarten"), n("chef dame info del 2"))
        assertEquals(OrdenVoz.Info(null), n("chef info"))
        assertEquals(OrdenVoz.Info(null), n("chef información"))
        assertEquals(OrdenVoz.Info(null), n("chef detalles"))
        assertEquals(OrdenVoz.Info("olla"), n("chef cómo va la olla"))
    }

    @Test
    fun `referencia a carril por numero`() {
        assertEquals(OrdenVoz.Hecho("sarten"), n("chef siguiente del 2"))
        assertEquals(OrdenVoz.Pausa("mesa"), n("chef pausa el 1"))
        assertEquals(OrdenVoz.Seguir("olla"), n("chef sigue el 3"))
        assertEquals(OrdenVoz.Seguir("olla"), n("chef sigue el tres"))
        assertEquals(OrdenVoz.Empezar("sarten"), n("chef empieza el carril 2"))
        assertEquals(OrdenVoz.Hecho("mesa"), n("chef hecho fuego 1"))
        assertEquals(OrdenVoz.CuantoQueda("olla"), n("chef cuánto queda en la 3"))
    }

    @Test
    fun `numero de carril inexistente da null`() {
        assertEquals(OrdenVoz.Pausa(null), n("chef pausa el 9"))
        assertEquals(OrdenVoz.Hecho(null), n("chef hecho del 0"))
    }

    @Test
    fun `para con referencia es pausa y para solo es silenciar`() {
        assertEquals(OrdenVoz.Pausa("mesa"), n("chef para el 1"))
        assertEquals(OrdenVoz.Pausa("olla"), n("chef para la olla"))
        assertEquals(OrdenVoz.Pausa(null), n("chef para el 9"))
        assertEquals(OrdenVoz.Para, n("chef para"))
    }

    @Test
    fun `hecho amplio`() {
        for (frase in listOf("acabado", "terminé", "hecha", "pasa", "avanza", "terminado")) {
            assertEquals(OrdenVoz.Hecho(null), n("chef $frase"), frase)
        }
        assertEquals(OrdenVoz.Hecho("sarten"), n("chef avanza el 2"))
    }

    @Test
    fun `activadores ampliados`() {
        assertEquals(OrdenVoz.Hecho(null), n("jefe hecho"))
        assertEquals(OrdenVoz.Hecho(null), n("chefs hecho"))
        assertEquals(OrdenVoz.Hecho(null), n("sheff hecho"))
        assertEquals(setOf("chef", "jef", "shef", "jefe", "chefs", "sheff"), InterpreteVoz.ACTIVADORES)
    }

    @Test
    fun `prioridades entre reglas`() {
        assertEquals(OrdenVoz.Prorrogar(30, null), n("chef dale 30 segundos"))
        assertEquals(OrdenVoz.PorQue(null), n("chef para qué"))
        assertEquals(OrdenVoz.QueToca(null), n("chef qué hago ahora, listo"))
    }

    @Test
    fun `numero de carril seguido de unidad de tiempo no es carril`() {
        // «al 2» es carril, pero «el 2 minutos» no: es la cantidad.
        assertEquals(OrdenVoz.Prorrogar(120, null), n("chef pon el 2 minutos"))
    }

    // --- Palabra clave y errores del reconocedor ---

    @Test
    fun `chef mal reconocido`() {
        assertEquals(OrdenVoz.Hecho(null), i("chéf hecho"))
        assertEquals(OrdenVoz.Hecho(null), i("jef hecho"))
        assertEquals(OrdenVoz.Hecho(null), i("shef hecho"))
        assertEquals(OrdenVoz.Hecho(null), i("CHEF HECHO"))
    }

    @Test
    fun `solo se analiza lo que sigue a la ultima chef`() {
        assertEquals(OrdenVoz.Pausa(null), i("chef hecho chef pausa"))
        assertNull(i("chef hecho chef"))
    }

    @Test
    fun `numero en palabra y cifra equivalen`() {
        assertEquals(i("chef treinta más"), i("chef 30 más"))
    }

    // --- Negativos ---

    @Test
    fun `sin chef no hay orden`() {
        assertNull(i("hecho"))
        assertNull(i("pausa la olla"))
        assertNull(i("qué toca ahora"))
    }

    @Test
    fun `chef solo no es orden`() {
        assertNull(i("chef"))
        assertNull(i("  Chef.  "))
    }

    @Test
    fun `ruido tras chef no es orden`() {
        assertNull(i("chef cuéntame un chiste"))
        assertNull(i("chef qué tiempo hace mañana"))
        assertNull(i(""))
    }

    @Test
    fun `chef dentro de otra palabra no cuenta`() {
        assertNull(i("el chefcito hecho"))
    }

    // --- Alternativas del reconocedor ---

    @Test
    fun `alternativas devuelve la primera que da orden`() {
        val alternativas = listOf("hecho", "chef cuéntame algo", "chef listo la olla", "chef pausa")
        assertEquals(OrdenVoz.Hecho("olla"), InterpreteVoz.interpretar(alternativas, carriles))
    }

    @Test
    fun `alternativas sin ninguna orden da null`() {
        assertNull(InterpreteVoz.interpretar(listOf("hola", "chef"), carriles))
        assertNull(InterpreteVoz.interpretar(emptyList(), carriles))
    }

    // --- Número de carril dicho de otras formas (ronda 9: «info del dos» respondía del foco) ---

    @Test
    fun `info del dos en todas las formas que da el reconocedor`() {
        listOf(
            "chef info del dos",
            "chef info del 2",
            "chef info dos",
            "chef info 2",
            "chef info de 2",
            "chef info de dos",
            "chef info en el dos",
            "chef info del numero dos",
            "chef info del número 2",
            "chef info de los",
        ).forEach { assertEquals(OrdenVoz.Info("sp"), i(it), it) }
    }

    @Test
    fun `un numero suelto al final no se confunde con una prorroga`() {
        assertEquals(OrdenVoz.Hecho("olla"), i("chef hecho 3"))
        assertEquals(OrdenVoz.Prorrogar(120, null), i("chef dos minutos más"))
        assertEquals(OrdenVoz.Prorrogar(30, "sp"), i("chef 30 segundos más en el 2"))
    }

    @Test
    fun `entre alternativas gana la que nombra carril`() {
        assertEquals(
            OrdenVoz.Info("sp"),
            InterpreteVoz.interpretar(listOf("chef info del", "chef info del dos"), carriles),
        )
        assertEquals(OrdenVoz.Info(null), InterpreteVoz.interpretar(listOf("chef info", "chef nada"), carriles))
    }

    // --- Próximos pasos ---

    @Test
    fun `proximos pasos en general`() {
        assertEquals(OrdenVoz.ProximosPasos(null), i("chef próximos pasos"))
        assertEquals(OrdenVoz.ProximosPasos(null), i("chef cuáles son los siguientes pasos"))
        assertEquals(OrdenVoz.ProximosPasos(null), i("chef qué viene después"))
    }

    @Test
    fun `proximos pasos de un carril`() {
        assertEquals(OrdenVoz.ProximosPasos("olla"), i("chef próximos pasos del tres"))
        assertEquals(OrdenVoz.ProximosPasos("sp"), i("chef qué viene después en el dos"))
        assertEquals(OrdenVoz.ProximosPasos("horno"), i("chef siguientes pasos del horno"))
    }

    @Test
    fun `que viene sigue siendo el siguiente fin`() = assertEquals(OrdenVoz.QueViene, i("chef qué viene"))

    // --- Deshacer ---

    @Test
    fun `deshacer en sus variantes`() {
        listOf(
            "chef deshaz",
            "chef deshacer",
            "chef atrás",
            "chef vuelve atrás",
            "chef vuelve al paso anterior",
            "chef me he equivocado",
            "chef paso anterior",
        ).forEach { assertEquals(OrdenVoz.Deshacer(null), i(it), it) }
    }

    @Test
    fun `deshacer con carril`() {
        assertEquals(OrdenVoz.Deshacer("sp"), i("chef deshaz el dos"))
        assertEquals(OrdenVoz.Deshacer("olla"), i("chef vuelve atrás en la olla"))
        assertEquals(OrdenVoz.Deshacer("sg"), i("chef para atrás del 1"))
    }

    // --- Revisión de la ronda 9 ---

    @Test
    fun `una alternativa con carril no cambia la orden de la primera`() {
        assertEquals(OrdenVoz.Pausa(null), InterpreteVoz.interpretar(listOf("chef pausa", "chef pasa 2"), carriles))
    }

    @Test
    fun `una pregunta sobre el paso anterior no deshace`() {
        assertEquals(OrdenVoz.PorQue(null), i("chef por qué el paso anterior lleva tapa"))
    }

    @Test
    fun `un numero suelto que no es carril no cambia el sentido`() {
        assertEquals(OrdenVoz.Para, i("chef para 30"))
        assertEquals(OrdenVoz.Seguir("sp"), i("chef dale 2"))
    }

    // --- Cantidades e ingredientes del paso (F12) ---

    @Test
    fun `cantidad de un ingrediente en sus variantes`() {
        assertEquals(OrdenVoz.Cantidad("sal"), i("chef cuánto de sal"))
        assertEquals(OrdenVoz.Cantidad("sal"), i("chef, ¿cuánta sal?"))
        assertEquals(OrdenVoz.Cantidad("aceite de oliva"), i("chef cantidad de aceite de oliva"))
        assertEquals(OrdenVoz.Cantidad("cebollas"), i("chef cuántas cebollas"))
        assertEquals(OrdenVoz.Cantidad("mantequilla"), i("chef cuánto de la mantequilla"))
        assertEquals(OrdenVoz.Cantidad("harina"), i("chef cuánta harina lleva"))
        assertEquals(OrdenVoz.Cantidad("sal"), i("chef cuánta sal por favor"))
    }

    @Test
    fun `cantidad sin ingrediente o sobre el tiempo no es cantidad`() {
        assertNull(i("chef cuánto"))
        assertNull(i("chef cuánto de"))
        assertNull(i("chef cuánto tiempo"))
        assertNull(i("chef cuánto tiempo lleva"))
        assertNull(i("chef cantidad"))
    }

    @Test
    fun `cuanto queda y cuanto falta siguen siendo del tiempo`() {
        assertEquals(OrdenVoz.CuantoQueda(null), i("chef cuánto queda"))
        assertEquals(OrdenVoz.CuantoQueda("olla"), i("chef cuánto tiempo falta en la olla"))
    }

    @Test
    fun `cantidad no se confunde con otras ordenes`() {
        assertEquals(OrdenVoz.Cantidad("pasta"), i("chef cuánta pasta"))
        assertEquals(OrdenVoz.Cantidad("agua"), i("chef cuánta agua echo"))
        assertEquals(OrdenVoz.QueToca(null), i("chef qué hago ahora"))
        assertEquals(OrdenVoz.Info(null), i("chef info"))
    }

    @Test
    fun `cantidad quita los numerales del ingrediente`() {
        assertEquals(OrdenVoz.Cantidad("limon"), i("chef cuánto de un limón"))
    }

    @Test
    fun `ingredientes del paso en sus variantes`() {
        assertEquals(OrdenVoz.IngredientesPaso(null), i("chef ingredientes de este paso"))
        assertEquals(OrdenVoz.IngredientesPaso(null), i("chef qué lleva este paso"))
        assertEquals(OrdenVoz.IngredientesPaso(null), i("chef, ¿qué ingredientes lleva?"))
        assertEquals(OrdenVoz.IngredientesPaso(null), i("chef ingredientes"))
    }

    @Test
    fun `ingredientes del paso de un carril`() {
        assertEquals(OrdenVoz.IngredientesPaso("sp"), i("chef ingredientes del paso del dos"))
        assertEquals(OrdenVoz.IngredientesPaso("sp"), i("chef ingredientes de este paso del 2"))
        assertEquals(OrdenVoz.IngredientesPaso("olla"), i("chef qué lleva la olla"))
        assertEquals(OrdenVoz.IngredientesPaso("sp"), i("chef ingredientes de este paso de la sartén pequeña"))
    }

    @Test
    fun `ingredientes del paso gana a paso anterior y a siguiente`() {
        assertEquals(OrdenVoz.IngredientesPaso(null), i("chef ingredientes del paso siguiente"))
        assertEquals(OrdenVoz.IngredientesPaso(null), i("chef ingredientes del paso anterior"))
    }

    @Test
    fun `los proximos pasos y el por que mantienen su prioridad`() {
        assertEquals(OrdenVoz.ProximosPasos(null), i("chef próximos pasos"))
        assertEquals(OrdenVoz.PorQue(null), i("chef por qué lleva este paso sal"))
    }

    @Test
    fun `entre alternativas la cantidad y el carril del paso se conservan`() {
        assertEquals(
            OrdenVoz.IngredientesPaso("sp"),
            InterpreteVoz.interpretar(listOf("chef qué lleva este paso", "chef qué lleva este paso del dos"), carriles),
        )
        assertEquals(
            OrdenVoz.Cantidad("sal"),
            InterpreteVoz.interpretar(listOf("chef cuánta sal", "chef cuánto de sal"), carriles),
        )
    }

    @Test
    fun `cantidad e ingredientes no llevan carril salvo el del paso`() {
        assertNull(OrdenVoz.Cantidad("sal").carril)
        assertEquals("olla", OrdenVoz.IngredientesPaso("olla").carril)
    }
}
