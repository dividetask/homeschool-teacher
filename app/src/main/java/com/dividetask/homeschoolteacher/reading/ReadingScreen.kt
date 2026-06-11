package com.dividetask.homeschoolteacher.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dividetask.homeschoolteacher.Tts
import kotlinx.coroutines.delay

@Composable
fun ReadingScreen(
    viewModel: ReadingViewModel,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var inputReady by remember { mutableStateOf(false) }
    LaunchedEffect(state.animal) {
        inputReady = false
        delay(1000)
        inputReady = true
    }

    LaunchedEffect(state.animal) {
        Tts.speak(state.animal.name)
    }

    LaunchedEffect(state.feedback, state.animal) {
        when (state.feedback) {
            ReadingFeedback.Correct -> {
                delay(900)
                Tts.stopAll()
                onCompleted()
            }
            ReadingFeedback.Wrong -> {
                delay(2000)
                Tts.stopAll()
                onCompleted()
            }
            ReadingFeedback.Revealed -> {
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

        Text(
            text = state.animal.emoji,
            fontSize = 120.sp,
        )

        Text(
            text = when (state.feedback) {
                ReadingFeedback.Correct -> "Correct! ${state.animal.name}"
                ReadingFeedback.Wrong -> "Not quite — the answer was ${state.animal.letter} (${state.animal.name})"
                ReadingFeedback.Revealed -> "The answer was ${state.animal.letter} — ${state.animal.name}"
                ReadingFeedback.None -> "Which letter does it start with?"
            },
            fontSize = 18.sp,
            color = when (state.feedback) {
                ReadingFeedback.Correct -> Color(0xFF22C55E)
                ReadingFeedback.Wrong -> Color(0xFFEF4444)
                ReadingFeedback.Revealed -> Color(0xFFFACC15)
                ReadingFeedback.None -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            },
        )

        LetterGrid(
            selected = state.selected,
            feedback = state.feedback,
            correct = state.animal.letter,
            onChoose = viewModel::onAnswer,
            inputEnabled = inputReady,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { Tts.speak(state.animal.name) }) {
                Text("🔊 Repeat", fontSize = 14.sp)
            }
            TextButton(onClick = viewModel::giveUp) {
                Text("Give up", fontSize = 14.sp)
            }
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
private fun LetterGrid(
    selected: Char?,
    feedback: ReadingFeedback,
    correct: Char,
    onChoose: (Char) -> Unit,
    inputEnabled: Boolean,
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
                        enabled = inputEnabled,
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
    feedback: ReadingFeedback,
    correct: Char,
    onChoose: (Char) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val container = when {
        feedback == ReadingFeedback.None -> MaterialTheme.colorScheme.primary
        letter == correct -> Color(0xFF22C55E)
        letter == selected -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    }
    Button(
        onClick = { onChoose(letter) },
        enabled = enabled && feedback == ReadingFeedback.None,
        shape = RoundedCornerShape(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            disabledContainerColor = container,
            contentColor = Color.White,
            disabledContentColor = Color.White,
        ),
        modifier = modifier,
    ) {
        Text(
            text = letter.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
