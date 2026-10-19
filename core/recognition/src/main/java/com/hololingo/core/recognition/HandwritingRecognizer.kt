package com.hololingo.core.recognition

import com.hololingo.core.model.HandwritingInk
import com.hololingo.core.model.LearningLanguage
import com.hololingo.core.model.RecognitionCandidate

interface HandwritingRecognizer {
    suspend fun prepare(language: LearningLanguage): ModelPreparationResult

    suspend fun recognize(
        ink: HandwritingInk,
        language: LearningLanguage,
    ): List<RecognitionCandidate>
}

sealed interface ModelPreparationResult {
    data object Ready : ModelPreparationResult
    data class Failed(val cause: Throwable) : ModelPreparationResult
}
