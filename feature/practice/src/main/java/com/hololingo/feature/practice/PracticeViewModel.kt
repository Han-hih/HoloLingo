package com.hololingo.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hololingo.core.data.AnswerEvaluator
import com.hololingo.core.data.ConceptRepository
import com.hololingo.core.model.EvaluationResult
import com.hololingo.core.model.HandwritingInk
import com.hololingo.core.model.HologramScene
import com.hololingo.core.model.InkStroke
import com.hololingo.core.model.LearningLanguage
import com.hololingo.core.model.LearningQuestion
import com.hololingo.core.recognition.HandwritingRecognizer
import com.hololingo.core.recognition.ModelPreparationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PracticeViewModel(
    private val repository: ConceptRepository,
    private val recognizer: HandwritingRecognizer,
    private val evaluator: AnswerEvaluator,
) : ViewModel() {
    private val initialConcept = requireNotNull(repository.getConcepts().firstOrNull())

    private val _uiState = MutableStateFlow(
        PracticeUiState(
            language = LearningLanguage.KOREAN,
            question = LearningQuestion(initialConcept, LearningLanguage.KOREAN),
            supportedLanguages = repository.getSupportedLanguages(),
        ),
    )
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    init {
        prepareModel(LearningLanguage.KOREAN)
    }

    fun selectLanguage(language: LearningLanguage) {
        if (language == _uiState.value.language || _uiState.value.isRecognizing) return
        _uiState.update {
            it.copy(
                language = language,
                question = LearningQuestion(initialConcept, language),
                strokes = emptyList(),
                result = null,
                recognizedText = null,
                scene = null,
                modelState = ModelState.Downloading,
            )
        }
        prepareModel(language)
    }

    fun addStroke(stroke: InkStroke) {
        if (stroke.points.isEmpty()) return
        _uiState.update {
            it.copy(
                strokes = it.strokes + stroke,
                result = null,
                recognizedText = null,
                scene = null,
            )
        }
    }

    fun clearInk() {
        _uiState.update {
            it.copy(
                strokes = emptyList(),
                result = null,
                recognizedText = null,
                scene = null,
            )
        }
    }

    fun recognize() {
        val state = _uiState.value
        if (state.strokes.isEmpty() || state.isRecognizing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRecognizing = true, result = null) }
            runCatching {
                when (recognizer.prepare(state.language)) {
                    ModelPreparationResult.Ready -> Unit
                    is ModelPreparationResult.Failed -> error("언어 모델을 준비하지 못했습니다.")
                }
                recognizer.recognize(HandwritingInk(state.strokes), state.language)
            }.onSuccess { candidates ->
                val evaluation = evaluator.evaluate(state.question, candidates)
                val scene = if (evaluation is EvaluationResult.Correct) {
                    repository.getScene(state.question.concept.sceneId)
                } else {
                    null
                }
                _uiState.update {
                    it.copy(
                        isRecognizing = false,
                        recognizedText = candidates.firstOrNull()?.text,
                        result = evaluation,
                        scene = scene,
                        modelState = ModelState.Ready,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isRecognizing = false,
                        modelState = ModelState.Error(throwable.message ?: "인식 중 오류가 발생했습니다."),
                    )
                }
            }
        }
    }

    private fun prepareModel(language: LearningLanguage) {
        viewModelScope.launch {
            _uiState.update { it.copy(modelState = ModelState.Downloading) }
            val result = recognizer.prepare(language)
            _uiState.update {
                if (it.language != language) {
                    it
                } else {
                    it.copy(
                        modelState = when (result) {
                            ModelPreparationResult.Ready -> ModelState.Ready
                            is ModelPreparationResult.Failed -> ModelState.Error(
                                result.cause.message ?: "언어 모델 다운로드에 실패했습니다.",
                            )
                        },
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            repository: ConceptRepository,
            recognizer: HandwritingRecognizer,
            evaluator: AnswerEvaluator,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PracticeViewModel(repository, recognizer, evaluator) as T
        }
    }
}

data class PracticeUiState(
    val language: LearningLanguage,
    val question: LearningQuestion,
    val supportedLanguages: List<LearningLanguage>,
    val strokes: List<InkStroke> = emptyList(),
    val recognizedText: String? = null,
    val result: EvaluationResult? = null,
    val scene: HologramScene? = null,
    val modelState: ModelState = ModelState.Downloading,
    val isRecognizing: Boolean = false,
)

sealed interface ModelState {
    data object Downloading : ModelState
    data object Ready : ModelState
    data class Error(val message: String) : ModelState
}
