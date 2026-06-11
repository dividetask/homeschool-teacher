package com.dividetask.homeschoolteacher.reading

import androidx.lifecycle.ViewModel
import com.dividetask.homeschoolteacher.Storage
import com.dividetask.homeschoolteacher.lesson.LessonId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

data class RhymingWordProblem(val word: String) {
    val missingLetter: Char get() = word[0].uppercaseChar()
}

enum class RhymingWordsFeedback { None, Correct, Wrong, Revealed }

data class RhymingWordsState(
    val problem: RhymingWordProblem,
    val feedback: RhymingWordsFeedback = RhymingWordsFeedback.None,
    val selected: Char? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
)

class RhymingWordsViewModel : ViewModel() {

    private val streakMap: MutableMap<String, Int> = RhymingWords.all
        .associateWith { Storage.loadRhymingWordStreak(it) }
        .toMutableMap()

    private val _streaks = MutableStateFlow(streakMap.toMap())
    val streaks: StateFlow<Map<String, Int>> = _streaks.asStateFlow()

    private val _passed = MutableStateFlow(Storage.loadLessonPassed(LessonId.RhymingWords0))
    val passed: StateFlow<Boolean> = _passed.asStateFlow()

    private val _state: MutableStateFlow<RhymingWordsState>
    val state: StateFlow<RhymingWordsState>

    init {
        evaluatePassedFlag()
        val (correct, wrong) = Storage.loadRhymingWordsCounts()
        _state = MutableStateFlow(
            RhymingWordsState(
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
        Storage.saveLessonPassed(LessonId.RhymingWords0, value)
        Storage.saveLessonManualOverride(LessonId.RhymingWords0, true)
    }

    fun onAnswer(letter: Char) {
        val current = _state.value
        if (current.feedback != RhymingWordsFeedback.None) return
        val word = current.problem.word
        val correct = letter == current.problem.missingLetter
        if (correct) {
            streakMap[word] = (streakMap[word] ?: 0) + 1
        } else {
            streakMap[word] = 0
        }
        Storage.saveRhymingWordStreak(word, streakMap[word] ?: 0)
        _streaks.value = streakMap.toMap()
        evaluatePassedFlag()
        _state.update {
            it.copy(
                feedback = if (correct) RhymingWordsFeedback.Correct else RhymingWordsFeedback.Wrong,
                selected = letter,
                correctCount = it.correctCount + if (correct) 1 else 0,
                wrongCount = it.wrongCount + if (correct) 0 else 1,
            )
        }
        Storage.saveRhymingWordsCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun giveUp() {
        val current = _state.value
        if (current.feedback == RhymingWordsFeedback.Correct ||
            current.feedback == RhymingWordsFeedback.Revealed) return
        val word = current.problem.word
        streakMap[word] = 0
        Storage.saveRhymingWordStreak(word, 0)
        _streaks.value = streakMap.toMap()
        _state.update {
            it.copy(
                feedback = RhymingWordsFeedback.Revealed,
                selected = current.problem.missingLetter,
                wrongCount = it.wrongCount + 1,
            )
        }
        Storage.saveRhymingWordsCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun nextProblem() {
        _state.update {
            it.copy(
                problem = chooseProblem(previous = it.problem.word),
                feedback = RhymingWordsFeedback.None,
                selected = null,
            )
        }
    }

    private fun evaluatePassedFlag() {
        if (Storage.loadLessonManualOverride(LessonId.RhymingWords0)) return
        val pool = RhymingWords.all
        if (pool.isEmpty()) return
        val mastered = pool.all { (streakMap[it] ?: 0) >= 2 }
        if (mastered && !_passed.value) {
            _passed.value = true
            Storage.saveLessonPassed(LessonId.RhymingWords0, true)
        }
    }

    private fun chooseProblem(previous: String?): RhymingWordProblem {
        val pool = RhymingWords.all
        if (pool.isEmpty()) return RhymingWordProblem("cat")
        val forceRandom = Random.nextDouble() < 0.10
        val basePool: List<String> = if (forceRandom) {
            pool
        } else {
            val zeros = pool.filter { (streakMap[it] ?: 0) == 0 }
            if (zeros.isNotEmpty()) {
                zeros
            } else {
                val minVal = pool.minOf { streakMap[it] ?: 0 }
                pool.filter { (streakMap[it] ?: 0) == minVal }
            }
        }
        val candidate = if (previous != null && basePool.size > 1) {
            basePool.filter { it != previous }.ifEmpty { basePool }
        } else {
            basePool
        }
        return RhymingWordProblem(candidate[Random.nextInt(candidate.size)])
    }
}
