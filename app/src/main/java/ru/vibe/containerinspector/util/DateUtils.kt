package ru.vibe.containerinspector.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
