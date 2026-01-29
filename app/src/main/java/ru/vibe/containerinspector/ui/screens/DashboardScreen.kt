package ru.vibe.containerinspector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.vibe.containerinspector.R
import ru.vibe.containerinspector.data.InspectionReport
import ru.vibe.containerinspector.data.ReportStatus
import ru.vibe.containerinspector.ui.components.BottomNavBar
import ru.vibe.containerinspector.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel, 
    onStartInspection: (InspectionReport) -> Unit,
    onNewScan: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val allReports by viewModel.allReports.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()

    // Filter reports for the "Queue" tab (Dashboard)
    val queueReports = allReports.filter { 
        it.status == ReportStatus.READY_TO_INSPECT || 
        it.status == ReportStatus.IN_PROGRESS || 
        it.status == ReportStatus.UNFINISHED 
    }

    val primaryBg = colorResource(id = R.color.primary_background)

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = "dashboard",
                onNavigate = { route ->
                    when (route) {
                        "history" -> onNavigateToHistory()
                        "profile" -> onNavigateToProfile()
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
                .padding(horizontal = 16.dp)
        ) {
            // Header: User Info & Sync Status
            DashboardHeader(
                operatorName = sessionState.operator, 
                isSynced = appConfig != null,
                onProfileClick = onNavigateToProfile
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Очередь на осмотр",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (queueReports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("В очереди нет контейнеров", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(queueReports) { report ->
                        InspectionReportCard(
                            report = report, 
                            onAction = onStartInspection,
                            onDelete = { viewModel.deleteReport(it) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHeader(operatorName: String, isSynced: Boolean, onProfileClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.card_blue)),
        shape = RoundedCornerShape(12.dp),
        onClick = onProfileClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = colorResource(id = R.color.accent_orange),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(operatorName, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Оператор", color = Color.Gray, fontSize = 12.sp)
                }
            }
            
            // Sync status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isSynced) "В сети" else "Оффлайн",
                    color = if (isSynced) Color.Green else Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    if (isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = if (isSynced) Color.Green else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun InspectionReportCard(
    report: InspectionReport, 
    onAction: (InspectionReport) -> Unit,
    onDelete: (InspectionReport) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.card_blue)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    report.containerNumber,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = { onDelete(report) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (statusText, statusColor) = when (report.status) {
                    ReportStatus.READY_TO_INSPECT -> "Готов к осмотру" to colorResource(id = R.color.accent_orange)
                    ReportStatus.UNFINISHED -> "Не завершен" to Color.Red
                    ReportStatus.IN_PROGRESS -> "В процессе" to Color.Yellow
                    else -> "" to Color.Gray
                }
                
                Text(statusText, color = statusColor, fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onAction(report) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.accent_orange)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                val btnText = if (report.status == ReportStatus.UNFINISHED) "ЗАВЕРШИТЬ ОСМОТР" else "НАЧАТЬ ОСМОТР"
                Text(btnText, fontWeight = FontWeight.Bold)
            }
        }
    }
}
