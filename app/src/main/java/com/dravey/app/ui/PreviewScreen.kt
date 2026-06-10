package com.dravey.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dravey.app.data.SuppressionResult
import com.dravey.app.data.UavTarget
import com.dravey.app.docx.DocxGenerator
import com.dravey.app.docx.ReportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(vm: DraveyViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val targets by vm.targets.collectAsStateWithLifecycle()
    val positionName by vm.positionName.collectAsStateWithLifecycle()
    val region by vm.region.collectAsStateWithLifecycle()
    val dateStart by vm.dateStart.collectAsStateWithLifecycle()
    val dateEnd by vm.dateEnd.collectAsStateWithLifecycle()
    val timeStart by vm.timeStart.collectAsStateWithLifecycle()
    val timeEnd by vm.timeEnd.collectAsStateWithLifecycle()
    val totalAlerts by vm.totalAlerts.collectAsStateWithLifecycle()
    val activations by vm.equipmentActivations.collectAsStateWithLifecycle()
    val dfNoAlert by vm.dfNoAlertFreqs.collectAsStateWithLifecycle()
    val dfAlert by vm.dfAlertFreqs.collectAsStateWithLifecycle()

    var isGenerating by remember { mutableStateOf(false) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val successCount = vm.calcSuccessCount()
    val failedCount = vm.calcFailedCount()
    val flightCount = targets.size - successCount - failedCount
    val workMinutes = vm.calcTotalWorkMinutes()
    val workRange = vm.calcWorkTimeRange()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Перегляд звіту", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.currentScreen.value = Screen.Main }) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    errorMsg?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                    generatedFile?.let { file ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Поділитися звітом"))
                                    } catch (e: Exception) {
                                        errorMsg = "Помилка: ${e.message ?: "невідома помилка"}"
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Поділитися")
                            }
                            Button(
                                onClick = {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        errorMsg = "Немає застосунку для відкриття .docx"
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Відкрити")
                            }
                        }
                    } ?: Button(
                        onClick = {
                            isGenerating = true
                            errorMsg = null
                            scope.launch {
                                try {
                                    val reportData = ReportData(
                                        positionName = positionName,
                                        region = region,
                                        dateStart = dateStart,
                                        dateEnd = dateEnd,
                                        timeStart = timeStart,
                                        timeEnd = timeEnd,
                                        totalAlerts = totalAlerts,
                                        totalDetected = targets.size,
                                        totalSuppressed = successCount,
                                        targets = targets,
                                        workTimeRange = workRange,
                                        workMinutes = workMinutes,
                                        activations = activations,
                                        dfNoAlertFreqs = dfNoAlert,
                                        dfAlertFreqs = dfAlert
                                    )
                                    val file = withContext(Dispatchers.IO) {
                                        DocxGenerator.generate(context, reportData)
                                    }
                                    vm.saveCurrentReport()
                                    generatedFile = file
                                } catch (e: Exception) {
                                    errorMsg = "Помилка генерації: ${e.message ?: "невідома помилка"}"
                                } finally {
                                    isGenerating = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isGenerating
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Генерується...")
                        } else {
                            Icon(Icons.Default.Description, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Згенерувати .docx", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Document header
            ReportCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DocText("Позиція: $positionName ($region)", bold = true)
                    DocText("У період з $timeStart $dateStart по $timeEnd $dateEnd")
                    Spacer(Modifier.height(4.dp))
                    DocText("Кількість тривог по області: $totalAlerts")
                    DocText("Кількість виявлених БпЛА на території області: ${targets.size}")
                    DocText("Кількість виявлених БпЛА, що заходили в область подавлення: $successCount")
                }
            }

            // Table 1: Targets
            Text("Таблиця 1 — Цілі", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                TargetsTable(targets = targets)
            }

            // Table 2: Results
            Text("Таблиця 2 — Результати", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ReportCard {
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        "ЗАГАЛЬНА" to targets.size.toString(),
                        "УСПІШНА" to successCount.toString(),
                        "НЕУСПІШНА" to failedCount.toString(),
                        "ПРОЛІТ" to flightCount.toString()
                    ).forEach { (label, value) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                value,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = when (label) {
                                    "УСПІШНА" -> Color(0xFF4CAF50)
                                    "НЕУСПІШНА" -> Color(0xFFEF5350)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            // Table 3: Work time
            Text("Таблиця 3 — Час роботи", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ReportCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            "Часові проміжки",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(workRange, fontSize = 15.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Тривалість, хв.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$workMinutes",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Table 4: Activations
            Text("Таблиця 4 — Включення", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ReportCard {
                activations.forEach { act ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(act.name, fontSize = 15.sp)
                        Text("${act.count} вкл.", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Table 5: Pelengator
            Text("Таблиця 5 — Пеленгатор", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ReportCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Без тривоги:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dfNoAlert, fontSize = 13.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Під час тривоги:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dfAlert, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun TargetsTable(targets: List<UavTarget>) {
    val borderColor = MaterialTheme.colorScheme.outline
    val headerBg = MaterialTheme.colorScheme.surfaceVariant
    val headers = listOf("ЧАС", "ТИП БпЛА", "ВИС.\nЦІЛІ", "ЕФ.\nВИС.", "ЧАСТОТИ\nПОД.", "ЕФ.\nЧАСТОТИ", "НАПРЯМОК", "РЕЗУЛЬТАТ")
    val colWidths = listOf(80, 140, 70, 70, 110, 90, 90, 130)

    Column(
        modifier = Modifier.border(1.dp, borderColor, RoundedCornerShape(4.dp))
    ) {
        // Header row
        Row(modifier = Modifier.background(headerBg)) {
            headers.forEachIndexed { i, h ->
                Box(
                    modifier = Modifier
                        .width(colWidths[i].dp)
                        .padding(4.dp)
                        .defaultMinSize(minHeight = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        h,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp
                    )
                }
                if (i < headers.size - 1) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(borderColor)
                    )
                }
            }
        }
        HorizontalDivider(color = borderColor)

        if (targets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Немає даних", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        } else {
            targets.forEachIndexed { idx, target ->
                val resultColor = when {
                    target.result.contains("Подавлено") -> Color(0xFF4CAF50)
                    target.result.contains("Неуспішно") -> Color(0xFFEF5350)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                val cells = listOf(
                    target.timeRange,
                    target.uavType.ifBlank { "-" },
                    target.altitudeM.ifBlank { "-" },
                    target.effectiveAltitudeM,
                    target.suppressionFreqs.ifBlank { "-" },
                    target.effectiveFreqs,
                    target.directionChange,
                    target.result
                )
                Row(
                    modifier = Modifier.background(
                        if (idx % 2 == 0) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.background
                    )
                ) {
                    cells.forEachIndexed { i, text ->
                        Box(
                            modifier = Modifier
                                .width(colWidths[i].dp)
                                .padding(4.dp)
                                .defaultMinSize(minHeight = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp,
                                color = if (i == cells.size - 1) resultColor else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (i < cells.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .background(borderColor)
                                    .defaultMinSize(minHeight = 32.dp)
                            )
                        }
                    }
                }
                if (idx < targets.size - 1) {
                    HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun ReportCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
fun DocText(text: String, bold: Boolean = false) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
}
