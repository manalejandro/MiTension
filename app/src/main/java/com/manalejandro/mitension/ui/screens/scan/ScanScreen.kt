package com.manalejandro.mitension.ui.screens.scan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.manalejandro.mitension.R
import com.manalejandro.mitension.ui.components.BigButton
import com.manalejandro.mitension.ui.components.BigOutlinedButton
import com.manalejandro.mitension.ui.str
import com.manalejandro.mitension.util.BloodPressure
import com.manalejandro.mitension.util.MonitorTextParser
import com.manalejandro.mitension.util.ScanValues
import java.util.concurrent.Executors

private data class Detection(val values: ScanValues? = null, val stable: Boolean = false)

/**
 * Full screen camera scanner. It analyses preview frames with ML Kit, so no
 * photo is ever taken or stored. Detected values must be confirmed by the user.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onClose: () -> Unit,
    onValuesConfirmed: (ScanValues) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }
    var detection by remember { mutableStateOf(Detection()) }
    var showGuide by remember { mutableStateOf(false) }

    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val analyzer = remember {
        MonitorAnalyzer { result -> mainExecutor.execute { detection = result } }
    }
    DisposableEffect(analyzer) {
        onDispose { analyzer.close() }
    }

    // Re-check on resume so granting the permission from system settings
    // starts the camera without having to leave and reopen the scanner.
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                permissionGranted = granted
                if (granted) permissionDenied = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        permissionDenied = !granted
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val cameraActive = permissionGranted && !cameraFailed
    val canUseValues = cameraActive && detection.stable && detection.values != null

    // Never keep showing or accepting values from a camera that is no longer
    // active (permission denied, hardware failure, etc.).
    LaunchedEffect(cameraActive) {
        if (!cameraActive) detection = Detection()
    }

    BackHandler(onBack = onClose)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(str(R.string.scan_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = str(R.string.scan_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showGuide = true }) {
                        Icon(Icons.Filled.Info, contentDescription = str(R.string.scan_guide_expand))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GuideCard(onExpand = { showGuide = true })

            CameraArea(
                permissionGranted = permissionGranted,
                permissionDenied = permissionDenied,
                cameraFailed = cameraFailed,
                onPermissionRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onCameraResult = { failed -> cameraFailed = failed },
                analyzer = analyzer
            )

            DetectionStatus(detection = detection, cameraActive = cameraActive)

            ValuesRow(values = if (cameraActive) detection.values else null)

            BigButton(
                text = str(R.string.scan_use_values),
                onClick = { detection.values?.let(onValuesConfirmed) },
                enabled = canUseValues,
                icon = Icons.Filled.CheckCircle
            )
            BigOutlinedButton(
                text = str(R.string.scan_manual),
                onClick = onClose,
                icon = Icons.AutoMirrored.Filled.ArrowBack
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showGuide) {
        AlertDialog(
            onDismissRequest = { showGuide = false },
            title = { Text(str(R.string.scan_guide_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.omron_guide),
                        contentDescription = str(R.string.scan_guide_caption),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Text(str(R.string.scan_guide_text), style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showGuide = false }) {
                    Text(str(R.string.scan_guide_close))
                }
            }
        )
    }
}

@Composable
private fun GuideCard(onExpand: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.omron_guide),
                contentDescription = str(R.string.scan_guide_caption),
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    str(R.string.scan_guide_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    str(R.string.scan_guide_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onExpand) {
                    Text(str(R.string.scan_guide_expand))
                }
            }
        }
    }
}

@Composable
private fun CameraArea(
    permissionGranted: Boolean,
    permissionDenied: Boolean,
    cameraFailed: Boolean,
    onPermissionRequest: () -> Unit,
    onCameraResult: (failed: Boolean) -> Unit,
    analyzer: MonitorAnalyzer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { CameraController(context) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    DisposableEffect(controller) {
        onDispose { controller.stop() }
    }

    LaunchedEffect(permissionGranted, analyzer) {
        if (!permissionGranted) return@LaunchedEffect
        try {
            controller.start(lifecycleOwner, previewView, analyzer)
            onCameraResult(false)
        } catch (e: Exception) {
            Log.e("ScanScreen", "Could not start camera", e)
            onCameraResult(true)
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(androidx.compose.ui.graphics.Color.Black)
        ) {
            if (permissionGranted && !cameraFailed) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize(),
                    // Detach the reused PreviewView so re-entering this branch
                    // (for example after toggling the permission in Settings)
                    // does not fail with "already has a parent".
                    onRelease = { view -> (view.parent as? ViewGroup)?.removeView(view) }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 36.dp, vertical = 28.dp)
                    .border(
                        3.dp,
                        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                        RoundedCornerShape(18.dp)
                    )
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(50),
                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    str(R.string.scan_frame_hint),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(50),
                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    str(R.string.scan_reading_labels),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }

        if (permissionDenied) {
            Text(
                str(R.string.scan_permission_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                str(R.string.scan_permission_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = onPermissionRequest) {
                Text(str(R.string.scan_permission_button))
            }
            TextButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            ) {
                Text(str(R.string.scan_permission_settings))
            }
        }
        if (cameraFailed) {
            Text(
                str(R.string.scan_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DetectionStatus(detection: Detection, cameraActive: Boolean) {
    val ready = cameraActive && detection.stable && detection.values != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ready) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                !cameraActive -> Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ready -> Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                else -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
            Text(
                text = when {
                    !cameraActive -> str(R.string.scan_camera_inactive)
                    ready -> str(R.string.scan_stable)
                    detection.values != null -> str(R.string.scan_detecting)
                    else -> str(R.string.scan_searching)
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ValuesRow(values: ScanValues?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ValueBox(
            label = str(R.string.scan_label_sys),
            value = values?.let { BloodPressure.formatValue(it.systolic) },
            modifier = Modifier.weight(1f)
        )
        ValueBox(
            label = str(R.string.scan_label_dia),
            value = values?.let { BloodPressure.formatValue(it.diastolic) },
            modifier = Modifier.weight(1f)
        )
        ValueBox(
            label = str(R.string.scan_label_pul),
            value = values?.let { BloodPressure.formatValue(it.pulse) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ValueBox(label: String, value: String?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value ?: "—",
                style = MaterialTheme.typography.headlineSmall,
                color = if (value == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

/** Owns the CameraX lifecycle and feeds preview frames to the text analyzer. */
private class CameraController(private val context: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    private var provider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null

    suspend fun start(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer
    ) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).await()
        provider = cameraProvider

        val preview = Preview.Builder().build().also { previewUseCase ->
            previewUseCase.surfaceProvider = previewView.surfaceProvider
        }
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(executor, analyzer) }
        analysis = imageAnalysis

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis
        )
    }

    fun stop() {
        analysis?.clearAnalyzer()
        provider?.unbindAll()
        executor.shutdown()
    }
}

/** Recognizes digits in preview frames and only reports stable readings. */
private class MonitorAnalyzer(
    private val onDetection: (Detection) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastProcessedAt = 0L
    private var previous: ScanValues? = null
    private var repeats = 0

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        var closed = false
        fun close() {
            if (!closed) {
                closed = true
                imageProxy.close()
            }
        }

        try {
            val now = System.currentTimeMillis()
            if (now - lastProcessedAt < 400) {
                close()
                return
            }
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                close()
                return
            }
            lastProcessedAt = now
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val values = MonitorTextParser.parse(visionText.text)
                    if (values == null) {
                        previous = null
                        repeats = 0
                        onDetection(Detection())
                    } else {
                        repeats = if (values == previous) repeats + 1 else 1
                        previous = values
                        onDetection(Detection(values, repeats >= 2))
                    }
                }
                .addOnFailureListener { onDetection(Detection()) }
                .addOnCompleteListener { close() }
        } catch (e: Exception) {
            Log.e("ScanScreen", "Frame analysis failed", e)
            close()
        }
    }

    fun close() {
        recognizer.close()
    }
}
