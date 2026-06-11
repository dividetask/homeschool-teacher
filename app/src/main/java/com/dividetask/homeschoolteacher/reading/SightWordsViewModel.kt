package com.dividetask.homeschoolteacher.reading

import androidx.lifecycle.ViewModel
import com.dividetask.homeschoolteacher.Storage
import com.dividetask.homeschoolteacher.lesson.LessonId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

data class SightWordProblem(
    val word: String,
    val missingIndex: Int,
) {
    val missingLetter: Char get() = word[missingIndex].uppercaseChar()
}

enum class SightWordsFeedback { None, Correct, Wrong, Revealed }

data class SightWordsState(
    val problem: SightWordProblem,
    val feedback: SightWordsFeedback = SightWordsFeedback.None,
    val selected: Char? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
)

private val SUPPORTED_LESSONS = setOf(LessonId.SightWords0, LessonId.SightWords1)

class SightWordsViewModel : ViewModel() {

    // Per-word, per-letter streak. The IntArray length equals the word's
    // length. Level 0 (first letter) only ever touches index 0; Level 1
    // (random letter) touches any index. Both lessons share this data so
    // mastering the first letter of "cat" counts toward Level 1 as well.
    private val streakMap: MutableMap<String, IntArray> = SightWords.all
        .associateWith { word ->
            IntArray(word.length) { pos -> Storage.loadSightWordStreak(word, pos) }
        }.toMutableMap()

    private val _streaks = MutableStateFlow(streakSnapshot())
    val streaks: StateFlow<Map<String, List<Int>>> = _streaks.asStateFlow()

    private val passedFlow: MutableMap<LessonId, MutableStateFlow<Boolean>> =
        SUPPORTED_LESSONS.associateWith {
            MutableStateFlow(Storage.loadLessonPassed(it))
        }.toMutableMap()

    private val _activeLesson = MutableStateFlow(LessonId.SightWords0)
    val activeLesson: StateFlow<LessonId> = _activeLesson.asStateFlow()

    private val _state: MutableStateFlow<SightWordsState>
    val state: StateFlow<SightWordsState>

    init {
        evaluatePassedFlags()
        val (correct, wrong) = Storage.loadSightWordsCounts()
        _state = MutableStateFlow(
            SightWordsState(
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
        Storage.saveLessonManualOverride(id, true)
    }

    fun startLesson(id: LessonId) {
        require(id in SUPPORTED_LESSONS) { "SightWordsViewModel does not run $id" }
        _activeLesson.value = id
        nextProblem()
    }

    fun onAnswer(letter: Char) {
        val current = _state.value
        if (current.feedback != SightWordsFeedback.None) return
        val word = current.problem.word
        val position = current.problem.missingIndex
        val correct = letter == current.problem.missingLetter
        val arr = streakMap.getValue(word)
        if (correct) {
            arr[position] = arr[position] + 1
        } else {
            arr[position] = 0
        }
        Storage.saveSightWordStreak(word, position, arr[position])
        _streaks.value = streakSnapshot()
        evaluatePassedFlags()
        _state.update {
            it.copy(
                feedback = if (correct) SightWordsFeedback.Correct else SightWordsFeedback.Wrong,
                selected = letter,
                correctCount = it.correctCount + if (correct) 1 else 0,
                wrongCount = it.wrongCount + if (correct) 0 else 1,
            )
        }
        Storage.saveSightWordsCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun giveUp() {
        val current = _state.value
        if (current.feedback == SightWordsFeedback.Correct ||
            current.feedback == SightWordsFeedback.Revealed) return
        val word = current.problem.word
        val position = current.problem.missingIndex
        val arr = streakMap.getValue(word)
        arr[position] = 0
        Storage.saveSightWordStreak(word, position, 0)
        _streaks.value = streakSnapshot()
        _state.update {
            it.copy(
                feedback = SightWordsFeedback.Revealed,
                selected = current.problem.missingLetter,
                wrongCount = it.wrongCount + 1,
            )
        }
        Storage.saveSightWordsCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun nextProblem() {
        _state.update {
            it.copy(
                problem = chooseProblem(previous = it.problem, lesson = _activeLesson.value),
                feedback = SightWordsFeedback.None,
                selected = null,
            )
        }
    }

    private fun evaluatePassedFlags() {
        val pool = SightWords.all
        if (pool.isEmpty()) return
        // Level 0 = every word's position-0 streak >= 2.
        if (!Storage.loadLessonManualOverride(LessonId.SightWords0)) {
            val passed0 = pool.all { (streakMap[it]?.getOrNull(0) ?: 0) >= 2 }
            val flag0 = passedFlow.getValue(LessonId.SightWords0)
            if (passed0 && !flag0.value) {
                flag0.value = true
                Storage.saveLessonPassed(LessonId.SightWords0, true)
            }
        }
        // Level 1 = every (word, position) streak >= 2.
        if (!Storage.loadLessonManualOverride(LessonId.SightWords1)) {
            val passed1 = pool.all { word ->
                val arr = streakMap[word] ?: return@all false
                arr.all { it >= 2 }
            }
            val flag1 = passedFlow.getValue(LessonId.SightWords1)
            if (passed1 && !flag1.value) {
                flag1.value = true
                Storage.saveLessonPassed(LessonId.SightWords1, true)
            }
        }
    }

    private fun streakSnapshot(): Map<String, List<Int>> =
        streakMap.mapValues { it.value.toList() }

    private fun chooseProblem(previous: SightWordProblem?, lesson: LessonId): SightWordProblem {
        // Build the candidate (word, position) pool for this lesson.
        val pool: List<Pair<String, Int>> = when (lesson) {
            LessonId.SightWords0 -> SightWords.all.map { it to 0 }
            LessonId.SightWords1 -> SightWords.all.flatMap { word ->
                (0 until word.length).map { word to it }
            }
            else -> SightWords.all.map { it to 0 }
        }
        if (pool.isEmpty()) {
            return SightWordProblem("a", 0)
        }
        val forceRandom = Random.nextDouble() < 0.10
        val basePool: List<Pair<String, Int>> = if (forceRandom) {
            pool
        } else {
            val zeros = pool.filter { (w, p) -> (streakMap[w]?.getOrNull(p) ?: 0) == 0 }
            if (zeros.isNotEmpty()) {
                zeros
            } else {
                val minVal = pool.minOf { (w, p) -> streakMap[w]?.getOrNull(p) ?: 0 }
                pool.filter { (w, p) -> (streakMap[w]?.getOrNull(p) ?: 0) == minVal }
            }
        }
        val finalPool = if (previous != null && basePool.size > 1) {
            basePool.filter { (w, p) ->
                w != previous.word || p != previous.missingIndex
            }.ifEmpty { basePool }
        } else {
            basePool
        }
        val (word, position) = finalPool[Random.nextInt(finalPool.size)]
        return SightWordProblem(word, position)
    }
}
