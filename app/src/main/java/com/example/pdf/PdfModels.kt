package com.example.pdf

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PointDto(val x: Float, val y: Float)

data class PdfTextElement(
    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var x: Float, // Relative 0f..1f within page
    var y: Float, // Relative 0f..1f within page
    var fontSize: Float = 16f,
    var colorHex: Long = 0xFF111827L, // Dark ink
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var isWhiteoutBackground: Boolean = false,
    var backgroundColorHex: Long = 0xFFFFFFFFL,
    var rotation: Float = 0f
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("text", text)
        put("x", x.toDouble())
        put("y", y.toDouble())
        put("fontSize", fontSize.toDouble())
        put("colorHex", colorHex)
        put("isBold", isBold)
        put("isItalic", isItalic)
        put("isWhiteoutBackground", isWhiteoutBackground)
        put("backgroundColorHex", backgroundColorHex)
        put("rotation", rotation.toDouble())
    }

    companion object {
        fun fromJson(json: JSONObject): PdfTextElement = PdfTextElement(
            id = json.optString("id", UUID.randomUUID().toString()),
            text = json.optString("text", ""),
            x = json.optDouble("x", 0.1).toFloat(),
            y = json.optDouble("y", 0.1).toFloat(),
            fontSize = json.optDouble("fontSize", 16.0).toFloat(),
            colorHex = json.optLong("colorHex", 0xFF111827L),
            isBold = json.optBoolean("isBold", false),
            isItalic = json.optBoolean("isItalic", false),
            isWhiteoutBackground = json.optBoolean("isWhiteoutBackground", false),
            backgroundColorHex = json.optLong("backgroundColorHex", 0xFFFFFFFFL),
            rotation = json.optDouble("rotation", 0.0).toFloat()
        )
    }
}

data class PdfImageElement(
    val id: String = UUID.randomUUID().toString(),
    var imagePath: String, // Internal file path or drawable ref
    var x: Float, // Relative 0f..1f
    var y: Float, // Relative 0f..1f
    var width: Float = 0.35f, // Relative width
    var height: Float = 0.25f, // Relative height
    var rotation: Float = 0f,
    var alpha: Float = 1.0f
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("imagePath", imagePath)
        put("x", x.toDouble())
        put("y", y.toDouble())
        put("width", width.toDouble())
        put("height", height.toDouble())
        put("rotation", rotation.toDouble())
        put("alpha", alpha.toDouble())
    }

    companion object {
        fun fromJson(json: JSONObject): PdfImageElement = PdfImageElement(
            id = json.optString("id", UUID.randomUUID().toString()),
            imagePath = json.optString("imagePath", ""),
            x = json.optDouble("x", 0.1).toFloat(),
            y = json.optDouble("y", 0.1).toFloat(),
            width = json.optDouble("width", 0.35).toFloat(),
            height = json.optDouble("height", 0.25).toFloat(),
            rotation = json.optDouble("rotation", 0.0).toFloat(),
            alpha = json.optDouble("alpha", 1.0).toFloat()
        )
    }
}

data class PdfWhiteoutElement(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var colorHex: Long = 0xFFFFFFFFL // Default white, or 0xFF000000 for redaction
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("x", x.toDouble())
        put("y", y.toDouble())
        put("width", width.toDouble())
        put("height", height.toDouble())
        put("colorHex", colorHex)
    }

    companion object {
        fun fromJson(json: JSONObject): PdfWhiteoutElement = PdfWhiteoutElement(
            id = json.optString("id", UUID.randomUUID().toString()),
            x = json.optDouble("x", 0.1).toFloat(),
            y = json.optDouble("y", 0.1).toFloat(),
            width = json.optDouble("width", 0.3).toFloat(),
            height = json.optDouble("height", 0.05).toFloat(),
            colorHex = json.optLong("colorHex", 0xFFFFFFFFL)
        )
    }
}

data class PdfDrawingElement(
    val id: String = UUID.randomUUID().toString(),
    val points: List<PointDto>,
    val colorHex: Long = 0xFF1E293BL,
    val strokeWidth: Float = 4f
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("colorHex", colorHex)
        put("strokeWidth", strokeWidth.toDouble())
        val arr = JSONArray()
        points.forEach { pt ->
            val pJson = JSONObject().apply {
                put("x", pt.x.toDouble())
                put("y", pt.y.toDouble())
            }
            arr.put(pJson)
        }
        put("points", arr)
    }

    companion object {
        fun fromJson(json: JSONObject): PdfDrawingElement {
            val pts = mutableListOf<PointDto>()
            val arr = json.optJSONArray("points")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    pts.add(PointDto(p.optDouble("x").toFloat(), p.optDouble("y").toFloat()))
                }
            }
            return PdfDrawingElement(
                id = json.optString("id", UUID.randomUUID().toString()),
                points = pts,
                colorHex = json.optLong("colorHex", 0xFF1E293BL),
                strokeWidth = json.optDouble("strokeWidth", 4.0).toFloat()
            )
        }
    }
}

data class PdfHighlightElement(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var colorHex: Long = 0x66FDE047L // Semi-transparent yellow
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("x", x.toDouble())
        put("y", y.toDouble())
        put("width", width.toDouble())
        put("height", height.toDouble())
        put("colorHex", colorHex)
    }

    companion object {
        fun fromJson(json: JSONObject): PdfHighlightElement = PdfHighlightElement(
            id = json.optString("id", UUID.randomUUID().toString()),
            x = json.optDouble("x", 0.1).toFloat(),
            y = json.optDouble("y", 0.1).toFloat(),
            width = json.optDouble("width", 0.4).toFloat(),
            height = json.optDouble("height", 0.04).toFloat(),
            colorHex = json.optLong("colorHex", 0x66FDE047L)
        )
    }
}

data class PdfPageEdits(
    val textElements: MutableList<PdfTextElement> = mutableListOf(),
    val imageElements: MutableList<PdfImageElement> = mutableListOf(),
    val whiteouts: MutableList<PdfWhiteoutElement> = mutableListOf(),
    val drawings: MutableList<PdfDrawingElement> = mutableListOf(),
    val highlights: MutableList<PdfHighlightElement> = mutableListOf()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("textElements", JSONArray().apply { textElements.forEach { put(it.toJson()) } })
        put("imageElements", JSONArray().apply { imageElements.forEach { put(it.toJson()) } })
        put("whiteouts", JSONArray().apply { whiteouts.forEach { put(it.toJson()) } })
        put("drawings", JSONArray().apply { drawings.forEach { put(it.toJson()) } })
        put("highlights", JSONArray().apply { highlights.forEach { put(it.toJson()) } })
    }

    companion object {
        fun fromJson(json: JSONObject): PdfPageEdits {
            val edits = PdfPageEdits()
            json.optJSONArray("textElements")?.let { arr ->
                for (i in 0 until arr.length()) edits.textElements.add(PdfTextElement.fromJson(arr.getJSONObject(i)))
            }
            json.optJSONArray("imageElements")?.let { arr ->
                for (i in 0 until arr.length()) edits.imageElements.add(PdfImageElement.fromJson(arr.getJSONObject(i)))
            }
            json.optJSONArray("whiteouts")?.let { arr ->
                for (i in 0 until arr.length()) edits.whiteouts.add(PdfWhiteoutElement.fromJson(arr.getJSONObject(i)))
            }
            json.optJSONArray("drawings")?.let { arr ->
                for (i in 0 until arr.length()) edits.drawings.add(PdfDrawingElement.fromJson(arr.getJSONObject(i)))
            }
            json.optJSONArray("highlights")?.let { arr ->
                for (i in 0 until arr.length()) edits.highlights.add(PdfHighlightElement.fromJson(arr.getJSONObject(i)))
            }
            return edits
        }
    }
}

enum class EditorTool {
    PAN_ZOOM,
    EDIT_TEXT,
    INSERT_IMAGE,
    WHITEOUT_REDACT,
    SIGN_DRAW,
    HIGHLIGHT
}
