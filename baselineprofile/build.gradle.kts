plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.justnpcx.mirage.baselineprofile"
    compileSdk = 35

    defaultConfig {
        // Baseline profile generation requires API 28+ (and a rooted/emulator device).
        minSdk = 28
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.3.4")
}

androidComponents {
    beforeVariants(selector().all()) {
        // Only build the benchmark variant of this test module.
        it.enable = it.buildType == "benchmark"
    }
}
