package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_projects")
data class PdfProjectEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val filePath: String,
    val pageCount: Int,
    val lastModified: Long,
    val elementsJson: String = "{}",
    val thumbnailPath: String? = null,
    val fileSize: Long = 0L
)
