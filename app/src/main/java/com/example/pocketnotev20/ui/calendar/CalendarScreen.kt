package com.example.pocketnotev20.ui.calendar

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.example.pocketnotev20.ui.common.AppInfoStrip
import com.example.pocketnotev20.ui.common.AppPanelCard
import com.example.pocketnotev20.ui.common.AppPrimaryButton
import com.example.pocketnotev20.ui.common.AppSecondaryButton
import com.example.pocketnotev20.ui.common.AppSectionTitle
import com.example.pocketnotev20.ui.common.ProfessionalPageScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val Calendar2026AssetPath = "calendar_2026.pdf"

private data class PdfRenderState(
    val bitmapWidth: Int = 0,
    val bitmapHeight: Int = 0,
    val pageBitmap: androidx.compose.ui.graphics.ImageBitmap? = null,
    val pageCount: Int = 0,
    val error: String? = null
)

@Composable
fun CalendarScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var currentPage by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += offsetChange
    }

    val pdfState by produceState(
        initialValue = PdfRenderState(),
        key1 = currentPage
    ) {
        value = withContext(Dispatchers.IO) {
            renderPdfPage(
                context = context,
                assetPath = Calendar2026AssetPath,
                pageIndex = currentPage
            )
        }
    }

    LaunchedEffect(currentPage) {
        scale = 1f
        offset = Offset.Zero
    }

    LaunchedEffect(pdfState.pageCount) {
        val maxPageIndex = (pdfState.pageCount - 1).coerceAtLeast(0)
        if (currentPage > maxPageIndex) {
            currentPage = maxPageIndex
        }
    }

    ProfessionalPageScaffold(
        title = "Academic Calendar 2026",
        subtitle = "View the full calendar in a cleaner reading layout, zoom in for detail, then save the original PDF when needed.",
        onBackClick = onBackClick,
        reserveBottomBarSpace = true,
        actionLabel = "Download PDF",
        onActionClick = { downloadPdf(context, Calendar2026AssetPath) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppInfoStrip(label = "Pages", value = pdfState.pageCount.coerceAtLeast(1).toString())
            AppInfoStrip(label = "Zoom", value = "${(scale * 100).toInt()}%")
        }

        Spacer(modifier = Modifier.height(18.dp))

        AppPanelCard {
            AppSectionTitle(
                title = "Viewer Controls",
                subtitle = "Use buttons or pinch gestures for detailed reading."
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppSecondaryButton(
                    text = "Zoom Out",
                    onClick = {
                        scale = (scale - 0.25f).coerceIn(1f, 5f)
                        if (scale == 1f) offset = Offset.Zero
                    },
                    modifier = Modifier.weight(1f)
                )
                AppSecondaryButton(
                    text = "Reset",
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    modifier = Modifier.weight(1f)
                )
                AppPrimaryButton(
                    text = "Zoom In",
                    onClick = { scale = (scale + 0.25f).coerceIn(1f, 5f) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AppPanelCard {
            AppSectionTitle(title = "Calendar Preview")
            Spacer(modifier = Modifier.height(16.dp))

            when {
                pdfState.error != null -> {
                    Text(
                        text = pdfState.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                pdfState.pageBitmap != null -> {
                    val aspectRatio = if (pdfState.bitmapHeight == 0) {
                        0.72f
                    } else {
                        pdfState.bitmapWidth.toFloat() / pdfState.bitmapHeight.toFloat()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .aspectRatio(aspectRatio)
                            .transformable(state = transformState)
                    ) {
                        pdfState.pageBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Calendar page ${currentPage + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppSecondaryButton(
                            text = "Previous",
                            onClick = { if (currentPage > 0) currentPage -= 1 },
                            modifier = Modifier.weight(1f),
                            enabled = currentPage > 0
                        )
                        Text(
                            text = "Page ${currentPage + 1} / ${pdfState.pageCount.coerceAtLeast(1)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        AppSecondaryButton(
                            text = "Next",
                            onClick = {
                                if (currentPage < pdfState.pageCount - 1) {
                                    currentPage += 1
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = currentPage < pdfState.pageCount - 1
                        )
                    }
                }

                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading calendar preview...")
                    }
                }
            }
        }
    }
}

private fun renderPdfPage(
    context: Context,
    assetPath: String,
    pageIndex: Int
): PdfRenderState {
    return try {
        val pdfFile = copyAssetToCache(context, assetPath)
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fileDescriptor ->
            PdfRenderer(fileDescriptor).use { renderer ->
                if (renderer.pageCount == 0) {
                    PdfRenderState(error = "PDF is empty.")
                } else {
                    val safePageIndex = pageIndex.coerceIn(0, renderer.pageCount - 1)
                    renderer.openPage(safePageIndex).use { page ->
                        val bitmap = createBitmap(
                            page.width * 2,
                            page.height * 2,
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        PdfRenderState(
                            bitmapWidth = bitmap.width,
                            bitmapHeight = bitmap.height,
                            pageBitmap = bitmap.asImageBitmap(),
                            pageCount = renderer.pageCount
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        PdfRenderState(error = e.message ?: "Unable to load PDF.")
    }
}

private fun copyAssetToCache(
    context: Context,
    assetPath: String
): File {
    val fileName = assetPath.substringAfterLast("/")
    val cachedFile = File(context.cacheDir, fileName)

    if (!cachedFile.exists()) {
        context.assets.open(assetPath).use { input ->
            cachedFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    return cachedFile
}

private fun downloadPdf(context: Context, assetPath: String) {
    try {
        val fileName = assetPath.substringAfterLast("/")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.assets.open(assetPath).use { input ->
                    context.contentResolver.openOutputStream(uri).use { output ->
                        if (output != null) {
                            input.copyTo(output)
                            Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            context.assets.open(assetPath).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
