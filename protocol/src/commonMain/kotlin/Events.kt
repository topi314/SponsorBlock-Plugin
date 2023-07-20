@file:OptIn(ExperimentalSerializationApi::class)

package com.github.topi314.sponsorblock.plugin.protocol

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

fun eventSerializer() = SponsorblockPluginEvent.serializer()

@JsonClassDiscriminator("type")
@Serializable
sealed interface SponsorblockPluginEvent

@SerialName("SegmentSkipped")
@Serializable
data class SegmentSkipped(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val op: String = "event",
    val guildId: Long,
    val segment: Segment
) : SponsorblockPluginEvent

@SerialName("SegmentsLoaded")
@Serializable
data class SegmentsLoaded(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val op: String = "event",
    val guildId: Long,
    val segments: List<Segment>
) : SponsorblockPluginEvent

@SerialName("ChapterStarted")
@Serializable
data class ChapterStarted(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val op: String = "event",
    val guildId: Long,
    val chapter: Chapter
) : SponsorblockPluginEvent

@SerialName("ChaptersLoaded")
@Serializable
data class ChaptersLoaded(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val op: String = "event",
    val guildId: Long,
    val chapters: List<Chapter>
) : SponsorblockPluginEvent
