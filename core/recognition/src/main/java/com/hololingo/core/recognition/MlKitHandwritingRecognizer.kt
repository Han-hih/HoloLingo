package com.hololingo.core.recognition

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.hololingo.core.model.HandwritingInk
import com.hololingo.core.model.LearningLanguage
import com.hololingo.core.model.RecognitionCandidate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MlKitHandwritingRecognizer : HandwritingRecognizer {
    private val modelManager = RemoteModelManager.getInstance()

    override suspend fun prepare(language: LearningLanguage): ModelPreparationResult = runCatching {
        val model = modelFor(language)
        if (!modelManager.isModelDownloaded(model).await()) {
            modelManager.download(model, DownloadConditions.Builder().build()).await()
        }
        ModelPreparationResult.Ready
    }.getOrElse(ModelPreparationResult::Failed)

    override suspend fun recognize(
        ink: HandwritingInk,
        language: LearningLanguage,
    ): List<RecognitionCandidate> {
        if (ink.isEmpty) return emptyList()

        val model = modelFor(language)
        val recognizer = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build(),
        )

        return try {
            recognizer.recognize(ink.toMlKitInk()).await().candidates.map { candidate ->
                RecognitionCandidate(
                    text = candidate.text,
                    normalizedText = candidate.text,
                    score = candidate.score,
                )
            }
        } finally {
            recognizer.close()
        }
    }

    private fun modelFor(language: LearningLanguage): DigitalInkRecognitionModel {
        val identifier = requireNotNull(
            DigitalInkRecognitionModelIdentifier.fromLanguageTag(language.languageTag),
        ) { "Unsupported language: ${language.languageTag}" }
        return DigitalInkRecognitionModel.builder(identifier).build()
    }

    private fun HandwritingInk.toMlKitInk(): Ink {
        val inkBuilder = Ink.builder()
        strokes.filter { it.points.isNotEmpty() }.forEach { stroke ->
            val strokeBuilder = Ink.Stroke.builder()
            stroke.points.forEach { point ->
                strokeBuilder.addPoint(Ink.Point.create(point.x, point.y, point.timestampMillis))
            }
            inkBuilder.addStroke(strokeBuilder.build())
        }
        return inkBuilder.build()
    }

    private suspend fun <T> Task<T>.await(): T = suspendCoroutine { continuation ->
        addOnSuccessListener(continuation::resume)
        addOnFailureListener(continuation::resumeWithException)
    }
}
