package io.pm.finlight.ui.theme

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

// --- UPDATED: Dark theme color scheme using our new custom colors ---
private val DarkColorScheme =
    darkColorScheme(
        primary = FinanceGreenLight,
        secondary = GoldAccentLight,
        tertiary = Charcoal,
        background = Color(0xFF121212),
        surface = DarkSurface,
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White,
        onError = Color.Red,
    )

// --- UPDATED: Light theme color scheme using our new custom colors ---
private val LightColorScheme =
    lightColorScheme(
        primary = FinanceGreen,
        secondary = GoldAccent,
        tertiary = Charcoal,
        background = OffWhite,
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF1C1B1F),
        onSurface = Color(0xFF1C1B1F),
        error = Color(0xFFB00020),
    )

@Composable
fun PersonalFinanceAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Set to false to always use your custom theme
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
