package com.dividetask.homeschoolteacher.division

import androidx.lifecycle.ViewModel
import com.dividetask.homeschoolteacher.Storage
import com.dividetask.homeschoolteacher.lesson.LessonId
import com.dividetask.homeschoolteacher.practice.GridOperation
import com.dividetask.homeschoolteacher.practice.PracticeGrid
import com.dividetask.homeschoolteacher.reading.Animal
import com.dividetask.homeschoolteacher.reading.Animals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

data class DivisionProblem(
    /** How many animals are in the middle. Always a multiple of [divisor]. */
    val dividend: Int,
    /** How many groups they get shared into, 1..6. */
    val divisor: Int,
    val animal: Animal,
) {
    val answer: Int get() = dividend / divisor
}

enum class DivisionFeedback { None, Correct, Wrong, Revealed }

data class DivisionState(
    val problem: DivisionProblem,
    val feedback: DivisionFeedback = DivisionFeedback.None,
    val selected: Int? = null,
    /**
     * How many animals the learner has dragged into each pen. Purely a
     * counting aid — the answer is graded off the tapped number, not off
     * whether the pens ended up even. Reset with every new problem.
     */
    val groupCounts: List<Int> = emptyList(),
    /** Which pen the next tapped animal goes into, if any. */
    val selectedGroup: Int? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
) {
    /** Animals still waiting in the middle. */
    val poolCount: Int get() = problem.dividend - groupCounts.sum()
}

/** Largest number of animals a problem ever shows. */
const val MAX_DIVIDEND = 24

/** Divisors run 1..6 — one per pen on the Level 1 screen. */
const val MAX_DIVISOR = 6

/** Pens shown by Level 1, whatever the divisor is. */
const val LEVEL1_GROUPS = MAX_DIVISOR

/** Threshold for the per-lesson consecutive-correct streak gate. */
private const val LESSON_STREAK_TARGET = 4

/** A run of this many correct in a row passes a lesson outright. */
private const val RUN_TARGET = 8

private val SUPPORTED_LESSONS = setOf(
    LessonId.CountingDivision0,
    LessonId.CountingDivision1,
)

/**
 * Every (dividend, divisor) a division problem can use: divisor 1..6, and
 * a dividend that is a whole number of groups of it, up to [MAX_DIVIDEND].
 *
 * Note this leaves most of the coverage grid empty — there is no problem
 * for, say, 7 ÷ 2 — so mastery is judged over these cells only and the
 * grid on the Progress screen is deliberately sparse.
 */
internal val DIVISION_CELLS: List<Pair<Int, Int>> =
    (1..MAX_DIVISOR).flatMap { divisor ->
        (1..MAX_DIVIDEND / divisor).map { quotient -> divisor * quotient to divisor }
    }

/**
 * Runs both animal-division lessons. They ask the same questions and
 * differ only in how much scaffolding the screen gives: Level 0 puts out
 * exactly as many pens as the divisor, so filling them evenly shows the
 * answer; Level 1 always puts out six and leaves the learner to work out
 * how many to use.
 *
 * Each level keeps its own coverage grid, the way the binary lessons do:
 * doing it without the pens giving the answer away is the whole point of
 * Level 1, so Level 0's coverage must not pass it.
 */
class CountingDivisionViewModel : ViewModel() {

    // grid[level][dividend][divisor]; only the cells in DIVISION_CELLS
    // are ever written.
    private val grid: Array<Array<IntArray>> = Storage.loadDivisionStreaks()

    private val _gridFlow = MutableStateFlow(snapshotGrid())

    /** Coverage per level, indexed `[level][dividend][divisor]`. */
    val streaks: StateFlow<List<List<List<Int>>>> = _gridFlow.asStateFlow()

    private val passedFlow: MutableMap<LessonId, MutableStateFlow<Boolean>> =
        SUPPORTED_LESSONS.associateWith {
            MutableStateFlow(Storage.loadLessonPassed(it))
        }.toMutableMap()

    private val manualOverride: MutableMap<LessonId, Boolean> =
        SUPPORTED_LESSONS.associateWith { Storage.loadLessonManualOverride(it) }
            .toMutableMap()

    private val lessonStreaks: MutableMap<LessonId, MutableStateFlow<Int>> =
        SUPPORTED_LESSONS.associateWith {
            MutableStateFlow(Storage.loadWinStreak(it.name))
        }.toMutableMap()

    private val _activeLesson = MutableStateFlow(LessonId.CountingDivision0)
    val activeLesson: StateFlow<LessonId> = _activeLesson.asStateFlow()

    private val _state: MutableStateFlow<DivisionState>
    val state: StateFlow<DivisionState>

    init {
        evaluatePassedFlags()
        val (correct, wrong) = Storage.loadDivisionCounts()
        val problem = chooseProblem(previous = null)
        _state = MutableStateFlow(
            DivisionState(
                problem = problem,
                groupCounts = emptyPens(problem, _activeLesson.value),
                correctCount = correct,
                wrongCount = wrong,
            ),
        )
        state = _state.asStateFlow()
    }

    fun passed(id: LessonId): StateFlow<Boolean> = passedFlow.getValue(id).asStateFlow()

    fun lessonStreak(id: LessonId): StateFlow<Int> = lessonStreaks.getValue(id).asStateFlow()

    fun setPassed(id: LessonId, value: Boolean) {
        if (id !in SUPPORTED_LESSONS) return
        passedFlow.getValue(id).value = value
        manualOverride[id] = value
        Storage.saveLessonPassed(id, value)
        Storage.saveLessonManualOverride(id, value)
    }

    fun startLesson(id: LessonId) {
        require(id in SUPPORTED_LESSONS) { "CountingDivisionViewModel does not run $id" }
        _activeLesson.value = id
        nextProblem()
    }

    /** Tap a pen to make it the one the next animal moves into. */
    fun selectGroup(index: Int) {
        val current = _state.value
        if (current.feedback != DivisionFeedback.None) return
        if (index !in current.groupCounts.indices) return
        _state.update {
            it.copy(selectedGroup = if (it.selectedGroup == index) null else index)
        }
    }

    /** Tap an animal in the middle to move it into the selected pen. */
    fun moveToSelectedGroup() {
        val current = _state.value
        if (current.feedback != DivisionFeedback.None) return
        val target = current.selectedGroup ?: return
        if (current.poolCount <= 0) return
        _state.update {
            it.copy(groupCounts = it.groupCounts.mapIndexed { i, n -> if (i == target) n + 1 else n })
        }
    }

    /** Tap an animal already in a pen to send it back to the middle. */
    fun removeFromGroup(index: Int) {
        val current = _state.value
        if (current.feedback != DivisionFeedback.None) return
        if (current.groupCounts.getOrElse(index) { 0 } <= 0) return
        _state.update {
            it.copy(groupCounts = it.groupCounts.mapIndexed { i, n -> if (i == index) n - 1 else n })
        }
    }

    /** Send every animal back to the middle, keeping the same problem. */
    fun resetGroups() {
        val current = _state.value
        if (current.feedback != DivisionFeedback.None) return
        _state.update { it.copy(groupCounts = it.groupCounts.map { 0 }, selectedGroup = null) }
    }

    fun onAnswer(choice: Int) {
        val current = _state.value
        if (current.feedback != DivisionFeedback.None) return
        val problem = current.problem
        val lesson = _activeLesson.value
        val correct = choice == problem.answer

        val level = levelOf(lesson)
        val cell = if (correct) grid[level][problem.dividend][problem.divisor] + 1 else 0
        grid[level][problem.dividend][problem.divisor] = cell
        Storage.saveDivisionStreak(level, problem.dividend, problem.divisor, cell)
        _gridFlow.value = snapshotGrid()

        val streakFlow = lessonStreaks.getValue(lesson)
        streakFlow.value = if (correct) streakFlow.value + 1 else 0
        Storage.saveWinStreak(lesson.name, streakFlow.value)

        evaluatePassedFlags()

        _state.update {
            it.copy(
                feedback = if (correct) DivisionFeedback.Correct else DivisionFeedback.Wrong,
                selected = choice,
                correctCount = it.correctCount + if (correct) 1 else 0,
                wrongCount = it.wrongCount + if (correct) 0 else 1,
            )
        }
        Storage.saveDivisionCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun giveUp() {
        val current = _state.value
        if (current.feedback == DivisionFeedback.Correct ||
            current.feedback == DivisionFeedback.Revealed) return
        val problem = current.problem
        val lesson = _activeLesson.value
        val level = levelOf(lesson)
        grid[level][problem.dividend][problem.divisor] = 0
        Storage.saveDivisionStreak(level, problem.dividend, problem.divisor, 0)
        _gridFlow.value = snapshotGrid()

        lessonStreaks.getValue(lesson).value = 0
        Storage.saveWinStreak(lesson.name, 0)

        _state.update {
            it.copy(
                feedback = DivisionFeedback.Revealed,
                selected = problem.answer,
                wrongCount = it.wrongCount + 1,
            )
        }
        Storage.saveDivisionCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun nextProblem() {
        _state.update {
            val problem = chooseProblem(previous = it.problem)
            it.copy(
                problem = problem,
                feedback = DivisionFeedback.None,
                selected = null,
                groupCounts = emptyPens(problem, _activeLesson.value),
                selectedGroup = null,
            )
        }
    }

    /**
     * How many empty pens the screen puts out: one per group at Level 0,
     * so filling them evenly reveals the answer, and always six at Level
     * 1, so it doesn't.
     */
    private fun emptyPens(problem: DivisionProblem, lesson: LessonId): List<Int> {
        val count = if (lesson == LessonId.CountingDivision1) LEVEL1_GROUPS else problem.divisor
        return List(count) { 0 }
    }

    /**
     * A lesson passes when EITHER its streak reaches [RUN_TARGET], OR
     * every askable cell has been covered AND the streak has reached
     * [LESSON_STREAK_TARGET] — the same rule the other math lessons use,
     * including the lighter coverage target easy cells get (dividing by
     * one takes one correct answer, not two).
     */
    private fun evaluatePassedFlags() {
        SUPPORTED_LESSONS.forEach { id ->
            if (manualOverride[id] == true) return@forEach
            val level = levelOf(id)
            val covered = PracticeGrid.covered(
                cells = DIVISION_CELLS,
                operation = GridOperation.Divide,
                value = { dividend, divisor -> grid[level][dividend][divisor] },
            )
            val streak = lessonStreaks.getValue(id).value
            val shouldPass = streak >= RUN_TARGET ||
                (covered && streak >= LESSON_STREAK_TARGET)
            val flow = passedFlow.getValue(id)
            if (shouldPass && !flow.value) {
                flow.value = true
                Storage.saveLessonPassed(id, true)
            }
        }
    }

    private fun snapshotGrid(): List<List<List<Int>>> =
        grid.map { perLevel -> perLevel.map { it.toList() } }

    internal fun levelOf(id: LessonId): Int =
        if (id == LessonId.CountingDivision1) 1 else 0

    /** Standard math-grid selection, restricted to the askable cells. */
    private fun chooseProblem(previous: DivisionProblem?): DivisionProblem {
        val cells = grid[levelOf(_activeLesson.value)]
        val (dividend, divisor) = PracticeGrid.choose(
            cells = DIVISION_CELLS,
            operation = GridOperation.Divide,
            value = { dividend, divisor -> cells[dividend][divisor] },
            previous = previous?.let { it.dividend to it.divisor },
        )
        return DivisionProblem(
            dividend = dividend,
            divisor = divisor,
            animal = Animals.all[Random.nextInt(Animals.all.size)],
        )
    }
}
