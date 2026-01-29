package ru.vibe.containerinspector.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OperatorDao {
    // Operators
    @Query("SELECT * FROM operators ORDER BY name ASC")
    fun getAllOperators(): Flow<List<Operator>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperator(operator: Operator)

    @Delete
    suspend fun deleteOperator(operator: Operator)

    @Query("SELECT COUNT(*) FROM operators")
    suspend fun getCount(): Int

    // Inspection Reports
    @Query("SELECT * FROM inspection_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<InspectionReport>>

    @Query("SELECT * FROM inspection_reports WHERE id = :id")
    suspend fun getReportById(id: Long): InspectionReport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: InspectionReport): Long

    @Update
    suspend fun updateReport(report: InspectionReport)

    @Delete
    suspend fun deleteReport(report: InspectionReport)

    // App Config
    @Query("SELECT * FROM app_config WHERE id = 1")
    fun getConfigFlow(): Flow<AppConfig?>

    @Query("SELECT * FROM app_config WHERE id = 1")
    suspend fun getConfig(): AppConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AppConfig)
}
