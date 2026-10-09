package com.example.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object SamplePdfGenerator {

    /**
     * Generates a sample Business Invoice PDF.
     */
    suspend fun createSampleInvoice(context: Context): File = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "sample_invoice_${UUID.randomUUID()}.pdf")
        val document = PdfDocument()

        val width = PdfEngine.DEFAULT_A4_WIDTH
        val height = PdfEngine.DEFAULT_A4_HEIGHT
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply { isAntiAlias = true }

        // Top Accent Bar
        paint.color = Color.parseColor("#E11D48") // Ruby accent
        canvas.drawRect(0f, 0f, width.toFloat(), 12f, paint)

        // Company Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 24f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText("ACME STUDIO INNOVATIONS", 48f, 64f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("100 Innovation Way, Suite 400 • San Francisco, CA", 48f, 82f, paint)
        canvas.drawText("contact@acmestudio.example.com • +1 (555) 019-2834", 48f, 96f, paint)

        // Invoice Title & Meta
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 28f
        paint.color = Color.parseColor("#E11D48")
        canvas.drawText("INVOICE", 400f, 68f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        paint.color = Color.parseColor("#334155")
        canvas.drawText("Invoice #: INV-2026-0849", 400f, 86f, paint)
        canvas.drawText("Date: October 15, 2026", 400f, 100f, paint)
        canvas.drawText("Due Date: November 15, 2026", 400f, 114f, paint)

        // Divider
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1.5f
        canvas.drawLine(48f, 130f, (width - 48).toFloat(), 130f, paint)

        // Bill To section
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText("BILLED TO:", 48f, 160f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        paint.color = Color.parseColor("#1E293B")
        canvas.drawText("Apex Global Technologies Corp.", 48f, 178f, paint)
        canvas.drawText("Attn: Accounts Payable Department", 48f, 194f, paint)
        canvas.drawText("742 Evergreen Boulevard, Austin, TX", 48f, 210f, paint)

        // Table Header
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(RectF(48f, 240f, (width - 48).toFloat(), 270f), 6f, 6f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        paint.color = Color.parseColor("#334155")
        canvas.drawText("DESCRIPTION", 64f, 258f, paint)
        canvas.drawText("QTY", 320f, 258f, paint)
        canvas.drawText("RATE", 380f, 258f, paint)
        canvas.drawText("AMOUNT", 460f, 258f, paint)

        // Items
        val items = listOf(
            Triple("Cloud Architecture & System Design", "40 hrs", "$150.00"),
            Triple("Android Jetpack Compose UI Suite", "60 hrs", "$140.00"),
            Triple("REST & Real-time Synchronization Layer", "25 hrs", "$130.00"),
            Triple("Security Audit & Production Hardening", "15 hrs", "$160.00")
        )

        var y = 300f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        paint.color = Color.parseColor("#0F172A")

        items.forEach { item ->
            canvas.drawText(item.first, 64f, y, paint)
            canvas.drawText(item.second, 320f, y, paint)
            canvas.drawText(item.third, 380f, y, paint)
            val total = when (item.second) {
                "40 hrs" -> "$6,000.00"
                "60 hrs" -> "$8,400.00"
                "25 hrs" -> "$3,250.00"
                else -> "$2,400.00"
            }
            canvas.drawText(total, 460f, y, paint)

            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawLine(48f, y + 10f, (width - 48).toFloat(), y + 10f, paint)
            paint.color = Color.parseColor("#0F172A")
            y += 32f
        }

        // Summary Card
        y += 20f
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(RectF(340f, y, (width - 48).toFloat(), y + 110f), 8f, 8f, paint)

        paint.textSize = 11f
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("Subtotal:", 356f, y + 28f, paint)
        canvas.drawText("Tax (8.25%):", 356f, y + 54f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText("Total Due:", 356f, y + 90f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        paint.color = Color.parseColor("#1E293B")
        canvas.drawText("$20,050.00", 460f, y + 28f, paint)
        canvas.drawText("$1,654.12", 460f, y + 54f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        paint.color = Color.parseColor("#E11D48")
        canvas.drawText("$21,704.12", 450f, y + 90f, paint)

        // Draw Official Verified Badge
        val badgeRes = context.resources.getIdentifier("img_verified_badge_1791517499627", "drawable", context.packageName)
        if (badgeRes != 0) {
            val bmp = BitmapFactory.decodeResource(context.resources, badgeRes)
            if (bmp != null) {
                canvas.drawBitmap(bmp, null, RectF(64f, y - 10f, 184f, y + 110f), null)
                bmp.recycle()
            }
        }

        // Signature block
        val sigY = y + 160f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        paint.color = Color.parseColor("#334155")
        canvas.drawText("AUTHORIZED SIGNATURE", 48f, sigY, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.strokeWidth = 1f
        canvas.drawLine(48f, sigY + 40f, 220f, sigY + 40f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.textSize = 10f
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("John Doe, Chief Technology Officer", 48f, sigY + 56f, paint)

        // Footer
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        paint.color = Color.parseColor("#94A3B8")
        canvas.drawText("Thank you for your business! Payment terms: Net 30 days.", 48f, (height - 36).toFloat(), paint)

        document.finishPage(page)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        file
    }

    /**
     * Generates a sample Professional Resume PDF.
     */
    suspend fun createSampleResume(context: Context): File = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "sample_resume_${UUID.randomUUID()}.pdf")
        val document = PdfDocument()

        val width = PdfEngine.DEFAULT_A4_WIDTH
        val height = PdfEngine.DEFAULT_A4_HEIGHT
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply { isAntiAlias = true }

        // Header Background
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 0f, width.toFloat(), 130f, paint)

        // Candidate Name
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 26f
        paint.color = Color.WHITE
        canvas.drawText("ALEXANDRA REID", 48f, 54f, paint)

        // Role
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 13f
        paint.color = Color.parseColor("#FB7185")
        canvas.drawText("Senior Mobile Architect & Android Engineer", 48f, 78f, paint)

        paint.textSize = 10f
        paint.color = Color.parseColor("#94A3B8")
        canvas.drawText("alexandra.reid@email.com • +1 555-014-9988 • San Francisco, CA", 48f, 102f, paint)

        // Section: Summary
        var curY = 170f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText("PROFESSIONAL SUMMARY", 48f, curY, paint)

        paint.color = Color.parseColor("#E11D48")
        paint.strokeWidth = 2f
        canvas.drawLine(48f, curY + 6f, 120f, curY + 6f, paint)

        curY += 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        paint.color = Color.parseColor("#334155")
        canvas.drawText("Senior Android Engineer with 8+ years building mission-critical apps using Jetpack Compose,", 48f, curY, paint)
        curY += 16f
        canvas.drawText("Kotlin Coroutines, and scalable Clean Architecture. Passionate about performant mobile UX.", 48f, curY, paint)

        // Section: Experience
        curY += 36f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText("WORK EXPERIENCE", 48f, curY, paint)

        paint.color = Color.parseColor("#E11D48")
        canvas.drawLine(48f, curY + 6f, 100f, curY + 6f, paint)

        curY += 28f
        paint.textSize = 12f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText("Staff Android Engineer — CloudTech Labs", 48f, curY, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.textSize = 10f
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("2022 – Present | San Francisco, CA", 370f, curY, paint)

        curY += 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        paint.color = Color.parseColor("#334155")
        canvas.drawText("• Led mobile team of 12 engineers in redesigning flagship Android app with Compose.", 48f, curY, paint)
        curY += 16f
        canvas.drawText("• Reduced app launch time by 42% and achieved 99.95% crash-free user rate.", 48f, curY, paint)

        curY += 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText("Senior Android Developer — NextGen Fintech", 48f, curY, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.textSize = 10f
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("2018 – 2022 | Austin, TX", 400f, curY, paint)

        curY += 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        paint.color = Color.parseColor("#334155")
        canvas.drawText("• Architected biometric authentication and encrypted offline document storage vault.", 48f, curY, paint)
        curY += 16f
        canvas.drawText("• Integrated real-time document scanner and automated PDF verification workflow.", 48f, curY, paint)

        // Section: Skills
        curY += 36f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText("CORE SKILLS & TECHNOLOGIES", 48f, curY, paint)

        paint.color = Color.parseColor("#E11D48")
        canvas.drawLine(48f, curY + 6f, 110f, curY + 6f, paint)

        curY += 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        paint.color = Color.parseColor("#334155")
        canvas.drawText("Languages: Kotlin, Java, Python, C++", 48f, curY, paint)
        curY += 16f
        canvas.drawText("Frameworks: Jetpack Compose, Coroutines, Flow, Room, Retrofit, Navigation 3", 48f, curY, paint)
        curY += 16f
        canvas.drawText("Tools & Cloud: Git, CI/CD, Gradle DSL, Firebase, Android Studio, Docker", 48f, curY, paint)

        document.finishPage(page)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        file
    }

    /**
     * Generates a sample Certificate of Achievement PDF.
     */
    suspend fun createSampleCertificate(context: Context): File = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "sample_certificate_${UUID.randomUUID()}.pdf")
        val document = PdfDocument()

        val width = PdfEngine.DEFAULT_A4_WIDTH
        val height = PdfEngine.DEFAULT_A4_HEIGHT
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply { isAntiAlias = true }

        // Ornate Borders
        paint.color = Color.parseColor("#E11D48")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        canvas.drawRect(24f, 24f, (width - 24).toFloat(), (height - 24).toFloat(), paint)

        paint.color = Color.parseColor("#F59E0B") // Gold inner border
        paint.strokeWidth = 2f
        canvas.drawRect(34f, 34f, (width - 34).toFloat(), (height - 34).toFloat(), paint)

        paint.style = Paint.Style.FILL

        // Header Title
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 28f
        paint.color = Color.parseColor("#0F172A")
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("CERTIFICATE OF EXCELLENCE", width / 2f, 130f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("THIS IS PROUDLY PRESENTED TO", width / 2f, 180f, paint)

        // Awardee Name
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        paint.textSize = 32f
        paint.color = Color.parseColor("#E11D48")
        canvas.drawText("Jordan Hayes", width / 2f, 250f, paint)

        // Line under name
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1f
        canvas.drawLine(100f, 270f, (width - 100).toFloat(), 270f, paint)

        // Description
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 13f
        paint.color = Color.parseColor("#334155")
        canvas.drawText("for outstanding achievement and mastery in", width / 2f, 320f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 16f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText("Advanced Mobile Architecture & UI Systems", width / 2f, 350f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("Issued on October 2026 • Certificate ID: CRT-993821", width / 2f, 400f, paint)

        // Draw Official Verified Badge
        val badgeRes = context.resources.getIdentifier("img_verified_badge_1791517499627", "drawable", context.packageName)
        if (badgeRes != 0) {
            val bmp = BitmapFactory.decodeResource(context.resources, badgeRes)
            if (bmp != null) {
                paint.textAlign = Paint.Align.LEFT
                canvas.drawBitmap(bmp, null, RectF(width / 2f - 75f, 440f, width / 2f + 75f, 590f), null)
                bmp.recycle()
            }
        }

        // Signatures
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        paint.color = Color.parseColor("#1E293B")
        canvas.drawText("Dr. Marcus Vance", 150f, 680f, paint)
        canvas.drawText("Elena Rostova", 445f, 680f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.strokeWidth = 1f
        canvas.drawLine(80f, 660f, 220f, 660f, paint)
        canvas.drawLine(375f, 660f, 515f, 660f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        canvas.drawText("Program Director", 150f, 696f, paint)
        canvas.drawText("Board of Trustees", 445f, 696f, paint)

        document.finishPage(page)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        file
    }
}
