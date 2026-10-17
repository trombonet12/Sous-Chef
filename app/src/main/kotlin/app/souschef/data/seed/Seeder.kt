package app.souschef.data.seed

import androidx.room.withTransaction
import app.souschef.data.db.DespensaItemEntity
import app.souschef.data.db.HistorialEntity
import app.souschef.data.db.MetaEntity
import app.souschef.data.db.PreferenciaEntity
import app.souschef.data.db.RecetaEntity
import app.souschef.data.db.SousChefDatabase
import app.souschef.data.db.TecnicaEntity
import app.souschef.data.repo.PasoRealDto
import app.souschef.receta.RecetaJson
import app.souschef.receta.leerReceta
import app.souschef.receta.leerTecnica
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.security.MessageDigest
import java.text.Normalizer
import javax.inject.Inject

@Serializable
private data class HistorialLegado(
    val slug: String,
    val titulo: String = "",
    val fecha: String,
    val base: String? = null,
    val tecnica: List<String> = emptyList(),
    val nota: String? = null,
    val minutosReales: Int? = null,
    val pasosReales: List<PasoRealDto>? = null,
)

@Serializable
private data class PreferenciaLegado(
    val slug: String,
    val favorito: Boolean = false,
    val raciones: Int? = null,
)

/**
 * Siembra la base local desde `data/` (assets). Recetas y técnicas se recargan si cambia el hash del conjunto;
 * historial y preferencias solo la primera vez (a partir de ahí manda el usuario).
 *
 * La despensa se siembra entera la primera vez. Después, si cambia el hash de `despensa-semilla.md`
 * ([CLAVE_HASH_DESPENSA]; en instalaciones previas sin hash cuenta como cambiado), se añaden solo los artículos
 * cuyo nombre normalizado (minúsculas, sin tildes) no exista ya: estado HAY (VETADO los vetos) y categoría de la
 * semilla. Nunca se borra ni se modifica nada existente. Un artículo que el usuario borró a mano reaparecerá
 * cuando la semilla cambie; es un comportamiento aceptado.
 */
class Seeder
    @Inject
    constructor(
        private val db: SousChefDatabase,
        private val fuente: FuenteSemilla,
    ) {
        suspend fun sembrar() {
            val recetas = fuente.listar("recetas").filter { it.endsWith(".json") }.map { it to fuente.leer(it) }
            val tecnicas = fuente.listar("tecnicas").filter { it.endsWith(".json") }.map { it to fuente.leer(it) }
            val hash = hashConjunto(recetas + tecnicas)
            val legadoYaSembrado = db.metaDao().valor(CLAVE_LEGADO) != null
            val semilla = fuente.leer("despensa-semilla.md")
            val hashDespensa = sha256(semilla)
            val legado = if (legadoYaSembrado) null else leerLegado(semilla)
            db.withTransaction {
                if (db.metaDao().valor(CLAVE_HASH) != hash) {
                    recargarContenido(recetas, tecnicas)
                    db.metaDao().fijar(MetaEntity(CLAVE_HASH, hash))
                }
                if (legado != null && db.metaDao().valor(CLAVE_LEGADO) == null) {
                    db.historialDao().insertarTodas(legado.historial)
                    db.preferenciaDao().insertarTodas(legado.preferencias)
                    db.despensaDao().insertarTodos(legado.despensa)
                    db.metaDao().fijar(MetaEntity(CLAVE_LEGADO, "1"))
                    db.metaDao().fijar(MetaEntity(CLAVE_HASH_DESPENSA, hashDespensa))
                } else if (db.metaDao().valor(CLAVE_HASH_DESPENSA) != hashDespensa) {
                    anadirNuevosDeLaSemilla(semilla)
                    db.metaDao().fijar(MetaEntity(CLAVE_HASH_DESPENSA, hashDespensa))
                }
            }
        }

        private suspend fun recargarContenido(
            recetas: List<Pair<String, String>>,
            tecnicas: List<Pair<String, String>>,
        ) {
            db.recetaDao().borrarTodas()
            db.recetaDao().insertar(recetas.map { (_, doc) -> RecetaEntity(leerReceta(doc).slug, doc, sha256(doc)) })
            db.tecnicaDao().borrarTodas()
            db.tecnicaDao().insertar(
                tecnicas.map { (_, doc) ->
                    TecnicaEntity(leerTecnica(doc).slug, doc, sha256(doc))
                },
            )
        }

        private suspend fun anadirNuevosDeLaSemilla(semilla: String) {
            val existentes =
                db
                    .despensaDao()
                    .observar()
                    .first()
                    .map { normalizar(it.nombre) }
                    .toMutableSet()
            val nuevos =
                SemillaDespensa
                    .parsear(semilla)
                    .filter { existentes.add(normalizar(it.nombre)) }
                    .map { it.aEntidad() }
            if (nuevos.isNotEmpty()) db.despensaDao().insertarTodos(nuevos)
        }

        private fun ItemSemilla.aEntidad() =
            DespensaItemEntity(nombre = nombre, categoria = categoria, estado = estado.name, nota = null)

        private fun normalizar(nombre: String): String =
            Normalizer
                .normalize(nombre.trim().lowercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")

        private class Legado(
            val historial: List<HistorialEntity>,
            val preferencias: List<PreferenciaEntity>,
            val despensa: List<DespensaItemEntity>,
        )

        private fun leerLegado(semilla: String): Legado {
            val historial =
                RecetaJson
                    .decodeFromString(
                        MapSerializer(String.serializer(), HistorialLegado.serializer()),
                        fuente.leer("legado/historial.json"),
                    ).map { (id, h) ->
                        HistorialEntity(
                            id = id,
                            slug = h.slug,
                            titulo = h.titulo,
                            fecha = h.fecha,
                            base = h.base,
                            tecnicaJson = RecetaJson.encodeToString(ListSerializer(String.serializer()), h.tecnica),
                            raciones = null,
                            minutosReales = h.minutosReales,
                            nota = h.nota,
                            pasosRealesJson =
                                h.pasosReales?.takeIf { it.isNotEmpty() }?.let {
                                    RecetaJson.encodeToString(ListSerializer(PasoRealDto.serializer()), it)
                                },
                        )
                    }
            val preferencias =
                RecetaJson
                    .decodeFromString(
                        MapSerializer(String.serializer(), PreferenciaLegado.serializer()),
                        fuente.leer("legado/preferencias.json"),
                    ).values
                    .map { PreferenciaEntity(it.slug, it.favorito, it.raciones) }
            val despensa =
                SemillaDespensa.parsear(semilla).map { it.aEntidad() }
            return Legado(historial, preferencias, despensa)
        }

        private fun hashConjunto(archivos: List<Pair<String, String>>): String =
            sha256(archivos.sortedBy { it.first }.joinToString("\n") { (ruta, doc) -> "$ruta\u0000$doc" })

        private fun sha256(texto: String): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(texto.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

        companion object {
            const val CLAVE_HASH = "datos_hash"
            const val CLAVE_LEGADO = "legado_sembrado"
            const val CLAVE_HASH_DESPENSA = "despensa_hash"
        }
    }
