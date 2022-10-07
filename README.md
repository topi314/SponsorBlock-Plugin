[![](https://jitpack.io/v/TopiSenpai/Sponsorblock-Plugin.svg)](https://jitpack.io/#TopiSenpai/sponsorblock-plugin)

# SponsorBlock-Plugin

A [Lavalink](https://github.com/freyacodes/Lavalink) plugin to skip [SponsorBlock](https://sponsor.ajay.app) segments in [YouTube](https://youtube.com) videos

## Installation

To install this plugin either download the latest release and place it into your `plugins` folder or add the following
into your `application.yml`

```yaml
lavalink:
  plugins:
    - dependency: "com.github.TopiSenpai:Sponsorblock-Plugin:x.x.x" # replace x.x.x with the latest release tag!
      repository: "https://jitpack.io"
```

## Usage

In the `play` op you can tell which segment categories you want to skip. The plugin then fetches the segments for the
played youtube video and skips those.

```yaml
{
  ...
  "skipSegments": [
    "segmentCategory"
  ]
}
```

[Segment Categories](https://wiki.sponsor.ajay.app/w/Segment_Categories):

* `sponsor`
* `selfpromo`
* `interaction`
* `intro`
* `outro`
* `preview`
* `music_offtopic`
* `filler`

---

There are also two new events:

### SegmentsLoaded

which is fired when the segments for a track are loaded

````json
{
  "op": "event",
  "type": "SegmentsLoaded",
  "guildId": "...",
  "segments": [
    {
      "category": "...",
      "start": "...", // in milliseconds
      "end": "..." // in milliseconds
    }
  ]
}
````

### SegmentSkipped

which is fired when a segment is skipped

````json
{
  "op": "event",
  "type": "SegmentSkipped",
  "guildId": "...",
  "segment": {
    "category": "...",
    "start": "...", // in milliseconds
    "end": "..." // in milliseconds
  }
}
````

## Example

An example implementation in Go can be found [here](https://github.com/TopiSenpai/sponsorblock-plugin-example)
