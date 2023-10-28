@file:JvmName("InnerTubeClient")

package com.github.topi314.sponsorblock.plugin.inntertube

import com.github.topi314.sponsorblock.plugin.protocol.Chapter
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.http.HttpHeaders
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.StringEntity
import java.net.URI
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

val json = Json {
    ignoreUnknownKeys = true
}
private val youtube = URI.create("https://www.youtube.com")
private val webContext = InnerTubeContext(InnerTubeContext.Client("WEB", "2.20220502.01.00"))

@Serializable
data class InnerTubeContext(val client: Client) {
    @Serializable
    data class Client(val clientName: String, val clientVersion: String, val hl: String = "en", val gl: String = "US")
}

interface InnerTubeRequest {
    val context: InnerTubeContext
}

@Serializable
data class SearchRequest(override val context: InnerTubeContext, val query: String) : InnerTubeRequest

fun HttpInterface.requestVideoSearch(query: String): InnerTubeSingleBox<TwoColumnSearchResultsRendererContent> =
    makeRequest(
        youtube, "search", body = SearchRequest(webContext, query)
    )

fun HttpInterface.requestVideoRendererById(id: String): VideoRenderer? {
    val response = requestVideoSearch(id)

    return response
        .contents
        .twoColumnSearchResultsRenderer
        .primaryContents
        .sectionListRenderer
        .contents
        .asSequence()
        .flatMap {
            it.itemSectionRenderer?.contents?.map(VideoRendererConsent::videoRenderer) ?: emptyList()
        }
        .filterNotNull()
        .firstOrNull {
            it.videoId == id
        }
}

fun HttpInterface.requestVideoChaptersById(id: String, trackLength: Long): List<Chapter> {
    val video = requestVideoRendererById(id) ?: return emptyList()

    val content = video.expandableMetadata?.expandableMetadataRenderer?.expandedContent ?: return emptyList()

    val all = content.horizontalCardListRenderer.cards
    return (all.zipWithNext() + (all.last() to null)).map { (now, next) ->
        val renderer = now.macroMarkersListItemRenderer
        val start = renderer.timeDescription.joinRuns().parseDuration()
        val end = next?.macroMarkersListItemRenderer?.timeDescription?.joinRuns()?.parseDuration() ?: trackLength.toDuration(DurationUnit.MILLISECONDS)

        Chapter(
            renderer.title.joinRuns(),
            start.inWholeMilliseconds,
            end.inWholeMilliseconds,
            start + end
        )
    }
}


inline fun <reified B, reified R> HttpInterface.makeRequest(
    domain: URI,
    vararg endpoint: String,
    body: B? = null,
    builder: HttpPost.() -> Unit = {}
): R {
    val uri = URIBuilder(domain)
        .setPathSegments(listOf("youtubei", "v1") + endpoint.asList())
        .addParameter("prettyPrint", "false")
    val post = HttpPost(uri.build()).apply {
        addHeader(HttpHeaders.REFERER, domain.toString())
        if (body != null) {
            entity = StringEntity(json.encodeToString(body))
        }
        builder()
    }

    val response = execute(post)
    val jsonText = response.entity.content.buffered().readAllBytes().decodeToString()

    return json.decodeFromString(jsonText)
}

private fun String.parseDuration(): Duration {
    val units = split(':')
    val unitCount = units.size - 1
    val multiplierOffset = if (unitCount > 2) 1 else 0

    val seconds = units.foldRightIndexed(0) { index, input, acc ->
        val multiplier = 60.0.pow(multiplierOffset + (unitCount - index)).toInt()
        val parsed = input.trimEnd().toInt() * multiplier

        acc + parsed
    }

    return seconds.seconds
}

