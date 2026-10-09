package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object PdfEngine {

    // Default A4 dimensions in PDF points (72 DPI)
    const val DEFAULT_A4_WIDTH = 595
    const val DEFAULT_A4_HEIGHT = 842

    /**
     * Renders a specific page from a PDF file to a high-quality Bitmap.
     */
    suspend fun renderPageToBitmap(
        pdfFile: File,
        pageIndex: Int,
        targetWidth: Int = 1200
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!pdfFile.exists() || pdfFile.length() == 0L) return@withContext null
        try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                pfd.close()
                return@withContext null
            }

            val page = renderer.openPage(pageIndex)
            val originalWidth = page.width
            val originalHeight = page.height

            val scale = targetWidth.toFloat() / originalWidth.toFloat()
            val targetHeight = (originalHeight * scale).toInt()

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            // Initialize with white background
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Gets page count of a PDF file.
     */
    suspend fun getPageCount(pdfFile: File): Int = withContext(Dispatchers.IO) {
        if (!pdfFile.exists() || pdfFile.length() == 0L) return@withContext 0
        try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Copies an external URI to an internal file in the app files directory.
     */
    suspend fun importPdfFromUri(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val destination = File(context.filesDir, "doc_${UUID.randomUUID()}.pdf")
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            destination
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates a new blank PDF file with the specified number of blank A4 pages.
     */
    suspend fun createBlankPdf(context: Context, title: String, pageCount: Int = 1): File = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "blank_${UUID.randomUUID()}.pdf")
        val document = PdfDocument()

        for (i in 0 until pageCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(DEFAULT_A4_WIDTH, DEFAULT_A4_HEIGHT, i + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            // Draw subtle margin guide line
            val paint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }
            canvas.drawRect(36f, 36f, (DEFAULT_A4_WIDTH - 36).toFloat(), (DEFAULT_A4_HEIGHT - 36).toFloat(), paint)

            document.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        file
    }

    /**
     * Converts an image to a single page PDF.
     */
    suspend fun convertImageToPdf(context: Context, imageUri: Uri, title: String): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return@withContext null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap == null) return@withContext null

            val file = File(context.filesDir, "scanned_${UUID.randomUUID()}.pdf")
            val document = PdfDocument()

            // Scale to fit A4
            val pageInfo = PdfDocument.PageInfo.Builder(DEFAULT_A4_WIDTH, DEFAULT_A4_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            val destRect = RectF(20f, 20f, (DEFAULT_A4_WIDTH - 20).toFloat(), (DEFAULT_A4_HEIGHT - 20).toFloat())
            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            canvas.drawBitmap(bitmap, srcRect, destRect, null)

            document.finishPage(page)
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            bitmap.recycle()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exports the edited PDF, applying whiteouts, text edits, image inserts, drawings, and highlights.
     */
    suspend fun exportPdf(
        context: Context,
        sourcePdfFile: File?,
        pageCount: Int,
        editsPerPage: Map<Int, PdfPageEdits>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            var renderer: PdfRenderer? = null
            var pfd: ParcelFileDescriptor? = null

            if (sourcePdfFile != null && sourcePdfFile.exists() && sourcePdfFile.length() > 0L) {
                pfd = ParcelFileDescriptor.open(sourcePdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)
            }

            for (pageIndex in 0 until pageCount) {
                var pageWidth = DEFAULT_A4_WIDTH
                var pageHeight = DEFAULT_A4_HEIGHT
                var baseBitmap: Bitmap? = null

                if (renderer != null && pageIndex < renderer.pageCount) {
                    val srcPage = renderer.openPage(pageIndex)
                    pageWidth = srcPage.width
                    pageHeight = srcPage.height

                    // Render source page at 2x resolution for crisp export
                    val renderW = pageWidth * 2
                    val renderH = pageHeight * 2
                    baseBitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                    val c = Canvas(baseBitmap)
                    c.drawColor(Color.WHITE)
                    srcPage.render(baseBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    srcPage.close()
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                // 1. Draw base page bitmap if exists
                if (baseBitmap != null) {
                    val destRect = RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat())
                    canvas.drawBitmap(baseBitmap, null, destRect, null)
                    baseBitmap.recycle()
                }

                val edits = editsPerPage[pageIndex]
                if (edits != null) {
                    // 2. Apply Whiteout / Redaction boxes (this cleanly covers existing text & images)
                    val whiteoutPaint = Paint().apply {
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    edits.whiteouts.forEach { wo ->
                        whiteoutPaint.color = wo.colorHex.toInt()
                        val left = wo.x * pageWidth
                        val top = wo.y * pageHeight
                        val right = left + (wo.width * pageWidth)
                        val bottom = top + (wo.height * pageHeight)
                        canvas.drawRect(left, top, right, bottom, whiteoutPaint)
                    }

                    // 3. Draw Highlights (translucent overlays)
                    val highlightPaint = Paint().apply {
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    edits.highlights.forEach { hl ->
                        highlightPaint.color = hl.colorHex.toInt()
                        val left = hl.x * pageWidth
                        val top = hl.y * pageHeight
                        val right = left + (hl.width * pageWidth)
                        val bottom = top + (hl.height * pageHeight)
                        canvas.drawRect(left, top, right, bottom, highlightPaint)
                    }

                    // 4. Draw Inserted / Replaced Images
                    val imagePaint = Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                    }
                    edits.imageElements.forEach { imgElem ->
                        val bmp = loadBitmap(context, imgElem.imagePath)
                        if (bmp != null) {
                            imagePaint.alpha = (imgElem.alpha * 255).toInt().coerceIn(0, 255)
                            val left = imgElem.x * pageWidth
                            val top = imgElem.y * pageHeight
                            val w = imgElem.width * pageWidth
                            val h = imgElem.height * pageHeight
                            val rect = RectF(left, top, left + w, top + h)

                            canvas.save()
                            if (imgElem.rotation != 0f) {
                                canvas.rotate(imgElem.rotation, rect.centerX(), rect.centerY())
                            }
                            canvas.drawBitmap(bmp, null, rect, imagePaint)
                            canvas.restore()
                            bmp.recycle()
                        }
                    }

                    // 5. Draw Drawings / Signatures
                    edits.drawings.forEach { drawing ->
                        if (drawing.points.size > 1) {
                            val strokePaint = Paint().apply {
                                color = drawing.colorHex.toInt()
                                strokeWidth = drawing.strokeWidth
                                style = Paint.Style.STROKE
                                strokeCap = Paint.Cap.ROUND
                                strokeJoin = Paint.Join.ROUND
                                isAntiAlias = true
                            }
                            val path = Path()
                            val first = drawing.points.first()
                            path.moveTo(first.x * pageWidth, first.y * pageHeight)
                            for (i in 1 until drawing.points.size) {
                                val pt = drawing.points[i]
                                path.lineTo(pt.x * pageWidth, pt.y * pageHeight)
                            }
                            canvas.drawPath(path, strokePaint)
                        }
                    }

                    // 6. Draw Text Elements (supports whiteout backing, bold/italic, size, multiline)
                    val textPaint = Paint().apply {
                        isAntiAlias = true
                    }
                    edits.textElements.forEach { txtElem ->
                        val x = txtElem.x * pageWidth
                        val y = txtElem.y * pageHeight

                        // Configure typeface
                        val style = when {
                            txtElem.isBold && txtElem.isItalic -> Typeface.BOLD_ITALIC
                            txtElem.isBold -> Typeface.BOLD
                            txtElem.isItalic -> Typeface.ITALIC
                            else -> Typeface.NORMAL
                        }
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, style)
                        textPaint.textSize = txtElem.fontSize
                        textPaint.color = txtElem.colorHex.toInt()

                        val lines = txtElem.text.split("\n")
                        val fm = textPaint.fontMetrics
                        val lineHeight = fm.descent - fm.ascent

                        // Measure maximum width
                        var maxLineWidth = 0f
                        lines.forEach { line ->
                            val w = textPaint.measureText(line)
                            if (w > maxLineWidth) maxLineWidth = w
                        }
                        val totalHeight = lineHeight * lines.size

                        canvas.save()
                        if (txtElem.rotation != 0f) {
                            canvas.rotate(txtElem.rotation, x, y)
                        }

                        // If whiteout background enabled: mask what's underneath first!
                        if (txtElem.isWhiteoutBackground) {
                            val bgPaint = Paint().apply {
                                color = txtElem.backgroundColorHex.toInt()
                                this.style = Paint.Style.FILL
                            }
                            canvas.drawRect(
                                x - 4f,
                                y - 4f,
                                x + maxLineWidth + 8f,
                                y + totalHeight + 4f,
                                bgPaint
                            )
                        }

                        var currentY = y - fm.ascent
                        lines.forEach { line ->
                            canvas.drawText(line, x, currentY, textPaint)
                            currentY += lineHeight
                        }

                        canvas.restore()
                    }
                }

                document.finishPage(page)
            }

            renderer?.close()
            pfd?.close()

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun loadBitmap(context: Context, pathOrName: String): Bitmap? {
        return try {
            when {
                pathOrName.startsWith("drawable:") -> {
                    val resName = pathOrName.removePrefix("drawable:")
                    val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                    if (resId != 0) BitmapFactory.decodeResource(context.resources, resId) else null
                }
                pathOrName.startsWith("content://") || pathOrName.startsWith("file://") -> {
                    val uri = Uri.parse(pathOrName)
                    val input = context.contentResolver.openInputStream(uri)
                    val bmp = BitmapFactory.decodeStream(input)
                    input?.close()
                    bmp
                }
                else -> {
                    val file = File(pathOrName)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Saves a thumbnail bitmap to app cache for fast display.
     */
    suspend fun saveThumbnail(context: Context, bitmap: Bitmap, id: String): String = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "thumb_${id}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        file.absolutePath
    }
}
