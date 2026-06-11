package com.dividetask.homeschoolteacher

import android.content.Context
import android.content.SharedPreferences
import com.dividetask.homeschoolteacher.lesson.LessonId

object Storage {
    private const val PREFS_NAME = "homeschool_teacher"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            migrateIfNeeded()
        }
    }

    private fun prefs(): SharedPreferences =
        prefs ?: throw IllegalStateException("Storage.init() not called")

    // --- Per-lesson passed flag ---
    fun loadLessonPassed(id: LessonId): Boolean =
        prefs().getBoolean("lesson.${id.name}.passed", false)

    fun saveLessonPassed(id: LessonId, value: Boolean) {
        prefs().edit().putBoolean("lesson.${id.name}.passed", value).apply()
    }

    /**
     * Whether the learner (or a grown-up) has manually toggled this lesson's
     * passed flag. When true, automatic criteria evaluation no longer touches
     * the flag — the manual value sticks until manually changed again.
     */
    fun loadLessonManualOverride(id: LessonId): Boolean =
        prefs().getBoolean("lesson.${id.name}.manualOverride", false)

    fun saveLessonManualOverride(id: LessonId, value: Boolean) {
        prefs().edit().putBoolean("lesson.${id.name}.manualOverride", value).apply()
    }

    /**
     * Whether this lesson is *manually unlocked* — i.e. it should appear in
     * the menu and Random rotation regardless of whether its parents have
     * been passed. Independent of [loadLessonPassed]: a manually-unlocked
     * lesson is available to play but is not marked as completed.
     */
    fun loadLessonManualUnlock(id: LessonId): Boolean =
        prefs().getBoolean("lesson.${id.name}.manualUnlock", false)

    fun saveLessonManualUnlock(id: LessonId, value: Boolean) {
        prefs().edit().putBoolean("lesson.${id.name}.manualUnlock", value).apply()
    }

    // --- Math (Addition) ---
    fun loadMathStreaks(): Array<IntArray> {
        val p = prefs()
        val out = Array(16) { IntArray(16) }
        for (a in 0..15) for (b in 0..15) {
            out[a][b] = p.getInt("math.streak.$a.$b", 0)
        }
        return out
    }

    fun saveMathStreak(a: Int, b: Int, value: Int) {
        prefs().edit().putInt("math.streak.$a.$b", value).apply()
    }

    fun loadMathCounts(): Pair<Int, Int> {
        val p = prefs()
        return p.getInt("math.correct", 0) to p.getInt("math.wrong", 0)
    }

    fun saveMathCounts(correct: Int, wrong: Int) {
        prefs().edit()
            .putInt("math.correct", correct)
            .putInt("math.wrong", wrong)
            .apply()
    }

    /**
     * Per-lesson consecutive-correct streak for math lessons. Each
     * addition / subtraction variant tracks its own — passing requires
     * 8 consecutive correct answers *in that variant* on top of the
     * shared cell coverage.
     */
    fun loadMathLessonStreak(id: LessonId): Int =
        prefs().getInt("math.lessonstreak.${id.name}", 0)

    fun saveMathLessonStreak(id: LessonId, value: Int) {
        prefs().edit().putInt("math.lessonstreak.${id.name}", value).apply()
    }

    // --- Math (Subtraction) ---
    fun loadSubtractionStreaks(): Array<IntArray> {
        val p = prefs()
        val out = Array(16) { IntArray(16) }
        for (a in 0..15) for (b in 0..15) {
            out[a][b] = p.getInt("subtraction.streak.$a.$b", 0)
        }
        return out
    }

    fun saveSubtractionStreak(a: Int, b: Int, value: Int) {
        prefs().edit().putInt("subtraction.streak.$a.$b", value).apply()
    }

    // --- Tic Tac Toe (per-lesson) ---
    fun loadTttStreak(id: LessonId): Int =
        prefs().getInt("ttt.streak.${id.name}", 0)

    fun saveTttStreak(id: LessonId, value: Int) {
        prefs().edit().putInt("ttt.streak.${id.name}", value).apply()
    }

    fun loadTttScores(): Triple<Int, Int, Int> {
        val p = prefs()
        return Triple(
            p.getInt("ttt.playerScore", 0),
            p.getInt("ttt.cpuScore", 0),
            p.getInt("ttt.drawScore", 0),
        )
    }

    fun saveTttScores(player: Int, cpu: Int, draw: Int) {
        prefs().edit()
            .putInt("ttt.playerScore", player)
            .putInt("ttt.cpuScore", cpu)
            .putInt("ttt.drawScore", draw)
            .apply()
    }

    // --- Reading ---
    fun loadReadingStreak(letter: Char): Int = prefs().getInt("reading.streak.$letter", 0)
    fun saveReadingStreak(letter: Char, value: Int) {
        prefs().edit().putInt("reading.streak.$letter", value).apply()
    }

    fun loadReadingCounts(): Pair<Int, Int> {
        val p = prefs()
        return p.getInt("reading.correct", 0) to p.getInt("reading.wrong", 0)
    }

    fun saveReadingCounts(correct: Int, wrong: Int) {
        prefs().edit()
            .putInt("reading.correct", correct)
            .putInt("reading.wrong", wrong)
            .apply()
    }

    // --- Chess (per-lesson) ---
    fun loadChessStreak(id: LessonId): Int = prefs().getInt("chess.streak.${id.name}", 0)
    fun saveChessStreak(id: LessonId, value: Int) {
        prefs().edit().putInt("chess.streak.${id.name}", value).apply()
    }

    fun loadChessCounts(): Pair<Int, Int> {
        val p = prefs()
        return p.getInt("chess.correct", 0) to p.getInt("chess.wrong", 0)
    }

    fun saveChessCounts(correct: Int, wrong: Int) {
        prefs().edit()
            .putInt("chess.correct", correct)
            .putInt("chess.wrong", wrong)
            .apply()
    }

    // --- Sight Words ---
    // Per-letter streak: one slot per (word, position). Level 0 only writes
    // position 0; Level 1 writes any position. Shared between the lessons.
    fun loadSightWordStreak(word: String, position: Int): Int =
        prefs().getInt("sightwords.streak.$word.$position", 0)

    fun saveSightWordStreak(word: String, position: Int, value: Int) {
        prefs().edit().putInt("sightwords.streak.$word.$position", value).apply()
    }

    fun loadSightWordsCounts(): Pair<Int, Int> {
        val p = prefs()
        return p.getInt("sightwords.correct", 0) to p.getInt("sightwords.wrong", 0)
    }

    fun saveSightWordsCounts(correct: Int, wrong: Int) {
        prefs().edit()
            .putInt("sightwords.correct", correct)
            .putInt("sightwords.wrong", wrong)
            .apply()
    }

    // --- Rhyming Words ---
    // Per-word streak (only the first letter is ever blanked).
    fun loadRhymingWordStreak(word: String): Int =
        prefs().getInt("rhymingwords.streak.$word", 0)

    fun saveRhymingWordStreak(word: String, value: Int) {
        prefs().edit().putInt("rhymingwords.streak.$word", value).apply()
    }

    fun loadRhymingWordsCounts(): Pair<Int, Int> {
        val p = prefs()
        return p.getInt("rhymingwords.correct", 0) to p.getInt("rhymingwords.wrong", 0)
    }

    fun saveRhymingWordsCounts(correct: Int, wrong: Int) {
        prefs().edit()
            .putInt("rhymingwords.correct", correct)
            .putInt("rhymingwords.wrong", wrong)
            .apply()
    }

    // --- Letter Sounds ---
    // Per-letter consecutive-correct streak, keyed by uppercase letter.
    fun loadLetterSoundStreak(letter: Char): Int =
        prefs().getInt("lettersounds.streak.$letter", 0)

    fun saveLetterSoundStreak(letter: Char, value: Int) {
        prefs().edit().putInt("lettersounds.streak.$letter", value).apply()
    }

    // Global consecutive-correct run across all letters (the "8 in a row"
    // half of the pass criteria).
    fun loadLetterSoundsRunStreak(): Int =
        prefs().getInt("lettersounds.runstreak", 0)

    fun saveLetterSoundsRunStreak(value: Int) {
        prefs().edit().putInt("lettersounds.runstreak", value).apply()
    }

    fun loadLetterSoundsCounts(): Pair<Int, Int> {
        val p = prefs()
        return p.getInt("lettersounds.correct", 0) to p.getInt("lettersounds.wrong", 0)
    }

    fun saveLetterSoundsCounts(correct: Int, wrong: Int) {
        prefs().edit()
            .putInt("lettersounds.correct", correct)
            .putInt("lettersounds.wrong", wrong)
            .apply()
    }

    // --- Phonemes ---
    fun loadPhonemeWordStreak(word: String): Int =
        prefs().getInt("phonemes.streak.$word", 0)

    fun savePhonemeWordStreak(word: String, value: Int) {
        prefs().edit().putInt("phonemes.streak.$word", value).apply()
    }

    fun loadPhonemesCounts(): Pair<Int, Int> {
        val p = prefs()
        return p.getInt("phonemes.correct", 0) to p.getInt("phonemes.wrong", 0)
    }

    fun savePhonemesCounts(correct: Int, wrong: Int) {
        prefs().edit()
            .putInt("phonemes.correct", correct)
            .putInt("phonemes.wrong", wrong)
            .apply()
    }

    // --- Counting Multiplication ---
    fun loadMultiplicationStreak(op1: Int, op2: Int): Int =
        prefs().getInt("multiplication.streak.$op1.$op2", 0)

    fun saveMultiplicationStreak(op1: Int, op2: Int, value: Int) {
        prefs().edit().putInt("multiplication.streak.$op1.$op2", value).apply()
    }

    fun loadMultiplicationCounts(): Pair<Int, Int> {
        val p = prefs()
        return p.getInt("multiplication.correct", 0) to p.getInt("multiplication.wrong", 0)
    }

    fun saveMultiplicationCounts(correct: Int, wrong: Int) {
        prefs().edit()
            .putInt("multiplication.correct", correct)
            .putInt("multiplication.wrong", wrong)
            .apply()
    }

    // --- Binary Operations ---
    // streak[level][operator][op1][op2] -- level 0 only uses op1,op2 ∈ 0..1.
    fun loadBinaryStreak(level: Int, operator: Int, op1: Int, op2: Int): Int =
        prefs().getInt("binary.streak.$level.$operator.$op1.$op2", 0)

    fun saveBinaryStreak(level: Int, operator: Int, op1: Int, op2: Int, value: Int) {
        prefs().edit()
            .putInt("binary.streak.$level.$operator.$op1.$op2", value)
            .apply()
    }

    fun loadBinaryCounts(): Pair<Int, Int> {
        val p = prefs()
        return p.getInt("binary.correct", 0) to p.getInt("binary.wrong", 0)
    }

    fun saveBinaryCounts(correct: Int, wrong: Int) {
        prefs().edit()
            .putInt("binary.correct", correct)
            .putInt("binary.wrong", wrong)
            .apply()
    }

    // --- Migration from earlier schemas ---
    private fun migrateIfNeeded() {
        val p = prefs()

        if (!p.contains("migration.v2")) {
            val editor = p.edit()
            val oldStreak = p.getInt("ttt.nonLossStreak", 0)
            val oldLevel = p.getInt("ttt.level", 0).coerceIn(0, 2)

            // Stamp passed-lesson flags from the legacy single-level counter.
            if (oldLevel >= 1) editor.putBoolean("lesson.TicTacToe0.passed", true)
            if (oldLevel >= 2) editor.putBoolean("lesson.TicTacToe1.passed", true)

            // Migrate the in-progress streak into the highest level that is not
            // yet passed (i.e. the level the player was working on).
            when {
                oldLevel == 0 -> editor.putInt("ttt.streak.TicTacToe0", oldStreak)
                oldLevel == 1 -> editor.putInt("ttt.streak.TicTacToe1", oldStreak)
                else -> { /* both passed, streak is irrelevant */ }
            }

            // Derive math passed flags from the existing streak grid.
            val all5 = (0..4).all { a -> (0..4).all { b -> p.getInt("math.streak.$a.$b", 0) >= 2 } }
            val all10 = (0..9).all { a -> (0..9).all { b -> p.getInt("math.streak.$a.$b", 0) >= 2 } }
            if (all5) editor.putBoolean("lesson.Math0.passed", true)
            if (all10) editor.putBoolean("lesson.Math1.passed", true)

            editor.putBoolean("migration.v2", true)
            editor.apply()
        }

        if (!p.contains("migration.v3")) {
            // Phonemes — Level 0 was inserted ahead of Animals. Existing
            // users with any Animals progress would otherwise lose access
            // to Animals until they passed Phonemes 0. Auto-pass Phonemes
            // 0 for them so the chain remains unlocked.
            val editor = p.edit()
            val readingPassed = p.getBoolean("lesson.Reading0.passed", false)
            val readingCorrect = p.getInt("reading.correct", 0)
            if (readingPassed || readingCorrect > 0) {
                editor.putBoolean("lesson.Phonemes0.passed", true)
            }
            editor.putBoolean("migration.v3", true)
            editor.apply()
        }

        if (!p.contains("migration.v4")) {
            // Letter Sounds — Level 0 was inserted ahead of Phonemes as the
            // new head of the Reading chain. Existing users who already
            // reached Phonemes (or beyond) would otherwise have the whole
            // chain re-locked behind a brand-new lesson. Auto-pass Letter
            // Sounds for them so their progress stays unlocked.
            val editor = p.edit()
            val phonemesPassed = p.getBoolean("lesson.Phonemes0.passed", false)
            val phonemesCorrect = p.getInt("phonemes.correct", 0)
            if (phonemesPassed || phonemesCorrect > 0) {
                editor.putBoolean("lesson.LetterSounds0.passed", true)
            }
            editor.putBoolean("migration.v4", true)
            editor.apply()
        }
    }
}
