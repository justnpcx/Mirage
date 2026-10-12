plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
    // Required on this module too, not just on :app. Without it the module never exposes an
    // outgoing variant with usage=baselineProfile, and :app's `ossReleaseBaselineProfile`
    // configuration fails to resolve — the task graph then contains no instrumentation test at
    // all and :app:generateOssBaselineProfile "succeeds" while producing nothing.
    id("androidx.baselineprofile")
}

android {
    namespace = "com.justnpcx.mirage.baselineprofile"
    compileSdk = 35

    defaultConfig {
        // Baseline profile generation requires API 28+.
        minSdk = 28
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // The app declares a `vendor` flavor dimension (oss/fdroid/play/preview). Without a
        // strategy AGP cannot match this module's variants to the app's.
        missingDimensionStrategy("vendor", "oss")
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.5.0")
}
