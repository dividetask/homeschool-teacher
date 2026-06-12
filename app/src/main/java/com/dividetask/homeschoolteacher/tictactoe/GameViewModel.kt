package com.dividetask.homeschoolteacher.tictactoe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dividetask.homeschoolteacher.Storage
import com.dividetask.homeschoolteacher.lesson.LessonId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

private val SUPPORTED_LESSONS = setOf(
    LessonId.TicTacToe0,
    LessonId.TicTacToe1,
    LessonId.TicTacToe2,
)

class GameViewModel : ViewModel() {

    private val streaks: MutableMap<LessonId, MutableStateFlow<Int>> =
        SUPPORTED_LESSONS.associateWith {
            MutableStateFlow(Storage.loadWinStreak(it.name))
        }.toMutableMap()

    private val passedFlow: MutableMap<LessonId, MutableStateFlow<Boolean>> =
        SUPPORTED_LESSONS.associateWith {
            MutableStateFlow(Storage.loadLessonPassed(it))
        }.toMutableMap()

    private val _activeLesson = MutableStateFlow(LessonId.TicTacToe0)
    val activeLesson: StateFlow<LessonId> = _activeLesson.asStateFlow()

    // The level the CPU plays at this specific game. With 10% probability
    // it is downgraded to a random lower level when the lesson is L1+.
    private var cpuGameLevel: Int = 0

    private val _state: MutableStateFlow<GameState>
    val state: StateFlow<GameState>

    init {
        val (player, cpu, draw) = Storage.loadTttScores()
        _state = MutableStateFlow(
            GameState(
                playerScore = player,
                cpuScore = cpu,
                drawScore = draw,
            ),
        )
        state = _state.asStateFlow()
    }

    fun streak(id: LessonId): StateFlow<Int> = streaks.getValue(id).asStateFlow()
    fun passed(id: LessonId): StateFlow<Boolean> = passedFlow.getValue(id).asStateFlow()

    fun setPassed(id: LessonId, value: Boolean) {
        if (id !in SUPPORTED_LESSONS) return
        passedFlow.getValue(id).value = value
        Storage.saveLessonPassed(id, value)
        Storage.saveLessonManualOverride(id, true)
    }

    fun startLesson(id: LessonId) {
        require(id in SUPPORTED_LESSONS) { "GameViewModel does not run $id" }
        _activeLesson.value = id
        newGame()
    }

    fun newGame() {
        val playerMark = if (Random.nextBoolean()) Mark.X else Mark.O
        val cpuMark = if (playerMark == Mark.X) Mark.O else Mark.X
        val previous = _state.value
        _state.value = GameState(
            playerMark = playerMark,
            cpuMark = cpuMark,
            currentTurn = Mark.X,
            playerScore = previous.playerScore,
            cpuScore = previous.cpuScore,
            drawScore = previous.drawScore,
        )

        val baseLevel = lessonAiLevel(_activeLesson.value)
        cpuGameLevel = if (baseLevel > 0 && Random.nextDouble() < 0.10) {
            Random.nextInt(0, baseLevel)
        } else {
            baseLevel
        }

        if (_state.value.currentTurn == cpuMark) {
            scheduleCpuMove()
        }
    }

    fun onCellTapped(index: Int) {
        val s = _state.value
        if (s.isGameOver) return
        if (s.currentTurn != s.playerMark) return
        if (s.board[index] != null) return
        applyMove(index, s.playerMark)
        if (!_state.value.isGameOver) scheduleCpuMove()
    }

    private fun lessonAiLevel(id: LessonId): Int = when (id) {
        LessonId.TicTacToe0 -> 0
        LessonId.TicTacToe1 -> 1
        LessonId.TicTacToe2 -> 2
        else -> 0
    }

    private fun scheduleCpuMove() {
        viewModelScope.launch {
            delay(500)
            val s = _state.value
            if (s.isGameOver) return@launch
            val pick = pickCpuMove(s) ?: return@launch
            applyMove(pick, s.cpuMark)
        }
    }

    /**
     * Per docs § Tic Tac Toe levels:
     *  - L0: uniformly random legal move.
     *  - L1: take a winning move if one exists, else random.
     *  - L2: take a winning move if one exists, else block the
     *    opponent's winning move if one exists, else random.
     */
    private fun pickCpuMove(s: GameState): Int? {
        val choices = s.emptyCells()
        if (choices.isEmpty()) return null
        if (cpuGameLevel >= 1) {
            s.findWinningMoveFor(s.cpuMark)?.let { return it }
        }
        if (cpuGameLevel >= 2) {
            s.findWinningMoveFor(s.playerMark)?.let { return it }
        }
        return choices[Random.nextInt(choices.size)]
    }

    private fun applyMove(index: Int, mark: Mark) {
        val before = _state.value
        _state.update { s ->
            val newBoard = s.board.toMutableList().also { it[index] = mark }
            val next = if (mark == Mark.X) Mark.O else Mark.X
            s.copy(board = newBoard, currentTurn = next).evaluate().let { evaluated ->
                when {
                    evaluated.winner == s.playerMark -> evaluated.copy(playerScore = s.playerScore + 1)
                    evaluated.winner == s.cpuMark -> evaluated.copy(cpuScore = s.cpuScore + 1)
                    evaluated.isDraw -> evaluated.copy(drawScore = s.drawScore + 1)
                    else -> evaluated
                }
            }
        }
        val after = _state.value
        if (!before.isGameOver && after.isGameOver) {
            val lesson = _activeLesson.value
            val isLoss = after.winner != null && after.winner == after.cpuMark
            val streakFlow = streaks.getValue(lesson)
            if (isLoss) {
                streakFlow.value = 0
            } else {
                streakFlow.value = streakFlow.value + 1
                if (streakFlow.value >= 8 && !passedFlow.getValue(lesson).value &&
                    !Storage.loadLessonManualOverride(lesson)) {
                    passedFlow.getValue(lesson).value = true
                    Storage.saveLessonPassed(lesson, true)
                }
            }
            Storage.saveWinStreak(lesson.name, streakFlow.value)
            Storage.saveTttScores(after.playerScore, after.cpuScore, after.drawScore)
        }
    }
}
