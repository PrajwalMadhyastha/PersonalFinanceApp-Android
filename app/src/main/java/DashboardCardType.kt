package io.pm.finlight

/**
 * Represents the different types of cards that can be displayed on the dashboard.
 * Used for managing customizable dashboard layouts.
 */
enum class DashboardCardType {
    OVERALL_BUDGET,
    // --- UPDATED: Replaced MONTHLY_STATS with QUICK_STATS ---
    QUICK_STATS,
    QUICK_ACTIONS,
    NET_WORTH,
    RECENT_ACTIVITY,
    // --- UPDATED: Replaced ACCOUNTS_SUMMARY with ACCOUNTS_CAROUSEL ---
    ACCOUNTS_CAROUSEL,
    BUDGET_WATCH
}
