package com.dividetask.homeschoolteacher.tictactoe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

@Composable
fun TttPuzzleScreen(
    viewModel: TttPuzzleViewModel,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.feedback, state.board) {
        val hold = when (state.feedback) {
            TttPuzzleFeedback.None -> return@LaunchedEffect
            TttPuzzleFeedback.Correct -> 1200L
            TttPuzzleFeedback.Wrong -> 2200L
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
            ScoreItem("Streak", state.streak, Color(0xFF3B82F6))
            ScoreItem("Wrong", state.wrongCount, Color(0xFFEF4444))
        }

        Text(
            text = "You are X. Win if you can — otherwise block O!",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )

        Board(
            state = state,
            onCellTap = viewModel::onCellTap,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .aspectRatio(1f),
        )

        Text(
            text = when (state.feedback) {
                TttPuzzleFeedback.Correct -> if (state.isWin) "You win! 🎉" else "Blocked! 🛡️"
                TttPuzzleFeedback.Wrong ->
                    if (state.isWin) "That loses — the winning move is outlined"
                    else "That loses — the block is outlined"
                TttPuzzleFeedback.None -> " "
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = when (state.feedback) {
                TttPuzzleFeedback.Correct -> Color(0xFF22C55E)
                TttPuzzleFeedback.Wrong -> Color(0xFFEF4444)
                TttPuzzleFeedback.None -> Color.Transparent
            },
        )
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
private fun Board(
    state: TttPuzzleState,
    onCellTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val answered = state.feedback != TttPuzzleFeedback.None
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
                        // After answering, outline the critical cell (green if
                        // they took it, yellow if they missed it) and flag a
                        // wrong tap in red.
                        val outline = when {
                            !answered -> null
                            i == state.critical && state.feedback == TttPuzzleFeedback.Correct -> Color(0xFF22C55E)
                            i == state.critical -> Color(0xFFFACC15)
                            i == state.tapped -> Color(0xFFEF4444)
                            else -> null
                        }
                        Cell(
                            mark = state.board[i],
                            outline = outline,
                            enabled = !answered && state.board[i] == null,
                            onTap = { onCellTap(i) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Cell(
    mark: Mark?,
    outline: Color?,
    enabled: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val markColor = when (mark) {
        Mark.X -> Color(0xFF60A5FA)
        Mark.O -> Color(0xFFF472B6)
        null -> Color.Transparent
    }
    var box = modifier
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF374151))
    if (outline != null) {
        box = box.border(3.dp, outline, RoundedCornerShape(12.dp))
    }
    Box(
        modifier = box.clickable(enabled = enabled, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        if (mark != null) {
            Text(
                text = mark.name,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = markColor,
            )
        }
    }
}
