package com.github.topisenpai.plugin.sponsorblock;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class TrackUtils {

	public static AudioTrackInfo getAudioTrackInfo(String track) {
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
			e.printStackTrace();
		}

		return null;
	}
}
