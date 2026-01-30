package ru.vibe.containerinspector.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.vibe.containerinspector.R

@Composable
fun CameraOverlay(currentStep: Int) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Logic for frame dimensions based on steps
    // Step 0: Initial (Wide)
    // Steps 1-3: Cargo (Medium)
    // Step 4: Door (Medium)
    // Step 5: CSC Plate (Square)
    // Step 6: Seal (Square)
    
    val targetWidth = when (currentStep) {
        0, 6 -> screenWidth * 0.8f // Wide for steps 1 and 7 (0 and 6 in code)
        4, 5 -> 200.dp // Square for steps 5 and 6 (4 and 5 in code)
        else -> screenWidth * 0.7f // Medium for others
    }

    val targetHeight = when (currentStep) {
        0, 6 -> 200.dp
        4, 5 -> 200.dp
        else -> 300.dp
    }

    val hintText = when (currentStep) {
        0 -> "Поместите открытый контейнер в рамку"
        1, 2 -> "Сфотографируйте груз"
        3 -> "Закройте левую дверь"
        4 -> "Поместите табличку КБК сюда"
        5 -> "Поместите пломбу сюда"
        6 -> "Финальный вид контейнера"
        else -> ""
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = tween(durationMillis = 500),
        label = "width"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(durationMillis = 500),
        label = "height"
    )

    val accentOrange = colorResource(id = R.color.accent_orange)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(animatedWidth, animatedHeight)) {
            val strokeWidth = 4.dp.toPx()
            val cornerRadius = 12.dp.toPx()
            
            // Draw a semi-transparent orange frame
            drawRoundRect(
                color = accentOrange.copy(alpha = 0.6f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = strokeWidth)
            )
        }

        // Hint Text
        if (hintText.isNotEmpty()) {
            Text(
                text = hintText,
                color = accentOrange.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = (animatedHeight / 2) + 24.dp)
            )
        }
    }
}
