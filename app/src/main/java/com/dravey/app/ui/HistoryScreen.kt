package com.dravey.app.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dravey.app.data.ReportEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: DraveyViewModel) {
    val reports by vm.savedReports.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Історія звітів", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.currentScreen.value = Screen.Main }) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Збережених звітів немає", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reports) { report ->
                    HistoryCard(
                        report = report,
                        onLoad = { vm.loadReport(report) },
                        onDelete = { vm.deleteReport(report) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(report: ReportEntity, onLoad: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dateStr = remember(report.createdAt) {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(report.createdAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    report.positionName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    dateStr,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Цілей: ${report.totalDetected}  |  Подавлено: ${report.totalSuppressed}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (report.dateStart.isNotBlank()) {
                    Text(
                        "${report.dateStart} — ${report.dateEnd}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onLoad) {
                    Icon(Icons.Default.Restore, "Завантажити", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, "Видалити", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Видалити звіт?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Видалити", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}
