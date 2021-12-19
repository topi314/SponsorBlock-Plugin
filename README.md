# SponsorBLockPlugin

This plugin integrates https://sponsor.ajay.app/ into lavalink

---

## Usage

In the `play` op you can tell which segment categories you want to skip. The plugin then fetches the segments for the played youtube video and skips those.

````json
{
  ...
  "skipSegments": [
    "segmentCategory"
  ]
}
````

### [Segment Categories](https://wiki.sponsor.ajay.app/w/Segment_Categories):
* sponsor
* selfpromo
* interaction
* intro
* outro
* preview
* music_offtopic 
* filler
