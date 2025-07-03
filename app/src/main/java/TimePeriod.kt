// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/TimePeriod.kt
// REASON: NEW FILE - Defines a simple enum to represent the different time
// periods for reporting, making the new generic report system type-safe.
// =================================================================================
package io.pm.finlight

enum class TimePeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}