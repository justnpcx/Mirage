package com.justnpcx.mirage.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a Baseline Profile for the app's startup critical path.
 *
 * Run on a connected device/emulator:
 * ```
 * ./gradlew :app:generateOssBenchmarkBaselineProfile
 * ```
 * The profile is written to `app/src/ossBenchmark/generated/baselineProfiles/` and picked up by the
 * `androidx.profileinstaller` dependency at install time.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(packageName = "com.justnpcx.mirage") {
        pressHome()
        startActivityAndWait()
        // Let the Compose Home screen settle, then open the drawer once so its layout is profiled.
        device.wait(Until.hasObject(By.text("Mirage")), 5_000)
    }
}
