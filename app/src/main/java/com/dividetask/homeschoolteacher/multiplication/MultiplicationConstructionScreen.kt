package com.dividetask.homeschoolteacher.multiplication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import kotlinx.coroutines.delay
import com.dividetask.homeschoolteacher.ui.FeedbackHold

/**
 * Counting Multiplication — Level 1. Shows the same boxed animal groups as
 * Level 0 but hides the operands: the learner taps the two numbers that are
 * being multiplied (order doesn't matter).
 */
@Composable
fun MultiplicationConstructionScreen(
    viewModel: MultiplicationConstructionViewModel,
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
            ConstructionFeedback.None -> return@LaunchedEffect
            ConstructionFeedback.Correct -> FeedbackHold.CORRECT_MS
            ConstructionFeedback.Wrong -> FeedbackHold.WRONG_MS
            ConstructionFeedback.Revealed -> FeedbackHold.REVEALED_MS
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

        AnimalGroups(
            op1 = problem.op1,
            op2 = problem.op2,
            emoji = problem.animal.emoji,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
        )

        // The equation with the two operand blanks filled in as the learner
        // taps. On a wrong/revealed outcome the true operands are shown.
        val reveal = state.feedback == ConstructionFeedback.Wrong ||
            state.feedback == ConstructionFeedback.Revealed
        val left = if (reveal) problem.op1.toString() else state.firstPick?.toString() ?: "_"
        val right = if (reveal) problem.op2.toString() else state.secondPick?.toString() ?: "_"
        Text(
            text = "$left × $right",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            text = when (state.feedback) {
                ConstructionFeedback.Correct -> "Correct!"
                ConstructionFeedback.Wrong -> "Not quite — it was ${problem.op1} × ${problem.op2}"
                ConstructionFeedback.Revealed -> "It was ${problem.op1} × ${problem.op2}"
                ConstructionFeedback.None -> "Which two numbers are being multiplied?"
            },
            fontSize = 16.sp,
            color = when (state.feedback) {
                ConstructionFeedback.Correct -> Color(0xFF22C55E)
                ConstructionFeedback.Wrong -> Color(0xFFEF4444)
                ConstructionFeedback.Revealed -> Color(0xFFFACC15)
                ConstructionFeedback.None -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            },
        )

        NumberPad(
            feedback = state.feedback,
            onPick = viewModel::onPick,
            inputEnabled = inputReady,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(
                onClick = viewModel::clearPicks,
                enabled = state.firstPick != null && state.feedback == ConstructionFeedback.None,
            ) {
                Text("Clear", fontSize = 14.sp)
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
private fun NumberPad(
    feedback: ConstructionFeedback,
    onPick: (Int) -> Unit,
    inputEnabled: Boolean,
) {
    // Buttons keep the same colour throughout — the chosen numbers show in
    // the equation blanks above, so highlighting the pressed button just
    // added confusion.
    val container = MaterialTheme.colorScheme.primary
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
    ) {
        (1..4).forEach { n ->
            Button(
                onClick = { onPick(n) },
                enabled = inputEnabled && feedback == ConstructionFeedback.None,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = container,
                    disabledContainerColor = container,
                    contentColor = Color.White,
                    disabledContentColor = Color.White,
                ),
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            ) {
                Text(
                    text = n.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
