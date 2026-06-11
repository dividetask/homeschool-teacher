package com.dividetask.homeschoolteacher.reading

import androidx.lifecycle.ViewModel
import com.dividetask.homeschoolteacher.Storage
import com.dividetask.homeschoolteacher.lesson.LessonId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

enum class LetterSoundsFeedback { None, Correct, Wrong, Revealed }

data class LetterSoundsProblem(
    val letter: Char,
    /** Raw resource id of the word clip played as the question. */
    val clipRes: Int,
    /** Raw resource id of the letter clip played back after answering. */
    val answerClipRes: Int,
)

data class LetterSoundsState(
    val problem: LetterSoundsProblem,
    val feedback: LetterSoundsFeedback = LetterSoundsFeedback.None,
    val selected: Char? = null,
    val runStreak: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
)

/**
 * Runner for "Letter Sounds — Level 0". Each problem plays a recorded word
 * clip and asks which letter the word starts with (A–Z keyboard).
 *
 * Passing takes two things at once (the user calls both out explicitly):
 *  - a run of [RUN_STREAK_TARGET] correct answers in a row, AND
 *  - every available letter answered correctly at least
 *    [LETTER_STREAK_TARGET] times in a row.
 *
 * With a single letter (A) available, the run streak is the binding
 * constraint; the per-letter rule starts to matter once more clips land.
 */
class LetterSoundsViewModel : ViewModel() {

    // Per-letter consecutive-correct streak, keyed by uppercase letter.
    private val streakMap: MutableMap<Char, Int> = LetterSounds.letters
        .associate { it.uppercaseChar() to Storage.loadLetterSoundStreak(it.uppercaseChar()) }
        .toMutableMap()

    private val _streaks = MutableStateFlow(streakMap.toMap())
    val streaks: StateFlow<Map<Char, Int>> = _streaks.asStateFlow()

    private val _passed = MutableStateFlow(Storage.loadLessonPassed(LessonId.LetterSounds0))
    val passed: StateFlow<Boolean> = _passed.asStateFlow()

    // Global consecutive-correct run across all letters.
    private var runStreak: Int = Storage.loadLetterSoundsRunStreak()

    private val _state: MutableStateFlow<LetterSoundsState>
    val state: StateFlow<LetterSoundsState>

    init {
        evaluatePassedFlag()
        val (correct, wrong) = Storage.loadLetterSoundsCounts()
        _state = MutableStateFlow(
            LetterSoundsState(
                problem = chooseProblem(previous = null),
                runStreak = runStreak,
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
        Storage.saveLessonPassed(LessonId.LetterSounds0, value)
        Storage.saveLessonManualOverride(LessonId.LetterSounds0, true)
    }

    fun onAnswer(letter: Char) {
        val current = _state.value
        if (current.feedback != LetterSoundsFeedback.None) return
        val key = current.problem.letter.uppercaseChar()
        val correct = letter.uppercaseChar() == key

        val newLetterStreak = if (correct) (streakMap[key] ?: 0) + 1 else 0
        streakMap[key] = newLetterStreak
        Storage.saveLetterSoundStreak(key, newLetterStreak)
        _streaks.value = streakMap.toMap()

        runStreak = if (correct) runStreak + 1 else 0
        Storage.saveLetterSoundsRunStreak(runStreak)

        evaluatePassedFlag()
        _state.update {
            it.copy(
                feedback = if (correct) LetterSoundsFeedback.Correct else LetterSoundsFeedback.Wrong,
                selected = letter,
                runStreak = runStreak,
                correctCount = it.correctCount + if (correct) 1 else 0,
                wrongCount = it.wrongCount + if (correct) 0 else 1,
            )
        }
        Storage.saveLetterSoundsCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun giveUp() {
        val current = _state.value
        if (current.feedback == LetterSoundsFeedback.Correct ||
            current.feedback == LetterSoundsFeedback.Revealed) return
        val key = current.problem.letter.uppercaseChar()
        streakMap[key] = 0
        Storage.saveLetterSoundStreak(key, 0)
        _streaks.value = streakMap.toMap()
        runStreak = 0
        Storage.saveLetterSoundsRunStreak(0)
        _state.update {
            it.copy(
                feedback = LetterSoundsFeedback.Revealed,
                selected = it.problem.letter.uppercaseChar(),
                runStreak = 0,
                wrongCount = it.wrongCount + 1,
            )
        }
        Storage.saveLetterSoundsCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun nextProblem() {
        _state.update {
            it.copy(
                problem = chooseProblem(previous = it.problem),
                feedback = LetterSoundsFeedback.None,
                selected = null,
            )
        }
    }

    private fun evaluatePassedFlag() {
        if (Storage.loadLessonManualOverride(LessonId.LetterSounds0)) return
        val letters = LetterSounds.letters
        if (letters.isEmpty()) return
        val everyLetterMastered =
            letters.all { (streakMap[it.uppercaseChar()] ?: 0) >= LETTER_STREAK_TARGET }
        val runMastered = runStreak >= RUN_STREAK_TARGET
        if (everyLetterMastered && runMastered && !_passed.value) {
            _passed.value = true
            Storage.saveLessonPassed(LessonId.LetterSounds0, true)
        }
    }

    private fun chooseProblem(previous: LetterSoundsProblem?): LetterSoundsProblem {
        val entries = LetterSounds.entries
        if (entries.isEmpty()) {
            // No clips bundled — degrade to a no-op problem rather than crash.
            return LetterSoundsProblem('A', clipRes = 0, answerClipRes = 0)
        }
        // Prioritise letters still below the per-letter target.
        val needsWork = entries.filter {
            (streakMap[it.letter.uppercaseChar()] ?: 0) < LETTER_STREAK_TARGET
        }
        val basePool = needsWork.ifEmpty { entries }
        val finalPool = if (previous != null && basePool.size > 1) {
            basePool.filter { it.letter != previous.letter }.ifEmpty { basePool }
        } else {
            basePool
        }
        val picked = finalPool[Random.nextInt(finalPool.size)]
        return LetterSoundsProblem(
            letter = picked.letter,
            clipRes = picked.wordClipRes,
            answerClipRes = picked.answerClipRes,
        )
    }

    companion object {
        /** Correct-in-a-row run required across all letters. */
        const val RUN_STREAK_TARGET = 8

        /** Correct-in-a-row required for each individual letter. */
        const val LETTER_STREAK_TARGET = 2
    }
}
