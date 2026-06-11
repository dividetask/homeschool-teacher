package com.dividetask.homeschoolteacher.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dividetask.homeschoolteacher.Tts
import kotlinx.coroutines.delay

@Composable
fun PhonemesScreen(
    viewModel: PhonemesViewModel,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val problem = state.problem

    var inputReady by remember { mutableStateOf(false) }
    LaunchedEffect(state.problem) {
        inputReady = false
        delay(1000)
        inputReady = true
    }

    // Play the trio at every speed each time a new problem is shown.
    LaunchedEffect(state.problem) {
        Tts.speakAll(problem.words)
    }

    // Show answer time: 4 seconds for every outcome (longer than the
    // default delays because the answer reveals three words the learner
    // needs time to read).
    LaunchedEffect(state.feedback, state.problem) {
        if (state.feedback == PhonemesFeedback.None) return@LaunchedEffect
        delay(4000)
        Tts.stopAll()
        onCompleted()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ScoreItem("Correct", state.correctCount, Color(0xFF22C55E))
            ScoreItem("Wrong", state.wrongCount, Color(0xFFEF4444))
        }

        WordStack(
            words = problem.words,
            reveal = state.feedback != PhonemesFeedback.None,
        )

        Text(
            text = when (state.feedback) {
                PhonemesFeedback.Correct -> "Correct! Same first letter: ${problem.letter.uppercaseChar()}"
                PhonemesFeedback.Wrong -> "Not quite — the first letter was ${problem.letter.uppercaseChar()}"
                PhonemesFeedback.Revealed -> "The first letter was ${problem.letter.uppercaseChar()}"
                PhonemesFeedback.None -> "Three words. Listen — what's the first letter?"
            },
            fontSize = 16.sp,
            color = when (state.feedback) {
                PhonemesFeedback.Correct -> Color(0xFF22C55E)
                PhonemesFeedback.Wrong -> Color(0xFFEF4444)
                PhonemesFeedback.Revealed -> Color(0xFFFACC15)
                PhonemesFeedback.None -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            },
        )

        LetterGrid(
            selected = state.selected,
            feedback = state.feedback,
            correct = problem.letter.uppercaseChar(),
            onChoose = viewModel::onAnswer,
            enabled = inputReady,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { Tts.speakAll(problem.words) }) {
                Text("🔊 Repeat", fontSize = 14.sp)
            }
            TextButton(onClick = viewModel::giveUp) {
                Text("Give up", fontSize = 14.sp)
            }
        }
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
private fun WordStack(words: List<String>, reveal: Boolean) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        words.forEachIndexed { i, word ->
            val display = if (reveal) word else "🔊  ${i + 1}"
            Box(
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 320.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = display,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (reveal) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun LetterGrid(
    selected: Char?,
    feedback: PhonemesFeedback,
    correct: Char,
    onChoose: (Char) -> Unit,
    enabled: Boolean,
) {
    val cols = 7
    val letters = ('A'..'Z').toList()
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
    ) {
        letters.chunked(cols).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { letter ->
                    LetterButton(
                        letter = letter,
                        selected = selected,
                        feedback = feedback,
                        correct = correct,
                        onChoose = onChoose,
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                    )
                }
                repeat(cols - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LetterButton(
    letter: Char,
    selected: Char?,
    feedback: PhonemesFeedback,
    correct: Char,
    onChoose: (Char) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val container = when {
        feedback == PhonemesFeedback.None -> MaterialTheme.colorScheme.primary
        letter == correct -> Color(0xFF22C55E)
        letter == selected -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    }
    Box(
        modifier = modifier
            .background(container, shape = RoundedCornerShape(10.dp))
            .clickable(
                enabled = enabled && feedback == PhonemesFeedback.None,
                onClick = { onChoose(letter) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
