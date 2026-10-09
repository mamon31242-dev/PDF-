package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfProjectDao {
    @Query("SELECT * FROM pdf_projects ORDER BY lastModified DESC")
    fun getAllProjects(): Flow<List<PdfProjectEntity>>

    @Query("SELECT * FROM pdf_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): PdfProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(project: PdfProjectEntity)

    @Update
    suspend fun update(project: PdfProjectEntity)

    @Delete
    suspend fun delete(project: PdfProjectEntity)

    @Query("DELETE FROM pdf_projects WHERE id = :id")
    suspend fun deleteById(id: String)
}
