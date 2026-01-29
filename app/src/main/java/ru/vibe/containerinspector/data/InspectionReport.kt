package ru.vibe.containerinspector.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReportStatus {
    READY_TO_INSPECT,    // Scanned, waiting to start
    IN_PROGRESS,         // Inspection started
    UNFINISHED,          // Inspection deferred
    COMPLETED,           // All photos taken, waiting for upload
    SENT                 // Successfully uploaded to Nextcloud
}

@Entity(tableName = "inspection_reports")
data class InspectionReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val containerNumber: String,
    val containerType: String = "",
    val status: ReportStatus = ReportStatus.READY_TO_INSPECT,
    val currentStep: Int = 0, // 0 to 6 (for 1 to 7 photos)
    val operatorName: String,
    val shift: Int,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Photo paths
    val photo1: String? = null,
    val photo2: String? = null,
    val photo3: String? = null,
    val photo4: String? = null,
    val photo5: String? = null,
    val photo6: String? = null,
    val photo7: String? = null
)
