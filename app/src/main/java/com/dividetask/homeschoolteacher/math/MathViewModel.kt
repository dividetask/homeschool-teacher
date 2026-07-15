package com.dividetask.homeschoolteacher.math

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

enum class MathOperator(val symbol: String) { Plus("+"), Minus("−"), Times("×") }

data class MathProblem(
    val left: Int,
    val right: Int,
    val operator: MathOperator = MathOperator.Plus,
    /** Only populated for picture-style problems. */
    val leftAnimal: Animal? = null,
    /** Only populated for picture-style problems. */
    val rightAnimal: Animal? = null,
) {
    val answer: Int get() = when (operator) {
        MathOperator.Plus -> left + right
        MathOperator.Minus -> left - right
        MathOperator.Times -> left * right
    }
}

enum class MathFeedback { None, Correct, Wrong, Revealed }

data class MathState(
    val problem: MathProblem,
    val feedback: MathFeedback = MathFeedback.None,
    val selected: Int? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
)

/** Threshold for the per-lesson consecutive-correct streak gate. */
private const val LESSON_STREAK_TARGET = 4

private val SUPPORTED_LESSONS = setOf(
    // Addition L0
    LessonId.MathPictures,
    LessonId.Math0,
    LessonId.HorizontalAddition0,
    LessonId.NumberLineAddition0,
    // Addition L1
    LessonId.CountingAddition1,
    LessonId.Math1,
    LessonId.HorizontalAddition1,
    LessonId.MathNumberLine,
    // Subtraction L0
    LessonId.CountingSubtraction0,
    LessonId.HorizontalSubtraction0,
    LessonId.VerticalSubtraction0,
    LessonId.NumberLineSubtraction0,
    // Multiplication equations (tap the product), operands 0..4
    LessonId.HorizontalMultiplication0,
    LessonId.VerticalMultiplication0,
    LessonId.NumberLineMultiplication0,
    // Multiplication equations Level 1 (type the product), operands 0..9
    LessonId.HorizontalMultiplication1,
    LessonId.VerticalMultiplication1,
    LessonId.NumberLineMultiplication1,
)

private fun lessonOperator(id: LessonId): MathOperator = when (id) {
    LessonId.CountingSubtraction0,
    LessonId.HorizontalSubtraction0,
    LessonId.VerticalSubtraction0,
    LessonId.NumberLineSubtraction0 -> MathOperator.Minus
    LessonId.HorizontalMultiplication0,
    LessonId.VerticalMultiplication0,
    LessonId.NumberLineMultiplication0,
    LessonId.HorizontalMultiplication1,
    LessonId.VerticalMultiplication1,
    LessonId.NumberLineMultiplication1 -> MathOperator.Times
    else -> MathOperator.Plus
}

/**
 * Range used for the LEFT operand. For subtraction this is op1 ∈ 4..9
 * so the answer can't be negative; for addition variants left and right
 * share the same range (declared in [lessonRightRange] below as well).
 * Pictures (Counting Addition L0) starts at 1 because rendering 0 emoji
 * is not meaningful.
 */
private fun lessonLeftRange(id: LessonId): IntRange = when (id) {
    LessonId.MathPictures -> 1..4
    LessonId.Math0,
    LessonId.HorizontalAddition0,
    LessonId.NumberLineAddition0 -> 0..4
    LessonId.CountingAddition1,
    LessonId.Math1,
    LessonId.HorizontalAddition1,
    LessonId.MathNumberLine -> 0..9
    LessonId.CountingSubtraction0,
    LessonId.HorizontalSubtraction0,
    LessonId.VerticalSubtraction0,
    LessonId.NumberLineSubtraction0 -> 4..9
    LessonId.HorizontalMultiplication0,
    LessonId.VerticalMultiplication0,
    LessonId.NumberLineMultiplication0 -> 0..4
    LessonId.HorizontalMultiplication1,
    LessonId.VerticalMultiplication1,
    LessonId.NumberLineMultiplication1 -> 0..9
    else -> 0..4
}

/** Range used for the RIGHT operand. */
private fun lessonRightRange(id: LessonId): IntRange = when (id) {
    LessonId.MathPictures -> 1..4
    LessonId.Math0,
    LessonId.HorizontalAddition0,
    LessonId.NumberLineAddition0 -> 0..4
    LessonId.CountingAddition1,
    LessonId.Math1,
    LessonId.HorizontalAddition1,
    LessonId.MathNumberLine -> 0..9
    LessonId.CountingSubtraction0,
    LessonId.HorizontalSubtraction0,
    LessonId.VerticalSubtraction0,
    LessonId.NumberLineSubtraction0 -> 0..4
    LessonId.HorizontalMultiplication0,
    LessonId.VerticalMultiplication0,
    LessonId.NumberLineMultiplication0 -> 0..4
    LessonId.HorizontalMultiplication1,
    LessonId.VerticalMultiplication1,
    LessonId.NumberLineMultiplication1 -> 0..9
    else -> 0..4
}

private fun isPictureLesson(id: LessonId): Boolean = when (id) {
    LessonId.MathPictures,
    LessonId.CountingAddition1,
    LessonId.CountingSubtraction0 -> true
    else -> false
}

class MathViewModel : ViewModel() {

    // Addition cell grid (existing math.streak). Cell value tracks the
    // count of correct answers for (left, right). Shared across every
    // addition lesson.
    private val additionStreaks: Array<IntArray> = Storage.loadMathStreaks()

    // Subtraction cell grid. Same shape, separate storage; cells track
    // op1 - op2 correctness.
    private val subtractionStreaks: Array<IntArray> = Storage.loadSubtractionStreaks()

    // Multiplication (product) cell grid, shared by the Horizontal /
    // Vertical / Number Line multiplication presentations.
    private val multiplicationStreaks: Array<IntArray> = Storage.loadMultiplicationGridStreaks()

    private val _additionGridFlow = MutableStateFlow(snapshot(additionStreaks))
    val streaks: StateFlow<List<List<Int>>> = _additionGridFlow.asStateFlow()

    private val _subtractionGridFlow = MutableStateFlow(snapshot(subtractionStreaks))
    val subtractionGrid: StateFlow<List<List<Int>>> = _subtractionGridFlow.asStateFlow()

    private val _multiplicationGridFlow = MutableStateFlow(snapshot(multiplicationStreaks))
    val multiplicationGrid: StateFlow<List<List<Int>>> = _multiplicationGridFlow.asStateFlow()

    private val passedFlow: MutableMap<LessonId, MutableStateFlow<Boolean>> =
        SUPPORTED_LESSONS.associateWith {
            MutableStateFlow(Storage.loadLessonPassed(it))
        }.toMutableMap()

    private val manualOverride: MutableMap<LessonId, Boolean> =
        SUPPORTED_LESSONS.associateWith { Storage.loadLessonManualOverride(it) }
            .toMutableMap()

    // Per-lesson consecutive-correct streak. Increments on a correct
    // answer in that lesson; resets on a wrong answer or Give up.
    // Independent of the cell grids — passing requires both.
    private val lessonStreaks: MutableMap<LessonId, MutableStateFlow<Int>> =
        SUPPORTED_LESSONS.associateWith {
            MutableStateFlow(Storage.loadWinStreak(it.name))
        }.toMutableMap()

    private val _activeLesson = MutableStateFlow(LessonId.MathPictures)
    val activeLesson: StateFlow<LessonId> = _activeLesson.asStateFlow()

    private val _state: MutableStateFlow<MathState>
    val state: StateFlow<MathState>

    init {
        evaluatePassedFlags()
        val (correct, wrong) = Storage.loadMathCounts()
        _state = MutableStateFlow(
            MathState(
                problem = chooseProblem(previous = null, lesson = _activeLesson.value),
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
        manualOverride[id] = true
        Storage.saveLessonPassed(id, value)
        Storage.saveLessonManualOverride(id, true)
    }

    fun startLesson(id: LessonId) {
        require(id in SUPPORTED_LESSONS) { "MathViewModel does not run $id" }
        _activeLesson.value = id
        nextProblem()
    }

    fun onAnswer(choice: Int) {
        val current = _state.value
        if (current.feedback != MathFeedback.None) return
        val problem = current.problem
        val lesson = _activeLesson.value
        val correct = choice == problem.answer
        val grid = gridFor(problem.operator)
        if (correct) {
            grid[problem.left][problem.right]++
        } else {
            grid[problem.left][problem.right] = 0
        }
        saveCellStreak(problem.operator, problem.left, problem.right,
            grid[problem.left][problem.right])
        refreshGridSnapshot(problem.operator)

        // Per-lesson consecutive-correct streak.
        val lessonStreakFlow = lessonStreaks.getValue(lesson)
        lessonStreakFlow.value = if (correct) lessonStreakFlow.value + 1 else 0
        Storage.saveWinStreak(lesson.name, lessonStreakFlow.value)

        evaluatePassedFlags()

        _state.update {
            it.copy(
                feedback = if (correct) MathFeedback.Correct else MathFeedback.Wrong,
                selected = choice,
                correctCount = it.correctCount + if (correct) 1 else 0,
                wrongCount = it.wrongCount + if (correct) 0 else 1,
            )
        }
        Storage.saveMathCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun giveUp() {
        val current = _state.value
        if (current.feedback == MathFeedback.Correct ||
            current.feedback == MathFeedback.Revealed) return
        val problem = current.problem
        val lesson = _activeLesson.value
        val grid = gridFor(problem.operator)
        grid[problem.left][problem.right] = 0
        saveCellStreak(problem.operator, problem.left, problem.right, 0)
        refreshGridSnapshot(problem.operator)

        // Giving up resets the lesson's consecutive-correct streak too.
        val lessonStreakFlow = lessonStreaks.getValue(lesson)
        lessonStreakFlow.value = 0
        Storage.saveWinStreak(lesson.name, 0)

        _state.update {
            it.copy(
                feedback = MathFeedback.Revealed,
                selected = problem.answer,
                wrongCount = it.wrongCount + 1,
            )
        }
        Storage.saveMathCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun nextProblem() {
        _state.update {
            it.copy(
                problem = chooseProblem(previous = it.problem, lesson = _activeLesson.value),
                feedback = MathFeedback.None,
                selected = null,
            )
        }
    }

    /**
     * Passing requires BOTH cell coverage in the lesson's range AND the
     * per-lesson consecutive-correct streak reaching [LESSON_STREAK_TARGET].
     * Cell coverage alone is not enough — each variant has its own
     * streak that the learner has to build by answering in that
     * variant's screen.
     */
    private fun evaluatePassedFlags() {
        SUPPORTED_LESSONS.forEach { id ->
            if (manualOverride[id] == true) return@forEach
            val leftRange = lessonLeftRange(id)
            val rightRange = lessonRightRange(id)
            val grid = gridFor(lessonOperator(id))
            val cellsCovered = leftRange.all { a ->
                rightRange.all { b -> grid[a][b] >= 2 }
            }
            val streakHit = lessonStreaks.getValue(id).value >= LESSON_STREAK_TARGET
            val flow = passedFlow.getValue(id)
            if (cellsCovered && streakHit && !flow.value) {
                flow.value = true
                Storage.saveLessonPassed(id, true)
            }
        }
    }

    private fun gridFor(op: MathOperator): Array<IntArray> = when (op) {
        MathOperator.Plus -> additionStreaks
        MathOperator.Minus -> subtractionStreaks
        MathOperator.Times -> multiplicationStreaks
    }

    private fun saveCellStreak(op: MathOperator, a: Int, b: Int, value: Int) {
        when (op) {
            MathOperator.Plus -> Storage.saveMathStreak(a, b, value)
            MathOperator.Minus -> Storage.saveSubtractionStreak(a, b, value)
            MathOperator.Times -> Storage.saveMultiplicationGridStreak(a, b, value)
        }
    }

    private fun refreshGridSnapshot(op: MathOperator) {
        when (op) {
            MathOperator.Plus -> _additionGridFlow.value = snapshot(additionStreaks)
            MathOperator.Minus -> _subtractionGridFlow.value = snapshot(subtractionStreaks)
            MathOperator.Times -> _multiplicationGridFlow.value = snapshot(multiplicationStreaks)
        }
    }

    private fun snapshot(grid: Array<IntArray>): List<List<Int>> = grid.map { it.toList() }

    private fun chooseProblem(previous: MathProblem?, lesson: LessonId): MathProblem {
        val leftRange = lessonLeftRange(lesson)
        val rightRange = lessonRightRange(lesson)
        val operator = lessonOperator(lesson)
        val grid = gridFor(operator)
        val allCells = leftRange.flatMap { a -> rightRange.map { b -> a to b } }

        val forceFullyRandom = Random.nextDouble() < 0.10
        val basePool: List<Pair<Int, Int>> = if (forceFullyRandom) {
            allCells
        } else {
            val zeros = allCells.filter { (a, b) -> grid[a][b] == 0 }
            if (zeros.isNotEmpty()) {
                zeros
            } else {
                val minVal = allCells.minOf { (a, b) -> grid[a][b] }
                allCells.filter { (a, b) -> grid[a][b] == minVal }
            }
        }
        val pool = if (previous != null && basePool.size > 1) {
            basePool.filter { it.first != previous.left || it.second != previous.right }
                .ifEmpty { basePool }
        } else {
            basePool
        }
        val (left, right) = pool[Random.nextInt(pool.size)]
        return if (isPictureLesson(lesson)) {
            val animal = Animals.all[Random.nextInt(Animals.all.size)]
            MathProblem(
                left = left,
                right = right,
                operator = operator,
                leftAnimal = animal,
                rightAnimal = animal,
            )
        } else {
            MathProblem(left = left, right = right, operator = operator)
        }
    }
}
