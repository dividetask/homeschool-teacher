package com.dividetask.homeschoolteacher.chess

import androidx.lifecycle.ViewModel
import com.dividetask.homeschoolteacher.Storage
import com.dividetask.homeschoolteacher.lesson.LessonId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ChessFeedback { None, Correct, Wrong, Revealed }

data class ChessState(
    val puzzle: ChessPuzzle,
    val selectedPlayer: Boolean = false,
    val feedback: ChessFeedback = ChessFeedback.None,
    val lastTapped: Int? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
)

private val SUPPORTED_LESSONS = setOf(
    LessonId.Chess0,
    LessonId.Chess1,
    LessonId.Chess2,
    LessonId.Chess3,
)

/**
 * Piece + friendly-pawn configuration for each chess lesson.
 *  - L0: queen alone, opposing pawns only.
 *  - L1: queen, opposing pawns plus a friendly pawn distractor.
 *  - L2: rook with friendly distractor.
 *  - L3: bishop with friendly distractor.
 */
private fun configFor(id: LessonId): ChessLevelConfig = when (id) {
    LessonId.Chess0 -> ChessLevelConfig.Queen
    LessonId.Chess1 -> ChessLevelConfig.QueenWithFriendly
    LessonId.Chess2 -> ChessLevelConfig.Rook
    LessonId.Chess3 -> ChessLevelConfig.Bishop
    else -> ChessLevelConfig.Queen
}

class ChessViewModel : ViewModel() {

    private val streakFlows: MutableMap<LessonId, MutableStateFlow<Int>> =
        SUPPORTED_LESSONS.associateWith {
            MutableStateFlow(Storage.loadWinStreak(it.name))
        }.toMutableMap()

    private val passedFlows: MutableMap<LessonId, MutableStateFlow<Boolean>> =
        SUPPORTED_LESSONS.associateWith {
            MutableStateFlow(Storage.loadLessonPassed(it))
        }.toMutableMap()

    private val _activeLesson = MutableStateFlow(LessonId.Chess0)
    val activeLesson: StateFlow<LessonId> = _activeLesson.asStateFlow()

    private val _state: MutableStateFlow<ChessState>
    val state: StateFlow<ChessState>

    init {
        val (correct, wrong) = Storage.loadChessCounts()
        _state = MutableStateFlow(
            ChessState(
                puzzle = ChessPuzzleGenerator.generate(configFor(_activeLesson.value)),
                correctCount = correct,
                wrongCount = wrong,
            ),
        )
        state = _state.asStateFlow()
    }

    fun streak(id: LessonId): StateFlow<Int> = streakFlows.getValue(id).asStateFlow()
    fun passed(id: LessonId): StateFlow<Boolean> = passedFlows.getValue(id).asStateFlow()

    fun setPassed(id: LessonId, value: Boolean) {
        if (id !in SUPPORTED_LESSONS) return
        passedFlows.getValue(id).value = value
        Storage.saveLessonPassed(id, value)
        Storage.saveLessonManualOverride(id, true)
    }

    fun startLesson(id: LessonId) {
        require(id in SUPPORTED_LESSONS) { "ChessViewModel does not run $id" }
        _activeLesson.value = id
        nextPuzzle()
    }

    fun onSquareTapped(index: Int) {
        val s = _state.value
        if (s.feedback == ChessFeedback.Correct || s.feedback == ChessFeedback.Revealed) return

        if (!s.selectedPlayer) {
            // First tap must be the player piece.
            if (index == s.puzzle.playerIndex) {
                _state.update { it.copy(selectedPlayer = true, feedback = ChessFeedback.None) }
            } else {
                markWrong(index)
            }
            return
        }

        if (index in s.puzzle.capturable) {
            markCorrect(index)
        } else {
            markWrong(index)
        }
    }

    fun nextPuzzle() {
        _state.update {
            it.copy(
                puzzle = ChessPuzzleGenerator.generate(configFor(_activeLesson.value)),
                selectedPlayer = false,
                feedback = ChessFeedback.None,
                lastTapped = null,
            )
        }
    }

    fun giveUp() {
        val s = _state.value
        if (s.feedback == ChessFeedback.Correct || s.feedback == ChessFeedback.Revealed) return
        val active = _activeLesson.value
        streakFlows.getValue(active).value = 0
        Storage.saveWinStreak(active.name, 0)
        val reveal = s.puzzle.capturable.firstOrNull()
        _state.update {
            it.copy(
                selectedPlayer = true,
                feedback = ChessFeedback.Revealed,
                lastTapped = reveal,
                wrongCount = it.wrongCount + 1,
            )
        }
        Storage.saveChessCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    private fun markCorrect(index: Int) {
        val active = _activeLesson.value
        val newStreak = streakFlows.getValue(active).value + 1
        streakFlows.getValue(active).value = newStreak
        Storage.saveWinStreak(active.name, newStreak)
        if (newStreak >= 8 && !passedFlows.getValue(active).value &&
            !Storage.loadLessonManualOverride(active)) {
            passedFlows.getValue(active).value = true
            Storage.saveLessonPassed(active, true)
        }
        _state.update {
            it.copy(
                feedback = ChessFeedback.Correct,
                lastTapped = index,
                correctCount = it.correctCount + 1,
            )
        }
        Storage.saveChessCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    private fun markWrong(index: Int) {
        val active = _activeLesson.value
        streakFlows.getValue(active).value = 0
        Storage.saveWinStreak(active.name, 0)
        _state.update {
            it.copy(
                selectedPlayer = false,
                feedback = ChessFeedback.Wrong,
                lastTapped = index,
                wrongCount = it.wrongCount + 1,
            )
        }
        Storage.saveChessCounts(_state.value.correctCount, _state.value.wrongCount)
    }
}
