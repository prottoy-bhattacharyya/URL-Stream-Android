# Graph Report - .  (2026-06-03)

## Corpus Check
- Corpus is ~5,032 words - fits in a single context window. You may not need a graph.

## Summary
- 40 nodes · 57 edges · 12 communities (8 shown, 4 thin omitted)
- Extraction: 84% EXTRACTED · 16% INFERRED · 0% AMBIGUOUS · INFERRED: 9 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Video Parsing Core|Video Parsing Core]]
- [[_COMMUNITY_Navigation & Home|Navigation & Home]]
- [[_COMMUNITY_App Core & Theme|App Core & Theme]]
- [[_COMMUNITY_Video Metadata Model|Video Metadata Model]]
- [[_COMMUNITY_Player Screen|Player Screen]]
- [[_COMMUNITY_Main ViewModel|Main ViewModel]]

## God Nodes (most connected - your core abstractions)
1. `VideoParser` - 13 edges
2. `PlayerScreen()` - 5 edges
3. `VideoInfo` - 4 edges
4. `NavGraph()` - 4 edges
5. `HomeScreen()` - 4 edges
6. `MainActivity` - 2 edges
7. `SubtitleTrack` - 2 edges
8. `URLStreamTheme()` - 2 edges
9. `VideoCard()` - 2 edges
10. `formatDuration()` - 2 edges

## Surprising Connections (you probably didn't know these)
- `HomeScreen()` --calls--> `VideoParser`  [INFERRED]
  app/src/main/java/com/urlstream/ui/screens/HomeScreen.kt → app/src/main/java/com/urlstream/network/VideoParser.kt
- `NavGraph()` --calls--> `PlayerScreen()`  [INFERRED]
  app/src/main/java/com/urlstream/navigation/NavGraph.kt → app/src/main/java/com/urlstream/ui/screens/PlayerScreen.kt
- `NavGraph()` --calls--> `HomeScreen()`  [INFERRED]
  app/src/main/java/com/urlstream/navigation/NavGraph.kt → app/src/main/java/com/urlstream/ui/screens/HomeScreen.kt

## Communities (12 total, 4 thin omitted)

### Community 1 - "Navigation & Home"
Cohesion: 0.33
Nodes (5): NavGraph(), Routes, VideoHolder, HomeScreen(), VideoCard()

### Community 4 - "Player Screen"
Cohesion: 0.7
Nodes (4): formatDuration(), InfoChip(), PlayerScreen(), playWithExternalApp()

## Knowledge Gaps
- **3 isolated node(s):** `VideoHolder`, `Routes`, `MainViewModel`
  These have ≤1 connection - possible missing edges or undocumented components.
- **4 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `NavGraph()` connect `Navigation & Home` to `App Core & Theme`, `Player Screen`?**
  _High betweenness centrality (0.408) - this node is a cross-community bridge._
- **Why does `VideoParser` connect `Video Parsing Core` to `Navigation & Home`, `Video Metadata Model`, `URL Resolution`?**
  _High betweenness centrality (0.399) - this node is a cross-community bridge._
- **Why does `HomeScreen()` connect `Navigation & Home` to `Video Parsing Core`?**
  _High betweenness centrality (0.383) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `VideoInfo` (e.g. with `.parseVideoTags()` and `.parseDirectVideoLinks()`) actually correct?**
  _`VideoInfo` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Are the 3 inferred relationships involving `NavGraph()` (e.g. with `.onCreate()` and `HomeScreen()`) actually correct?**
  _`NavGraph()` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Are the 2 inferred relationships involving `HomeScreen()` (e.g. with `NavGraph()` and `VideoParser`) actually correct?**
  _`HomeScreen()` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `VideoHolder`, `Routes`, `MainViewModel` to the rest of the system?**
  _3 weakly-connected nodes found - possible documentation gaps or missing edges._