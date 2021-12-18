package wtf.topi.plugin.sponsorblock;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler;

public class SegmentHandler implements TrackMarkerHandler {
	private final AudioTrack track;

	private int currentSegment;
	private final SponsorBlockPlugin plugin;

	public SegmentHandler(AudioTrack track, SponsorBlockPlugin plugin) {
		this.track = track;
		this.plugin = plugin;
	}

	@Override
	public void handle(MarkerState state) {
		if (!(state == MarkerState.REACHED || state == MarkerState.LATE)) {
			return;
		}
		var segments = plugin.getVideoSegments(track.getIdentifier());
		track.setPosition(segments.get(this.currentSegment).getSegmentEnd());

		this.currentSegment++;
		if (this.currentSegment < segments.size()) {
			track.setMarker(new TrackMarker(segments.get(this.currentSegment).getSegmentStart(), this));
		}
	}
}
