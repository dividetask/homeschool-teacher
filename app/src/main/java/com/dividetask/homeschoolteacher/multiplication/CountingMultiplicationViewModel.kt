package com.dividetask.homeschoolteacher.multiplication

import androidx.lifecycle.ViewModel
import com.dividetask.homeschoolteacher.Storage
import com.dividetask.homeschoolteacher.lesson.LessonId
import com.dividetask.homeschoolteacher.reading.Animal
import com.dividetask.homeschoolteacher.reading.Animals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

data class MultiplicationProblem(
    val op1: Int,
    val op2: Int,
    val animal: Animal,
) {
    val answer: Int get() = op1 * op2
}

enum class MultiplicationFeedback { None, Correct, Wrong, Revealed }

data class MultiplicationState(
    val problem: MultiplicationProblem,
    val feedback: MultiplicationFeedback = MultiplicationFeedback.None,
    val selected: Int? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
)

private const val MAX_OPERAND = 4
private const val GRID_SIZE = 10  // pad room for future levels

class CountingMultiplicationViewModel : ViewModel() {

    private val grid: Array<IntArray> = Array(GRID_SIZE) { IntArray(GRID_SIZE) }

    private val _streaks = MutableStateFlow(snapshotGrid())
    val streaks: StateFlow<List<List<Int>>> = _streaks.asStateFlow()

    private val _passed = MutableStateFlow(
        Storage.loadLessonPassed(LessonId.CountingMultiplication0),
    )
    val passed: StateFlow<Boolean> = _passed.asStateFlow()

    private val _state: MutableStateFlow<MultiplicationState>
    val state: StateFlow<MultiplicationState>

    init {
        for (a in 0 until GRID_SIZE) for (b in 0 until GRID_SIZE) {
            grid[a][b] = Storage.loadMultiplicationStreak(a, b)
        }
        evaluatePassedFlag()
        val (correct, wrong) = Storage.loadMultiplicationCounts()
        _state = MutableStateFlow(
            MultiplicationState(
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
        Storage.saveLessonPassed(LessonId.CountingMultiplication0, value)
        Storage.saveLessonManualOverride(LessonId.CountingMultiplication0, true)
    }

    fun onAnswer(choice: Int) {
        val current = _state.value
        if (current.feedback != MultiplicationFeedback.None) return
        val problem = current.problem
        val correct = choice == problem.answer
        val newCell = if (correct) grid[problem.op1][problem.op2] + 1 else 0
        grid[problem.op1][problem.op2] = newCell
        Storage.saveMultiplicationStreak(problem.op1, problem.op2, newCell)
        _streaks.value = snapshotGrid()
        evaluatePassedFlag()
        _state.update {
            it.copy(
                feedback = if (correct) MultiplicationFeedback.Correct else MultiplicationFeedback.Wrong,
                selected = choice,
                correctCount = it.correctCount + if (correct) 1 else 0,
                wrongCount = it.wrongCount + if (correct) 0 else 1,
            )
        }
        Storage.saveMultiplicationCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun giveUp() {
        val current = _state.value
        if (current.feedback == MultiplicationFeedback.Correct ||
            current.feedback == MultiplicationFeedback.Revealed) return
        val problem = current.problem
        grid[problem.op1][problem.op2] = 0
        Storage.saveMultiplicationStreak(problem.op1, problem.op2, 0)
        _streaks.value = snapshotGrid()
        _state.update {
            it.copy(
                feedback = MultiplicationFeedback.Revealed,
                selected = problem.answer,
                wrongCount = it.wrongCount + 1,
            )
        }
        Storage.saveMultiplicationCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun nextProblem() {
        _state.update {
            it.copy(
                problem = chooseProblem(previous = it.problem),
                feedback = MultiplicationFeedback.None,
                selected = null,
            )
        }
    }

    private fun evaluatePassedFlag() {
        if (Storage.loadLessonManualOverride(LessonId.CountingMultiplication0)) return
        val mastered = (0..MAX_OPERAND).all { a ->
            (0..MAX_OPERAND).all { b -> grid[a][b] >= 2 }
        }
        if (mastered && !_passed.value) {
            _passed.value = true
            Storage.saveLessonPassed(LessonId.CountingMultiplication0, true)
        }
    }

    private fun snapshotGrid(): List<List<Int>> = grid.map { it.toList() }

    private fun chooseProblem(previous: MultiplicationProblem?): MultiplicationProblem {
        val allCells: List<Pair<Int, Int>> =
            (0..MAX_OPERAND).flatMap { op1 -> (0..MAX_OPERAND).map { op2 -> op1 to op2 } }
        val wildcard = Random.nextInt(1, 11)
        val pool: List<Pair<Int, Int>> = if (wildcard == 1) {
            allCells
        } else {
            val minVal = allCells.minOf { (a, b) -> grid[a][b] }
            allCells.filter { (a, b) -> grid[a][b] == minVal }
        }
        val finalPool = if (previous != null && pool.size > 1) {
            pool.filter { (a, b) -> a != previous.op1 || b != previous.op2 }
                .ifEmpty { pool }
        } else {
            pool
        }
        val (op1, op2) = finalPool[Random.nextInt(finalPool.size)]
        val animal = Animals.all[Random.nextInt(Animals.all.size)]
        return MultiplicationProblem(op1 = op1, op2 = op2, animal = animal)
    }
}
