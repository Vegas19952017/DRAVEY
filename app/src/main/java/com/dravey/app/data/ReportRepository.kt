package com.dravey.app.data

import kotlinx.coroutines.flow.Flow

class ReportRepository(private val dao: ReportDao) {
    val allReports: Flow<List<ReportEntity>> = dao.getAllReports()

    suspend fun save(report: ReportEntity): Long = dao.insertReport(report)
    suspend fun delete(report: ReportEntity) = dao.deleteReport(report)
    suspend fun getById(id: Long): ReportEntity? = dao.getReportById(id)
}
