package com.dividetask.homeschoolteacher.binary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dividetask.homeschoolteacher.Tts
import kotlinx.coroutines.delay

@Composable
fun BinaryOperationsScreen(
    viewModel: BinaryOperationsViewModel,
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
            BinaryFeedback.Correct -> {
                delay(900)
                Tts.stopAll()
                onCompleted()
            }
            BinaryFeedback.Wrong -> {
                delay(2000)
                Tts.stopAll()
                onCompleted()
            }
            BinaryFeedback.Revealed -> {
                delay(1600)
                Tts.stopAll()
                onCompleted()
            }
            else -> Unit
        }
    }

    // The cheat sheet is hidden behind a button. Showing it starts an
    // 8-second auto-hide; pressing the button again hides it early. Resets
    // to hidden on each new problem.
    var showCheat by remember(state.problem) { mutableStateOf(false) }
    LaunchedEffect(showCheat) {
        if (showCheat) {
            delay(8000)
            showCheat = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                ScoreItem("Correct", state.correctCount, Color(0xFF22C55E))
                ScoreItem("Wrong", state.wrongCount, Color(0xFFEF4444))
            }

            Button(
                onClick = { showCheat = !showCheat },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = if (showCheat) "Hide cheat sheet" else "Cheat sheet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            BinaryEquation(
                problem = problem,
                input = state.answerInput,
                feedback = state.feedback,
            )

            Text(
                text = when (state.feedback) {
                    BinaryFeedback.Correct -> "Correct!"
                    BinaryFeedback.Wrong -> "Not quite — the answer was ${problem.answerBinary}₂"
                    BinaryFeedback.Revealed -> "The answer was ${problem.answerBinary}₂"
                    BinaryFeedback.None -> "${problem.operator.verbalName} — pick each binary digit"
                },
                fontSize = 16.sp,
                color = when (state.feedback) {
                    BinaryFeedback.Correct -> Color(0xFF22C55E)
                    BinaryFeedback.Wrong -> Color(0xFFEF4444)
                    BinaryFeedback.Revealed -> Color(0xFFFACC15)
                    BinaryFeedback.None -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                },
            )

            BinaryKeypad(
                inputEnabled = inputReady && state.feedback == BinaryFeedback.None,
                canBack = state.answerInput.isNotEmpty(),
                onDigit = viewModel::onDigit,
                onBack = viewModel::onBack,
            )

            TextButton(onClick = viewModel::giveUp) {
                Text("Give up", fontSize = 14.sp)
            }
        }

        if (showCheat) {
            CheatSheetOverlay(
                operator = problem.operator,
                onDismiss = { showCheat = false },
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

/**
 * Full-screen cheat-sheet overlay for the current operator only: the
 * single-bit truth table (0/1 × 0/1), each entry drawn in the same stacked
 * layout and size as the problem itself. For Level 0 (1-bit) these are
 * literally every possible question; for Level 1 (3-bit) they are the rule
 * applied to each column. Tap anywhere to close.
 */
@Composable
private fun CheatSheetOverlay(operator: BinaryOperator, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "${operator.verbalName} cheat sheet — tap to close",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            listOf(listOf(0 to 0, 0 to 1), listOf(1 to 0, 1 to 1)).forEach { rowPairs ->
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    rowPairs.forEach { (a, b) -> CheatSheetEntry(a, b, operator) }
                }
            }
        }
    }
}

/** One truth-table entry, stacked and sized exactly like the problem. */
@Composable
private fun CheatSheetEntry(op1: Int, op2: Int, operator: BinaryOperator) {
    val onBg = MaterialTheme.colorScheme.onBackground
    Column(horizontalAlignment = Alignment.End) {
        BinaryLine(leading = "  ", digits = op1.toString(), color = onBg, subscript = true)
        BinaryLine(leading = "${operator.verbalName} ", digits = op2.toString(), color = onBg, subscript = true)
        Text(
            text = "─".repeat(3),
            fontFamily = FontFamily.Monospace,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = onBg,
        )
        BinaryLine(leading = "  ", digits = operator.apply(op1, op2).toString(), color = onBg, subscript = true)
    }
}

@Composable
private fun BinaryEquation(
    problem: BinaryProblem,
    input: String,
    feedback: BinaryFeedback,
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val ruleWidth = problem.bits + 2
    val rule = "─".repeat(ruleWidth)
    val inputColor = when (feedback) {
        BinaryFeedback.Correct -> Color(0xFF22C55E)
        BinaryFeedback.Wrong -> Color(0xFFEF4444)
        BinaryFeedback.Revealed -> Color(0xFFFACC15)
        BinaryFeedback.None -> MaterialTheme.colorScheme.primary
    }
    // Each leading is two characters wide so the operand digits sit one
    // space to the right of the operator (e.g. "& 011" rather than the
    // visually crowded "&011"). The empty-leading lines pad to the same
    // width so the columns stay aligned.
    Column(horizontalAlignment = Alignment.End) {
        BinaryLine(
            leading = "  ",
            digits = problem.op1Binary,
            color = onBg,
            subscript = true,
        )
        BinaryLine(
            leading = "${problem.operator.verbalName} ",
            digits = problem.op2Binary,
            color = onBg,
            subscript = true,
        )
        Text(
            text = rule,
            fontFamily = FontFamily.Monospace,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = onBg,
        )
        val padded = input.padEnd(problem.bits, '_')
        BinaryLine(
            leading = "  ",
            digits = padded,
            color = inputColor,
            subscript = feedback != BinaryFeedback.None,
        )
    }
}

@Composable
private fun BinaryLine(
    leading: String,
    digits: String,
    color: Color,
    subscript: Boolean,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = leading,
            fontFamily = FontFamily.Monospace,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = digits,
            fontFamily = FontFamily.Monospace,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        if (subscript) {
            Text(
                text = "₂",
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun BinaryKeypad(
    inputEnabled: Boolean,
    canBack: Boolean,
    onDigit: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BinaryButton(
                text = "0",
                onTap = { onDigit(0) },
                container = MaterialTheme.colorScheme.primary,
                enabled = inputEnabled,
                modifier = Modifier.weight(1f).heightIn(min = 64.dp),
            )
            BinaryButton(
                text = "1",
                onTap = { onDigit(1) },
                container = MaterialTheme.colorScheme.primary,
                enabled = inputEnabled,
                modifier = Modifier.weight(1f).heightIn(min = 64.dp),
            )
        }
        BinaryButton(
            text = "Back",
            onTap = onBack,
            container = MaterialTheme.colorScheme.secondary,
            enabled = inputEnabled && canBack,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        )
    }
}

@Composable
private fun BinaryButton(
    text: String,
    onTap: () -> Unit,
    container: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val effective = if (enabled) container else container.copy(alpha = 0.4f)
    Button(
        onClick = onTap,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = effective,
            disabledContainerColor = effective,
            contentColor = Color.White,
            disabledContentColor = Color.White,
        ),
        modifier = modifier,
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
