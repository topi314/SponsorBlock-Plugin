package com.github.topi314.sponsorblock.plugin;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler;
import dev.arbjerg.lavalink.api.ISocketContext;
import kotlinx.serialization.json.JsonElementKt;
import kotlinx.serialization.json.JsonObject;

import java.util.List;
import java.util.Map;

public class SegmentHandler implements TrackMarkerHandler {

    private final ISocketContext context;
    private final long guildId;
    private final AudioTrack track;
    private final List<VideoSegment> segments;
    private int currentSegment;

    public SegmentHandler(ISocketContext context, long guildId, AudioTrack track, List<VideoSegment> segments) {
        this.context = context;
        this.guildId = guildId;
        this.track = track;
        this.segments = segments;
    }

    @Override
    public void handle(MarkerState state) {
        if (!(state == MarkerState.REACHED || state == MarkerState.LATE || state == MarkerState.BYPASSED)) {
            return;
        }
        var segment = segments.get(this.currentSegment);
        track.setPosition(segment.getSegmentEnd());
        context.sendMessage(new JsonObject(Map.of(
                "op", JsonElementKt.JsonPrimitive("event"),
                "type", JsonElementKt.JsonPrimitive("SegmentSkipped"),
                "guildId", JsonElementKt.JsonPrimitive(String.valueOf(guildId)),
                "segment", segment.toJSON()
        )));
        this.currentSegment++;
        if (this.currentSegment < segments.size()) {
            track.setMarker(new TrackMarker(segments.get(this.currentSegment).getSegmentStart(), this));
        }
    }
}
