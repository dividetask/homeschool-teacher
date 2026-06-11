package com.dividetask.homeschoolteacher.ui

/**
 * The "Games X    Math X    Reading X" tally shown in the header
 * above every screen. The number for each category is the count of
 * passed lessons within that category.
 */
data class Proficiency(
    val games: Int = 0,
    val math: Int = 0,
    val reading: Int = 0,
)
