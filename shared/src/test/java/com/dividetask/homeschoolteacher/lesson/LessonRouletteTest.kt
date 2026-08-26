package com.dividetask.homeschoolteacher.lesson

import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonRouletteTest {

    private val freshPassedMap: Map<LessonId, Boolean> =
        LessonId.entries.associateWith { false }

    private val expectedEntryLessons: Set<LessonId> = setOf(
        LessonId.TicTacToe0,
        // Four parallel Addition Level 0 variants, all unlocked from
        // the start (Counting / Vertical / Horizontal / Number Line).
        LessonId.MathPictures,
        LessonId.Math0,
        LessonId.HorizontalAddition0,
        LessonId.NumberLineAddition0,
        // Letter Sounds is the new head of the Reading chain; Phonemes now
        // sits behind it and is no longer entry-level.
        LessonId.LetterSounds0,
    )

    @Test
    fun `fresh state unlocks only the entry-level lessons`() {
        val unlocked = Lessons.unlocked(freshPassedMap).map { it.id }.toSet()
        assertEquals(
            "After clearing memory only no-parent lessons should be unlocked",
            expectedEntryLessons,
            unlocked,
        )
    }

    /**
     * The reported bug case: brand-new install (or wiped storage), then the
     * user runs Random / Mixed. Every entry-level lesson should actually
     * appear in rotation.
     */
    @Test
    fun `fresh state random rotation picks every entry-level lesson`() {
        val unlocked = Lessons.unlocked(freshPassedMap)
        val counts = mutableMapOf<LessonId, Int>()
        val rng = Random(seed = 42)
        val iterations = 6_000

        repeat(iterations) {
            val picked = LessonRoulette.choose(
                unlocked = unlocked,
                passed = freshPassedMap,
                excludeCategory = null,
                random = rng,
            )
            assertNotNull("LessonRoulette returned null", picked)
            counts.merge(picked!!.id, 1) { a, b -> a + b }
        }

        expectedEntryLessons.forEach { id ->
            val c = counts[id] ?: 0
            assertTrue(
                "Lesson $id was not picked in $iterations iterations — count was $c",
                c > 0,
            )
        }

        // Every entry-level lesson is unpassed (weight 2). Fair share is
        // 1 / N. Allow generous slack so the test isn't flaky.
        val expectedShare = 1.0 / expectedEntryLessons.size
        counts.forEach { (id, c) ->
            val share = c.toDouble() / iterations
            assertTrue(
                "Lesson $id share $share drifted far from fair $expectedShare " +
                    "(observed count $c / $iterations)",
                abs(share - expectedShare) < 0.05,
            )
        }
    }

    @Test
    fun `exclude category never returns a lesson of that category when alternatives exist`() {
        val unlocked = Lessons.unlocked(freshPassedMap)
        repeat(500) {
            val picked = LessonRoulette.choose(
                unlocked = unlocked,
                passed = freshPassedMap,
                excludeCategory = Category.Game,
                random = Random,
            )
            assertNotNull(picked)
            assertTrue(
                "Game lesson ${picked!!.id} leaked through Game filter",
                picked.category != Category.Game,
            )
        }
    }

    @Test
    fun `passed lessons take about one draw in ten`() {
        val unlocked = listOf(
            Lessons.get(LessonId.TicTacToe0),
            Lessons.get(LessonId.MathPictures),
            Lessons.get(LessonId.Phonemes0),
        )
        // TTT0 is the only unpassed; the other two are passed.
        val passed = mapOf(
            LessonId.TicTacToe0 to false,
            LessonId.MathPictures to true,
            LessonId.Phonemes0 to true,
        )
        val counts = mutableMapOf<LessonId, Int>()
        val rng = Random(seed = 7)
        val iterations = 20_000

        repeat(iterations) {
            val picked = LessonRoulette.choose(unlocked, passed, null, rng)!!
            counts.merge(picked.id, 1) { a, b -> a + b }
        }

        // 10% of draws go to the two passed lessons (5% each), the other
        // 90% to the single unpassed one.
        val tttShare = (counts[LessonId.TicTacToe0] ?: 0).toDouble() / iterations
        val picShare = (counts[LessonId.MathPictures] ?: 0).toDouble() / iterations
        val phoShare = (counts[LessonId.Phonemes0] ?: 0).toDouble() / iterations
        assertTrue("TTT share $tttShare not ~0.90", abs(tttShare - 0.90) < 0.02)
        assertTrue("Pictures share $picShare not ~0.05", abs(picShare - 0.05) < 0.02)
        assertTrue("Phonemes share $phoShare not ~0.05", abs(phoShare - 0.05) < 0.02)
    }

    @Test
    fun `every draw is a passed lesson when nothing is left unpassed`() {
        val unlocked = listOf(
            Lessons.get(LessonId.TicTacToe0),
            Lessons.get(LessonId.MathPictures),
        )
        val passed = mapOf(
            LessonId.TicTacToe0 to true,
            LessonId.MathPictures to true,
        )
        val rng = Random(seed = 5)
        repeat(500) {
            assertNotNull(LessonRoulette.choose(unlocked, passed, null, rng))
        }
    }

    @Test
    fun `returns null when the unlocked pool is empty`() {
        val picked = LessonRoulette.choose(
            unlocked = emptyList(),
            passed = freshPassedMap,
            excludeCategory = null,
            random = Random,
        )
        assertNull(picked)
    }

    @Test
    fun `excludeCategory falls back to full pool when filter would empty it`() {
        // Only one unlocked lesson and it is in the excluded category.
        // The selector should still return that lesson rather than crashing.
        val onlyOne = listOf(Lessons.get(LessonId.TicTacToe0))
        val picked = LessonRoulette.choose(
            unlocked = onlyOne,
            passed = freshPassedMap,
            excludeCategory = Category.Game,
            random = Random,
        )
        assertEquals(LessonId.TicTacToe0, picked?.id)
    }

    @Test
    fun `every lesson eventually appears once all parents are passed`() {
        // Simulate everything-unlocked: all lessons marked passed except
        // we leave them in the unlocked list as candidates. (When passed,
        // they get weight 1, but they still appear.)
        val passed = LessonId.entries.associateWith { true }
        val unlocked = Lessons.unlocked(passed)
        assertEquals(
            "With everything passed, every lesson should be unlocked",
            LessonId.entries.toSet(),
            unlocked.map { it.id }.toSet(),
        )
        val seen = mutableSetOf<LessonId>()
        val rng = Random(seed = 1)
        repeat(20_000) {
            seen.add(LessonRoulette.choose(unlocked, passed, null, rng)!!.id)
        }
        assertEquals(LessonId.entries.toSet(), seen)
    }
}
