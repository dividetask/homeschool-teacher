package com.dividetask.homeschoolteacher.division

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.dividetask.homeschoolteacher.ui.FeedbackHold
import kotlinx.coroutines.delay

private val CORRECT_GREEN = Color(0xFF22C55E)
private val WRONG_RED = Color(0xFFEF4444)
private val REVEAL_YELLOW = Color(0xFFFACC15)

/**
 * Animal division. The problem is stated as `X ÷ Y = ?` with X animals
 * loose in the middle and a column of pens down the side. Tapping a pen
 * then tapping an animal moves that animal in, so a child can share the
 * animals out and *see* what dividing does. It is only an aid — the
 * answer is graded off the tapped number, so a child who already knows
 * it can skip the sorting entirely.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountingDivisionScreen(
    viewModel: CountingDivisionViewModel,
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
            DivisionFeedback.Correct -> FeedbackHold.CORRECT_MS
            DivisionFeedback.Wrong -> FeedbackHold.WRONG_MS
            DivisionFeedback.Revealed -> FeedbackHold.REVEALED_MS
            DivisionFeedback.None -> return@LaunchedEffect
        }
        delay(hold)
        Tts.stopAll()
        onCompleted()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ScoreItem("Correct", state.correctCount, CORRECT_GREEN)
            ScoreItem("Wrong", state.wrongCount, WRONG_RED)
        }

        Text(
            text = "${problem.dividend} ÷ ${problem.divisor} = ?",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        SortingArea(
            state = state,
            onSelectGroup = viewModel::selectGroup,
            onTakeFromPool = viewModel::moveToSelectedGroup,
            onRemoveFromGroup = viewModel::removeFromGroup,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )

        Text(
            text = when (state.feedback) {
                DivisionFeedback.Correct -> "Correct!"
                DivisionFeedback.Wrong -> "Not quite — the answer was ${problem.answer}"
                DivisionFeedback.Revealed -> "The answer was ${problem.answer}"
                DivisionFeedback.None -> when {
                    state.selectedGroup != null -> "Now tap an animal to move it in"
                    else -> "Share the ${problem.animal.name.lowercase()}s into " +
                        "${problem.divisor} equal groups"
                }
            },
            fontSize = 14.sp,
            color = when (state.feedback) {
                DivisionFeedback.Correct -> CORRECT_GREEN
                DivisionFeedback.Wrong -> WRONG_RED
                DivisionFeedback.Revealed -> REVEAL_YELLOW
                DivisionFeedback.None -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            },
        )

        ChoiceGrid(
            selected = state.selected,
            feedback = state.feedback,
            correct = problem.answer,
            onChoose = viewModel::onAnswer,
            inputEnabled = inputReady,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(
                onClick = viewModel::resetGroups,
                enabled = state.groupCounts.any { it > 0 } &&
                    state.feedback == DivisionFeedback.None,
            ) {
                Text("Start over", fontSize = 14.sp)
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

/** The pens down the side and the loose animals in the middle. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SortingArea(
    state: DivisionState,
    onSelectGroup: (Int) -> Unit,
    onTakeFromPool: () -> Unit,
    onRemoveFromGroup: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val emoji = state.problem.animal.emoji
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier
                .width(124.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.groupCounts.forEachIndexed { index, count ->
                GroupPen(
                    count = count,
                    emoji = emoji,
                    selected = state.selectedGroup == index,
                    onSelect = { onSelectGroup(index) },
                    onRemove = { onRemoveFromGroup(index) },
                )
            }
        }

        AnimalPool(
            count = state.poolCount,
            emoji = emoji,
            armed = state.selectedGroup != null,
            onTake = onTakeFromPool,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * One pen. Tapping the pen arms it; tapping an animal already inside
 * sends that animal back to the middle, so a wrong share can be undone
 * one animal at a time.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupPen(
    count: Int,
    emoji: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    val border = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .border(
                width = if (selected) 3.dp else 1.5.dp,
                color = border,
                shape = RoundedCornerShape(12.dp),
            )
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(12.dp),
            )
            .clickable { onSelect() }
            .padding(horizontal = 6.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (count == 0) {
            Text(
                text = if (selected) "tap an animal" else " ",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                repeat(count) {
                    Text(
                        text = emoji,
                        fontSize = 18.sp,
                        modifier = Modifier.clickable { onRemove() },
                    )
                }
            }
        }
    }
}

/** The animals still loose in the middle, waiting to be shared out. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimalPool(
    count: Int,
    emoji: String,
    armed: Boolean,
    onTake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (count == 0) {
            Text(
                text = "All shared out!",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                repeat(count) {
                    Text(
                        text = emoji,
                        fontSize = 24.sp,
                        modifier = Modifier.clickable(enabled = armed) { onTake() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceGrid(
    selected: Int?,
    feedback: DivisionFeedback,
    correct: Int,
    onChoose: (Int) -> Unit,
    inputEnabled: Boolean,
) {
    // The grid always covers every answer the lesson can ask for, so its
    // size never hints at how big this particular answer is.
    val cells = (0..MAX_DIVIDEND).toList()
    val cols = 7
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
    ) {
        cells.chunked(cols).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { choice ->
                    val container = when {
                        feedback == DivisionFeedback.None -> MaterialTheme.colorScheme.primary
                        choice == correct -> CORRECT_GREEN
                        choice == selected -> WRONG_RED
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    }
                    Button(
                        onClick = { onChoose(choice) },
                        enabled = inputEnabled && feedback == DivisionFeedback.None,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = container,
                            disabledContainerColor = container,
                            contentColor = Color.White,
                            disabledContentColor = Color.White,
                        ),
                        modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                    ) {
                        Text(
                            text = choice.toString(),
                            fontSize = 16.sp,
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
