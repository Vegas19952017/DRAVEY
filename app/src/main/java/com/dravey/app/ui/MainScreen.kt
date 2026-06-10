package com.dravey.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dravey.app.data.UavTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: DraveyViewModel) {
    val context = LocalContext.current
    val targets by vm.targets.collectAsStateWithLifecycle()
    val positionName by vm.positionName.collectAsStateWithLifecycle()
    val region by vm.region.collectAsStateWithLifecycle()
    val totalAlerts by vm.totalAlerts.collectAsStateWithLifecycle()
    val successMsg by vm.successMessage.collectAsStateWithLifecycle()
    val errorMsg by vm.errorMessage.collectAsStateWithLifecycle()
    val voiceLang by vm.voiceLanguage.collectAsStateWithLifecycle()

    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(successMsg) {
        if (successMsg != null) {
            kotlinx.coroutines.delay(3000)
            vm.successMessage.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DRAVEY", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(positionName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    // Language toggle
                    TextButton(onClick = {
                        vm.setVoiceLanguage(if (voiceLang.startsWith("uk")) "ru-RU" else "uk-UA")
                    }) {
                        Text(
                            if (voiceLang.startsWith("uk")) "UA" else "RU",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { vm.currentScreen.value = Screen.History }) {
                        Icon(Icons.Default.History, "Історія")
                    }
                    IconButton(onClick = { vm.currentScreen.value = Screen.Settings }) {
                        Icon(Icons.Default.Settings, "Налаштування")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Add manually
                SmallFloatingActionButton(
                    onClick = { vm.openManualEntry() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Add, "Додати вручну")
                }
                // Start voice wizard
                FloatingActionButton(
                    onClick = { vm.startWizard(context) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Mic, "Голосовий ввід")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Messages
            successMsg?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            errorMsg?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            // Header info card
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Тривог по області:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalAlerts", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Виявлено БпЛА:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${targets.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Подавлено:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${vm.calcSuccessCount()}", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Час роботи:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${vm.calcTotalWorkMinutes()} хв", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Targets section header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Цілі (${targets.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (targets.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { vm.currentScreen.value = Screen.Preview },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Перегляд", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Очистити", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (targets.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.FlightLand, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Цілі не додані", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                        Text("Натисніть 🎤 для голосового вводу\nабо + для ручного",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                            textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(targets) { index, target ->
                        TargetCard(
                            target = target,
                            index = index,
                            onEdit = { vm.openManualEntry(target, index) },
                            onDelete = { vm.deleteTarget(index) }
                        )
                    }
                }
            }

            // Bottom action bar
            if (targets.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { vm.currentScreen.value = Screen.Preview },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Visibility, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Перегляд")
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Очистити звіт?") },
            text = { Text("Всі цілі будуть видалені. Продовжити?") },
            confirmButton = {
                TextButton(onClick = { vm.clearCurrentReport(); showClearConfirm = false }) {
                    Text("Очистити", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Скасувати") }
            }
        )
    }
}

@Composable
fun TargetCard(target: UavTarget, index: Int, onEdit: () -> Unit, onDelete: () -> Unit) {
    val resultColor = when {
        target.result.contains("Подавлено") -> Color(0xFF4CAF50)
        target.result.contains("Неуспішно") -> Color(0xFFEF5350)
        else -> Color(0xFF90A4AE)
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            // Number badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("${index + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(target.timeRange.ifBlank { "-" }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Surface(
                        color = resultColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            target.result.take(20),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = resultColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(target.uavType.ifBlank { "-" }, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (target.altitudeM.isNotBlank()) {
                    Text("Висота: ${target.altitudeM} м", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (target.suppressionFreqs.isNotBlank()) {
                    Text("Частоти: ${target.suppressionFreqs.replace("\n", ", ")}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Редагувати", modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Видалити", modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
