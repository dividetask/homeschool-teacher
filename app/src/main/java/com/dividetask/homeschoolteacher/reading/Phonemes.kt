package com.dividetask.homeschoolteacher.reading

import com.dividetask.homeschoolteacher.AppConfig

/**
 * Phoneme word bank. Sourced from `phonemes` in
 * `app/src/main/assets/config.yaml` (loaded into
 * [AppConfig.phonemeWords] during app start). Edit the YAML to change
 * which words are used.
 */
object Phonemes {
    /** Map of first letter to the words that begin with that letter. */
    val byLetter: Map<Char, List<String>> get() = AppConfig.phonemeWords

    /** Flat list of every phoneme word. */
    val all: List<String> get() = AppConfig.phonemeWordList
}
