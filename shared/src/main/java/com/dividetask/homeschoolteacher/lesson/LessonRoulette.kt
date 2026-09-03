package com.dividetask.homeschoolteacher.lesson

import kotlin.random.Random

/**
 * Pure selection algorithm for [LessonSelector.rollRandomLesson]. Extracted
 * here so it can be exercised in unit tests without spinning up Android
 * ViewModels or SharedPreferences.
 */
object LessonRoulette {

    /**
     * Share of draws that go to a lesson the learner has already passed.
     * Revision keeps earlier material fresh, but the bulk of a session
     * should be spent on what is still being learned.
     */
    const val PASSED_LESSON_SHARE = 0.10

    /**
     * Pick the next lesson at random.
     *
     * One draw in ten (see [PASSED_LESSON_SHARE]) comes from the lessons
     * already passed; the rest come from the lessons still unpassed. When
     * only one of the two groups has any lessons in it, every draw comes
     * from that group. Within a group the pick is uniform.
     *
     * @param unlocked lessons currently eligible to run (no-parent lessons
     *   or lessons whose parent has been passed).
     * @param passed map of lesson -> whether it has been passed. Entries
     *   missing from the map are treated as unpassed.
     * @param excludeCategory if non-null, lessons in this category are
     *   filtered out unless that would leave the pool empty (in which case
     *   the filter is dropped).
     * @param random the random source.
     * @return the picked lesson, or null when [unlocked] itself is empty.
     */
    fun choose(
        unlocked: List<LessonDefinition>,
        passed: Map<LessonId, Boolean>,
        excludeCategory: Category?,
        random: Random = Random,
    ): LessonDefinition? {
        if (unlocked.isEmpty()) return null
        val pool = unlocked
            .filter { excludeCategory == null || it.category != excludeCategory }
            .ifEmpty { unlocked }
        val (done, todo) = pool.partition { passed[it.id] == true }
        val group = when {
            done.isEmpty() -> todo
            todo.isEmpty() -> done
            random.nextDouble() < PASSED_LESSON_SHARE -> done
            else -> todo
        }
        return group[random.nextInt(group.size)]
    }
}
