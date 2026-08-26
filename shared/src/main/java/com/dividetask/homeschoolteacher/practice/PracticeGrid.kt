package com.dividetask.homeschoolteacher.practice

import kotlin.random.Random

/** The arithmetic a grid-backed lesson drills. */
enum class GridOperation { Add, Subtract, Multiply, Divide }

/**
 * Shared policy for the lessons that track coverage in an operand grid:
 * which cell to ask next, and how many correct answers a cell needs
 * before it counts as covered.
 *
 * Pure so it can be exercised in unit tests without Android ViewModels or
 * SharedPreferences — the ViewModels supply their own cell list and a
 * lookup into whatever grid they keep.
 *
 * ## Easy cells
 *
 * Some cells are barely worth drilling: adding or subtracting zero,
 * multiplying by zero or one, dividing by one. A learner gets them right
 * on sight, so asking them as often as `7 × 8` wastes the round. They are
 * damped in two ways:
 *
 * - they need only [EASY_CELL_TARGET] correct answer to count as covered,
 *   where an ordinary cell needs [CELL_TARGET]; and
 * - whenever a pool is drawn from, they carry [EASY_CELL_WEIGHT] — half
 *   the weight of an ordinary cell — so they come up half as often both
 *   while the lesson is still being covered and afterwards.
 *
 * ## Balanced operands
 *
 * A lesson's cells are not always spread evenly across its operands.
 * Division is the clear case: the dividend runs 1..24 and only the pairs
 * that divide exactly are ever asked, so `÷ 1` owns 24 of the 58 cells
 * while `÷ 5` and `÷ 6` own four each. Drawing uniformly over cells
 * spends a quarter of every round dividing by one, and halving the easy
 * cells only takes that to an eighth — the problem is the shape of the
 * cell space, not the weight of any one cell.
 *
 * A lesson can pass [choose] a `balanceBy` key. Every distinct key value
 * then comes up equally often however many cells it owns, because a
 * cell's weight is divided by the number of cells in the pool sharing its
 * key. It multiplies with the easy weight rather than replacing it.
 */
object PracticeGrid {

    /** Correct answers an ordinary cell needs before it counts as covered. */
    const val CELL_TARGET = 2

    /** Correct answers an easy cell needs — see [isEasy]. */
    const val EASY_CELL_TARGET = 1

    /** Pick weight of an easy cell, against 1.0 for an ordinary one. */
    const val EASY_CELL_WEIGHT = 0.5

    /** Chance of ignoring coverage and picking from the whole grid. */
    private const val WILDCARD_CHANCE = 0.10

    /**
     * Whether `a op b` is one of the cells a learner gets right on sight:
     * a zero operand in addition or subtraction, a zero or one operand in
     * multiplication, dividing by one (or, if a lesson ever asks it,
     * dividing zero by something).
     */
    fun isEasy(operation: GridOperation, a: Int, b: Int): Boolean = when (operation) {
        GridOperation.Add,
        GridOperation.Subtract -> a == 0 || b == 0
        GridOperation.Multiply -> a <= 1 || b <= 1
        // a is the dividend, b the divisor.
        GridOperation.Divide -> b <= 1 || a == 0
    }

    /** Correct answers this cell needs before it counts as covered. */
    fun target(operation: GridOperation, a: Int, b: Int): Int =
        if (isEasy(operation, a, b)) EASY_CELL_TARGET else CELL_TARGET

    /** Whether every cell has reached its [target]. */
    fun covered(
        cells: List<Pair<Int, Int>>,
        operation: GridOperation,
        value: (Int, Int) -> Int,
    ): Boolean = cells.all { (a, b) -> value(a, b) >= target(operation, a, b) }

    /**
     * Pick the next cell to ask.
     *
     * One roll in ten ignores coverage entirely and draws from the whole
     * grid. Otherwise the draw is from the cells furthest behind their
     * [target] — so a never-asked ordinary cell outranks one already
     * answered right once — and once every cell is covered, from the whole
     * grid again. Easy cells are drawn at [EASY_CELL_WEIGHT] throughout.
     *
     * @param cells every cell the lesson can ask, as `(a, b)`.
     * @param value current coverage count of a cell.
     * @param previous the cell just asked, avoided when there is a choice.
     * @param balanceBy optional key whose every value should come up
     *   equally often regardless of how many cells carry it — see
     *   "Balanced operands" above. Division passes the divisor.
     */
    fun choose(
        cells: List<Pair<Int, Int>>,
        operation: GridOperation,
        value: (Int, Int) -> Int,
        previous: Pair<Int, Int>?,
        random: Random = Random,
        balanceBy: ((Int, Int) -> Int)? = null,
    ): Pair<Int, Int> {
        require(cells.isNotEmpty()) { "No cells to choose from" }
        val behind = cells.filter { (a, b) -> value(a, b) < target(operation, a, b) }
        val basePool = if (behind.isEmpty() || random.nextDouble() < WILDCARD_CHANCE) {
            cells
        } else {
            val deepest = behind.maxOf { (a, b) -> target(operation, a, b) - value(a, b) }
            behind.filter { (a, b) -> target(operation, a, b) - value(a, b) == deepest }
        }
        val pool = if (previous != null && basePool.size > 1) {
            basePool.filter { it != previous }.ifEmpty { basePool }
        } else {
            basePool
        }
        return weightedPick(pool, operation, random, balanceBy)
    }

    private fun weightedPick(
        pool: List<Pair<Int, Int>>,
        operation: GridOperation,
        random: Random,
        balanceBy: ((Int, Int) -> Int)?,
    ): Pair<Int, Int> {
        // Cells sharing a balance key split one key's worth of weight
        // between them, so every key is drawn equally often however many
        // cells it owns.
        val share: Map<Int, Int> = if (balanceBy == null) {
            emptyMap()
        } else {
            pool.groupingBy { (a, b) -> balanceBy(a, b) }.eachCount()
        }
        val weights = pool.map { (a, b) ->
            val easy = if (isEasy(operation, a, b)) EASY_CELL_WEIGHT else 1.0
            if (balanceBy == null) easy
            else easy / (share[balanceBy(a, b)] ?: 1)
        }
        var r = random.nextDouble(weights.sum())
        for (i in pool.indices) {
            if (r < weights[i]) return pool[i]
            r -= weights[i]
        }
        return pool.last()
    }
}
