package com.dividetask.homeschoolteacher.math

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dividetask.homeschoolteacher.Tts
import com.dividetask.homeschoolteacher.lesson.LessonId
import kotlin.math.ceil
import kotlin.random.Random
import kotlinx.coroutines.delay

// Lessons whose answer is typed on a number pad (Enter to submit) instead
// of tapped from a grid — used where the answer range is too large for a
// comfortable button grid (multiplication products up to 81).
private val TYPED_ANSWER_LESSONS = setOf(
    LessonId.HorizontalMultiplication1,
    LessonId.VerticalMultiplication1,
    LessonId.NumberLineMultiplication1,
)

@Composable
fun MathScreen(
    viewModel: MathViewModel,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val active by viewModel.activeLesson.collectAsStateWithLifecycle()
    val problem = state.problem
    val maxAnswer = when (active) {
        LessonId.MathPictures -> 9          // addition operands 1..4, max sum 8 (pad to 9)
        LessonId.Math0,
        LessonId.HorizontalAddition0,
        LessonId.NumberLineAddition0 -> 9   // addition operands 0..4, max sum 8 (pad to 9)
        LessonId.CountingAddition1,
        LessonId.Math1,
        LessonId.HorizontalAddition1,
        LessonId.MathNumberLine -> 18       // addition operands 0..9, max sum 18
        LessonId.CountingSubtraction0,
        LessonId.HorizontalSubtraction0,
        LessonId.VerticalSubtraction0,
        LessonId.NumberLineSubtraction0 -> 9 // subtraction op1 4..9, op2 0..4, max diff 9
        LessonId.HorizontalMultiplication0,
        LessonId.VerticalMultiplication0,
        LessonId.NumberLineMultiplication0 -> 16 // multiplication operands 0..4, max product 16
        LessonId.HorizontalMultiplication1,
        LessonId.VerticalMultiplication1,
        LessonId.NumberLineMultiplication1 -> 81 // multiplication operands 0..9, max product 81
        else -> 9
    }

    val isTyped = active in TYPED_ANSWER_LESSONS
    // In-progress typed answer for the number-pad lessons; resets each problem.
    var typed by remember(state.problem) { mutableStateOf("") }
    // What the equation's answer box shows: the typed digits while entering,
    // otherwise the submitted choice (once feedback is showing).
    val answerText = if (isTyped && state.feedback == MathFeedback.None) {
        typed
    } else {
        state.selected?.toString() ?: ""
    }

    var inputReady by remember { mutableStateOf(false) }
    LaunchedEffect(state.problem) {
        inputReady = false
        delay(1000)
        inputReady = true
    }

    LaunchedEffect(state.feedback, state.problem) {
        when (state.feedback) {
            MathFeedback.Correct -> {
                delay(900)
                Tts.stopAll()
                onCompleted()
            }
            MathFeedback.Wrong -> {
                delay(2000)
                Tts.stopAll()
                onCompleted()
            }
            MathFeedback.Revealed -> {
                delay(1600)
                Tts.stopAll()
                onCompleted()
            }
            else -> Unit
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        ScoreRow(correct = state.correctCount, wrong = state.wrongCount)

        when (active) {
            LessonId.MathPictures,
            LessonId.CountingAddition1,
            LessonId.CountingSubtraction0 -> PictureProblem(problem)
            LessonId.MathNumberLine,
            LessonId.NumberLineAddition0,
            LessonId.NumberLineSubtraction0,
            LessonId.NumberLineMultiplication0,
            LessonId.NumberLineMultiplication1 -> NumberLineProblem(
                problem = problem,
                answerText = answerText,
                feedback = state.feedback,
                maxTick = maxAnswer,
            )
            LessonId.HorizontalAddition0,
            LessonId.HorizontalAddition1,
            LessonId.HorizontalSubtraction0,
            LessonId.HorizontalMultiplication0,
            LessonId.HorizontalMultiplication1 -> HorizontalProblem(
                problem = problem,
                answerText = answerText,
                feedback = state.feedback,
            )
            else -> StackedProblem(
                problem = problem,
                feedback = state.feedback,
            )
        }

        Text(
            text = when (state.feedback) {
                MathFeedback.Correct -> "Correct!"
                MathFeedback.Wrong -> "Not quite — the answer was ${problem.answer}"
                MathFeedback.Revealed -> "The answer was ${problem.answer}"
                MathFeedback.None -> "Pick the answer"
            },
            fontSize = 16.sp,
            color = when (state.feedback) {
                MathFeedback.Correct -> Color(0xFF22C55E)
                MathFeedback.Wrong -> Color(0xFFEF4444)
                MathFeedback.Revealed -> Color(0xFFFACC15)
                MathFeedback.None -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            },
        )

        if (isTyped) {
            DecimalKeypad(
                typed = typed,
                feedback = state.feedback,
                inputEnabled = inputReady && state.feedback == MathFeedback.None,
                onDigit = { d -> if (typed.length < 2) typed += d.toString() },
                onBack = { if (typed.isNotEmpty()) typed = typed.dropLast(1) },
                onEnter = { if (typed.isNotEmpty()) viewModel.onAnswer(typed.toInt()) },
            )
        } else {
            ChoiceGrid(
                selected = state.selected,
                feedback = state.feedback,
                correct = problem.answer,
                onChoose = viewModel::onAnswer,
                inputEnabled = inputReady,
                maxAnswer = maxAnswer,
            )
        }

        TextButton(onClick = viewModel::giveUp) {
            Text("Give up", fontSize = 14.sp)
        }
    }
}

@Composable
private fun ScoreRow(correct: Int, wrong: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        ScoreItem("Correct", correct, Color(0xFF22C55E))
        ScoreItem("Wrong", wrong, Color(0xFFEF4444))
    }
}

@Composable
private fun ScoreItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Text(
            text = value.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun StackedProblem(
    problem: MathProblem,
    feedback: MathFeedback,
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = " ${problem.left}",
            fontFamily = FontFamily.Monospace,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = onBg,
        )
        Text(
            text = "${problem.operator.symbol}${problem.right}",
            fontFamily = FontFamily.Monospace,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = onBg,
        )
        Text(
            text = "──",
            fontFamily = FontFamily.Monospace,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = onBg,
        )
    }
}

@Composable
private fun PictureProblem(problem: MathProblem) {
    val animalA = problem.leftAnimal
    val animalB = problem.rightAnimal
    if (animalA == null || animalB == null) {
        // Fallback if animals weren't populated for some reason.
        StackedProblem(problem, MathFeedback.None)
        return
    }
    // Decide once per problem whether each group is arranged on one or two
    // lines, so recompositions don't reshuffle the layout mid-problem.
    val (splitLeft, splitRight) = remember(problem) {
        splitGroup(problem.left) to splitGroup(problem.right)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        AnimalGroup(animal = animalA.emoji, count = problem.left, twoLines = splitLeft)
        Text(problem.operator.symbol, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        AnimalGroup(animal = animalB.emoji, count = problem.right, twoLines = splitRight)
        Text("=", fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text("?", fontSize = 40.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Whether an animal group of [count] emoji should stack on two lines.
 * Always for big groups (they don't fit on one line and are easier to
 * count in two rows); occasionally for smaller ones so kids learn the
 * count doesn't depend on the arrangement.
 */
private fun splitGroup(count: Int): Boolean = when {
    count > 4 -> true
    count >= 2 -> Random.nextInt(10) < 3
    else -> false
}

@Composable
private fun AnimalGroup(animal: String, count: Int, twoLines: Boolean) {
    if (!twoLines) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(count) {
                Text(text = animal, fontSize = 32.sp)
            }
        }
    } else {
        // Top row gets the larger half, e.g. 3 -> 2 over 1, 7 -> 4 over 3.
        val top = (count + 1) / 2
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(top) {
                    Text(text = animal, fontSize = 32.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(count - top) {
                    Text(text = animal, fontSize = 32.sp)
                }
            }
        }
    }
}

@Composable
private fun HorizontalProblem(
    problem: MathProblem,
    answerText: String,
    feedback: MathFeedback,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        Text("${problem.left}", fontSize = 40.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace)
        Text(problem.operator.symbol, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text("${problem.right}", fontSize = 40.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace)
        Text("=", fontSize = 40.sp, fontWeight = FontWeight.Bold)
        AnswerBox(answerText, feedback)
    }
}

@Composable
private fun NumberLineProblem(
    problem: MathProblem,
    answerText: String,
    feedback: MathFeedback,
    maxTick: Int,
) {
    val a = problem.left
    val c = problem.answer
    // Pad each side with a randomized 1..3 extra ticks so the answer can't
    // be read off as "the right-most labelled tick". Same random padding
    // sticks for the lifetime of this problem.
    val extras = remember(problem) {
        Random.nextInt(1, 4) to Random.nextInt(1, 4)
    }
    val low = (minOf(a, c) - extras.first).coerceAtLeast(0)
    val high = (maxOf(a, c) + extras.second).coerceAtMost(maxTick)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        NumberLine(
            rangeStart = low,
            rangeEnd = high,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("$a", fontSize = 32.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace)
            Text(problem.operator.symbol, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("${problem.right}", fontSize = 32.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace)
            Text("=", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            AnswerBox(answerText, feedback)
        }
    }
}

@Composable
private fun AnswerBox(text: String, feedback: MathFeedback) {
    val borderColor = when (feedback) {
        MathFeedback.Correct -> Color(0xFF22C55E)
        MathFeedback.Wrong -> Color(0xFFEF4444)
        MathFeedback.Revealed -> Color(0xFFFACC15)
        MathFeedback.None -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
    }
    Box(
        modifier = Modifier
            .widthIn(min = 64.dp, max = 96.dp)
            .heightIn(min = 56.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.ifEmpty { " " },
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = borderColor,
        )
    }
}

@Composable
private fun NumberLine(
    rangeStart: Int,
    rangeEnd: Int,
    modifier: Modifier = Modifier,
) {
    val count = rangeEnd - rangeStart + 1
    if (count < 2) return
    // Keep labels readable when the range is wide (multiplication to 81):
    // label roughly every Nth tick so at most ~12 numbers show.
    val labelEvery = maxOf(1, ceil(count / 12.0).toInt())
    val tickColor = MaterialTheme.colorScheme.onBackground
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
        ) {
            val w = size.width
            val h = size.height
            val lineY = h * 0.6f
            val cell = w / count
            val tickHalf = 8.dp.toPx()

            drawLine(
                color = tickColor,
                start = Offset(cell / 2, lineY),
                end = Offset(w - cell / 2, lineY),
                strokeWidth = 3f,
            )

            for (i in 0 until count) {
                val x = cell * i + cell / 2
                val labelled = i % labelEvery == 0 || i == count - 1
                drawLine(
                    color = tickColor,
                    start = Offset(x, lineY - tickHalf),
                    end = Offset(x, lineY + if (labelled) tickHalf else tickHalf * 0.5f),
                    strokeWidth = if (labelled) 2.5f else 1.5f,
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            for (i in rangeStart..rangeEnd) {
                val idx = i - rangeStart
                val show = idx % labelEvery == 0 || i == rangeEnd
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (show) {
                        Text(
                            text = i.toString(),
                            fontSize = if (count > 10) 9.sp else 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Calculator-style number pad for typed answers: digits 1–9, then a
 * Back / 0 / Enter row. A readout above shows the digits entered so far
 * (feedback-coloured once submitted). Answers are at most two digits
 * (products reach 81), enforced by the caller.
 */
@Composable
private fun DecimalKeypad(
    typed: String,
    feedback: MathFeedback,
    inputEnabled: Boolean,
    onDigit: (Int) -> Unit,
    onBack: () -> Unit,
    onEnter: () -> Unit,
) {
    val readoutColor = when (feedback) {
        MathFeedback.Correct -> Color(0xFF22C55E)
        MathFeedback.Wrong -> Color(0xFFEF4444)
        MathFeedback.Revealed -> Color(0xFFFACC15)
        MathFeedback.None -> MaterialTheme.colorScheme.primary
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 96.dp)
                .heightIn(min = 48.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = typed.ifEmpty { "_" },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = readoutColor,
            )
        }
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { d ->
                    KeypadButton(
                        label = d.toString(),
                        onTap = { onDigit(d) },
                        container = MaterialTheme.colorScheme.primary,
                        enabled = inputEnabled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            KeypadButton(
                label = "⌫",
                onTap = onBack,
                container = MaterialTheme.colorScheme.secondary,
                enabled = inputEnabled && typed.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
            KeypadButton(
                label = "0",
                onTap = { onDigit(0) },
                container = MaterialTheme.colorScheme.primary,
                enabled = inputEnabled,
                modifier = Modifier.weight(1f),
            )
            KeypadButton(
                label = "Enter",
                onTap = onEnter,
                container = Color(0xFF22C55E),
                enabled = inputEnabled && typed.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    onTap: () -> Unit,
    container: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val effective = if (enabled) container else container.copy(alpha = 0.4f)
    Button(
        onClick = onTap,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = effective,
            disabledContainerColor = effective,
            contentColor = Color.White,
            disabledContentColor = Color.White,
        ),
        modifier = modifier.heightIn(min = 56.dp),
    ) {
        Text(text = label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChoiceGrid(
    selected: Int?,
    feedback: MathFeedback,
    correct: Int,
    onChoose: (Int) -> Unit,
    inputEnabled: Boolean,
    maxAnswer: Int,
) {
    val cols = when {
        maxAnswer <= 9 -> 5
        maxAnswer <= 18 -> 5
        else -> 7
    }
    val cells = (0..maxAnswer).toList()
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
    ) {
        cells.chunked(cols).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { choice ->
                    val container = when {
                        feedback == MathFeedback.None -> MaterialTheme.colorScheme.primary
                        choice == correct -> Color(0xFF22C55E)
                        choice == selected -> Color(0xFFEF4444)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    }
                    Button(
                        onClick = { onChoose(choice) },
                        enabled = inputEnabled && feedback == MathFeedback.None,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = container,
                            disabledContainerColor = container,
                            contentColor = Color.White,
                            disabledContentColor = Color.White,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                    ) {
                        Text(
                            text = choice.toString(),
                            fontSize = if (maxAnswer <= 9) 24.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                repeat(cols - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Numpad and NumpadButton helpers were removed when math switched to
// single-tap answers across every lesson. ChoiceGrid now handles all
// answer surfaces, expanding to cover whatever range each lesson needs.
