package com.example.data

import android.content.Context
import android.net.Uri
import com.example.pdf.PdfEngine
import com.example.pdf.PdfPageEdits
import com.example.pdf.SamplePdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class PdfRepository(
    private val database: AppDatabase
) {
    private val dao = database.pdfProjectDao()

    val allProjects: Flow<List<PdfProjectEntity>> = dao.getAllProjects()

    suspend fun getProjectById(id: String): PdfProjectEntity? = withContext(Dispatchers.IO) {
        dao.getProjectById(id)
    }

    suspend fun ensureInitialSampleDocs(context: Context) = withContext(Dispatchers.IO) {
        val existing = dao.getAllProjects().firstOrNull()
        if (existing.isNullOrEmpty()) {
            createSampleProject(context, "invoice")
            createSampleProject(context, "certificate")
        }
    }

    suspend fun createSampleProject(context: Context, type: String): PdfProjectEntity = withContext(Dispatchers.IO) {
        val (file, title) = when (type.lowercase()) {
            "invoice" -> Pair(SamplePdfGenerator.createSampleInvoice(context), "Client Invoice (Sample).pdf")
            "resume" -> Pair(SamplePdfGenerator.createSampleResume(context), "Executive Resume (Sample).pdf")
            else -> Pair(SamplePdfGenerator.createSampleCertificate(context), "Certificate of Excellence (Sample).pdf")
        }

        val pageCount = PdfEngine.getPageCount(file).coerceAtLeast(1)
        val id = UUID.randomUUID().toString()

        // Generate thumbnail
        var thumbPath: String? = null
        val bmp = PdfEngine.renderPageToBitmap(file, 0, targetWidth = 400)
        if (bmp != null) {
            thumbPath = PdfEngine.saveThumbnail(context, bmp, id)
            bmp.recycle()
        }

        val entity = PdfProjectEntity(
            id = id,
            title = title,
            filePath = file.absolutePath,
            pageCount = pageCount,
            lastModified = System.currentTimeMillis(),
            elementsJson = "{}",
            thumbnailPath = thumbPath,
            fileSize = file.length()
        )
        dao.insertOrUpdate(entity)
        entity
    }

    suspend fun createProjectFromUploadedPdf(context: Context, uri: Uri, originalName: String?): PdfProjectEntity? = withContext(Dispatchers.IO) {
        val file = PdfEngine.importPdfFromUri(context, uri) ?: return@withContext null
        val pageCount = PdfEngine.getPageCount(file).coerceAtLeast(1)
        val id = UUID.randomUUID().toString()

        var thumbPath: String? = null
        val bmp = PdfEngine.renderPageToBitmap(file, 0, targetWidth = 400)
        if (bmp != null) {
            thumbPath = PdfEngine.saveThumbnail(context, bmp, id)
            bmp.recycle()
        }

        val title = originalName?.takeIf { it.isNotBlank() } ?: "Document_${System.currentTimeMillis() % 10000}.pdf"

        val entity = PdfProjectEntity(
            id = id,
            title = title,
            filePath = file.absolutePath,
            pageCount = pageCount,
            lastModified = System.currentTimeMillis(),
            elementsJson = "{}",
            thumbnailPath = thumbPath,
            fileSize = file.length()
        )
        dao.insertOrUpdate(entity)
        entity
    }

    suspend fun createBlankProject(context: Context, title: String): PdfProjectEntity = withContext(Dispatchers.IO) {
        val file = PdfEngine.createBlankPdf(context, title, 1)
        val id = UUID.randomUUID().toString()

        var thumbPath: String? = null
        val bmp = PdfEngine.renderPageToBitmap(file, 0, targetWidth = 400)
        if (bmp != null) {
            thumbPath = PdfEngine.saveThumbnail(context, bmp, id)
            bmp.recycle()
        }

        val cleanTitle = if (title.endsWith(".pdf", ignoreCase = true)) title else "$title.pdf"
        val entity = PdfProjectEntity(
            id = id,
            title = cleanTitle,
            filePath = file.absolutePath,
            pageCount = 1,
            lastModified = System.currentTimeMillis(),
            elementsJson = "{}",
            thumbnailPath = thumbPath,
            fileSize = file.length()
        )
        dao.insertOrUpdate(entity)
        entity
    }

    suspend fun createProjectFromImage(context: Context, imageUri: Uri, title: String): PdfProjectEntity? = withContext(Dispatchers.IO) {
        val file = PdfEngine.convertImageToPdf(context, imageUri, title) ?: return@withContext null
        val id = UUID.randomUUID().toString()

        var thumbPath: String? = null
        val bmp = PdfEngine.renderPageToBitmap(file, 0, targetWidth = 400)
        if (bmp != null) {
            thumbPath = PdfEngine.saveThumbnail(context, bmp, id)
            bmp.recycle()
        }

        val cleanTitle = if (title.endsWith(".pdf", ignoreCase = true)) title else "$title.pdf"
        val entity = PdfProjectEntity(
            id = id,
            title = cleanTitle,
            filePath = file.absolutePath,
            pageCount = 1,
            lastModified = System.currentTimeMillis(),
            elementsJson = "{}",
            thumbnailPath = thumbPath,
            fileSize = file.length()
        )
        dao.insertOrUpdate(entity)
        entity
    }

    suspend fun saveProjectEdits(
        context: Context,
        projectId: String,
        pageCount: Int,
        editsPerPage: Map<Int, PdfPageEdits>
    ): PdfProjectEntity? = withContext(Dispatchers.IO) {
        val current = dao.getProjectById(projectId) ?: return@withContext null

        val json = JSONObject()
        editsPerPage.forEach { (pageIdx, edits) ->
            json.put(pageIdx.toString(), edits.toJson())
        }

        // Render updated first page thumbnail
        val sourceFile = File(current.filePath)
        var thumbPath = current.thumbnailPath
        if (sourceFile.exists()) {
            val thumbBmp = PdfEngine.renderPageToBitmap(sourceFile, 0, targetWidth = 400)
            if (thumbBmp != null) {
                thumbPath = PdfEngine.saveThumbnail(context, thumbBmp, projectId)
                thumbBmp.recycle()
            }
        }

        val updated = current.copy(
            pageCount = pageCount,
            lastModified = System.currentTimeMillis(),
            elementsJson = json.toString(),
            thumbnailPath = thumbPath
        )
        dao.update(updated)
        updated
    }

    suspend fun exportPdfToFile(
        context: Context,
        project: PdfProjectEntity,
        editsPerPage: Map<Int, PdfPageEdits>,
        customTitle: String? = null
    ): File? = withContext(Dispatchers.IO) {
        val baseName = (customTitle ?: project.title).removeSuffix(".pdf").replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
        val exportFile = File(context.cacheDir, "${baseName}_edited_${System.currentTimeMillis()}.pdf")
        val sourceFile = File(project.filePath)

        val success = PdfEngine.exportPdf(
            context = context,
            sourcePdfFile = if (sourceFile.exists()) sourceFile else null,
            pageCount = project.pageCount,
            editsPerPage = editsPerPage,
            outputFile = exportFile
        )
        if (success) exportFile else null
    }

    suspend fun deleteProject(context: Context, id: String) = withContext(Dispatchers.IO) {
        val project = dao.getProjectById(id)
        if (project != null) {
            val file = File(project.filePath)
            if (file.exists()) file.delete()
            project.thumbnailPath?.let { File(it).delete() }
            dao.delete(project)
        }
    }

    suspend fun duplicateProject(context: Context, id: String): PdfProjectEntity? = withContext(Dispatchers.IO) {
        val original = dao.getProjectById(id) ?: return@withContext null
        val origFile = File(original.filePath)
        if (!origFile.exists()) return@withContext null

        val newId = UUID.randomUUID().toString()
        val newFile = File(context.filesDir, "doc_${newId}.pdf")
        FileInputStream(origFile).use { input ->
            FileOutputStream(newFile).use { output ->
                input.copyTo(output)
            }
        }

        val copyTitle = "Copy of " + original.title
        val entity = original.copy(
            id = newId,
            title = copyTitle,
            filePath = newFile.absolutePath,
            lastModified = System.currentTimeMillis()
        )
        dao.insertOrUpdate(entity)
        entity
    }

    fun parseEdits(jsonString: String): Map<Int, PdfPageEdits> {
        val map = mutableMapOf<Int, PdfPageEdits>()
        try {
            if (jsonString.isNotBlank() && jsonString != "{}") {
                val json = JSONObject(jsonString)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val pageIdx = key.toIntOrNull() ?: continue
                    val pageJson = json.getJSONObject(key)
                    map[pageIdx] = PdfPageEdits.fromJson(pageJson)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }
}
