package com.dividetask.homeschoolteacher.intro

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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

private enum class Phase { Problem, Merge, Counting, Result }

/**
 * Worked example for the counting addition lessons.
 *
 * Shows `X + Y` as two groups of animals the way the lesson draws them
 * and says the problem aloud; slides the groups together into one row;
 * counts the row off one animal at a time, writing each number above the
 * animal as it is counted; then lands on the answer and says the whole
 * sentence — "3 plus 2 equals 5 zebras".
 *
 * Everything the narration says is also shown, so the demonstration
 * stands on its own with the sound off.
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

    var phase by remember { mutableStateOf(Phase.Problem) }
    var counted by remember { mutableStateOf(0) }

    DisposableEffect(Unit) { onDispose { Tts.stopAll() } }

    LaunchedEffect(Unit) {
        Tts.say("$left plus $right")
        delay(PROBLEM_MS)
        phase = Phase.Merge
        delay(MERGE_MS + MERGE_SETTLE_MS)
        phase = Phase.Counting
        val step = countStepMs(total)
        for (n in 1..total) {
            counted = n
            Tts.say(n.toString())
            delay(step)
        }
        delay(COUNT_TAIL_MS)
        phase = Phase.Result
        Tts.say("$left plus $right equals $total ${animal.name.lowercase()}s")
        delay(RESULT_MS)
        onFinished()
    }

    val merged = phase != Phase.Problem
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
        Equation(
            left = left,
            right = right,
            answer = if (phase == Phase.Result) total else null,
        )

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
            }
        }
    }
}

/**
 * One animal with room above it for its number, which appears when the
 * count reaches it. The space is always reserved so nothing shifts as
 * the numbers arrive.
 */
@Composable
private fun CountedAnimal(
    emoji: String,
    position: Int,
    counted: Int,
    slot: Dp,
    emojiSize: TextUnit,
    numeralSize: TextUnit,
) {
    val reached = position <= counted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(slot),
    ) {
        Box(
            modifier = Modifier.height(slot * 0.6f),
            contentAlignment = Alignment.Center,
        ) {
            if (reached) {
                Text(
                    text = position.toString(),
                    fontSize = numeralSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = emoji,
            fontSize = emojiSize,
            modifier = Modifier.alpha(if (counted == 0 || reached) 1f else 0.45f),
        )
    }
}

/** `X + Y = ?` until the count lands, then `X + Y = N`. */
@Composable
private fun Equation(left: Int, right: Int, answer: Int?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val onBg = MaterialTheme.colorScheme.onBackground
        Text("$left", fontSize = 40.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, color = onBg)
        Text("+", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = onBg)
        Text("$right", fontSize = 40.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, color = onBg)
        Text("=", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = onBg)
        Text(
            text = answer?.toString() ?: "?",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (answer == null) onBg else Color(0xFF22C55E),
        )
    }
}
