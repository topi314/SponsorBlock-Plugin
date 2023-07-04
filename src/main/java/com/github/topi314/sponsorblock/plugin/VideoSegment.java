package com.github.topi314.sponsorblock.plugin;

import kotlinx.serialization.json.JsonElement;
import kotlinx.serialization.json.JsonElementKt;
import kotlinx.serialization.json.JsonObject;

import java.util.Map;

public class VideoSegment {

    private final String category;
    private final long segmentStart;
    private final long segmentEnd;

    public VideoSegment(String category, long segmentStart, long segmentEnd) {
        this.category = category;
        this.segmentStart = segmentStart;
        this.segmentEnd = segmentEnd;
    }

    public JsonElement toJSON() {
        return new JsonObject(Map.of(
                "category", JsonElementKt.JsonPrimitive(this.category),
                "start", JsonElementKt.JsonPrimitive(this.segmentStart),
                "end", JsonElementKt.JsonPrimitive(this.segmentEnd)
        ));
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
