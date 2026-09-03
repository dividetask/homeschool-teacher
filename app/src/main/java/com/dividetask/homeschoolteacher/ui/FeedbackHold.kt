package com.dividetask.homeschoolteacher.ui

/**
 * How long a screen holds the answer on show before advancing. Every
 * lesson uses these, so the pacing is the same wherever the learner is;
 * see `docs/lessons.md` § Rules → Show answer time.
 *
 * The one documented exception is Phonemes, which reveals three words at
 * once and holds longer, and Letter Sounds, which waits for its letter
 * clip to finish playing.
 */
object FeedbackHold {
    /** Correct answer. */
    const val CORRECT_MS: Long = 900

    /** Wrong answer — longer, so the correct one can be read. */
    const val WRONG_MS: Long = 2000

    /** Answer revealed by **Give up**. */
    const val REVEALED_MS: Long = 1600
}
