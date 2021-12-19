# SponsorBLockPlugin

this plugin extends the play payload by following fields

---

## Payload
````json
{
  // Old Fields
  "op": "play",
  "guildId": "...",
  "track": "...",
  "startTime": "60000",
  "endTime": "120000",
  "volume": "100",
  "noReplace": false,
  "pause": false,
  // New Fields
  "skipSegments": [
    "segmentCategory"
  ]
}
````

## [Segment Categories](https://wiki.sponsor.ajay.app/w/Segment_Categories):
* sponsor
* selfpromo
* interaction
* intro
* outro
* preview
* music_offtopic 
* filler