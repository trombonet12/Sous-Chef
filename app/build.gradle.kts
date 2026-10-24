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
    compileSdk = 36

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

// Datos del repo (recetas, técnicas, legado) empaquetados como assets para el seed local.
abstract class CopiarDatos : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val origen: DirectoryProperty

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
            into(destino)
        }
    }
}

val copiarDatos =
    tasks.register<CopiarDatos>("copiarDatos") {
        origen.set(rootProject.layout.projectDirectory.dir("data"))
        destino.set(layout.buildDirectory.dir("generated/souschef-datos"))
    }

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copiarDatos, CopiarDatos::destino)
    }
}

// Robolectric (SDK 36) necesita abrir internals del JDK 21.
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
