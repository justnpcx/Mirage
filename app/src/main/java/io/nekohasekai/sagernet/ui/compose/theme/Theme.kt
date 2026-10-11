package io.nekohasekai.sagernet.ui.compose.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Mirage brand seed. Used when dynamic color is unavailable (Android < 12) or disabled.
 */
private val MiragePrimary = Color(0xFF3D5AFE)
private val MirageSecondary = Color(0xFF536DFE)
private val MirageTertiary = Color(0xFF00BFA5)

private val LightColors = lightColorScheme(
    primary = MiragePrimary,
    secondary = MirageSecondary,
    tertiary = MirageTertiary,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8C9EFF),
    secondary = Color(0xFFA5B4FF),
    tertiary = Color(0xFF64FFDA),
)

/**
 * Material 3 theme for Mirage.
 *
 * Uses Material You dynamic color on Android 12+ and falls back to the Mirage brand scheme.
 */
@Composable
fun MirageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
