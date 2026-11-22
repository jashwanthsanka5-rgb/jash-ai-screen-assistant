package com.jash.aiassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var question by remember { mutableStateOf("") }
                var answer by remember { mutableStateOf("Your practice answer will appear here.") }
                Scaffold { padding ->
                    Column(
                        modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Jash AI Screen Assistant", style = MaterialTheme.typography.headlineSmall)
                        Text("Practice and authorized mock-test mode")
                        OutlinedTextField(
                            value = question, onValueChange = { question = it },
                            label = { Text("Paste a practice question (English / Telugu)") },
                            modifier = Modifier.fillMaxWidth(), minLines = 5
                        )
                        Button(onClick = { answer = "Connect the configured backend to analyze this practice question." }) {
                            Text("Analyze practice question")
                        }
                        Text(answer, style = MaterialTheme.typography.titleLarge)
                        Text("This starter currently provides the interface only. OCR, backend networking, and overlay integration still need implementation and device testing.")
                    }
                }
            }
        }
    }
}
