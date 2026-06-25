package com.example.rotoshokand

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    CameraPermissionWrapper(cameraExecutor)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun CameraPermissionWrapper(cameraExecutor: ExecutorService) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    LaunchedEffect(key1 = true) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasPermission) {
        LensScannerScreen(cameraExecutor)
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission is required to scan book covers.", color = Color.White)
        }
    }
}

@Composable
fun LensScannerScreen(cameraExecutor: ExecutorService) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val serverIp = "https://blabber-garage-contempt.ngrok-free.dev"

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var statusText by remember { mutableStateOf("Align cover. Click shutter to parse text lines.") }
    var isScanning by remember { mutableStateOf(false) }
    var bookDownloadUrl by remember { mutableStateOf("") }
    var detectedTitle by remember { mutableStateOf("") }

    // Interactive Mode State Elements
    var detectedLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedQueryTerms by remember { mutableStateOf<List<String>>(emptyList()) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(this.surfaceProvider)
                        }
                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("CameraX", "Binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Targeting Overlay Box
        Box(
            modifier = Modifier.fillMaxSize().padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .border(BorderStroke(3.dp, if(detectedLines.isNotEmpty()) Color.Green else Color.Cyan), shape = RoundedCornerShape(12.dp))
            )
        }

        // Top Header Dashboard Info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(statusText, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                if (selectedQueryTerms.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Target Query: ${selectedQueryTerms.joinToString(" ")}", color = Color.Yellow, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Mid-screen Interactive Text Line Picker Drawer
        if (detectedLines.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(top = 260.dp)
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(8.dp)
            ) {
                Text("Tap clean Title/Author strings to add to search payload:", color = Color.LightGray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(detectedLines) { line ->
                        val isSelected = selectedQueryTerms.contains(line)
                        SuggestionChip(
                            onClick = {
                                selectedQueryTerms = if (isSelected) {
                                    selectedQueryTerms - line
                                } else {
                                    selectedQueryTerms + line
                                }
                            },
                            label = { Text(line) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) Color.Cyan else Color.DarkGray,
                                labelColor = if (isSelected) Color.Black else Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (selectedQueryTerms.isEmpty()) {
                            statusText = "⚠️ Please select at least one phrase item!"
                            return@Button
                        }
                        isScanning = true
                        statusText = "📡 Transmitting custom query selection..."

                        sendTextToNodeBackend(
                            text = selectedQueryTerms.joinToString(" "),
                            ip = serverIp,
                            onResponse = { title, url ->
                                detectedTitle = title
                                bookDownloadUrl = url
                                statusText = "✨ Match pulled successfully!"
                                isScanning = false
                            },
                            onFailure = { errorMsg ->
                                statusText = "❌ Failure: $errorMsg"
                                isScanning = false
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                ) {
                    Text("Search Selected Text", color = Color.Black)
                }
            }
        }

        // Bottom Dashboard Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (detectedTitle.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.9f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📚 Found: $detectedTitle", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        SelectionContainer {
                            Text(bookDownloadUrl, color = Color.Cyan, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shutter Click Button
                Button(
                    onClick = {
                        if (isScanning) return@Button
                        detectedLines = emptyList()
                        selectedQueryTerms = emptyList()
                        isScanning = true
                        statusText = "📸 Snapping image..."

                        val capture = imageCapture ?: return@Button
                        capture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                statusText = "🧠 Separating layout text fields..."
                                processImageProxyLayoutLines(
                                    imageProxy = imageProxy,
                                    onLinesParsed = { lines ->
                                        detectedLines = lines
                                        statusText = "💡 Lines mapped! Select Target Terms."
                                        isScanning = false
                                    },
                                    onFailure = {
                                        statusText = "❌ Failed to map structured text blocks."
                                        isScanning = false
                                    }
                                )
                            }

                            override fun onError(exception: ImageCaptureException) {
                                statusText = "❌ Photo capture failed"
                                isScanning = false
                            }
                        })
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(76.dp).border(4.dp, Color.White, CircleShape),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isScanning) Color.Red else Color.Cyan)
                ) {}

                // Reset Clear Button if terms are active
                if (detectedLines.isNotEmpty()) {
                    Button(
                        onClick = {
                            detectedLines = emptyList()
                            selectedQueryTerms = emptyList()
                            detectedTitle = ""
                            bookDownloadUrl = ""
                            statusText = "Clear slate. Ready for next scan."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("Reset")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
fun processImageProxyLayoutLines(
    imageProxy: ImageProxy,
    onLinesParsed: (List<String>) -> Unit,
    onFailure: () -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                imageProxy.close()
                val linesList = mutableListOf<String>()
                // Parse individual blocks down to text lines instead of smashing everything into one string
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val cleanLine = line.text.trim()
                        if (cleanLine.length > 2) {
                            linesList.add(cleanLine)
                        }
                    }
                }
                if (linesList.isNotEmpty()) {
                    onLinesParsed(linesList)
                } else {
                    onFailure()
                }
            }
            .addOnFailureListener {
                imageProxy.close()
                onFailure()
            }
    } else {
        imageProxy.close()
        onFailure()
    }
}

fun sendTextToNodeBackend(text: String, ip: String, onResponse: (String, String) -> Unit, onFailure: (String) -> Unit) {
    val client = OkHttpClient()
    val json = JSONObject().put("ocrText", text)
    val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

    val request = Request.Builder()
        .url("$ip/search-book")
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onFailure("Connection dropped. Is Node running?")
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    onFailure("HTTP Error Code: ${response.code}")
                    return
                }
                val resData = response.body?.string() ?: ""
                try {
                    val jsonObj = JSONObject(resData)
                    val title = jsonObj.optString("title", "Matched Book")
                    val url = jsonObj.optString("downloadUrl", "")
                    onResponse(title, url)
                } catch (e: Exception) {
                    onFailure("Payload processing mismatch.")
                }
            }
        }
    })
}