package com.dividetask.homeschoolteacher.intro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dividetask.homeschoolteacher.Tts
import com.dividetask.homeschoolteacher.reading.Animals
import kotlinx.coroutines.delay
import kotlin.random.Random

/** How long the problem sits on screen before the groups move. */
private const val PROBLEM_MS = 1_900L

/** The slide itself, then a beat before counting starts. */
private const val MERGE_MS = 900L
private const val MERGE_SETTLE_MS = 500L

/** A beat after the last animal is counted, before the answer lands. */
private const val COUNT_TAIL_MS = 700L

/** How long the finished equation stays up. */
private const val RESULT_MS = 2_800L

/**
 * Time per animal while counting. Small totals get a leisurely pace; a
 * bigger one would drag at that speed, so the step shortens rather than
 * letting a 16-animal count run past twenty seconds.
 */
private fun countStepMs(total: Int): Long = if (total <= 8) 620L else 430L

private enum class AdditionPhase { Problem, Merge, Counting, Result }

/**
 * Worked example for the counting addition lessons.
 *
 * Shows `X + Y` as two groups of animals — exactly the picture the
 * lesson puts up — and says the problem aloud; slides the groups
 * together into one row, clearing the operator away; counts the row off
 * one animal at a time, writing each number above the animal as it is
 * counted; then says the whole sentence, "3 plus 2 equals 5 zebras".
 *
 * The numbers over the animals are the only writing on screen: the
 * explanation is spoken, and with the sound off the pictures carry it.
 */
@Composable
internal fun CountingAdditionIntro(
    range: IntRange,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val left = remember { introOperand(range) }
    val right = remember { introOperand(range) }
    val animal = remember { Animals.all[Random.nextInt(Animals.all.size)] }
    val total = left + right

    var phase by remember { mutableStateOf(AdditionPhase.Problem) }
    var counted by remember { mutableStateOf(0) }

    DisposableEffect(Unit) { onDispose { Tts.stopAll() } }

    LaunchedEffect(Unit) {
        narrate("$left plus $right", PROBLEM_MS)
        phase = AdditionPhase.Merge
        delay(MERGE_MS + MERGE_SETTLE_MS)
        phase = AdditionPhase.Counting
        val step = countStepMs(total)
        for (n in 1..total) {
            counted = n
            narrate(n.toString(), step)
        }
        delay(COUNT_TAIL_MS)
        phase = AdditionPhase.Result
        narrate("$left plus $right equals $total ${animal.name.lowercase()}s", RESULT_MS)
        onFinished()
    }

    val merged = phase != AdditionPhase.Problem
    val gap by animateDpAsState(
        targetValue = if (merged) 0.dp else 20.dp,
        animationSpec = tween(MERGE_MS.toInt()),
        label = "gap",
    )
    val plusAlpha by animateFloatAsState(
        targetValue = if (merged) 0f else 1f,
        animationSpec = tween(MERGE_MS.toInt()),
        label = "plusAlpha",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Every animal gets an equal slice of the width, shrinking as
            // the total grows so even 8 + 8 stays on one line.
            val slot = minOf(52.dp, (maxWidth - gap * 2 - 40.dp) / total)
            val emoji = with(LocalDensity.current) { (slot * 0.78f).toSp() }
            val numeral = with(LocalDensity.current) { (slot * 0.42f).toSp() }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                repeat(left) { i ->
                    CountedAnimal(animal.emoji, i + 1, counted, slot, emoji, numeral)
                }
                Spacer(modifier = Modifier.width(gap))
                Text(
                    text = "+",
                    fontSize = emoji,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.alpha(plusAlpha),
                )
                Spacer(modifier = Modifier.width(gap))
                repeat(right) { i ->
                    CountedAnimal(animal.emoji, left + i + 1, counted, slot, emoji, numeral)
                }
                // The question mark belongs to the problem, not to the
                // working, so it clears away with the plus sign.
                AnimatedVisibility(
                    visible = !merged,
                    enter = fadeIn(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(gap))
                        Text(
                            text = "= ?",
                            fontSize = emoji,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
