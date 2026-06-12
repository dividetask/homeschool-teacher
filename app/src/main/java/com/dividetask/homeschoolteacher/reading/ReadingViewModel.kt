package com.dividetask.homeschoolteacher.reading

import androidx.lifecycle.ViewModel
import com.dividetask.homeschoolteacher.Storage
import com.dividetask.homeschoolteacher.lesson.LessonId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

enum class ReadingFeedback { None, Correct, Wrong, Revealed }

data class ReadingState(
    val animal: Animal,
    val feedback: ReadingFeedback = ReadingFeedback.None,
    val selected: Char? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
)

class ReadingViewModel : ViewModel() {

    private val letterStreaks: MutableMap<Char, Int> = Animals.all
        .associate { it.letter to Storage.loadWinStreak("Reading0.${it.letter}") }
        .toMutableMap()

    private val _streaks = MutableStateFlow(letterStreaks.toMap())
    val streaks: StateFlow<Map<Char, Int>> = _streaks.asStateFlow()

    private val _passed = MutableStateFlow(Storage.loadLessonPassed(LessonId.Reading0))
    val passed: StateFlow<Boolean> = _passed.asStateFlow()

    private val _state: MutableStateFlow<ReadingState>
    val state: StateFlow<ReadingState>

    init {
        evaluatePassedFlag()
        val (correct, wrong) = Storage.loadReadingCounts()
        _state = MutableStateFlow(
            ReadingState(
                animal = chooseAnimal(previous = null),
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
        Storage.saveLessonPassed(LessonId.Reading0, value)
        Storage.saveLessonManualOverride(LessonId.Reading0, true)
    }

    fun onAnswer(letter: Char) {
        val current = _state.value
        if (current.feedback == ReadingFeedback.Correct ||
            current.feedback == ReadingFeedback.Revealed) return
        val animalLetter = current.animal.letter
        val correct = letter == animalLetter
        if (correct) {
            letterStreaks[animalLetter] = (letterStreaks[animalLetter] ?: 0) + 1
        } else {
            letterStreaks[animalLetter] = 0
        }
        Storage.saveWinStreak("Reading0.$animalLetter", letterStreaks[animalLetter] ?: 0)
        _streaks.value = letterStreaks.toMap()
        evaluatePassedFlag()

        _state.update {
            it.copy(
                feedback = if (correct) ReadingFeedback.Correct else ReadingFeedback.Wrong,
                selected = letter,
                correctCount = it.correctCount + if (correct) 1 else 0,
                wrongCount = it.wrongCount + if (correct) 0 else 1,
            )
        }
        Storage.saveReadingCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun giveUp() {
        val current = _state.value
        if (current.feedback == ReadingFeedback.Correct ||
            current.feedback == ReadingFeedback.Revealed) return
        val animalLetter = current.animal.letter
        letterStreaks[animalLetter] = 0
        Storage.saveWinStreak("Reading0.$animalLetter", 0)
        _streaks.value = letterStreaks.toMap()
        _state.update {
            it.copy(
                feedback = ReadingFeedback.Revealed,
                selected = animalLetter,
                wrongCount = it.wrongCount + 1,
            )
        }
        Storage.saveReadingCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun nextProblem() {
        _state.update {
            it.copy(
                animal = chooseAnimal(previous = it.animal),
                feedback = ReadingFeedback.None,
                selected = null,
            )
        }
    }

    private fun evaluatePassedFlag() {
        if (Storage.loadLessonManualOverride(LessonId.Reading0)) return
        val mastered = Animals.all.all { (letterStreaks[it.letter] ?: 0) >= 2 }
        if (mastered && !_passed.value) {
            _passed.value = true
            Storage.saveLessonPassed(LessonId.Reading0, true)
        }
    }

    private fun chooseAnimal(previous: Animal?): Animal {
        val pool = Animals.all
        val forceRandom = Random.nextDouble() < 0.10
        val basePool: List<Animal> = if (forceRandom) {
            pool
        } else {
            val zeros = pool.filter { (letterStreaks[it.letter] ?: 0) == 0 }
            if (zeros.isNotEmpty()) {
                zeros
            } else {
                val minVal = pool.minOf { letterStreaks[it.letter] ?: 0 }
                pool.filter { (letterStreaks[it.letter] ?: 0) == minVal }
            }
        }
        val candidate = if (previous != null && basePool.size > 1) {
            basePool.filter { it.letter != previous.letter }.ifEmpty { basePool }
        } else {
            basePool
        }
        return candidate[Random.nextInt(candidate.size)]
    }
}
