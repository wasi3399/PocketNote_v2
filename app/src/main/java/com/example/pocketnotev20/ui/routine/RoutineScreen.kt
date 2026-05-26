package com.example.pocketnotev20.ui.routine

import android.content.ContentValues
import android.content.Context
import android.app.DownloadManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.example.pocketnotev20.model.RoutineItem
import com.example.pocketnotev20.repository.FirestoreRepository
import com.example.pocketnotev20.ui.common.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val Level1Term1SectionAAssetPath = "routines/levelterm_1_term_i_a.pdf"
private const val Level3Term1SectionBAssetPath = "routines/levelterm_3_term_i_b.pdf"

private data class RoutinePdfItem(
    val level: String,
    val term: String,
    val section: String,
    val assetPath: String,
    val upload: RoutineItem? = null
)

private data class PdfRenderState(
    val bitmapWidth: Int = 0,
    val bitmapHeight: Int = 0,
    val pageBitmap: androidx.compose.ui.graphics.ImageBitmap? = null,
    val pageCount: Int = 0,
    val error: String? = null
)

private val availableRoutinePdfs = listOf(
    RoutinePdfItem(
        level = "Level 1",
        term = "Term I",
        section = "Section A",
        assetPath = Level1Term1SectionAAssetPath
    ),
    RoutinePdfItem(
        level = "Level 3",
        term = "Term I",
        section = "Section B",
        assetPath = Level3Term1SectionBAssetPath
    )
)

@Composable
fun RoutineScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FirestoreRepository() }
    val prefs = remember { context.getSharedPreferences("routine_prefs", Context.MODE_PRIVATE) }

    val levels = listOf("Level 1", "Level 2", "Level 3", "Level 4")
    val terms = listOf("Term I", "Term II")
    val sections = listOf("Section A", "Section B")

    // Remember selections across app restarts
    var selectedLevel by rememberSaveable { mutableStateOf(prefs.getString("level", "") ?: "") }
    var selectedTerm by rememberSaveable { mutableStateOf(prefs.getString("term", "") ?: "") }
    var selectedSection by rememberSaveable { mutableStateOf(prefs.getString("section", "") ?: "") }
    var uploadedRoutines by remember { mutableStateOf<List<RoutineItem>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.getRoutine(
            onSuccess = { routines ->
                uploadedRoutines = routines.filter {
                    it.section.isNotBlank() && (it.storagePath.isNotBlank() || it.fileUrl.isNotBlank())
                }
                loadError = null
            },
            onFailure = { error ->
                loadError = error.message ?: "Unable to load uploaded routines."
            }
        )
    }

    // Save selection to storage
    LaunchedEffect(selectedLevel, selectedTerm, selectedSection) {
        prefs.edit().apply {
            putString("level", selectedLevel)
            putString("term", selectedTerm)
            putString("section", selectedSection)
            apply()
        }
    }

    val selectedRoutine = remember(selectedLevel, selectedTerm, selectedSection) {
        if (selectedLevel.isEmpty() || selectedTerm.isEmpty() || selectedSection.isEmpty()) {
            null
        } else {
            val uploadedRoutine = uploadedRoutines.firstOrNull {
                it.level == selectedLevel && it.term == selectedTerm && it.section == selectedSection
            }
            if (uploadedRoutine != null) {
                RoutinePdfItem(
                    level = uploadedRoutine.level,
                    term = uploadedRoutine.term,
                    section = uploadedRoutine.section,
                    assetPath = "",
                    upload = uploadedRoutine
                )
            } else {
                availableRoutinePdfs.firstOrNull {
                    it.level == selectedLevel && it.term == selectedTerm && it.section == selectedSection
                }
            }
        }
    }

    ProfessionalPageScaffold(
        title = "Class Routine",
        subtitle = "Find and view your academic schedule with a professional PDF reader.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppInfoStrip(label = "Level", value = selectedLevel.ifEmpty { "None" })
            AppInfoStrip(label = "Term", value = selectedTerm.ifEmpty { "None" })
        }

        Spacer(modifier = Modifier.height(16.dp))

        AppPanelCard {
            AppSectionTitle(title = "Choose Level")
            Spacer(modifier = Modifier.height(14.dp))
            SelectorRow(items = levels, selectedItem = selectedLevel, onSelect = { selectedLevel = it })
            Spacer(modifier = Modifier.height(16.dp))
            AppSectionTitle(title = "Choose Term")
            Spacer(modifier = Modifier.height(14.dp))
            SelectorRow(items = terms, selectedItem = selectedTerm, onSelect = { selectedTerm = it })
            Spacer(modifier = Modifier.height(16.dp))
            AppSectionTitle(title = "Choose Section")
            Spacer(modifier = Modifier.height(14.dp))
            SelectorRow(items = sections, selectedItem = selectedSection, onSelect = { selectedSection = it })
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!loadError.isNullOrBlank()) {
            AppPanelCard {
                Text(
                    text = loadError.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Animated Content for better UX
        AnimatedContent(
            targetState = selectedLevel.isNotEmpty() && selectedTerm.isNotEmpty() && selectedSection.isNotEmpty(),
            label = "RoutineContent"
        ) { isSelected ->
            if (isSelected) {
                if (selectedRoutine == null) {
                    AppPanelCard {
                        AppSectionTitle(title = "No Routine Available")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No routine PDF has been added yet for $selectedLevel / $selectedTerm / $selectedSection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTextDim
                        )
                    }
                } else {
                    RoutinePdfViewerCard(
                        item = selectedRoutine,
                        repository = repository,
                        onDownloadClick = {
                            val upload = selectedRoutine.upload
                            if (upload == null) {
                                downloadAssetPdf(context, selectedRoutine.assetPath)
                            } else {
                                downloadUploadedRoutine(context, repository, upload)
                            }
                        }
                    )
                }
            } else {
                AppPanelCard {
                    AppSectionTitle(title = "Selection Required", subtitle = "Pick your Level, Term and Section to see the schedule.")
                }
            }
        }
    }
}

@Composable
private fun SelectorRow(
    items: List<String>,
    selectedItem: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            val isSelected = selectedItem == item
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(item) },
                label = {
                    Text(
                        text = item,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White,
                    labelColor = Color(0xFF64748B),
                    selectedContainerColor = Color(0xFFE0E7FF),
                    selectedLabelColor = Color(0xFF4338CA)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = Color(0xFFE2E8F0),
                    selectedBorderColor = Color(0xFFC7D2FE),
                    borderWidth = 1.dp
                )
            )
        }
    }
}

@Composable
private fun RoutinePdfViewerCard(
    item: RoutinePdfItem,
    repository: FirestoreRepository,
    onDownloadClick: () -> Unit
) {
    val context = LocalContext.current
    val isUploadedRoutine = item.upload != null
    var currentPage by remember(item.assetPath, item.upload?.id) { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += offsetChange
    }

    val pdfState by produceState(
        initialValue = PdfRenderState(),
        key1 = item.assetPath,
        key2 = currentPage,
        key3 = item.upload?.storagePath
    ) {
        value = if (isUploadedRoutine) {
            val upload = item.upload
            if (upload.fileType.contains("pdf", ignoreCase = true)) {
                renderUploadedPdfPage(
                    context = context,
                    repository = repository,
                    routine = upload,
                    pageIndex = currentPage
                )
            } else {
                PdfRenderState()
            }
        } else {
            withContext(Dispatchers.IO) {
                renderPdfPage(
                    context = context,
                    assetPath = item.assetPath,
                    pageIndex = currentPage
                )
            }
        }
    }

    LaunchedEffect(item.assetPath, currentPage) {
        scale = 1f
        offset = Offset.Zero
    }

    AppPanelCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppSecondaryButton(
                text = "Download",
                onClick = onDownloadClick,
                modifier = Modifier.wrapContentWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isUploadedRoutine && item.upload?.fileType?.contains("pdf", ignoreCase = true) != true) {
            AppSectionTitle(
                title = item.upload?.uploadName?.ifBlank { "Uploaded Routine" } ?: "Uploaded Routine",
                subtitle = "Tap Download to save this routine file to your phone."
            )
            return@AppPanelCard
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppSecondaryButton(text = "Zoom Out", onClick = { scale = (scale - 0.5f).coerceIn(1f, 5f); if(scale==1f) offset=Offset.Zero }, modifier = Modifier.weight(1f))
            AppSecondaryButton(text = "Reset", onClick = { scale = 1f; offset = Offset.Zero }, modifier = Modifier.weight(1f))
            AppSecondaryButton(text = "Zoom In", onClick = { scale = (scale + 0.5f).coerceIn(1f, 5f) }, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            pdfState.error != null -> {
                Text(text = pdfState.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            pdfState.pageBitmap != null -> {
                val aspectRatio = if (pdfState.bitmapHeight == 0) 0.72f else pdfState.bitmapWidth.toFloat() / pdfState.bitmapHeight.toFloat()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .aspectRatio(aspectRatio)
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f; if(scale==1f) offset=Offset.Zero })
                        }
                        .transformable(state = transformState)
                ) {
                    pdfState.pageBitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "Routine",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RoutineMetaBadge(
                        label = "Pages",
                        value = pdfState.pageCount.coerceAtLeast(1).toString(),
                        modifier = Modifier.weight(1f)
                    )
                    RoutineMetaBadge(
                        label = "Zoom",
                        value = "${(scale * 100).toInt()}%",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppSecondaryButton(text = "Prev", onClick = { if (currentPage > 0) currentPage-- }, enabled = currentPage > 0, modifier = Modifier.weight(1f))
                    Text("Page ${currentPage + 1} / ${pdfState.pageCount}", fontWeight = FontWeight.SemiBold)
                    AppSecondaryButton(text = "Next", onClick = { if (currentPage < pdfState.pageCount - 1) currentPage++ }, enabled = currentPage < pdfState.pageCount - 1, modifier = Modifier.weight(1f))
                }
            }
            else -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun RoutineMetaBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = AppPanelMuted,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = AppTextDim)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun renderPdfPage(context: Context, assetPath: String, pageIndex: Int): PdfRenderState {
    return try {
        val pdfFile = copyAssetToCache(context, assetPath)
        renderPdfFile(pdfFile, pageIndex)
    } catch (e: Exception) { PdfRenderState(error = e.message) }
}

private suspend fun renderUploadedPdfPage(
    context: Context,
    repository: FirestoreRepository,
    routine: RoutineItem,
    pageIndex: Int
): PdfRenderState {
    return withContext(Dispatchers.IO) {
        try {
            val pdfFile = File(context.cacheDir, routineCacheFileName(routine))
            if (!pdfFile.exists() || pdfFile.length() == 0L) {
                downloadRoutineToFile(repository, routine, pdfFile)
            }
            renderPdfFile(pdfFile, pageIndex)
        } catch (e: Exception) {
            PdfRenderState(error = e.message)
        }
    }
}

private fun renderPdfFile(pdfFile: File, pageIndex: Int): PdfRenderState {
    return try {
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                val safeIndex = pageIndex.coerceIn(0, renderer.pageCount - 1)
                renderer.openPage(safeIndex).use { page ->
                    val bitmap = createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    PdfRenderState(bitmap.width, bitmap.height, bitmap.asImageBitmap(), renderer.pageCount)
                }
            }
        }
    } catch (e: Exception) { PdfRenderState(error = e.message) }
}

private fun copyAssetToCache(context: Context, assetPath: String): File {
    val file = File(context.cacheDir, assetPath.substringAfterLast("/"))
    if (!file.exists()) {
        context.assets.open(assetPath).use { input -> file.outputStream().use { input.copyTo(it) } }
    }
    return file
}

private fun downloadAssetPdf(context: Context, assetPath: String) {
    try {
        val name = assetPath.substringAfterLast("/")
        val file = copyAssetToCache(context, assetPath)
        copyFileToDownloads(context, file, name, "application/pdf")
        Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
}

private fun downloadUploadedRoutine(
    context: Context,
    repository: FirestoreRepository,
    routine: RoutineItem
) {
    val name = routine.uploadName.ifBlank { "routine_${System.currentTimeMillis()}" }
    val mimeType = routine.fileType.ifBlank { "application/octet-stream" }

    if (routine.fileUrl.isNotBlank()) {
        try {
            val request = DownloadManager.Request(Uri.parse(routine.fileUrl))
                .setTitle(name)
                .setDescription("Downloading routine")
                .setMimeType(mimeType)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        return
    }

    val tempFile = File(context.cacheDir, routineCacheFileName(routine))
    repository.downloadRoutineFile(
        routine = routine,
        destinationFile = tempFile,
        onSuccess = {
            try {
                copyFileToDownloads(context, tempFile, name, mimeType)
                Toast.makeText(context, "File saved to Downloads", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        },
        onFailure = {
            Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    )
}

private fun copyFileToDownloads(context: Context, sourceFile: File, displayName: String, mimeType: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Could not create download file.")
        context.contentResolver.openOutputStream(uri).use { output ->
            sourceFile.inputStream().use { input -> input.copyTo(output ?: throw IllegalStateException("Could not open download file.")) }
        }
    } else {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val destinationFile = File(downloadsDir, displayName)
        sourceFile.inputStream().use { input ->
            FileOutputStream(destinationFile).use { output -> input.copyTo(output) }
        }
    }
}

private suspend fun downloadRoutineToFile(
    repository: FirestoreRepository,
    routine: RoutineItem,
    destinationFile: File
) {
    suspendCancellableCoroutine { continuation ->
        repository.downloadRoutineFile(
            routine = routine,
            destinationFile = destinationFile,
            onSuccess = {
                if (continuation.isActive) continuation.resume(Unit)
            },
            onFailure = {
                if (continuation.isActive) continuation.resumeWithException(it)
            }
        )
    }
}

private fun routineCacheFileName(routine: RoutineItem): String {
    val baseName = routine.uploadName.ifBlank { routine.id.ifBlank { routine.storagePath.substringAfterLast("/") } }
    return baseName.ifBlank { "routine_upload" }.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
