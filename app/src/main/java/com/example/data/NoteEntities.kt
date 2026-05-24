package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String,
    val iconName: String
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val categoryId: Int? = null,
    val backgroundColorHex: String = "#FAF9F6", // Default soft cream white
    val audioFilePath: String? = null,
    val audioDurationMs: Long = 0,
    val isPinned: Boolean = false
)

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val configKey: String,
    val configValue: String
)
