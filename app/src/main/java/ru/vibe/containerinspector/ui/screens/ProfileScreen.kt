package ru.vibe.containerinspector.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.vibe.containerinspector.R
import ru.vibe.containerinspector.ui.components.BottomNavBar
import ru.vibe.containerinspector.viewmodel.MainViewModel

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onLogout: () -> Unit,
    onNewScan: () -> Unit
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val allReports by viewModel.allReports.collectAsState()
    val primaryBg = colorResource(id = R.color.primary_background)
    
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Calculate stats
    val completedCount = allReports.count { it.status == ru.vibe.containerinspector.data.ReportStatus.SENT || it.status == ru.vibe.containerinspector.data.ReportStatus.COMPLETED }
    val queueCount = allReports.count { it.status == ru.vibe.containerinspector.data.ReportStatus.READY_TO_INSPECT || it.status == ru.vibe.containerinspector.data.ReportStatus.UNFINISHED }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Выход из профиля") },
            text = { Text("Вы уверены, что хотите выйти? Текущая сессия будет завершена.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("ВЫЙТИ", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("ОТМЕНА")
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = "profile",
                onNavigate = { route ->
                    when (route) {
                        "dashboard" -> onNavigateToDashboard()
                        "history" -> onNavigateToHistory()
                    }
                },
                onFabClick = onNewScan
            )
        },
        containerColor = primaryBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                color = colorResource(id = R.color.card_blue)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(20.dp),
                    tint = colorResource(id = R.color.accent_orange)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                sessionState.operator,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Смена №${sessionState.shift}",
                color = Color.Gray,
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatItem("Готово", completedCount.toString())
                ProfileStatItem("В очереди", queueCount.toString())
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileActionButton(
                    icon = Icons.Default.Settings,
                    label = "Настройки оборудования",
                    onClick = onNavigateToAdmin
                )
                
                ProfileActionButton(
                    icon = Icons.Default.Logout,
                    label = "Завершить работу",
                    onClick = { showLogoutDialog = true },
                    isDestructive = true
                )
            }
        }
    }
}

@Composable
fun ProfileStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = colorResource(id = R.color.accent_orange), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun ProfileActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(id = R.color.card_blue)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) Color.Red else Color.White
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                label,
                color = if (isDestructive) Color.Red else Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
