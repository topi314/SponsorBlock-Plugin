package wtf.topi.plugin.sponsorblock;

public class AudioTrackInfo {

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
}
