package ru.vibe.containerinspector.ui.screens

import androidx.compose.foundation.layout.*
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
fun AuthScreen(
    viewModel: MainViewModel,
    onAuthSuccess: () -> Unit
) {
    val state by viewModel.sessionState.collectAsState()
    val operators by viewModel.allOperators.collectAsState()
    
    var expanded by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    val primaryBg = colorResource(id = R.color.primary_background)
    val accentOrange = colorResource(id = R.color.accent_orange)

    Surface(
        color = primaryBg,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "АВТОРИЗАЦИЯ", 
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.operator,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Выберите оператора", color = Color.Gray) },
                    trailingIcon = { 
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentOrange,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = accentOrange,
                        cursorColor = accentOrange,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    operators.forEach { op ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = op.name,
                                    color = Color.Black // Принудительно черный цвет для лучшей видимости на светлом фоне
                                )
                            },
                            onClick = {
                                viewModel.setOperator(op.name)
                                viewModel.setShift(op.shift)
                                expanded = false
                                error = ""
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    error = ""
                },
                label = { Text("Пароль", color = Color.Gray) },
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentOrange,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = accentOrange,
                    cursorColor = accentOrange,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (error.isNotEmpty()) {
                Text(
                    error, 
                    color = Color.Red, 
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    val operator = operators.find { it.name == state.operator }
                    if (operator != null && operator.password == password) {
                        onAuthSuccess()
                    } else {
                        error = "Неверный пароль или оператор не выбран"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentOrange),
                shape = MaterialTheme.shapes.medium,
                enabled = state.operator.isNotEmpty() && password.isNotEmpty()
            ) {
                Text(
                    "ВОЙТИ", 
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}
