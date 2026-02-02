package ru.vibe.containerinspector.ui.screens

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import ru.vibe.containerinspector.R
import ru.vibe.containerinspector.viewmodel.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import ru.vibe.containerinspector.ui.components.CameraOverlay
import ru.vibe.containerinspector.logic.AutoTorchManager
import ru.vibe.containerinspector.data.InspectionReport

@Composable
fun InspectionScreen(viewModel: MainViewModel, onPostpone: () -> Unit, onComplete: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activeReport by viewModel.activeReport.collectAsState()

    val report = activeReport ?: return // Defensive check
    
    val autoTorchManager = remember {
        AutoTorchManager(context, getCurrentStep = { viewModel.activeReport.value?.currentStep ?: 0 })
    }

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(autoTorchManager)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(autoTorchManager)
        }
    }
    
    val steps = listOf(
        "Контейнер открыт и пуст",
        "Груз (вид 1: Big Bags)",
        "Груз (вид 2: детально)",
        "Правая дверь закрыта",
        "Табличка КБК (CSC Plate)",
        "Пломба (крупный план)",
        "Финальный вид: контейнер закрыт, пломба установлена"
    )

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val mainExecutor = ContextCompat.getMainExecutor(context)
    
    // Tracking if the photo for CURRENT step is taken
    var isPhotoTakenForCurrentStep by remember { mutableStateOf(false) }

    val primaryBg = colorResource(id = R.color.primary_background)
    val accentOrange = colorResource(id = R.color.accent_orange)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryBg)
    ) {
        // Top Bar: Step Indicator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Шаг ${report.currentStep + 1} из 7",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                steps.getOrElse(report.currentStep) { "" },
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = (report.currentStep + 1) / 7f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = accentOrange,
                trackColor = Color.DarkGray,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // Camera Preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(Color.Black, RoundedCornerShape(12.dp))
        ) {
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
                        
                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                            autoTorchManager.setCameraControl(camera.cameraControl)
                        } catch (e: Exception) {
                            Log.e("Inspection", "Binding failed", e)
                        }
                    }, mainExecutor)
                }
            )

            // AR Viewfinder Overlay
            CameraOverlay(currentStep = report.currentStep)

            // Auto-torch Indicator Icon
            val currentStep = report.currentStep
            val isAutoTorchActiveStep = currentStep in 0..2
            if (isAutoTorchActiveStep) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.FlashAuto,
                            contentDescription = "Авто-фонарик",
                            tint = accentOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "A",
                            color = accentOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            if (isPhotoTakenForCurrentStep) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Фото сделано",
                        tint = Color.Green,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Postpone
            TextButton(onClick = { 
                viewModel.postponeInspection() 
                onPostpone()
            }) {
                Text("ОТЛОЖИТЬ", color = Color.Gray, fontWeight = FontWeight.Bold)
            }

            // Capture Button
            FloatingActionButton(
                onClick = {
                    imageCapture?.let { capture ->
                        takePhoto(context, capture, mainExecutor) { path ->
                            viewModel.addPhoto(path)
                            isPhotoTakenForCurrentStep = true
                        }
                    }
                },
                containerColor = accentOrange,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Снять", modifier = Modifier.size(36.dp))
            }

            // Next Button
            Button(
                onClick = {
                    if (report.currentStep >= 6) {
                        viewModel.nextStep()
                        onComplete()
                    } else {
                        viewModel.nextStep()
                        isPhotoTakenForCurrentStep = false
                    }
                },
                enabled = isPhotoTakenForCurrentStep,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentOrange,
                    disabledContainerColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(140.dp).height(56.dp)
            ) {
                val btnText = if (report.currentStep >= 6) "ЗАВЕРШИТЬ" else "ДАЛЕЕ"
                Text(btnText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    onPhotoCaptured: (String) -> Unit
) {
    val photoFile = File(
        context.externalMediaDirs.firstOrNull(),
        "IMG_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onPhotoCaptured(photoFile.absolutePath)
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e("Inspection", "Photo capture failed: ${exc.message}", exc)
            }
        }
    )
}
