package com.dravey.app.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dravey.app.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Steps in the wizard
enum class WizardStep(val index: Int, val questionUa: String, val questionRu: String) {
    TIME_START(0, "Вкажіть час початку роботи", "Укажите время начала работы"),
    TIME_END(1, "Вкажіть час закінчення роботи", "Укажите время окончания работы"),
    UAV_TYPE(2, "Тип БпЛА або ціль", "Тип БПЛА или цель"),
    ALTITUDE(3, "Висота цілі в метрах", "Высота цели в метрах"),
    EFF_ALTITUDE(4, "Ефективна висота подавлення в метрах", "Эффективная высота подавления в метрах"),
    SUPPRESSION_FREQS(5, "Частоти подавлення", "Частоты подавления"),
    EFF_FREQS(6, "Ефективні частоти", "Эффективные частоты"),
    DIRECTION_CHANGE(7, "Зміна напрямку руху", "Изменение направления движения"),
    RESULT(8, "Результат подавлення", "Результат подавления");

    companion object {
        fun fromIndex(i: Int) = values().firstOrNull { it.index == i }
    }
}

sealed class Screen {
    object Main : Screen()
    object Wizard : Screen()
    object ManualEntry : Screen()
    object Preview : Screen()
    object History : Screen()
    object Settings : Screen()
}

data class WizardState(
    val isActive: Boolean = false,
    val currentStep: WizardStep = WizardStep.TIME_START,
    val currentTarget: MutableMap<WizardStep, String> = mutableMapOf(),
    val droneNumber: Int = 1,
    val countdown: Int = 10,
    val isCountingDown: Boolean = false
)

class DraveyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = ReportRepository(db.reportDao())
    val prefs = PreferencesManager(application)

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val targetsType = Types.newParameterizedType(List::class.java, UavTarget::class.java)
    private val activationsType = Types.newParameterizedType(List::class.java, EquipmentActivation::class.java)
    private val targetsAdapter = moshi.adapter<List<UavTarget>>(targetsType)
    private val activationsAdapter = moshi.adapter<List<EquipmentActivation>>(activationsType)

    // Navigation
    val currentScreen = MutableStateFlow<Screen>(Screen.Main)

    // Report header fields
    val positionName = MutableStateFlow(prefs.positionTemplate.positionName)
    val region = MutableStateFlow(prefs.positionTemplate.region)
    val dateStart = MutableStateFlow("")
    val dateEnd = MutableStateFlow("")
    val timeStart = MutableStateFlow("06:00")
    val timeEnd = MutableStateFlow("06:00")
    val totalAlerts = MutableStateFlow(0)
    val totalDetected = MutableStateFlow(0)
    val totalSuppressed = MutableStateFlow(0)

    // Targets list
    val targets = MutableStateFlow<List<UavTarget>>(emptyList())

    // Equipment
    val equipmentActivations = MutableStateFlow(listOf(EquipmentActivation(prefs.positionTemplate.equipmentName, 0)))
    val dfNoAlertFreqs = MutableStateFlow("-")
    val dfAlertFreqs = MutableStateFlow("-")

    // Wizard state
    val wizardState = MutableStateFlow(WizardState())

    // Voice/UI state
    val isListening = MutableStateFlow(false)
    val partialText = MutableStateFlow("")
    val errorMessage = MutableStateFlow<String?>(null)
    val successMessage = MutableStateFlow<String?>(null)
    val voiceLanguage = MutableStateFlow(prefs.voiceLanguage)

    // Manual entry state (for add/edit)
    val editingTarget = MutableStateFlow<UavTarget?>(null)
    val editingIndex = MutableStateFlow<Int>(-1)

    // History
    val savedReports = MutableStateFlow<List<ReportEntity>>(emptyList())

    // Draft autosave
    private var autosaveJob: Job? = null

    // Speech
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var countdownJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        val df = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val today = Date()
        val yesterday = Date(today.time - 86400_000L)
        dateStart.value = df.format(yesterday)
        dateEnd.value = df.format(today)

        viewModelScope.launch(Dispatchers.IO) {
            repository.allReports.collect { savedReports.value = it }
        }

        initTts(application)
        startAutosave()
    }

    private fun initTts(context: Context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val lang = if (voiceLanguage.value.startsWith("uk")) Locale("uk", "UA") else Locale("ru", "RU")
                tts?.language = lang
            }
        }
    }

    private fun initSpeech(context: Context) {
        mainHandler.post {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(p: Bundle?) {
                            isListening.value = true
                            partialText.value = ""
                            startCountdown()
                        }
                        override fun onBeginningOfSpeech() { cancelCountdown() }
                        override fun onRmsChanged(v: Float) {}
                        override fun onBufferReceived(b: ByteArray?) {}
                        override fun onEndOfSpeech() { isListening.value = false }
                        override fun onPartialResults(r: Bundle?) {
                            val matches = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) partialText.value = matches[0]
                            cancelCountdown()
                        }
                        override fun onResults(r: Bundle?) {
                            isListening.value = false
                            cancelCountdown()
                            val matches = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = if (!matches.isNullOrEmpty()) matches[0] else ""
                            onVoiceResult(text)
                        }
                        override fun onError(error: Int) {
                            isListening.value = false
                            when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH,
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                    // Treat silence as skip
                                    onVoiceResult("")
                                }
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                    errorMessage.value = "Немає дозволу на мікрофон"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                                    speechRecognizer?.cancel()
                                    viewModelScope.launch {
                                        delay(500)
                                        startListeningStep()
                                    }
                                }
                                else -> errorMessage.value = "Помилка розпізнавання: $error"
                            }
                        }
                        override fun onEvent(e: Int, p: Bundle?) {}
                    })
                }
            } else {
                errorMessage.value = "Голосовий ввід недоступний. Використовуйте ручний ввід."
            }
        }
    }

    // ---- WIZARD ----

    fun startWizard(context: Context) {
        initSpeech(context)
        val state = WizardState(
            isActive = true,
            currentStep = WizardStep.TIME_START,
            currentTarget = mutableMapOf(),
            droneNumber = targets.value.size + 1
        )
        wizardState.value = state
        currentScreen.value = Screen.Wizard
        viewModelScope.launch {
            delay(600)
            speakStep(state.currentStep)
            delay(1200)
            startListeningStep()
        }
    }

    fun addNewDroneInWizard(context: Context) {
        val state = WizardState(
            isActive = true,
            currentStep = WizardStep.TIME_START,
            currentTarget = mutableMapOf(),
            droneNumber = targets.value.size + 1
        )
        wizardState.value = state
        viewModelScope.launch {
            delay(400)
            speakStep(state.currentStep)
            delay(1200)
            startListeningStep()
        }
    }

    fun stopWizardListening() {
        mainHandler.post { speechRecognizer?.stopListening() }
        isListening.value = false
        cancelCountdown()
    }

    fun skipCurrentStep() {
        onVoiceResult("")
    }

    private fun onVoiceResult(text: String) {
        val state = wizardState.value
        val step = state.currentStep
        val value = text.trim().ifBlank { "-" }

        val newMap = state.currentTarget.toMutableMap()
        newMap[step] = value
        partialText.value = ""

        val nextIndex = step.index + 1
        val nextStep = WizardStep.fromIndex(nextIndex)

        if (nextStep == null || nextStep == WizardStep.RESULT) {
            // RESULT step handled via buttons, save what we have and wait
            wizardState.value = state.copy(currentTarget = newMap, currentStep = WizardStep.RESULT)
        } else {
            wizardState.value = state.copy(currentTarget = newMap, currentStep = nextStep)
            viewModelScope.launch {
                delay(300)
                speakStep(nextStep)
                delay(1000)
                startListeningStep()
            }
        }
    }

    fun setWizardResult(result: String) {
        val state = wizardState.value
        val newMap = state.currentTarget.toMutableMap()
        newMap[WizardStep.RESULT] = result

        val target = buildTargetFromMap(newMap)
        targets.value = targets.value + target
        totalDetected.value = targets.value.size
        totalSuppressed.value = targets.value.count { it.result == SuppressionResult.SUPPRESSED.displayUa }

        val savedNumber = targets.value.size
        wizardState.value = WizardState(isActive = false, droneNumber = savedNumber + 1)
        successMessage.value = "Ціль #$savedNumber збережено!"
    }

    private fun buildTargetFromMap(map: Map<WizardStep, String>): UavTarget {
        return UavTarget(
            timeStart = map[WizardStep.TIME_START]?.takeIf { it != "-" } ?: "",
            timeEnd = map[WizardStep.TIME_END]?.takeIf { it != "-" } ?: "",
            uavType = map[WizardStep.UAV_TYPE]?.takeIf { it != "-" } ?: "",
            altitudeM = map[WizardStep.ALTITUDE]?.takeIf { it != "-" } ?: "",
            effectiveAltitudeM = map[WizardStep.EFF_ALTITUDE] ?: "-",
            suppressionFreqs = map[WizardStep.SUPPRESSION_FREQS]?.takeIf { it != "-" } ?: "",
            effectiveFreqs = map[WizardStep.EFF_FREQS] ?: "-",
            directionChange = map[WizardStep.DIRECTION_CHANGE] ?: "-",
            result = map[WizardStep.RESULT] ?: SuppressionResult.UNKNOWN.displayUa
        )
    }

    private fun speakStep(step: WizardStep) {
        val isUk = voiceLanguage.value.startsWith("uk")
        val text = if (isUk) step.questionUa else step.questionRu
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "step_${step.index}")
    }

    private fun startListeningStep() {
        val recognizer = speechRecognizer ?: return
        val lang = voiceLanguage.value
        mainHandler.post {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            }
            try { recognizer.startListening(intent) }
            catch (e: Exception) { Log.e("DRAVEY", "startListening error", e) }
        }
    }

    private fun startCountdown() {
        cancelCountdown()
        val state = wizardState.value
        wizardState.value = state.copy(countdown = 10, isCountingDown = true)
        countdownJob = viewModelScope.launch {
            for (i in 9 downTo 0) {
                delay(1000)
                wizardState.value = wizardState.value.copy(countdown = i)
            }
            wizardState.value = wizardState.value.copy(isCountingDown = false)
            mainHandler.post { speechRecognizer?.stopListening() }
            onVoiceResult("")
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        wizardState.value = wizardState.value.copy(isCountingDown = false, countdown = 10)
    }

    // ---- MANUAL ENTRY ----

    fun openManualEntry(target: UavTarget? = null, index: Int = -1) {
        editingTarget.value = target ?: UavTarget()
        editingIndex.value = index
        currentScreen.value = Screen.ManualEntry
    }

    fun saveManualEntry(target: UavTarget) {
        val list = targets.value.toMutableList()
        val idx = editingIndex.value
        if (idx >= 0 && idx < list.size) {
            list[idx] = target
        } else {
            list.add(target)
        }
        targets.value = list
        totalDetected.value = list.size
        totalSuppressed.value = list.count { it.result == SuppressionResult.SUPPRESSED.displayUa }
        editingTarget.value = null
        editingIndex.value = -1
        currentScreen.value = Screen.Main
    }

    fun deleteTarget(index: Int) {
        val list = targets.value.toMutableList()
        if (index in list.indices) list.removeAt(index)
        targets.value = list
        totalDetected.value = list.size
        totalSuppressed.value = list.count { it.result == SuppressionResult.SUPPRESSED.displayUa }
    }

    // ---- AUTO-CALCULATIONS ----

    fun calcTotalWorkMinutes(): Int = targets.value.sumOf { it.durationMinutes }

    fun calcWorkTimeRange(): String {
        val tgts = targets.value.filter { it.timeStart.isNotBlank() }
        if (tgts.isEmpty()) return "з ${timeStart.value} до ${timeEnd.value}"
        val starts = tgts.mapNotNull { parseMinutes(it.timeStart) }
        val ends = tgts.mapNotNull { parseMinutes(it.timeEnd) }
        val baseStart = parseMinutes(timeStart.value)
        val baseEnd = parseMinutes(timeEnd.value)
        val allStarts = if (baseStart != null) starts + baseStart else starts
        val allEnds = if (baseEnd != null) ends + baseEnd else ends
        val minStart = allStarts.minOrNull() ?: return "з ${timeStart.value} до ${timeEnd.value}"
        val maxEnd = allEnds.maxOrNull() ?: return "з ${timeStart.value} до ${timeEnd.value}"
        return "з ${minutesToTime(minStart)} до ${minutesToTime(maxEnd)}"
    }

    fun calcSuccessCount(): Int = targets.value.count { it.result == SuppressionResult.SUPPRESSED.displayUa }
    fun calcFailedCount(): Int = targets.value.count { it.result == SuppressionResult.FAILED.displayUa }
    fun calcFlightCount(): Int = targets.value.count {
        it.result != SuppressionResult.SUPPRESSED.displayUa &&
        it.result != SuppressionResult.FAILED.displayUa &&
        it.result != "-"
    }

    private fun parseMinutes(time: String): Int? = try {
        val parts = time.trim().split(":")
        parts[0].toInt() * 60 + parts[1].toInt()
    } catch (e: Exception) { null }

    private fun minutesToTime(minutes: Int): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
        return "%02d:%02d".format(h, m)
    }

    // ---- SAVE / GENERATE ----

    fun saveCurrentReport() {
        viewModelScope.launch(Dispatchers.IO) {
            try { repository.save(buildReportEntity()) }
            catch (e: Exception) { Log.e("DRAVEY", "saveCurrentReport failed", e) }
        }
    }

    fun buildReportEntity(): ReportEntity {
        return ReportEntity(
            positionName = positionName.value,
            region = region.value,
            dateStart = dateStart.value,
            dateEnd = dateEnd.value,
            timeStart = timeStart.value,
            timeEnd = timeEnd.value,
            totalAlerts = totalAlerts.value,
            totalDetected = targets.value.size,
            totalSuppressed = calcSuccessCount(),
            targetsJson = targetsAdapter.toJson(targets.value) ?: "[]",
            activationsJson = activationsAdapter.toJson(equipmentActivations.value) ?: "[]",
            dfNoAlertFreqs = dfNoAlertFreqs.value,
            dfAlertFreqs = dfAlertFreqs.value
        )
    }

    fun loadReport(entity: ReportEntity) {
        positionName.value = entity.positionName
        region.value = entity.region
        dateStart.value = entity.dateStart
        dateEnd.value = entity.dateEnd
        timeStart.value = entity.timeStart
        timeEnd.value = entity.timeEnd
        totalAlerts.value = entity.totalAlerts
        dfNoAlertFreqs.value = entity.dfNoAlertFreqs
        dfAlertFreqs.value = entity.dfAlertFreqs
        targets.value = try { targetsAdapter.fromJson(entity.targetsJson) ?: emptyList() } catch (e: Exception) { emptyList() }
        equipmentActivations.value = try { activationsAdapter.fromJson(entity.activationsJson) ?: listOf(EquipmentActivation()) } catch (e: Exception) { listOf(EquipmentActivation()) }
        totalDetected.value = targets.value.size
        totalSuppressed.value = calcSuccessCount()
        currentScreen.value = Screen.Main
    }

    fun deleteReport(entity: ReportEntity) {
        viewModelScope.launch { repository.delete(entity) }
    }

    fun clearCurrentReport() {
        val tmpl = prefs.positionTemplate
        positionName.value = tmpl.positionName
        region.value = tmpl.region
        targets.value = emptyList()
        totalAlerts.value = 0
        totalDetected.value = 0
        totalSuppressed.value = 0
        equipmentActivations.value = listOf(EquipmentActivation(tmpl.equipmentName, 0))
        dfNoAlertFreqs.value = "-"
        dfAlertFreqs.value = "-"
        errorMessage.value = null
        successMessage.value = null
    }

    fun setVoiceLanguage(lang: String) {
        voiceLanguage.value = lang
        prefs.voiceLanguage = lang
        tts?.language = if (lang.startsWith("uk")) Locale("uk", "UA") else Locale("ru", "RU")
    }

    fun savePositionTemplate() {
        prefs.positionTemplate = PositionTemplate(
            positionName = positionName.value,
            region = region.value,
            equipmentName = equipmentActivations.value.firstOrNull()?.name ?: "Дамба"
        )
        successMessage.value = "Шаблон позиції збережено"
    }

    private fun startAutosave() {
        autosaveJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                if (targets.value.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        val draftPrefix = if (voiceLanguage.value.startsWith("uk")) "[Чернетка]" else "[Черновик]"
                        try { repository.save(buildReportEntity().copy(positionName = "$draftPrefix ${positionName.value}")) }
                        catch (e: Exception) { Log.e("DRAVEY", "Autosave failed", e) }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
        tts?.shutdown()
        autosaveJob?.cancel()
    }
}
