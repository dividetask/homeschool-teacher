package com.dividetask.homeschoolteacher.reading

/**
 * Data for the Position Words lessons (spatial prepositions: on / in /
 * over / under). Scenes pair a three-letter animal with a three-letter
 * object; each object declares which prepositions make sense with it (you
 * can't be "in" a log, for example). "over" is drawn with an arrow.
 */
object PositionWords {
    data class Item(val word: String, val emoji: String)

    /**
     * Object plus the prepositions that read sensibly with it. [tall] is
     * true for deep containers (box, bin, cup…) — for "in" the container is
     * drawn in front of the animal so it peeks out the top. A flat, open
     * container (pan) sets [tall] = false so the animal sits inside it.
     */
    data class Obj(val item: Item, val preps: Set<String>, val tall: Boolean = true)

    val prepositions: List<String> = listOf("on", "in", "over", "under", "by")

    /** Three-letter animals with an emoji. */
    val animals: List<Item> = listOf(
        Item("cat", "🐱"), Item("dog", "🐶"), Item("pig", "🐷"),
        Item("cow", "🐮"), Item("hen", "🐔"), Item("owl", "🦉"),
        Item("bee", "🐝"), Item("ant", "🐜"), Item("fox", "🦊"),
        Item("bat", "🦇"), Item("rat", "🐀"),
    )

    /**
     * Three-letter objects with an emoji and the prepositions they allow.
     * "by" (beside) reads fine with anything, so every object includes it.
     */
    val objects: List<Obj> = listOf(
        // "in" is reserved for objects that read as a container (the animal
        // is drawn peeking out of them). Tall/worn things (hat, bag, can)
        // look like the animal is *on* them, so they don't offer "in".
        Obj(Item("box", "📦"), setOf("on", "in", "over", "under", "by")),
        Obj(Item("bus", "🚌"), setOf("on", "in", "over", "under", "by")),
        Obj(Item("hat", "🎩"), setOf("on", "over", "under", "by")),
        Obj(Item("log", "🪵"), setOf("on", "over", "under", "by")),
        Obj(Item("bed", "🛏️"), setOf("on", "over", "under", "by")),
        Obj(Item("cup", "☕"), setOf("in", "over", "by")),
        Obj(Item("jar", "🫙"), setOf("in", "over", "by")),
        Obj(Item("pot", "🍲"), setOf("in", "over", "by")),
        Obj(Item("bag", "👜"), setOf("over", "by")),
        Obj(Item("net", "🥅"), setOf("in", "over", "under", "by")),
        Obj(Item("tub", "🛁"), setOf("on", "in", "over", "under", "by")),
        Obj(Item("pan", "🍳"), setOf("on", "in", "over", "under", "by"), tall = false),
        Obj(Item("can", "🥫"), setOf("on", "over", "under", "by")),
        Obj(Item("bin", "🗑️"), setOf("in", "over", "under", "by")),
    )

    val animalWords: List<String> get() = animals.map { it.word }
    val objectWords: List<String> get() = objects.map { it.item.word }

    fun animal(word: String): Item = animals.first { it.word == word }
    fun obj(word: String): Obj = objects.first { it.item.word == word }
}
