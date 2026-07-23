package com.dividetask.homeschoolteacher.tictactoe

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

private val X_COLOR = Color(0xFF60A5FA)
private val O_COLOR = Color(0xFFF472B6)
private val MISS_COLOR = Color(0xFFEF4444)

@Composable
fun TttPuzzleScreen(
    viewModel: TttPuzzleViewModel,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Reveal state, reset for each new answer.
    var oppRevealed by remember(state.tapped, state.feedback) { mutableStateOf(false) }
    var lineShown by remember(state.tapped, state.feedback) { mutableStateOf(false) }

    LaunchedEffect(state.tapped, state.feedback) {
        when {
            state.feedback == TttPuzzleFeedback.None -> return@LaunchedEffect
            // Correct + winning move: the learner completes three in a row.
            state.feedback == TttPuzzleFeedback.Correct && state.isWin -> {
                delay(400); lineShown = true; delay(1600); onCompleted()
            }
            // Correct block: no one wins.
            state.feedback == TttPuzzleFeedback.Correct -> {
                delay(1100); onCompleted()
            }
            // Missed winning move: blink the correct move, then show the line.
            state.isWin -> {
                delay(1500); lineShown = true; delay(1100); onCompleted()
            }
            // Missed block: the opponent moves to punish, then the line shows.
            else -> {
                delay(700); oppRevealed = true; delay(400); lineShown = true
                delay(1400); onCompleted()
            }
        }
    }

    // Blink animation for the missed winning move.
    val blink = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "blinkAlpha",
    )
    val showMissMark = state.feedback == TttPuzzleFeedback.Wrong && state.isWin

    // The displayed board: add the opponent's punishing move once revealed.
    val displayBoard = if (oppRevealed) {
        state.board.toMutableList().also { it[state.critical] = Mark.O }
    } else {
        state.board
    }
    val lineColor = if (state.isWin) X_COLOR else O_COLOR

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ScoreItem("Correct", state.correctCount, Color(0xFF22C55E))
            ScoreItem("Streak", state.streak, Color(0xFF3B82F6))
            ScoreItem("Wrong", state.wrongCount, Color(0xFFEF4444))
        }

        Row(
            modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Player indicator: the learner is always X in the puzzle.
            PlayerMark(Mark.X)
            Board(
                board = displayBoard,
                enabled = state.feedback == TttPuzzleFeedback.None,
                missCell = if (showMissMark) state.critical else null,
                missAlpha = blinkAlpha,
                winningLine = if (lineShown) state.line else emptyList(),
                lineColor = lineColor,
                onCellTap = viewModel::onCellTap,
                modifier = Modifier.weight(1f).aspectRatio(1f),
            )
        }
    }
}

@Composable
private fun PlayerMark(mark: Mark) {
    Text(
        text = mark.name,
        fontSize = 64.sp,
        fontWeight = FontWeight.Bold,
        color = if (mark == Mark.X) X_COLOR else O_COLOR,
    )
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
private fun Board(
    board: List<Mark?>,
    enabled: Boolean,
    missCell: Int?,
    missAlpha: Float,
    winningLine: List<Int>,
    lineColor: Color,
    onCellTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0B1220))
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (col in 0 until 3) {
                        val i = row * 3 + col
                        Cell(
                            mark = board[i],
                            missMark = if (i == missCell) missAlpha else null,
                            enabled = enabled && board[i] == null,
                            onTap = { onCellTap(i) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    }
                }
            }
        }
        if (winningLine.size >= 2) {
            WinningLineOverlay(
                line = winningLine,
                color = lineColor,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun Cell(
    mark: Mark?,
    /** When non-null, draw a blinking red "missed move" X at [missMark] alpha. */
    missMark: Float?,
    enabled: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val markColor = when (mark) {
        Mark.X -> X_COLOR
        Mark.O -> O_COLOR
        null -> Color.Transparent
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF374151))
            .clickable(enabled = enabled, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        if (mark != null) {
            Text(text = mark.name, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = markColor)
        } else if (missMark != null) {
            Text(
                text = "X",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = MISS_COLOR,
                modifier = Modifier.alpha(missMark),
            )
        }
    }
}

/** Draws a thick line through the winning cells' centres. */
@Composable
internal fun WinningLineOverlay(line: List<Int>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        fun center(idx: Int): Offset {
            val r = idx / 3
            val c = idx % 3
            return Offset(size.width * (c + 0.5f) / 3f, size.height * (r + 0.5f) / 3f)
        }
        drawLine(
            color = color,
            start = center(line.first()),
            end = center(line.last()),
            strokeWidth = 18f,
            cap = StrokeCap.Round,
        )
    }
}
