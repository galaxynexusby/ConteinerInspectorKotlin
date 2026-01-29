package ru.vibe.containerinspector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.*
import coil.compose.AsyncImage
import ru.vibe.containerinspector.R
import ru.vibe.containerinspector.logic.PdfGenerator
import ru.vibe.containerinspector.sync.SyncWorker
import ru.vibe.containerinspector.viewmodel.MainViewModel
import java.io.File

@Composable
fun SummaryScreen(viewModel: MainViewModel, onFinish: () -> Unit) {
    val activeReport by viewModel.activeReport.collectAsState()
    val context = LocalContext.current
    
    val report = activeReport ?: return // Defensive check
    val primaryBg = colorResource(id = R.color.primary_background)
    val accentOrange = colorResource(id = R.color.accent_orange)

    val photos = listOfNotNull(
        report.photo1, report.photo2, report.photo3,
        report.photo4, report.photo5, report.photo6, report.photo7
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryBg)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "ПРОВЕРКА ОСМОТРА",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            "Контейнер: ${report.containerNumber}",
            color = Color.Gray,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Photos Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos) { path ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                val pdfFile = PdfGenerator.generateReport(
                    context,
                    report.containerNumber,
                    report.operatorName,
                    report.shift,
                    photos
                )
                
                if (pdfFile != null) {
                    val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                        .setInputData(workDataOf(
                            "report_id" to report.id,
                            "pdf_path" to pdfFile.absolutePath
                        ))
                        .setConstraints(Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                        .build()
                    
                    WorkManager.getInstance(context).enqueue(syncRequest)
                }
                
                onFinish()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentOrange),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("ПОДТВЕРДИТЬ И ОТПРАВИТЬ", color = Color.White, fontWeight = FontWeight.Bold)
        }
        
        TextButton(
            onClick = onFinish,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("ВЕРНУТЬСЯ ПОЗЖЕ", color = Color.Gray)
        }
    }
}
