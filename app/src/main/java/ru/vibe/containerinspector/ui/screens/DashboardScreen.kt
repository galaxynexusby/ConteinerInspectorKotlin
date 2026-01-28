package ru.vibe.containerinspector.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.vibe.containerinspector.viewmodel.MainViewModel

@Composable
fun DashboardScreen(viewModel: MainViewModel, onNewInspection: () -> Unit) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewInspection) {
                Icon(Icons.Default.Add, contentDescription = "Новый осмотр")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Рабочий стол", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Активные задачи отсутствуют")
        }
    }
}
