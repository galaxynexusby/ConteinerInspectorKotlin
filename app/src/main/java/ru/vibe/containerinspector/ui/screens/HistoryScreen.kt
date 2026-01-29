package ru.vibe.containerinspector.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import ru.vibe.containerinspector.data.ReportStatus
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import ru.vibe.containerinspector.ui.components.BottomNavBar
import ru.vibe.containerinspector.viewmodel.MainViewModel

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNewScan: () -> Unit
) {
    val allReports by viewModel.allReports.collectAsState()
    val historyReports = allReports.filter { 
        it.status == ReportStatus.COMPLETED || it.status == ReportStatus.SENT 
    }
    
    val primaryBg = colorResource(id = R.color.primary_background)

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = "history",
                onNavigate = { route ->
                    when (route) {
                        "dashboard" -> onNavigateToDashboard()
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
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "АРХИВ ОСМОТРОВ",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (historyReports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("В архиве пока пусто", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(historyReports) { report ->
                        HistoryReportCard(report)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryReportCard(report: ru.vibe.containerinspector.data.InspectionReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.card_blue)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(report.containerNumber, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    ru.vibe.containerinspector.util.DateUtils.formatTimestamp(report.timestamp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            val (statusText, statusColor, icon) = if (report.status == ReportStatus.SENT) {
                Triple("ВЫГРУЖЕНО", Color.Green, Icons.Default.CloudDone)
            } else {
                Triple("В ОЧЕРЕДИ", colorResource(id = R.color.accent_orange), Icons.Default.CloudUpload)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    statusText, 
                    color = statusColor, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 10.sp
                )
            }
        }
    }
}
