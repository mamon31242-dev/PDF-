package com.example.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdf.PdfDrawingElement
import com.example.pdf.PointDto

@Composable
fun SignatureDialog(
    onDismiss: () -> Unit,
    onSaveSignature: (PdfDrawingElement) -> Unit
) {
    val points = remember { mutableStateListOf<Offset>() }
    var selectedColor by remember { mutableLongStateOf(0xFF1D4ED8L) } // Royal Blue default for legal signatures
    var strokeWidth by remember { mutableFloatStateOf(4f) }

    val inkColors = listOf(
        Pair(0xFF1D4ED8L, "Blue Ink"),
        Pair(0xFF0F172AL, "Black Ink"),
        Pair(0xFF991B1BL, "Red Ink")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Draw E-Signature",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sign using your finger or stylus inside the box",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Drawing Surface
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFFFFF))
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .testTag("signature_canvas_box")
                ) {
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        points.add(offset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        points.add(change.position)
                                    }
                                )
                            }
                    ) {
                        // Draw signature baseline guide
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(24f, size.height * 0.75f),
                            end = Offset(size.width - 24f, size.height * 0.75f),
                            strokeWidth = 2f
                        )

                        if (points.size > 1) {
                            val path = Path()
                            path.moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                path.lineTo(points[i].x, points[i].y)
                            }
                            drawPath(
                                path = path,
                                color = Color(selectedColor),
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    if (points.isEmpty()) {
                        Text(
                            text = "✍️ Sign here...",
                            color = Color(0xFFCBD5E1),
                            fontSize = 18.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ink colors & Clear button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        inkColors.forEach { (cLong, _) ->
                            val isSelected = selectedColor == cLong
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(cLong))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x33888888),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = cLong }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { points.clear() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("clear_signature_button")
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Clear", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (points.isNotEmpty()) {
                        // Normalize points to relative 0f..1f (assuming 300x200 canvas)
                        val minX = points.minOf { it.x }
                        val maxX = points.maxOf { it.x }
                        val minY = points.minOf { it.y }
                        val maxY = points.maxOf { it.y }
                        val spanX = (maxX - minX).coerceAtLeast(1f)
                        val spanY = (maxY - minY).coerceAtLeast(1f)

                        // Place signature neatly inside normalized 0.35 x 0.12 bounds at center-right
                        val normalized = points.map { pt ->
                            val relX = ((pt.x - minX) / spanX) * 0.35f + 0.55f
                            val relY = ((pt.y - minY) / spanY) * 0.12f + 0.70f
                            PointDto(relX.coerceIn(0f, 1f), relY.coerceIn(0f, 1f))
                        }

                        onSaveSignature(
                            PdfDrawingElement(
                                points = normalized,
                                colorHex = selectedColor,
                                strokeWidth = strokeWidth
                            )
                        )
                    }
                },
                enabled = points.size > 1,
                modifier = Modifier.testTag("insert_signature_btn")
            ) {
                Text("Insert Signature")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
