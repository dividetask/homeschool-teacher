package com.dividetask.homeschoolteacher.binary

import androidx.lifecycle.ViewModel
import com.dividetask.homeschoolteacher.Storage
import com.dividetask.homeschoolteacher.lesson.LessonId
import com.dividetask.homeschoolteacher.practice.PracticeGrid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class BinaryOperator(val symbol: String, val verbalName: String) {
    AND("&", "AND"),
    OR("|", "OR"),
    XOR("^", "XOR");

    fun apply(a: Int, b: Int): Int = when (this) {
        AND -> a and b
        OR -> a or b
        XOR -> a xor b
    }
}

data class BinaryProblem(
    val op1: Int,
    val op2: Int,
    val operator: BinaryOperator,
    val bits: Int,
) {
    val answer: Int get() = operator.apply(op1, op2)

    fun toBinary(value: Int): String = value.toString(2).padStart(bits, '0')

    val op1Binary: String get() = toBinary(op1)
    val op2Binary: String get() = toBinary(op2)
    val answerBinary: String get() = toBinary(answer)
}

enum class BinaryFeedback { None, Correct, Wrong, Revealed }

data class BinaryState(
    val problem: BinaryProblem,
    val feedback: BinaryFeedback = BinaryFeedback.None,
    val answerInput: String = "",
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
)

private val SUPPORTED_LESSONS = setOf(LessonId.BinaryOps0, LessonId.BinaryOps1)

/** A run of this many correct in a row passes the lesson outright. */
private const val RUN_TARGET = 8

class BinaryOperationsViewModel : ViewModel() {

    // streaks[level][operatorOrdinal][op1][op2]
    // Level 0 only ever touches indices 0..1; Level 1 touches 0..7.
    private val streaks: Array<Array<Array<IntArray>>> = Array(2) { level ->
        Array(BinaryOperator.entries.size) { op ->
            Array(8) { op1 ->
                IntArray(8) { op2 -> Storage.loadBinaryStreak(level, op, op1, op2) }
            }
        }
    }

    private val _streaksSnapshot = MutableStateFlow(snapshotStreaks())
    val streaksSnapshot: StateFlow<List<List<List<List<Int>>>>> = _streaksSnapshot.asStateFlow()

    private val passedFlow: MutableMap<LessonId, MutableStateFlow<Boolean>> =
        SUPPORTED_LESSONS.associateWith {
            MutableStateFlow(Storage.loadLessonPassed(it))
        }.toMutableMap()

    // Consecutive-correct run per lesson; reaching RUN_TARGET passes it.
    private val runStreaks: MutableMap<LessonId, Int> =
        SUPPORTED_LESSONS.associateWith { Storage.loadWinStreak("run.${it.name}") }
            .toMutableMap()

    private val _activeLesson = MutableStateFlow(LessonId.BinaryOps0)
    val activeLesson: StateFlow<LessonId> = _activeLesson.asStateFlow()

    private val _state: MutableStateFlow<BinaryState>
    val state: StateFlow<BinaryState>

    init {
        evaluatePassedFlags()
        val (correct, wrong) = Storage.loadBinaryCounts()
        _state = MutableStateFlow(
            BinaryState(
                problem = chooseProblem(previous = null, lesson = _activeLesson.value),
                correctCount = correct,
                wrongCount = wrong,
            ),
        )
        state = _state.asStateFlow()
    }

    fun passed(id: LessonId): StateFlow<Boolean> = passedFlow.getValue(id).asStateFlow()

    fun setPassed(id: LessonId, value: Boolean) {
        if (id !in SUPPORTED_LESSONS) return
        passedFlow.getValue(id).value = value
        Storage.saveLessonPassed(id, value)
        Storage.saveLessonManualOverride(id, value)
    }

    fun startLesson(id: LessonId) {
        require(id in SUPPORTED_LESSONS) { "BinaryOperationsViewModel does not run $id" }
        _activeLesson.value = id
        nextProblem()
    }

    /** Tap a binary digit (0 or 1). Appends to input; auto-checks at full length. */
    fun onDigit(digit: Int) {
        val current = _state.value
        if (current.feedback != BinaryFeedback.None) return
        if (digit != 0 && digit != 1) return
        val bits = current.problem.bits
        if (current.answerInput.length >= bits) return
        val next = current.answerInput + digit.toString()
        if (next.length == bits) {
            submit(next)
        } else {
            _state.update { it.copy(answerInput = next) }
        }
    }

    /** Remove the right-most digit from the current input. */
    fun onBack() {
        val current = _state.value
        if (current.feedback != BinaryFeedback.None) return
        if (current.answerInput.isEmpty()) return
        _state.update { it.copy(answerInput = it.answerInput.dropLast(1)) }
    }

    fun giveUp() {
        val current = _state.value
        if (current.feedback == BinaryFeedback.Correct ||
            current.feedback == BinaryFeedback.Revealed) return
        val problem = current.problem
        val level = levelOf(_activeLesson.value)
        streaks[level][problem.operator.ordinal][problem.op1][problem.op2] = 0
        Storage.saveBinaryStreak(level, problem.operator.ordinal, problem.op1, problem.op2, 0)
        _streaksSnapshot.value = snapshotStreaks()
        val lesson = _activeLesson.value
        runStreaks[lesson] = 0
        Storage.saveWinStreak("run.${lesson.name}", 0)
        _state.update {
            it.copy(
                feedback = BinaryFeedback.Revealed,
                answerInput = problem.answerBinary,
                wrongCount = it.wrongCount + 1,
            )
        }
        Storage.saveBinaryCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun nextProblem() {
        _state.update {
            it.copy(
                problem = chooseProblem(previous = it.problem, lesson = _activeLesson.value),
                feedback = BinaryFeedback.None,
                answerInput = "",
            )
        }
    }

    private fun submit(input: String) {
        val current = _state.value
        val problem = current.problem
        val correct = input == problem.answerBinary
        val level = levelOf(_activeLesson.value)
        val opIdx = problem.operator.ordinal
        val cell = streaks[level][opIdx][problem.op1][problem.op2]
        val newCell = if (correct) cell + 1 else 0
        streaks[level][opIdx][problem.op1][problem.op2] = newCell
        Storage.saveBinaryStreak(level, opIdx, problem.op1, problem.op2, newCell)
        _streaksSnapshot.value = snapshotStreaks()
        val lesson = _activeLesson.value
        runStreaks[lesson] = if (correct) (runStreaks[lesson] ?: 0) + 1 else 0
        Storage.saveWinStreak("run.${lesson.name}", runStreaks.getValue(lesson))
        evaluatePassedFlags()
        _state.update {
            it.copy(
                feedback = if (correct) BinaryFeedback.Correct else BinaryFeedback.Wrong,
                answerInput = input,
                correctCount = it.correctCount + if (correct) 1 else 0,
                wrongCount = it.wrongCount + if (correct) 0 else 1,
            )
        }
        Storage.saveBinaryCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    private fun evaluatePassedFlags() {
        SUPPORTED_LESSONS.forEach { id ->
            if (Storage.loadLessonManualOverride(id)) return@forEach
            val level = levelOf(id)
            val mastered = PracticeGrid.covered(
                cells = cellsFor(level),
                value = { (op, op1, op2) -> streaks[level][op][op1][op2] },
            )
            val flow = passedFlow.getValue(id)
            if ((mastered || (runStreaks[id] ?: 0) >= RUN_TARGET) && !flow.value) {
                flow.value = true
                Storage.saveLessonPassed(id, true)
            }
        }
    }

    private fun snapshotStreaks(): List<List<List<List<Int>>>> = streaks.map { perLevel ->
        perLevel.map { perOp ->
            perOp.map { row -> row.toList() }
        }
    }

    private fun levelOf(id: LessonId): Int = when (id) {
        LessonId.BinaryOps0 -> 0
        LessonId.BinaryOps1 -> 1
        else -> 0
    }

    /**
     * Every cell a level can ask, as `(operatorOrdinal, op1, op2)`. Level 0
     * is single-bit operands, Level 1 three-bit.
     */
    private fun cellsFor(level: Int): List<Triple<Int, Int, Int>> {
        val maxOperand = if (level == 0) 1 else 7
        return BinaryOperator.entries.indices.flatMap { op ->
            (0..maxOperand).flatMap { op1 ->
                (0..maxOperand).map { op2 -> Triple(op, op1, op2) }
            }
        }
    }

    private fun chooseProblem(previous: BinaryProblem?, lesson: LessonId): BinaryProblem {
        val level = levelOf(lesson)
        val bits = if (level == 0) 1 else 3

        // Same selection every other grid-backed lesson uses. Binary has no
        // easy cells — every truth-table row is worth the same drilling — so
        // the default target and weight apply.
        val (opIdx, op1, op2) = PracticeGrid.choose(
            cells = cellsFor(level),
            value = { (op, a, b) -> streaks[level][op][a][b] },
            previous = previous?.let {
                Triple(it.operator.ordinal, it.op1, it.op2)
            },
        )
        return BinaryProblem(
            op1 = op1,
            op2 = op2,
            operator = BinaryOperator.entries[opIdx],
            bits = bits,
        )
    }
}
