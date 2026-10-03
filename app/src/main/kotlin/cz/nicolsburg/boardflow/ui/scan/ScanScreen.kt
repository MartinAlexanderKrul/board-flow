package cz.nicolsburg.boardflow.ui.scan

import cz.nicolsburg.boardflow.ui.theme.BoardFlowColors
import cz.nicolsburg.boardflow.ui.common.BoardFlowSecondaryButton
import cz.nicolsburg.boardflow.ui.theme.BoardFlowShape
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import cz.nicolsburg.boardflow.AppViewModel
import cz.nicolsburg.boardflow.data.ScanQualityIssue
import cz.nicolsburg.boardflow.data.ScanImageQualityAnalyzer
import cz.nicolsburg.boardflow.ui.common.BoardFlowCameraActionPanel
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowCameraSecondaryAction
import cz.nicolsburg.boardflow.ui.common.BoardFlowCameraPermissionPrompt
import cz.nicolsburg.boardflow.ui.common.BoardFlowCameraScene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    viewModel: AppViewModel,
    gameName: String,
    onScoresExtracted: () -> Unit,
    onDiscard: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val loading   by viewModel.scanLoading.collectAsState()
    val streaming by viewModel.scanStreaming.collectAsState()
    val error     by viewModel.scanError.collectAsState()
    val play      by viewModel.extractedPlay.collectAsState()

    // Navigate when extraction succeeds; only sync players when AI returned them.
    // Manual-entry plays have players = emptyList() — we leave _editablePlayers alone so
    // session/play-again pre-fills survive the transition to LogPlayScreen.
    LaunchedEffect(play) {
        play?.let { extracted ->
            if (extracted.players.isNotEmpty()) {
                viewModel.initEditablePlayers(extracted.players)
            }
            onScoresExtracted()
        }
    }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // Temp file for camera capture
    val photoFile = remember {
        File(context.cacheDir, "score_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")
    }
    val photoUri = remember(photoFile) {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    }

    // CameraX image capture use-case
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var pendingPhoto by remember { mutableStateOf<File?>(null) }
    var qualityWarningIssues by remember { mutableStateOf<List<ScanQualityIssue>>(emptyList()) }
    var qualityAnalysisRunning by remember { mutableStateOf(false) }

    LaunchedEffect(pendingPhoto) {
        val file = pendingPhoto
        qualityWarningIssues = emptyList()
        if (file == null) {
            qualityAnalysisRunning = false
            return@LaunchedEffect
        }
        qualityAnalysisRunning = true
        val result = withContext(Dispatchers.IO) {
            ScanImageQualityAnalyzer.analyze(file)
        }
        if (pendingPhoto == file) {
            qualityWarningIssues = result.issues
            qualityAnalysisRunning = false
        }
    }

    val onEnterManually: () -> Unit = {
        viewModel.setExtractedPlayManual()
        onScoresExtracted()
    }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            if (file != null) pendingPhoto = file
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
            when {
                loading -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(if (streaming) "Reading response…" else "Sending to AI…")
                }

                error != null -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Scan failed",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    SelectionContainer {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                    BoardFlowButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Text("Try again from gallery")
                    }
                    BoardFlowSecondaryButton(onClick = onEnterManually) {
                        Text("Enter manually")
                    }
                }

                pendingPhoto != null -> {
                    pendingPhoto?.let { file ->
                        Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.weight(1f))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = BoardFlowShape.Sheet,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        "Use this photo?",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    AsyncImage(
                                        model = file,
                                        contentDescription = "Captured scoresheet preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 220.dp, max = 360.dp)
                                            .clip(BoardFlowShape.Control)
                                    )
                                    if (qualityAnalysisRunning) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                            Text(
                                                "Checking scan quality...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else if (qualityWarningIssues.isNotEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = BoardFlowColors.Warning,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                "This scan may be hard to read.",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = BoardFlowColors.Warning
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            qualityWarningIssues
                                                .map { it.userMessage }
                                                .distinct()
                                                .forEach { reason ->
                                                    Text(
                                                        reason,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                                        ) {
                                            // A poor photo: retaking is the recommended way on.
                                            BoardFlowSecondaryButton(
                                                onClick = {
                                                    viewModel.extractScores(file)
                                                    pendingPhoto = null
                                                }
                                            ) {
                                                Text("Use anyway")
                                            }
                                            BoardFlowButton(
                                                onClick = { pendingPhoto = null }
                                            ) {
                                                Text("Retake")
                                            }
                                        }
                                    } else {
                                        Text(
                                            "Retake if the sheet is cropped or blurry.\nUse photo to extract scores.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                                        ) {
                                            BoardFlowSecondaryButton(
                                                onClick = { pendingPhoto = null }
                                            ) {
                                                Text("Retake")
                                            }
                                            BoardFlowButton(
                                                onClick = {
                                                    viewModel.extractScores(file)
                                                    pendingPhoto = null
                                                },
                                                enabled = !qualityAnalysisRunning
                                            ) {
                                                Text("Use photo")
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    }
                }

                cameraPermission.status.isGranted -> {
                // Shared capture action used by both the preview tap and the FAB
                    val capturePhoto = {
                        if (!loading) {
                            imageCapture?.let { capture ->
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                capture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            pendingPhoto = photoFile
                                        }
                                        override fun onError(exc: ImageCaptureException) {}
                                    }
                                )
                            }
                        }
                    }

                    BoardFlowCameraScene(
                        title = gameName,
                        subtitle = "Line up the scoresheet, then tap anywhere or use the shutter.",
                        preview = {
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).also { previewView ->
                                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                        cameraProviderFuture.addListener({
                                            val cameraProvider = cameraProviderFuture.get()
                                            val preview = Preview.Builder().build().also {
                                                it.setSurfaceProvider(previewView.surfaceProvider)
                                            }
                                            val capture = ImageCapture.Builder()
                                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                                .build()
                                            imageCapture = capture

                                            try {
                                                cameraProvider.unbindAll()
                                                cameraProvider.bindToLifecycle(
                                                    lifecycleOwner,
                                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                                    preview,
                                                    capture
                                                )
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }, ContextCompat.getMainExecutor(ctx))
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { capturePhoto() }
                            )
                        },
                        bottomContent = {
                            BoardFlowCameraActionPanel(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 20.dp, vertical = 24.dp),
                                status = "Tap anywhere or use the shutter",
                                secondaryActions = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BoardFlowCameraSecondaryAction(
                                            icon = Icons.Default.Photo,
                                            label = "Gallery",
                                            onClick = { galleryLauncher.launch("image/*") }
                                        )
                                        // The shutter: the one amber control on the camera.
                                        Surface(
                                            onClick = { capturePhoto() },
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(72.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.CameraAlt,
                                                    contentDescription = "Capture",
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        BoardFlowCameraSecondaryAction(
                                            icon = Icons.Default.Edit,
                                            label = "Manual",
                                            onClick = onEnterManually
                                        )
                                    }
                                }
                            )
                        }
                    )

                }

                else -> BoardFlowCameraPermissionPrompt(
                    message = "Camera permission is needed to scan scoresheets.",
                    actions = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BoardFlowButton(onClick = { cameraPermission.launchPermissionRequest() }) {
                                Text("Allow camera")
                            }
                            BoardFlowSecondaryButton(onClick = { galleryLauncher.launch("image/*") }) {
                                Text("Pick from gallery instead")
                            }
                            BoardFlowSecondaryButton(onClick = onEnterManually) {
                                Text("Enter manually")
                            }
                        }
                    }
                )
            }
            } // end Box
        } // end Column
    }
}

/** Copy a content:// URI to a temp cache file so we can read it as a File. */
private fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val file = File(context.cacheDir, "gallery_score_${System.currentTimeMillis()}.jpg")
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { source ->
            file.outputStream().use { output -> source.copyTo(output) }
        }
        file
    } catch (e: Exception) {
        null
    }
}
