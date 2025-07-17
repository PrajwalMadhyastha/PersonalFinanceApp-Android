package io.pm.finlight

/**
 * Represents the different types of cards that can be displayed on the dashboard.
 * Used for managing customizable dashboard layouts.
 */
enum class DashboardCardType {
    HERO_BUDGET,
    QUICK_ACTIONS,
    NET_WORTH,
    RECENT_ACTIVITY,
    ACCOUNTS_CAROUSEL,
    BUDGET_WATCH,
    // --- NEW: Add the new card type for the consistency calendar ---
    SPENDING_CONSISTENCY
}
