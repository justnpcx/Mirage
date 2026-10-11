@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("io.gitlab.arturbosch.detekt")
    id("androidx.baselineprofile")
}

setupApp()

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline-app.xml")
}

android {
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
    ksp {
        arg("room.incremental", "true")
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    bundle {
        language {
            enableSplit = false
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        aidl = true
        compose = true
    }
    namespace = "com.justnpcx.mirage"
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {

    implementation(fileTree("libs"))

    // --- Shared (Kotlin Multiplatform) core ---
    implementation(project(":core:model"))
    implementation(project(":core:subscription"))

    // --- Jetpack Compose / Material 3 ---
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // --- AndroidX View stack (legacy, kept during incremental migration) ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.multiprocess)
    // Installs baseline profiles (startup performance) when the app ships one.
    implementation(libs.androidx.profileinstaller)

    implementation(libs.material)
    implementation(libs.gson)

    implementation(libs.zxing.lite)
    implementation(libs.editorkit)
    implementation(libs.editorkit.language.base)
    implementation(libs.editorkit.language.json)

    implementation(libs.okhttp)
    implementation(libs.snakeyaml)
    implementation(libs.material.about.library)
    implementation(libs.process.phoenix)
    implementation(libs.kryo)
    implementation(libs.guava)
    implementation(libs.ini4j)

    implementation(libs.fastscroll) {
        exclude(group = "androidx.recyclerview")
        exclude(group = "androidx.appcompat")
    }

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.roomigrant.lib)
    ksp(libs.roomigrant.compiler)

    implementation(libs.kotlinx.serialization.json)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)

    // Generates the startup Baseline Profile on a connected device.
    baselineProfile(project(":baselineprofile"))
}
