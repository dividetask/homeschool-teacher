package com.dividetask.homeschoolteacher.lesson

import kotlin.random.Random

/**
 * Pure selection algorithm for [LessonSelector.rollRandomLesson]. Extracted
 * here so it can be exercised in unit tests without spinning up Android
 * ViewModels or SharedPreferences.
 */
object LessonRoulette {

    /**
     * Pick the next lesson at random.
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
        val weighted = pool.map { def ->
            val w = if (passed[def.id] == true) 1 else 2
            def to w
        }
        val total = weighted.sumOf { it.second }
        var r = random.nextInt(total)
        for ((def, w) in weighted) {
            if (r < w) return def
            r -= w
        }
        return pool.last()
    }
}
