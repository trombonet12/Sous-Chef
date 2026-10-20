package app.trombito.souschef.data.seed

import androidx.room.withTransaction
import app.trombito.souschef.data.db.HistorialEntity
import app.trombito.souschef.data.db.MetaEntity
import app.trombito.souschef.data.db.PreferenciaEntity
import app.trombito.souschef.data.db.RecetaEntity
import app.trombito.souschef.data.db.SousChefDatabase
import app.trombito.souschef.data.db.TecnicaEntity
import app.trombito.souschef.data.repo.PasoRealDto
import app.trombito.souschef.receta.RecetaJson
import app.trombito.souschef.receta.leerReceta
import app.trombito.souschef.receta.leerTecnica
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.security.MessageDigest
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
            val legado = if (legadoYaSembrado) null else leerLegado()
            db.withTransaction {
                if (db.metaDao().valor(CLAVE_HASH) != hash) {
                    recargarContenido(recetas, tecnicas)
                    db.metaDao().fijar(MetaEntity(CLAVE_HASH, hash))
                }
                if (legado != null && db.metaDao().valor(CLAVE_LEGADO) == null) {
                    db.historialDao().insertarTodas(legado.historial)
                    db.preferenciaDao().insertarTodas(legado.preferencias)
                    db.metaDao().fijar(MetaEntity(CLAVE_LEGADO, "1"))
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

        private class Legado(
            val historial: List<HistorialEntity>,
            val preferencias: List<PreferenciaEntity>,
        )

        private fun leerLegado(): Legado {
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
            return Legado(historial, preferencias)
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
        }
    }
