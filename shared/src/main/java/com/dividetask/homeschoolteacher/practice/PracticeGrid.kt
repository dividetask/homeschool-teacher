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
    ): Boolean = covered(
        cells = cells,
        value = { (a, b) -> value(a, b) },
        target = { (a, b) -> target(operation, a, b) },
    )

    /**
     * Whether every cell has reached its target, for a grid whose cells
     * are not `(op1, op2)` pairs — the binary lessons key theirs by
     * `(operator, op1, op2)`. Defaults to the ordinary [CELL_TARGET],
     * since only the arithmetic grids have easy cells.
     */
    fun <T> covered(
        cells: List<T>,
        value: (T) -> Int,
        target: (T) -> Int = { CELL_TARGET },
    ): Boolean = cells.all { cell -> value(cell) >= target(cell) }

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
     */
    fun choose(
        cells: List<Pair<Int, Int>>,
        operation: GridOperation,
        value: (Int, Int) -> Int,
        previous: Pair<Int, Int>?,
        random: Random = Random,
    ): Pair<Int, Int> = choose(
        cells = cells,
        value = { (a, b) -> value(a, b) },
        target = { (a, b) -> target(operation, a, b) },
        weight = { (a, b) ->
            if (isEasy(operation, a, b)) EASY_CELL_WEIGHT else 1.0
        },
        previous = previous,
        random = random,
    )

    /**
     * The same selection for a grid whose cells are not `(op1, op2)`
     * pairs — the binary lessons key theirs by `(operator, op1, op2)`.
     * [target] and [weight] default to the ordinary values, since only
     * the arithmetic grids have easy cells.
     */
    fun <T> choose(
        cells: List<T>,
        value: (T) -> Int,
        previous: T?,
        random: Random = Random,
        target: (T) -> Int = { CELL_TARGET },
        weight: (T) -> Double = { 1.0 },
    ): T {
        require(cells.isNotEmpty()) { "No cells to choose from" }
        val behind = cells.filter { value(it) < target(it) }
        val basePool = if (behind.isEmpty() || random.nextDouble() < WILDCARD_CHANCE) {
            cells
        } else {
            val deepest = behind.maxOf { target(it) - value(it) }
            behind.filter { target(it) - value(it) == deepest }
        }
        val pool = if (previous != null && basePool.size > 1) {
            basePool.filter { it != previous }.ifEmpty { basePool }
        } else {
            basePool
        }
        return weightedPick(pool, weight, random)
    }

    private fun <T> weightedPick(
        pool: List<T>,
        weight: (T) -> Double,
        random: Random,
    ): T {
        val weights = pool.map(weight)
        var r = random.nextDouble(weights.sum())
        for (i in pool.indices) {
            if (r < weights[i]) return pool[i]
            r -= weights[i]
        }
        return pool.last()
    }
}
