package com.hololingo.feature.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hololingo.core.model.EvaluationResult
import com.hololingo.core.model.InkPoint
import com.hololingo.core.model.InkStroke
import com.hololingo.core.model.LearningLanguage

private val Ink = Color(0xFF182225)
private val Mint = Color(0xFF45D6A8)
private val DeepGreen = Color(0xFF0B3B33)
private val Paper = Color(0xFFF4F1E8)
private val Muted = Color(0xFF64706E)

@Composable
fun PracticeScreen(
    state: PracticeUiState,
    onLanguageSelected: (LearningLanguage) -> Unit,
    onStrokeCompleted: (InkStroke) -> Unit,
    onClear: () -> Unit,
    onRecognize: () -> Unit,
) {
    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            BrandHeader()
            Spacer(Modifier.height(24.dp))
            LanguageSelector(
                selected = state.language,
                languages = state.supportedLanguages,
                onSelected = onLanguageSelected,
            )
            Spacer(Modifier.height(24.dp))
            QuestionCard(state)
            Spacer(Modifier.height(16.dp))
            WritingPad(
                strokes = state.strokes,
                enabled = !state.isRecognizing,
                onStrokeCompleted = onStrokeCompleted,
            )
            Spacer(Modifier.height(12.dp))
            ModelStatus(state.modelState)
            Spacer(Modifier.height(12.dp))
            ActionRow(
                canRecognize = state.strokes.isNotEmpty() && !state.isRecognizing,
                isRecognizing = state.isRecognizing,
                onClear = onClear,
                onRecognize = onRecognize,
            )
            AnimatedVisibility(state.result != null) {
                ResultPanel(state)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(DeepGreen, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("W", color = Mint, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "HOLOLINGO",
                color = Ink,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "손끝에서 시작되는 언어의 세계",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LanguageSelector(
    selected: LearningLanguage,
    languages: List<LearningLanguage>,
    onSelected: (LearningLanguage) -> Unit,
) {
    Column {
        Text("학습 언어", color = Muted, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            languages.forEach { language ->
                val isSelected = language == selected
                Surface(
                    onClick = { onSelected(language) },
                    color = if (isSelected) DeepGreen else Color.White.copy(alpha = 0.72f),
                    contentColor = if (isSelected) Color.White else Ink,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                    ) {
                        Text(language.shortLabel, fontWeight = FontWeight.Bold)
                        Text(language.displayName, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(state: PracticeUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DeepGreen),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(22.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "오늘의 개념 · ${state.question.concept.category}",
                    color = Mint,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "‘${state.question.answer.text}’을(를) 써보세요",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                )
                state.question.answer.pronunciation?.let {
                    Text(it, color = Color.White.copy(alpha = 0.66f))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    state.question.concept.meaning,
                    color = Color.White.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Mint.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("山", color = Mint, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun WritingPad(
    strokes: List<InkStroke>,
    enabled: Boolean,
    onStrokeCompleted: (InkStroke) -> Unit,
) {
    var activeStroke by remember { mutableStateOf<List<InkPoint>>(emptyList()) }
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.18f)
            .background(Color.White.copy(alpha = 0.84f), shape)
            .border(1.dp, Color(0xFFD8DDD8), shape),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            activeStroke = listOf(offset.toInkPoint())
                        },
                        onDragEnd = {
                            if (activeStroke.isNotEmpty()) {
                                onStrokeCompleted(InkStroke(activeStroke))
                            }
                            activeStroke = emptyList()
                        },
                        onDragCancel = { activeStroke = emptyList() },
                        onDrag = { change, _ ->
                            change.consume()
                            activeStroke = activeStroke + change.position.toInkPoint()
                        },
                    )
                },
        ) {
            val guideColor = Color(0xFFE1E5E1)
            drawLine(guideColor, Offset(size.width / 2, 24f), Offset(size.width / 2, size.height - 24f), 2f)
            drawLine(guideColor, Offset(24f, size.height / 2), Offset(size.width - 24f, size.height / 2), 2f)

            strokes.forEach { drawInkStroke(it.points) }
            drawInkStroke(activeStroke)
        }

        if (strokes.isEmpty() && activeStroke.isEmpty()) {
            Text(
                text = "여기에 천천히 써보세요",
                color = Muted.copy(alpha = 0.56f),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Surface(
            color = Paper,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp),
        ) {
            Text("PEN", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp, 4.dp))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawInkStroke(points: List<InkPoint>) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(Ink, radius = 7f, center = Offset(points[0].x, points[0].y))
        return
    }
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(path, Ink, style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun Offset.toInkPoint() = InkPoint(x, y, System.currentTimeMillis())

@Composable
private fun ModelStatus(state: ModelState) {
    when (state) {
        ModelState.Ready -> Text("● 필기 인식 준비 완료", color = DeepGreen, style = MaterialTheme.typography.labelMedium)
        ModelState.Downloading -> Text("● 언어 모델 준비 중…", color = Muted, style = MaterialTheme.typography.labelMedium)
        is ModelState.Error -> Text("● ${state.message}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ActionRow(
    canRecognize: Boolean,
    isRecognizing: Boolean,
    onClear: () -> Unit,
    onRecognize: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onClear,
            enabled = !isRecognizing,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Ink),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(0.38f).height(54.dp),
        ) {
            Text("다시 쓰기")
        }
        Button(
            onClick = onRecognize,
            enabled = canRecognize,
            colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = DeepGreen),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(0.62f).height(54.dp),
        ) {
            if (isRecognizing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("인식하고 펼쳐보기", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResultPanel(state: PracticeUiState) {
    Spacer(Modifier.height(16.dp))
    when (val result = state.result) {
        is EvaluationResult.Correct -> ScenePreview(state)
        is EvaluationResult.AlmostCorrect -> FeedbackCard(
            title = "거의 다 왔어요",
            message = "${result.recognized} → ${result.expected}",
            accent = Color(0xFFE8A838),
        )
        is EvaluationResult.Incorrect -> FeedbackCard(
            title = "한 번 더 써볼까요?",
            message = "인식 결과: ${result.candidates.take(3).joinToString()}",
            accent = Color(0xFFE16B61),
        )
        EvaluationResult.NotRecognized -> FeedbackCard(
            title = "글자를 찾지 못했어요",
            message = "획을 조금 더 크게 써보세요.",
            accent = Color(0xFFE16B61),
        )
        null -> Unit
    }
}

@Composable
private fun FeedbackCard(title: String, message: String, accent: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, color = accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(message, color = Muted)
        }
    }
}

@Composable
private fun ScenePreview(state: PracticeUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DeepGreen),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("정답이에요", color = Mint, fontWeight = FontWeight.Bold)
            Text(
                "${state.recognizedText.orEmpty()}의 세계가 펼쳐집니다",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(14.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF285E58), Color(0xFF102B28))),
                        RoundedCornerShape(18.dp),
                    ),
            ) {
                val back = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(size.width * 0.32f, size.height * 0.28f)
                    lineTo(size.width * 0.66f, size.height)
                    close()
                }
                val front = Path().apply {
                    moveTo(size.width * 0.25f, size.height)
                    lineTo(size.width * 0.67f, size.height * 0.16f)
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(back, Color(0xFF52A781))
                drawPath(front, Mint)
                drawCircle(Color.White.copy(alpha = 0.8f), 18f, Offset(size.width * 0.78f, size.height * 0.22f))
            }
            state.scene?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    "3D Scene · ${it.id.value}",
                    color = Color.White.copy(alpha = 0.58f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
