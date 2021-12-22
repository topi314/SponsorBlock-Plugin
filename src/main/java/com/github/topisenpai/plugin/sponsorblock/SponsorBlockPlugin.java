package com.github.topisenpai.plugin.sponsorblock;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import dev.arbjerg.lavalink.api.IPlayer;
import dev.arbjerg.lavalink.api.ISocketContext;
import dev.arbjerg.lavalink.api.PluginEventHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SponsorBlockPlugin implements PluginEventHandler {

	private static final Logger log = LoggerFactory.getLogger(SponsorBlockPlugin.class);
	private static final String SPONSORBLOCK_URL = "https://sponsor.ajay.app/api/skipSegments?videoID=%s&categories=%s";

	private final Map<Long, Set<String>> categoriesToSkip;

	public SponsorBlockPlugin() {
		log.info("Hello, world!");
		categoriesToSkip = new HashMap<>();
	}

	@Override
	public void onWebsocketMessageIn(ISocketContext socketContext, String message) {
		var json = new JSONObject(message);
		if (!json.getString("op").equals("play")) {
			return;
		}
		var info = TrackUtils.getAudioTrackInfo(json.getString("track"));
		if (info == null || !info.sourceName.equals("youtube")) {
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
		iPlayer.getAudioPlayer().addListener(new AudioEventAdapter() {
			@Override
			public void onTrackStart(AudioPlayer player, AudioTrack track) {
				if (track.getSourceManager() == null || !track.getSourceManager().getSourceName().equals("youtube")) {
					return;
				}
				var categories = categoriesToSkip.get(iPlayer.getGuildId());
				if (categories == null) {
					return;
				}
				var segments = retrieveVideoSegments(track.getIdentifier(), categories);
				if (segments != null && !segments.isEmpty()) {
					track.setMarker(new TrackMarker(segments.get(0).getSegmentStart(), new SegmentHandler(track, segments)));
				}
			}
		});
	}

	@Override
	public void onDestroyPlayer(ISocketContext context, IPlayer player) {
		categoriesToSkip.remove(player.getGuildId());
	}

	public List<VideoSegment> retrieveVideoSegments(String videoId, Set<String> categories) {
		var body = "";
		try {
			var url = new URL(String.format(SPONSORBLOCK_URL, videoId, URLEncoder.encode("[" + categories.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) + "]", StandardCharsets.UTF_8)));
			var con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.connect();

			int status = con.getResponseCode();
			var in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			var content = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}
			in.close();
			con.disconnect();
			body = content.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		var json = new JSONArray(body);
		var segments = new ArrayList<VideoSegment>();

		for (var i = 0; i < json.length(); i++) {
			var segmentTimes = json.getJSONObject(i).getJSONArray("segment");
			var segmentStart = (long) (segmentTimes.getFloat(0) * 1000);
			var segmentEnd = (long) (segmentTimes.getFloat(1) * 1000);
			segments.add(new VideoSegment(segmentStart, segmentEnd));
		}
		return segments;
	}
}
