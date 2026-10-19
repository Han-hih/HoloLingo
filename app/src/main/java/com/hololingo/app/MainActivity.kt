package com.hololingo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.hololingo.app.ui.theme.HoloLingoTheme
import com.hololingo.core.data.AnswerEvaluator
import com.hololingo.core.data.InMemoryConceptRepository
import com.hololingo.core.recognition.MlKitHandwritingRecognizer
import com.hololingo.feature.practice.PracticeRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HoloLingoTheme {
                val repository = remember { InMemoryConceptRepository() }
                val recognizer = remember { MlKitHandwritingRecognizer() }
                val evaluator = remember { AnswerEvaluator() }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        PracticeRoute(
                            repository = repository,
                            recognizer = recognizer,
                            evaluator = evaluator,
                        )
                    }
                }
            }
        }
    }
}
