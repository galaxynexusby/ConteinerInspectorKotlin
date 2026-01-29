package ru.vibe.containerinspector.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: Int = 1,
    val nextcloudUrl: String? = null,
    val nextcloudUser: String? = null,
    val nextcloudPass: String? = null
)
