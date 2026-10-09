package com.example.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PdfProjectEntity
import com.example.data.PdfRepository
import com.example.monetization.AdMobManager
import com.example.monetization.AdRemovalDialog
import com.example.monetization.BillingManager
import com.example.pdf.EditorTool
import com.example.pdf.PdfDrawingElement
import com.example.pdf.PdfEngine
import com.example.pdf.PdfHighlightElement
import com.example.pdf.PdfImageElement
import com.example.pdf.PdfPageEdits
import com.example.pdf.PdfTextElement
import com.example.pdf.PdfWhiteoutElement
import kotlinx.coroutines.launch
import java.io.File
import android.app.Activity
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorScreen(
    project: PdfProjectEntity,
    repository: PdfRepository,
    billingManager: BillingManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val isAdRemoved by billingManager.isAdRemoved.collectAsState()

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(project.pageCount.coerceAtLeast(1)) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }

    // Edits map per page index
    val editsMap = remember {
        mutableStateMapOf<Int, PdfPageEdits>().apply {
            putAll(repository.parseEdits(project.elementsJson))
        }
    }

    fun getOrCreateEdits(pageIdx: Int): PdfPageEdits {
        return editsMap.getOrPut(pageIdx) { PdfPageEdits() }
    }

    var currentTool by remember { mutableStateOf(EditorTool.EDIT_TEXT) }
    var selectedTextId by remember { mutableStateOf<String?>(null) }
    var selectedImageId by remember { mutableStateOf<String?>(null) }
    var selectedWhiteoutId by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showTextDialog by remember { mutableStateOf(false) }
    var editingTextElement by remember { mutableStateOf<PdfTextElement?>(null) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showRewardedAdExportPrompt by remember { mutableStateOf(false) }
    var showAdRemovalDialog by remember { mutableStateOf(false) }
    var showPageManagerSheet by remember { mutableStateOf(false) }

    // Load page bitmap when page changes
    LaunchedEffect(currentPageIndex, project.filePath) {
        isLoadingPage = true
        val file = File(project.filePath)
        val bmp = PdfEngine.renderPageToBitmap(file, currentPageIndex, targetWidth = 1400)
        pageBitmap = bmp
        isLoadingPage = false
    }

    // Photo picker for inserting images
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val edits = getOrCreateEdits(currentPageIndex)
            val newImg = PdfImageElement(
                imagePath = uri.toString(),
                x = 0.3f,
                y = 0.35f,
                width = 0.35f,
                height = 0.25f
            )
            edits.imageElements.add(newImg)
            selectedImageId = newImg.id
            Toast.makeText(context, "Image added to page", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-save edits on changes
    fun saveEdits() {
        scope.launch {
            repository.saveProjectEdits(context, project.id, totalPages, editsMap)
        }
    }

    BackHandler {
        saveEdits()
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                saveEdits()
                                onBack()
                            },
                            modifier = Modifier.testTag("editor_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = project.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "Page ${currentPageIndex + 1} of $totalPages",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Top action buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Page navigation steppers
                        IconButton(
                            onClick = {
                                if (currentPageIndex > 0) currentPageIndex--
                            },
                            enabled = currentPageIndex > 0
                        ) {
                            Icon(Icons.Default.NavigateBefore, contentDescription = "Previous page")
                        }

                        IconButton(
                            onClick = {
                                if (currentPageIndex < totalPages - 1) currentPageIndex++
                            },
                            enabled = currentPageIndex < totalPages - 1
                        ) {
                            Icon(Icons.Default.NavigateNext, contentDescription = "Next page")
                        }

                        // Undo last action on current page
                        IconButton(
                            onClick = {
                                val currentEdits = editsMap[currentPageIndex]
                                if (currentEdits != null) {
                                    when {
                                        currentEdits.textElements.isNotEmpty() -> currentEdits.textElements.removeLastOrNull()
                                        currentEdits.imageElements.isNotEmpty() -> currentEdits.imageElements.removeLastOrNull()
                                        currentEdits.whiteouts.isNotEmpty() -> currentEdits.whiteouts.removeLastOrNull()
                                        currentEdits.drawings.isNotEmpty() -> currentEdits.drawings.removeLastOrNull()
                                        currentEdits.highlights.isNotEmpty() -> currentEdits.highlights.removeLastOrNull()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("undo_action_btn")
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo")
                        }

                        // VIP / Ad Removal Chip
                        Surface(
                            color = if (isAdRemoved) Color(0xFFFEF3C7) else Color(0xFFF3E8FF),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isAdRemoved) Color(0xFFF59E0B) else Color(0xFFA855F7)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showAdRemovalDialog = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isAdRemoved) "👑 VIP" else "⭐ No Ads",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAdRemoved) Color(0xFFB45309) else Color(0xFF7E22CE)
                            )
                        }

                        // Export & Share
                        IconButton(
                            onClick = {
                                if (isAdRemoved) {
                                    showExportDialog = true
                                } else {
                                    showRewardedAdExportPrompt = true
                                }
                            },
                            modifier = Modifier.testTag("open_export_dialog_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export PDF",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // MAIN INTERACTIVE CANVAS
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                PdfCanvas(
                    pageBitmap = pageBitmap,
                    isLoadingPage = isLoadingPage,
                    edits = getOrCreateEdits(currentPageIndex),
                    currentTool = currentTool,
                    selectedTextId = selectedTextId,
                    selectedImageId = selectedImageId,
                    selectedWhiteoutId = selectedWhiteoutId,
                    onSelectText = { id ->
                        selectedTextId = id
                        selectedImageId = null
                        selectedWhiteoutId = null
                    },
                    onSelectImage = { id ->
                        selectedImageId = id
                        selectedTextId = null
                        selectedWhiteoutId = null
                    },
                    onSelectWhiteout = { id ->
                        selectedWhiteoutId = id
                        selectedTextId = null
                        selectedImageId = null
                    },
                    onTapCanvas = { relX, relY ->
                        when (currentTool) {
                            EditorTool.EDIT_TEXT -> {
                                val newElem = PdfTextElement(
                                    text = "New text",
                                    x = relX,
                                    y = relY,
                                    fontSize = 16f,
                                    isWhiteoutBackground = false
                                )
                                getOrCreateEdits(currentPageIndex).textElements.add(newElem)
                                editingTextElement = newElem
                                showTextDialog = true
                            }
                            EditorTool.WHITEOUT_REDACT -> {
                                val newWo = PdfWhiteoutElement(
                                    x = relX,
                                    y = relY,
                                    width = 0.25f,
                                    height = 0.04f,
                                    colorHex = 0xFFFFFFFFL
                                )
                                getOrCreateEdits(currentPageIndex).whiteouts.add(newWo)
                                selectedWhiteoutId = newWo.id
                            }
                            EditorTool.HIGHLIGHT -> {
                                val newHl = PdfHighlightElement(
                                    x = relX,
                                    y = relY,
                                    width = 0.35f,
                                    height = 0.03f
                                )
                                getOrCreateEdits(currentPageIndex).highlights.add(newHl)
                            }
                            else -> {
                                selectedTextId = null
                                selectedImageId = null
                                selectedWhiteoutId = null
                            }
                        }
                    },
                    onUpdateTextPosition = { id, newX, newY ->
                        getOrCreateEdits(currentPageIndex).textElements.find { it.id == id }?.let {
                            it.x = newX
                            it.y = newY
                        }
                    },
                    onUpdateImagePosition = { id, newX, newY ->
                        getOrCreateEdits(currentPageIndex).imageElements.find { it.id == id }?.let {
                            it.x = newX
                            it.y = newY
                        }
                    },
                    onUpdateWhiteoutPosition = { id, newX, newY ->
                        getOrCreateEdits(currentPageIndex).whiteouts.find { it.id == id }?.let {
                            it.x = newX
                            it.y = newY
                        }
                    },
                    onEditTextRequested = { txtElem ->
                        editingTextElement = txtElem
                        showTextDialog = true
                    },
                    onDeleteElement = { type, id ->
                        val edits = getOrCreateEdits(currentPageIndex)
                        when (type) {
                            "text" -> {
                                edits.textElements.removeAll { it.id == id }
                                selectedTextId = null
                            }
                            "image" -> {
                                edits.imageElements.removeAll { it.id == id }
                                selectedImageId = null
                            }
                            "whiteout" -> {
                                edits.whiteouts.removeAll { it.id == id }
                                selectedWhiteoutId = null
                            }
                        }
                    },
                    onAddDrawing = { drawing ->
                        getOrCreateEdits(currentPageIndex).drawings.add(drawing)
                    }
                )
            }

            // TOOL OPTIONS / CONTEXTUAL ACTIONS STRIP
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (currentTool) {
                            EditorTool.EDIT_TEXT -> {
                                ToolActionChip(
                                    label = "Add Text Box",
                                    icon = Icons.Default.Add,
                                    onClick = {
                                        val newElem = PdfTextElement(
                                            text = "Sample Text",
                                            x = 0.2f,
                                            y = 0.3f,
                                            fontSize = 16f,
                                            isWhiteoutBackground = false
                                        )
                                        getOrCreateEdits(currentPageIndex).textElements.add(newElem)
                                        editingTextElement = newElem
                                        showTextDialog = true
                                    }
                                )

                                ToolActionChip(
                                    label = "Whiteout & Replace Text",
                                    icon = Icons.Default.AutoFixHigh,
                                    onClick = {
                                        val edits = getOrCreateEdits(currentPageIndex)
                                        // Places a whiteout box + text with whiteout background
                                        val newElem = PdfTextElement(
                                            text = "Edited Text",
                                            x = 0.15f,
                                            y = 0.25f,
                                            fontSize = 16f,
                                            isWhiteoutBackground = true,
                                            backgroundColorHex = 0xFFFFFFFFL
                                        )
                                        edits.textElements.add(newElem)
                                        editingTextElement = newElem
                                        showTextDialog = true
                                        Toast.makeText(context, "Move over existing text to replace it seamlessly", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            EditorTool.INSERT_IMAGE -> {
                                ToolActionChip(
                                    label = "Add from Photos",
                                    icon = Icons.Default.AddPhotoAlternate,
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )

                                ToolActionChip(
                                    label = "Add Official Stamp",
                                    icon = Icons.Default.Verified,
                                    onClick = {
                                        val edits = getOrCreateEdits(currentPageIndex)
                                        val stamp = PdfImageElement(
                                            imagePath = "drawable:img_verified_badge_1791517499627",
                                            x = 0.55f,
                                            y = 0.65f,
                                            width = 0.3f,
                                            height = 0.2f
                                        )
                                        edits.imageElements.add(stamp)
                                        selectedImageId = stamp.id
                                        Toast.makeText(context, "Official verified stamp placed", Toast.LENGTH_SHORT).show()
                                    }
                                )

                                ToolActionChip(
                                    label = "Whiteout & Replace Image",
                                    icon = Icons.Default.AutoFixHigh,
                                    onClick = {
                                        val edits = getOrCreateEdits(currentPageIndex)
                                        // Creates whiteout patch to hide old image
                                        edits.whiteouts.add(
                                            PdfWhiteoutElement(
                                                x = 0.2f,
                                                y = 0.3f,
                                                width = 0.35f,
                                                height = 0.25f
                                            )
                                        )
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                            }

                            EditorTool.WHITEOUT_REDACT -> {
                                ToolActionChip(
                                    label = "Whiteout Mask (White)",
                                    icon = Icons.Default.Layers,
                                    onClick = {
                                        val newWo = PdfWhiteoutElement(
                                            x = 0.2f,
                                            y = 0.3f,
                                            width = 0.35f,
                                            height = 0.05f,
                                            colorHex = 0xFFFFFFFFL
                                        )
                                        getOrCreateEdits(currentPageIndex).whiteouts.add(newWo)
                                        selectedWhiteoutId = newWo.id
                                    }
                                )

                                ToolActionChip(
                                    label = "Redact (Blackout)",
                                    icon = Icons.Default.Brush,
                                    onClick = {
                                        val newWo = PdfWhiteoutElement(
                                            x = 0.2f,
                                            y = 0.3f,
                                            width = 0.35f,
                                            height = 0.05f,
                                            colorHex = 0xFF000000L
                                        )
                                        getOrCreateEdits(currentPageIndex).whiteouts.add(newWo)
                                        selectedWhiteoutId = newWo.id
                                    }
                                )
                            }

                            EditorTool.SIGN_DRAW -> {
                                ToolActionChip(
                                    label = "E-Signature Pad",
                                    icon = Icons.Default.Draw,
                                    onClick = { showSignatureDialog = true }
                                )
                            }

                            EditorTool.HIGHLIGHT -> {
                                ToolActionChip(
                                    label = "Add Yellow Highlight",
                                    icon = Icons.Default.Highlight,
                                    onClick = {
                                        getOrCreateEdits(currentPageIndex).highlights.add(
                                            PdfHighlightElement(
                                                x = 0.1f,
                                                y = 0.25f,
                                                width = 0.5f,
                                                height = 0.035f,
                                                colorHex = 0x66FDE047L
                                            )
                                        )
                                    }
                                )

                                ToolActionChip(
                                    label = "Green Highlight",
                                    icon = Icons.Default.Highlight,
                                    onClick = {
                                        getOrCreateEdits(currentPageIndex).highlights.add(
                                            PdfHighlightElement(
                                                x = 0.1f,
                                                y = 0.35f,
                                                width = 0.5f,
                                                height = 0.035f,
                                                colorHex = 0x6686EFACL
                                            )
                                        )
                                    }
                                )
                            }

                            EditorTool.PAN_ZOOM -> {
                                Text(
                                    text = "Pinch to zoom in/out • Drag to pan document",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // BOTTOM MAIN TOOL SELECTOR TABS
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorToolTab(
                        tool = EditorTool.EDIT_TEXT,
                        label = "Text",
                        icon = Icons.Default.TextFields,
                        isSelected = currentTool == EditorTool.EDIT_TEXT,
                        onSelect = { currentTool = EditorTool.EDIT_TEXT }
                    )

                    EditorToolTab(
                        tool = EditorTool.INSERT_IMAGE,
                        label = "Images",
                        icon = Icons.Default.Image,
                        isSelected = currentTool == EditorTool.INSERT_IMAGE,
                        onSelect = { currentTool = EditorTool.INSERT_IMAGE }
                    )

                    EditorToolTab(
                        tool = EditorTool.WHITEOUT_REDACT,
                        label = "Whiteout",
                        icon = Icons.Default.Layers,
                        isSelected = currentTool == EditorTool.WHITEOUT_REDACT,
                        onSelect = { currentTool = EditorTool.WHITEOUT_REDACT }
                    )

                    EditorToolTab(
                        tool = EditorTool.SIGN_DRAW,
                        label = "Sign",
                        icon = Icons.Default.Draw,
                        isSelected = currentTool == EditorTool.SIGN_DRAW,
                        onSelect = { currentTool = EditorTool.SIGN_DRAW }
                    )

                    EditorToolTab(
                        tool = EditorTool.HIGHLIGHT,
                        label = "Highlight",
                        icon = Icons.Default.Highlight,
                        isSelected = currentTool == EditorTool.HIGHLIGHT,
                        onSelect = { currentTool = EditorTool.HIGHLIGHT }
                    )

                    EditorToolTab(
                        tool = EditorTool.PAN_ZOOM,
                        label = "Pan / Zoom",
                        icon = Icons.Default.PanTool,
                        isSelected = currentTool == EditorTool.PAN_ZOOM,
                        onSelect = { currentTool = EditorTool.PAN_ZOOM }
                    )
                }
            }
        }
    }

    // TEXT EDIT DIALOG
    if (showTextDialog && editingTextElement != null) {
        TextEditDialog(
            initialElement = editingTextElement!!,
            onDismiss = {
                showTextDialog = false
                editingTextElement = null
            },
            onSave = { updated ->
                val edits = getOrCreateEdits(currentPageIndex)
                val idx = edits.textElements.indexOfFirst { it.id == updated.id }
                if (idx >= 0) {
                    edits.textElements[idx] = updated
                } else {
                    edits.textElements.add(updated)
                }
                selectedTextId = updated.id
                showTextDialog = false
                editingTextElement = null
                saveEdits()
            },
            onDelete = {
                getOrCreateEdits(currentPageIndex).textElements.removeAll { it.id == editingTextElement!!.id }
                showTextDialog = false
                editingTextElement = null
                selectedTextId = null
                saveEdits()
            }
        )
    }

    // SIGNATURE DIALOG
    if (showSignatureDialog) {
        SignatureDialog(
            onDismiss = { showSignatureDialog = false },
            onSaveSignature = { sigElement ->
                getOrCreateEdits(currentPageIndex).drawings.add(sigElement)
                showSignatureDialog = false
                Toast.makeText(context, "E-signature inserted into document", Toast.LENGTH_SHORT).show()
                saveEdits()
            }
        )
    }

    // EXPORT DIALOG
    if (showExportDialog) {
        ExportDialog(
            initialFileName = project.title,
            onDismiss = { showExportDialog = false },
            onExportRequested = { fileName, shareImmediately ->
                showExportDialog = false
                scope.launch {
                    val file = repository.exportPdfToFile(context, project, editsMap, fileName)
                    if (file != null) {
                        Toast.makeText(context, "Exported successfully: ${file.name}", Toast.LENGTH_LONG).show()
                        if (shareImmediately) {
                            sharePdfFile(context, file)
                        }
                    } else {
                        Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // REWARDED AD PROMPT DIALOG
    if (showRewardedAdExportPrompt) {
        AlertDialog(
            onDismissRequest = { showRewardedAdExportPrompt = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Export Document",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Watch a brief sponsored ad to export your vector PDF for free, or upgrade to VIP Ad-Free for instant lifetime exports.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRewardedAdExportPrompt = false
                        if (activity != null) {
                            AdMobManager.showRewardedAd(
                                activity = activity,
                                onUserEarnedReward = { showExportDialog = true },
                                onAdClosedOrFailed = { showExportDialog = true }
                            )
                        } else {
                            showExportDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🎬 Watch Ad to Export Free", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            showRewardedAdExportPrompt = false
                            showAdRemovalDialog = true
                        }
                    ) {
                        Text("👑 Remove Ads", fontWeight = FontWeight.Bold, color = Color(0xFF7E22CE))
                    }
                    TextButton(onClick = { showRewardedAdExportPrompt = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // AD REMOVAL / IN-APP PURCHASE DIALOG
    if (showAdRemovalDialog) {
        AdRemovalDialog(
            billingManager = billingManager,
            onDismiss = { showAdRemovalDialog = false }
        )
    }
}

@Composable
private fun EditorToolTab(
    tool: EditorTool,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val activeColor = when (tool) {
        EditorTool.EDIT_TEXT -> Color(0xFFE11D48)
        EditorTool.INSERT_IMAGE -> Color(0xFF0284C7)
        EditorTool.WHITEOUT_REDACT -> Color(0xFFD97706)
        EditorTool.SIGN_DRAW -> Color(0xFF7C3AED)
        EditorTool.HIGHLIGHT -> Color(0xFF059669)
        EditorTool.PAN_ZOOM -> Color(0xFF475569)
    }

    Surface(
        color = if (isSelected) activeColor.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, activeColor.copy(alpha = 0.4f)) else null,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("tool_tab_${label.lowercase().replace(" ", "_")}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ToolActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
