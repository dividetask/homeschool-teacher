package com.dividetask.homeschoolteacher.intro

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * Width to give each animal so [count] of them fit across [available],
 * with [chrome] taken out first for anything drawn around them (a pen's
 * border and padding, an operator). Capped so a small problem doesn't
 * blow the animals up to fill the screen.
 */
internal fun introSlot(
    available: Dp,
    count: Int,
    chrome: Dp = 0.dp,
    max: Dp = 52.dp,
): Dp {
    if (count <= 0) return max
    return minOf((available - chrome) / count, max)
}

/**
 * One animal with room above it for its number, which appears when the
 * count reaches it. The space is always reserved so nothing shifts as
 * the numbers arrive, and animals not yet counted sit back a little so
 * the count is easy to follow.
 */
@Composable
internal fun CountedAnimal(
    emoji: String,
    position: Int,
    counted: Int,
    slot: Dp,
    emojiSize: TextUnit,
    numeralSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val reached = position in 1..counted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(slot),
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

/** Border and padding a [GroupBox] draws around its animals. */
internal val GROUP_BOX_CHROME: Dp = 28.dp

/**
 * The rounded box the counting lessons draw around a group of animals,
 * and the pens the division lesson shares animals into — the same shape,
 * so a learner meeting division has seen it before. [highlighted] thickens
 * and tints it, for the group the narration is talking about.
 */
@Composable
internal fun GroupBox(
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    minHeight: Dp = 0.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .heightIn(min = minHeight)
            .border(
                width = if (highlighted) 4.dp else 2.dp,
                color = if (highlighted) primary else primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        content = content,
    )
}
