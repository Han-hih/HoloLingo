package com.hololingo.core.data

import com.hololingo.core.model.ConceptId
import com.hololingo.core.model.EvaluationResult
import com.hololingo.core.model.LearningLanguage
import com.hololingo.core.model.LearningQuestion
import com.hololingo.core.model.RecognitionCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerEvaluatorTest {
    private val repository = InMemoryConceptRepository()
    private val evaluator = AnswerEvaluator()

    @Test
    fun exactEnglishAnswerIsCorrect() {
        val question = LearningQuestion(repository.getConcepts().first(), LearningLanguage.ENGLISH)

        val result = evaluator.evaluate(
            question,
            listOf(RecognitionCandidate("Mountain", "mountain")),
        )

        assertEquals(EvaluationResult.Correct(ConceptId("MOUNTAIN")), result)
    }

    @Test
    fun oneCharacterTypoIsAlmostCorrect() {
        val question = LearningQuestion(repository.getConcepts().first(), LearningLanguage.ENGLISH)

        val result = evaluator.evaluate(
            question,
            listOf(RecognitionCandidate("mountin", "mountin")),
        )

        assertTrue(result is EvaluationResult.AlmostCorrect)
    }
}
