package com.dividetask.homeschoolteacher.chess

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dividetask.homeschoolteacher.Tts
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChessScreen(
    viewModel: ChessViewModel,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val active by viewModel.activeLesson.collectAsStateWithLifecycle()
    val streak by viewModel.streak(active).collectAsStateWithLifecycle()
    val pieceName = pieceNoun(state.puzzle.playerType)

    var inputReady by remember { mutableStateOf(false) }
    LaunchedEffect(state.puzzle) {
        inputReady = false
        delay(1000)
        inputReady = true
    }

    LaunchedEffect(state.feedback, state.puzzle) {
        when (state.feedback) {
            ChessFeedback.Correct -> {
                delay(900)
                Tts.stopAll()
                onCompleted()
            }
            ChessFeedback.Wrong -> {
                delay(2000)
                Tts.stopAll()
                onCompleted()
            }
            ChessFeedback.Revealed -> {
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
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ScoreItem("Correct", state.correctCount, Color(0xFF22C55E))
            ScoreItem("Wrong", state.wrongCount, Color(0xFFEF4444))
            ScoreItem("Streak", streak, MaterialTheme.colorScheme.primary)
        }

        Text(
            text = when {
                state.feedback == ChessFeedback.Correct -> "Correct!"
                state.feedback == ChessFeedback.Wrong -> "Not that one — the $pieceName could take a green pawn"
                state.feedback == ChessFeedback.Revealed -> "The $pieceName could take that one"
                state.selectedPlayer -> "Now tap a pawn the $pieceName can take"
                else -> "Tap the $pieceName, then tap a pawn"
            },
            fontSize = 16.sp,
            color = when (state.feedback) {
                ChessFeedback.Correct -> Color(0xFF22C55E)
                ChessFeedback.Wrong -> Color(0xFFEF4444)
                ChessFeedback.Revealed -> Color(0xFFFACC15)
                ChessFeedback.None -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            },
        )

        Board(
            state = state,
            onTap = viewModel::onSquareTapped,
            inputEnabled = inputReady,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .aspectRatio(1f),
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

@Composable
private fun Board(
    state: ChessState,
    onTap: (Int) -> Unit,
    inputEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val padding = 2.dp
        val cellSize = (maxWidth - padding * 2) / 8
        val density = LocalDensity.current
        val cellPx = with(density) { cellSize.toPx() }
        val padPx = with(density) { padding.toPx() }

        val playerX = remember { Animatable(0f) }
        val playerY = remember { Animatable(0f) }
        val animating = state.feedback == ChessFeedback.Correct ||
            state.feedback == ChessFeedback.Revealed

        // Snap the player overlay to its home square whenever the puzzle changes
        // (and once the board size is known) so the next puzzle begins at rest.
        LaunchedEffect(state.puzzle, cellPx, padPx) {
            val col = state.puzzle.playerIndex % 8
            val row = state.puzzle.playerIndex / 8
            playerX.snapTo(padPx + col * cellPx)
            playerY.snapTo(padPx + row * cellPx)
        }

        // Animate the player overlay to the capture target when a capture is
        // registered (correct play or give-up reveal).
        LaunchedEffect(state.feedback, state.puzzle, cellPx, padPx) {
            if (animating) {
                val target = state.lastTapped
                    ?: state.puzzle.capturable.firstOrNull()
                if (target != null) {
                    val col = target % 8
                    val row = target / 8
                    val tx = padPx + col * cellPx
                    val ty = padPx + row * cellPx
                    launch { playerX.animateTo(tx, animationSpec = tween(600)) }
                    launch { playerY.animateTo(ty, animationSpec = tween(600)) }
                }
            }
        }

        Column(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF111827))
                .padding(padding),
        ) {
            for (row in 0 until 8) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    for (col in 0 until 8) {
                        val idx = row * 8 + col
                        val piece = state.puzzle.board[idx]
                        val isLight = (row + col) % 2 == 0
                        val baseColor = if (isLight) Color(0xFFEAD6B0) else Color(0xFF7A5C3F)
                        val selectedPlayer = state.selectedPlayer && idx == state.puzzle.playerIndex
                        val revealing = state.feedback == ChessFeedback.Wrong ||
                            state.feedback == ChessFeedback.Revealed
                        val highlight = when {
                            state.feedback == ChessFeedback.Correct && idx == state.lastTapped ->
                                Color(0xFF22C55E)
                            revealing && idx in state.puzzle.capturable ->
                                Color(0xFF22C55E)
                            state.feedback == ChessFeedback.Wrong && idx == state.lastTapped ->
                                Color(0xFFEF4444)
                            selectedPlayer -> Color(0xFFFACC15)
                            else -> null
                        }
                        val isPlayerHome = idx == state.puzzle.playerIndex
                        val isCaptureTarget = animating && idx == state.lastTapped
                        // Hide the player at its home square while the overlay
                        // is animating to the target, and hide the captured
                        // pawn at the destination so the piece visibly lands
                        // on an empty square.
                        val drawPiece = piece != null &&
                            !(isPlayerHome && animating) &&
                            !isCaptureTarget
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(baseColor)
                                .then(
                                    if (highlight != null) Modifier.border(width = 3.dp, color = highlight)
                                    else Modifier,
                                )
                                .clickable(enabled = inputEnabled && state.feedback == ChessFeedback.None) {
                                    onTap(idx)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (drawPiece && piece != null) {
                                PieceText(piece)
                            }
                        }
                    }
                }
            }
        }

        if (animating) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            playerX.value.roundToInt(),
                            playerY.value.roundToInt(),
                        )
                    }
                    .size(cellSize),
                contentAlignment = Alignment.Center,
            ) {
                PieceText(Piece(state.puzzle.playerType, state.puzzle.playerColor))
            }
        }
    }
}

private fun pieceNoun(type: PieceType): String = when (type) {
    PieceType.Queen -> "queen"
    PieceType.Rook -> "rook"
    PieceType.Bishop -> "bishop"
    PieceType.Pawn -> "pawn"
}

@Composable
private fun PieceText(piece: Piece) {
    val isWhite = piece.color == PieceColor.White
    val glyph = when (piece.type) {
        PieceType.Queen -> "♛"
        PieceType.Rook -> "♜"
        PieceType.Bishop -> "♝"
        PieceType.Pawn -> "♟"
    }
    Text(
        text = glyph,
        style = TextStyle(
            color = if (isWhite) Color.White else Color(0xFF111827),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            shadow = Shadow(
                color = if (isWhite) Color(0xFF111827) else Color.White,
                blurRadius = 6f,
            ),
        ),
    )
}

