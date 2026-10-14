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

// Supabase en BuildConfig. Archivo o clave ausente => "" (CI compila igual). Nunca se loguean valores.
fun loadProps(file: File): Properties {
    val props = Properties()
    if (file.isFile) file.inputStream().use { props.load(it) }
    return props
}

fun Properties.asBuildConfig(key: String): String {
    val raw = getProperty(key).orEmpty().trim()
    return "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

fun requireHttps(
    props: Properties,
    variant: String,
) {
    val url = props.getProperty("supabase.url").orEmpty().trim()
    require(url.isEmpty() || url.startsWith("https://")) {
        "supabase.url remota de $variant debe empezar por https://"
    }
}

val remoteProps = loadProps(rootProject.file("local.properties"))
val localProps = loadProps(rootProject.file("supabase-local.properties"))

android {
    namespace = "app.souschef"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.souschef"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
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
        // debug: Supabase remoto (local.properties). localDebug: stack local de `npx supabase` (supabase-local.properties).
        debug {
            applicationIdSuffix = ".dev"
            requireHttps(remoteProps, "debug")
            buildConfigField("String", "SUPABASE_URL", remoteProps.asBuildConfig("supabase.url"))
            buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", remoteProps.asBuildConfig("supabase.publishableKey"))
        }
        create("localDebug") {
            initWith(getByName("debug"))
            matchingFallbacks += "debug"
            applicationIdSuffix = ".local"
            buildConfigField("String", "SUPABASE_URL", localProps.asBuildConfig("supabase.url"))
            buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", localProps.asBuildConfig("supabase.publishableKey"))
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            requireHttps(remoteProps, "release")
            buildConfigField("String", "SUPABASE_URL", remoteProps.asBuildConfig("supabase.url"))
            buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", remoteProps.asBuildConfig("supabase.publishableKey"))
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

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Datos del repo (recetas, técnicas, legado, despensa) empaquetados como assets para el seed local.
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
                include("recetas/**", "tecnicas/**", "legado/**", "despensa-semilla.md")
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

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.tink.android)

    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
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
