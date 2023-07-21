package com.github.topi314.sponsorblock.plugin.protocol

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
data class Chapter(
    val name: String, override val start: Long, override val end: Long,
    val duration: Duration
) : TrackMarkable {
    companion object {
        val EMPTY = Chapter("", 0, 0, 0.seconds)
    }
}
