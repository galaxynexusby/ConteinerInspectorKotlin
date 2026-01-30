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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
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
        
        val baseUrl = if (nextcloudUrl.endsWith("/")) nextcloudUrl else "$nextcloudUrl/"
        val fullNextcloudUrl = "${baseUrl}remote.php/dav/files/$user/"
        val authHeader = "Basic " + android.util.Base64.encodeToString("$user:$pass".toByteArray(), android.util.Base64.NO_WRAP)

        val year = SimpleDateFormat("yyyy", Locale.US).format(Date(report.timestamp))
        val month = getRussianMonthName(Calendar.getInstance().apply { timeInMillis = report.timestamp }.get(Calendar.MONTH))
        val targetDir = "$year/$month/Смена${report.shift}/${report.containerNumber}/"

        val photoPaths = listOfNotNull(report.photo1, report.photo2, report.photo3, report.photo4, report.photo5, report.photo6, report.photo7)

        return try {
            createFolders(fullNextcloudUrl, targetDir, authHeader)
            
            // 1. Загрузка PDF
            uploadFile("$fullNextcloudUrl$targetDir${pdfFile.name}", pdfFile, "application/pdf", authHeader)

            // 2. Загрузка сжатых фото
            photoPaths.forEach { path ->
                val originalFile = File(path)
                if (originalFile.exists()) {
                    val compressedFile = compressImage(originalFile)
                    uploadFile("$fullNextcloudUrl$targetDir${originalFile.name}", compressedFile, "image/jpeg", authHeader)
                    // Удаляем временный сжатый файл после загрузки
                    if (compressedFile.absolutePath != originalFile.absolutePath) {
                        compressedFile.delete()
                    }
                }
            }

            Log.d("SyncWorker", "Successfully uploaded all files for ${report.containerNumber}")
            dao.updateReport(report.copy(status = ReportStatus.SENT))
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync error", e)
            Result.retry()
        }
    }

    private fun uploadFile(url: String, file: File, contentType: String, authHeader: String) {
        val request = Request.Builder()
            .url(url)
            .put(file.asRequestBody(contentType.toMediaTypeOrNull()))
            .addHeader("Authorization", authHeader)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Upload failed: ${response.code} for $url")
            }
        }
    }

    private fun compressImage(file: File): File {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return file
        // Ограничиваем общие 8 файлов (1 PDF + 7 фото) в 3 Мб. 
        // Каждое фото должно быть ~350 Кб.
        val targetFile = File(applicationContext.cacheDir, "temp_" + file.name)
        var quality = 85
        
        do {
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            quality -= 10
        } while (targetFile.length() > 400 * 1024 && quality > 10)
        
        return targetFile
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
