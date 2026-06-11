package com.dividetask.homeschoolteacher.reading

import com.dividetask.homeschoolteacher.AppConfig

/**
 * The active sight-word pool. Sourced from `sight_words.words` in
 * `app/src/main/assets/config.yaml` (loaded into [AppConfig.sightWords]
 * during app start). Edit the YAML to change the list.
 */
object SightWords {
    val all: List<String> get() = AppConfig.sightWords
}
