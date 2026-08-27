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

private const val OPENING_MS = 2_600L
private const val GROUP_STEP_MS = 750L
private const val ANIMAL_STEP_MS = 640L
private const val BLANK_FILL_MS = 2_000L
private const val RESULT_MS = 3_000L

/**
 * Worked example for Multiplication Construction, which asks the question
 * backwards: the picture is given and the two numbers are missing.
 *
 * So the demonstration reads the picture. The boxed groups appear with
 * the equation's two blanks below them; the groups are counted first,
 * each numbered as it is counted, and that count drops into the first
 * blank; then one group is opened up and its animals counted, and that
 * drops into the second. It closes on "3 times 4".
 */
@Composable
internal fun MultiplicationConstructionIntro(
    range: IntRange,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = remember { introOperand(range) }
    val each = remember { introOperand(range) }
    val animal = remember { Animals.all[Random.nextInt(Animals.all.size)] }

    var countedGroups by remember { mutableStateOf(0) }
    var countedAnimals by remember { mutableStateOf(0) }
    var litGroup by remember { mutableStateOf<Int?>(null) }
    var firstBlank by remember { mutableStateOf<Int?>(null) }
    var secondBlank by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) { onDispose { Tts.stopAll() } }

    LaunchedEffect(Unit) {
        val plural = "${animal.name.lowercase()}s"
        narrate("Which two numbers made this?", OPENING_MS)

        // How many groups — count the boxes themselves.
        narrate("Count the groups", GROUP_STEP_MS)
        for (g in 1..groups) {
            countedGroups = g
            litGroup = g - 1
            narrate(g.toString(), GROUP_STEP_MS)
        }
        litGroup = null
        firstBlank = groups
        narrate("$groups groups", BLANK_FILL_MS)

        // How many in a group — count inside one of them.
        litGroup = 0
        narrate("Count one group", GROUP_STEP_MS)
        for (n in 1..each) {
            countedAnimals = n
            narrate(n.toString(), ANIMAL_STEP_MS)
        }
        litGroup = null
        secondBlank = each
        narrate("$each $plural in each group", BLANK_FILL_MS)

        narrate("$groups times $each", RESULT_MS)
        onFinished()
    }

    val perRow = if (groups <= 2) groups else (groups + 1) / 2

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
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
                                // The group's own number, while the groups
                                // are being counted.
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
                                        CountedAnimal(
                                            emoji = animal.emoji,
                                            // Only the opened group takes
                                            // numbers over its animals.
                                            position = if (group == 0) i + 1 else 0,
                                            counted = countedAnimals,
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

        // The lesson's own equation, filling in as each number is found.
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Blank(firstBlank)
            Text(
                text = "×",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Blank(secondBlank)
        }
    }
}

/** One operand slot: an underscore until its number is found. */
@Composable
private fun Blank(value: Int?) {
    Text(
        text = value?.toString() ?: "_",
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = if (value == null) {
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.primary
        },
    )
}
