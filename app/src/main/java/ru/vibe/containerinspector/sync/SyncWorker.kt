package ru.vibe.containerinspector.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import ru.vibe.containerinspector.data.AppDatabase
import ru.vibe.containerinspector.data.ReportStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val client = OkHttpClient()
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.operatorDao()

    override suspend fun doWork(): Result {
        val reportId = inputData.getLong("report_id", -1L)
        if (reportId == -1L) return Result.failure()
        
        val pdfPath = inputData.getString("pdf_path") ?: return Result.failure()
        val pdfFile = File(pdfPath)
        if (!pdfFile.exists()) return Result.failure()

        val report = dao.getReportById(reportId) ?: return Result.failure()
        val config = dao.getConfig() ?: return Result.failure()

        val nextcloudUrl = config.nextcloudUrl ?: return Result.failure()
        val user = config.nextcloudUser ?: ""
        val pass = config.nextcloudPass ?: ""
        
        // Ensure URL ends with /remote.php/dav/files/[user]/
        val baseUrl = if (nextcloudUrl.endsWith("/")) nextcloudUrl else "$nextcloudUrl/"
        val fullNextcloudUrl = "${baseUrl}remote.php/dav/files/$user/"
        
        val authHeader = "Basic " + android.util.Base64.encodeToString("$user:$pass".toByteArray(), android.util.Base64.NO_WRAP)

        val year = SimpleDateFormat("yyyy", Locale.US).format(Date(report.timestamp))
        val month = getRussianMonthName(Calendar.getInstance().apply { timeInMillis = report.timestamp }.get(Calendar.MONTH))
        
        // Путь на Nextcloud: /[Год]/[Месяц]/Смена[N]/[Номер_Контейнера]/
        val targetDir = "$year/$month/Смена${report.shift}/${report.containerNumber}/"
        val targetFileUrl = "$fullNextcloudUrl$targetDir${pdfFile.name}"

        return try {
            // 1. Создание структуры папок
            createFolders(fullNextcloudUrl, targetDir, authHeader)
            
            // 2. Загрузка файла
            val request = Request.Builder()
                .url(targetFileUrl)
                .put(pdfFile.asRequestBody("application/pdf".toMediaTypeOrNull()))
                .addHeader("Authorization", authHeader)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("SyncWorker", "Successfully uploaded ${pdfFile.name}")
                    // Обновляем статус в БД
                    dao.updateReport(report.copy(status = ReportStatus.SENT))
                    Result.success()
                } else {
                    Log.e("SyncWorker", "Upload failed: ${response.code} ${response.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync error", e)
            Result.retry()
        }
    }

    private fun createFolders(rootUrl: String, path: String, authHeader: String) {
        val segments = path.split("/").filter { it.isNotEmpty() }
        var currentPath = ""
        segments.forEach { segment ->
            currentPath += "$segment/"
            val url = "$rootUrl$currentPath"
            val request = Request.Builder()
                .url(url)
                .method("MKCOL", null)
                .addHeader("Authorization", authHeader)
                .build()
            
            // Мы не проверяем успех, так как папка может уже существовать (405)
            try {
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.w("SyncWorker", "Folder creation failed for $segment", e)
            }
        }
    }

    private fun getRussianMonthName(monthIndex: Int): String {
        return listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )[monthIndex]
    }
}
