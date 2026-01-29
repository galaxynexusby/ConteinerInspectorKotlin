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

@Composable
fun OCRConfirmScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val sessionState by viewModel.sessionState.collectAsState()
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

        Spacer(modifier = Modifier.height(48.dp))
        
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
                enabled = !isDuplicate && !isValidating
            ) {
                Text("ПОДТВЕРДИТЬ", fontWeight = FontWeight.Bold)
            }
        }
    }
}
