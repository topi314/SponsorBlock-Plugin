package com.github.topisenpai.plugin.sponsorblock;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler;
import dev.arbjerg.lavalink.api.ISocketContext;
import org.json.JSONObject;

import java.util.List;

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
		context.sendMessage(new JSONObject().put("op", "event").put("type", "SegmentSkipped").put("guildId", String.valueOf(guildId)).put("segment", segment.toJSON()));
		this.currentSegment++;
		if (this.currentSegment < segments.size()) {
			track.setMarker(new TrackMarker(segments.get(this.currentSegment).getSegmentStart(), this));
		}
	}
}
