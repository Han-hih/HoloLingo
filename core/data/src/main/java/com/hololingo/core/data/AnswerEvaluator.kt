package com.hololingo.core.data

import com.hololingo.core.model.EvaluationResult
import com.hololingo.core.model.LearningQuestion
import com.hololingo.core.model.RecognitionCandidate
import java.text.Normalizer
import java.util.Locale

class AnswerEvaluator {
    fun evaluate(
        question: LearningQuestion,
        candidates: List<RecognitionCandidate>,
    ): EvaluationResult {
        if (candidates.isEmpty()) return EvaluationResult.NotRecognized

        val expected = buildSet {
            add(normalize(question.answer.text))
            add(normalize(question.answer.normalizedText))
            question.answer.alternatives.mapTo(this) { normalize(it) }
        }

        val match = candidates.firstOrNull { normalize(it.text) in expected }
        if (match != null) return EvaluationResult.Correct(question.concept.id)

        val best = candidates.first().text
        return if (isNearMatch(normalize(best), normalize(question.answer.text))) {
            EvaluationResult.AlmostCorrect(question.answer.text, best)
        } else {
            EvaluationResult.Incorrect(candidates.map { it.text })
        }
    }

    fun normalize(value: String): String = Normalizer
        .normalize(value.trim(), Normalizer.Form.NFC)
        .lowercase(Locale.ROOT)
        .replace(WHITESPACE, " ")

    private fun isNearMatch(actual: String, expected: String): Boolean =
        actual.length >= 4 && expected.length >= 4 && levenshteinDistance(actual, expected) == 1

    private fun levenshteinDistance(left: String, right: String): Int {
        val previous = IntArray(right.length + 1) { it }
        for (leftIndex in left.indices) {
            var diagonal = previous[0]
            previous[0] = leftIndex + 1
            for (rightIndex in right.indices) {
                val above = previous[rightIndex + 1]
                previous[rightIndex + 1] = minOf(
                    previous[rightIndex + 1] + 1,
                    previous[rightIndex] + 1,
                    diagonal + if (left[leftIndex] == right[rightIndex]) 0 else 1,
                )
                diagonal = above
            }
        }
        return previous[right.length]
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
