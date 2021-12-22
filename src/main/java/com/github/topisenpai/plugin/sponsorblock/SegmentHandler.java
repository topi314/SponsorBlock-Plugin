package com.github.topisenpai.plugin.sponsorblock;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler;

import java.util.List;

public class SegmentHandler implements TrackMarkerHandler {

	private final AudioTrack track;
	private int currentSegment;
	private final List<VideoSegment> segments;

	public SegmentHandler(AudioTrack track, List<VideoSegment> segments) {
		this.track = track;
		this.segments = segments;
	}

	@Override
	public void handle(MarkerState state) {
		if (!(state == MarkerState.REACHED || state == MarkerState.LATE)) {
			return;
		}
		track.setPosition(segments.get(this.currentSegment).getSegmentEnd());

		this.currentSegment++;
		if (this.currentSegment < segments.size()) {
			track.setMarker(new TrackMarker(segments.get(this.currentSegment).getSegmentStart(), this));
		}
	}
}
