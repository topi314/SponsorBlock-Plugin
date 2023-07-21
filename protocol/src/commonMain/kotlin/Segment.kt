package com.github.topi314.sponsorblock.plugin.protocol

import kotlinx.serialization.Serializable

@Serializable
data class Segment(
    val category: String,
    override val start: Long,
    override val end: Long
) : TrackMarkable
