// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/CategoryIconHelper.kt
// REASON: Added new predefined categories for income ("Refund", "Credit") and
// updated the "Salary" category's icon. Also added the new icons to the
// helper maps.
// BUG FIX: Replaced the 'Replay' icon with 'Redo' to resolve compilation errors.
// FEATURE - Added the new `getCategoryBackground` function and a corresponding
// map to associate category keys with drawable resources for background images,
// enhancing the visual appeal of the transaction detail screen.
// =================================================================================
package io.pm.finlight

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A helper object to manage category icons and colors, and provide a predefined list of categories.
 */
object CategoryIconHelper {

    private val iconColors = mapOf(
        "green_light" to Color(0xFFA5D6A7),
        "blue_light" to Color(0xFF90CAF9),
        "purple_light" to Color(0xFFCE93D8),
        "orange_light" to Color(0xFFFFCC80),
        "red_light" to Color(0xFFEF9A9A),
        "teal_light" to Color(0xFF80CBC4),
        "pink_light" to Color(0xFFF48FB1),
        "brown_light" to Color(0xFFBCAAA4),
        "cyan_light" to Color(0xFF80DEEA),
        "indigo_light" to Color(0xFF9FA8DA),
        "deep_purple_light" to Color(0xFFB39DDB),
        "yellow_light" to Color(0xFFFFF59D),
        "gray_light" to Color(0xFFE0E0E0),
    )

    // --- NEW: Map category keys to background drawable resources ---
    // NOTE: Replace these placeholders with your actual drawable resource IDs.
    private val categoryBackgrounds = mapOf(
        "receipt_long" to R.drawable.bg_cat_bills,
        "trending_up" to R.drawable.bg_cat_investment,
        "star" to R.drawable.bg_cat_entertainment,
        "restaurant" to R.drawable.bg_cat_food,
        "local_gas_station" to R.drawable.bg_cat_fuel,
        "shopping_cart" to R.drawable.bg_cat_groceries,
        "favorite" to R.drawable.bg_cat_health,
        "business" to R.drawable.bg_cat_investment,
        "shopping_bag" to R.drawable.bg_cat_shopping,
        "swap_horiz" to R.drawable.bg_cat_transfer,
        "travel_explore" to R.drawable.bg_cat_travel,
        "work" to R.drawable.bg_cat_salary,
        "redo" to R.drawable.bg_cat_refund,
        "add_card" to R.drawable.bg_cat_card,
        "more_horiz" to R.drawable.bg_cat_general,
        "default" to R.drawable.bg_cat_general
    )

    @DrawableRes
    fun getCategoryBackground(categoryIconKey: String?): Int {
        return categoryBackgrounds[categoryIconKey] ?: R.drawable.bg_cat_general
    }


    fun getIconBackgroundColor(colorKey: String): Color {
        return iconColors[colorKey] ?: Color.LightGray
    }

    fun getAllIconColors(): Map<String, Color> {
        return iconColors
    }

    fun getNextAvailableColor(usedColorKeys: List<String>): String {
        return iconColors.keys.firstOrNull { it !in usedColorKeys }
            ?: iconColors.keys.firstOrNull()
            ?: "gray_light"
    }

    val predefinedCategories = listOf(
        Category(id = 1, name = "Bills", iconKey = "receipt_long", colorKey = "green_light"),
        Category(id = 2, name = "EMI", iconKey = "trending_up", colorKey = "blue_light"),
        Category(id = 3, name = "Entertainment", iconKey = "star", colorKey = "purple_light"),
        Category(id = 4, name = "Food & Drinks", iconKey = "restaurant", colorKey = "orange_light"),
        Category(id = 5, name = "Fuel", iconKey = "local_gas_station", colorKey = "red_light"),
        Category(id = 6, name = "Groceries", iconKey = "shopping_cart", colorKey = "teal_light"),
        Category(id = 7, name = "Health", iconKey = "favorite", colorKey = "pink_light"),
        Category(id = 8, name = "Investment", iconKey = "business", colorKey = "brown_light"),
        Category(id = 9, name = "Shopping", iconKey = "shopping_bag", colorKey = "cyan_light"),
        Category(id = 10, name = "Transfer", iconKey = "swap_horiz", colorKey = "indigo_light"),
        Category(id = 11, name = "Travel", iconKey = "travel_explore", colorKey = "deep_purple_light"),
        Category(id = 12, name = "Salary", iconKey = "work", colorKey = "yellow_light"),
        Category(id = 13, name = "Other", iconKey = "more_horiz", colorKey = "gray_light"),
        Category(id = 14, name = "Refund", iconKey = "redo", colorKey = "green_light"),
        Category(id = 15, name = "Credit", iconKey = "add_card", colorKey = "blue_light"),
    )

    fun getIcon(iconKey: String): ImageVector {
        return when (iconKey) {
            "receipt_long" -> Icons.AutoMirrored.Filled.ReceiptLong
            "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
            "star" -> Icons.Default.Star
            "restaurant" -> Icons.Default.Restaurant
            "local_gas_station" -> Icons.Default.LocalGasStation
            "shopping_cart" -> Icons.Default.ShoppingCart
            "favorite" -> Icons.Default.Favorite
            "business" -> Icons.Default.Business
            "shopping_bag" -> Icons.Default.ShoppingBag
            "swap_horiz" -> Icons.Default.SwapHoriz
            "travel_explore" -> Icons.Default.TravelExplore
            "account_balance" -> Icons.Default.AccountBalance
            "more_horiz" -> Icons.Default.MoreHoriz
            "card_giftcard" -> Icons.Default.CardGiftcard
            "school" -> Icons.Default.School
            "pets" -> Icons.Default.Pets
            "fastfood" -> Icons.Default.Fastfood
            "directions_car" -> Icons.Default.DirectionsCar
            "work" -> Icons.Default.Work
            "redo" -> Icons.Default.Redo
            "add_card" -> Icons.Default.AddCard
            else -> Icons.Default.Category
        }
    }

    fun getAllIcons(): Map<String, ImageVector> {
        return mapOf(
            "receipt_long" to Icons.AutoMirrored.Filled.ReceiptLong,
            "trending_up" to Icons.AutoMirrored.Filled.TrendingUp,
            "star" to Icons.Default.Star,
            "restaurant" to Icons.Default.Restaurant,
            "local_gas_station" to Icons.Default.LocalGasStation,
            "shopping_cart" to Icons.Default.ShoppingCart,
            "favorite" to Icons.Default.Favorite,
            "business" to Icons.Default.Business,
            "shopping_bag" to Icons.Default.ShoppingBag,
            "swap_horiz" to Icons.Default.SwapHoriz,
            "travel_explore" to Icons.Default.TravelExplore,
            "account_balance" to Icons.Default.AccountBalance,
            "more_horiz" to Icons.Default.MoreHoriz,
            "card_giftcard" to Icons.Default.CardGiftcard,
            "school" to Icons.Default.School,
            "pets" to Icons.Default.Pets,
            "fastfood" to Icons.Default.Fastfood,
            "directions_car" to Icons.Default.DirectionsCar,
            "category" to Icons.Default.Category,
            "work" to Icons.Default.Work,
            "redo" to Icons.Default.Redo,
            "add_card" to Icons.Default.AddCard,
        )
    }
}
