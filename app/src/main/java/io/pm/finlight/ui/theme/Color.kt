package io.pm.finlight.ui.theme

import androidx.compose.ui.graphics.Color

// Default Material colors (can be removed if not used)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// --- Custom colors for the "Aurora" theme ---
val AuroraPrimary = Color(0xFF00E5FF) // Vibrant Teal/Cyan
val AuroraSecondary = Color(0xFFFFBA3F) // Subtle Gold
val AuroraBackground = Color(0xFF121212) // Very dark charcoal
val GlassPanelFill = Color(0x1AFFFFFF) // White with 10% opacity
val GlassPanelBorder = Color(0x33FFFFFF) // White with 20% opacity

// --- UPDATED: Opaque colors for popup surfaces, now at 60% opacity ---
val PopupSurfaceDark = Color(0x99212125) // 60% opaque dark charcoal
val PopupSurfaceLight = Color(0x99FFFFFF) // 60% opaque white

// --- Text colors with better contrast ---
val TextPrimaryDark = Color(0xFFE6E1E5) // Primary text for Dark Theme (Bright off-white)
val TextSecondaryDark = Color(0xFFC4C7C5) // Secondary text for Dark Theme (Brighter gray)

val TextPrimaryLight = Color(0xFF191C1C) // Primary text for Light Theme (Dark charcoal)
val TextSecondaryLight = Color(0xFF444746) // Secondary text for Light Theme (Darker gray)

// --- Surface colors ---
val SurfaceLight = Color(0xE6FFFFFF) // Frosted Glass Fill for Light Theme (White @ 90%)
val SurfaceDark = Color(0x1AFFFFFF) // Frosted Glass Fill for Dark Theme (White @ 10%)

val SurfaceVariantLight = Color(0xFFDAE5E3) // Subtle dividers/disabled for Light Theme
val SurfaceVariantDark = Color(0xFF3F4849) // Subtle dividers/disabled for Dark Theme

// --- NEW: High-contrast colors for transaction amounts ---
val IncomeGreenDark = Color(0xFF66BB6A)
val ExpenseRedDark = Color(0xFFEF5350)
val IncomeGreenLight = Color(0xFF2E7D32)
val ExpenseRedLight = Color(0xFFC62828)
