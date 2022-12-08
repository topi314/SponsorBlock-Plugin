package com.github.topisenpai.plugin.sponsorblock;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import dev.arbjerg.lavalink.api.IPlayer;
import dev.arbjerg.lavalink.api.ISocketContext;
import dev.arbjerg.lavalink.api.PluginEventHandler;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SponsorBlockPlugin extends PluginEventHandler {

	private static final Logger log = LoggerFactory.getLogger(SponsorBlockPlugin.class);
	private static final String SPONSORBLOCK_URL = "https://sponsor.ajay.app/api/skipSegments?videoID=%s&categories=%s";

	private final HttpInterfaceManager httpInterfaceManager;
	private final Map<Long, Set<String>> categoriesToSkip;

	public SponsorBlockPlugin() {
		log.info("Loading SponsorBlock Plugin...");
		this.httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
		this.categoriesToSkip = new ConcurrentHashMap<>();
	}

	public Map<Long, Set<String>> getCategoriesToSkip() {
		return this.categoriesToSkip;
	}

	@Override
	public void onWebsocketMessageIn(ISocketContext socketContext, String message) {
		var json = new JSONObject(message);
		if (!json.optString("op").equals("play")) {
			return;
		}
		var sourceName = this.readSourceName(json.optString("track"));
		if (sourceName == null || !sourceName.equals("youtube")) {
			return;
		}
		var rawSegments = json.optJSONArray("skipSegments");
		if (rawSegments == null) {
			return;
		}
		var segments = new HashSet<String>();
		rawSegments.forEach(segment -> segments.add((String) segment));

		this.categoriesToSkip.put(json.getLong("guildId"), segments);
	}

	@Override
	public void onNewPlayer(ISocketContext context, IPlayer iPlayer) {
		iPlayer.getAudioPlayer().addListener(new PlayerListener(this, context, iPlayer.getGuildId()));
	}

	@Override
	public void onDestroyPlayer(ISocketContext context, IPlayer player) {
		categoriesToSkip.remove(player.getGuildId());
	}

	public List<VideoSegment> retrieveVideoSegments(String videoId, Set<String> categories) throws IOException {
		var queryParam = URLEncoder.encode("[" + categories.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) + "]", StandardCharsets.UTF_8);
		var request = new HttpGet(String.format(SPONSORBLOCK_URL, videoId, queryParam));

		var json = HttpClientTools.fetchResponseAsJson(this.httpInterfaceManager.getInterface(), request);
		if (json == null) {
			return null;
		}

		var segments = new ArrayList<VideoSegment>();
		for (var segment : json.values()) {
			var segmentTimes = segment.get("segment");
			segments.add(new VideoSegment(segment.get("category").text(), (long) (segmentTimes.index(0).as(Float.class) * 1000), (long) (segmentTimes.index(1).as(Float.class) * 1000)));
		}
		return segments;
	}

	public String readSourceName(String track) {
		var stream = new MessageInput(new ByteArrayInputStream(Base64.decodeBase64(track)));
		try {
			var input = stream.nextMessage();
			if (input == null) {
				return null;
			}

			int version = (stream.getMessageFlags() & 1) != 0 ? (input.readByte() & 0xFF) : 1;
			input.readUTF();
			input.readUTF();
			input.readLong();
			input.readUTF();
			input.readBoolean();
			if (version >= 2) {
				DataFormatTools.readNullableText(input);
			}
			var sourceName = input.readUTF();
			stream.skipRemainingBytes();
			return sourceName;
		} catch (IOException e) {
			log.error("Failed to read track info", e);
		}
		return null;
	}

	public static class PlayerListener extends AudioEventAdapter {

		private final SponsorBlockPlugin plugin;
		private final ISocketContext context;
		private final long guildID;

		public PlayerListener(SponsorBlockPlugin plugin, ISocketContext socketContext, long guildID) {
			this.plugin = plugin;
			this.context = socketContext;
			this.guildID = guildID;
		}

		@Override
		public void onTrackStart(AudioPlayer player, AudioTrack track) {
			if (track.getSourceManager() == null || !track.getSourceManager().getSourceName().equals("youtube")) {
				return;
			}
			var categories = this.plugin.getCategoriesToSkip().get(this.guildID);
			if (categories == null) {
				return;
			}
			List<VideoSegment> segments;
			try {
				segments = this.plugin.retrieveVideoSegments(track.getIdentifier(), categories);
			} catch (IOException e) {
				log.error("Failed to retrieve video segments", e);
				return;
			}
			if (segments != null && !segments.isEmpty()) {
				context.sendMessage(new JSONObject()
						.put("op", "event")
						.put("type", "SegmentsLoaded")
						.put("guildId", String.valueOf(this.guildID))
						.put("segments", new JSONArray(segments.stream().map(VideoSegment::toJSON).collect(Collectors.toList()))));
				track.setMarker(new TrackMarker(segments.get(0).getSegmentStart(), new SegmentHandler(context, this.guildID, track, segments)));
			}
		}

	}

}
