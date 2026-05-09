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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.kreutzm.gemma4test.model.GemmaModelConfig
import de.kreutzm.gemma4test.ui.theme.Gemma4TestTheme

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
    var status by remember { mutableStateOf("Bereit für Implementierung: Model-Download, Fotoaufnahme und LiteRT-LM-Inferenz.") }
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
                }
            }

            Button(onClick = { status = "TODO: ModelDownloader implementieren und Fortschritt anzeigen." }) {
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
