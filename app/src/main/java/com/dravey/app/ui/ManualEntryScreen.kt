package com.dravey.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dravey.app.data.SuppressionResult
import com.dravey.app.data.UavTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(vm: DraveyViewModel) {
    val editingTarget by vm.editingTarget.collectAsStateWithLifecycle()
    val editingIndex by vm.editingIndex.collectAsStateWithLifecycle()

    val initial = editingTarget ?: UavTarget()

    var timeStart by remember(initial) { mutableStateOf(initial.timeStart) }
    var timeEnd by remember(initial) { mutableStateOf(initial.timeEnd) }
    var uavType by remember(initial) { mutableStateOf(initial.uavType) }
    var altitudeM by remember(initial) { mutableStateOf(initial.altitudeM) }
    var effectiveAlt by remember(initial) { mutableStateOf(initial.effectiveAltitudeM) }
    var suppressionFreqs by remember(initial) { mutableStateOf(initial.suppressionFreqs) }
    var effectiveFreqs by remember(initial) { mutableStateOf(initial.effectiveFreqs) }
    var directionChange by remember(initial) { mutableStateOf(initial.directionChange) }
    var result by remember(initial) { mutableStateOf(initial.result) }

    val isEdit = editingIndex >= 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Редагувати ціль" else "Нова ціль", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.currentScreen.value = Screen.Main }) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
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
            Text("Час роботи", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = timeStart,
                    onValueChange = { timeStart = it },
                    label = { Text("Початок (ГГ:ХХ)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("21:02") }
                )
                OutlinedTextField(
                    value = timeEnd,
                    onValueChange = { timeEnd = it },
                    label = { Text("Кінець (ГГ:ХХ)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("21:21") }
                )
            }

            DraveyTextField(value = uavType, onValueChange = { uavType = it },
                label = "Тип БпЛА / Ціль", placeholder = "Повітряний противник")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DraveyTextField(value = altitudeM, onValueChange = { altitudeM = it },
                    label = "Висота цілі /м.", placeholder = "400", modifier = Modifier.weight(1f))
                DraveyTextField(value = effectiveAlt, onValueChange = { effectiveAlt = it },
                    label = "Еф. висота под. /м.", placeholder = "-", modifier = Modifier.weight(1f))
            }

            OutlinedTextField(
                value = suppressionFreqs,
                onValueChange = { suppressionFreqs = it },
                label = { Text("Частоти подавлення") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("1176-1602\n1295-1370") },
                minLines = 2,
                maxLines = 6
            )

            DraveyTextField(value = effectiveFreqs, onValueChange = { effectiveFreqs = it },
                label = "Ефективні частоти", placeholder = "-")

            DraveyTextField(value = directionChange, onValueChange = { directionChange = it },
                label = "Зміна напрямку руху", placeholder = "-")

            // Result selection
            Text("Результат подавлення", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            ResultButtons(
                selected = result,
                onSelect = { result = it }
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val target = UavTarget(
                        timeStart = timeStart.trim(),
                        timeEnd = timeEnd.trim(),
                        uavType = uavType.trim().ifBlank { "-" },
                        altitudeM = altitudeM.trim().ifBlank { "-" },
                        effectiveAltitudeM = effectiveAlt.trim().ifBlank { "-" },
                        suppressionFreqs = suppressionFreqs.trim().ifBlank { "-" },
                        effectiveFreqs = effectiveFreqs.trim().ifBlank { "-" },
                        directionChange = directionChange.trim().ifBlank { "-" },
                        result = result.ifBlank { "-" }
                    )
                    vm.saveManualEntry(target)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Зберегти", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ResultButtons(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { onSelect(SuppressionResult.SUPPRESSED.displayUa) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected == SuppressionResult.SUPPRESSED.displayUa)
                    androidx.compose.ui.graphics.Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selected == SuppressionResult.SUPPRESSED.displayUa)
                    androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(SuppressionResult.SUPPRESSED.displayUa, fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = { onSelect(SuppressionResult.FAILED.displayUa) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected == SuppressionResult.FAILED.displayUa)
                    androidx.compose.ui.graphics.Color(0xFFC62828) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selected == SuppressionResult.FAILED.displayUa)
                    androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Cancel, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(SuppressionResult.FAILED.displayUa, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun DraveyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    )
}
