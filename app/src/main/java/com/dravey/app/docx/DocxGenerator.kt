package com.dravey.app.docx

import android.content.Context
import android.os.Environment
import com.dravey.app.data.EquipmentActivation
import com.dravey.app.data.SuppressionResult
import com.dravey.app.data.UavTarget
import org.apache.poi.xwpf.usermodel.*
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger

data class ReportData(
    val positionName: String,
    val region: String,
    val dateStart: String,
    val dateEnd: String,
    val timeStart: String,
    val timeEnd: String,
    val totalAlerts: Int,
    val totalDetected: Int,
    val totalSuppressed: Int,
    val targets: List<UavTarget>,
    val workTimeRange: String,
    val workMinutes: Int,
    val activations: List<EquipmentActivation>,
    val dfNoAlertFreqs: String,
    val dfAlertFreqs: String
)

object DocxGenerator {

    private const val FONT_NAME = "Times New Roman"
    private const val FONT_SIZE_HP = 28 // half-points = 14pt

    fun generate(context: Context, data: ReportData): File {
        val doc = XWPFDocument()

        // Header paragraphs
        addParagraph(doc, "Позиція: ${data.positionName} (${data.region})")
        addParagraph(doc, "У період з ${data.timeStart} ${data.dateStart} по ${data.timeEnd} ${data.dateEnd}")
        addParagraph(doc, "")
        addParagraph(doc, "Кількість тривог по області: ${data.totalAlerts}")
        addParagraph(doc, "Кількість виявлених БпЛА на території області: ${data.totalDetected}")
        addParagraph(doc, "Кількість виявлених БпЛА, що заходили в область подавлення: ${data.totalSuppressed}")
        addParagraph(doc, "")

        // Table 1: UAV targets
        val table1 = doc.createTable(1 + data.targets.size, 8)
        setTableWidth(table1, 15139)
        val colWidths1 = intArrayOf(950, 1701, 1559, 2268, 2268, 1988, 2125, 2280)

        val headers1 = arrayOf(
            "ЧАС",
            "ТИП БпЛА",
            "ВИСОТА ЦІЛІ /м.",
            "ЕФЕКТИВНА ВИСОТА ПОДАВЛЕННЯ/ м.",
            "ЧАСТОТИ ПОДАВЛЕННЯ",
            "ЕФЕКТИВНІ ЧАСТОТИ",
            "ЗМІНА НАПРЯМКУ РУХУ",
            "РЕЗУЛЬТАТ ПОДАВЛЕННЯ"
        )
        val headerRow = table1.getRow(0)
        for (i in headers1.indices) {
            val cell = headerRow.getCell(i)
            setCellWidth(cell, colWidths1[i])
            setCellText(cell, headers1[i], bold = true, centered = true)
        }

        for ((rowIdx, target) in data.targets.withIndex()) {
            val row = table1.getRow(rowIdx + 1)
            val cells = listOf(
                target.timeRange,
                target.uavType,
                target.altitudeM.ifBlank { "-" },
                target.effectiveAltitudeM,
                target.suppressionFreqs.ifBlank { "-" },
                target.effectiveFreqs,
                target.directionChange,
                target.result
            )
            for (i in cells.indices) {
                val cell = row.getCell(i)
                setCellWidth(cell, colWidths1[i])
                setCellMultilineText(cell, cells[i], bold = false, centered = true)
            }
        }

        addParagraph(doc, "")
        addParagraph(doc, "Результати подавлення:")

        // Table 2: Results summary
        val successCount = data.targets.count { it.result == SuppressionResult.SUPPRESSED.displayUa }
        val failedCount = data.targets.count { it.result == SuppressionResult.FAILED.displayUa }
        val flightCount = data.targets.size - successCount - failedCount

        val table2 = doc.createTable(2, 4)
        setTableWidth(table2, 8364)
        val colWidths2 = intArrayOf(2091, 2091, 2091, 2090)
        val headers2 = arrayOf("ЗАГАЛЬНА", "УСПІШНА", "НЕУСПІШНА", "ПРОЛІТ")
        val values2 = arrayOf(
            data.targets.size.toString(),
            successCount.toString(),
            failedCount.toString(),
            flightCount.toString()
        )
        for (i in 0..3) {
            val cell = table2.getRow(0).getCell(i)
            setCellWidth(cell, colWidths2[i])
            setCellText(cell, headers2[i], bold = true, centered = true)
        }
        for (i in 0..3) {
            val cell = table2.getRow(1).getCell(i)
            setCellWidth(cell, colWidths2[i])
            setCellText(cell, values2[i], bold = false, centered = true)
        }

        addParagraph(doc, "")
        addParagraph(doc, "Загальна тривалість часу роботи засобу за період:::")

        // Table 3: Work time
        val table3 = doc.createTable(2, 2)
        setTableWidth(table3, 8501)
        val colWidths3 = intArrayOf(3246, 5254)
        setCellWidth(table3.getRow(0).getCell(0), colWidths3[0])
        setCellText(table3.getRow(0).getCell(0), "Часові проміжки", bold = true, centered = true)
        setCellWidth(table3.getRow(0).getCell(1), colWidths3[1])
        setCellText(table3.getRow(0).getCell(1), "Загальна тривалість роботи засобу, хв.", bold = true, centered = true)
        setCellWidth(table3.getRow(1).getCell(0), colWidths3[0])
        setCellText(table3.getRow(1).getCell(0), data.workTimeRange, bold = false, centered = true)
        setCellWidth(table3.getRow(1).getCell(1), colWidths3[1])
        setCellText(table3.getRow(1).getCell(1), data.workMinutes.toString(), bold = false, centered = true)

        addParagraph(doc, "")
        addParagraph(doc, "Кількість включень засобу :")

        // Table 4: Activations
        val actRows = maxOf(data.activations.size, 1)
        val table4 = doc.createTable(actRows + 1, 2)
        setTableWidth(table4, 6375)
        val colWidths4 = intArrayOf(3672, 2702)
        setCellWidth(table4.getRow(0).getCell(0), colWidths4[0])
        setCellText(table4.getRow(0).getCell(0), "Назва", bold = true, centered = true)
        setCellWidth(table4.getRow(0).getCell(1), colWidths4[1])
        setCellText(table4.getRow(0).getCell(1), "Кількість включень", bold = true, centered = true)
        for ((i, act) in data.activations.withIndex()) {
            val rowIdx = i + 1
            if (rowIdx >= table4.rows.size) break
            val row = table4.getRow(rowIdx)
            setCellWidth(row.getCell(0), colWidths4[0])
            setCellText(row.getCell(0), act.name, bold = false, centered = false)
            setCellWidth(row.getCell(1), colWidths4[1])
            setCellText(row.getCell(1), act.count.toString(), bold = false, centered = true)
        }

        addParagraph(doc, "")
        addParagraph(doc, "ПЕЛЕНГАТОР")
        addParagraph(doc, "(Вказувати частоти що пеленгувалися)")

        // Table 5: Pelengator
        val table5 = doc.createTable(2, 2)
        setTableWidth(table5, 10205)
        val colWidths5 = intArrayOf(2472, 7732)
        setCellWidth(table5.getRow(0).getCell(0), colWidths5[0])
        setCellText(table5.getRow(0).getCell(0), "Без Тривоги", bold = false, centered = false)
        setCellWidth(table5.getRow(0).getCell(1), colWidths5[1])
        setCellText(table5.getRow(0).getCell(1), data.dfNoAlertFreqs, bold = false, centered = false)
        setCellWidth(table5.getRow(1).getCell(0), colWidths5[0])
        setCellText(table5.getRow(1).getCell(0), "Під час тривоги", bold = false, centered = false)
        setCellWidth(table5.getRow(1).getCell(1), colWidths5[1])
        setCellText(table5.getRow(1).getCell(1), data.dfAlertFreqs, bold = false, centered = false)

        // Save file
        val outFile = getOutputFile(context, data)
        FileOutputStream(outFile).use { doc.write(it) }
        doc.close()
        return outFile
    }

    private fun getOutputFile(context: Context, data: ReportData): File {
        val dir = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir
        } else {
            @Suppress("DEPRECATION")
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
        dir?.mkdirs()
        val timestamp = java.text.SimpleDateFormat("ddMMyyyy_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
        val safeName = data.positionName.replace("/", "_").replace(" ", "")
        return File(dir, "DRAVEY_${safeName}_${timestamp}.docx")
    }

    private fun addParagraph(doc: XWPFDocument, text: String): XWPFParagraph {
        val p = doc.createParagraph()
        val run = p.createRun()
        run.setText(text)
        run.fontFamily = FONT_NAME
        run.fontSize = FONT_SIZE_HP / 2
        return p
    }

    private fun setTableWidth(table: XWPFTable, widthDxa: Int) {
        val tblPr = table.ctTbl.tblPr ?: table.ctTbl.addNewTblPr()
        val tblW = tblPr.tblW ?: tblPr.addNewTblW()
        tblW.w = BigInteger.valueOf(widthDxa.toLong())
        tblW.type = STTblWidth.DXA
    }

    private fun setCellWidth(cell: XWPFTableCell, widthDxa: Int) {
        val tcPr = cell.ctTc.tcPr ?: cell.ctTc.addNewTcPr()
        val tcW = tcPr.tcW ?: tcPr.addNewTcW()
        tcW.w = BigInteger.valueOf(widthDxa.toLong())
        tcW.type = STTblWidth.DXA
    }

    private fun setCellText(cell: XWPFTableCell, text: String, bold: Boolean, centered: Boolean) {
        val para = if (cell.paragraphs.isEmpty()) cell.addParagraph() else cell.paragraphs[0]
        if (centered) para.alignment = ParagraphAlignment.CENTER
        para.spacingAfter = 0
        if (para.runs.isNotEmpty()) {
            para.runs[0].also { run ->
                run.setText(text, 0)
                run.isBold = bold
                run.fontFamily = FONT_NAME
                run.fontSize = FONT_SIZE_HP / 2
            }
        } else {
            para.createRun().also { run ->
                run.setText(text)
                run.isBold = bold
                run.fontFamily = FONT_NAME
                run.fontSize = FONT_SIZE_HP / 2
            }
        }
    }

    private fun setCellMultilineText(cell: XWPFTableCell, text: String, bold: Boolean, centered: Boolean) {
        val lines = text.split("\n")
        // Clear existing paragraphs content
        for (i in cell.paragraphs.indices) {
            val p = cell.paragraphs[i]
            if (centered) p.alignment = ParagraphAlignment.CENTER
            p.spacingAfter = 0
        }
        for ((idx, line) in lines.withIndex()) {
            val para = if (idx < cell.paragraphs.size) cell.paragraphs[idx] else cell.addParagraph()
            if (centered) para.alignment = ParagraphAlignment.CENTER
            para.spacingAfter = 0
            val run = if (para.runs.isNotEmpty()) {
                para.runs[0].also { it.setText(line, 0) }
            } else {
                para.createRun().also { it.setText(line) }
            }
            run.isBold = bold
            run.fontFamily = FONT_NAME
            run.fontSize = FONT_SIZE_HP / 2
        }
    }
}
