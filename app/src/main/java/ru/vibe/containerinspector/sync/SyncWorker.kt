package ru.vibe.containerinspector.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val client = OkHttpClient()
    
    // В реальном приложении эти данные должны быть в настройках или защищенном хранилище
    private val NEXTCLOUD_URL = "https://your-nextcloud-instance.com/remote.php/dav/files/user/"
    private val AUTH_HEADER = "Basic " + android.util.Base64.encodeToString("user:password".toByteArray(), android.util.Base64.NO_WRAP)

    override suspend fun doWork(): Result {
        val containerNumber = inputData.getString("container_number") ?: return Result.failure()
        val shift = inputData.getInt("shift", 1)
        val pdfPath = inputData.getString("pdf_path") ?: return Result.failure()
        val pdfFile = File(pdfPath)

        if (!pdfFile.exists()) return Result.failure()

        val year = SimpleDateFormat("yyyy", Locale.US).format(Date())
        val month = getRussianMonthName(Calendar.getInstance().get(Calendar.MONTH))
        
        // Путь на Nextcloud: /[Год]/[Месяц]/Смена[N]/[Номер_Контейнера]/
        val targetDir = "$year/$month/Смена$shift/$containerNumber/"
        val targetUrl = "$NEXTCLOUD_URL$targetDir${pdfFile.name}"

        return try {
            // 1. Создание структуры папок (через MKCOL, упрощенно)
            createFolders(targetDir)
            
            // 2. Загрузка файла
            val request = Request.Builder()
                .url(targetUrl)
                .put(pdfFile.asRequestBody("application/pdf".toMediaTypeOrNull()))
                .addHeader("Authorization", AUTH_HEADER)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("SyncWorker", "Successfully uploaded ${pdfFile.name}")
                    Result.success()
                } else {
                    Log.e("SyncWorker", "Upload failed: ${response.code}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync error", e)
            Result.retry()
        }
    }

    private fun createFolders(path: String) {
        val segments = path.split("/").filter { it.isNotEmpty() }
        var currentPath = ""
        segments.forEach { segment ->
            currentPath += "$segment/"
            val url = "$NEXTCLOUD_URL$currentPath"
            val request = Request.Builder()
                .url(url)
                .method("MKCOL", null)
                .addHeader("Authorization", AUTH_HEADER)
                .build()
            
            // Мы не проверяем успех, так как папка может уже существовать (405)
            client.newCall(request).execute().close()
        }
    }

    private fun getRussianMonthName(monthIndex: Int): String {
        return listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )[monthIndex]
    }
}
