package ru.vibe.containerinspector.ui.screens

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import ru.vibe.containerinspector.R
import ru.vibe.containerinspector.viewmodel.MainViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun OCRScanScreen(viewModel: MainViewModel, onScanSuccess: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.sessionState.collectAsState()
    
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    
    val primaryBg = colorResource(id = R.color.primary_background)
    val accentOrange = colorResource(id = R.color.accent_orange)
    val cardBlue = colorResource(id = R.color.card_blue)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryBg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        textRecognizer.process(image)
                                            .addOnSuccessListener { visionText ->
                                                val pattern = Regex("[A-Z]{4}\\d{7}")
                                                visionText.textBlocks.forEach { block ->
                                                    val match = pattern.find(block.text.replace(" ", "").uppercase())
                                                    if (match != null) {
                                                        viewModel.setContainerNumber(match.value)
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalyzer
                            )
                        } catch (e: Exception) {
                            Log.e("OCR", "Use case binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp)
                    .border(2.dp, if (state.isContainerValid) Color.Green else Color.White, RoundedCornerShape(12.dp))
            )
        }
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (state.containerNumber.isEmpty()) "Наведите на номер..." else state.containerNumber,
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                )
                Text(
                    text = if (state.isContainerValid) "Номер распознан" else "Формат: ABCD 1234567",
                    color = if (state.isContainerValid) Color.Green else Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
        
        Button(
            onClick = onScanSuccess,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(64.dp),
            enabled = state.isContainerValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = accentOrange,
                disabledContainerColor = Color.DarkGray
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ПОДТВЕРДИТЬ НОМЕР", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
