package com.justnpcx.mirage.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the Baseline Profile: app startup, then the drawer and the node picker.
 *
 * Regeneration is documented in `.github/workflows/baselineprofile.yml`. Run it against a device or
 * emulator with API 28+:
 * ```
 * ./gradlew :app:generateOssBaselineProfile
 * ```
 * It writes `app/src/ossRelease/generated/baselineProfiles/baseline-prof.txt`, which is picked up
 * by `androidx.profileinstaller` at install time.
 *
 * Every step here is best-effort on purpose: a missing label or an empty list must not fail the
 * test, because a failed test means no profile is produced at all. On a freshly installed app the
 * node lists have no entries, so that part mostly profiles the RecyclerView machinery — on a device
 * that already has nodes it covers the node rows as well.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupAndNodeList() = rule.collect(packageName = "com.justnpcx.mirage") {
        pressHome()
        startActivityAndWait()

        val width = device.displayWidth
        val height = device.displayHeight

        // Home screen settles.
        device.waitForIdle(SETTLE_MS)

        // The drawer has no hamburger button: MainActivity.onKeyDown opens it on DPAD_LEFT.
        // Then jump to the node picker (Configuration: a tab per group, each listing its nodes).
        device.pressDPadLeft()
        clickAnyOf(device, timeoutMs = SETTLE_MS, "Subscriptions & Groups", "订阅与分组")
        device.waitForIdle(SETTLE_MS)

        // Scroll the node list both ways so the LazyColumn compose + scroll path is profiled.
        repeat(3) {
            device.swipe(width / 2, height * 3 / 4, width / 2, height / 4, SWIPE_STEPS)
            device.waitForIdle(SETTLE_MS)
        }
        repeat(2) {
            device.swipe(width / 2, height / 4, width / 2, height * 3 / 4, SWIPE_STEPS)
            device.waitForIdle(SETTLE_MS)
        }

        // Back to Home, so the return path is covered too.
        device.pressDPadLeft()
        clickAnyOf(device, timeoutMs = SETTLE_MS, "Home", "主页")
        device.waitForIdle(SETTLE_MS)
    }

    private companion object {
        const val SETTLE_MS = 2_000L
        const val SWIPE_STEPS = 20

        /**
         * Clicks the first of [labels] that appears, waiting up to [timeoutMs] for one to show up.
         * Returns false instead of throwing when none do — see the class comment.
         */
        fun clickAnyOf(device: UiDevice, timeoutMs: Long, vararg labels: String): Boolean {
            val deadline = SystemClock.uptimeMillis() + timeoutMs
            while (SystemClock.uptimeMillis() < deadline) {
                for (label in labels) {
                    val node = device.findObject(By.text(label))
                    if (node != null) {
                        node.click()
                        return true
                    }
                }
                SystemClock.sleep(200)
            }
            return false
        }
    }
}
