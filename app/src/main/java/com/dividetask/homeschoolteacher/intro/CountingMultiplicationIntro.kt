package com.dividetask.homeschoolteacher.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Held on the picture while the sentence explaining it is spoken. */
private const val MEANS_MS = 3_400L

/** Each group lights in turn while "and X groups" is said. */
private const val GROUP_SWEEP_MS = 700L

private const val COUNT_TAIL_MS = 800L
private const val RESULT_MS = 3_000L

private fun countStepMs(total: Int): Long = if (total <= 8) 620L else 430L

/**
 * Worked example for Counting Multiplication.
 *
 * `X × Y` is drawn as X boxed groups of Y animals, the way the lesson
 * draws it, and narrated as what it means: "3 times 4 means 3 groups of
 * 4. There are 4 zebras in each group, and 3 groups." One group lights
 * while the size of a group is named, then each in turn while they are
 * counted. Every animal is then counted straight through, numbers
 * appearing above them, and the sentence closes: "3 times 4 equals 12".
 */
@Composable
internal fun CountingMultiplicationIntro(
    range: IntRange,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = remember { introOperand(range) }
    val each = remember { introOperand(range) }
    val animal = remember { Animals.all[Random.nextInt(Animals.all.size)] }
    val total = groups * each

    var counted by remember { mutableStateOf(0) }
    // -1 lights every group at once, null lights none.
    var litGroup by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) { onDispose { Tts.stopAll() } }

    LaunchedEffect(Unit) {
        val plural = "${animal.name.lowercase()}s"
        narrate("$groups times $each means $groups groups of $each", MEANS_MS)

        // "There are Y zebras in each group" — one group stands out, so
        // it is clear which number is which.
        litGroup = 0
        narrate("There are $each $plural in each group", MEANS_MS)

        // "...and X groups" — the outline walks along them while it is
        // said, so both finish before the counting starts.
        val sweep = launch {
            for (g in 0 until groups) {
                litGroup = g
                delay(GROUP_SWEEP_MS)
            }
        }
        Tts.sayAwait("and $groups groups")
        sweep.join()
        litGroup = null

        val step = countStepMs(total)
        for (n in 1..total) {
            counted = n
            narrate(n.toString(), step)
        }
        delay(COUNT_TAIL_MS)
        narrate("$groups times $each equals $total", RESULT_MS)
        onFinished()
    }

    // Two rows of boxes once there are more than two groups, so the
    // animals inside stay big enough to count.
    val perRow = if (groups <= 2) groups else (groups + 1) / 2

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val slot = introSlot(
                available = maxWidth,
                count = perRow * each,
                chrome = GROUP_BOX_CHROME * perRow + 16.dp,
                max = 46.dp,
            )
            val emoji = with(LocalDensity.current) { (slot * 0.78f).toSp() }
            val numeral = with(LocalDensity.current) { (slot * 0.42f).toSp() }
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                (0 until groups).chunked(perRow).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        row.forEach { group ->
                            GroupBox(highlighted = litGroup == group) {
                                repeat(each) { i ->
                                    CountedAnimal(
                                        emoji = animal.emoji,
                                        position = group * each + i + 1,
                                        counted = counted,
                                        slot = slot,
                                        emojiSize = emoji,
                                        numeralSize = numeral,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
