package com.dividetask.homeschoolteacher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.dividetask.homeschoolteacher.division.CountingDivisionViewModel
import com.dividetask.homeschoolteacher.division.MAX_DIVISOR
import com.dividetask.homeschoolteacher.division.divisionCells
import com.dividetask.homeschoolteacher.multiplication.CountingMultiplicationViewModel
import com.dividetask.homeschoolteacher.multiplication.MultiplicationOperandsViewModel
import com.dividetask.homeschoolteacher.practice.GridOperation
import com.dividetask.homeschoolteacher.practice.PracticeGrid
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
import com.dividetask.homeschoolteacher.reading.PositionWords
import com.dividetask.homeschoolteacher.reading.PositionWordsViewModel
import com.dividetask.homeschoolteacher.reading.RhymingWords
import com.dividetask.homeschoolteacher.reading.RhymingWordsViewModel
import com.dividetask.homeschoolteacher.reading.SightWords
import com.dividetask.homeschoolteacher.reading.SightWordsViewModel
import com.dividetask.homeschoolteacher.tictactoe.GameViewModel
import com.dividetask.homeschoolteacher.tictactoe.TttPuzzleViewModel

@Composable
fun ProgressScreen(
    game: GameViewModel,
    tttPuzzle: TttPuzzleViewModel,
    chess: ChessViewModel,
    math: MathViewModel,
    binary: BinaryOperationsViewModel,
    multiplication: CountingMultiplicationViewModel,
    multiplicationOperands: MultiplicationOperandsViewModel,
    division: CountingDivisionViewModel,
    letterSounds: LetterSoundsViewModel,
    phonemes: PhonemesViewModel,
    reading: ReadingViewModel,
    sightWords: SightWordsViewModel,
    rhymingWords: RhymingWordsViewModel,
    positionWords: PositionWordsViewModel,
    passedMap: Map<LessonId, Boolean>,
    manualUnlockMap: Map<LessonId, Boolean>,
    onToggleManualUnlock: (LessonId, Boolean) -> Unit,
    onSetPassed: (LessonId, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tttGame by game.state.collectAsStateWithLifecycle()
    val ttt0Streak by game.streak(LessonId.TicTacToe0).collectAsStateWithLifecycle()
    val ttt1Streak by game.streak(LessonId.TicTacToe1).collectAsStateWithLifecycle()
    val ttt2Streak by game.streak(LessonId.TicTacToe2).collectAsStateWithLifecycle()
    val puzzleState by tttPuzzle.state.collectAsStateWithLifecycle()

    val mathStreaks by math.streaks.collectAsStateWithLifecycle()
    val mathState by math.state.collectAsStateWithLifecycle()

    val readingStreaks by reading.streaks.collectAsStateWithLifecycle()
    val readingState by reading.state.collectAsStateWithLifecycle()

    val sightWordStreaks by sightWords.streaks.collectAsStateWithLifecycle()
    val sightWordState by sightWords.state.collectAsStateWithLifecycle()

    val rhymingWordStreaks by rhymingWords.streaks.collectAsStateWithLifecycle()
    val rhymingFamilyStreaks by rhymingWords.familyStreaksFlow.collectAsStateWithLifecycle()
    val rhymingWordState by rhymingWords.state.collectAsStateWithLifecycle()
    val positionStreaks by positionWords.streaks.collectAsStateWithLifecycle()
    val positionState by positionWords.state.collectAsStateWithLifecycle()

    val phonemeStreaks by phonemes.streaks.collectAsStateWithLifecycle()
    val phonemeState by phonemes.state.collectAsStateWithLifecycle()

    val letterSoundStreaks by letterSounds.streaks.collectAsStateWithLifecycle()
    val letterSoundState by letterSounds.state.collectAsStateWithLifecycle()

    val binaryStreaks by binary.streaksSnapshot.collectAsStateWithLifecycle()
    val binaryState by binary.state.collectAsStateWithLifecycle()

    val multiplicationStreaks by multiplication.streaks.collectAsStateWithLifecycle()
    val multiplicationState by multiplication.state.collectAsStateWithLifecycle()

    val operandsStreaks by multiplicationOperands.streaks.collectAsStateWithLifecycle()
    val operandsState by multiplicationOperands.state.collectAsStateWithLifecycle()

    val divisionStreaks by division.streaks.collectAsStateWithLifecycle()
    val divisionState by division.state.collectAsStateWithLifecycle()

    val subtractionStreaks by math.subtractionGrid.collectAsStateWithLifecycle()
    val multEquationStreaks by math.multiplicationGrid.collectAsStateWithLifecycle()

    // Per-lesson math streak (cells AND streak >= 4 are both required to
    // pass — the user calls this out explicitly).
    val mathLessonStreaks: Map<LessonId, Int> = listOf(
        LessonId.MathPictures,
        LessonId.Math0, LessonId.HorizontalAddition0, LessonId.NumberLineAddition0,
        LessonId.CountingAddition1, LessonId.Math1, LessonId.HorizontalAddition1,
        LessonId.MathNumberLine,
        LessonId.CountingSubtraction0, LessonId.HorizontalSubtraction0,
        LessonId.VerticalSubtraction0, LessonId.NumberLineSubtraction0,
        LessonId.CountingSubtraction1,
        LessonId.HorizontalMultiplication0, LessonId.VerticalMultiplication0,
        LessonId.NumberLineMultiplication0,
        LessonId.HorizontalMultiplication1, LessonId.VerticalMultiplication1,
        LessonId.NumberLineMultiplication1,
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
            text = "Each lesson has two switches. Unlocked makes the lesson " +
                "appear in the menu and Random rotation right away, even if " +
                "its prerequisites haven't been passed yet. Passed marks the " +
                "lesson complete by hand — use it to skip a lesson the learner " +
                "already knows — which unlocks whatever it gates. Turning " +
                "Passed off clears the completion and lets the lesson be " +
                "earned normally again.",
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
                onPassedChange = { onSetPassed(id, it) },
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

        Section(LessonId.TicTacToeWinBlock) {
            InfoRow("Correct streak", "${puzzleState.streak} / 8")
            InfoRow("Correct (lifetime)", puzzleState.correctCount.toString())
            InfoRow("Wrong (lifetime)", puzzleState.wrongCount.toString())
            Text(
                text = "Single-move puzzle: take the winning move or block " +
                    "the opponent's. Any other move is a loss.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
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
                text = "Passing takes 8 correct in a row OR every letter " +
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
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.MathPictures]} / 4")
            Text(
                text = "Uses the 1..4 corner of the addition streak grid.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }

        Section(LessonId.Math0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.Math0]} / 4")
            InfoRow("Correct (lifetime)", mathState.correctCount.toString())
            InfoRow("Wrong (lifetime)", mathState.wrongCount.toString())
            Text(
                text = "Addition streak grid (rows = left, columns = right). " +
                    "Every addition variant writes the same cells, but each " +
                    "variant also keeps its own consecutive-correct streak " +
                    "above — both have to land before a variant passes." +
                    EASY_CELL_NOTE,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            MathStreakGrid(mathStreaks, GridOperation.Add, rows = 8, cols = 8)
        }

        Section(LessonId.HorizontalAddition0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.HorizontalAddition0]} / 4")
        }

        Section(LessonId.NumberLineAddition0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.NumberLineAddition0]} / 4")
        }

        Section(LessonId.CountingAddition1) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.CountingAddition1]} / 4")
        }

        Section(LessonId.Math1) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.Math1]} / 4")
        }

        Section(LessonId.HorizontalAddition1) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.HorizontalAddition1]} / 4")
        }

        Section(LessonId.MathNumberLine) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.MathNumberLine]} / 4")
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
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.CountingSubtraction0]} / 4")
            Text(
                text = "Subtraction streak grid (rows = op1 ∈ 4..9, " +
                    "columns = op2 ∈ 0..4). All four subtraction variants " +
                    "share these cells but each keeps its own streak." +
                    EASY_CELL_NOTE,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            MathStreakGrid(subtractionStreaks, GridOperation.Subtract, rows = 16, cols = 8)
        }

        Section(LessonId.HorizontalSubtraction0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.HorizontalSubtraction0]} / 4")
        }

        Section(LessonId.VerticalSubtraction0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.VerticalSubtraction0]} / 4")
        }

        Section(LessonId.NumberLineSubtraction0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.NumberLineSubtraction0]} / 4")
        }

        Section(LessonId.CountingSubtraction1) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.CountingSubtraction1]} / 4")
            Text(
                text = "Level 1: op1 ∈ 8..16, op2 ∈ 0..8 — the inverse of " +
                    "Addition Level 1, whose sums land in that same range. " +
                    "Shares the subtraction grid above (its 8..16 slice)." +
                    EASY_CELL_NOTE,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }

        Section(LessonId.CountingMultiplication0) {
            InfoRow("Correct (lifetime)", multiplicationState.correctCount.toString())
            InfoRow("Wrong (lifetime)", multiplicationState.wrongCount.toString())
            Text(
                text = "Streak grid for op1 × op2 (op1, op2 ∈ 0..4)." +
                    EASY_CELL_NOTE,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            MultiplicationStreakGrid(multiplicationStreaks)
        }

        Section(LessonId.MultiplicationOperands0) {
            InfoRow("Correct (lifetime)", operandsState.correctCount.toString())
            InfoRow("Wrong (lifetime)", operandsState.wrongCount.toString())
            Text(
                text = "Identify the operands — grid for op1 × op2 " +
                    "(op1, op2 ∈ 1..4)." + EASY_CELL_NOTE,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            MultiplicationStreakGrid(operandsStreaks, minOperand = 1)
        }

        Section(LessonId.HorizontalMultiplication0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.HorizontalMultiplication0]} / 4")
            Text(
                text = "Multiplication (product) grid, op1 × op2 ∈ 0..4. " +
                    "Shared by the three symbolic multiplication screens; " +
                    "each also keeps its own streak." + EASY_CELL_NOTE,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            MultiplicationStreakGrid(multEquationStreaks)
        }

        Section(LessonId.VerticalMultiplication0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.VerticalMultiplication0]} / 4")
        }

        Section(LessonId.NumberLineMultiplication0) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.NumberLineMultiplication0]} / 4")
        }

        Section(LessonId.HorizontalMultiplication1) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.HorizontalMultiplication1]} / 4")
            Text(
                text = "Level 1: operands 0..9 (products to 81), answer typed " +
                    "on the number pad. Shares the multiplication grid above " +
                    "(0..9 slice); each Level 1 screen keeps its own streak.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }

        Section(LessonId.VerticalMultiplication1) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.VerticalMultiplication1]} / 4")
        }

        Section(LessonId.NumberLineMultiplication1) {
            InfoRow("Correct streak", "${mathLessonStreaks[LessonId.NumberLineMultiplication1]} / 4")
        }

        Section(LessonId.CountingDivision0) {
            InfoRow("Correct (lifetime)", divisionState.correctCount.toString())
            InfoRow("Wrong (lifetime)", divisionState.wrongCount.toString())
            InfoRow(
                "Correct streak",
                "${division.lessonStreak(LessonId.CountingDivision0).value} / 4",
            )
            Text(
                text = "Streak grid for dividend ÷ divisor. Only the cells " +
                    "that divide evenly are ever asked, so the blanks are " +
                    "expected — mastery is judged over the marked cells only." +
                    EASY_CELL_NOTE,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            DivisionStreakGrid(divisionStreaks.getOrNull(0).orEmpty(), level = 0)
        }

        Section(LessonId.CountingDivision1) {
            InfoRow(
                "Correct streak",
                "${division.lessonStreak(LessonId.CountingDivision1).value} / 4",
            )
            Text(
                text = "Same problems as Level 0, but the screen always puts " +
                    "out eight pens instead of one per group, so the sharing no " +
                    "longer hands over the answer. Keeps its own coverage — " +
                    "Level 0's does not count towards it.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            DivisionStreakGrid(divisionStreaks.getOrNull(1).orEmpty(), level = 1)
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
                text = "Pick-the-rhyme streak per target word",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            RhymingWordsTable(rhymingWordStreaks)
        }

        Section(LessonId.RhymingWords1) {
            Text(
                text = "Odd-one-out streak per rhyme family (a family is " +
                    "mastered at 2).",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            RhymeFamilyTable(rhymingFamilyStreaks)
        }

        Section(LessonId.PositionWords0) {
            InfoRow("Correct (lifetime)", positionState.correctCount.toString())
            InfoRow("Wrong (lifetime)", positionState.wrongCount.toString())
            Text(
                text = "Streak per animal (name the animal in the scene)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            PositionStreakChips(PositionWords.animalWords, positionStreaks)
        }

        Section(LessonId.PositionWords1) {
            Text(
                text = "Streak per position word (on / in / over / under)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            PositionStreakChips(PositionWords.prepositions, positionStreaks)
        }

        Section(LessonId.PositionWords2) {
            Text(
                text = "Streak per object (name the object in the scene)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            PositionStreakChips(PositionWords.objectWords, positionStreaks)
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
    onPassedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val badge = when {
        passed -> "✓ passed"
        manuallyUnlocked && !naturallyUnlocked -> "🔓 manually unlocked"
        naturallyUnlocked -> "available"
        else -> "🔒 locked"
    }
    // Entry-level lessons (no parents) are always available, so the
    // unlock switch can't change anything for them. Disable it to make
    // that visually clear. Passed can be toggled on any lesson.
    val unlockEnabled = def.parents.isNotEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
            LabeledSwitch(
                label = "Unlocked",
                checked = manuallyUnlocked,
                enabled = unlockEnabled,
                onCheckedChange = onManualUnlockChange,
            )
            LabeledSwitch(
                label = "Passed",
                checked = passed,
                enabled = true,
                onCheckedChange = onPassedChange,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        HorizontalDivider()
        content()
    }
}

@Composable
private fun LabeledSwitch(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
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

/**
 * Appended to every coverage-grid caption: the easy cells (a zero
 * operand, multiplying or dividing by one) are damped both in how much
 * they have to be answered and in how often they come up.
 */
private const val EASY_CELL_NOTE =
    " Easy cells — the ones a learner gets right on sight — go green " +
        "after one correct answer instead of two, and come up half as often."

/**
 * Coverage for one arithmetic grid. [rows] and [cols] bound it to the
 * cells the lessons can actually reach — the stored array is wider than
 * any lesson needs, and drawing all of it would be a wall of zeros.
 */
@Composable
private fun MathStreakGrid(
    streaks: List<List<Int>>,
    operation: GridOperation,
    rows: Int,
    cols: Int,
) {
    if (streaks.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(modifier = Modifier.heightIn(min = 22.dp).padding(end = 2.dp))
            (0..cols).forEach { c ->
                HeaderCell(c.toString(), modifier = Modifier.weight(1f))
            }
        }
        (0..rows).forEach { r ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                HeaderCell(r.toString())
                (0..cols).forEach { c ->
                    StreakCell(
                        streaks.getOrNull(r)?.getOrNull(c) ?: 0,
                        modifier = Modifier.weight(1f),
                        target = PracticeGrid.target(operation, r, c),
                    )
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

/**
 * One coverage cell. Green once it has reached [target] — which is 1 for
 * the easy cells (a zero operand, multiplying by one, dividing by one)
 * and 2 for the rest — amber part-way there, red at zero.
 */
@Composable
private fun StreakCell(
    value: Int,
    modifier: Modifier = Modifier,
    target: Int = PracticeGrid.CELL_TARGET,
) {
    val bg = when {
        value >= target -> Color(0xFF22C55E).copy(alpha = 0.35f)
        value == 0 -> Color(0xFFEF4444).copy(alpha = 0.35f)
        else -> Color(0xFFF59E0B).copy(alpha = 0.35f)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PositionStreakChips(items: List<String>, streaks: Map<String, Int>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { word ->
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

@Composable
private fun RhymeFamilyTable(streaks: Map<Int, Int>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RhymingWords.groups.forEachIndexed { groupIdx, words ->
            val v = streaks[groupIdx] ?: 0
            val bg = when {
                v == 0 -> Color(0xFFEF4444).copy(alpha = 0.35f)
                v == 1 -> Color(0xFFF59E0B).copy(alpha = 0.35f)
                else -> Color(0xFF22C55E).copy(alpha = 0.35f)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(bg)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "fam ${groupIdx + 1}: $v",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = words.joinToString(", "),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LetterSoundsTable(streaks: Map<Char, Int>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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

/**
 * Coverage for dividend ÷ divisor at one level. Rows are the dividends
 * that level can ask, columns are divisors, and any pair the level never
 * asks is drawn as a blank.
 */
@Composable
private fun DivisionStreakGrid(streaks: List<List<Int>>, level: Int) {
    if (streaks.isEmpty()) return
    val askable = divisionCells(level).toSet()
    val dividends = askable.map { it.first }.distinct().sorted()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(modifier = Modifier.heightIn(min = 22.dp).padding(end = 2.dp))
            (1..MAX_DIVISOR).forEach { c ->
                HeaderCell(c.toString(), modifier = Modifier.weight(1f))
            }
        }
        dividends.forEach { dividend ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                HeaderCell(dividend.toString())
                (1..MAX_DIVISOR).forEach { divisor ->
                    if (dividend to divisor in askable) {
                        StreakCell(
                            streaks.getOrNull(dividend)?.getOrNull(divisor) ?: 0,
                            modifier = Modifier.weight(1f),
                            target = PracticeGrid.target(
                                GridOperation.Divide, dividend, divisor,
                            ),
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f).heightIn(min = 22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiplicationStreakGrid(streaks: List<List<Int>>, minOperand: Int = 0) {
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
            (minOperand..maxOperand).forEach { c ->
                HeaderCell(c.toString(), modifier = Modifier.weight(1f))
            }
        }
        (minOperand..maxOperand).forEach { r ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                HeaderCell(r.toString())
                (minOperand..maxOperand).forEach { c ->
                    StreakCell(
                        streaks.getOrNull(r)?.getOrNull(c) ?: 0,
                        modifier = Modifier.weight(1f),
                        target = PracticeGrid.target(GridOperation.Multiply, r, c),
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
