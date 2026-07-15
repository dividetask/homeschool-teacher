package com.dividetask.homeschoolteacher.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dividetask.homeschoolteacher.binary.BinaryOperationsScreen
import com.dividetask.homeschoolteacher.binary.BinaryOperationsViewModel
import com.dividetask.homeschoolteacher.chess.ChessScreen
import com.dividetask.homeschoolteacher.multiplication.CountingMultiplicationScreen
import com.dividetask.homeschoolteacher.multiplication.CountingMultiplicationViewModel
import com.dividetask.homeschoolteacher.multiplication.MultiplicationOperandsScreen
import com.dividetask.homeschoolteacher.multiplication.MultiplicationOperandsViewModel
import com.dividetask.homeschoolteacher.chess.ChessViewModel
import com.dividetask.homeschoolteacher.lesson.LessonId
import com.dividetask.homeschoolteacher.lesson.LessonSelector
import com.dividetask.homeschoolteacher.lesson.LessonSelectorFactory
import com.dividetask.homeschoolteacher.lesson.Lessons
import com.dividetask.homeschoolteacher.lesson.SelectionMode
import com.dividetask.homeschoolteacher.math.MathScreen
import com.dividetask.homeschoolteacher.math.MathViewModel
import com.dividetask.homeschoolteacher.reading.LetterSoundsScreen
import com.dividetask.homeschoolteacher.reading.LetterSoundsViewModel
import com.dividetask.homeschoolteacher.reading.PhonemesScreen
import com.dividetask.homeschoolteacher.reading.PhonemesViewModel
import com.dividetask.homeschoolteacher.reading.ReadingScreen
import com.dividetask.homeschoolteacher.reading.ReadingViewModel
import com.dividetask.homeschoolteacher.reading.RhymingWordsScreen
import com.dividetask.homeschoolteacher.reading.RhymingWordsViewModel
import com.dividetask.homeschoolteacher.reading.SightWordsScreen
import com.dividetask.homeschoolteacher.reading.SightWordsViewModel
import com.dividetask.homeschoolteacher.tictactoe.GameScreen
import com.dividetask.homeschoolteacher.tictactoe.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeschoolTeacherApp() {
    val game: GameViewModel = viewModel()
    val chess: ChessViewModel = viewModel()
    val math: MathViewModel = viewModel()
    val binary: BinaryOperationsViewModel = viewModel()
    val multiplication: CountingMultiplicationViewModel = viewModel()
    val multiplicationOperands: MultiplicationOperandsViewModel = viewModel()
    val letterSounds: LetterSoundsViewModel = viewModel()
    val phonemes: PhonemesViewModel = viewModel()
    val reading: ReadingViewModel = viewModel()
    val sightWords: SightWordsViewModel = viewModel()
    val rhymingWords: RhymingWordsViewModel = viewModel()
    val selector: LessonSelector = viewModel(
        factory = LessonSelectorFactory(game, chess, math, binary, multiplication, multiplicationOperands, letterSounds, phonemes, reading, sightWords, rhymingWords),
    )

    val mode by selector.mode.collectAsStateWithLifecycle()
    val current by selector.currentLesson.collectAsStateWithLifecycle()
    val passed by selector.passedMap.collectAsStateWithLifecycle()
    val manualUnlock by selector.manualUnlockMap.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val topBarTitle = when (mode) {
        SelectionMode.Progress -> "Progress"
        else -> Lessons.get(current).title
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Swipe-to-open is too easy for kids to trigger accidentally. Allow
        // gestures only when the drawer is already open (so a swipe can
        // close it). The 5-second hold below is the only way to open it.
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                // The lesson list is taller than most screens; without this
                // the items below the fold are simply unreachable.
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Homeschool Teacher",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    NavigationDrawerItem(
                        label = { Text("Random / Mixed") },
                        selected = mode == SelectionMode.Random,
                        onClick = {
                            selector.selectRandom()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Lessons",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                    Lessons.all.forEach { def ->
                        val isUnlocked = manualUnlock[def.id] == true ||
                            def.parents.all { passed[it] == true }
                        val passedLabel = if (passed[def.id] == true) "  ✓" else ""
                        val lockedLabel = if (isUnlocked) "" else "  🔒"
                        NavigationDrawerItem(
                            label = { Text(def.title + passedLabel + lockedLabel) },
                            selected = mode == SelectionMode.SingleLesson && current == def.id,
                            onClick = {
                                if (isUnlocked) {
                                    selector.selectLesson(def.id)
                                    scope.launch { drawerState.close() }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        label = { Text("Progress") },
                        selected = mode == SelectionMode.Progress,
                        onClick = {
                            selector.selectProgress()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(topBarTitle) },
                    navigationIcon = {
                        HoldToOpenMenu(
                            holdMillis = 500L,
                            onTriggered = { scope.launch { drawerState.open() } },
                        )
                    },
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (mode == SelectionMode.Progress) {
                    ProgressScreen(
                        game = game,
                        math = math,
                        binary = binary,
                        multiplication = multiplication,
                        multiplicationOperands = multiplicationOperands,
                        letterSounds = letterSounds,
                        phonemes = phonemes,
                        reading = reading,
                        sightWords = sightWords,
                        rhymingWords = rhymingWords,
                        chess = chess,
                        passedMap = passed,
                        manualUnlockMap = manualUnlock,
                        onToggleManualUnlock = { id, value -> selector.setManualUnlock(id, value) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    when (current) {
                        LessonId.TicTacToe0,
                        LessonId.TicTacToe1,
                        LessonId.TicTacToe2 -> GameScreen(
                            viewModel = game,
                            onCompleted = selector::onLessonInstanceCompleted,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LessonId.Chess0,
                        LessonId.Chess1,
                        LessonId.Chess2,
                        LessonId.Chess3 -> ChessScreen(
                            viewModel = chess,
                            onCompleted = selector::onLessonInstanceCompleted,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LessonId.MathPictures,
                        LessonId.Math0,
                        LessonId.HorizontalAddition0,
                        LessonId.NumberLineAddition0,
                        LessonId.CountingAddition1,
                        LessonId.Math1,
                        LessonId.HorizontalAddition1,
                        LessonId.MathNumberLine,
                        LessonId.CountingSubtraction0,
                        LessonId.HorizontalSubtraction0,
                        LessonId.VerticalSubtraction0,
                        LessonId.NumberLineSubtraction0,
                        LessonId.HorizontalMultiplication0,
                        LessonId.VerticalMultiplication0,
                        LessonId.NumberLineMultiplication0 -> MathScreen(
                            viewModel = math,
                            onCompleted = selector::onLessonInstanceCompleted,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LessonId.BinaryOps0,
                        LessonId.BinaryOps1 -> BinaryOperationsScreen(
                            viewModel = binary,
                            onCompleted = selector::onLessonInstanceCompleted,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LessonId.CountingMultiplication0 -> CountingMultiplicationScreen(
                            viewModel = multiplication,
                            onCompleted = selector::onLessonInstanceCompleted,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LessonId.CountingMultiplication1 -> MultiplicationOperandsScreen(
                            viewModel = multiplicationOperands,
                            onCompleted = selector::onLessonInstanceCompleted,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LessonId.LetterSounds0 -> LetterSoundsScreen(
                            viewModel = letterSounds,
                            onCompleted = selector::onLessonInstanceCompleted,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LessonId.Phonemes0 -> PhonemesScreen(
                            viewModel = phonemes,
                            onCompleted = selector::onLessonInstanceCompleted,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LessonId.Reading0 -> ReadingScreen(
                            viewModel = reading,
                            onCompleted = selector::onLessonInstanceCompleted,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LessonId.SightWords0,
                        LessonId.SightWords1 -> SightWordsScreen(
                            viewModel = sightWords,
                            onCompleted = selector::onLessonInstanceCompleted,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LessonId.RhymingWords0 -> RhymingWordsScreen(
                            viewModel = rhymingWords,
                            onCompleted = selector::onLessonInstanceCompleted,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Hamburger icon that opens the menu only after being held for `holdMillis`.
 * A circular progress ring fills up around the icon while the press is in
 * progress so the gesture is visibly registering. Releasing before the
 * timeout cancels without opening the menu.
 */
@Composable
private fun HoldToOpenMenu(
    holdMillis: Long,
    onTriggered: () -> Unit,
) {
    var pressing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(pressing) {
        if (pressing) {
            val start = System.currentTimeMillis()
            while (pressing) {
                val elapsed = System.currentTimeMillis() - start
                progress = (elapsed.toFloat() / holdMillis.toFloat()).coerceAtMost(1f)
                if (elapsed >= holdMillis) {
                    onTriggered()
                    pressing = false
                    break
                }
                delay(50)
            }
            progress = 0f
        } else {
            progress = 0f
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressing = true
                    waitForUpOrCancellation()
                    pressing = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (progress > 0f) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(44.dp),
                strokeWidth = 3.dp,
            )
        }
        Icon(
            imageVector = Icons.Filled.Menu,
            contentDescription = "Hold for half a second to open menu",
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
