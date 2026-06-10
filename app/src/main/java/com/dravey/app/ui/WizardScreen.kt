package com.dravey.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dravey.app.data.SuppressionResult

@Composable
fun WizardScreen(vm: DraveyViewModel) {
    val context = LocalContext.current
    val state by vm.wizardState.collectAsStateWithLifecycle()
    val isListening by vm.isListening.collectAsStateWithLifecycle()
    val partialText by vm.partialText.collectAsStateWithLifecycle()
    val voiceLang by vm.voiceLanguage.collectAsStateWithLifecycle()
    val targets by vm.targets.collectAsStateWithLifecycle()

    val isUk = voiceLang.startsWith("uk")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                vm.stopWizardListening()
                vm.currentScreen.value = Screen.Main
            }) {
                Icon(Icons.Default.ArrowBack, "Назад")
            }
            Text(
                if (isUk) "Ціль #${state.droneNumber}" else "Цель #${state.droneNumber}",
                fontWeight = FontWeight.Bold, fontSize = 20.sp
            )
            TextButton(onClick = {
                vm.setVoiceLanguage(if (isUk) "ru-RU" else "uk-UA")
            }) {
                Text(if (isUk) "UA" else "RU", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Progress steps
        StepProgressBar(currentStep = state.currentStep)

        Spacer(Modifier.height(24.dp))

        // RESULT step — show buttons instead of voice
        if (state.currentStep == WizardStep.RESULT) {
            ResultStep(vm = vm, isUk = isUk)
        } else {
            // Current question card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Питання ${state.currentStep.index + 1} / ${WizardStep.RESULT.index}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (isUk) state.currentStep.questionUa else state.currentStep.questionRu,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Partial text / countdown
                    AnimatedVisibility(visible = isListening) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (partialText.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        partialText,
                                        modifier = Modifier.padding(10.dp),
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            if (state.isCountingDown) {
                                Text(
                                    if (isUk) "Тиша: ${state.countdown} сек" else "Тишина: ${state.countdown} сек",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Microphone button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListening) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }

                    if (isListening) {
                        Text(
                            if (isUk) "Говоріть..." else "Говорите...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { vm.skipCurrentStep() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isUk) "Пропустити" else "Пропустить")
                }
                if (isListening) {
                    Button(
                        onClick = { vm.stopWizardListening() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Стоп")
                    }
                }
            }

            // Collected data so far
            if (state.currentTarget.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    if (isUk) "Зібрані дані:" else "Собранные данные:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                state.currentTarget.entries.sortedBy { it.key.index }.forEach { (step, value) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isUk) step.questionUa.take(25) else step.questionRu.take(25),
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Previously saved targets count
        if (targets.isNotEmpty()) {
            Text(
                "Збережено цілей: ${targets.size}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ResultStep(vm: DraveyViewModel, isUk: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            if (isUk) "Результат подавлення" else "Результат подавления",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            if (isUk) "Оберіть результат:" else "Выберите результат:",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Big result buttons
        Button(
            onClick = { vm.setWizardResult(SuppressionResult.SUPPRESSED.displayUa) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                SuppressionResult.SUPPRESSED.displayUa,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Button(
            onClick = { vm.setWizardResult(SuppressionResult.FAILED.displayUa) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Cancel, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                SuppressionResult.FAILED.displayUa,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = { vm.setWizardResult("-") },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isUk) "Невідомо / пропустити" else "Неизвестно / пропустить")
        }

        Spacer(Modifier.height(16.dp))

        // After saving — add new drone button
        val state by vm.wizardState.collectAsStateWithLifecycle()
        if (!state.isActive) {
            val context = LocalContext.current
            Divider()
            Text(
                if (isUk) "Ціль збережена!" else "Цель сохранена!",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { vm.currentScreen.value = Screen.Main },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isUk) "Повернутись" else "Вернуться")
                }
                Button(
                    onClick = { vm.addNewDroneInWizard(context) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isUk) "+ Нова ціль" else "+ Новая цель")
                }
            }
        }
    }
}

@Composable
fun StepProgressBar(currentStep: WizardStep) {
    val steps = WizardStep.values()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        steps.forEach { step ->
            val isCompleted = step.index < currentStep.index
            val isCurrent = step.index == currentStep.index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            isCompleted -> MaterialTheme.colorScheme.secondary
                            isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
            )
        }
    }
}
