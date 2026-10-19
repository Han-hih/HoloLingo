package com.hololingo.core.model

enum class LearningLanguage(
    val languageTag: String,
    val displayName: String,
    val shortLabel: String,
) {
    KOREAN("ko-KR", "한국어", "KO"),
    ENGLISH("en-US", "English", "EN"),
    JAPANESE("ja-JP", "日本語", "JA"),
    CHINESE("zh-CN", "中文", "ZH"),
}

@JvmInline
value class ConceptId(val value: String)

@JvmInline
value class SceneId(val value: String)

data class Lexeme(
    val language: LearningLanguage,
    val text: String,
    val normalizedText: String,
    val pronunciation: String? = null,
    val alternatives: List<String> = emptyList(),
)

data class Concept(
    val id: ConceptId,
    val category: String,
    val meaning: String,
    val lexemes: List<Lexeme>,
    val sceneId: SceneId,
) {
    fun lexemeFor(language: LearningLanguage): Lexeme? =
        lexemes.firstOrNull { it.language == language }
}

data class HologramScene(
    val id: SceneId,
    val modelAsset: String,
    val appearAnimation: String? = null,
    val loopAnimation: String? = null,
    val scale: Float = 1f,
)

data class LearningQuestion(
    val concept: Concept,
    val language: LearningLanguage,
) {
    val answer: Lexeme
        get() = requireNotNull(concept.lexemeFor(language))
}

data class InkPoint(
    val x: Float,
    val y: Float,
    val timestampMillis: Long,
)

data class InkStroke(val points: List<InkPoint>)

data class HandwritingInk(val strokes: List<InkStroke>) {
    val isEmpty: Boolean
        get() = strokes.none { it.points.isNotEmpty() }
}

data class RecognitionCandidate(
    val text: String,
    val normalizedText: String,
    val score: Float? = null,
)

sealed interface EvaluationResult {
    data class Correct(val conceptId: ConceptId) : EvaluationResult
    data class AlmostCorrect(
        val expected: String,
        val recognized: String,
    ) : EvaluationResult

    data class Incorrect(val candidates: List<String>) : EvaluationResult
    data object NotRecognized : EvaluationResult
}
