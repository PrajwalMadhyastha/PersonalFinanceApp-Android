package io.pm.finlight.data.model

import kotlinx.serialization.Serializable

/**
 * Represents the different types of cards that can be displayed on the dashboard.
 */
@Serializable
enum class DashboardCardType {
    HERO_BUDGET,
    QUICK_ACTIONS,
    RECENT_TRANSACTIONS,
    ACCOUNTS_CAROUSEL,
    BUDGET_WATCH,
    SPENDING_CONSISTENCY
}
