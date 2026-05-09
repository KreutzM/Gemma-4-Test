package de.kreutzm.gemma4test

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    var downloadState by remember { mutableStateOf<ModelDownloadState>(ModelDownloadState.Idle) }
    var status by remember { mutableStateOf("Bereit: Modell laden, Foto aufnehmen und später lokal mit LiteRT-LM beschreiben.") }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        status = if (granted) {
            "Kamera-Berechtigung erteilt. Nächster Schritt: Fotoaufnahme mit ActivityResultContracts.TakePicturePreview oder CameraX."
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

            Button(
                enabled = downloadState !is ModelDownloadState.Downloading && downloadState !is ModelDownloadState.Starting,
                onClick = {
                    coroutineScope.launch {
                        val request = ModelDownloadRequest.gemma4E2B()
                        val downloader = ModelDownloader(ModelFileStore.fromContext(context))
                        downloader.download(request) { state ->
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
            Button(onClick = { status = "TODO: LiteRT-LM Engine initialisieren, Bild als PNG-Bytes + Prompt senden." }) {
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
