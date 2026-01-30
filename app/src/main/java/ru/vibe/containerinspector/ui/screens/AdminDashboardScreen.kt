package ru.vibe.containerinspector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import ru.vibe.containerinspector.data.Operator
import ru.vibe.containerinspector.viewmodel.MainViewModel
import ru.vibe.containerinspector.viewmodel.ConnectionState
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val primaryBg = colorResource(id = R.color.primary_background)
    val accentOrange = colorResource(id = R.color.accent_orange)
    val context = LocalContext.current

    // Экспорт: сохранение файла
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(viewModel.getExportJson().toByteArray())
            }
        }
    }

    // Импорт: выбор файла
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                viewModel.importData(content)
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = colorResource(id = R.color.card_blue),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.People, "Операторы") },
                    label = { Text("Операторы") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentOrange,
                        selectedTextColor = accentOrange,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Settings, "Настройки") },
                    label = { Text("Настройки") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentOrange,
                        selectedTextColor = accentOrange,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        },
        containerColor = primaryBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (selectedTab == 0) "УПРАВЛЕНИЕ ОПЕРАТОРАМИ" else "НАСТРОЙКИ СИНХРОНИЗАЦИИ",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.ExitToApp, "Выход", tint = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (selectedTab) {
                0 -> OperatorsTab(
                    viewModel = viewModel,
                    onExport = { createDocumentLauncher.launch("container_inspector_backup.json") },
                    onImport = { openDocumentLauncher.launch(arrayOf("application/json")) }
                )
                1 -> SettingsTab(viewModel)
            }
        }
    }
}

@Composable
fun OperatorsTab(
    viewModel: MainViewModel,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val operators by viewModel.allOperators.collectAsState()
    val accentOrange = colorResource(id = R.color.accent_orange)

    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f).height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentOrange),
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(2.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                    Text(
                        "ДОБАВИТЬ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
            
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.weight(1f).height(64.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentOrange),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentOrange),
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(2.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(20.dp))
                    Text(
                        "ЭКСПОРТ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.weight(1f).height(64.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentOrange),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentOrange),
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(2.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(20.dp))
                    Text(
                        "ИМПОРТ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(operators) { op ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.card_blue))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(op.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Смена: ${op.shift}", color = Color.Gray, fontSize = 12.sp)
                        }
                        if (op.name != "admin") {
                            IconButton(onClick = { viewModel.deleteOperator(op) }) {
                                Icon(Icons.Default.Delete, "Удалить", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }
        var shift by remember { mutableStateOf("1") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новый оператор") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") })
                    OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Пароль") })
                    OutlinedTextField(value = shift, onValueChange = { shift = it }, label = { Text("Смена") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addOperator(name, shift.toIntOrNull() ?: 1, pass)
                    showAddDialog = false
                }) {
                    Text("СОЗДАТЬ", color = accentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("ОТМЕНА")
                }
            }
        )
    }
}

@Composable
fun SettingsTab(viewModel: MainViewModel) {
    val config by viewModel.appConfig.collectAsState()
    val connSuccess by viewModel.ncConnectionState.collectAsState()
    val accentOrange = colorResource(id = R.color.accent_orange)

    var url by remember { mutableStateOf(config?.nextcloudUrl ?: "") }
    var user by remember { mutableStateOf(config?.nextcloudUser ?: "") }
    var pass by remember { mutableStateOf(config?.nextcloudPass ?: "") }

    LaunchedEffect(config) {
        config?.let {
            url = it.nextcloudUrl ?: ""
            user = it.nextcloudUser ?: ""
            pass = it.nextcloudPass ?: ""
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Nextcloud URL (например, https://use20.thegood.cloud/)", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("Пользователь", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Пароль приложения", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.updateConfig(url, user, pass) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.card_blue))
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("СОХРАНИТЬ")
            }

            Button(
                onClick = { viewModel.testConnection() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = accentOrange)
            ) {
                Text("ПРОВЕРКА")
            }
        }

        if (connSuccess !is ConnectionState.Idle) {
            val (icon, color, text) = when (connSuccess) {
                is ConnectionState.Loading -> Triple(Icons.Default.Refresh, Color.Gray, "Проверка...")
                is ConnectionState.Success -> Triple(Icons.Default.CheckCircle, Color.Green, "Связь установлена")
                is ConnectionState.Error -> Triple(Icons.Default.Cancel, Color.Red, (connSuccess as ConnectionState.Error).message)
                else -> Triple(Icons.Default.Info, Color.Gray, "")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
                Spacer(Modifier.width(8.dp))
                Text(text, color = color, fontSize = 14.sp)
            }
        }
    }
}
