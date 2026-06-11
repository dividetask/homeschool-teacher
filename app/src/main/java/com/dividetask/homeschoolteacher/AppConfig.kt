package com.dividetask.homeschoolteacher

import android.content.Context
import com.dividetask.homeschoolteacher.lesson.Category
import com.dividetask.homeschoolteacher.lesson.LessonDefinition
import com.dividetask.homeschoolteacher.lesson.LessonId
import org.yaml.snakeyaml.Yaml

object AppConfig {
    private val DEFAULT_PHONEME_WORDS: Map<Char, List<String>> = mapOf(
        'b' to listOf("ball", "bed", "bat", "book", "boy", "bag", "bug"),
        'c' to listOf("cat", "cup", "car", "cake"),
        'd' to listOf("dog", "duck", "doll", "dish", "desk", "dad", "dot"),
        'f' to listOf("fan", "fish", "fox", "foot", "fire", "four", "frog"),
        'g' to listOf("goat", "gum", "girl", "game", "gate", "goose", "grape"),
        'h' to listOf("hat", "hen", "hop", "house", "horse", "ham", "hill"),
        'j' to listOf("jet", "jam", "juice", "jump", "jeep", "jar", "jelly"),
        'k' to listOf("kite", "king", "key", "kid"),
        'l' to listOf("leaf", "lion", "leg", "lake", "lamp", "lock", "log"),
        'm' to listOf("moon", "mom", "milk", "mat", "map", "man", "mouse"),
        'n' to listOf("nest", "nut", "net", "nose", "name", "nail", "nine"),
        'p' to listOf("pig", "pen", "pot", "pie", "pan", "pup", "pop"),
        'r' to listOf("rat", "red", "rain", "run", "rock", "ring", "robot"),
        's' to listOf("sun", "sock", "soup", "seal", "six", "sit", "snake"),
        't' to listOf("top", "tap", "ten", "tea", "toy", "two", "tub"),
        'v' to listOf("van", "vet", "vine", "voice", "vase", "valley"),
        'w' to listOf("web", "water", "wagon", "walk", "win", "wind", "wave"),
        'y' to listOf("yo-yo", "yellow", "yes", "year", "yard", "yawn"),
        'z' to listOf("zebra", "zoo", "zip", "zero", "zigzag"),
    )

    private val DEFAULT_SIGHT_WORDS = listOf(
        "cat", "bat", "hat", "rat", "sat",
        "can", "man", "ran", "pan",
        "dog", "log", "frog", "hog",
        "pig", "big", "dig", "wig",
        "pet", "net", "wet", "vet",
    )

    private val DEFAULT_RHYMING_GROUPS: List<List<String>> = listOf(
        listOf("cat", "bat", "hat", "mat", "rat", "sat"),
        listOf("dog", "fog", "log", "jog", "hog"),
        listOf("sun", "fun", "run", "bun", "done"),
        listOf("top", "mop", "hop", "stop", "pop"),
        listOf("pig", "big", "wig", "dig", "fig"),
        listOf("cake", "lake", "bake", "make", "shake"),
        listOf("bell", "well", "tell", "shell", "sell"),
        listOf("car", "star", "jar", "bar", "far"),
        listOf("tree", "bee", "three", "free", "see"),
        listOf("ball", "fall", "tall", "call", "wall"),
        listOf("light", "kite", "night", "fight", "might"),
        listOf("box", "fox", "socks", "rocks", "locks"),
        listOf("pan", "fan", "man", "can", "van"),
        listOf("chair", "bear", "hair", "pair", "stair"),
        listOf("book", "hook", "look", "cook", "took"),
    )

    @Volatile
    var ticTacToeAutoRestartSeconds: Int = 2
        private set

    @Volatile
    var gameRunsPerRound: Int = 1
        private set

    @Volatile
    var mathRunsPerRound: Int = 4
        private set

    @Volatile
    var readingRunsPerRound: Int = 2
        private set

    // Phonemes overrides the Reading category default: each problem is a
    // trio of spoken words, so a slightly longer run keeps the lesson from
    // feeling like it ends as soon as it starts.
    @Volatile
    var phonemesRunsPerRound: Int = 3
        private set

    @Volatile
    var sightWords: List<String> = DEFAULT_SIGHT_WORDS
        private set

    @Volatile
    var rhymingWordGroups: List<List<String>> = DEFAULT_RHYMING_GROUPS
        private set

    /** Flat pool of every rhyming word across all groups. */
    val rhymingWords: List<String> get() = rhymingWordGroups.flatten()

    @Volatile
    var phonemeWords: Map<Char, List<String>> = DEFAULT_PHONEME_WORDS
        private set

    /** Flat list of every phoneme word across all letter groups. */
    val phonemeWordList: List<String> get() = phonemeWords.values.flatten()

    fun runsPerRound(category: Category): Int = when (category) {
        Category.Game -> gameRunsPerRound
        Category.Math -> mathRunsPerRound
        Category.Reading -> readingRunsPerRound
    }.coerceAtLeast(1)

    /** Per-lesson run count; falls back to the lesson's category default. */
    fun runsPerRound(def: LessonDefinition): Int = when (def.id) {
        LessonId.Phonemes0 -> phonemesRunsPerRound.coerceAtLeast(1)
        else -> runsPerRound(def.category)
    }

    fun load(context: Context) {
        runCatching {
            context.assets.open("config.yaml").use { stream ->
                val data = Yaml().load<Map<String, Any?>>(stream) ?: return@use
                (data["tictactoe"] as? Map<*, *>)?.let { tt ->
                    (tt["auto_restart_seconds"] as? Number)?.toInt()?.let {
                        ticTacToeAutoRestartSeconds = it
                    }
                }
                (data["session"] as? Map<*, *>)?.let { s ->
                    (s["math_runs_per_round"] as? Number)?.toInt()?.let {
                        mathRunsPerRound = it.coerceAtLeast(1)
                    }
                    (s["game_runs_per_round"] as? Number)?.toInt()?.let {
                        gameRunsPerRound = it.coerceAtLeast(1)
                    }
                    (s["reading_runs_per_round"] as? Number)?.toInt()?.let {
                        readingRunsPerRound = it.coerceAtLeast(1)
                    }
                    (s["phonemes_runs_per_round"] as? Number)?.toInt()?.let {
                        phonemesRunsPerRound = it.coerceAtLeast(1)
                    }
                    // Backwards-compat with the older per-activity keys.
                    (s["math_problems_per_round"] as? Number)?.toInt()?.let {
                        mathRunsPerRound = it.coerceAtLeast(1)
                    }
                    (s["tictactoe_games_per_round"] as? Number)?.toInt()?.let {
                        gameRunsPerRound = it.coerceAtLeast(1)
                    }
                    (s["reading_puzzles_per_round"] as? Number)?.toInt()?.let {
                        readingRunsPerRound = it.coerceAtLeast(1)
                    }
                }
                (data["sight_words"] as? Map<*, *>)?.let { sw ->
                    (sw["words"] as? List<*>)?.let { list ->
                        val parsed = list
                            .mapNotNull { (it as? String)?.lowercase()?.trim() }
                            .filter { it.isNotEmpty() }
                        if (parsed.isNotEmpty()) {
                            sightWords = parsed
                        }
                    }
                }
                (data["rhyming_words"] as? Map<*, *>)?.let { rw ->
                    (rw["groups"] as? List<*>)?.let { groups ->
                        val parsed: List<List<String>> = groups.mapNotNull { grp ->
                            (grp as? List<*>)
                                ?.mapNotNull { (it as? String)?.lowercase()?.trim() }
                                ?.filter { it.isNotEmpty() }
                                ?.takeIf { it.isNotEmpty() }
                        }
                        if (parsed.isNotEmpty()) {
                            rhymingWordGroups = parsed
                        }
                    }
                }
                (data["phonemes"] as? Map<*, *>)?.let { ph ->
                    val parsed: Map<Char, List<String>> = ph.entries.mapNotNull { (k, v) ->
                        val letter = (k as? String)?.lowercase()?.singleOrNull()
                            ?: return@mapNotNull null
                        if (!letter.isLetter()) return@mapNotNull null
                        val words = (v as? List<*>)
                            ?.mapNotNull { (it as? String)?.lowercase()?.trim() }
                            ?.filter { it.isNotEmpty() }
                            ?: return@mapNotNull null
                        if (words.size < 3) return@mapNotNull null
                        letter to words
                    }.toMap()
                    if (parsed.isNotEmpty()) {
                        phonemeWords = parsed
                    }
                }
            }
        }
    }
}
