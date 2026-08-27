package com.dividetask.homeschoolteacher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The single-tap answer grid every math lesson uses: one button per
 * candidate answer from `0` to [maxAnswer]. Shared so the column count,
 * button size and feedback colours are the same on every screen — see
 * `docs/lessons.md` § Answer Surfaces → Numeric Grid.
 *
 * @param answered whether the problem has been answered (or revealed), at
 *   which point the correct button turns green, the learner's wrong pick
 *   red, and the rest fade.
 */
@Composable
fun NumericGrid(
    maxAnswer: Int,
    selected: Int?,
    correct: Int,
    answered: Boolean,
    inputEnabled: Boolean,
    onChoose: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cols = columnsFor(maxAnswer)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.widthIn(max = 480.dp).fillMaxWidth(),
    ) {
        (0..maxAnswer).toList().chunked(cols).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { choice ->
                    val container = when {
                        !answered -> MaterialTheme.colorScheme.primary
                        choice == correct -> Color(0xFF22C55E)
                        choice == selected -> Color(0xFFEF4444)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    }
                    Button(
                        onClick = { onChoose(choice) },
                        enabled = inputEnabled && !answered,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = container,
                            disabledContainerColor = container,
                            contentColor = Color.White,
                            disabledContentColor = Color.White,
                        ),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    ) {
                        Text(
                            text = choice.toString(),
                            fontSize = if (maxAnswer <= 9) 24.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                repeat(cols - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Columns for a grid running `0..maxAnswer`: 5 while the answers stay
 * small, 7 once there are enough of them that 5 columns would run off
 * the bottom of a phone.
 */
private fun columnsFor(maxAnswer: Int): Int = if (maxAnswer <= 18) 5 else 7
