package com.dividetask.homeschoolteacher.lesson

enum class Category(val title: String) {
    Game("Games"),
    Math("Math"),
    Reading("Reading"),
}

enum class LessonId {
    TicTacToe0,
    TicTacToe1,
    TicTacToe2,
    Chess0,
    Chess1,
    Chess2,
    Chess3,
    MathPictures,
    Math0,
    HorizontalAddition0,
    NumberLineAddition0,
    CountingAddition1,
    Math1,
    HorizontalAddition1,
    MathNumberLine,
    BinaryOps0,
    BinaryOps1,
    CountingSubtraction0,
    HorizontalSubtraction0,
    VerticalSubtraction0,
    NumberLineSubtraction0,
    CountingMultiplication0,
    CountingMultiplication1,
    LetterSounds0,
    Phonemes0,
    Reading0,
    SightWords0,
    SightWords1,
    RhymingWords0,
}

data class LessonDefinition(
    val id: LessonId,
    val title: String,
    val category: Category,
    /**
     * Lessons that must be passed before this one unlocks. An empty list
     * means the lesson is at the head of its chain (always unlocked).
     * Multiple parents express the docs' "All <Subject> Difficulty N
     * passed" gate — e.g. Horizontal Addition Level 1 requires all four
     * Addition Level 0 variants to be passed first.
     */
    val parents: List<LessonId> = emptyList(),
)

object Lessons {
    // Named gates re-used in multiple lesson definitions.
    private val ADDITION_L0 = listOf(
        LessonId.MathPictures,
        LessonId.Math0,
        LessonId.HorizontalAddition0,
        LessonId.NumberLineAddition0,
    )
    private val ADDITION_L1 = listOf(
        LessonId.CountingAddition1,
        LessonId.Math1,
        LessonId.HorizontalAddition1,
        LessonId.MathNumberLine,
    )
    private val SUBTRACTION_L0 = listOf(
        LessonId.CountingSubtraction0,
        LessonId.HorizontalSubtraction0,
        LessonId.VerticalSubtraction0,
        LessonId.NumberLineSubtraction0,
    )

    val definitions: Map<LessonId, LessonDefinition> = listOf(
        LessonDefinition(LessonId.TicTacToe0, "Tic Tac Toe — Level 0", Category.Game),
        LessonDefinition(LessonId.TicTacToe1, "Tic Tac Toe — Level 1", Category.Game, listOf(LessonId.TicTacToe0)),
        LessonDefinition(LessonId.TicTacToe2, "Tic Tac Toe — Level 2", Category.Game, listOf(LessonId.TicTacToe1)),
        LessonDefinition(LessonId.Chess0, "Chess — Level 0", Category.Game, listOf(LessonId.TicTacToe1)),
        LessonDefinition(LessonId.Chess1, "Chess — Level 1", Category.Game, listOf(LessonId.Chess0)),
        LessonDefinition(LessonId.Chess2, "Chess — Level 2", Category.Game, listOf(LessonId.Chess1)),
        LessonDefinition(LessonId.Chess3, "Chess — Level 3", Category.Game, listOf(LessonId.Chess2)),
        // Addition — Level 0 group. Four sibling presentations of the
        // same 0..4 problem space (Counting / Vertical / Horizontal /
        // Number Line). No parents — all unlocked from the start. They
        // share the math.streak grid for cell coverage, but each lesson
        // also keeps its own consecutive-correct streak, so the four
        // are passed independently rather than all together.
        LessonDefinition(LessonId.MathPictures, "Counting Addition — Level 0", Category.Math),
        LessonDefinition(LessonId.Math0, "Vertical Addition — Level 0", Category.Math),
        LessonDefinition(LessonId.HorizontalAddition0, "Horizontal Addition — Level 0", Category.Math),
        LessonDefinition(LessonId.NumberLineAddition0, "Number Line Addition — Level 0", Category.Math),
        // Addition — Level 1 group. Same four presentations at the wider
        // range. Number Line Addition 1 is the gateway: it opens as soon
        // as Number Line Addition 0 is passed, and the other three L1
        // variants then require all four Addition L0 variants AND Number
        // Line Addition 1 to be passed.
        LessonDefinition(LessonId.MathNumberLine, "Number Line Addition — Level 1", Category.Math, listOf(LessonId.NumberLineAddition0)),
        LessonDefinition(LessonId.CountingAddition1, "Counting Addition — Level 1", Category.Math, ADDITION_L0 + LessonId.MathNumberLine),
        LessonDefinition(LessonId.Math1, "Vertical Addition — Level 1", Category.Math, ADDITION_L0 + LessonId.MathNumberLine),
        LessonDefinition(LessonId.HorizontalAddition1, "Horizontal Addition — Level 1", Category.Math, ADDITION_L0 + LessonId.MathNumberLine),
        // Binary unlocks once the whole Addition L0 group is passed;
        // Level 1 additionally requires Binary 0.
        LessonDefinition(LessonId.BinaryOps0, "Binary — Level 0", Category.Math, ADDITION_L0),
        LessonDefinition(LessonId.BinaryOps1, "Binary — Level 1", Category.Math, ADDITION_L0 + LessonId.BinaryOps0),
        // Subtraction — Level 0 group. Four parallel presentations of
        // op1 - op2 with op1 ∈ 4..9, op2 ∈ 0..4 (answer always
        // non-negative). All gated on the full Addition L1 group.
        LessonDefinition(LessonId.CountingSubtraction0, "Counting Subtraction — Level 0", Category.Math, ADDITION_L1),
        LessonDefinition(LessonId.HorizontalSubtraction0, "Horizontal Subtraction — Level 0", Category.Math, ADDITION_L1),
        LessonDefinition(LessonId.VerticalSubtraction0, "Vertical Subtraction — Level 0", Category.Math, ADDITION_L1),
        LessonDefinition(LessonId.NumberLineSubtraction0, "Number Line Subtraction — Level 0", Category.Math, ADDITION_L1),
        // Counting Multiplication unlocks after the whole Subtraction
        // L0 group is passed.
        LessonDefinition(LessonId.CountingMultiplication0, "Counting Multiplication — Level 0", Category.Math, SUBTRACTION_L0),
        // Level 1 keeps the same boxed groups but asks which two numbers are
        // being multiplied (operands, not the product); operands 1..4.
        LessonDefinition(LessonId.CountingMultiplication1, "Counting Multiplication — Level 1", Category.Math, listOf(LessonId.CountingMultiplication0)),
        // Letter Sounds is the head of the Reading chain: a recorded word
        // clip plays and the learner taps the letter it starts with.
        // Everything else in Reading now sits behind it (Phonemes requires
        // it, and the rest follow Phonemes transitively).
        LessonDefinition(LessonId.LetterSounds0, "Letter Sounds — Level 0", Category.Reading),
        LessonDefinition(LessonId.Phonemes0, "Phonemes — Level 0", Category.Reading, listOf(LessonId.LetterSounds0)),
        LessonDefinition(LessonId.Reading0, "Animals — Level 0", Category.Reading, listOf(LessonId.Phonemes0)),
        LessonDefinition(LessonId.SightWords0, "Sight Words — Level 0", Category.Reading, listOf(LessonId.Reading0)),
        LessonDefinition(LessonId.SightWords1, "Sight Words — Level 1", Category.Reading, listOf(LessonId.SightWords0)),
        LessonDefinition(LessonId.RhymingWords0, "Rhyming Words — Level 0", Category.Reading, listOf(LessonId.SightWords1)),
    ).associateBy { it.id }

    val all: List<LessonDefinition> = definitions.values.toList()

    fun get(id: LessonId): LessonDefinition = definitions.getValue(id)

    /**
     * Lessons currently available to play. A lesson is available if either
     *  - every parent has been [passed], OR
     *  - it has been manually unlocked from the Progress screen (in
     *    which case its parents are irrelevant for menu visibility).
     */
    fun unlocked(
        passed: Map<LessonId, Boolean>,
        manualUnlock: Map<LessonId, Boolean> = emptyMap(),
    ): List<LessonDefinition> =
        all.filter { def ->
            manualUnlock[def.id] == true ||
                def.parents.all { passed[it] == true }
        }
}
