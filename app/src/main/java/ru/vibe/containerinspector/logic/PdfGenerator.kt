package ru.vibe.containerinspector.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {
    /**
     * Генерирует PDF отчет с данными осмотра.
     */
    fun generateReport(
        context: Context,
        containerNumber: String,
        operator: String,
        shift: Int,
        photoPaths: List<String>
    ): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            textSize = 16f
        }

        // Страница 1: Информация и первые фото
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        canvas.drawText("Отчет об осмотре контейнера", 50f, 50f, titlePaint)
        canvas.drawText("Номер контейнера: $containerNumber", 50f, 90f, textPaint)
        canvas.drawText("Оператор: $operator", 50f, 120f, textPaint)
        canvas.drawText("Смена: $shift", 50f, 150f, textPaint)
        val dateFormat = SimpleDateFormat("dd.MM.yyyy в HH:mm", Locale.getDefault())
        canvas.drawText("Дата: ${dateFormat.format(Date())}", 50f, 180f, textPaint)

        val steps = listOf(
            "Контейнер открыт и пуст",
            "Груз (вид 1: Big Bags)",
            "Груз (вид 2: детально)",
            "Правая дверь закрыта",
            "Табличка КБК (CSC Plate)",
            "Пломба (крупный план)",
            "Финальный вид: контейнер закрыт"
        )

        var yOffset = 220f
        photoPaths.forEachIndexed { index, path ->
            if (index > 0 && index % 2 == 0 && yOffset > 500f) {
                // Новая страница
                pdfDocument.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, (index / 2) + 1).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                yOffset = 50f
            }

            // Загружаем и масштабируем (меняем местами W и H для вертикальной ориентации после поворота)
            val bitmap = loadAndScaleBitmap(path, 200, 260) 
            if (bitmap != null) {
                val xOffset = if (index % 2 == 0) 50f else 310f
                canvas.drawBitmap(bitmap, xOffset, yOffset, paint)
                
                val caption = "Фото ${index + 1}: ${steps.getOrElse(index) { "" }}"
                canvas.drawText(caption, xOffset, yOffset + 280f, textPaint)
                
                if (index % 2 != 0) {
                    yOffset += 320f
                }
            }
        }

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(null), "Report_${containerNumber}_${System.currentTimeMillis()}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }

        return file
    }

    private fun loadAndScaleBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        
        val bitmap = BitmapFactory.decodeFile(path, options) ?: return null
        
        // Поворот на 90 градусов по часовой стрелке
        val matrix = Matrix().apply { postRotate(90f) }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        
        return Bitmap.createScaledBitmap(rotatedBitmap, reqWidth, reqHeight, true)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
