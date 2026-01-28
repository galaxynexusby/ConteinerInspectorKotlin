package ru.vibe.containerinspector.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.work.*
import coil.compose.rememberAsyncImagePainter
import ru.vibe.containerinspector.logic.PdfGenerator
import ru.vibe.containerinspector.sync.SyncWorker
import ru.vibe.containerinspector.viewmodel.MainViewModel
import java.io.File

@Composable
fun SummaryScreen(viewModel: MainViewModel, onFinish: () -> Unit) {
    val state by viewModel.sessionState.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Итоги осмотра: ${state.containerNumber}", 
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineSmall
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(state.photos.size) { index ->
                Card(
                    modifier = Modifier.padding(4.dp).height(150.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(File(state.photos[index])),
                        contentDescription = "Фото ${index + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        
        Button(
            onClick = {
                val pdfFile = PdfGenerator.generateReport(
                    context,
                    state.containerNumber,
                    state.operator,
                    state.shift,
                    state.photos
                )
                
                if (pdfFile != null) {
                    val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                        .setInputData(workDataOf(
                            "container_number" to state.containerNumber,
                            "shift" to state.shift,
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
                .padding(16.dp)
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("ОТПРАВИТЬ ОТЧЕТ (PDF)", style = MaterialTheme.typography.titleMedium)
        }
    }
}
