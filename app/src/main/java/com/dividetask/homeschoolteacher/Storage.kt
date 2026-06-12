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

    // --- Win streak (the single, unified streak variable) ---
    // Every consecutive-correct / non-loss streak in the app lives here,
    // under one `win_streak.<key>` namespace. The key identifies the
    // lesson and, for lessons that track several parallel streaks, the
    // item within it:
    //   - Games & math:        win_streak.<LessonId>            (e.g. "Chess1")
    //   - Animals/Letter Sounds: win_streak.<LessonId>.<LETTER>
    //   - Phonemes/Rhyming:    win_streak.<LessonId>.<word>
    //   - Sight Words:         win_streak.SightWords.<word>.<pos> (shared L0/L1)
    //   - Letter Sounds run:   win_streak.LetterSounds0.run
    // The old per-feature streak keys are folded into this namespace by
    // the v5 migration below.
    fun loadWinStreak(key: String): Int = prefs().getInt("win_streak.$key", 0)

    fun saveWinStreak(key: String, value: Int) {
        prefs().edit().putInt("win_streak.$key", value).apply()
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

    // --- Tic Tac Toe ---
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

    // --- Chess ---
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

        if (!p.contains("migration.v5")) {
            // Streak consolidation: every per-feature streak key is folded
            // into the single `win_streak.*` namespace. We scan the existing
            // integer entries by prefix and rewrite them, then drop the old
            // keys. The coverage grids (`math.streak.*`, `subtraction.*`,
            // `binary.streak.*`, `multiplication.streak.*`) are NOT streaks
            // and are deliberately left untouched.
            val editor = p.edit()
            for ((key, value) in p.all) {
                if (value !is Int) continue
                val newKey: String? = when {
                    key.startsWith("ttt.streak.") ->
                        "win_streak." + key.removePrefix("ttt.streak.")
                    key.startsWith("chess.streak.") ->
                        "win_streak." + key.removePrefix("chess.streak.")
                    key.startsWith("math.lessonstreak.") ->
                        "win_streak." + key.removePrefix("math.lessonstreak.")
                    key.startsWith("reading.streak.") ->
                        "win_streak.Reading0." + key.removePrefix("reading.streak.")
                    key.startsWith("phonemes.streak.") ->
                        "win_streak.Phonemes0." + key.removePrefix("phonemes.streak.")
                    key.startsWith("sightwords.streak.") ->
                        "win_streak.SightWords." + key.removePrefix("sightwords.streak.")
                    key.startsWith("rhymingwords.streak.") ->
                        "win_streak.RhymingWords0." + key.removePrefix("rhymingwords.streak.")
                    key.startsWith("lettersounds.streak.") ->
                        "win_streak.LetterSounds0." + key.removePrefix("lettersounds.streak.")
                    key == "lettersounds.runstreak" ->
                        "win_streak.LetterSounds0.run"
                    else -> null
                }
                if (newKey != null) {
                    editor.putInt(newKey, value)
                    editor.remove(key)
                }
            }
            editor.putBoolean("migration.v5", true)
            editor.apply()
        }
    }
}
