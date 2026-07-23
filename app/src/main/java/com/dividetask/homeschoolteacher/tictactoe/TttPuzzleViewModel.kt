package com.dividetask.homeschoolteacher.tictactoe

import androidx.lifecycle.ViewModel
import com.dividetask.homeschoolteacher.Storage
import com.dividetask.homeschoolteacher.lesson.LessonId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

enum class TttPuzzleFeedback { None, Correct, Wrong }

data class TttPuzzleState(
    /** Pre-set board; the learner's move (always X) is applied on tap. */
    val board: List<Mark?>,
    /** The one correct cell — the winning move or the block. */
    val critical: Int,
    /** true = the learner has a winning move; false = they must block. */
    val isWin: Boolean,
    /** The threatening three-in-a-row line (contains [critical]). */
    val line: List<Int>,
    val feedback: TttPuzzleFeedback = TttPuzzleFeedback.None,
    val tapped: Int? = null,
    val streak: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
)

private const val STREAK_TARGET = 8

private val LINES = listOf(
    listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
    listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
    listOf(0, 4, 8), listOf(2, 4, 6),
)

/**
 * "Tic Tac Toe — Win or Block": a single-move puzzle. The board is set up
 * so it is the learner's turn (they play X) and exactly one side has a
 * winning move — either X can complete three-in-a-row (take the win) or O
 * threatens three-in-a-row (block it). The critical cell is the only
 * correct move; any other tap is a loss. Passing needs [STREAK_TARGET]
 * correct in a row.
 */
class TttPuzzleViewModel : ViewModel() {

    private val _passed = MutableStateFlow(Storage.loadLessonPassed(LessonId.TicTacToeWinBlock))
    val passed: StateFlow<Boolean> = _passed.asStateFlow()

    private var streak: Int = Storage.loadWinStreak(LessonId.TicTacToeWinBlock.name)
    private val _streak = MutableStateFlow(streak)
    val streakFlow: StateFlow<Int> = _streak.asStateFlow()

    private val _state = MutableStateFlow(freshState())
    val state: StateFlow<TttPuzzleState> = _state.asStateFlow()

    fun startLesson() {
        nextPuzzle()
    }

    fun setPassed(value: Boolean) {
        _passed.value = value
        Storage.saveLessonPassed(LessonId.TicTacToeWinBlock, value)
        Storage.saveLessonManualOverride(LessonId.TicTacToeWinBlock, true)
    }

    fun onCellTap(i: Int) {
        val cur = _state.value
        if (cur.feedback != TttPuzzleFeedback.None) return
        if (cur.board[i] != null) return
        val correct = i == cur.critical
        val newBoard = cur.board.toMutableList().also { it[i] = Mark.X }

        streak = if (correct) streak + 1 else 0
        _streak.value = streak
        Storage.saveWinStreak(LessonId.TicTacToeWinBlock.name, streak)
        if (correct && streak >= STREAK_TARGET && !_passed.value &&
            !Storage.loadLessonManualOverride(LessonId.TicTacToeWinBlock)) {
            _passed.value = true
            Storage.saveLessonPassed(LessonId.TicTacToeWinBlock, true)
        }

        _state.update {
            it.copy(
                board = newBoard,
                tapped = i,
                feedback = if (correct) TttPuzzleFeedback.Correct else TttPuzzleFeedback.Wrong,
                streak = streak,
                correctCount = it.correctCount + if (correct) 1 else 0,
                wrongCount = it.wrongCount + if (correct) 0 else 1,
            )
        }
    }

    fun nextPuzzle() {
        val p = buildPuzzle(Random)
        _state.update {
            TttPuzzleState(
                board = p.board,
                critical = p.critical,
                isWin = p.isWin,
                line = p.line,
                streak = streak,
                correctCount = it.correctCount,
                wrongCount = it.wrongCount,
            )
        }
    }

    private fun freshState(): TttPuzzleState {
        val p = buildPuzzle(Random)
        return TttPuzzleState(
            board = p.board,
            critical = p.critical,
            isWin = p.isWin,
            line = p.line,
            streak = streak,
        )
    }

    private data class Puzzle(
        val board: List<Mark?>,
        val critical: Int,
        val isWin: Boolean,
        val line: List<Int>,
    )

    /**
     * Build a valid win-or-block position: the threatening side has exactly
     * one winning move (the critical cell) and the other side has none, with
     * equal X/O counts (so it is legally X's turn).
     */
    private fun buildPuzzle(rng: Random): Puzzle {
        repeat(500) {
            val isWin = rng.nextBoolean()
            val threat = if (isWin) Mark.X else Mark.O
            val other = if (threat == Mark.X) Mark.O else Mark.X
            val line = LINES[rng.nextInt(LINES.size)]
            val critical = line[rng.nextInt(line.size)]
            val board = arrayOfNulls<Mark>(9)
            for (idx in line) if (idx != critical) board[idx] = threat
            val slots = (0..8).filter { board[it] == null && it != critical }.shuffled(rng)
            if (slots.size < 2) return@repeat
            board[slots[0]] = other
            board[slots[1]] = other
            val list: List<Mark?> = board.toList()
            if (alreadyWon(list)) return@repeat
            if (winningMoves(list, threat) != listOf(critical)) return@repeat
            if (winningMoves(list, other).isNotEmpty()) return@repeat
            return Puzzle(list, critical, isWin, line)
        }
        // Deterministic fallback (should never be reached): O threatens the
        // top row (0,1,_); X must block at 2. X's pieces (3,7) form no threat.
        val fb = arrayOfNulls<Mark>(9)
        fb[0] = Mark.O; fb[1] = Mark.O
        fb[3] = Mark.X; fb[7] = Mark.X
        return Puzzle(fb.toList(), critical = 2, isWin = false, line = listOf(0, 1, 2))
    }

    private fun winningMoves(board: List<Mark?>, mark: Mark): List<Int> {
        val res = mutableListOf<Int>()
        for (line in LINES) {
            var count = 0
            var empty = -1
            for (i in line) {
                when (board[i]) {
                    mark -> count++
                    null -> empty = i
                    else -> { count = -1; break }
                }
            }
            if (count == 2 && empty != -1) res.add(empty)
        }
        return res.distinct().sorted()
    }

    private fun alreadyWon(board: List<Mark?>): Boolean =
        LINES.any { l ->
            board[l[0]] != null && board[l[0]] == board[l[1]] && board[l[1]] == board[l[2]]
        }
}
