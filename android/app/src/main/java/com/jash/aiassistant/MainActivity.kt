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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.Executors

private val Navy = Color(0xFF080D20)
private val Panel = Color(0xFF141D38)
private val Electric = Color(0xFF4C8DFF)
private val Violet = Color(0xFF8B5CF6)
private val Muted = Color(0xFFADB9D5)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val network = Executors.newSingleThreadExecutor()
    private var tts: TextToSpeech? = null
    private var latestLiveText by mutableStateOf("Your live screen text will appear here after you start sharing.")
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
            } else {
                latestLiveText = "Screen-sharing permission was not granted. Tap Start Screen Reader and allow sharing."
            }
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Electric, secondary = Violet, background = Navy,
                    surface = Panel, onPrimary = Color.White, onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                var backendUrl by remember { mutableStateOf("") }
                var question by remember { mutableStateOf("") }
                var answer by remember { mutableStateOf("") }
                var answerNote by remember { mutableStateOf("") }
                var busy by remember { mutableStateOf(false) }
                var status by remember { mutableStateOf("Ready") }

                val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    if (uri != null) {
                        status = "Reading image…"
                        try {
                            recognizer.process(InputImage.fromFilePath(this, uri))
                                .addOnSuccessListener { result ->
                                    question = result.text
                                    status = if (result.text.isBlank()) "No readable text found in this image." else "Image text extracted. Review it, then submit as a practice question."
                                }
                                .addOnFailureListener { status = "Image reading failed: ${it.localizedMessage ?: "unknown error"}" }
                        } catch (e: Exception) {
                            status = "Could not open image: ${e.localizedMessage ?: "unknown error"}"
                        }
                    }
                }

                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color(0xFF080D20), Color(0xFF101A37), Color(0xFF17102F)))
                    )
                ) {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = RoundedCornerShape(16.dp), color = Electric.copy(alpha = .18f)) {
                                Text("⚡", Modifier.padding(horizontal = 15.dp, vertical = 10.dp), fontSize = 28.sp)
                            }
                            Column {
                                Text("JASH AI", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                                Text("SCREEN INTELLIGENCE", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                            }
                            Spacer(Modifier.weight(1f))
                            Surface(color = if (sharing) Color(0xFF145B4A) else Color(0xFF27314C), shape = RoundedCornerShape(30.dp)) {
                                Text(if (sharing) "LIVE" else "READY", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Your screen. Understood.", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Text("Read text, understand practice questions, and listen to explanations.", color = Muted, fontSize = 14.sp)
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = .96f)), shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("LIVE SCREEN READER", color = Electric, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Text("Share your screen", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text("Android will show its screen-sharing permission and a persistent notification while capture is active.", color = Muted, fontSize = 12.sp)
                                Button(
                                    onClick = { captureLauncher.launch(projectionManager.createScreenCaptureIntent()) },
                                    enabled = !sharing,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Electric)
                                ) { Text(if (sharing) "Screen sharing active" else "▶  Start Screen Sharing", fontWeight = FontWeight.Bold) }
                                OutlinedButton(
                                    onClick = {
                                        startService(Intent(this@MainActivity, LiveCaptureService::class.java).setAction(LiveCaptureService.ACTION_STOP))
                                        sharing = false
                                        status = "Screen sharing stopped."
                                    },
                                    enabled = sharing,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Stop Screen Sharing") }
                            }
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = .96f)), shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("LATEST SCREEN TEXT", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Text(latestLiveText, color = Color.White, fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { question = latestLiveText; status = "Screen text copied into the practice question box." }, modifier = Modifier.weight(1f)) { Text("Use this text") }
                                    OutlinedButton(onClick = { tts?.speak(latestLiveText, TextToSpeech.QUEUE_FLUSH, null, "jash-screen-text") }, modifier = Modifier.weight(1f)) { Text("🔊 Read aloud") }
                                }
                            }
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = .96f)), shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("PRACTICE QUESTION ASSISTANT", color = Electric, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                                Text("Ask Jash AI", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = backendUrl, onValueChange = { backendUrl = it },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                                    label = { Text("Backend HTTPS URL") },
                                    placeholder = { Text("https://your-api.onrender.com") }
                                )
                                OutlinedTextField(
                                    value = question, onValueChange = { question = it },
                                    modifier = Modifier.fillMaxWidth(), minLines = 4,
                                    label = { Text("Paste or review a practice question") },
                                    placeholder = { Text("Type a practice question here…") }
                                )
                                OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("▧  Read question from an image") }
                                Button(
                                    onClick = {
                                        val base = backendUrl.trim().trimEnd('/')
                                        val prompt = question.trim()
                                        if (!base.startsWith("https://")) {
                                            answer = ""
                                            answerNote = "Enter your deployed HTTPS backend URL first."
                                        } else if (prompt.isBlank()) {
                                            answerNote = "Enter or extract a practice question first."
                                        } else {
                                            busy = true
                                            answer = ""
                                            answerNote = "Thinking…"
                                            network.execute {
                                                var connection: HttpURLConnection? = null
                                                try {
                                                    connection = (URL("$base/api/practice/solve").openConnection() as HttpURLConnection).apply {
                                                        requestMethod = "POST"
                                                        connectTimeout = 15000
                                                        readTimeout = 30000
                                                        doOutput = true
                                                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                                                    }
                                                    val payload = JSONObject().put("question", prompt).put("choices", JSONArray())
                                                    connection.outputStream.use { it.write(payload.toString().toByteArray(StandardCharsets.UTF_8)) }
                                                    val code = connection.responseCode
                                                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                                                    val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                                                    val json = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                                                    runOnUiThread {
                                                        busy = false
                                                        if (code in 200..299) {
                                                            answer = json.optString("answer", "No answer returned.")
                                                            answerNote = json.optString("verification_note", "Review the explanation and verify important details.")
                                                        } else {
                                                            answerNote = json.optString("error", "Backend returned HTTP $code")
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    runOnUiThread { busy = false; answerNote = "Connection failed: ${e.localizedMessage ?: "check backend URL and internet"}" }
                                                } finally { connection?.disconnect() }
                                            }
                                        }
                                    },
                                    enabled = !busy,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Violet)
                                ) { Text(if (busy) "Working…" else "✦  Explain practice question", fontWeight = FontWeight.Bold) }
                                if (answer.isNotBlank() || answerNote.isNotBlank()) {
                                    Surface(color = Color(0xFF202B4B), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                            Text("JASH AI RESPONSE", color = Electric, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            if (answer.isNotBlank()) Text(answer, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            Text(answerNote, color = Muted, fontSize = 12.sp)
                                            if (answer.isNotBlank()) TextButton(onClick = { tts?.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "jash-answer") }) { Text("🔊 Read answer aloud") }
                                        }
                                    }
                                }
                            }
                        }

                        Text(status, color = Muted, fontSize = 11.sp)
                        Text("Privacy: capture is visible and permission-based. This build provides practice-question help inside the app; it does not create a hidden overlay or intercept gestures in other apps.", color = Muted, fontSize = 10.sp)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(LiveCaptureService.ACTION_TEXT)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(liveReceiver, filter, RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(liveReceiver, filter)
    }

    override fun onStop() {
        try { unregisterReceiver(liveReceiver) } catch (_: Exception) {}
        super.onStop()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
    }

    override fun onDestroy() {
        recognizer.close()
        tts?.stop()
        tts?.shutdown()
        network.shutdown()
        super.onDestroy()
    }
}
