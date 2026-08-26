package com.dividetask.homeschoolteacher.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dividetask.homeschoolteacher.Tts
import com.dividetask.homeschoolteacher.ui.FeedbackHold
import kotlinx.coroutines.delay

@Composable
fun RhymingWordsScreen(
    viewModel: RhymingWordsViewModel,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val problem = state.problem
    val speakingWord by Tts.speakingWord.collectAsStateWithLifecycle()

    var inputReady by remember { mutableStateOf(false) }
    LaunchedEffect(state.problem) {
        inputReady = false
        delay(1000)
        inputReady = true
    }

    LaunchedEffect(state.problem) {
        Tts.speakSequence(problem.speakWords)
    }

    LaunchedEffect(state.feedback, state.problem) {
        val hold = when (state.feedback) {
            RhymingWordsFeedback.None -> return@LaunchedEffect
            RhymingWordsFeedback.Correct -> FeedbackHold.CORRECT_MS
            RhymingWordsFeedback.Wrong -> FeedbackHold.WRONG_MS
            RhymingWordsFeedback.Revealed -> FeedbackHold.REVEALED_MS
        }
        Tts.stopAll() // stop the word read-out and highlight once answered
        delay(hold)
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

        // Level 0 shows the spoken target word to rhyme with; tap to hear it.
        if (problem.level == RhymingLevel.PickRhyme) {
            val targetSpeaking = speakingWord == problem.target
            Text(
                text = "🔊  ${problem.target}",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = if (targetSpeaking) Color(0xFFFACC15) else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { Tts.speakSequence(listOf(problem.target ?: "")) }
                    .padding(4.dp),
            )
        }

        Text(
            text = when {
                state.feedback == RhymingWordsFeedback.Correct -> "Correct!"
                state.feedback == RhymingWordsFeedback.Wrong ->
                    "Not quite — the answer was \"${problem.correctWord}\""
                state.feedback == RhymingWordsFeedback.Revealed ->
                    "The answer was \"${problem.correctWord}\""
                problem.level == RhymingLevel.PickRhyme ->
                    "Which word rhymes with \"${problem.target}\"?"
                else -> "Which word does NOT rhyme?"
            },
            fontSize = 16.sp,
            color = when (state.feedback) {
                RhymingWordsFeedback.Correct -> Color(0xFF22C55E)
                RhymingWordsFeedback.Wrong -> Color(0xFFEF4444)
                RhymingWordsFeedback.Revealed -> Color(0xFFFACC15)
                RhymingWordsFeedback.None -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            },
        )

        Column(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            problem.choices.forEach { word ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Separate play control (not the answer button), to the left.
                    SpeakerButton(
                        speaking = speakingWord == word,
                        onClick = { Tts.speakSequence(listOf(word)) },
                    )
                    ChoiceButton(
                        word = word,
                        selected = state.selected,
                        feedback = state.feedback,
                        correct = problem.correctWord,
                        enabled = inputReady,
                        isSpeaking = speakingWord == word,
                        onChoose = viewModel::onAnswer,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { Tts.speakSequence(problem.speakWords) }) {
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
private fun ChoiceButton(
    word: String,
    selected: String?,
    feedback: RhymingWordsFeedback,
    correct: String,
    enabled: Boolean,
    isSpeaking: Boolean,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = when {
        feedback == RhymingWordsFeedback.None -> MaterialTheme.colorScheme.primary
        word == correct -> Color(0xFF22C55E)
        word == selected -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    }
    val shape = RoundedCornerShape(12.dp)
    var box = modifier
        .heightIn(min = 56.dp)
        .background(container, shape = shape)
    if (isSpeaking) {
        box = box.border(3.dp, Color(0xFFFACC15), shape)
    }
    Box(
        modifier = box.clickable(
            enabled = enabled && feedback == RhymingWordsFeedback.None,
            onClick = { onChoose(word) },
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = word,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

/** A play-sound control shown to the right of a choice (not the answer). */
@Composable
private fun SpeakerButton(speaking: Boolean, onClick: () -> Unit) {
    val bg = if (speaking) Color(0xFFFACC15) else MaterialTheme.colorScheme.secondary
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(bg, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "🔊", fontSize = 24.sp)
    }
}
