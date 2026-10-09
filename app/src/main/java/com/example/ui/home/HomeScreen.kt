package com.example.ui.home

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PdfProjectEntity
import com.example.data.PdfRepository
import com.example.monetization.AdRemovalDialog
import com.example.monetization.BillingManager
import com.example.ui.editor.sharePdfFile
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    repository: PdfRepository,
    billingManager: BillingManager,
    onOpenProject: (PdfProjectEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val projects by repository.allProjects.collectAsState(initial = emptyList())
    val isAdRemoved by billingManager.isAdRemoved.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showNewBlankDialog by remember { mutableStateOf(false) }
    var showAdRemovalDialog by remember { mutableStateOf(false) }

    // SAF Document Picker to Upload any PDF
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                var fileName: String? = null
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
                val proj = repository.createProjectFromUploadedPdf(context, uri, fileName)
                if (proj != null) {
                    Toast.makeText(context, "PDF imported successfully", Toast.LENGTH_SHORT).show()
                    onOpenProject(proj)
                } else {
                    Toast.makeText(context, "Could not open PDF file", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Photo Picker to convert images/scans to PDF
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val proj = repository.createProjectFromImage(context, uri, "Photo Document")
                if (proj != null) {
                    Toast.makeText(context, "Converted image to PDF", Toast.LENGTH_SHORT).show()
                    onOpenProject(proj)
                }
            }
        }
    }

    val filteredProjects = projects.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header
            item {
                HomeHeader(
                    isAdRemoved = isAdRemoved,
                    onOpenVipClick = { showAdRemovalDialog = true },
                    onUploadClick = {
                        pdfPickerLauncher.launch(arrayOf("application/pdf"))
                    }
                )
            }

            // Quick Actions Cards (Larger icons, clean concise labels)
            item {
                QuickActionsRow(
                    onUploadPdf = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                    onConvertImage = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onNewBlank = { showNewBlankDialog = true }
                )
            }

            // Templates Shelf (Larger icons, clean titles)
            item {
                TemplatesShelf(
                    onSelectTemplate = { type ->
                        scope.launch {
                            val sample = repository.createSampleProject(context, type)
                            Toast.makeText(context, "Created ${sample.title}", Toast.LENGTH_SHORT).show()
                            onOpenProject(sample)
                        }
                    }
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search documents...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .testTag("search_documents_input"),
                    singleLine = true
                )
            }

            // Recent Documents Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${filteredProjects.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Documents List or Empty State
            if (filteredProjects.isEmpty()) {
                item {
                    EmptyDocumentsCard(
                        onUploadClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        onSampleClick = {
                            scope.launch {
                                val s = repository.createSampleProject(context, "invoice")
                                onOpenProject(s)
                            }
                        }
                    )
                }
            } else {
                itemsIndexed(filteredProjects, key = { _, item -> item.id }) { index, project ->
                    DocumentItemCard(
                        project = project,
                        colorIndex = index,
                        onClick = { onOpenProject(project) },
                        onShare = {
                            val f = File(project.filePath)
                            if (f.exists()) sharePdfFile(context, f)
                        },
                        onDuplicate = {
                            scope.launch {
                                repository.duplicateProject(context, project.id)
                                Toast.makeText(context, "Duplicated", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDelete = {
                            scope.launch {
                                repository.deleteProject(context, project.id)
                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
                .testTag("fab_upload_pdf"),
            containerColor = Color(0xFFE11D48),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Upload PDF", modifier = Modifier.size(28.dp))
        }
    }

    if (showNewBlankDialog) {
        NewBlankDocDialog(
            onDismiss = { showNewBlankDialog = false },
            onCreate = { title ->
                showNewBlankDialog = false
                scope.launch {
                    val p = repository.createBlankProject(context, title)
                    onOpenProject(p)
                }
            }
        )
    }

    if (showAdRemovalDialog) {
        AdRemovalDialog(
            billingManager = billingManager,
            onDismiss = { showAdRemovalDialog = false }
        )
    }
}

@Composable
private fun HomeHeader(
    isAdRemoved: Boolean,
    onOpenVipClick: () -> Unit,
    onUploadClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // LARGER APP ICON (48dp with 28dp icon)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFE11D48), Color(0xFFFB7185))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "PDF Studio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // VIP / Ad Removal Button
                Surface(
                    color = if (isAdRemoved) Color(0xFFFEF3C7) else Color(0xFFF3E8FF),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAdRemoved) Color(0xFFF59E0B) else Color(0xFFA855F7)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenVipClick)
                        .testTag("vip_ad_removal_header_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAdRemoved) "👑 VIP" else "⭐ No Ads",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAdRemoved) Color(0xFFB45309) else Color(0xFF7E22CE)
                        )
                    }
                }

                Surface(
                    color = Color(0xFFFFE4E6),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onUploadClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Open",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9F1239)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onUploadPdf: () -> Unit,
    onConvertImage: () -> Unit,
    onNewBlank: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card 1: Upload (Large icon)
        LargeIconActionCard(
            title = "Upload",
            icon = Icons.Default.FolderOpen,
            gradient = listOf(Color(0xFFE11D48), Color(0xFFF43F5E)),
            modifier = Modifier.weight(1f),
            onClick = onUploadPdf
        )

        // Card 2: Scan (Large icon)
        LargeIconActionCard(
            title = "Scan",
            icon = Icons.Default.AddPhotoAlternate,
            gradient = listOf(Color(0xFF0284C7), Color(0xFF0EA5E9)),
            modifier = Modifier.weight(1f),
            onClick = onConvertImage
        )

        // Card 3: Blank (Large icon)
        LargeIconActionCard(
            title = "Blank",
            icon = Icons.Default.NoteAdd,
            gradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
            modifier = Modifier.weight(1f),
            onClick = onNewBlank
        )
    }
}

@Composable
private fun LargeIconActionCard(
    title: String,
    icon: ImageVector,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp))
            .testTag("action_${title.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(vertical = 16.dp, horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // LARGER ICON CONTAINER (48dp with 28dp icon)
                Surface(
                    color = Color(0x33FFFFFF),
                    shape = CircleShape,
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TemplatesShelf(
    onSelectTemplate: (type: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = "Templates",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                LargeIconTemplateCard(
                    title = "Invoice",
                    icon = Icons.Default.Receipt,
                    gradient = listOf(Color(0xFFEA580C), Color(0xFFF97316)),
                    onClick = { onSelectTemplate("invoice") }
                )
            }
            item {
                LargeIconTemplateCard(
                    title = "Resume",
                    icon = Icons.Default.Description,
                    gradient = listOf(Color(0xFF059669), Color(0xFF10B981)),
                    onClick = { onSelectTemplate("resume") }
                )
            }
            item {
                LargeIconTemplateCard(
                    title = "Certificate",
                    icon = Icons.Default.CardGiftcard,
                    gradient = listOf(Color(0xFF7C3AED), Color(0xFFA855F7)),
                    onClick = { onSelectTemplate("certificate") }
                )
            }
        }
    }
}

@Composable
private fun LargeIconTemplateCard(
    title: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(vertical = 12.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // LARGER ICON (24dp)
                Surface(
                    color = Color(0x33FFFFFF),
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

data class DocColorTheme(
    val bgGradient: List<Color>,
    val accentColor: Color,
    val badgeBg: Color,
    val badgeText: Color
)

private val docThemes = listOf(
    DocColorTheme(
        bgGradient = listOf(Color(0xFFFFF1F2), Color(0xFFFFE4E6)),
        accentColor = Color(0xFFE11D48),
        badgeBg = Color(0xFFFFCCD5),
        badgeText = Color(0xFF9F1239)
    ),
    DocColorTheme(
        bgGradient = listOf(Color(0xFFF0F9FF), Color(0xFFE0F2FE)),
        accentColor = Color(0xFF0284C7),
        badgeBg = Color(0xFFBAE6FD),
        badgeText = Color(0xFF0369A1)
    ),
    DocColorTheme(
        bgGradient = listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5)),
        accentColor = Color(0xFF059669),
        badgeBg = Color(0xFFA7F3D0),
        badgeText = Color(0xFF065F46)
    ),
    DocColorTheme(
        bgGradient = listOf(Color(0xFFF5F3FF), Color(0xFFEDE9FE)),
        accentColor = Color(0xFF7C3AED),
        badgeBg = Color(0xFFDDD6FE),
        badgeText = Color(0xFF5B21B6)
    ),
    DocColorTheme(
        bgGradient = listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)),
        accentColor = Color(0xFFD97706),
        badgeBg = Color(0xFFFDE68A),
        badgeText = Color(0xFF92400E)
    )
)

@Composable
private fun DocumentItemCard(
    project: PdfProjectEntity,
    colorIndex: Int,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val theme = docThemes[colorIndex % docThemes.size]

    val formattedDate = remember(project.lastModified) {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.format(Date(project.lastModified))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp))
            .testTag("doc_card_${project.id.take(6)}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(theme.bgGradient))
                .border(1.dp, theme.accentColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LARGER THUMBNAIL / ICON (70dp x 86dp)
                Box(
                    modifier = Modifier
                        .size(70.dp, 86.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.5.dp, theme.accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    if (project.thumbnailPath != null && File(project.thumbnailPath).exists()) {
                        AsyncImage(
                            model = File(project.thumbnailPath),
                            contentDescription = "Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = theme.accentColor,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = theme.badgeBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${project.pageCount} ${if (project.pageCount == 1) "Page" else "Pages"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.badgeText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = formattedDate,
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }

                // Menu button
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color(0xFF334155)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open & Edit") },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = theme.accentColor) },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share PDF") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDocumentsCard(
    onUploadClick: () -> Unit,
    onSampleClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF1F2), Color(0xFFF0FDF4), Color(0xFFEFF6FF))
                    )
                )
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // LARGE ICON (68dp)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFE11D48), Color(0xFFFB7185))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "No Documents Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onSampleClick) {
                        Text("Load Sample", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }
                    Button(
                        onClick = onUploadClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                    ) {
                        Text("Upload PDF")
                    }
                }
            }
        }
    }
}
