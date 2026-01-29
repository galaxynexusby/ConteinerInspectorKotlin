package ru.vibe.containerinspector.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operators")
data class Operator(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val shift: Int,
    val password: String
)
