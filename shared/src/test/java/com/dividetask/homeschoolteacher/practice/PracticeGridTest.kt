package com.dividetask.homeschoolteacher.practice

import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeGridTest {

    /** Operands 0..4 either side, as the Level 0 math lessons use. */
    private val cells: List<Pair<Int, Int>> =
        (0..4).flatMap { a -> (0..4).map { b -> a to b } }

    /** Of those 25 cells, 16 are easy for multiplication and 9 are not. */
    private val easyCount = cells.count { (a, b) -> PracticeGrid.isEasy(GridOperation.Multiply, a, b) }

    @Test
    fun `a zero operand is easy for addition and subtraction`() {
        assertTrue(PracticeGrid.isEasy(GridOperation.Add, 0, 7))
        assertTrue(PracticeGrid.isEasy(GridOperation.Add, 7, 0))
        assertTrue(PracticeGrid.isEasy(GridOperation.Subtract, 7, 0))
        assertFalse(PracticeGrid.isEasy(GridOperation.Add, 1, 7))
        assertFalse(PracticeGrid.isEasy(GridOperation.Subtract, 7, 1))
    }

    @Test
    fun `zero and one are easy for multiplication, one is easy for division`() {
        assertTrue(PracticeGrid.isEasy(GridOperation.Multiply, 0, 8))
        assertTrue(PracticeGrid.isEasy(GridOperation.Multiply, 8, 1))
        assertFalse(PracticeGrid.isEasy(GridOperation.Multiply, 2, 3))
        // (dividend, divisor)
        assertTrue(PracticeGrid.isEasy(GridOperation.Divide, 12, 1))
        assertFalse(PracticeGrid.isEasy(GridOperation.Divide, 12, 3))
    }

    @Test
    fun `easy cells need one correct answer, the rest need two`() {
        assertEquals(1, PracticeGrid.target(GridOperation.Add, 0, 4))
        assertEquals(2, PracticeGrid.target(GridOperation.Add, 3, 4))
        assertEquals(1, PracticeGrid.target(GridOperation.Multiply, 1, 4))
        assertEquals(2, PracticeGrid.target(GridOperation.Multiply, 2, 4))
    }

    @Test
    fun `coverage accepts an easy cell answered right once`() {
        val values = cells.associateWith { (a, b) ->
            if (a == 0 || b == 0) 1 else 2
        }.toMutableMap()
        assertTrue(
            PracticeGrid.covered(cells, GridOperation.Add) { a, b -> values.getValue(a to b) },
        )
        // An ordinary cell one short is still not covered.
        values[2 to 3] = 1
        assertFalse(
            PracticeGrid.covered(cells, GridOperation.Add) { a, b -> values.getValue(a to b) },
        )
    }

    @Test
    fun `untouched ordinary cells are drilled before untouched easy ones`() {
        // Nothing answered yet, so ordinary cells are two behind their
        // target and easy cells only one: easy cells should only surface
        // through the one-in-ten wildcard roll.
        val random = Random(seed = 7)
        val iterations = 20_000
        var easy = 0
        repeat(iterations) {
            val (a, b) = PracticeGrid.choose(
                cells = cells,
                operation = GridOperation.Multiply,
                value = { _, _ -> 0 },
                previous = null,
                random = random,
            )
            if (PracticeGrid.isEasy(GridOperation.Multiply, a, b)) easy++
        }
        val easyShare = easy.toDouble() / iterations
        assertTrue("easy cells took $easyShare of the draws", easyShare < 0.10)
    }

    @Test
    fun `once covered, easy cells come up at half weight where the cap allows`() {
        // Addition 0..8: 17 of the 81 cells are easy, so half weight puts
        // them well under the cap and the halving is what decides.
        val wide = (0..8).flatMap { a -> (0..8).map { b -> a to b } }
        val easyCount = wide.count { (a, b) -> PracticeGrid.isEasy(GridOperation.Add, a, b) }
        val ordinaryCount = wide.size - easyCount
        val easyWeight = easyCount * PracticeGrid.EASY_CELL_WEIGHT
        val expected = easyWeight / (easyWeight + ordinaryCount)
        assertTrue("this grid should sit under the cap", expected < PracticeGrid.EASY_SHARE_CAP)

        val random = Random(seed = 11)
        val iterations = 20_000
        var easy = 0
        repeat(iterations) {
            val (a, b) = PracticeGrid.choose(
                cells = wide,
                operation = GridOperation.Add,
                value = { _, _ -> 5 },
                previous = null,
                random = random,
            )
            if (PracticeGrid.isEasy(GridOperation.Add, a, b)) easy++
        }
        val easyShare = easy.toDouble() / iterations
        assertTrue(
            "easy share was $easyShare, expected about $expected",
            abs(easyShare - expected) < 0.02,
        )
    }

    @Test
    fun `the cap holds down a grid that is mostly easy cells`() {
        // Multiplication 0..4 is the case the cap exists for: 16 easy
        // cells against 9 ordinary ones, so half weight alone would still
        // leave nearly half the round on times zero and times one.
        val easyWeight = easyCount * PracticeGrid.EASY_CELL_WEIGHT
        val uncapped = easyWeight / (easyWeight + (cells.size - easyCount))
        assertTrue("this grid should exceed the cap", uncapped > PracticeGrid.EASY_SHARE_CAP)

        val random = Random(seed = 11)
        val iterations = 20_000
        var easy = 0
        repeat(iterations) {
            val (a, b) = PracticeGrid.choose(
                cells = cells,
                operation = GridOperation.Multiply,
                value = { _, _ -> 5 },
                previous = null,
                random = random,
            )
            if (PracticeGrid.isEasy(GridOperation.Multiply, a, b)) easy++
        }
        val easyShare = easy.toDouble() / iterations
        assertTrue(
            "easy share was $easyShare, expected about ${PracticeGrid.EASY_SHARE_CAP}",
            abs(easyShare - PracticeGrid.EASY_SHARE_CAP) < 0.02,
        )
    }

    @Test
    fun `the previous cell is avoided when there is an alternative`() {
        val random = Random(seed = 3)
        val previous = 2 to 3
        repeat(500) {
            val picked = PracticeGrid.choose(
                cells = cells,
                operation = GridOperation.Multiply,
                value = { _, _ -> 0 },
                previous = previous,
                random = random,
            )
            assertTrue("repeated the previous problem", picked != previous)
        }
    }

    @Test
    fun `a single cell is returned even when it was the previous one`() {
        val picked = PracticeGrid.choose(
            cells = listOf(3 to 4),
            operation = GridOperation.Multiply,
            value = { _, _ -> 0 },
            previous = 3 to 4,
            random = Random(seed = 1),
        )
        assertEquals(3 to 4, picked)
    }

    /** The division lessons' cell space: divisor 1..6, dividend a multiple of it up to 24. */
    private val divisionCells: List<Pair<Int, Int>> =
        (1..6).flatMap { divisor ->
            (1..24 / divisor).map { quotient -> divisor * quotient to divisor }
        }

    @Test
    fun `dividing by one owns most of the division grid`() {
        // The premise for balancing: it is not that the weight is wrong,
        // it is that the cell space is lopsided.
        assertEquals(58, divisionCells.size)
        assertEquals(24, divisionCells.count { (_, divisor) -> divisor == 1 })
        assertEquals(4, divisionCells.count { (_, divisor) -> divisor == 6 })
    }

    @Test
    fun `balancing on the divisor stops one crowding out the rest`() {
        val random = Random(seed = 7)
        val draws = 12_000
        val counts = IntArray(7)
        repeat(draws) {
            val (_, divisor) = PracticeGrid.choose(
                cells = divisionCells,
                operation = GridOperation.Divide,
                value = { _, _ -> PracticeGrid.CELL_TARGET },   // fully covered: draws from the whole grid
                previous = null,
                random = random,
                balanceBy = { _, divisor -> divisor },
            )
            counts[divisor]++
        }
        // Divisor 1 is easy, so it lands at half a share; 2..6 are level.
        val one = counts[1].toDouble() / draws
        assertTrue("÷1 share was $one", abs(one - 1.0 / 11.0) < 0.02)
        for (divisor in 2..6) {
            val share = counts[divisor].toDouble() / draws
            assertTrue("÷$divisor share was $share", abs(share - 2.0 / 11.0) < 0.02)
        }
    }

    @Test
    fun `without balancing, dividing by one takes the whole easy allowance`() {
        // Every easy cell in the division grid is a divide by one, so the
        // cap alone leaves it the largest single divisor by some way. That
        // is the premise for balancing: the cap bounds it, balancing then
        // levels it with the others.
        val random = Random(seed = 7)
        val draws = 12_000
        var byOne = 0
        repeat(draws) {
            val (_, divisor) = PracticeGrid.choose(
                cells = divisionCells,
                operation = GridOperation.Divide,
                value = { _, _ -> PracticeGrid.CELL_TARGET },
                previous = null,
                random = random,
            )
            if (divisor == 1) byOne++
        }
        val share = byOne.toDouble() / draws
        assertTrue(
            "÷1 share was $share, expected about the cap ${PracticeGrid.EASY_SHARE_CAP}",
            abs(share - PracticeGrid.EASY_SHARE_CAP) < 0.02,
        )
        // Balancing takes it further still — see the test above, where it
        // lands at a eleventh against two elevenths for each of ÷2..÷6.
        assertTrue("balancing should improve on this", share > 1.0 / 11.0 + 0.02)
    }
}
