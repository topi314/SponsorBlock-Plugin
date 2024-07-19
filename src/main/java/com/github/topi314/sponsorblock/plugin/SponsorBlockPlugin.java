package com.github.topi314.sponsorblock.plugin;

import com.github.topi314.sponsorblock.plugin.inntertube.InnerTubeClient;
import com.github.topi314.sponsorblock.plugin.protocol.*;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import dev.arbjerg.lavalink.api.IPlayer;
import dev.arbjerg.lavalink.api.ISocketContext;
import dev.arbjerg.lavalink.api.PluginEventHandler;
import org.apache.http.client.methods.HttpGet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RestController
public class SponsorBlockPlugin extends PluginEventHandler {

	private static final Logger log = LoggerFactory.getLogger(SponsorBlockPlugin.class);
	private static final String SPONSORBLOCK_URL = "https://sponsor.ajay.app/api/skipSegments?videoID=%s&categories=%s";

	private final HttpInterfaceManager httpInterfaceManager;
	private final Map<String, Map<Long, Set<String>>> categoriesToSkip;

	public SponsorBlockPlugin() {
		log.info("Loading SponsorBlock Plugin...");
		this.httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
		this.categoriesToSkip = new ConcurrentHashMap<>();
	}

	public Map<Long, Set<String>> getCategoriesToSkip(String sessionId) {
		return this.categoriesToSkip.get(sessionId);
	}

	@GetMapping("/v4/sessions/{sessionId}/players/{guildId}/sponsorblock/categories")
	public Set<String> getCategoriesToSkip(@PathVariable String sessionId, @PathVariable long guildId) {
		var guildCategoriesToSkip = this.categoriesToSkip.get(sessionId);
		if (guildCategoriesToSkip == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
		}
		var categoriesToSkip = guildCategoriesToSkip.get(guildId);
		if (categoriesToSkip == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Guild not found");
		}
		return categoriesToSkip;
	}

	@PutMapping("/v4/sessions/{sessionId}/players/{guildId}/sponsorblock/categories")
	public void setCategoriesToSkip(@PathVariable String sessionId, @PathVariable long guildId, @RequestBody Set<String> categories) {
		var guildCategoriesToSkip = this.categoriesToSkip.get(sessionId);
		if (guildCategoriesToSkip == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
		}
		guildCategoriesToSkip.put(guildId, categories);
	}

	@DeleteMapping("/v4/sessions/{sessionId}/players/{guildId}/sponsorblock/categories")
	public void removeCategoriesToSkip(@PathVariable String sessionId, @PathVariable long guildId) {
		var guildCategoriesToSkip = this.categoriesToSkip.get(sessionId);
		if (guildCategoriesToSkip == null) {
			return;
		}
		guildCategoriesToSkip.remove(guildId);
	}

	@Override
	public void onWebSocketOpen(ISocketContext context, boolean resumed) {
		this.categoriesToSkip.put(context.getSessionId(), new ConcurrentHashMap<>());
	}

	@Override
	public void onSocketContextDestroyed(ISocketContext context) {
		this.categoriesToSkip.remove(context.getSessionId());
	}

	@Override
	public void onNewPlayer(@NotNull ISocketContext context, IPlayer iPlayer) {
		iPlayer.getAudioPlayer().addListener(new PlayerListener(this, context, iPlayer.getGuildId()));
	}

	@Override
	public void onDestroyPlayer(ISocketContext context, @NotNull IPlayer player) {
		var guildCategoriesToSkip = categoriesToSkip.get(context.getSessionId());
		if (guildCategoriesToSkip != null) {
			guildCategoriesToSkip.remove(player.getGuildId());
		}
	}

	public List<Segment> retrieveVideoSegments(String videoId, Set<String> categories) throws IOException {
		var queryParam = URLEncoder.encode("[" + categories.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) + "]", StandardCharsets.UTF_8);
		var request = new HttpGet(String.format(SPONSORBLOCK_URL, videoId, queryParam));

		var json = HttpClientTools.fetchResponseAsJson(this.httpInterfaceManager.getInterface(), request);
		if (json == null) {
			return List.of();
		}

		var segments = new ArrayList<Segment>();
		for (var segment : json.values()) {
			var segmentTimes = segment.get("segment");
			segments.add(new Segment(segment.get("category").text(), (long) (segmentTimes.index(0).as(Float.class) * 1000), (long) (segmentTimes.index(1).as(Float.class) * 1000)));
		}
		return segments;
	}

	public static class PlayerListener extends AudioEventAdapter {

		private final SponsorBlockPlugin plugin;
		private final ISocketContext context;
		private final long guildId;

		public PlayerListener(SponsorBlockPlugin plugin, ISocketContext socketContext, long guildId) {
			this.plugin = plugin;
			this.context = socketContext;
			this.guildId = guildId;
		}

		@Override
		public void onTrackStart(AudioPlayer player, AudioTrack track) {
			if (track.getSourceManager() == null || !track.getSourceManager().getSourceName().equals("youtube")) {
				return;
			}
			var guildCategoriesToSkip = this.plugin.getCategoriesToSkip(this.context.getSessionId());
			if (guildCategoriesToSkip == null) {
				return;
			}
			var categories = guildCategoriesToSkip.get(this.guildId);
			if (categories == null) {
				return;
			}
			List<TrackMarkable> markables;
			try (var httpInterface = plugin.httpInterfaceManager.getInterface()) {
				var segments = this.plugin.retrieveVideoSegments(track.getIdentifier(), categories);
				if (!segments.isEmpty()) {
					context.sendMessage(EventsKt.eventSerializer(), new SegmentsLoaded("event", String.valueOf(guildId), segments));
				}
				var chapters = InnerTubeClient.requestVideoChaptersById(httpInterface, track.getIdentifier(), track.getDuration());
				if (!chapters.isEmpty()) {
					context.sendMessage(EventsKt.eventSerializer(), new ChaptersLoaded("event", String.valueOf(guildId), chapters));
				}
				var output = new ArrayList<TrackMarkable>(segments);
				output.addAll(chapters);
				markables = output;

				output.sort(Comparator.comparing(TrackMarkable::getStart));
			} catch (IOException e) {
				log.error("Failed to retrieve video segments", e);
				return;
			}
			log.info("Categories are: {}", markables);
			if (!markables.isEmpty()) {
				track.setMarker(new TrackMarker(markables.get(0).getStart(), new SegmentHandler(context, this.guildId, track, markables)));
			}
		}

	}

}
