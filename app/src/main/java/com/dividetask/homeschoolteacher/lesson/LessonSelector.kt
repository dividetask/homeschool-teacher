package com.dividetask.homeschoolteacher.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dividetask.homeschoolteacher.AppConfig
import com.dividetask.homeschoolteacher.Storage
import com.dividetask.homeschoolteacher.binary.BinaryOperationsViewModel
import com.dividetask.homeschoolteacher.chess.ChessViewModel
import com.dividetask.homeschoolteacher.multiplication.CountingMultiplicationViewModel
import com.dividetask.homeschoolteacher.math.MathViewModel
import com.dividetask.homeschoolteacher.reading.PhonemesViewModel
import com.dividetask.homeschoolteacher.reading.ReadingViewModel
import com.dividetask.homeschoolteacher.reading.RhymingWordsViewModel
import com.dividetask.homeschoolteacher.reading.SightWordsViewModel
import com.dividetask.homeschoolteacher.tictactoe.GameViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class SelectionMode {
    Random,
    SingleLesson,
    Progress,
}

class LessonSelector(
    private val ttt: GameViewModel,
    private val chess: ChessViewModel,
    private val math: MathViewModel,
    private val binary: BinaryOperationsViewModel,
    private val multiplication: CountingMultiplicationViewModel,
    private val phonemes: PhonemesViewModel,
    private val reading: ReadingViewModel,
    private val sightWords: SightWordsViewModel,
    private val rhymingWords: RhymingWordsViewModel,
) : ViewModel() {

    private val _mode = MutableStateFlow(SelectionMode.Random)
    val mode: StateFlow<SelectionMode> = _mode.asStateFlow()

    private val _currentLesson = MutableStateFlow(LessonId.TicTacToe0)
    val currentLesson: StateFlow<LessonId> = _currentLesson.asStateFlow()

    /**
     * Every lesson that contributes to the menu/unlock map. The order
     * here is the order entries are zipped with the combine() output;
     * keep [passedFlows] and [initialPassedMap] in lock-step.
     */
    private val orderedLessons: List<LessonId> = listOf(
        LessonId.TicTacToe0,
        LessonId.TicTacToe1,
        LessonId.TicTacToe2,
        LessonId.Chess0,
        LessonId.Chess1,
        LessonId.Chess2,
        LessonId.Chess3,
        LessonId.MathPictures,
        LessonId.Math0,
        LessonId.HorizontalAddition0,
        LessonId.NumberLineAddition0,
        LessonId.CountingAddition1,
        LessonId.Math1,
        LessonId.HorizontalAddition1,
        LessonId.MathNumberLine,
        LessonId.BinaryOps0,
        LessonId.BinaryOps1,
        LessonId.CountingSubtraction0,
        LessonId.HorizontalSubtraction0,
        LessonId.VerticalSubtraction0,
        LessonId.NumberLineSubtraction0,
        LessonId.CountingMultiplication0,
        LessonId.Phonemes0,
        LessonId.Reading0,
        LessonId.SightWords0,
        LessonId.SightWords1,
        LessonId.RhymingWords0,
    )

    private fun passedFlowFor(id: LessonId): StateFlow<Boolean> = when (id) {
        LessonId.TicTacToe0, LessonId.TicTacToe1, LessonId.TicTacToe2 -> ttt.passed(id)
        LessonId.Chess0, LessonId.Chess1, LessonId.Chess2, LessonId.Chess3 -> chess.passed(id)
        LessonId.MathPictures, LessonId.Math0, LessonId.HorizontalAddition0,
        LessonId.NumberLineAddition0, LessonId.CountingAddition1, LessonId.Math1,
        LessonId.HorizontalAddition1, LessonId.MathNumberLine,
        LessonId.CountingSubtraction0, LessonId.HorizontalSubtraction0,
        LessonId.VerticalSubtraction0, LessonId.NumberLineSubtraction0 -> math.passed(id)
        LessonId.BinaryOps0, LessonId.BinaryOps1 -> binary.passed(id)
        LessonId.CountingMultiplication0 -> multiplication.passed
        LessonId.Phonemes0 -> phonemes.passed
        LessonId.Reading0 -> reading.passed
        LessonId.SightWords0, LessonId.SightWords1 -> sightWords.passed(id)
        LessonId.RhymingWords0 -> rhymingWords.passed
    }

    val passedMap: StateFlow<Map<LessonId, Boolean>> = combine(
        orderedLessons.map { passedFlowFor(it) },
    ) { values: Array<Boolean> ->
        orderedLessons.withIndex().associate { (i, id) -> id to values[i] }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialPassedMap())

    private fun initialPassedMap(): Map<LessonId, Boolean> =
        orderedLessons.associateWith { passedFlowFor(it).value }

    /**
     * Per-lesson manual-unlock override. Persists across launches. When
     * true, the lesson appears in the menu and Random rotation even if
     * its parents haven't been passed. This is a learner / grown-up
     * escape hatch for skipping ahead; it doesn't mark the lesson as
     * passed.
     */
    private val _manualUnlockMap = MutableStateFlow(
        orderedLessons.associateWith { Storage.loadLessonManualUnlock(it) },
    )
    val manualUnlockMap: StateFlow<Map<LessonId, Boolean>> = _manualUnlockMap.asStateFlow()

    fun setManualUnlock(id: LessonId, value: Boolean) {
        Storage.saveLessonManualUnlock(id, value)
        _manualUnlockMap.update { it + (id to value) }
    }

    private var remaining: Int = 1
    private var lastCompletedCategory: Category? = null

    init {
        // Default mode is Random. Roll the first lesson immediately.
        rollRandomLesson(excludeCategory = null)
    }

    /** Pick a specific lesson from the menu. Runs that lesson indefinitely. */
    fun selectLesson(id: LessonId) {
        _mode.value = SelectionMode.SingleLesson
        remaining = Int.MAX_VALUE
        lastCompletedCategory = null
        // Update the lesson's instance state BEFORE flipping currentLesson so
        // that when the screen mounts the first composition already sees the
        // freshly-started instance. Otherwise effects keyed on the lesson
        // state would fire twice (once for the stale state, once for the new
        // one) and any audio cue for the first item would be cut off.
        startInstance(id)
        _currentLesson.value = id
    }

    /** Switch to Random rotation, immediately rolling a new lesson. */
    fun selectRandom() {
        _mode.value = SelectionMode.Random
        lastCompletedCategory = null
        rollRandomLesson(excludeCategory = null)
    }

    /** Switch to the progress / debug view. Activity ViewModels are untouched. */
    fun selectProgress() {
        _mode.value = SelectionMode.Progress
    }

    /** Called by a lesson screen when one instance of the lesson finishes. */
    fun onLessonInstanceCompleted() {
        if (_mode.value == SelectionMode.Random) {
            remaining--
            if (remaining <= 0) {
                lastCompletedCategory = Lessons.get(_currentLesson.value).category
                rollRandomLesson(excludeCategory = lastCompletedCategory)
                return
            }
        }
        startInstance(_currentLesson.value)
    }

    private fun rollRandomLesson(excludeCategory: Category?) {
        val passed = passedMap.value
        val unlocked = Lessons.unlocked(passed, _manualUnlockMap.value)
        val pickedDef = LessonRoulette.choose(unlocked, passed, excludeCategory) ?: return
        remaining = AppConfig.runsPerRound(pickedDef)
        // Same ordering as selectLesson: start the instance first so the
        // screen mounts with the new state already in place.
        startInstance(pickedDef.id)
        _currentLesson.value = pickedDef.id
    }

    private fun startInstance(id: LessonId) {
        when (id) {
            LessonId.TicTacToe0,
            LessonId.TicTacToe1,
            LessonId.TicTacToe2 -> ttt.startLesson(id)
            LessonId.Chess0,
            LessonId.Chess1,
            LessonId.Chess2,
            LessonId.Chess3 -> chess.startLesson(id)
            LessonId.MathPictures,
            LessonId.Math0,
            LessonId.HorizontalAddition0,
            LessonId.NumberLineAddition0,
            LessonId.CountingAddition1,
            LessonId.Math1,
            LessonId.HorizontalAddition1,
            LessonId.MathNumberLine,
            LessonId.CountingSubtraction0,
            LessonId.HorizontalSubtraction0,
            LessonId.VerticalSubtraction0,
            LessonId.NumberLineSubtraction0 -> math.startLesson(id)
            LessonId.BinaryOps0, LessonId.BinaryOps1 -> binary.startLesson(id)
            LessonId.CountingMultiplication0 -> multiplication.startLesson()
            LessonId.Phonemes0 -> phonemes.startLesson()
            LessonId.Reading0 -> reading.startLesson()
            LessonId.SightWords0, LessonId.SightWords1 -> sightWords.startLesson(id)
            LessonId.RhymingWords0 -> rhymingWords.startLesson()
        }
    }

    fun setPassed(id: LessonId, value: Boolean) {
        when (id) {
            LessonId.TicTacToe0,
            LessonId.TicTacToe1,
            LessonId.TicTacToe2 -> ttt.setPassed(id, value)
            LessonId.Chess0,
            LessonId.Chess1,
            LessonId.Chess2,
            LessonId.Chess3 -> chess.setPassed(id, value)
            LessonId.MathPictures,
            LessonId.Math0,
            LessonId.HorizontalAddition0,
            LessonId.NumberLineAddition0,
            LessonId.CountingAddition1,
            LessonId.Math1,
            LessonId.HorizontalAddition1,
            LessonId.MathNumberLine,
            LessonId.CountingSubtraction0,
            LessonId.HorizontalSubtraction0,
            LessonId.VerticalSubtraction0,
            LessonId.NumberLineSubtraction0 -> math.setPassed(id, value)
            LessonId.BinaryOps0, LessonId.BinaryOps1 -> binary.setPassed(id, value)
            LessonId.CountingMultiplication0 -> multiplication.setPassed(value)
            LessonId.Phonemes0 -> phonemes.setPassed(value)
            LessonId.Reading0 -> reading.setPassed(value)
            LessonId.SightWords0, LessonId.SightWords1 -> sightWords.setPassed(id, value)
            LessonId.RhymingWords0 -> rhymingWords.setPassed(value)
        }
    }
}

class LessonSelectorFactory(
    private val ttt: GameViewModel,
    private val chess: ChessViewModel,
    private val math: MathViewModel,
    private val binary: BinaryOperationsViewModel,
    private val multiplication: CountingMultiplicationViewModel,
    private val phonemes: PhonemesViewModel,
    private val reading: ReadingViewModel,
    private val sightWords: SightWordsViewModel,
    private val rhymingWords: RhymingWordsViewModel,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LessonSelector(ttt, chess, math, binary, multiplication, phonemes, reading, sightWords, rhymingWords) as T
    }
}
