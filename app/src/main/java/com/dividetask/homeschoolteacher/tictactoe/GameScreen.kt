package com.dividetask.homeschoolteacher.tictactoe

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.dividetask.homeschoolteacher.AppConfig
import com.dividetask.homeschoolteacher.Tts
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isGameOver) {
        if (state.isGameOver) {
            val seconds = AppConfig.ticTacToeAutoRestartSeconds.coerceAtLeast(0)
            delay(seconds * 1000L)
            Tts.stopAll()
            onCompleted()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        ScoreRow(state)
        Text(
            text = statusText(state),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
        Board(
            state = state,
            onCellTap = viewModel::onCellTapped,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .aspectRatio(1f),
        )
        Button(
            onClick = { viewModel.newGame() },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text("New Game", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ScoreRow(state: GameState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ScoreItem("You (${state.playerMark})", state.playerScore)
        ScoreItem("CPU (${state.cpuMark})", state.cpuScore)
        ScoreItem("Draws", state.drawScore)
    }
}

@Composable
private fun ScoreItem(label: String, value: Int) {
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
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun Board(
    state: GameState,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (col in 0 until 3) {
                        val i = row * 3 + col
                        Cell(
                            mark = state.board[i],
                            isWinning = i in state.winningLine,
                            enabled = !state.isGameOver &&
                                state.board[i] == null &&
                                state.currentTurn == state.playerMark,
                            onTap = { onCellTap(i) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
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
    isWinning: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isWinning) Color(0xFF065F46) else Color(0xFF374151)
    val color = when (mark) {
        Mark.X -> Color(0xFF60A5FA)
        Mark.O -> Color(0xFFF472B6)
        null -> Color.Transparent
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        if (mark != null) {
            Text(
                text = mark.name,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = if (isWinning) Color.White else color,
            )
        }
    }
}

private fun statusText(state: GameState): String = when {
    state.winner == state.playerMark -> "You win!"
    state.winner == state.cpuMark -> "CPU wins!"
    state.isDraw -> "It's a draw!"
    state.currentTurn == state.playerMark -> "Your turn"
    else -> "CPU is thinking…"
}
