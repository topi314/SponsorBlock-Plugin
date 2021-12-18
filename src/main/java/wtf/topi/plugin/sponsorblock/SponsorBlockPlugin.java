package wtf.topi.plugin.sponsorblock;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SponsorBlockPlugin implements PluginEventHandler {

	private static final Logger log = LoggerFactory.getLogger(SponsorBlockPlugin.class);
	private static final String SPONSORBLOCK_URL = "https://sponsor.ajay.app/api/skipSegments?videoID=%s&categories=%s";

	private Map<String, List<VideoSegment>> segmentsCache;

	public SponsorBlockPlugin() {
		log.info("Hello, world!");
		segmentsCache = new HashMap<>();
	}

	@Override
	public void onWebsocketMessageIn(ISocketContext socketContext, String message) {
		var json = new JSONObject(message);
		if (!json.getString("op").equals("play")) {
			return;
		}
		if (!json.getBoolean("skipSegments")) {
			return;
		}

		var info = TrackUtils.getAudioTrackInfo(json.getString("track"));
		if (info == null || !info.sourceName.equals("youtube")) {
			return;
		}

		// load segments
		var segments = this.retrieveVideoSegments(info.identifier);
		this.segmentsCache.put(info.identifier, segments);
	}

	@Override
	public void onNewPlayer(ISocketContext context, IPlayer player) {
		var plugin = this;
		player.getAudioPlayer().addListener(new AudioEventAdapter() {
			@Override
			public void onTrackStart(AudioPlayer player, AudioTrack track) {
				if (track.getSourceManager() == null || !track.getSourceManager().getSourceName().equals("youtube")) {
					return;
				}
				var segments = segmentsCache.get(track.getIdentifier());
				if (segments != null && !segments.isEmpty()) {
					track.setMarker(new TrackMarker(segments.get(0).getSegmentStart(), new SegmentHandler(track, plugin)));
				}
			}

			@Override
			public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
				segmentsCache.remove(track.getIdentifier());
			}
		});
	}

	public List<VideoSegment> getVideoSegments(String videoId) {
		return this.segmentsCache.get(videoId);
	}

	public List<VideoSegment> retrieveVideoSegments(String videoId) {
		var body = "";
		try {
			var url = new URL(String.format(SPONSORBLOCK_URL, videoId, URLEncoder.encode("[\"sponsor\", \"selfpromo\", \"interaction\", \"intro\", \"outro\", \"preview\", \"music_offtopic\"]", StandardCharsets.UTF_8)));
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

		System.out.println("BODY: " + body);

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
