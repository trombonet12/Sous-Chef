import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Firma release solo si existen las variables SOUSCHEF_* (scripts/android-release.mjs las carga de ~/.souschef).
val releaseStoreFile: String? = System.getenv("SOUSCHEF_KEYSTORE_FILE")
val releaseStorePassword: String? = System.getenv("SOUSCHEF_KEYSTORE_PASSWORD")
val releaseKeyAlias: String? = System.getenv("SOUSCHEF_KEY_ALIAS")
val releaseKeyPassword: String? = System.getenv("SOUSCHEF_KEY_PASSWORD")
val hasReleaseSigning =
    listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }

// Versión de la app: major/minor en version.properties; patch y versionCode salen de git (ver version.properties).
val versionProps =
    Properties().apply {
        val file = rootProject.file("version.properties")
        check(file.isFile) { "Falta version.properties en la raíz del repo (versionMajor, versionMinor)" }
        providers
            .fileContents(rootProject.layout.projectDirectory.file("version.properties"))
            .asText
            .get()
            .reader()
            .use { load(it) }
    }

fun versionProp(key: String): String =
    versionProps.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
        ?: error("version.properties: falta la clave '$key'")

/** Salida de git (compatible con la configuration cache) o null si git no está o el comando falla. */
fun gitOutput(vararg args: String): String? =
    try {
        val result =
            providers.exec {
                workingDir = rootProject.projectDir
                commandLine(listOf("git") + args)
                isIgnoreExitValue = true
            }
        if (result.result.get().exitValue == 0) {
            result.standardOutput.asText
                .get()
                .trim()
                .takeIf { it.isNotEmpty() }
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }

val gitPatch: Int? =
    gitOutput("log", "-1", "--format=%H", "--", "version.properties")
        ?.let { gitOutput("rev-list", "--count", "$it..HEAD") }
        ?.toIntOrNull()
val gitCount: Int? = gitOutput("rev-list", "--count", "HEAD")?.toIntOrNull()
if (gitPatch == null || gitCount == null) {
    logger.warn("No se pudo leer git: versión con patch 0 y versionCode 1")
}
val appVersionName = "${versionProp("versionMajor")}.${versionProp("versionMinor")}.${gitPatch ?: 0}"
val appVersionCode = gitCount ?: 1

tasks.register("imprimirVersion") {
    group = "help"
    description = "Imprime 'versionName versionCode' en una línea."
    val linea = "$appVersionName $appVersionCode"
    doLast { println(linea) }
}

android {
    namespace = "app.trombito.souschef"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.trombito.souschef"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    // Los schemas de Room como assets de test: MigrationTestHelper los lee en los tests JVM (Robolectric).
    sourceSets
        .getByName("test")
        .assets.directories
        .add("$projectDir/schemas")

    buildFeatures {
        compose = true
    }
}

// Datos del repo (recetas, técnicas, legado) y novedades por versión empaquetados como assets.
abstract class CopiarDatos : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val origen: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val novedades: DirectoryProperty

    @get:OutputDirectory
    abstract val destino: DirectoryProperty

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun copiar() {
        fs.sync {
            from(origen) {
                include("recetas/**", "tecnicas/**", "legado/**")
            }
            from(novedades) {
                include("*.json")
                into("novedades")
            }
            into(destino)
        }
    }
}

val copiarDatos =
    tasks.register<CopiarDatos>("copiarDatos") {
        origen.set(rootProject.layout.projectDirectory.dir("data"))
        novedades.set(rootProject.layout.projectDirectory.dir("distribution/novedades"))
        destino.set(layout.buildDirectory.dir("generated/souschef-datos"))
    }

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copiarDatos, CopiarDatos::destino)
    }
}

// Comprueba las novedades tras actualizar: formato de los JSON y que whatsnew (Play) coincida con ellas.
abstract class ComprobarNovedades : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val novedades: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val whatsnew: RegularFileProperty

    @get:Input
    abstract val major: Property<Int>

    @get:Input
    abstract val minor: Property<Int>

    // Mismas reglas que FuenteNovedadesAssets.parsear (la app ignora en silencio lo que las incumple).
    @Suppress("UNCHECKED_CAST")
    private fun textosDe(f: File): List<String> {
        val json = groovy.json.JsonSlurper().parseText(f.readText(Charsets.UTF_8)) as Map<String, Any?>
        val version = f.name.removeSuffix(".json")
        check(json["version"] == version) { "${f.name}: \"version\" debe ser \"$version\" (el nombre del archivo)" }
        check((json["titulo"] as? String).orEmpty().isNotBlank()) { "${f.name}: falta \"titulo\"" }
        val lineas = json["lineas"] as? List<Map<String, Any?>> ?: error("${f.name}: falta \"lineas\"")
        val textos = lineas.map { it["texto"] as? String ?: error("${f.name}: línea sin texto") }
        check(textos.all { it.isNotBlank() }) { "${f.name}: hay una línea vacía" }
        check(textos.size in 3..5) { "${f.name}: debe tener entre 3 y 5 líneas (tiene ${textos.size})" }
        textos.forEach { t -> check(t.length <= 90) { "${f.name}: línea de más de 90 caracteres: $t" } }
        return textos
    }

    @TaskAction
    fun comprobar() {
        val nombre = Regex("""(\d+)\.(\d+)\.json""")
        val jsons =
            novedades
                .get()
                .asFile
                .listFiles()
                .orEmpty()
                .mapNotNull { f ->
                    nombre.matchEntire(f.name)?.let { Triple(it.groupValues[1].toInt(), it.groupValues[2].toInt(), f) }
                }.sortedWith(compareBy({ it.first }, { it.second }))
        check(jsons.isNotEmpty()) { "No hay JSON de novedades en ${novedades.get().asFile}" }

        // Desde la 1.6 cada versión publicada exige su JSON (antes no existía la función).
        val actual = "${major.get()}.${minor.get()}"
        if (major.get() > 1 || (major.get() == 1 && minor.get() >= 6)) {
            check(jsons.any { it.first == major.get() && it.second == minor.get() }) {
                "Falta distribution/novedades/$actual.json (obligatorio desde la 1.6)"
            }
        }

        jsons.forEach { (_, _, f) -> textosDe(f) }

        // whatsnew se publica en Play junto a la versión que viene: se compara con el JSON de versión más alta
        // que exista (con versionMinor=5 ya es la 1.6), no con el major.minor actual.
        val ultimo = jsons.last().third
        val esperado = textosDe(ultimo).joinToString("\n") { "• $it" }
        val crudo = whatsnew.get().asFile.readText(Charsets.UTF_8)
        val real = crudo.replace("\r\n", "\n").removeSuffix("\n")
        val msg = "distribution/whatsnew/whatsnew-es-ES no coincide con ${ultimo.name} (líneas con prefijo «• »)"
        check(real == esperado) { msg }
        check(real.length <= 500) { "whatsnew-es-ES pasa de 500 caracteres (${real.length})" }
    }
}

val comprobarNovedades =
    tasks.register<ComprobarNovedades>("comprobarNovedades") {
        group = "verification"
        description = "Valida distribution/novedades/*.json y que whatsnew-es-ES coincida con el más reciente."
        novedades.set(rootProject.layout.projectDirectory.dir("distribution/novedades"))
        whatsnew.set(rootProject.layout.projectDirectory.file("distribution/whatsnew/whatsnew-es-ES"))
        major.set(versionProp("versionMajor").toInt())
        minor.set(versionProp("versionMinor").toInt())
    }

tasks.named("preBuild") { dependsOn(comprobarNovedades) }

// Robolectric (corre con el targetSdk 36; se compila contra 37) necesita abrir internals del JDK 21.
tasks.withType<Test>().configureEach {
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.base/java.lang=ALL-UNNAMED")
}

room {
    schemaDirectory("$projectDir/schemas")
}

ktlint {
    version.set(libs.versions.ktlint.get())
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    implementation(project(":receta"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    implementation(libs.play.app.update.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
}

// Un release sin firma no sirve para instalar ni actualizar: falla al ejecutar (no al configurar, para no romper debug).
tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    doFirst {
        check(
            hasReleaseSigning,
        ) { "Release sin firma: faltan las variables SOUSCHEF_* (usa node scripts/android-release.mjs)" }
    }
}

// La caché de build restauraba un componente Hilt viejo tras cambiar el constructor de un ViewModel (NoSuchMethodError
// en ejecución, ronda 14). El componente se regenera siempre.
tasks.matching { it.name.startsWith("hiltJavaCompile") }.configureEach { outputs.cacheIf { false } }
