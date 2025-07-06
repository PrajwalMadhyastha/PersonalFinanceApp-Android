package io.pm.finlight

/**
 * Represents the different types of cards that can be displayed on the dashboard.
 * Used for managing customizable dashboard layouts.
 */
enum class DashboardCardType {
    // --- UPDATED: Replaced OVERALL_BUDGET and QUICK_STATS with a single HERO_BUDGET ---
    HERO_BUDGET,
    QUICK_ACTIONS,
    NET_WORTH,
    RECENT_ACTIVITY,
    ACCOUNTS_CAROUSEL,
    BUDGET_WATCH
}
