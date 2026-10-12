plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

apply(from = "../repositories.gradle.kts")

// NOTE: this file is the single source of truth for the Gradle plugin versions (AGP, Kotlin,
// KSP, detekt). The app/core modules apply them version-less via id(...), so the version catalog's
// [plugins] aliases are NOT what drives the build.
dependencies {
    // Gradle Plugins
    implementation("com.android.tools.build:gradle:8.8.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    // Compose compiler Gradle plugin (Kotlin 2.0+ ships the Compose compiler as a Kotlin plugin)
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.0.21")
    // kotlinx.serialization compiler plugin
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.0.21")
    // Static analysis
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.7")
    // Baseline profile generation
    implementation("androidx.baselineprofile:androidx.baselineprofile.gradle.plugin:1.5.0")
}
