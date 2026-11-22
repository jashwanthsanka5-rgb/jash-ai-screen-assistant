package com.jash.aiassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private var tts: TextToSpeech? = null
    private var latestLiveText by mutableStateOf("Start live sharing to read text visible on your screen.")
    private var sharing by mutableStateOf(false)

    private val liveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra(LiveCaptureService.EXTRA_TEXT)?.let { latestLiveText = it }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val serviceIntent = Intent(this, LiveCaptureService::class.java).apply {
                    putExtra(LiveCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(LiveCaptureService.EXTRA_RESULT_DATA, result.data)
                }
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
                sharing = true
            }
        }
        setContent {
            MaterialTheme {
                var importedText by remember { mutableStateOf(intent?.getStringExtra(Intent.EXTRA_TEXT) ?: "") }
                var status by remember { mutableStateOf("Ready") }
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    if (uri != null) {
                        status = "Reading selected image…"
                        try {
                            recognizer.process(InputImage.fromFilePath(this, uri))
                                .addOnSuccessListener { importedText = it.text; status = "Image text extracted. Review it below." }
                                .addOnFailureListener { status = "Image OCR failed: ${it.localizedMessage}" }
                        } catch (e: Exception) { status = "Could not open image: ${e.localizedMessage}" }
                    }
                }
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Jash AI", style = MaterialTheme.typography.headlineLarge)
                    Text("Live Screen Reader", style = MaterialTheme.typography.titleLarge)
                    Text("Share your screen with Android's permission dialog. The app reads text temporarily on-device; it does not save a video.")
                    Button(onClick = { captureLauncher.launch(projectionManager.createScreenCaptureIntent()) }, enabled = !sharing, modifier = Modifier.fillMaxWidth()) {
                        Text(if (sharing) "Screen sharing active" else "Start live screen sharing")
                    }
                    OutlinedButton(onClick = {
                        startService(Intent(this@MainActivity, LiveCaptureService::class.java).setAction(LiveCaptureService.ACTION_STOP))
                        sharing = false
                        status = "Screen sharing stopped."
                    }, enabled = sharing, modifier = Modifier.fillMaxWidth()) { Text("Stop sharing") }
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Latest visible text", style = MaterialTheme.typography.titleMedium)
                            Text(latestLiveText)
                            Text("Two-finger tap the panel below to read the latest text aloud.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 110.dp).pointerInput(latestLiveText) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var pointersSeen = 1
                                var finished = false
                                while (!finished) {
                                    val event = awaitPointerEvent()
                                    pointersSeen = maxOf(pointersSeen, event.changes.count { it.pressed })
                                    if (event.changes.none { it.pressed }) finished = true
                                }
                                if (pointersSeen >= 2) {
                                    val text = latestLiveText
                                    if (text.isNotBlank()) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jash-live-read")
                                }
                            }
                        },
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
                            Text("TWO-FINGER TAP TO READ ALOUD", Modifier.padding(24.dp), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Text("Image import (manual)", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("Choose screenshot or image") }
                    OutlinedTextField(value = importedText, onValueChange = { importedText = it }, label = { Text("Extracted / pasted text") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                    Text(status, style = MaterialTheme.typography.bodySmall)
                    Text("Live capture requires Android's visible sharing permission and ongoing notification. Two-finger gesture is supported inside this app's reader panel; it does not secretly intercept touches in other apps.")
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(LiveCaptureService.ACTION_TEXT)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(liveReceiver, filter, RECEIVER_NOT_EXPORTED) else @Suppress("DEPRECATION") registerReceiver(liveReceiver, filter)
    }

    override fun onStop() {
        try { unregisterReceiver(liveReceiver) } catch (_: Exception) {}
        super.onStop()
    }

    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault() }
    override fun onDestroy() {
        recognizer.close()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
