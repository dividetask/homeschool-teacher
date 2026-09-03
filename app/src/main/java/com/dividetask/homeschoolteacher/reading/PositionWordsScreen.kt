package com.dividetask.homeschoolteacher.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.dividetask.homeschoolteacher.ui.FeedbackHold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PositionWordsScreen(
    viewModel: PositionWordsViewModel,
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

    LaunchedEffect(state.feedback, state.problem) {
        val hold = when (state.feedback) {
            PositionFeedback.None -> return@LaunchedEffect
            PositionFeedback.Correct -> FeedbackHold.CORRECT_MS
            PositionFeedback.Wrong -> FeedbackHold.WRONG_MS
            PositionFeedback.Revealed -> FeedbackHold.REVEALED_MS
        }
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

        SceneView(problem.scene)

        Sentence(problem = problem, feedback = state.feedback)

        FlowRow(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            problem.choices.forEach { word ->
                ChoiceButton(
                    word = word,
                    selected = state.selected,
                    feedback = state.feedback,
                    correct = problem.correctWord,
                    enabled = inputReady,
                    onChoose = viewModel::onAnswer,
                )
            }
        }

        TextButton(onClick = viewModel::giveUp) {
            Text("Give up", fontSize = 14.sp)
        }
    }
}

/** Depict the scene: animal positioned on / in / over / under the object. */
@Composable
private fun SceneView(scene: PositionScene) {
    val animal = scene.animal.emoji
    val obj = scene.obj.item.emoji
    val big = 72.sp
    Box(
        modifier = Modifier.heightIn(min = 180.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (scene.prep) {
            // On: the animal rests directly on the object (touching).
            "on" -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((-16).dp),
            ) {
                Text(animal, fontSize = big)
                Text(obj, fontSize = big)
            }
            "under" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(obj, fontSize = big)
                Text(animal, fontSize = big)
            }
            // Over: the animal floats higher above the object than "on".
            "over" -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(36.dp),
            ) {
                Text(animal, fontSize = big)
                Text(obj, fontSize = big)
            }
            "in" -> if (scene.obj.tall) {
                // Deep container drawn in front of the animal, so the animal
                // peeks out the top — clearly "inside".
                Box(contentAlignment = Alignment.Center) {
                    Text(animal, fontSize = 52.sp, modifier = Modifier.offset(y = (-44).dp))
                    Text(obj, fontSize = 116.sp)
                }
            } else {
                // Flat, open container: the animal sits inside it.
                Box(contentAlignment = Alignment.Center) {
                    Text(obj, fontSize = 116.sp)
                    Text(animal, fontSize = 46.sp)
                }
            }
            "by" -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(animal, fontSize = big)
                Text(obj, fontSize = big)
            }
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(animal, fontSize = big)
                Text(obj, fontSize = big)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Sentence(problem: PositionProblem, feedback: PositionFeedback) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val answered = feedback != PositionFeedback.None
    val blankColor = when (feedback) {
        PositionFeedback.Correct -> Color(0xFF22C55E)
        PositionFeedback.Wrong -> Color(0xFFEF4444)
        PositionFeedback.Revealed -> Color(0xFFFACC15)
        PositionFeedback.None -> MaterialTheme.colorScheme.primary
    }

    fun token(word: String, isBlank: Boolean): Pair<String, Boolean> {
        return if (isBlank) {
            (if (answered) problem.correctWord else "____") to true
        } else {
            word to false
        }
    }

    val tokens = listOf(
        "The" to false,
        token(problem.scene.animal.word, problem.blank == PositionBlank.Subject),
        "is" to false,
        token(problem.scene.prep, problem.blank == PositionBlank.Preposition),
        "the" to false,
        token(problem.scene.obj.item.word + ".", problem.blank == PositionBlank.Object),
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth(),
    ) {
        tokens.forEach { (text, isBlank) ->
            Text(
                text = text,
                fontSize = 24.sp,
                fontWeight = if (isBlank) FontWeight.Bold else FontWeight.Normal,
                color = if (isBlank) blankColor else onBg,
            )
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
    feedback: PositionFeedback,
    correct: String,
    enabled: Boolean,
    onChoose: (String) -> Unit,
) {
    val container = when {
        feedback == PositionFeedback.None -> MaterialTheme.colorScheme.primary
        word == correct -> Color(0xFF22C55E)
        word == selected -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    }
    Box(
        modifier = Modifier
            .heightIn(min = 52.dp)
            .widthIn(min = 88.dp)
            .background(container, shape = RoundedCornerShape(12.dp))
            .clickable(
                enabled = enabled && feedback == PositionFeedback.None,
                onClick = { onChoose(word) },
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = word,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
