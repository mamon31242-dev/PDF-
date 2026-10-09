package com.example.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pdf.EditorTool
import com.example.pdf.PdfDrawingElement
import com.example.pdf.PdfHighlightElement
import com.example.pdf.PdfImageElement
import com.example.pdf.PdfPageEdits
import com.example.pdf.PdfTextElement
import com.example.pdf.PdfWhiteoutElement
import com.example.pdf.PointDto
import kotlin.math.roundToInt

@Composable
fun PdfCanvas(
    pageBitmap: Bitmap?,
    isLoadingPage: Boolean,
    edits: PdfPageEdits,
    currentTool: EditorTool,
    selectedTextId: String?,
    selectedImageId: String?,
    selectedWhiteoutId: String?,
    onSelectText: (String?) -> Unit,
    onSelectImage: (String?) -> Unit,
    onSelectWhiteout: (String?) -> Unit,
    onTapCanvas: (relX: Float, relY: Float) -> Unit,
    onUpdateTextPosition: (id: String, newX: Float, newY: Float) -> Unit,
    onUpdateImagePosition: (id: String, newX: Float, newY: Float) -> Unit,
    onUpdateWhiteoutPosition: (id: String, newX: Float, newY: Float) -> Unit,
    onEditTextRequested: (PdfTextElement) -> Unit,
    onDeleteElement: (type: String, id: String) -> Unit,
    onAddDrawing: (PdfDrawingElement) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        if (currentTool == EditorTool.PAN_ZOOM) {
            scale = (scale * zoomChange).coerceIn(0.75f, 4.0f)
            offset += offsetChange
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE2E8F0))
            .transformable(state = transformState)
            .testTag("pdf_canvas_container"),
        contentAlignment = Alignment.Center
    ) {
        if (isLoadingPage) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            return@Box
        }

        if (pageBitmap == null) {
            Text(
                text = "Unable to render page preview",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Box
        }

        BoxWithConstraints(
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(2.dp))
                .background(Color.White)
        ) {
            // Determine aspect ratio dimensions
            val bmpW = pageBitmap.width.toFloat()
            val bmpH = pageBitmap.height.toFloat()
            val aspectRatio = bmpW / bmpH

            val canvasW = maxWidth
            val canvasH = canvasW / aspectRatio

            Box(
                modifier = Modifier
                    .size(canvasW, canvasH)
                    .pointerInput(currentTool) {
                        detectTapGestures { tapOffset ->
                            val relX = (tapOffset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val relY = (tapOffset.y / size.height.toFloat()).coerceIn(0f, 1f)
                            onTapCanvas(relX, relY)
                        }
                    }
            ) {
                // Base PDF Page Image
                Image(
                    bitmap = pageBitmap.asImageBitmap(),
                    contentDescription = "PDF Page Surface",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                // Layer 1: Whiteouts & Redactions (Covers original text or graphics)
                edits.whiteouts.forEach { wo ->
                    val isSelected = selectedWhiteoutId == wo.id
                    val woLeft = wo.x * canvasW.value
                    val woTop = wo.y * canvasH.value
                    val woWidth = wo.width * canvasW.value
                    val woHeight = wo.height * canvasH.value

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(woLeft.dp.roundToPx(), woTop.dp.roundToPx()) }
                            .size(woWidth.dp, woHeight.dp)
                            .background(Color(wo.colorHex))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x3394A3B8)
                            )
                            .pointerInput(wo.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val newRelX = (wo.x + (dragAmount.x / (canvasW.value * density))).coerceIn(0f, 1f)
                                    val newRelY = (wo.y + (dragAmount.y / (canvasH.value * density))).coerceIn(0f, 1f)
                                    onUpdateWhiteoutPosition(wo.id, newRelX, newRelY)
                                }
                            }
                            .clickable { onSelectWhiteout(wo.id) }
                    ) {
                        if (isSelected) {
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .clickable { onDeleteElement("whiteout", wo.id) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete whiteout",
                                    tint = Color.White,
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }
                }

                // Layer 2: Highlights
                edits.highlights.forEach { hl ->
                    val hlLeft = hl.x * canvasW.value
                    val hlTop = hl.y * canvasH.value
                    val hlW = hl.width * canvasW.value
                    val hlH = hl.height * canvasH.value

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(hlLeft.dp.roundToPx(), hlTop.dp.roundToPx()) }
                            .size(hlW.dp, hlH.dp)
                            .background(Color(hl.colorHex))
                    )
                }

                // Layer 3: Image Elements (New or replaced images)
                edits.imageElements.forEach { imgElem ->
                    val isSelected = selectedImageId == imgElem.id
                    val imgLeft = imgElem.x * canvasW.value
                    val imgTop = imgElem.y * canvasH.value
                    val imgW = imgElem.width * canvasW.value
                    val imgH = imgElem.height * canvasH.value

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(imgLeft.dp.roundToPx(), imgTop.dp.roundToPx()) }
                            .size(imgW.dp, imgH.dp)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .pointerInput(imgElem.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val newRelX = (imgElem.x + (dragAmount.x / (canvasW.value * density))).coerceIn(0f, 1f)
                                    val newRelY = (imgElem.y + (dragAmount.y / (canvasH.value * density))).coerceIn(0f, 1f)
                                    onUpdateImagePosition(imgElem.id, newRelX, newRelY)
                                }
                            }
                            .clickable { onSelectImage(imgElem.id) }
                    ) {
                        RenderElementImage(
                            imagePath = imgElem.imagePath,
                            alpha = imgElem.alpha,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isSelected) {
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.TopEnd)
                                    .clickable { onDeleteElement("image", imgElem.id) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete image",
                                    tint = Color.White,
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }
                }

                // Layer 4: Drawings & Signatures
                Canvas(modifier = Modifier.fillMaxSize()) {
                    edits.drawings.forEach { drawing ->
                        if (drawing.points.size > 1) {
                            val path = Path()
                            val first = drawing.points.first()
                            path.moveTo(first.x * size.width, first.y * size.height)
                            for (i in 1 until drawing.points.size) {
                                val pt = drawing.points[i]
                                path.lineTo(pt.x * size.width, pt.y * size.height)
                            }
                            drawPath(
                                path = path,
                                color = Color(drawing.colorHex),
                                style = Stroke(
                                    width = drawing.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                // Layer 5: Text Elements (Editable, draggable, whiteout support)
                edits.textElements.forEach { txtElem ->
                    val isSelected = selectedTextId == txtElem.id
                    val txtLeft = txtElem.x * canvasW.value
                    val txtTop = txtElem.y * canvasH.value

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(txtLeft.dp.roundToPx(), txtTop.dp.roundToPx()) }
                            .then(
                                if (txtElem.isWhiteoutBackground) {
                                    Modifier.background(
                                        Color(txtElem.backgroundColorHex),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                } else Modifier
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .pointerInput(txtElem.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val newRelX = (txtElem.x + (dragAmount.x / (canvasW.value * density))).coerceIn(0f, 1f)
                                    val newRelY = (txtElem.y + (dragAmount.y / (canvasH.value * density))).coerceIn(0f, 1f)
                                    onUpdateTextPosition(txtElem.id, newRelX, newRelY)
                                }
                            }
                            .clickable {
                                onSelectText(txtElem.id)
                            }
                    ) {
                        Text(
                            text = txtElem.text,
                            fontSize = (txtElem.fontSize * 0.9f).sp,
                            color = Color(txtElem.colorHex),
                            fontWeight = if (txtElem.isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (txtElem.isItalic) FontStyle.Italic else FontStyle.Normal
                        )

                        if (isSelected) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 12.dp, y = (-16).dp),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { onEditTextRequested(txtElem) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit text",
                                        tint = Color.White,
                                        modifier = Modifier.padding(3.dp)
                                    )
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { onDeleteElement("text", txtElem.id) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete text",
                                        tint = Color.White,
                                        modifier = Modifier.padding(2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderElementImage(imagePath: String, alpha: Float, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (imagePath.startsWith("drawable:")) {
        val resName = imagePath.removePrefix("drawable:")
        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        if (resId != 0) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = resId),
                contentDescription = "Sticker or badge",
                modifier = modifier.graphicsLayer(alpha = alpha),
                contentScale = ContentScale.Fit
            )
            return
        }
    }

    AsyncImage(
        model = imagePath,
        contentDescription = "Inserted image",
        modifier = modifier.graphicsLayer(alpha = alpha),
        contentScale = ContentScale.Fit
    )
}
