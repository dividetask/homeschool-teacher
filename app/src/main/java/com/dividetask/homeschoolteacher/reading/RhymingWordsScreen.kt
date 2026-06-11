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
fun RhymingWordsScreen(
    viewModel: RhymingWordsViewModel,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val word = state.problem.word

    var inputReady by remember { mutableStateOf(false) }
    LaunchedEffect(state.problem) {
        inputReady = false
        delay(1000)
        inputReady = true
    }

    // Speak the word three times (1x, 0.5x, 0.125x) whenever a new
    // problem is shown.
    LaunchedEffect(state.problem) {
        Tts.speak(word)
    }

    LaunchedEffect(state.feedback, state.problem) {
        when (state.feedback) {
            RhymingWordsFeedback.Correct -> {
                delay(900)
                Tts.stopAll()
                onCompleted()
            }
            RhymingWordsFeedback.Wrong -> {
                delay(2000)
                Tts.stopAll()
                onCompleted()
            }
            RhymingWordsFeedback.Revealed -> {
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
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ScoreItem("Correct", state.correctCount, Color(0xFF22C55E))
            ScoreItem("Wrong", state.wrongCount, Color(0xFFEF4444))
        }

        WordDisplay(
            word = word,
            revealed = state.feedback == RhymingWordsFeedback.Correct ||
                state.feedback == RhymingWordsFeedback.Revealed,
            onTap = { Tts.speak(word) },
        )

        Text(
            text = when (state.feedback) {
                RhymingWordsFeedback.Correct -> "Correct! $word"
                RhymingWordsFeedback.Wrong -> "Not quite — the answer was ${state.problem.missingLetter} (\"$word\")"
                RhymingWordsFeedback.Revealed -> "The answer was ${state.problem.missingLetter} — \"$word\""
                RhymingWordsFeedback.None -> "Which letter does it start with?"
            },
            fontSize = 16.sp,
            color = when (state.feedback) {
                RhymingWordsFeedback.Correct -> Color(0xFF22C55E)
                RhymingWordsFeedback.Wrong -> Color(0xFFEF4444)
                RhymingWordsFeedback.Revealed -> Color(0xFFFACC15)
                RhymingWordsFeedback.None -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            },
        )

        LetterGrid(
            selected = state.selected,
            feedback = state.feedback,
            correct = state.problem.missingLetter,
            onChoose = viewModel::onAnswer,
            inputEnabled = inputReady,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { Tts.speak(word) }) {
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
private fun WordDisplay(
    word: String,
    revealed: Boolean,
    onTap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onTap)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "🔊", fontSize = 28.sp)
        Spacer(modifier = Modifier.padding(end = 4.dp))
        word.forEachIndexed { i, c ->
            val isBlank = !revealed && i == 0
            val glyph = if (isBlank) "_" else c.uppercaseChar().toString()
            Text(
                text = glyph,
                fontFamily = FontFamily.Monospace,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = if (isBlank) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun LetterGrid(
    selected: Char?,
    feedback: RhymingWordsFeedback,
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
    feedback: RhymingWordsFeedback,
    correct: Char,
    onChoose: (Char) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val container = when {
        feedback == RhymingWordsFeedback.None -> MaterialTheme.colorScheme.primary
        letter == correct -> Color(0xFF22C55E)
        letter == selected -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    }
    Box(
        modifier = modifier
            .background(container, shape = RoundedCornerShape(10.dp))
            .clickable(
                enabled = enabled && feedback == RhymingWordsFeedback.None,
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
