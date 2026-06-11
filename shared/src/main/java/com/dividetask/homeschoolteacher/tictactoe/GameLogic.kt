package com.dividetask.homeschoolteacher.tictactoe

enum class Mark { X, O }

data class GameState(
    val board: List<Mark?> = List(9) { null },
    val playerMark: Mark = Mark.X,
    val cpuMark: Mark = Mark.O,
    val currentTurn: Mark = Mark.X,
    val winner: Mark? = null,
    val winningLine: List<Int> = emptyList(),
    val isDraw: Boolean = false,
    val playerScore: Int = 0,
    val cpuScore: Int = 0,
    val drawScore: Int = 0,
) {
    val isGameOver: Boolean get() = winner != null || isDraw
}

private val WINNING_LINES = listOf(
    listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
    listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
    listOf(0, 4, 8), listOf(2, 4, 6),
)

fun GameState.evaluate(): GameState {
    for (line in WINNING_LINES) {
        val (a, b, c) = Triple(board[line[0]], board[line[1]], board[line[2]])
        if (a != null && a == b && a == c) {
            return copy(winner = a, winningLine = line)
        }
    }
    if (board.all { it != null }) return copy(isDraw = true)
    return this
}

fun GameState.emptyCells(): List<Int> = board.mapIndexedNotNull { i, m -> if (m == null) i else null }

fun GameState.findWinningMoveFor(mark: Mark): Int? {
    for (line in WINNING_LINES) {
        var matchCount = 0
        var emptyIndex = -1
        for (idx in line) {
            when (board[idx]) {
                mark -> matchCount++
                null -> emptyIndex = idx
                else -> { matchCount = -1; break }
            }
        }
        if (matchCount == 2 && emptyIndex != -1) return emptyIndex
    }
    return null
}
