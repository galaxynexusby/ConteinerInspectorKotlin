package ru.vibe.containerinspector.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.vibe.containerinspector.R

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onFabClick: () -> Unit
) {
    val backgroundColor = colorResource(id = R.color.primary_background)
    val accentColor = colorResource(id = R.color.accent_orange)

    Surface(
        color = backgroundColor,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                icon = Icons.Default.List,
                label = "Очередь",
                isSelected = currentRoute == "dashboard",
                onClick = { onNavigate("dashboard") },
                modifier = Modifier.weight(1f)
            )
            
            NavBarItem(
                icon = Icons.Default.AddBox, // Square '+' icon as requested "non-rounded"
                label = "Сканировать",
                isSelected = false,
                onClick = onFabClick,
                colorOverride = accentColor,
                modifier = Modifier.weight(1f)
            )

            NavBarItem(
                icon = Icons.Default.History,
                label = "Архив",
                isSelected = currentRoute == "history",
                onClick = { onNavigate("history") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colorOverride: Color? = null
) {
    val color = colorOverride ?: if (isSelected) colorResource(id = R.color.accent_orange) else Color.Gray
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(32.dp) // Uniform size
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontSize = 12.sp
            )
        }
    }
}
