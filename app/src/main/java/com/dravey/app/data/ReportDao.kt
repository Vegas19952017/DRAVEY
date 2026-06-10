package com.dravey.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity): Long

    @Delete
    suspend fun deleteReport(report: ReportEntity)

    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun getReportById(id: Long): ReportEntity?
}
