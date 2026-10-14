package org.wastesort

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.wastesort.ui.ScanUiState
import org.wastesort.ui.ScanViewModel
import org.wastesort.ui.components.ResultOverlay
import org.wastesort.ui.theme.WasteSortTheme
import org.wastesort.vision.WasteClassifierClient
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WasteSortTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: ScanViewModel) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val state by viewModel.uiState.collectAsState()
    var showManualDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (hasCameraPermission) {
                CameraView(
                    onImageCaptured = { bitmap ->
                        viewModel.onFrameCaptured(bitmap)
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Reticle Overlay
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .align(Alignment.Center)
                        .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                )

                // Processing indicator
                if (state is ScanUiState.Analyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White,
                    )
                }

                // Result Bottom Sheet
                if (state is ScanUiState.ResultReady) {
                    val result = (state as ScanUiState.ResultReady).recommendation
                    ResultOverlay(
                        recommendation = result,
                        onRetake = { viewModel.resetScan() },
                        onManualFallback = { showManualDialog = true },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Camera Permission")
                    }
                }
            }

            // Manual Selection Dropdown
            if (showManualDialog) {
                DropdownMenu(
                    expanded = showManualDialog,
                    onDismissRequest = { showManualDialog = false },
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    WasteClassifierClient.CLASS_NAMES.forEach { className ->
                        DropdownMenuItem(
                            text = { Text(className.replace("_", " ")) },
                            onClick = {
                                showManualDialog = false
                                viewModel.onManualSelect(className)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraView(
    onImageCaptured: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, capture)
                    } catch (_: Exception) {
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Capture Floating Action Button
        FloatingActionButton(
            onClick = {
                imageCapture?.takePicture(
                    cameraExecutor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = image.toBitmap()
                            image.close()
                            onImageCaptured(bitmap)
                        }

                        override fun onError(exception: ImageCaptureException) {
                        }
                    },
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            containerColor = Color.White,
        ) {
            Text("Scan", color = Color.Black)
        }
    }
}
