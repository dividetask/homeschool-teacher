package com.dividetask.homeschoolteacher.intro

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dividetask.homeschoolteacher.division.divisionCells
import com.dividetask.homeschoolteacher.lesson.LessonId
import com.dividetask.homeschoolteacher.math.lessonRange
import kotlin.random.Random

/**
 * A worked example, played once at the start of a round.
 *
 * An intro is **not a lesson**: nothing is asked, nothing is scored, and
 * nothing about it is stored. It rolls its own problem, animates its way
 * through solving it with narration, and hands over to the lesson, whose
 * first question is already waiting behind it. A round of four questions
 * sees it once, not four times.
 *
 * Lessons without an intro written yet simply start as they always have,
 * so these can be added one at a time.
 */
object LessonIntros {

    /** Whether [id] opens with a worked example. */
    fun exists(id: LessonId): Boolean = when (id) {
        LessonId.MathPictures,
        LessonId.CountingAddition1,
        LessonId.CountingMultiplication0,
        LessonId.MultiplicationConstruction0,
        LessonId.CountingDivision0,
        LessonId.CountingDivision1 -> true
        else -> false
    }

    /**
     * The intro for [id]. Calls [onFinished] once, when the animation has
     * played out; the caller then shows the lesson.
     */
    @Composable
    fun Play(id: LessonId, onFinished: () -> Unit, modifier: Modifier = Modifier) {
        when (id) {
            LessonId.MathPictures,
            LessonId.CountingAddition1 -> CountingAdditionIntro(
                range = lessonRange(id),
                onFinished = onFinished,
                modifier = modifier,
            )
            LessonId.CountingMultiplication0 -> CountingMultiplicationIntro(
                range = lessonRange(id),
                onFinished = onFinished,
                modifier = modifier,
            )
            LessonId.MultiplicationConstruction0 -> MultiplicationConstructionIntro(
                range = lessonRange(id),
                onFinished = onFinished,
                modifier = modifier,
            )
            LessonId.CountingDivision0,
            LessonId.CountingDivision1 -> CountingDivisionIntro(
                cells = divisionCells(if (id == LessonId.CountingDivision1) 1 else 0),
                onFinished = onFinished,
                modifier = modifier,
            )
            else -> onFinished()
        }
    }
}

/**
 * Operands for a demonstration, drawn from [range] but **never 0 or 1**.
 *
 * A worked example has to have something to work: nothing merges into a
 * group of zero, `X - 0` crosses nothing out, and one of anything is over
 * before the counting starts. The questions that follow still ask those
 * cells — they are just a poor thing to open with.
 */
internal fun introOperand(range: IntRange, random: Random = Random): Int {
    val lowest = maxOf(2, range.first)
    if (lowest >= range.last) return maxOf(lowest, range.last)
    return random.nextInt(lowest, range.last + 1)
}
