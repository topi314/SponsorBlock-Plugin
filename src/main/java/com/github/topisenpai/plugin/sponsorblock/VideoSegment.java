package com.github.topisenpai.plugin.sponsorblock;

import org.json.JSONObject;

public class VideoSegment {

	private final String category;
	private final long segmentStart;
	private final long segmentEnd;

	public VideoSegment(String category, long segmentStart, long segmentEnd) {
		this.category = category;
		this.segmentStart = segmentStart;
		this.segmentEnd = segmentEnd;
	}

	public JSONObject toJSON() {
		return new JSONObject().put("category", category).put("start", segmentStart).put("end", segmentEnd);
	}

	public String getCategory() {
		return category;
	}

	public long getSegmentStart() {
		return this.segmentStart;
	}

	public long getSegmentEnd() {
		return this.segmentEnd;
	}
}
