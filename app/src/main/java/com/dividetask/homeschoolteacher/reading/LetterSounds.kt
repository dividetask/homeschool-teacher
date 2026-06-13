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
 * The audio is recorded per letter and bundled under `res/raw` as
 * `<x>3.mp3` (word) and `<x>1.mp3` (letter). The whole alphabet (A–Z) is
 * present. The pass criteria scale automatically: every listed letter
 * must be answered correctly at least twice in a row (plus the overall
 * run streak in the lesson).
 */
object LetterSounds {
    data class Entry(
        val letter: Char,
        val wordClipRes: Int,
        val answerClipRes: Int,
    )

    val entries: List<Entry> = listOf(
        Entry('A', wordClipRes = R.raw.a3, answerClipRes = R.raw.a1),
        Entry('B', wordClipRes = R.raw.b3, answerClipRes = R.raw.b1),
        Entry('C', wordClipRes = R.raw.c3, answerClipRes = R.raw.c1),
        Entry('D', wordClipRes = R.raw.d3, answerClipRes = R.raw.d1),
        Entry('E', wordClipRes = R.raw.e3, answerClipRes = R.raw.e1),
        Entry('F', wordClipRes = R.raw.f3, answerClipRes = R.raw.f1),
        Entry('G', wordClipRes = R.raw.g3, answerClipRes = R.raw.g1),
        Entry('H', wordClipRes = R.raw.h3, answerClipRes = R.raw.h1),
        Entry('I', wordClipRes = R.raw.i3, answerClipRes = R.raw.i1),
        Entry('J', wordClipRes = R.raw.j3, answerClipRes = R.raw.j1),
        Entry('K', wordClipRes = R.raw.k3, answerClipRes = R.raw.k1),
        Entry('L', wordClipRes = R.raw.l3, answerClipRes = R.raw.l1),
        Entry('M', wordClipRes = R.raw.m3, answerClipRes = R.raw.m1),
        Entry('N', wordClipRes = R.raw.n3, answerClipRes = R.raw.n1),
        Entry('O', wordClipRes = R.raw.o3, answerClipRes = R.raw.o1),
        Entry('P', wordClipRes = R.raw.p3, answerClipRes = R.raw.p1),
        Entry('Q', wordClipRes = R.raw.q3, answerClipRes = R.raw.q1),
        Entry('R', wordClipRes = R.raw.r3, answerClipRes = R.raw.r1),
        Entry('S', wordClipRes = R.raw.s3, answerClipRes = R.raw.s1),
        Entry('T', wordClipRes = R.raw.t3, answerClipRes = R.raw.t1),
        Entry('U', wordClipRes = R.raw.u3, answerClipRes = R.raw.u1),
        Entry('V', wordClipRes = R.raw.v3, answerClipRes = R.raw.v1),
        Entry('W', wordClipRes = R.raw.w3, answerClipRes = R.raw.w1),
        Entry('X', wordClipRes = R.raw.x3, answerClipRes = R.raw.x1),
        Entry('Y', wordClipRes = R.raw.y3, answerClipRes = R.raw.y1),
        Entry('Z', wordClipRes = R.raw.z3, answerClipRes = R.raw.z1),
    )

    /** The letters that currently have a clip, in registry order. */
    val letters: List<Char> get() = entries.map { it.letter }

    /** The word-clip resource for [letter], or null if it has no clip. */
    fun clipFor(letter: Char): Int? =
        entries.firstOrNull { it.letter.equals(letter, ignoreCase = true) }?.wordClipRes
}
