package com.dividetask.homeschoolteacher.reading

import com.dividetask.homeschoolteacher.R

/**
 * Registry for the Letter Sounds lesson. Each [Entry] pairs a letter with
 * two pre-cut clips (raw resources):
 *  - [wordClipRes] — the word recording (`<x>3.mp3`) played as the
 *    question; the learner taps the letter the word starts with.
 *  - [answerClipRes] — the letter recording (`<x>1.mp3`) played back as
 *    reinforcement once the learner has answered.
 *
 * The audio is recorded per letter and bundled under `res/raw`. Right now
 * only "A" has clips (`a3.mp3` / `a1.mp3`); as the rest of the alphabet is
 * added, append an [Entry] per letter and drop the matching `<x>3.mp3`
 * and `<x>1.mp3` into `res/raw`. The pass criteria scale automatically:
 * every listed letter must be answered correctly at least twice in a row.
 */
object LetterSounds {
    data class Entry(
        val letter: Char,
        val wordClipRes: Int,
        val answerClipRes: Int,
    )

    val entries: List<Entry> = listOf(
        Entry('A', wordClipRes = R.raw.a3, answerClipRes = R.raw.a1),
    )

    /** The letters that currently have a clip, in registry order. */
    val letters: List<Char> get() = entries.map { it.letter }

    /** The word-clip resource for [letter], or null if it has no clip. */
    fun clipFor(letter: Char): Int? =
        entries.firstOrNull { it.letter.equals(letter, ignoreCase = true) }?.wordClipRes
}
