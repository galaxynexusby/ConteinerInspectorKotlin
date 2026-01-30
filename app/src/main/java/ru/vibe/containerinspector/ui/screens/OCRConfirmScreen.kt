package ru.vibe.containerinspector.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.vibe.containerinspector.R
import ru.vibe.containerinspector.viewmodel.MainViewModel
import ru.vibe.containerinspector.viewmodel.RemoteCheckState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween

@Composable
fun OCRConfirmScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val remoteCheck by viewModel.remoteCheckState.collectAsState()
    val primaryBg = colorResource(id = R.color.primary_background)
    
    var isDuplicate by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(true) }

    // Prevent system back navigation
    BackHandler(enabled = true) {
        // Do nothing as per requirement
    }

    LaunchedEffect(sessionState.containerNumber) {
        isValidating = true
        isDuplicate = viewModel.checkContainerExists(sessionState.containerNumber)
        viewModel.checkAndLockRemoteContainer(sessionState.containerNumber)
        isValidating = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "НОМЕР КОНТЕЙНЕРА",
            color = Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = sessionState.containerNumber,
            color = if (isDuplicate) Color.Red else Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        if (isDuplicate) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ошибка: контейнер с таким номером уже существует в базе (в очереди или в архиве)",
                color = Color.Red,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Блок облачной проверки
        Spacer(modifier = Modifier.height(24.dp))
        
        val statusColor by animateColorAsState(
            targetValue = when (remoteCheck) {
                is RemoteCheckState.Success -> Color.Green
                is RemoteCheckState.AlreadyExists -> Color.Red
                is RemoteCheckState.NetworkError -> Color.Yellow
                else -> Color.Gray
            },
            animationSpec = tween(500)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.card_blue).copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                when (remoteCheck) {
                    is RemoteCheckState.Checking -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Проверка в облаке...", color = Color.White, fontSize = 14.sp)
                    }
                    is RemoteCheckState.Success -> {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Контейнер свободен", color = Color.Green, fontSize = 14.sp)
                    }
                    is RemoteCheckState.AlreadyExists -> {
                        Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("УЖЕ ОСМАТРИВАЕТСЯ ДРУГИМ ПОСТОМ", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    is RemoteCheckState.NetworkError -> {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color.Yellow, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Offline: статус облака неизвестен", color = Color.Yellow, fontSize = 14.sp)
                    }
                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Подтвердите правильность\nраспознавания номера",
            color = Color.White,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.button_gray)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ИСПРАВИТЬ", fontWeight = FontWeight.Bold) // Improved label for "Back to scanner"
            }
            
            Button(
                onClick = {
                    viewModel.confirmContainerScan()
                    onConfirm()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.accent_orange)),
                shape = RoundedCornerShape(8.dp),
                enabled = !isDuplicate && !isValidating && (remoteCheck !is RemoteCheckState.Checking && remoteCheck !is RemoteCheckState.AlreadyExists)
            ) {
                Text("ПОДТВЕРДИТЬ", fontWeight = FontWeight.Bold)
            }
        }
        
        if (remoteCheck is RemoteCheckState.AlreadyExists || remoteCheck is RemoteCheckState.NetworkError) {
            TextButton(
                onClick = {
                    viewModel.confirmContainerScan()
                    onConfirm()
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "ПРОДОЛЖИТЬ ПРИНУДИТЕЛЬНО",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            }
        }
    }
}
