package com.dividetask.homeschoolteacher.intro

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dividetask.homeschoolteacher.Tts
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val PROBLEM_MS = 1_800L
private const val LAND_MS = 1_600L
private const val HOP_MS = 800L
private const val RESULT_MS = 3_000L

/** How long a single hop's arc takes to draw. */
private const val HOP_DRAW_MS = 420

/**
 * Worked example for the number line addition lessons.
 *
 * A dot lands on the first operand — you start from what you already
 * have, rather than counting up from zero — and then hops one place at a
 * time, an arc drawn over each hop, counting aloud as it lands. Where it
 * stops is the answer.
 *
 * Only the line's own numbers are written; the arithmetic is spoken.
 */
@Composable
internal fun NumberLineAdditionIntro(
    range: IntRange,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val left = remember { introOperand(range) }
    val right = remember { introOperand(range) }
    val total = left + right

    // The line runs a little past the answer so the last hop isn't at the
    // very edge, and always from zero.
    val highest = total + 1

    var landed by remember { mutableStateOf(false) }
    var hops by remember { mutableStateOf(0) }

    DisposableEffect(Unit) { onDispose { Tts.stopAll() } }

    LaunchedEffect(Unit) {
        narrate("$left plus $right", PROBLEM_MS)
        landed = true
        narrate("Start at $left", LAND_MS)
        for (n in 1..right) {
            hops = n
            narrate((left + n).toString(), HOP_MS)
        }
        narrate("$left plus $right equals $total", RESULT_MS)
        onFinished()
    }

    // The dot slides along as each hop is taken.
    val dotAt by animateFloatAsState(
        targetValue = (left + hops).toFloat(),
        animationSpec = tween(HOP_DRAW_MS),
        label = "dot",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cell = maxWidth / (highest + 1)
            val lineColor = MaterialTheme.colorScheme.onBackground
            val markColor = MaterialTheme.colorScheme.primary

            Column(modifier = Modifier.fillMaxWidth()) {
                // The arcs and the travelling dot, drawn above the line.
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cell * 2.2f),
                ) {
                    val step = size.width / (highest + 1)
                    fun x(value: Float) = step * (value + 0.5f)
                    val baseline = size.height

                    // One arc per hop taken so far, kept on screen so the
                    // whole journey stays visible.
                    for (hop in 0 until hops) {
                        val from = x((left + hop).toFloat())
                        val to = x((left + hop + 1).toFloat())
                        val path = Path().apply {
                            moveTo(from, baseline)
                            quadraticBezierTo(
                                (from + to) / 2f,
                                baseline - size.height * 0.9f,
                                to,
                                baseline,
                            )
                        }
                        drawPath(
                            path = path,
                            color = markColor,
                            style = Stroke(width = 6f, cap = StrokeCap.Round),
                        )
                    }

                    if (landed) {
                        drawCircle(
                            color = markColor,
                            radius = step * 0.22f,
                            center = Offset(x(dotAt), baseline),
                        )
                    }
                }

                // The line itself: every integer ticked and labelled, the
                // way the lesson draws it.
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (n in 0..highest) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(lineColor),
                                )
                                Box(
                                    Modifier
                                        .width(2.dp)
                                        .height(12.dp)
                                        .background(lineColor),
                                )
                            }
                            val reached = landed && n >= left && n <= left + hops
                            Text(
                                text = n.toString(),
                                fontSize = if (cell < 26.dp) 11.sp else 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (reached) FontWeight.Bold else FontWeight.Normal,
                                color = if (reached) markColor else lineColor.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
    }
}
