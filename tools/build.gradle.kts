import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// CLI `scripts/sc`: validar, publicar, comparar, despensa, migrar, importar-legado (docs/herramientas.md).
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    application
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

application {
    mainClass.set("app.souschef.tools.MainKt")
    applicationName = "tools"
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
    implementation(libs.postgresql)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}
