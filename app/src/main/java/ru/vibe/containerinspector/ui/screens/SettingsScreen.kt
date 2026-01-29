package ru.vibe.containerinspector.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.vibe.containerinspector.R
import ru.vibe.containerinspector.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onSave: () -> Unit) {
    val operators by viewModel.allOperators.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()
    
    val primaryBg = colorResource(id = R.color.primary_background)
    val cardBg = colorResource(id = R.color.card_blue)
    val accentOrange = colorResource(id = R.color.accent_orange)

    // Operator state
    var opName by remember { mutableStateOf("") }
    var opShift by remember { mutableStateOf(1) }
    var opPassword by remember { mutableStateOf("") }

    // Nextcloud state
    var ncUrl by remember { mutableStateOf("") }
    var ncUser by remember { mutableStateOf("") }
    var ncPass by remember { mutableStateOf("") }

    // Initialize Nextcloud state from config
    LaunchedEffect(appConfig) {
        appConfig?.let {
            ncUrl = it.nextcloudUrl ?: ""
            ncUser = it.nextcloudUser ?: ""
            ncPass = it.nextcloudPass ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки системы", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBg)
            )
        },
        containerColor = primaryBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Nextcloud Configuration
            item {
                Text("Облачное хранилище (Nextcloud)", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = ncUrl,
                            onValueChange = { ncUrl = it },
                            label = { Text("URL сервера") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Gray, focusedBorderColor = accentOrange, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = ncUser,
                            onValueChange = { ncUser = it },
                            label = { Text("Имя пользователя") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Gray, focusedBorderColor = accentOrange, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = ncPass,
                            onValueChange = { ncPass = it },
                            label = { Text("Пароль приложения") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Gray, focusedBorderColor = accentOrange, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        Button(
                            onClick = { viewModel.updateConfig(ncUrl, ncUser, ncPass) },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = accentOrange)
                        ) {
                            Text("ОБНОВИТЬ ОБЛАКО")
                        }
                    }
                }
            }

            // Add Operator
            item {
                Text("Добавить нового оператора", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = opName,
                            onValueChange = { opName = it },
                            label = { Text("Фамилия оператора") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Gray, focusedBorderColor = accentOrange, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Смена: ", color = Color.White)
                            (1..4).forEach { s ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = opShift == s,
                                        onClick = { opShift = s },
                                        colors = RadioButtonDefaults.colors(selectedColor = accentOrange)
                                    )
                                    Text(s.toString(), color = Color.White)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = opPassword,
                            onValueChange = { opPassword = it },
                            label = { Text("Пароль") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Gray, focusedBorderColor = accentOrange, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )

                        Button(
                            onClick = {
                                if (opName.isNotBlank() && opPassword.isNotBlank()) {
                                    viewModel.addOperator(opName, opShift, opPassword)
                                    opName = ""
                                    opPassword = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = accentOrange)
                        ) {
                            Text("ДОБАВИТЬ")
                        }
                    }
                }
            }

            // Operators List
            item {
                Text("Действующие операторы", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            items(operators) { op ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(op.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Смена №${op.shift}", color = Color.Gray, fontSize = 12.sp)
                        }
                        if (op.name != "admin") {
                            IconButton(onClick = { viewModel.deleteOperator(op) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("СОХРАНИТЬ И ВЕРНУТЬСЯ", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
