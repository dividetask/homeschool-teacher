package com.dividetask.homeschoolteacher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dividetask.homeschoolteacher.binary.BinaryOperationsViewModel
import com.dividetask.homeschoolteacher.binary.BinaryOperator
import com.dividetask.homeschoolteacher.chess.ChessViewModel
import com.dividetask.homeschoolteacher.multiplication.CountingMultiplicationViewModel
import com.dividetask.homeschoolteacher.lesson.LessonDefinition
import com.dividetask.homeschoolteacher.lesson.LessonId
import com.dividetask.homeschoolteacher.lesson.Lessons
import com.dividetask.homeschoolteacher.math.MathViewModel
import com.dividetask.homeschoolteacher.reading.Animals
import com.dividetask.homeschoolteacher.reading.LetterSounds
import com.dividetask.homeschoolteacher.reading.LetterSoundsViewModel
import com.dividetask.homeschoolteacher.reading.Phonemes
import com.dividetask.homeschoolteacher.reading.PhonemesViewModel
import com.dividetask.homeschoolteacher.reading.ReadingViewModel
import com.dividetask.homeschoolteacher.reading.RhymingWords
import com.dividetask.homeschoolteacher.reading.RhymingWordsViewModel
import com.dividetask.homeschoolteacher.reading.SightWords
import com.dividetask.homeschoolteacher.reading.SightWordsViewModel
import com.dividetask.homeschoolteacher.tictactoe.GameViewModel

@Composable
fun ProgressScreen(
    game: GameViewModel,
    chess: ChessViewModel,
    math: MathViewModel,
    binary: BinaryOperationsViewModel,
    multiplication: CountingMultiplicationViewModel,
    letterSounds: LetterSoundsViewModel,
    phonemes: PhonemesViewModel,
    reading: ReadingViewModel,
    sightWords: SightWordsViewModel,
    rhymingWords: RhymingWordsViewModel,
    passedMap: Map<LessonId, Boolean>,
    manualUnlockMap: Map<LessonId, Boolean>,
    onToggleManualUnlock: (LessonId, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tttGame by game.state.collectAsStateWithLifecycle()
    val ttt0Streak by game.streak(LessonId.TicTacToe0).collectAsStateWithLifecycle()
    val ttt1Streak by game.streak(LessonId.TicTacToe1).collectAsStateWithLifecycle()
    val ttt2Streak by game.streak(LessonId.TicTacToe2).collectAsStateWithLifecycle()

    val mathStreaks by math.streaks.collectAsStateWithLifecycle()
    val mathState by math.state.collectAsStateWithLifecycle()

    val readingStreaks by reading.streaks.collectAsStateWithLifecycle()
    val readingState by reading.state.collectAsStateWithLifecycle()

    val sightWordStreaks by sightWords.streaks.collectAsStateWithLifecycle()
    val sightWordState by sightWords.state.collectAsStateWithLifecycle()

    val rhymingWordStreaks by rhymingWords.streaks.collectAsStateWithLifecycle()
    val rhymingWordState by rhymingWords.state.collectAsStateWithLifecycle()

    val phonemeStreaks by phonemes.streaks.collectAsStateWithLifecycle()
    val phonemeState by phonemes.state.collectAsStateWithLifecycle()

    val letterSoundStreaks by letterSounds.streaks.collectAsStateWithLifecycle()
    val letterSoundState by letterSounds.state.collectAsStateWithLifecycle()

    val binaryStreaks by binary.streaksSnapshot.collectAsStateWithLifecycle()
    val binaryState by binary.state.collectAsStateWithLifecycle()

    val multiplicationStreaks by multiplication.streaks.collectAsStateWithLifecycle()
    val multiplicationState by multiplication.state.collectAsStateWithLifecycle()

    val subtractionStreaks by math.subtractionGrid.collectAsStateWithLifecycle()

    // Per-lesson math streak (cells AND streak >= 8 are both required to
    // pass — the user calls this out explicitly).
    val mathLessonStreaks: Map<LessonId, Int> = listOf(
        LessonId.MathPictures,
        LessonId.Math0, LessonId.HorizontalAddition0, LessonId.NumberLineAddition0,
        LessonId.CountingAddition1, LessonId.Math1, LessonId.HorizontalAddition1,
        LessonId.MathNumberLine,
        LessonId.CountingSubtraction0, LessonId.HorizontalSubtraction0,
        LessonId.VerticalSubtraction0, LessonId.NumberLineSubtraction0,
    ).associateWith { id ->
        // collectAsStateWithLifecycle inside a loop isn't possible here,
        // so we read the StateFlow's current value. The grid + lifetime
        // counters above already trigger recomposition when an answer
        // lands, which is also when streaks change.
        math.lessonStreak(id).value
    }

    /** True when every parent of [id] has been passed (or [id] is entry-level). */
    fun naturallyUnlocked(id: LessonId): Boolean =
        Lessons.get(id).parents.all { passedMap[it] == true }

    val chess0Streak by chess.streak(LessonId.Chess0).collectAsStateWithLifecycle()
    val chess1Streak by chess.streak(LessonId.Chess1).collectAsStateWithLifecycle()
    val chess2Streak by chess.streak(LessonId.Chess2).collectAsStateWithLifecycle()
    val chess3Streak by chess.streak(LessonId.Chess3).collectAsStateWithLifecycle()
    val chessState by chess.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Flip a switch on to unlock that lesson so it appears in " +
                "the menu and Random rotation right away, even if its " +
                "prerequisites haven't been passed yet. Flip it off to " +
                "relock. The switch doesn't mark a lesson as completed — " +
                "the learner still has to play and pass it.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )

        // Lightweight helper so every section reads the same five lines
        // (passed, manuallyUnlocked, naturallyUnlocked, callback) without
        // restating the LessonId four times.
        @Composable
        fun Section(
            id: LessonId,
            content: @Composable () -> Unit,
        ) {
            LessonSection(
                def = Lessons.get(id),
                passed = passedMap[id] == true,
                manuallyUnlocked = manualUnlockMap[id] == true,
                naturallyUnlocked = naturallyUnlocked(id),
                onManualUnlockChange = { onToggleManualUnlock(id, it) },
                content = content,
            )
        }

        Section(LessonId.TicTacToe0) {
            InfoRow("Non-loss streak", "$ttt0Streak / 8")
        }

        Section(LessonId.TicTacToe1) {
            InfoRow("Non-loss streak", "$ttt1Streak / 8")
        }

        Section(LessonId.TicTacToe2) {
            InfoRow("Non-loss streak", "$ttt2Streak / 8")
        }

        InfoRow("TTT player wins", tttGame.playerScore.toString())
        InfoRow("TTT CPU wins", tttGame.cpuScore.toString())
        InfoRow("TTT draws", tttGame.drawScore.toString())

        Section(LessonId.Chess0) {
            InfoRow("Correct streak", "$chess0Streak / 8")
            InfoRow("Correct moves (chess)", chessState.correctCount.toString())
            InfoRow("Wrong moves (chess)", chessState.wrongCount.toString())
        }

        Section(LessonId.Chess1) {
            InfoRow("Correct streak", "$chess1Streak / 8")
        }

        Section(LessonId.Chess2) {
            InfoRow("Correct streak", "$chess2Streak / 8")
        }

        Section(LessonId.Chess3) {
            InfoRow("Correct streak", "$chess3Streak / 8")
        }

        Section(LessonId.LetterSounds0) {
            InfoRow("Correct streak", "${letterSoundState.runStreak} / 8")
            InfoRow("Correct (lifetime)", letterSoundState.correctCount.toString())
            InfoRow("Wrong (lifetime)", letterSoundState.wrongCount.toString())
            Text(
                text = "Passing takes 8 correct in a row AND every letter " +
                    "right at least twice in a row.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Text(
                text = "Streak per letter",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            LetterSoundsTable(letterSoundStreaks)
        }

        Section(LessonId.Phonemes0) {
            InfoRow("Correct (lifetime)", phonemeState.correctCount.toString())
            InfoRow("Wrong (lifetime)", phonemeState.wrongCount.toString())
            Text(
                text = "First-letter streak per word",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            PhonemesTable(phonemeStreaks)
        }

        // Every math Section shows its per-lesson consecutive-correct
        // streak (N/8). Passing a math lesson takes both that streak AND
        // every cell in the lesson's slice ≥ 2.
        Section(LessonId.MathPictures) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.MathPictures]} / 8")
            Text(
                text = "Uses the 1..4 corner of the addition streak grid.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }

        Section(LessonId.Math0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.Math0]} / 8")
            InfoRow("Correct (lifetime)", mathState.correctCount.toString())
            InfoRow("Wrong (lifetime)", mathState.wrongCount.toString())
            Text(
                text = "Addition streak grid (rows = left, columns = right). " +
                    "Every addition variant writes the same cells, but each " +
                    "variant also keeps its own consecutive-correct streak " +
                    "above — both have to land before a variant passes.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            MathStreakGrid(mathStreaks)
        }

        Section(LessonId.HorizontalAddition0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.HorizontalAddition0]} / 8")
        }

        Section(LessonId.NumberLineAddition0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.NumberLineAddition0]} / 8")
        }

        Section(LessonId.CountingAddition1) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.CountingAddition1]} / 8")
        }

        Section(LessonId.Math1) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.Math1]} / 8")
        }

        Section(LessonId.HorizontalAddition1) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.HorizontalAddition1]} / 8")
        }

        Section(LessonId.MathNumberLine) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.MathNumberLine]} / 8")
        }

        Section(LessonId.BinaryOps0) {
            InfoRow("Correct (lifetime)", binaryState.correctCount.toString())
            InfoRow("Wrong (lifetime)", binaryState.wrongCount.toString())
            Text(
                text = "Single-bit AND / OR / XOR streak (4 cells per operator)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            BinaryStreakTable(level = 0, maxOperand = 1, streaks = binaryStreaks)
        }

        Section(LessonId.BinaryOps1) {
            Text(
                text = "3-bit AND / OR / XOR streak (64 cells per operator)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            BinaryStreakTable(level = 1, maxOperand = 7, streaks = binaryStreaks)
        }

        Section(LessonId.CountingSubtraction0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.CountingSubtraction0]} / 8")
            Text(
                text = "Subtraction streak grid (rows = op1 ∈ 4..9, " +
                    "columns = op2 ∈ 0..4). All four subtraction variants " +
                    "share these cells but each keeps its own streak.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            MathStreakGrid(subtractionStreaks)
        }

        Section(LessonId.HorizontalSubtraction0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.HorizontalSubtraction0]} / 8")
        }

        Section(LessonId.VerticalSubtraction0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.VerticalSubtraction0]} / 8")
        }

        Section(LessonId.NumberLineSubtraction0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.NumberLineSubtraction0]} / 8")
        }

        Section(LessonId.CountingMultiplication0) {
            InfoRow("Correct (lifetime)", multiplicationState.correctCount.toString())
            InfoRow("Wrong (lifetime)", multiplicationState.wrongCount.toString())
            Text(
                text = "Streak grid for op1 × op2 (op1, op2 ∈ 0..4)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            MultiplicationStreakGrid(multiplicationStreaks)
        }

        Section(LessonId.Reading0) {
            InfoRow("Correct (lifetime)", readingState.correctCount.toString())
            InfoRow("Wrong (lifetime)", readingState.wrongCount.toString())
            Text(
                text = "Streak per animal",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            ReadingStreakTable(readingStreaks)
        }

        Section(LessonId.SightWords0) {
            InfoRow("Correct (lifetime)", sightWordState.correctCount.toString())
            InfoRow("Wrong (lifetime)", sightWordState.wrongCount.toString())
            Text(
                text = "Streak per letter (rows = word, columns = letter position)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            SightWordsTable(sightWordStreaks)
        }

        Section(LessonId.SightWords1) {
            Text(
                text = "Uses the same per-letter streak table above.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }

        Section(LessonId.RhymingWords0) {
            InfoRow("Correct (lifetime)", rhymingWordState.correctCount.toString())
            InfoRow("Wrong (lifetime)", rhymingWordState.wrongCount.toString())
            Text(
                text = "First-letter streak per rhyming word",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            RhymingWordsTable(rhymingWordStreaks)
        }
    }
}

@Composable
private fun LessonSection(
    def: LessonDefinition,
    passed: Boolean,
    manuallyUnlocked: Boolean,
    /**
     * True when the lesson is available because every parent has been
     * passed (i.e. the natural unlock condition is satisfied). Entry-
     * level lessons with no parents are always naturally unlocked.
     */
    naturallyUnlocked: Boolean,
    onManualUnlockChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val badge = when {
        passed -> "✓ passed"
        manuallyUnlocked && !naturallyUnlocked -> "🔓 manually unlocked"
        naturallyUnlocked -> "available"
        else -> "🔒 locked"
    }
    // Entry-level lessons (no parents) are always available, so the
    // switch can't change anything for them. Disable it to make that
    // visually clear.
    val switchEnabled = def.parents.isNotEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = def.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = badge,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
            Switch(
                checked = manuallyUnlocked,
                onCheckedChange = onManualUnlockChange,
                enabled = switchEnabled,
            )
        }
        HorizontalDivider()
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun MathStreakGrid(streaks: List<List<Int>>) {
    if (streaks.isEmpty()) return
    val rows = streaks.size
    val cols = streaks[0].size
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(modifier = Modifier.heightIn(min = 22.dp).padding(end = 2.dp))
            (0 until cols).forEach { c ->
                HeaderCell(c.toString(), modifier = Modifier.weight(1f))
            }
        }
        (0 until rows).forEach { r ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                HeaderCell(r.toString())
                (0 until cols).forEach { c ->
                    StreakCell(streaks[r][c], modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 18.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun StreakCell(value: Int, modifier: Modifier = Modifier) {
    val bg = when {
        value == 0 -> Color(0xFFEF4444).copy(alpha = 0.35f)
        value == 1 -> Color(0xFFF59E0B).copy(alpha = 0.35f)
        else -> Color(0xFF22C55E).copy(alpha = 0.35f)
    }
    Box(
        modifier = modifier
            .heightIn(min = 18.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value.toString(),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ReadingStreakTable(streaks: Map<Char, Int>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Animals.all.chunked(2).forEach { rowAnimals ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowAnimals.forEach { animal ->
                    val v = streaks[animal.letter] ?: 0
                    val bg = when {
                        v == 0 -> Color(0xFFEF4444).copy(alpha = 0.35f)
                        v == 1 -> Color(0xFFF59E0B).copy(alpha = 0.35f)
                        else -> Color(0xFF22C55E).copy(alpha = 0.35f)
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(bg)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(text = animal.emoji, fontSize = 18.sp)
                        Text(
                            text = "${animal.letter}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "= $v",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
                if (rowAnimals.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SightWordsTable(streaks: Map<String, List<Int>>) {    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SightWords.all.forEach { word ->
            val perLetter = streaks[word] ?: List(word.length) { 0 }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = word,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                word.forEachIndexed { i, ch ->
                    val v = perLetter.getOrNull(i) ?: 0
                    val bg = when {
                        v == 0 -> Color(0xFFEF4444).copy(alpha = 0.35f)
                        v == 1 -> Color(0xFFF59E0B).copy(alpha = 0.35f)
                        else -> Color(0xFF22C55E).copy(alpha = 0.35f)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(bg)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "${ch.uppercaseChar()}:$v",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RhymingWordsTable(streaks: Map<String, Int>) {    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RhymingWords.groups.forEachIndexed { groupIdx, words ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Rhyme family ${groupIdx + 1}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    words.forEach { word ->
                        val v = streaks[word] ?: 0
                        val bg = when {
                            v == 0 -> Color(0xFFEF4444).copy(alpha = 0.35f)
                            v == 1 -> Color(0xFFF59E0B).copy(alpha = 0.35f)
                            else -> Color(0xFF22C55E).copy(alpha = 0.35f)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(bg)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "$word:$v",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterSoundsTable(streaks: Map<Char, Int>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LetterSounds.letters.forEach { letter ->
            val v = streaks[letter.uppercaseChar()] ?: 0
            val bg = when {
                v == 0 -> Color(0xFFEF4444).copy(alpha = 0.35f)
                v == 1 -> Color(0xFFF59E0B).copy(alpha = 0.35f)
                else -> Color(0xFF22C55E).copy(alpha = 0.35f)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(bg)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "${letter.uppercaseChar()}:$v",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
private fun PhonemesTable(streaks: Map<String, Int>) {    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Phonemes.byLetter.entries.sortedBy { it.key }.forEach { (letter, words) ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Letter ${letter.uppercaseChar()}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    words.forEach { word ->
                        val v = streaks[word] ?: 0
                        val bg = when {
                            v == 0 -> Color(0xFFEF4444).copy(alpha = 0.35f)
                            v == 1 -> Color(0xFFF59E0B).copy(alpha = 0.35f)
                            else -> Color(0xFF22C55E).copy(alpha = 0.35f)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(bg)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "$word:$v",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiplicationStreakGrid(streaks: List<List<Int>>) {
    if (streaks.isEmpty()) return
    val maxOperand = 4
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(modifier = Modifier.heightIn(min = 22.dp).padding(end = 2.dp))
            (0..maxOperand).forEach { c ->
                HeaderCell(c.toString(), modifier = Modifier.weight(1f))
            }
        }
        (0..maxOperand).forEach { r ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                HeaderCell(r.toString())
                (0..maxOperand).forEach { c ->
                    StreakCell(
                        streaks.getOrNull(r)?.getOrNull(c) ?: 0,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BinaryStreakTable(
    level: Int,
    maxOperand: Int,
    streaks: List<List<List<List<Int>>>>,
) {
    if (streaks.size <= level) return
    val perLevel = streaks[level]
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BinaryOperator.entries.forEach { op ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${op.verbalName} (${op.symbol})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier.widthIn(min = 28.dp),
                        contentAlignment = Alignment.Center,
                    ) {}
                    (0..maxOperand).forEach { c ->
                        HeaderCell(
                            text = c.toString(2).padStart(if (level == 0) 1 else 3, '0'),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                (0..maxOperand).forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        HeaderCell(
                            text = r.toString(2).padStart(if (level == 0) 1 else 3, '0'),
                            modifier = Modifier.widthIn(min = 28.dp),
                        )
                        (0..maxOperand).forEach { c ->
                            StreakCell(
                                perLevel[op.ordinal][r][c],
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
