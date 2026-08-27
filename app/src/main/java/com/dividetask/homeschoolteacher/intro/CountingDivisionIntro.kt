package com.dividetask.homeschoolteacher.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dividetask.homeschoolteacher.Tts
import com.dividetask.homeschoolteacher.reading.Animals
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val OPENING_MS = 3_000L
private const val DEAL_SETTLE_MS = 900L
private const val COUNT_LEAD_MS = 900L
private const val COUNT_STEP_MS = 660L
private const val RESULT_MS = 3_200L

/** How fast the animals are dealt out; a long share-out speeds up. */
private fun dealStepMs(total: Int): Long = if (total <= 12) 420L else 300L

/**
 * Worked example for the counting division lessons.
 *
 * The dividend stands loose above a row of empty pens — the picture the
 * lesson opens with. The animals are then dealt into the pens one at a
 * time, round and round, so the sharing is visibly fair rather than
 * something that happens off screen. When the last one lands, one pen is
 * opened up and its animals counted: that count is the answer, and it is
 * the same in every pen.
 *
 * @param cells the (dividend, divisor) pairs this level can ask, so the
 *   example is one of the lesson's own problems.
 */
@Composable
internal fun CountingDivisionIntro(
    cells: List<Pair<Int, Int>>,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Never one pen or one animal per pen: a share-out has to share.
    val problem = remember {
        val worthShowing = cells.filter { (dividend, divisor) ->
            divisor >= 2 && dividend / divisor >= 2
        }.ifEmpty { cells }
        worthShowing[Random.nextInt(worthShowing.size)]
    }
    val dividend = problem.first
    val divisor = problem.second
    val quotient = dividend / divisor
    val animal = remember { Animals.all[Random.nextInt(Animals.all.size)] }

    // How many have been dealt, and how many of the opened pen counted.
    var dealt by remember { mutableStateOf(0) }
    var counted by remember { mutableStateOf(0) }
    var litPen by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) { onDispose { Tts.stopAll() } }

    LaunchedEffect(Unit) {
        val plural = "${animal.name.lowercase()}s"
        narrate("$dividend $plural, shared into $divisor pens", OPENING_MS)

        val step = dealStepMs(dividend)
        for (n in 1..dividend) {
            dealt = n
            litPen = (n - 1) % divisor
            delay(step)
        }
        litPen = null
        delay(DEAL_SETTLE_MS)

        litPen = 0
        narrate("Now count one pen", COUNT_LEAD_MS)
        for (n in 1..quotient) {
            counted = n
            narrate(n.toString(), COUNT_STEP_MS)
        }
        narrate("$quotient $plural in every pen. $dividend divided by $divisor equals $quotient", RESULT_MS)
        onFinished()
    }

    // Round-robin: pen p ends up with every animal whose turn fell on it.
    fun penCount(pen: Int): Int = (0 until dealt).count { it % divisor == pen }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val penSlot = introSlot(
                available = maxWidth,
                count = divisor * quotient,
                chrome = GROUP_BOX_CHROME * divisor + 16.dp,
                max = 44.dp,
            )
            val poolSlot = introSlot(available = maxWidth, count = dividend, max = 44.dp)
            val penEmoji = with(LocalDensity.current) { (penSlot * 0.78f).toSp() }
            val penNumeral = with(LocalDensity.current) { (penSlot * 0.42f).toSp() }
            val poolEmoji = with(LocalDensity.current) { (poolSlot * 0.78f).toSp() }

            Column(
                verticalArrangement = Arrangement.spacedBy(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // The animals still waiting to be shared out. The row keeps
                // its height as it empties so the pens below stay put.
                Box(
                    modifier = Modifier.fillMaxWidth().height(poolSlot * 1.4f),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(dividend - dealt) {
                            CountedAnimal(
                                emoji = animal.emoji,
                                position = 0,
                                counted = 0,
                                slot = poolSlot,
                                emojiSize = poolEmoji,
                                numeralSize = poolEmoji,
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    (0 until divisor).forEach { pen ->
                        GroupBox(
                            highlighted = litPen == pen,
                            minHeight = penSlot * 1.6f,
                        ) {
                            repeat(penCount(pen)) { i ->
                                CountedAnimal(
                                    emoji = animal.emoji,
                                    // Only the opened pen is numbered.
                                    position = if (pen == 0) i + 1 else 0,
                                    counted = counted,
                                    slot = penSlot,
                                    emojiSize = penEmoji,
                                    numeralSize = penNumeral,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
