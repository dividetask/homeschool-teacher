package com.dividetask.homeschoolteacher.reading

import com.dividetask.homeschoolteacher.R

/**
 * Registry for the Letter Sounds lesson. Each [Entry] pairs a letter with
 * the pre-cut word clip (a raw resource) that is played aloud; the learner
 * then taps the letter the word starts with.
 *
 * The audio is recorded per letter and bundled under `res/raw`. Right now
 * only "A" has a clip (`a3.mp3` — the word recording); as clips for the
 * rest of the alphabet are added, append an [Entry] per letter and drop
 * the matching `<x>3.mp3` into `res/raw`. The pass criteria scale
 * automatically: every listed letter must be answered correctly at least
 * twice in a row.
 */
object LetterSounds {
    data class Entry(val letter: Char, val wordClipRes: Int)

    val entries: List<Entry> = listOf(
        Entry('A', R.raw.a3),
    )

    /** The letters that currently have a clip, in registry order. */
    val letters: List<Char> get() = entries.map { it.letter }

    /** The word-clip resource for [letter], or null if it has no clip. */
    fun clipFor(letter: Char): Int? =
        entries.firstOrNull { it.letter.equals(letter, ignoreCase = true) }?.wordClipRes
}
