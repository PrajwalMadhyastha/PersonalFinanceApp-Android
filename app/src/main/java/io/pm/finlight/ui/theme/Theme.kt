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

// --- UPDATED: Dark theme color scheme using our new Aurora colors ---
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF), // Vibrant Teal/Cyan
    onPrimary = Color(0xFF00363A), // Dark color for text/icons on primary
    secondary = Color(0xFFFFBA3F), // Muted Gold
    onSecondary = Color(0xFF452B00), // Dark color for text on secondary
    background = Color(0xFF121212), // Very dark charcoal
    onBackground = Color(0xFFE6E1E5), // Off-white for text on background
    surface = Color(0x1AFFFFFF), // Frosted Glass Fill (White @ 10%)
    onSurface = Color(0xFFE6E1E5), // Off-white for text on glass panels
    surfaceVariant = Color(0xFF3F4849), // For subtle dividers or disabled components
    onSurfaceVariant = Color(0xFFBFC8C9), // For secondary text
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

// Light theme remains unchanged for now
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006A60), // Professional Deep Green
    onPrimary = Color.White,
    secondary = Color(0xFF815600), // Rich Gold/Amber
    onSecondary = Color.White,
    background = Color(0xFFF8F9FA), // A very light off-white
    onBackground = Color(0xFF191C1C), // Dark charcoal for text on background
    surface = Color(0xE6FFFFFF), // Frosted Glass Fill (White @ 90%)
    onSurface = Color(0xFF191C1C), // Dark charcoal for text on glass panels
    surfaceVariant = Color(0xFFDAE5E3), // For subtle dividers or disabled components
    onSurfaceVariant = Color(0xFF3F4947), // For secondary text
    error = Color(0xFFB00020),
    onError = Color.White
)

@Composable
fun PersonalFinanceAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // --- UPDATED: Set dynamicColor to false to enforce our custom theme ---
    dynamicColor: Boolean = false,
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
