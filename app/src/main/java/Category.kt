package io.pm.finlight

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "categories",
    // Ensure that no two categories can have the same name.
    indices = [Index(value = ["name"], unique = true)],
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "name")
    val name: String,
    // A key to identify the Material Icon for the category
    @ColumnInfo(name = "iconKey")
    val iconKey: String = "category", // Default icon
    // --- NEW: A key to identify the icon's background color ---
    @ColumnInfo(name = "colorKey")
    val colorKey: String = "gray" // Default color
)
