package com.dividetask.homeschoolteacher.reading

import androidx.lifecycle.ViewModel
import com.dividetask.homeschoolteacher.Storage
import com.dividetask.homeschoolteacher.lesson.LessonId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

enum class RhymingLevel {
    /** Level 0: pick the word that rhymes with the spoken target. */
    PickRhyme,
    /** Level 1: pick the word that does NOT rhyme with the others. */
    OddOneOut,
}

data class RhymingProblem(
    val level: RhymingLevel,
    /** Spoken target word (Level 0 only); null for Odd One Out. */
    val target: String?,
    /** Rhyme family this problem is built around (index into groups). */
    val familyIndex: Int,
    /** The word buttons shown, in display order. */
    val choices: List<String>,
    /** The correct choice (a rhyme for Level 0; the odd one for Level 1). */
    val correctWord: String,
    /** Words spoken when the problem appears. */
    val speakWords: List<String>,
)

enum class RhymingWordsFeedback { None, Correct, Wrong, Revealed }

data class RhymingWordsState(
    val problem: RhymingProblem,
    val feedback: RhymingWordsFeedback = RhymingWordsFeedback.None,
    val selected: String? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
)

private const val STREAK_TARGET = 2

/** A run of this many correct in a row passes the lesson outright. */
private const val RUN_TARGET = 8
private val SUPPORTED_LESSONS = setOf(LessonId.RhymingWords0, LessonId.RhymingWords1)

class RhymingWordsViewModel : ViewModel() {

    // Level 0 mastery is per target word; Level 1 is per rhyme family.
    private val wordStreaks: MutableMap<String, Int> = RhymingWords.all
        .associateWith { Storage.loadWinStreak("RhymingWords0.$it") }
        .toMutableMap()
    private val familyStreaks: MutableMap<Int, Int> = RhymingWords.groups.indices
        .associateWith { Storage.loadWinStreak("RhymingWords1.fam$it") }
        .toMutableMap()

    private val _wordStreaks = MutableStateFlow(wordStreaks.toMap())
    val streaks: StateFlow<Map<String, Int>> = _wordStreaks.asStateFlow()

    private val _familyStreaks = MutableStateFlow(familyStreaks.toMap())
    val familyStreaksFlow: StateFlow<Map<Int, Int>> = _familyStreaks.asStateFlow()

    private val passedFlow: MutableMap<LessonId, MutableStateFlow<Boolean>> =
        SUPPORTED_LESSONS.associateWith { MutableStateFlow(Storage.loadLessonPassed(it)) }
            .toMutableMap()

    // Consecutive-correct run per lesson; reaching RUN_TARGET passes it.
    private val runStreaks: MutableMap<LessonId, Int> =
        SUPPORTED_LESSONS.associateWith { Storage.loadWinStreak("run.${it.name}") }
            .toMutableMap()

    private val _activeLesson = MutableStateFlow(LessonId.RhymingWords0)
    val activeLesson: StateFlow<LessonId> = _activeLesson.asStateFlow()

    private val _state: MutableStateFlow<RhymingWordsState>
    val state: StateFlow<RhymingWordsState>

    init {
        evaluatePassedFlags()
        val (correct, wrong) = Storage.loadRhymingWordsCounts()
        _state = MutableStateFlow(
            RhymingWordsState(
                problem = chooseProblem(_activeLesson.value, previous = null),
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
        require(id in SUPPORTED_LESSONS) { "RhymingWordsViewModel does not run $id" }
        _activeLesson.value = id
        nextProblem()
    }

    fun onAnswer(word: String) {
        val current = _state.value
        if (current.feedback != RhymingWordsFeedback.None) return
        val problem = current.problem
        val correct = word == problem.correctWord
        recordOutcome(problem, correct)
        _state.update {
            it.copy(
                feedback = if (correct) RhymingWordsFeedback.Correct else RhymingWordsFeedback.Wrong,
                selected = word,
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
        recordOutcome(current.problem, correct = false)
        _state.update {
            it.copy(
                feedback = RhymingWordsFeedback.Revealed,
                selected = it.problem.correctWord,
                wrongCount = it.wrongCount + 1,
            )
        }
        Storage.saveRhymingWordsCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun nextProblem() {
        _state.update {
            it.copy(
                problem = chooseProblem(_activeLesson.value, previous = it.problem),
                feedback = RhymingWordsFeedback.None,
                selected = null,
            )
        }
    }

    /** Update the per-item streak for the answered problem. */
    private fun recordOutcome(problem: RhymingProblem, correct: Boolean) {
        when (problem.level) {
            RhymingLevel.PickRhyme -> {
                val t = problem.target ?: return
                val v = if (correct) (wordStreaks[t] ?: 0) + 1 else 0
                wordStreaks[t] = v
                Storage.saveWinStreak("RhymingWords0.$t", v)
                _wordStreaks.value = wordStreaks.toMap()
            }
            RhymingLevel.OddOneOut -> {
                val i = problem.familyIndex
                val v = if (correct) (familyStreaks[i] ?: 0) + 1 else 0
                familyStreaks[i] = v
                Storage.saveWinStreak("RhymingWords1.fam$i", v)
                _familyStreaks.value = familyStreaks.toMap()
            }
        }
        val lesson = _activeLesson.value
        runStreaks[lesson] = if (correct) (runStreaks[lesson] ?: 0) + 1 else 0
        Storage.saveWinStreak("run.${lesson.name}", runStreaks.getValue(lesson))
        evaluatePassedFlags()
    }

    private fun evaluatePassedFlags() {
        val groups = RhymingWords.groups
        if (groups.isEmpty()) return
        if (!Storage.loadLessonManualOverride(LessonId.RhymingWords0)) {
            val mastered = RhymingWords.all.all { (wordStreaks[it] ?: 0) >= STREAK_TARGET } ||
                (runStreaks[LessonId.RhymingWords0] ?: 0) >= RUN_TARGET
            val flag = passedFlow.getValue(LessonId.RhymingWords0)
            if (mastered && !flag.value) {
                flag.value = true
                Storage.saveLessonPassed(LessonId.RhymingWords0, true)
            }
        }
        if (!Storage.loadLessonManualOverride(LessonId.RhymingWords1)) {
            val mastered = groups.indices.all { (familyStreaks[it] ?: 0) >= STREAK_TARGET } ||
                (runStreaks[LessonId.RhymingWords1] ?: 0) >= RUN_TARGET
            val flag = passedFlow.getValue(LessonId.RhymingWords1)
            if (mastered && !flag.value) {
                flag.value = true
                Storage.saveLessonPassed(LessonId.RhymingWords1, true)
            }
        }
    }

    private fun chooseProblem(lesson: LessonId, previous: RhymingProblem?): RhymingProblem {
        val groups = RhymingWords.groups
        if (groups.isEmpty()) {
            return RhymingProblem(RhymingLevel.PickRhyme, "cat", 0, listOf("cat"), "cat", listOf("cat"))
        }
        return if (lesson == LessonId.RhymingWords1) {
            oddOneOutProblem(previous)
        } else {
            pickRhymeProblem(previous)
        }
    }

    /** Level 0: a spoken target plus one rhyme and two non-rhyming distractors. */
    private fun pickRhymeProblem(previous: RhymingProblem?): RhymingProblem {
        val groups = RhymingWords.groups
        val allWords = RhymingWords.all
        // Prefer target words still below the streak target.
        val pool = if (Random.nextDouble() < 0.10) {
            allWords
        } else {
            val needsWork = allWords.filter { (wordStreaks[it] ?: 0) < STREAK_TARGET }
            needsWork.ifEmpty { allWords }
        }
        val avoid = previous?.takeIf { it.level == RhymingLevel.PickRhyme }?.target
        val candidates = if (avoid != null && pool.size > 1) pool.filter { it != avoid } else pool
        val target = candidates[Random.nextInt(candidates.size)]
        val familyIndex = groups.indexOfFirst { target in it }
        val family = groups[familyIndex]
        val rhyme = family.filter { it != target }.random()
        val distractors = allWords.filter { it !in family }.shuffled().take(2)
        val choices = (listOf(rhyme) + distractors).shuffled()
        return RhymingProblem(
            level = RhymingLevel.PickRhyme,
            target = target,
            familyIndex = familyIndex,
            choices = choices,
            correctWord = rhyme,
            // Say the target, then each choice (highlighted as spoken).
            speakWords = listOf(target) + choices,
        )
    }

    /** Level 1: three words from one family plus one odd word from another. */
    private fun oddOneOutProblem(previous: RhymingProblem?): RhymingProblem {
        val groups = RhymingWords.groups
        val allWords = RhymingWords.all
        val pool: List<Int> = if (Random.nextDouble() < 0.10) {
            groups.indices.toList()
        } else {
            val needsWork = groups.indices.filter { (familyStreaks[it] ?: 0) < STREAK_TARGET }
            needsWork.ifEmpty { groups.indices.toList() }
        }
        val avoid = previous?.takeIf { it.level == RhymingLevel.OddOneOut }?.familyIndex
        val candidates = if (avoid != null && pool.size > 1) pool.filter { it != avoid } else pool
        val familyIndex = candidates[Random.nextInt(candidates.size)]
        val family = groups[familyIndex]
        val rhymeSet = family.shuffled().take(3)
        val odd = allWords.filter { it !in family }.shuffled().first()
        val choices = (rhymeSet + odd).shuffled()
        return RhymingProblem(
            level = RhymingLevel.OddOneOut,
            target = null,
            familyIndex = familyIndex,
            choices = choices,
            correctWord = odd,
            speakWords = choices,
        )
    }
}
