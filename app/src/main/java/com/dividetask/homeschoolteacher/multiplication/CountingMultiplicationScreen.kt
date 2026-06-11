package com.dividetask.homeschoolteacher.multiplication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountingMultiplicationScreen(
    viewModel: CountingMultiplicationViewModel,
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
        when (state.feedback) {
            MultiplicationFeedback.Correct -> {
                delay(900)
                Tts.stopAll()
                onCompleted()
            }
            MultiplicationFeedback.Wrong -> {
                delay(2000)
                Tts.stopAll()
                onCompleted()
            }
            MultiplicationFeedback.Revealed -> {
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

        Text(
            text = "${problem.op1} × ${problem.op2} = ?",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        AnimalGroups(
            op1 = problem.op1,
            op2 = problem.op2,
            emoji = problem.animal.emoji,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
        )

        Text(
            text = when (state.feedback) {
                MultiplicationFeedback.Correct -> "Correct!"
                MultiplicationFeedback.Wrong -> "Not quite — the answer was ${problem.answer}"
                MultiplicationFeedback.Revealed -> "The answer was ${problem.answer}"
                MultiplicationFeedback.None -> "Count the ${problem.animal.name.lowercase()}s"
            },
            fontSize = 16.sp,
            color = when (state.feedback) {
                MultiplicationFeedback.Correct -> Color(0xFF22C55E)
                MultiplicationFeedback.Wrong -> Color(0xFFEF4444)
                MultiplicationFeedback.Revealed -> Color(0xFFFACC15)
                MultiplicationFeedback.None -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            },
        )

        ChoiceGrid(
            selected = state.selected,
            feedback = state.feedback,
            correct = problem.answer,
            onChoose = viewModel::onAnswer,
            inputEnabled = inputReady,
        )

        TextButton(onClick = viewModel::giveUp) {
            Text("Give up", fontSize = 14.sp)
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

/**
 * Render `op2` groups, each containing `op1` copies of [emoji]. Groups
 * flow left-to-right and wrap to additional lines as needed so a long
 * product like 4 × 4 = 16 stays readable on a phone. A single group is
 * never broken across a line because each group is its own [Row].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimalGroups(
    op1: Int,
    op2: Int,
    emoji: String,
    modifier: Modifier = Modifier,
) {
    if (op1 == 0 || op2 == 0) {
        Text(
            text = "(no ${emoji})",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = modifier.padding(vertical = 8.dp),
        )
        return
    }
    FlowRow(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(op2) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(op1) {
                    Text(text = emoji, fontSize = 28.sp)
                }
            }
        }
    }
}

@Composable
private fun ChoiceGrid(
    selected: Int?,
    feedback: MultiplicationFeedback,
    correct: Int,
    onChoose: (Int) -> Unit,
    inputEnabled: Boolean,
) {
    val maxAnswer = 16  // 4 × 4 = 16
    val cols = 6        // 17 cells → 3 rows of 6 + 1 spacer in the last row
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
                        feedback == MultiplicationFeedback.None -> MaterialTheme.colorScheme.primary
                        choice == correct -> Color(0xFF22C55E)
                        choice == selected -> Color(0xFFEF4444)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    }
                    Button(
                        onClick = { onChoose(choice) },
                        enabled = inputEnabled && feedback == MultiplicationFeedback.None,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = container,
                            disabledContainerColor = container,
                            contentColor = Color.White,
                            disabledContentColor = Color.White,
                        ),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = choice.toString(),
                            fontSize = 18.sp,
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
