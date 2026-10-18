import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Dominio puro compartido por la app y la CLI: sin Android, sin disco, sin entorno (ADR-0001).
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

ktlint {
    version.set(libs.versions.ktlint.get())
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}

// DatosRealesTest lee ../data: sin declararlo, Gradle da el test por al día aunque cambien recetas o técnicas.
tasks.test {
    inputs
        .dir(rootProject.layout.projectDirectory.dir("data"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("datos")
}
