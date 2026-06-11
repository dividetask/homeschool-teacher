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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.dividetask.homeschoolteacher.ClipPlayer
import kotlinx.coroutines.delay

@Composable
fun LetterSoundsScreen(
    viewModel: LetterSoundsViewModel,
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

    // Play the word clip each time a new problem appears.
    LaunchedEffect(state.problem) {
        ClipPlayer.play(problem.clipRes)
    }

    // Silence the clip if the lesson leaves the screen mid-playback.
    DisposableEffect(Unit) {
        onDispose { ClipPlayer.stopAll() }
    }

    // Once an answer lands, play the letter clip (e.g. a1.mp3) as
    // reinforcement, hold so it can be heard, then advance.
    LaunchedEffect(state.feedback, state.problem) {
        val hold = when (state.feedback) {
            LetterSoundsFeedback.None -> return@LaunchedEffect
            LetterSoundsFeedback.Correct -> 2000L
            LetterSoundsFeedback.Wrong -> 2400L
            LetterSoundsFeedback.Revealed -> 2200L
        }
        ClipPlayer.play(problem.answerClipRes)
        delay(hold)
        ClipPlayer.stopAll()
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
            ScoreItem("Streak", state.runStreak, Color(0xFF3B82F6))
            ScoreItem("Wrong", state.wrongCount, Color(0xFFEF4444))
        }

        // Big tappable speaker — replays the clip on demand.
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                .clickable { ClipPlayer.play(problem.clipRes) },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🔊", fontSize = 56.sp)
        }

        Text(
            text = when (state.feedback) {
                LetterSoundsFeedback.Correct ->
                    "Correct! It starts with ${problem.letter.uppercaseChar()}"
                LetterSoundsFeedback.Wrong ->
                    "Not quite — it starts with ${problem.letter.uppercaseChar()}"
                LetterSoundsFeedback.Revealed ->
                    "It starts with ${problem.letter.uppercaseChar()}"
                LetterSoundsFeedback.None ->
                    "Listen — what letter does the word start with?"
            },
            fontSize = 16.sp,
            color = when (state.feedback) {
                LetterSoundsFeedback.Correct -> Color(0xFF22C55E)
                LetterSoundsFeedback.Wrong -> Color(0xFFEF4444)
                LetterSoundsFeedback.Revealed -> Color(0xFFFACC15)
                LetterSoundsFeedback.None ->
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
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
            TextButton(onClick = { ClipPlayer.play(problem.clipRes) }) {
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
private fun LetterGrid(
    selected: Char?,
    feedback: LetterSoundsFeedback,
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
    feedback: LetterSoundsFeedback,
    correct: Char,
    onChoose: (Char) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val container = when {
        feedback == LetterSoundsFeedback.None -> MaterialTheme.colorScheme.primary
        letter == correct -> Color(0xFF22C55E)
        letter == selected -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    }
    Box(
        modifier = modifier
            .background(container, shape = RoundedCornerShape(10.dp))
            .clickable(
                enabled = enabled && feedback == LetterSoundsFeedback.None,
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
