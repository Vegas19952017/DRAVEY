package com.dravey.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: DraveyViewModel) {
    val positionName by vm.positionName.collectAsStateWithLifecycle()
    val region by vm.region.collectAsStateWithLifecycle()
    val voiceLang by vm.voiceLanguage.collectAsStateWithLifecycle()
    val activations by vm.equipmentActivations.collectAsStateWithLifecycle()
    val totalAlerts by vm.totalAlerts.collectAsStateWithLifecycle()
    val timeStart by vm.timeStart.collectAsStateWithLifecycle()
    val timeEnd by vm.timeEnd.collectAsStateWithLifecycle()
    val dateStart by vm.dateStart.collectAsStateWithLifecycle()
    val dateEnd by vm.dateEnd.collectAsStateWithLifecycle()
    val dfNoAlert by vm.dfNoAlertFreqs.collectAsStateWithLifecycle()
    val dfAlert by vm.dfAlertFreqs.collectAsStateWithLifecycle()
    val successMsg by vm.successMessage.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Налаштування", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            successMsg?.let {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        it,
                        modifier = Modifier.padding(10.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Voice language
            SettingsSection(title = "Мова голосового вводу") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = voiceLang.startsWith("uk"),
                        onClick = { vm.setVoiceLanguage("uk-UA") },
                        label = { Text("Українська") }
                    )
                    FilterChip(
                        selected = voiceLang.startsWith("ru"),
                        onClick = { vm.setVoiceLanguage("ru-RU") },
                        label = { Text("Русский") }
                    )
                }
            }

            // Position template
            SettingsSection(title = "Шаблон позиції") {
                OutlinedTextField(
                    value = positionName,
                    onValueChange = { vm.positionName.value = it },
                    label = { Text("Назва позиції") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = region,
                    onValueChange = { vm.region.value = it },
                    label = { Text("Область") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                val equipName = activations.firstOrNull()?.name ?: ""
                OutlinedTextField(
                    value = equipName,
                    onValueChange = { name ->
                        val list = activations.toMutableList()
                        if (list.isEmpty()) {
                            list.add(com.dravey.app.data.EquipmentActivation(name, 0))
                        } else {
                            list[0] = list[0].copy(name = name)
                        }
                        vm.equipmentActivations.value = list
                    },
                    label = { Text("Назва засобу (для Таблиці 4)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = { vm.savePositionTemplate() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Зберегти як шаблон")
                }
            }

            // Report period
            SettingsSection(title = "Період звіту") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateStart,
                        onValueChange = { vm.dateStart.value = it },
                        label = { Text("Дата початку") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("05.06.2026") }
                    )
                    OutlinedTextField(
                        value = dateEnd,
                        onValueChange = { vm.dateEnd.value = it },
                        label = { Text("Дата кінця") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("06.06.2026") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = timeStart,
                        onValueChange = { vm.timeStart.value = it },
                        label = { Text("Час початку") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("06:00") }
                    )
                    OutlinedTextField(
                        value = timeEnd,
                        onValueChange = { vm.timeEnd.value = it },
                        label = { Text("Час кінця") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("06:00") }
                    )
                }
                OutlinedTextField(
                    value = totalAlerts.toString(),
                    onValueChange = { vm.totalAlerts.value = it.toIntOrNull() ?: 0 },
                    label = { Text("Кількість тривог по області") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Equipment activations
            SettingsSection(title = "Включення засобу") {
                activations.forEachIndexed { idx, act ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = act.count.toString(),
                            onValueChange = { str ->
                                val list = activations.toMutableList()
                                list[idx] = list[idx].copy(count = str.toIntOrNull() ?: 0)
                                vm.equipmentActivations.value = list
                            },
                            label = { Text("Включень (${act.name})") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // DF / Pelengator
            SettingsSection(title = "Пеленгатор") {
                OutlinedTextField(
                    value = dfNoAlert,
                    onValueChange = { vm.dfNoAlertFreqs.value = it },
                    label = { Text("Без тривоги") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dfAlert,
                    onValueChange = { vm.dfAlertFreqs.value = it },
                    label = { Text("Під час тривоги") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}
