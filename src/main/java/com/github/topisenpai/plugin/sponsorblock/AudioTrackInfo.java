package com.github.topisenpai.plugin.sponsorblock;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class AudioTrackInfo {

	private static final Logger log = LoggerFactory.getLogger(SponsorBlockPlugin.class);

	public final String title;
	public final String author;
	public final long length;
	public final String identifier;
	public final boolean isStream;
	public final String uri;
	public final String sourceName;
	public final long position;


	public AudioTrackInfo(String title, String author, long length, String identifier, boolean isStream, String uri, String sourceName, long position) {
		this.title = title;
		this.author = author;
		this.length = length;
		this.identifier = identifier;
		this.isStream = isStream;
		this.uri = uri;
		this.sourceName = sourceName;
		this.position = position;
	}

	public static AudioTrackInfo fromTrack(String track) {
		var stream = new MessageInput(new ByteArrayInputStream(Base64.decodeBase64(track)));
		try {
			var input = stream.nextMessage();
			if (input == null) {
				return null;
			}

			int version = (stream.getMessageFlags() & 1) != 0 ? (input.readByte() & 0xFF) : 1;

			var title = input.readUTF();
			var author = input.readUTF();
			var length = input.readLong();
			var identifier = input.readUTF();
			var isStream = input.readBoolean();
			var uri = version >= 2 ? DataFormatTools.readNullableText(input) : null;
			var sourceName = input.readUTF();
			var position = input.readLong();
			var trackInfo = new AudioTrackInfo(title, author, length, identifier, isStream, uri, sourceName, position);
			stream.skipRemainingBytes();
			return trackInfo;
		} catch (IOException e) {
			log.error("Failed to read track info", e);
		}
		return null;
	}

}
