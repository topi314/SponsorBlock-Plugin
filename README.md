[![](https://jitpack.io/v/TopiSenpai/Sponsorblock-Plugin.svg)](https://jitpack.io/#TopiSenpai/sponsorblock-plugin)

# SponsorBlock-Plugin

A [Lavalink](https://github.com/lavalink-devs/Lavalink) plugin
to skip [SponsorBlock](https://sponsor.ajay.app) segments in [YouTube](https://youtube.com) videos
and provide information about [chapters](https://support.google.com/youtube/answer/9884579)

## Usage

> **Warning** This plugin requires Lavalink v4 beta 3 or higher

To install this plugin either download the latest release and place it into your `plugins` folder or add the following into your `application.yml`

> **Note**
> For a full `application.yml` example see [here](application.example.yml)

```yaml
lavalink:
  plugins:
    - dependency: "com.github.topi314.sponsorblock:sponsorblock-plugin:x.x.x" # replace x.x.x with the latest release tag!
      repository: "https://maven.lavalink.dev/releases" # this is optional for lavalink v4.0.0-beta.5 or greater
      snapshot: false # set to true if you want to use snapshot builds (see below)
```

Snapshot builds are available in https://maven.lavalink.dev/snapshots with the short commit hash as the version

> **Tip** An example implementation in Go can be found [here](https://github.com/topi314/sponsorblock-plugin-example)

### API

Fields marked with `?` are optional and types marked with `?` are nullable.

#### Common Types

##### Categories

The category types.

> **Note** You can find more information here: https://wiki.sponsor.ajay.app/w/Segment_Categories

| Name             | Description                                                                                   |
|------------------|-----------------------------------------------------------------------------------------------|
| `sponsor`        | Paid promotion, paid referrals and direct advertisements.                                     |
| `selfpromo`      | Merchandise, donations, or information about who they collaborated with.                      |
| `interaction`    | Like, subscribe or follow in the middle of content.                                           |
| `intro`          | An introduction to a section of the track, without actual content.                            |
| `outro`          | Credits or when the YouTube endcards appear.                                                  |
| `preview`        | Collection of clips that show what is coming up in this track.                                |
| `music_offtopic` | Offtopic music content that isn't covered by other categories.                                |
| `filler`         | Tangential scenes or jokes that are not required to understand the main content of the track. |

##### Segment

A segment of the track.

| Name       | Type                           | Description                                       |
|------------|--------------------------------|---------------------------------------------------|
| `category` | [Categories](#categories) type | The category type for the segment.                |
| `start`    | int                            | The start time of this segment. (in milliseconds) |
| `end`      | int                            | The end time of this segment. (in milliseconds)   |

##### Chapter

A chapter of the track.

| Name       | Type   | Description                                       |
|------------|--------|---------------------------------------------------|
| `name`     | string | The chapter name.                                 |
| `start`    | int    | The start time of this chapter. (in milliseconds) |
| `end`      | int    | The end time of this chapter. (in milliseconds)   |
| `duration` | string | The duration of the chapter. (in milliseconds)    |

#### Get Categories

Get the categories that will be currently skipped.

```http
GET /v4/sessions/{sessionId}/players/{guildId}/sponsorblock/categories
```

Response:

200 OK:
List of [Categories](#categories)

<details>
<summary>Example Payload</summary>

```json
[
    "sponsor",
    "selfpromo"
]
```

</details>

404 Not Found:

- If there is no track playing in the guild

#### Put Categories

Add new categories to skip.

```http
PUT /v4/sessions/{sessionId}/players/{guildId}/sponsorblock/categories
```

Request:
List of [Categories](#categories)

<details>
<summary>Example Payload</summary>

```json
[
    "sponsor",
    "selfpromo"
]
```

</details>

Response:

204 No Content: Success

#### Delete Categories

Delete categories that will no longer be skipped.

```http
DELETE /v4/sessions/{sessionId}/players/{guildId}/sponsorblock/categories
```

Request:
List of [Categories](#categories)

<details>
<summary>Example Payload</summary>

```json
[
    "sponsor",
    "selfpromo"
]
```

</details>

Response:

204 No Content: Success

#### Events

> **note** All events follow the default arguments for the event op: https://lavalink.dev/api/websocket.html#event-op

##### Segments Loaded Event

This event is dispatched when the segments for a track have been loaded.

Type name: `SegmentsLoaded`

| Field      | Type                                  | Description                    |
|------------|---------------------------------------|--------------------------------|
| `segments` | Array of [Segment](#segment) objects. | The segments that were loaded. |

<details>
<summary>Example Payload</summary>

```json5
{
  "op": "event",
  "type": "SegmentsLoaded",
  "guildId": "...",
  "segments": [
    {
      "category": "...",
      "start": 0,
      "end": 3000
    }
  ]
}
```

</details>

##### Segment Skipped Event

This event is dispatched when a segment is skipped.

Type name: `SegmentSkipped`

| Field     | Type                        | Description                   |
|-----------|-----------------------------|-------------------------------|
| `segment` | [Segment](#segment) object. | The segment that was skipped. |

<details>
<summary>Example Payload</summary>

```json5
{
  "op": "event",
  "type": "SegmentSkipped",
  "guildId": "...",
  "segment": {
    "category": "...",
    "start": 0,
    "end": 3000
  }
}
```

</details>

##### Chapters Loaded Event

This event is dispatched when the chapters for a track have been loaded.

Type name: `ChaptersLoaded`

| Field      | Type                                  | Description                    |
|------------|---------------------------------------|--------------------------------|
| `chapters` | Array of [Chapter](#chapter) objects. | The chapters that were loaded. |

<details>
<summary>Example Payload</summary>

```json5
{
  "op": "event",
  "type": "ChaptersLoaded",
  "guildId": "...",
  "chapters": [
    {
      "name": "Prelude",
      "start": 0,
      "end": 0,
      "duration": "0"
    }
  ]
}
```

</details>

##### Chapter Started Event

This event is dispatched when a chapter is started.

Type name: `ChapterStarted`

| Field     | Type                        | Description               |
|-----------|-----------------------------|---------------------------|
| `chapter` | [Chapter](#chapter) object. | The chapter that started. |

<details>
<summary>Example Payload</summary>

```json5
{
  "op": "event",
  "type": "ChapterStarted",
  "guildId": "...",
  "chapter": {
    "name": "Prelude",
    "start": 0,
    "end": 0,
    "duration": "0"
  }
}
```

</details>

