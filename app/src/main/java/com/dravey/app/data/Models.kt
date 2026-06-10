package com.dravey.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

// Result options for UAV suppression
enum class SuppressionResult(val displayUa: String) {
    SUPPRESSED("Подавлено/локаційно втрачено"),
    FAILED("Неуспішно/проліт"),
    UNKNOWN("-")
}

@JsonClass(generateAdapter = true)
data class UavTarget(
    val timeStart: String = "",       // e.g. "21:02"
    val timeEnd: String = "",         // e.g. "21:21"
    val uavType: String = "",         // e.g. "Повітряний противник"
    val altitudeM: String = "",       // e.g. "400"
    val effectiveAltitudeM: String = "-",
    val suppressionFreqs: String = "",  // multiline e.g. "1176-1602\n1295-1370"
    val effectiveFreqs: String = "-",
    val directionChange: String = "-",
    val result: String = SuppressionResult.UNKNOWN.displayUa
) {
    val timeRange: String get() = if (timeStart.isNotBlank() && timeEnd.isNotBlank()) "$timeStart-$timeEnd" else timeStart.ifBlank { "-" }
    val durationMinutes: Int get() {
        return try {
            val (sh, sm) = timeStart.split(":").map { it.trim().toInt() }
            val (eh, em) = timeEnd.split(":").map { it.trim().toInt() }
            val startTotal = sh * 60 + sm
            val endTotal = eh * 60 + em
            if (endTotal >= startTotal) endTotal - startTotal else (endTotal + 1440) - startTotal
        } catch (e: Exception) { 0 }
    }
}

@JsonClass(generateAdapter = true)
data class EquipmentActivation(
    val name: String = "Дамба",
    val count: Int = 0
)

@JsonClass(generateAdapter = true)
data class PositionTemplate(
    val positionName: String = "ДРАВЕЙ/Дамба",
    val region: String = "Кіровоградська область",
    val equipmentName: String = "Дамба"
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val positionName: String = "ДРАВЕЙ/Дамба",
    val region: String = "Кіровоградська область",
    val dateStart: String = "",
    val dateEnd: String = "",
    val timeStart: String = "06:00",
    val timeEnd: String = "06:00",
    val totalAlerts: Int = 0,
    val totalDetected: Int = 0,
    val totalSuppressed: Int = 0,
    val targetsJson: String = "[]",
    val activationsJson: String = "[]",
    val dfNoAlertFreqs: String = "-",
    val dfAlertFreqs: String = "-",
    val createdAt: Long = System.currentTimeMillis()
)
