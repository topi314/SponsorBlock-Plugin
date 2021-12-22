package com.github.topisenpai.plugin.sponsorblock;

public class VideoSegment {
	private final long segmentStart;
	private final long segmentEnd;

	public VideoSegment(long segmentStart, long segmentEnd) {
		this.segmentStart = segmentStart;
		this.segmentEnd = segmentEnd;
	}

	public long getSegmentStart() {
		return this.segmentStart;
	}

	public long getSegmentEnd() {
		return this.segmentEnd;
	}
}
