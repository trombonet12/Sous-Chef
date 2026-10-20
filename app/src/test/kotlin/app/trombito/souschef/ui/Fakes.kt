package app.trombito.souschef.ui

import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.HistorialRepo
import app.trombito.souschef.domain.Preferencia
import app.trombito.souschef.domain.PreferenciasRepo
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.TecnicasRepo
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.Tags
import app.trombito.souschef.receta.Tecnica
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

fun receta(
    slug: String,
    titulo: String = slug,
    base: String? = null,
    tecnicas: List<String> = emptyList(),
    fuegos: Int = 0,
) = Receta(
    slug = slug,
    titulo = titulo,
    totalMin = 30.0,
    tags = Tags(base = base, tecnica = tecnicas),
    carriles =
        List(fuegos) { Carril("f$it", tipo = "fuego") } + Carril("h", tipo = "horno") + Carril("m", tipo = "mesa"),
    pasos = listOf(Paso("p1", "Picar cebolla", "m", 0.0, 5.0)),
)

class FakeRecetasRepo(
    inicial: List<Receta> = emptyList(),
) : RecetasRepo {
    val datos = MutableStateFlow(inicial)

    override fun observarTodas(): Flow<List<Receta>> = datos

    override fun observar(slug: String): Flow<Receta?> = datos.map { l -> l.firstOrNull { it.slug == slug } }

    override suspend fun obtener(slug: String): Receta? = datos.value.firstOrNull { it.slug == slug }
}

class FakeTecnicasRepo(
    inicial: List<Tecnica> = emptyList(),
) : TecnicasRepo {
    val datos = MutableStateFlow(inicial)

    override fun observarTodas(): Flow<List<Tecnica>> = datos

    override fun observar(slug: String): Flow<Tecnica?> = datos.map { l -> l.firstOrNull { it.slug == slug } }

    override suspend fun obtener(slug: String): Tecnica? = datos.value.firstOrNull { it.slug == slug }
}

class FakeHistorialRepo(
    inicial: List<EntradaHistorial> = emptyList(),
) : HistorialRepo {
    val datos = MutableStateFlow(inicial)
    val ultimas = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun observarTodo(): Flow<List<EntradaHistorial>> = datos

    override fun observarUltimaFechaPorSlug(): Flow<Map<String, String>> = ultimas

    override suspend fun agregar(entrada: EntradaHistorial) {
        datos.value = listOf(entrada) + datos.value
    }

    override suspend fun borrar(id: String) {
        datos.value = datos.value.filterNot { it.id == id }
    }
}

class FakePreferenciasRepo : PreferenciasRepo {
    val datos = MutableStateFlow<Map<String, Preferencia>>(emptyMap())
    val racionesGuardadas = mutableListOf<Pair<String, Int>>()

    override fun observarTodas(): Flow<Map<String, Preferencia>> = datos

    override fun observar(slug: String): Flow<Preferencia?> = datos.map { it[slug] }

    override suspend fun fijarFavorito(
        slug: String,
        favorito: Boolean,
    ) {
        val actual = datos.value[slug] ?: Preferencia(slug)
        datos.value = datos.value + (slug to actual.copy(favorito = favorito))
    }

    override suspend fun fijarRaciones(
        slug: String,
        raciones: Int,
    ) {
        racionesGuardadas += slug to raciones
        val actual = datos.value[slug] ?: Preferencia(slug)
        datos.value = datos.value + (slug to actual.copy(raciones = raciones))
    }
}
