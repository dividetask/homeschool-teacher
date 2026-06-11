package com.dividetask.homeschoolteacher.reading

import com.dividetask.homeschoolteacher.AppConfig

/**
 * The active rhyming-word pool. Sourced from `rhyming_words.groups` in
 * `app/src/main/assets/config.yaml` (loaded into
 * [AppConfig.rhymingWordGroups] during app start). Edit the YAML to
 * change the families or add new ones.
 */
object RhymingWords {
    /** Flat list of every word from every rhyme family. */
    val all: List<String> get() = AppConfig.rhymingWords

    /** Nested list, one inner list per rhyme family. */
    val groups: List<List<String>> get() = AppConfig.rhymingWordGroups
}
