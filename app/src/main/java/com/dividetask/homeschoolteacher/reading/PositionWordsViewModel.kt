package com.dividetask.homeschoolteacher.reading

import androidx.lifecycle.ViewModel
import com.dividetask.homeschoolteacher.Storage
import com.dividetask.homeschoolteacher.lesson.LessonId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/** Which slot of "The __ is __ the __." is blank. */
enum class PositionBlank { Subject, Preposition, Object }

data class PositionScene(
    val animal: PositionWords.Item,
    val prep: String,
    val obj: PositionWords.Obj,
)

data class PositionProblem(
    val scene: PositionScene,
    val blank: PositionBlank,
    val choices: List<String>,
    val correctWord: String,
)

enum class PositionFeedback { None, Correct, Wrong, Revealed }

data class PositionState(
    val problem: PositionProblem,
    val feedback: PositionFeedback = PositionFeedback.None,
    val selected: String? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
)

private const val STREAK_TARGET = 2

/** A run of this many correct in a row passes the lesson outright. */
private const val RUN_TARGET = 8

private val SUPPORTED_LESSONS = setOf(
    LessonId.PositionWords0,
    LessonId.PositionWords1,
    LessonId.PositionWords2,
)

/**
 * "Position Words" — spatial prepositions. A scene shows a three-letter
 * animal positioned on / in / over / under a three-letter object, with the
 * sentence "The <animal> is <prep> the <object>." exactly one slot blank.
 * Level 0 blanks the animal, Level 1 the preposition, Level 2 the object.
 */
class PositionWordsViewModel : ViewModel() {

    private val animalStreaks: MutableMap<String, Int> = PositionWords.animalWords
        .associateWith { Storage.loadWinStreak("PositionWords0.$it") }.toMutableMap()
    private val prepStreaks: MutableMap<String, Int> = PositionWords.prepositions
        .associateWith { Storage.loadWinStreak("PositionWords1.$it") }.toMutableMap()
    private val objectStreaks: MutableMap<String, Int> = PositionWords.objectWords
        .associateWith { Storage.loadWinStreak("PositionWords2.$it") }.toMutableMap()

    private val _streaks = MutableStateFlow(snapshotStreaks())
    /** Combined streak snapshot for the Progress screen: item -> streak. */
    val streaks: StateFlow<Map<String, Int>> = _streaks.asStateFlow()

    private val passedFlow: MutableMap<LessonId, MutableStateFlow<Boolean>> =
        SUPPORTED_LESSONS.associateWith { MutableStateFlow(Storage.loadLessonPassed(it)) }
            .toMutableMap()

    // Consecutive-correct run per lesson; reaching RUN_TARGET passes it.
    private val runStreaks: MutableMap<LessonId, Int> =
        SUPPORTED_LESSONS.associateWith { Storage.loadWinStreak("run.${it.name}") }
            .toMutableMap()

    private val _activeLesson = MutableStateFlow(LessonId.PositionWords0)
    val activeLesson: StateFlow<LessonId> = _activeLesson.asStateFlow()

    private val _state: MutableStateFlow<PositionState>
    val state: StateFlow<PositionState>

    init {
        evaluatePassedFlags()
        val (correct, wrong) = Storage.loadPositionWordsCounts()
        _state = MutableStateFlow(
            PositionState(
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
        require(id in SUPPORTED_LESSONS) { "PositionWordsViewModel does not run $id" }
        _activeLesson.value = id
        nextProblem()
    }

    fun onAnswer(word: String) {
        val current = _state.value
        if (current.feedback != PositionFeedback.None) return
        val correct = word == current.problem.correctWord
        recordOutcome(current.problem, correct)
        _state.update {
            it.copy(
                feedback = if (correct) PositionFeedback.Correct else PositionFeedback.Wrong,
                selected = word,
                correctCount = it.correctCount + if (correct) 1 else 0,
                wrongCount = it.wrongCount + if (correct) 0 else 1,
            )
        }
        Storage.savePositionWordsCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun giveUp() {
        val current = _state.value
        if (current.feedback == PositionFeedback.Correct ||
            current.feedback == PositionFeedback.Revealed) return
        recordOutcome(current.problem, correct = false)
        _state.update {
            it.copy(
                feedback = PositionFeedback.Revealed,
                selected = it.problem.correctWord,
                wrongCount = it.wrongCount + 1,
            )
        }
        Storage.savePositionWordsCounts(_state.value.correctCount, _state.value.wrongCount)
    }

    fun nextProblem() {
        _state.update {
            it.copy(
                problem = chooseProblem(_activeLesson.value, previous = it.problem),
                feedback = PositionFeedback.None,
                selected = null,
            )
        }
    }

    private fun recordOutcome(problem: PositionProblem, correct: Boolean) {
        val (map, key, lessonKey) = when (problem.blank) {
            PositionBlank.Subject -> Triple(animalStreaks, problem.scene.animal.word, "PositionWords0")
            PositionBlank.Preposition -> Triple(prepStreaks, problem.scene.prep, "PositionWords1")
            PositionBlank.Object -> Triple(objectStreaks, problem.scene.obj.item.word, "PositionWords2")
        }
        val v = if (correct) (map[key] ?: 0) + 1 else 0
        map[key] = v
        Storage.saveWinStreak("$lessonKey.$key", v)
        _streaks.value = snapshotStreaks()
        val lesson = _activeLesson.value
        runStreaks[lesson] = if (correct) (runStreaks[lesson] ?: 0) + 1 else 0
        Storage.saveWinStreak("run.${lesson.name}", runStreaks.getValue(lesson))
        evaluatePassedFlags()
    }

    private fun evaluatePassedFlags() {
        maybePass(LessonId.PositionWords0, PositionWords.animalWords, animalStreaks)
        maybePass(LessonId.PositionWords1, PositionWords.prepositions, prepStreaks)
        maybePass(LessonId.PositionWords2, PositionWords.objectWords, objectStreaks)
    }

    private fun maybePass(id: LessonId, items: List<String>, streaks: Map<String, Int>) {
        if (items.isEmpty()) return
        if (Storage.loadLessonManualOverride(id)) return
        val mastered = items.all { (streaks[it] ?: 0) >= STREAK_TARGET } ||
            (runStreaks[id] ?: 0) >= RUN_TARGET
        val flag = passedFlow.getValue(id)
        if (mastered && !flag.value) {
            flag.value = true
            Storage.saveLessonPassed(id, true)
        }
    }

    private fun snapshotStreaks(): Map<String, Int> =
        animalStreaks + prepStreaks + objectStreaks

    private fun chooseProblem(lesson: LessonId, previous: PositionProblem?): PositionProblem {
        return when (lesson) {
            LessonId.PositionWords1 -> prepositionProblem(previous)
            LessonId.PositionWords2 -> objectProblem(previous)
            else -> subjectProblem(previous)
        }
    }

    /** Level 0: blank the animal. */
    private fun subjectProblem(previous: PositionProblem?): PositionProblem {
        val animalWord = pickByStreak(
            PositionWords.animalWords, animalStreaks,
            avoid = previous?.takeIf { it.blank == PositionBlank.Subject }?.scene?.animal?.word,
        )
        val animal = PositionWords.animal(animalWord)
        val obj = PositionWords.objects.random()
        val prep = obj.preps.random()
        val distractors = PositionWords.animalWords.filter { it != animalWord }.shuffled().take(2)
        val choices = (listOf(animalWord) + distractors).shuffled()
        return PositionProblem(PositionScene(animal, prep, obj), PositionBlank.Subject, choices, animalWord)
    }

    /** Level 1: blank the preposition. */
    private fun prepositionProblem(previous: PositionProblem?): PositionProblem {
        val prep = pickByStreak(
            PositionWords.prepositions, prepStreaks,
            avoid = previous?.takeIf { it.blank == PositionBlank.Preposition }?.scene?.prep,
        )
        val obj = PositionWords.objects.filter { prep in it.preps }.random()
        val animal = PositionWords.animals.random()
        return PositionProblem(
            PositionScene(animal, prep, obj),
            PositionBlank.Preposition,
            PositionWords.prepositions,
            prep,
        )
    }

    /** Level 2: blank the object. */
    private fun objectProblem(previous: PositionProblem?): PositionProblem {
        val objectWord = pickByStreak(
            PositionWords.objectWords, objectStreaks,
            avoid = previous?.takeIf { it.blank == PositionBlank.Object }?.scene?.obj?.item?.word,
        )
        val obj = PositionWords.obj(objectWord)
        val prep = obj.preps.random()
        val animal = PositionWords.animals.random()
        val distractors = PositionWords.objectWords.filter { it != objectWord }.shuffled().take(2)
        val choices = (listOf(objectWord) + distractors).shuffled()
        return PositionProblem(PositionScene(animal, prep, obj), PositionBlank.Object, choices, objectWord)
    }

    /** Prefer items still below the streak target; 10% fully random. */
    private fun pickByStreak(items: List<String>, streaks: Map<String, Int>, avoid: String?): String {
        val pool = if (Random.nextDouble() < 0.10) {
            items
        } else {
            items.filter { (streaks[it] ?: 0) < STREAK_TARGET }.ifEmpty { items }
        }
        val candidates = if (avoid != null && pool.size > 1) pool.filter { it != avoid } else pool
        return candidates[Random.nextInt(candidates.size)]
    }
}
