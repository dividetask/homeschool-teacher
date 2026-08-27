package com.dividetask.homeschoolteacher.multiplication

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

data class ConstructionProblem(
    val op1: Int,
    val op2: Int,
    val animal: Animal,
)

enum class ConstructionFeedback { None, Correct, Wrong, Revealed }

data class ConstructionState(
    val problem: ConstructionProblem,
    val feedback: ConstructionFeedback = ConstructionFeedback.None,
    /** First number the learner tapped, or null before the first tap. */
    val firstPick: Int? = null,
    /** Second number tapped — set at the moment the answer is evaluated. */
    val secondPick: Int? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
)

// Operands never include 0 in this lesson.
private const val MIN_OPERAND = 1
private const val MAX_OPERAND = 4
private const val GRID_SIZE = 10  // pad room for future levels

/** A run of this many correct in a row passes the lesson outright. */
private const val RUN_TARGET = 8

/** Every problem this lesson can ask. */
private val ALL_CELLS: List<Pair<Int, Int>> =
    (MIN_OPERAND..MAX_OPERAND).flatMap { a -> (MIN_OPERAND..MAX_OPERAND).map { b -> a to b } }

/**
 * Runner for "Counting Multiplication — Level 1". Same boxed animal groups
 * as Level 0, but instead of asking for the product it asks **which two
 * numbers are being multiplied** — the learner taps the two operands. The
 * answer is order-independent (multiplication is commutative). Operands are
 * 1..4 (no zero). Passing covers every (op1, op2) ∈ 1..4 at streak ≥ 2,
 * tracked in its own grid (separate from the Level 0 product grid).
 */
class MultiplicationConstructionViewModel : ViewModel() {

    private val grid: Array<IntArray> = Array(GRID_SIZE) { IntArray(GRID_SIZE) }

    private val _streaks = MutableStateFlow(snapshotGrid())
    val streaks: StateFlow<List<List<Int>>> = _streaks.asStateFlow()

    private val _passed = MutableStateFlow(
        Storage.loadLessonPassed(LessonId.MultiplicationConstruction0),
    )
    val passed: StateFlow<Boolean> = _passed.asStateFlow()

    // Consecutive-correct run; reaching RUN_TARGET passes the lesson.
    private var runStreak: Int = Storage.loadWinStreak("run.MultiplicationConstruction0")

    private val _state: MutableStateFlow<ConstructionState>
    val state: StateFlow<ConstructionState>

    init {
        for (a in 0 until GRID_SIZE) for (b in 0 until GRID_SIZE) {
            grid[a][b] = Storage.loadMultiplicationOperandsStreak(a, b)
        }
        evaluatePassedFlag()
        val (correct, wrong) = Storage.loadMultiplicationOperandsCounts()
        _state = MutableStateFlow(
            ConstructionState(
                problem = chooseProblem(previous = null),
                correctCount = correct,
                wrongCount = wrong,
            ),
        )
        state = _state.asStateFlow()
    }

    fun startLesson() {
        nextProblem()
    }

    fun setPassed(value: Boolean) {
        _passed.value = value
        Storage.saveLessonPassed(LessonId.MultiplicationConstruction0, value)
        Storage.saveLessonManualOverride(LessonId.MultiplicationConstruction0, value)
    }

    /**
     * Tap a number. The first tap fills the first operand slot; the second
     * tap fills the second slot and submits the answer.
     */
    fun onPick(n: Int) {
        val current = _state.value
        if (current.feedback != ConstructionFeedback.None) return
        val first = current.firstPick
        if (first == null) {
            _state.update { it.copy(firstPick = n) }
            return
        }
        val problem = current.problem
        val correct = setOf(first, n) == setOf(problem.op1, problem.op2)
        val newCell = if (correct) grid[problem.op1][problem.op2] + 1 else 0
        grid[problem.op1][problem.op2] = newCell
        Storage.saveMultiplicationOperandsStreak(problem.op1, problem.op2, newCell)
        _streaks.value = snapshotGrid()
        runStreak = if (correct) runStreak + 1 else 0
        Storage.saveWinStreak("run.MultiplicationConstruction0", runStreak)
        evaluatePassedFlag()
        _state.update {
            it.copy(
                secondPick = n,
                feedback = if (correct) ConstructionFeedback.Correct else ConstructionFeedback.Wrong,
                correctCount = it.correctCount + if (correct) 1 else 0,
                wrongCount = it.wrongCount + if (correct) 0 else 1,
            )
        }
        Storage.saveMultiplicationOperandsCounts(
            _state.value.correctCount,
            _state.value.wrongCount,
        )
    }

    /** Undo the first pick before the answer is submitted (mis-tap escape). */
    fun clearPicks() {
        val current = _state.value
        if (current.feedback != ConstructionFeedback.None) return
        _state.update { it.copy(firstPick = null, secondPick = null) }
    }

    fun giveUp() {
        val current = _state.value
        if (current.feedback == ConstructionFeedback.Correct ||
            current.feedback == ConstructionFeedback.Revealed) return
        val problem = current.problem
        grid[problem.op1][problem.op2] = 0
        Storage.saveMultiplicationOperandsStreak(problem.op1, problem.op2, 0)
        _streaks.value = snapshotGrid()
        runStreak = 0
        Storage.saveWinStreak("run.MultiplicationConstruction0", 0)
        _state.update {
            it.copy(
                feedback = ConstructionFeedback.Revealed,
                wrongCount = it.wrongCount + 1,
            )
        }
        Storage.saveMultiplicationOperandsCounts(
            _state.value.correctCount,
            _state.value.wrongCount,
        )
    }

    fun nextProblem() {
        _state.update {
            it.copy(
                problem = chooseProblem(previous = it.problem),
                feedback = ConstructionFeedback.None,
                firstPick = null,
                secondPick = null,
            )
        }
    }

    private fun evaluatePassedFlag() {
        if (Storage.loadLessonManualOverride(LessonId.MultiplicationConstruction0)) return
        val mastered = PracticeGrid.covered(ALL_CELLS, GridOperation.Multiply) { a, b ->
            grid[a][b]
        }
        if ((mastered || runStreak >= RUN_TARGET) && !_passed.value) {
            _passed.value = true
            Storage.saveLessonPassed(LessonId.MultiplicationConstruction0, true)
        }
    }

    private fun snapshotGrid(): List<List<Int>> = grid.map { it.toList() }

    private fun chooseProblem(previous: ConstructionProblem?): ConstructionProblem {
        val (op1, op2) = PracticeGrid.choose(
            cells = ALL_CELLS,
            operation = GridOperation.Multiply,
            value = { a, b -> grid[a][b] },
            previous = previous?.let { it.op1 to it.op2 },
        )
        val animal = Animals.all[Random.nextInt(Animals.all.size)]
        return ConstructionProblem(op1 = op1, op2 = op2, animal = animal)
    }
}
