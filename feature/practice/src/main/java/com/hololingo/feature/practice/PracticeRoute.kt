package com.hololingo.feature.practice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hololingo.core.data.AnswerEvaluator
import com.hololingo.core.data.ConceptRepository
import com.hololingo.core.recognition.HandwritingRecognizer

@Composable
fun PracticeRoute(
    repository: ConceptRepository,
    recognizer: HandwritingRecognizer,
    evaluator: AnswerEvaluator,
) {
    val practiceViewModel: PracticeViewModel = viewModel(
        factory = PracticeViewModel.factory(repository, recognizer, evaluator),
    )
    val state by practiceViewModel.uiState.collectAsState()

    PracticeScreen(
        state = state,
        onLanguageSelected = practiceViewModel::selectLanguage,
        onStrokeCompleted = practiceViewModel::addStroke,
        onClear = practiceViewModel::clearInk,
        onRecognize = practiceViewModel::recognize,
    )
}
