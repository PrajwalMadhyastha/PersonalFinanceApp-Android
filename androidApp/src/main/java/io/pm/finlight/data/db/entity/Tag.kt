package io.pm.finlight

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents a user-defined Tag (e.g., "Work Trip", "Vacation 2025", "Tax-Deductible").
 * Tags provide a flexible way to organize transactions outside of the rigid category system.
 */
@Serializable
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)