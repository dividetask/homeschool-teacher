package com.dividetask.homeschoolteacher.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dividetask.homeschoolteacher.Tts
import com.dividetask.homeschoolteacher.reading.Animals
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Held on the picture while a sentence explaining it is spoken. */
private const val SENTENCE_MS = 3_400L

/** A shorter sentence — the lead-in to a count. */
private const val LEAD_MS = 1_600L

/** One group being counted, and one animal being counted. */
private const val GROUP_STEP_MS = 700L
private const val ANIMAL_STEP_MS = 620L

/** A beat between one count finishing and the next thing being said. */
private const val BETWEEN_MS = 700L

private const val RESULT_MS = 3_200L

/** A long total is counted a little faster so it doesn't drag. */
private fun countStepMs(total: Int): Long = if (total <= 8) ANIMAL_STEP_MS else 430L

/** Which count is running, which decides what the numbers mean. */
private enum class MultiplicationPhase { Means, Groups, FirstGroup, All }

/**
 * Worked example for Counting Multiplication.
 *
 * `X × Y` is drawn as X boxed groups of Y animals, the way the lesson
 * draws it, and read out as what it means before anything is counted:
 *
 * 1. "4 times 3 means 4 groups of 3."
 * 2. "Count the groups" — 1, 2, 3, 4, a number landing on each box.
 * 3. "Count the zebras in the first group" — 1, 2, 3 inside that box.
 * 4. "The answer is the total number of zebras, let's count them" —
 *    1 through 12, straight through every box.
 * 5. "4 times 3 equals 12 zebras."
 *
 * The three counts answer three different questions with the same
 * picture, which is the point: how many groups, how big a group, and how
 * many altogether.
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

    var phase by remember { mutableStateOf(MultiplicationPhase.Means) }
    var countedGroups by remember { mutableStateOf(0) }
    var countedFirst by remember { mutableStateOf(0) }
    var countedAll by remember { mutableStateOf(0) }
    var litGroup by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) { onDispose { Tts.stopAll() } }

    LaunchedEffect(Unit) {
        val plural = "${animal.name.lowercase()}s"

        narrate("$groups times $each means $groups groups of $each", SENTENCE_MS)

        // How many groups: a number lands on each box in turn.
        phase = MultiplicationPhase.Groups
        narrate("Count the groups", LEAD_MS)
        for (g in 1..groups) {
            countedGroups = g
            litGroup = g - 1
            narrate(g.toString(), GROUP_STEP_MS)
        }
        litGroup = null
        delay(BETWEEN_MS)

        // How big a group: only the first one is opened up.
        phase = MultiplicationPhase.FirstGroup
        litGroup = 0
        narrate("Count the $plural in the first group", SENTENCE_MS)
        for (n in 1..each) {
            countedFirst = n
            narrate(n.toString(), ANIMAL_STEP_MS)
        }
        litGroup = null
        delay(BETWEEN_MS)

        // How many altogether: straight through every box.
        phase = MultiplicationPhase.All
        narrate("The answer is the total number of $plural, let's count them", SENTENCE_MS)
        val step = countStepMs(total)
        for (n in 1..total) {
            countedAll = n
            narrate(n.toString(), step)
        }
        delay(BETWEEN_MS)

        narrate("$groups times $each equals $total $plural", RESULT_MS)
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // The group's own number, from the first count.
                                Box(
                                    modifier = Modifier.width(slot),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (group < countedGroups) {
                                        Text(
                                            text = (group + 1).toString(),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                GroupBox(highlighted = litGroup == group) {
                                    repeat(each) { i ->
                                        // What a number over an animal means
                                        // depends on which count is running:
                                        // its place in this group, or its
                                        // place among all of them.
                                        val position = when (phase) {
                                            MultiplicationPhase.FirstGroup -> if (group == 0) i + 1 else 0
                                            MultiplicationPhase.All -> group * each + i + 1
                                            else -> 0
                                        }
                                        val counted = when (phase) {
                                            MultiplicationPhase.FirstGroup -> countedFirst
                                            MultiplicationPhase.All -> countedAll
                                            else -> 0
                                        }
                                        CountedAnimal(
                                            emoji = animal.emoji,
                                            position = position,
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
}
