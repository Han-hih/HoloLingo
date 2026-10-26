package com.hololingo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.hololingo.app.ui.theme.HoloLingoTheme
import com.hololingo.core.connectivity.NearbyConnectionManager
import com.hololingo.core.data.AnswerEvaluator
import com.hololingo.core.data.InMemoryConceptRepository
import com.hololingo.core.recognition.MlKitHandwritingRecognizer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HoloLingoTheme {
                val repository = remember { InMemoryConceptRepository() }
                val recognizer = remember { MlKitHandwritingRecognizer() }
                val evaluator = remember { AnswerEvaluator() }
                val connectionManager = remember { NearbyConnectionManager(applicationContext) }

                HoloLingoApp(
                    repository = repository,
                    recognizer = recognizer,
                    evaluator = evaluator,
                    connectionManager = connectionManager,
                )
            }
        }
    }
}
