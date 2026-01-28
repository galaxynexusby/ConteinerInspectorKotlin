package ru.vibe.containerinspector.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.vibe.containerinspector.viewmodel.MainViewModel

@Composable
fun AuthScreen(viewModel: MainViewModel, onAuthSuccess: () -> Unit) {
    val state by viewModel.sessionState.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Авторизация", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Выберите оператора:")
        Row {
            Button(onClick = { viewModel.setOperator("Оксана") }) { Text("Оксана") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { viewModel.setOperator("Андрей") }) { Text("Андрей") }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Выбран: ${state.operator}")
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Смена (1-4):")
        Row {
            (1..4).forEach { shift ->
                FilterChip(
                    selected = state.shift == shift,
                    onClick = { viewModel.setShift(shift) },
                    label = { Text(shift.toString()) }
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onAuthSuccess,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            enabled = state.operator.isNotEmpty()
        ) {
            Text("Войти")
        }
    }
}
