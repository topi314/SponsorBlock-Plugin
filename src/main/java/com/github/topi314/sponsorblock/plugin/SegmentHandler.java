package com.github.topi314.sponsorblock.plugin;

import com.github.topi314.sponsorblock.plugin.protocol.*;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler;
import dev.arbjerg.lavalink.api.ISocketContext;

import java.util.List;

public class SegmentHandler implements TrackMarkerHandler {

    private final ISocketContext context;
    private final long guildId;
    private final AudioTrack track;
    private final List<? extends TrackMarkable> segments;
    private int currentSegment;

    public SegmentHandler(ISocketContext context, long guildId, AudioTrack track, List<? extends TrackMarkable> segments) {
        this.context = context;
        this.guildId = guildId;
        this.track = track;
        this.segments = segments;
    }

    @Override
    public void handle(MarkerState state) {
        System.out.println("Started with state: " + state + " and segment");
        if (!(state == MarkerState.REACHED || state == MarkerState.LATE || state == MarkerState.BYPASSED)) {
            return;
        }
        var markable = segments.get(this.currentSegment);
        System.out.println(markable);
        if (markable instanceof Chapter chapter) {
            System.out.println("Found Chapter:" + chapter);
            context.sendMessage(EventsKt.eventSerializer(), new ChapterStarted("event", guildId, chapter));
        } else if (markable instanceof Segment segment) {
            track.setPosition(segment.getEnd());
            context.sendMessage(EventsKt.eventSerializer(), new SegmentSkipped("event", guildId, segment));
        }
        this.currentSegment++;
        if (this.currentSegment < segments.size()) {
            track.setMarker(new TrackMarker(segments.get(this.currentSegment).getStart(), this));
        }
    }
}
