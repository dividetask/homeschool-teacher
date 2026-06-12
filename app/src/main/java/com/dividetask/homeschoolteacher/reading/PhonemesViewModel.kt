package com.dividetask.homeschoolteacher.reading

import androidx.lifecycle.ViewModel
import com.dividetask.homeschoolteacher.Storage
import com.dividetask.homeschoolteacher.lesson.LessonId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

data class PhonemesProblem(
    val letter: Char,
    val words: List<String>,
)

enum class PhonemesFeedback { None, Correct, Wrong, Revealed }

data class PhonemesState(
    val problem: PhonemesProblem,
    val feedback: PhonemesFeedback = PhonemesFeedback.None,
    val selected: Char? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
)

class PhonemesViewModel : ViewModel() {

    private val streakMap: MutableMap<String, Int> = Phonemes.all
        .associateWith { Storage.loadWinStreak("Phonemes0.$it") }
        .toMutableMap()

    private val _streaks = MutableStateFlow(streakMap.toMap())
    val streaks: StateFlow<Map<String, Int>> = _streaks.asStateFlow()

    private val _passed = MutableStateFlow(Storage.loadLessonPassed(LessonId.Phonemes0))
    val passed: StateFlow<Boolean> = _passed.asStateFlow()

    private val _state: MutableStateFlow<PhonemesState>
    val state: StateFlow<PhonemesState>

    init {
        evaluatePassedFlag()
        val (correct, wrong) = Storage.loadPhonemesCounts()
        _state = MutableStateFlow(
            PhonemesState(
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
        Storage.saveLessonPassed(LessonId.Phonemes0, value)
        Storage.saveLessonManualOverride(LessonId.Phonemes0, true)
    }

    fun onAnswer(letter: Char) {
        val current = _state.value
        if (current.feedback != PhonemesFeedback.None) return
        val problem = current.problem
        val correct = letter == problem.letter.uppercaseChar()
        // A single answer applies to all three words at once.
        problem.words.forEach { word ->
            val newValue = if (correct) (streakMap[word] ?: 0) + 1 else 0
            streakMap[word] = newValue
            Storage.saveWinStreak("Phonemes0.$word", newValue)
        }
        _streaks.value = streakMap.toMap()
        evaluatePassedFlag()
        _state.update {
            it.copy(
                feedback = if (correct) PhonemesFeedback.Correct else PhonemesFeedback.Wrong,
                selected = letter,
                correctCount = it.correctCount + if (correct) 1 else 0,
                wrongCount = it.wrongCount + if (correct) 0 else 1,
            )
        }
        Storage.savePhonemesCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun giveUp() {
        val current = _state.value
        if (current.feedback == PhonemesFeedback.Correct ||
            current.feedback == PhonemesFeedback.Revealed) return
        // Same "reset all three" penalty as a wrong answer.
        current.problem.words.forEach { word ->
            streakMap[word] = 0
            Storage.saveWinStreak("Phonemes0.$word", 0)
        }
        _streaks.value = streakMap.toMap()
        _state.update {
            it.copy(
                feedback = PhonemesFeedback.Revealed,
                selected = it.problem.letter.uppercaseChar(),
                wrongCount = it.wrongCount + 1,
            )
        }
        Storage.savePhonemesCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun nextProblem() {
        _state.update {
            it.copy(
                problem = chooseProblem(previous = it.problem),
                feedback = PhonemesFeedback.None,
                selected = null,
            )
        }
    }

    private fun evaluatePassedFlag() {
        if (Storage.loadLessonManualOverride(LessonId.Phonemes0)) return
        val pool = Phonemes.all
        if (pool.isEmpty()) return
        val mastered = pool.all { (streakMap[it] ?: 0) >= 2 }
        if (mastered && !_passed.value) {
            _passed.value = true
            Storage.saveLessonPassed(LessonId.Phonemes0, true)
        }
    }

    private fun chooseProblem(previous: PhonemesProblem?): PhonemesProblem {
        val byLetter = Phonemes.byLetter
        if (byLetter.isEmpty()) {
            return PhonemesProblem('b', listOf("ball", "bed", "bat"))
        }

        val forceRandom = Random.nextDouble() < 0.10
        val basePool: List<Char> = if (forceRandom) {
            byLetter.keys.toList()
        } else {
            // Prioritise letters that still have words below streak 2.
            val needsWork = byLetter.filterValues { words ->
                words.any { (streakMap[it] ?: 0) < 2 }
            }.keys.toList()
            needsWork.ifEmpty { byLetter.keys.toList() }
        }

        val finalPool = if (previous != null && basePool.size > 1) {
            basePool.filter { it != previous.letter }.ifEmpty { basePool }
        } else {
            basePool
        }

        val pickedLetter = finalPool[Random.nextInt(finalPool.size)]
        val wordsForLetter = byLetter[pickedLetter] ?: emptyList()
        val pickedWords = if (wordsForLetter.size <= 3) {
            wordsForLetter
        } else {
            wordsForLetter.shuffled().take(3)
        }

        return PhonemesProblem(letter = pickedLetter, words = pickedWords)
    }
}
