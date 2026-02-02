package ru.vibe.containerinspector.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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

    val accentOrange = colorResource(id = R.color.accent_orange)
    
    // Hint Text logic
    val hintText = when (currentStep) {
        0 -> "ОТКРЫТЫЙ КОНТЕЙНЕР\nСовместите углы проема с рамкой"
        1, 2 -> "ГРУЗ ВНУТРИ\nСнимайте по центру"
        3 -> "ПРАВАЯ ДВЕРЬ\nВид на номер и запоры"
        4 -> "ТАБЛИЧКА КБК\nПоместите в центр рамки"
        5 -> "ПЛОМБА\nНаведите прицел на пломбу"
        6 -> "ОБЩИЙ ВИД\nКонтейнер закрыт и опломбирован"
        else -> ""
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val strokeWidth = 3.dp.toPx()
            val dashEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            when (currentStep) {
                0, 1, 2 -> {
                    // Perspective Tunnel (Container Interior)
                    // Inner rectangle (vanishing point area)
                    val innerW = width * 0.4f
                    val innerH = height * 0.4f
                    val innerL = (width - innerW) / 2
                    val innerT = (height - innerH) / 2
                    
                    // Outer rectangle (door frame)
                    val outerW = width * 0.85f
                    val outerH = height * 0.85f
                    val outerL = (width - outerW) / 2
                    val outerT = (height - outerH) / 2

                    // Draw outer frame
                    drawRoundRect(
                        color = accentOrange,
                        topLeft = Offset(outerL, outerT),
                        size = Size(outerW, outerH),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = Stroke(width = strokeWidth)
                    )

                    // Draw perspective lines
                    drawLine(color = accentOrange.copy(alpha = 0.4f), start = Offset(outerL, outerT), end = Offset(innerL, innerT), strokeWidth = 2.dp.toPx(), pathEffect = dashEffect)
                    drawLine(color = accentOrange.copy(alpha = 0.4f), start = Offset(outerL + outerW, outerT), end = Offset(innerL + innerW, innerT), strokeWidth = 2.dp.toPx(), pathEffect = dashEffect)
                    drawLine(color = accentOrange.copy(alpha = 0.4f), start = Offset(outerL, outerT + outerH), end = Offset(innerL, innerT + innerH), strokeWidth = 2.dp.toPx(), pathEffect = dashEffect)
                    drawLine(color = accentOrange.copy(alpha = 0.4f), start = Offset(outerL + outerW, outerT + outerH), end = Offset(innerL + innerW, innerT + innerH), strokeWidth = 2.dp.toPx(), pathEffect = dashEffect)
                }
                4 -> {
                    // CSC Plate (Corner Brackets)
                    val rectW = width * 0.7f
                    val rectH = rectW * 0.7f
                    val left = (width - rectW) / 2
                    val top = (height - rectH) / 2
                    val bracketLen = 40.dp.toPx()

                    // Top-Left
                    drawLine(accentOrange, Offset(left, top), Offset(left + bracketLen, top), strokeWidth)
                    drawLine(accentOrange, Offset(left, top), Offset(left, top + bracketLen), strokeWidth)
                    // Top-Right
                    drawLine(accentOrange, Offset(left + rectW, top), Offset(left + rectW - bracketLen, top), strokeWidth)
                    drawLine(accentOrange, Offset(left + rectW, top), Offset(left + rectW, top + bracketLen), strokeWidth)
                    // Bottom-Left
                    drawLine(accentOrange, Offset(left, top + rectH), Offset(left + bracketLen, top + rectH), strokeWidth)
                    drawLine(accentOrange, Offset(left, top + rectH), Offset(left, top + rectH - bracketLen), strokeWidth)
                    // Bottom-Right
                    drawLine(accentOrange, Offset(left + rectW, top + rectH), Offset(left + rectW - bracketLen, top + rectH), strokeWidth)
                    drawLine(accentOrange, Offset(left + rectW, top + rectH), Offset(left + rectW, top + rectH - bracketLen), strokeWidth)
                }
                5 -> {
                    // Seal (Target/Crosshair)
                    val radius = 40.dp.toPx()
                    drawCircle(color = accentOrange, radius = radius, center = center, style = Stroke(width = strokeWidth))
                    drawLine(accentOrange, Offset(center.x - radius - 20f, center.y), Offset(center.x + radius + 20f, center.y), 2.dp.toPx())
                    drawLine(accentOrange, Offset(center.x, center.y - radius - 20f), Offset(center.x, center.y + radius + 20f), 2.dp.toPx())
                }
                else -> {
                    // Simple Frame for others
                    drawRoundRect(
                        color = accentOrange.copy(alpha = 0.5f),
                        topLeft = Offset(width * 0.1f, height * 0.2f),
                        size = Size(width * 0.8f, height * 0.6f),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }

        // Hint Text with subtle background
        if (hintText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = hintText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
