package de.kreutzm.gemma4test

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.kreutzm.gemma4test.image.ImagePreprocessor
import de.kreutzm.gemma4test.inference.GemmaInferenceState
import de.kreutzm.gemma4test.inference.GemmaVisionEngine
import de.kreutzm.gemma4test.model.GemmaModelConfig
import de.kreutzm.gemma4test.model.ModelDownloadRequest
import de.kreutzm.gemma4test.model.ModelDownloadState
import de.kreutzm.gemma4test.model.ModelDownloader
import de.kreutzm.gemma4test.model.ModelFileStore
import de.kreutzm.gemma4test.ui.theme.Gemma4TestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Gemma4TestTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GemmaMvpScreen()
                }
            }
        }
    }
}

@Composable
private fun GemmaMvpScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val modelRequest = remember { ModelDownloadRequest.gemma4E2B() }
    val modelFileStore = remember { ModelFileStore.fromContext(context) }
    var downloadState by remember { mutableStateOf<ModelDownloadState>(ModelDownloadState.Idle) }
    var inferenceState by remember { mutableStateOf<GemmaInferenceState>(GemmaInferenceState.Idle) }
    var status by remember { mutableStateOf("Bereit: Modell laden, Foto aufnehmen und lokal mit LiteRT-LM beschreiben.") }
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedPngBytes by remember { mutableStateOf<ByteArray?>(null) }
    var descriptionText by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        if (bitmap == null) {
            status = "Kein Foto aufgenommen."
            return@rememberLauncherForActivityResult
        }
        val scaledBitmap = ImagePreprocessor.scaleToMaxLongEdge(bitmap)
        val pngBytes = ImagePreprocessor.toPngBytes(scaledBitmap)
        capturedBitmap = scaledBitmap
        processedPngBytes = pngBytes
        descriptionText = ""
        inferenceState = GemmaInferenceState.Idle
        status = "Foto vorbereitet: ${scaledBitmap.width} x ${scaledBitmap.height} px, ${pngBytes.size} PNG-Bytes."
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraPermissionGranted = granted
        status = if (granted) {
            "Kamera-Berechtigung erteilt. Du kannst jetzt ein Foto aufnehmen."
        } else {
            "Kamera-Berechtigung verweigert. Ohne Kamera kann der MVP kein Foto aufnehmen."
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Gemma 4 E2B Bildbeschreibung",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Offline-MVP für Samsung S23+: Modell einmal laden, Foto aufnehmen, Bild lokal beschreiben.",
                style = MaterialTheme.typography.bodyLarge,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Modelldatei", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(GemmaModelConfig.fileName)
                    Text("${GemmaModelConfig.sizeBytes} Bytes")
                    Text("Status: ${downloadState.toUiText()}")
                }
            }

            capturedBitmap?.let { bitmap ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Foto", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Aufgenommenes Foto",
                            modifier = Modifier
                                .fillMaxWidth()
                                .sizeIn(maxHeight = 280.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Text("Vorbereitet: ${bitmap.width} x ${bitmap.height} px, ${processedPngBytes?.size ?: 0} PNG-Bytes")
                    }
                }
            }

            if (descriptionText.isNotBlank() || inferenceState !is GemmaInferenceState.Idle) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Beschreibung", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(inferenceState.toUiText())
                        if (descriptionText.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(descriptionText)
                        }
                    }
                }
            }

            Button(
                enabled = downloadState !is ModelDownloadState.Downloading && downloadState !is ModelDownloadState.Starting,
                onClick = {
                    coroutineScope.launch {
                        val downloader = ModelDownloader(modelFileStore)
                        downloader.download(modelRequest) { state ->
                            withContext(Dispatchers.Main) {
                                downloadState = state
                                status = state.toUiText()
                            }
                        }
                    }
                },
            ) {
                Text("Modell laden")
            }
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Kamera-Berechtigung anfordern")
            }
            Button(
                enabled = cameraPermissionGranted,
                onClick = { cameraLauncher.launch(null) },
            ) {
                Text("Foto aufnehmen")
            }
            Button(
                enabled = processedPngBytes != null && inferenceState !is GemmaInferenceState.Running && inferenceState !is GemmaInferenceState.Initializing,
                onClick = {
                    val imageBytes = processedPngBytes ?: return@Button
                    coroutineScope.launch {
                        val modelFile = modelFileStore.modelFile(modelRequest)
                        if (!modelFileStore.hasCompleteModel(modelRequest)) {
                            inferenceState = GemmaInferenceState.Failed("Modell ist noch nicht vollständig geladen.")
                            status = inferenceState.toUiText()
                            return@launch
                        }

                        inferenceState = GemmaInferenceState.Initializing
                        descriptionText = ""
                        status = inferenceState.toUiText()

                        GemmaVisionEngine(
                            context = context.applicationContext,
                            modelPath = modelFile.absolutePath,
                        ).use { engine ->
                            val initResult = engine.initialize()
                            if (initResult.isFailure) {
                                val message = initResult.exceptionOrNull()?.message ?: "Initialisierung fehlgeschlagen"
                                inferenceState = GemmaInferenceState.Failed(message)
                                status = inferenceState.toUiText()
                                return@use
                            }

                            inferenceState = GemmaInferenceState.Running
                            status = inferenceState.toUiText()
                            val result = engine.describeImage(imageBytes) { partialText ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    descriptionText = partialText
                                    inferenceState = GemmaInferenceState.Streaming(partialText)
                                }
                            }
                            result.fold(
                                onSuccess = { text ->
                                    descriptionText = text
                                    inferenceState = GemmaInferenceState.Completed(text)
                                    status = "Beschreibung abgeschlossen."
                                },
                                onFailure = { throwable ->
                                    val message = throwable.message ?: throwable::class.java.simpleName
                                    inferenceState = GemmaInferenceState.Failed(message)
                                    status = inferenceState.toUiText()
                                },
                            )
                        }
                    }
                },
            ) {
                Text("Bild beschreiben")
            }

            Text(text = status, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun ModelDownloadState.toUiText(): String = when (this) {
    ModelDownloadState.Idle -> "Noch nicht geladen"
    ModelDownloadState.Starting -> "Download wird vorbereitet"
    is ModelDownloadState.AlreadyAvailable -> "Bereits vorhanden: $sizeBytes Bytes"
    is ModelDownloadState.Downloading -> "Download: $progressPercent % ($downloadedBytes / $totalBytes Bytes)"
    is ModelDownloadState.Completed -> "Download abgeschlossen: $sizeBytes Bytes"
    is ModelDownloadState.Failed -> "Download fehlgeschlagen: $message"
}

private fun GemmaInferenceState.toUiText(): String = when (this) {
    GemmaInferenceState.Idle -> "Noch nicht gestartet"
    GemmaInferenceState.Initializing -> "LiteRT-LM wird initialisiert"
    GemmaInferenceState.Running -> "Beschreibung läuft"
    is GemmaInferenceState.Streaming -> "Beschreibung wird erzeugt"
    is GemmaInferenceState.Completed -> "Beschreibung abgeschlossen"
    is GemmaInferenceState.Failed -> "Inferenz fehlgeschlagen: $message"
}
