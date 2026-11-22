package com.jash.aiassistant

import android.net.Uri
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var question by remember { mutableStateOf(intent?.getStringExtra(Intent.EXTRA_TEXT) ?: "") }
                var apiBase by remember { mutableStateOf("http://10.0.2.2:8787") }
                var answer by remember { mutableStateOf("Import a practice-question image or paste text below.") }
                var busy by remember { mutableStateOf(false) }
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    if (uri != null) {
                        busy = true
                        try {
                            val image = InputImage.fromFilePath(this, uri)
                            recognizer.process(image)
                                .addOnSuccessListener { result ->
                                    question = result.text
                                    answer = if (result.text.isBlank()) "No readable English text found. You can paste Telugu text manually." else "OCR complete. Review the extracted text before analyzing."
                                    busy = false
                                }
                                .addOnFailureListener { e ->
                                    answer = "OCR failed: ${e.localizedMessage ?: "unknown error"}"
                                    busy = false
                                }
                        } catch (e: Exception) {
                            answer = "Could not open image: ${e.localizedMessage}"
                            busy = false
                        }
                    }
                }
                Scaffold { padding ->
                    Column(
                        modifier = Modifier.padding(padding).fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Jash AI Screen Assistant", style = MaterialTheme.typography.headlineSmall)
                        Text("Ask about text and questions you choose to share", style = MaterialTheme.typography.titleMedium)
                        Text("Import a screenshot, share selected text from another app, or paste a question. You control what is sent for analysis.")
                        OutlinedTextField(
                            value = apiBase, onValueChange = { apiBase = it.trimEnd('/') },
                            label = { Text("Backend URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text("Import question screenshot")
                        }
                        Text("Current OCR supports printed English. Telugu text can be pasted or shared; Telugu-script image OCR is still being added.")
                        OutlinedTextField(
                            value = question, onValueChange = { question = it },
                            label = { Text("Practice question / OCR text") },
                            modifier = Modifier.fillMaxWidth(), minLines = 5
                        )
                        Button(
                            onClick = {
                                if (question.isBlank() || busy) {
                                    answer = "Enter or import a practice question first."
                                } else {
                                    busy = true
                                    answer = "Analyzing practice question…"
                                    val endpoint = apiBase.trim().trimEnd('/') + "/api/practice/solve"
                                    val prompt = question
                                    worker.execute {
                                        val result = try {
                                            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                                                requestMethod = "POST"
                                                connectTimeout = 12000
                                                readTimeout = 30000
                                                doOutput = true
                                                setRequestProperty("Content-Type", "application/json")
                                            }
                                            OutputStreamWriter(connection.outputStream).use {
                                                it.write(JSONObject().put("question", prompt).toString())
                                            }
                                            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                                            val body = stream.bufferedReader().use { it.readText() }
                                            if (connection.responseCode !in 200..299) {
                                                "Server error: ${JSONObject(body).optString("error", "HTTP ${connection.responseCode}")}"
                                            } else {
                                                val json = JSONObject(body)
                                                "Answer: ${json.optString("answer", "UNCERTAIN")}\nConfidence: ${json.optDouble("confidence", 0.0)}\nNote: ${json.optString("verification_note", "")}"
                                            }.also { connection.disconnect() }
                                        } catch (e: Exception) {
                                            "Could not reach backend. Check the URL and server. ${e.localizedMessage ?: ""}"
                                        }
                                        runOnUiThread { answer = result; busy = false }
                                    }
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (busy) "Working…" else "Analyze practice question") }
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Result", style = MaterialTheme.typography.titleMedium)
                                Text(answer)
                            }
                        }
                        Text("This version analyzes content you explicitly import or share. It does not silently monitor other apps. Always verify AI output.")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        recognizer.close()
        worker.shutdown()
        super.onDestroy()
    }
}
