package com.urlstream.network

import com.urlstream.model.SubtitleTrack
import com.urlstream.model.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class VideoParser {

    suspend fun parseUrl(url: String): Result<List<VideoInfo>> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .followRedirects(true)
                .get()

            val videos = mutableListOf<VideoInfo>()
            var id = 0
            val baseUri = doc.baseUri()

            val seenUrls = mutableSetOf<String>()

            videos.addAll(parseVideoTags(doc, baseUri, id, seenUrls))
            id = videos.size
            videos.addAll(parseDirectVideoLinks(doc, baseUri, id, seenUrls))
            id = videos.size
            videos.addAll(parseIframeVideoSources(doc, baseUri, id, seenUrls))

            if (videos.isEmpty()) {
                Result.failure(Exception("No video links found on this page"))
            } else {
                Result.success(videos)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseVideoTags(
        doc: Document,
        baseUri: String,
        startId: Int,
        seenUrls: MutableSet<String>
    ): List<VideoInfo> {
        val videos = mutableListOf<VideoInfo>()
        var id = startId

        doc.select("video").forEach { video ->
            val poster = video.attr("poster")
            val videoTitle = video.attr("title").ifBlank {
                video.attr("data-title").ifBlank {
                    video.parent()?.text()?.take(80) ?: ""
                }
            }

            val subtitleTracks = video.select("track").mapNotNull { track ->
                val src = track.attr("src").trim()
                if (src.isNotBlank()) {
                    val absoluteSrc = resolveUrl(baseUri, src)
                    if (absoluteSrc != null) {
                        SubtitleTrack(
                            url = absoluteSrc,
                            srclang = track.attr("srclang"),
                            label = track.attr("label")
                        )
                    } else null
                } else null
            }

            val sources = video.select("source")
            if (sources.isNotEmpty()) {
                sources.forEachIndexed { index, source ->
                    val src = source.attr("src").trim()
                    if (src.isNotBlank()) {
                        val absoluteSrc = resolveUrl(baseUri, src) ?: return@forEachIndexed
                        if (absoluteSrc in seenUrls) return@forEachIndexed
                        seenUrls.add(absoluteSrc)

                        val type = source.attr("type")
                        val fileName = extractNameFromUrl(absoluteSrc)
                        val resolution = source.attr("resolution").ifBlank {
                            source.attr("label").ifBlank {
                                source.attr("height").let { h ->
                                    if (h.isNotBlank()) "${h}p" else ""
                                }
                            }
                        }
                        val size = getFileSizeAsync(absoluteSrc)

                        videos.add(
                            VideoInfo(
                                id = id++,
                                title = videoTitle.ifBlank { fileName },
                                url = absoluteSrc,
                                thumbnailUrl = resolveUrl(baseUri, poster) ?: extractThumbnail(doc, baseUri),
                                resolution = resolution,
                                size = size,
                                format = extractFormat(type, absoluteSrc),
                                subtitleTracks = subtitleTracks
                            )
                        )
                    }
                }
            } else {
                val src = video.attr("src").trim()
                if (src.isNotBlank()) {
                    val absoluteSrc = resolveUrl(baseUri, src) ?: return@forEach
                    if (absoluteSrc in seenUrls) return@forEach
                    seenUrls.add(absoluteSrc)

                    val fileName = extractNameFromUrl(absoluteSrc)
                    val size = getFileSizeAsync(absoluteSrc)

                    videos.add(
                        VideoInfo(
                            id = id++,
                            title = videoTitle.ifBlank { fileName },
                            url = absoluteSrc,
                            thumbnailUrl = resolveUrl(baseUri, poster) ?: extractThumbnail(doc, baseUri),
                            resolution = video.attr("height").let { h ->
                                if (h.isNotBlank()) "${h}p" else ""
                            },
                            size = size,
                            format = extractFormat("", absoluteSrc),
                            subtitleTracks = subtitleTracks
                        )
                    )
                }
            }
        }

        return videos
    }

    private fun parseDirectVideoLinks(
        doc: Document,
        baseUri: String,
        startId: Int,
        seenUrls: MutableSet<String>
    ): List<VideoInfo> {
        val videoExtensions = setOf(".mp4", ".webm", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".3gp", ".m4v")
        val videos = mutableListOf<VideoInfo>()
        var id = startId

        doc.select("a[href]").forEach { link ->
            val href = link.attr("href").trim()
            val ext = href.substringAfterLast(".").lowercase()
            if (videoExtensions.any { href.lowercase().endsWith(it) }) {
                val absoluteSrc = resolveUrl(baseUri, href) ?: return@forEach
                if (absoluteSrc in seenUrls) return@forEach
                seenUrls.add(absoluteSrc)

                val name = link.text().ifBlank {
                    link.attr("title").ifBlank {
                        extractNameFromUrl(absoluteSrc)
                    }
                }
                val size = getFileSizeAsync(absoluteSrc)

                videos.add(
                    VideoInfo(
                        id = id++,
                        title = name,
                        url = absoluteSrc,
                        thumbnailUrl = extractThumbnail(doc, baseUri),
                        resolution = extractResolutionFromUrl(absoluteSrc),
                        size = size,
                        format = ext.uppercase()
                    )
                )
            }
        }

        return videos
    }

    private fun parseIframeVideoSources(
        doc: Document,
        baseUri: String,
        startId: Int,
        seenUrls: MutableSet<String>
    ): List<VideoInfo> {
        val videos = mutableListOf<VideoInfo>()
        var id = startId

        doc.select("iframe[src]").forEach { iframe ->
            val src = iframe.attr("src").trim()
            if (src.isNotBlank() && (src.contains("youtube.com") || src.contains("vimeo.com") ||
                        src.contains("dailymotion.com") || src.contains("player"))
            ) {
                val absoluteSrc = resolveUrl(baseUri, src) ?: return@forEach
                if (absoluteSrc in seenUrls) return@forEach
                seenUrls.add(absoluteSrc)

                val name = iframe.attr("title").ifBlank {
                    iframe.attr("data-title").ifBlank {
                        "Embedded Video ${id + 1}"
                    }
                }
                val thumbnail = iframe.attr("data-thumbnail").ifBlank {
                    extractThumbnail(doc, baseUri)
                }

                videos.add(
                    VideoInfo(
                        id = id++,
                        title = name,
                        url = absoluteSrc,
                        thumbnailUrl = thumbnail,
                        resolution = "",
                        size = "",
                        format = "EMBED"
                    )
                )
            }
        }

        return videos
    }

    private fun resolveUrl(baseUri: String, src: String): String? {
        if (src.isBlank()) return null
        return try {
            if (src.startsWith("http://") || src.startsWith("https://")) src
            else if (src.startsWith("//")) "https:$src"
            else URI(baseUri).resolve(src).toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun extractThumbnail(doc: Document, baseUri: String): String {
        doc.select("meta[property=og:image]").firstOrNull()?.let {
            val content = it.attr("content")
            if (content.isNotBlank()) return resolveUrl(baseUri, content) ?: ""
        }
        doc.select("meta[name=twitter:image]").firstOrNull()?.let {
            val content = it.attr("content")
            if (content.isNotBlank()) return resolveUrl(baseUri, content) ?: ""
        }
        doc.select("img").firstOrNull()?.let {
            val src = it.attr("src")
            if (src.isNotBlank()) return resolveUrl(baseUri, src) ?: ""
        }
        return ""
    }

    private fun extractNameFromUrl(url: String): String {
        val fileName = url.substringAfterLast("/").substringBefore("?")
            .substringBefore("#")
        return fileName.substringBeforeLast(".")
            .replace("-", " ")
            .replace("_", " ")
            .replace(".", " ")
            .trim()
            .ifBlank { "Video" }
            .replaceFirstChar { it.uppercase() }
    }

    private fun extractFormat(type: String, src: String): String {
        if (type.isNotBlank()) {
            return type.substringAfter("/").uppercase()
        }
        return src.substringAfterLast(".").substringBefore("?").uppercase()
            .ifBlank { "UNKNOWN" }
    }

    private fun extractResolutionFromUrl(url: String): String {
        val resolutionPatterns = listOf(
            Regex("""(\d{3,4})p""", RegexOption.IGNORE_CASE),
            Regex("""(\d{3,4})[pP]"""),
            Regex("""(\d{4})x(\d{3,4})"""),
            Regex("""(1080|720|480|360|2160|1440|240)""")
        )
        for (pattern in resolutionPatterns) {
            val match = pattern.find(url)
            if (match != null) {
                val value = match.groupValues[1]
                return when (value) {
                    "2160" -> "4K"
                    "1440" -> "1440p"
                    "1080" -> "1080p"
                    "720" -> "720p"
                    "480" -> "480p"
                    "360" -> "360p"
                    "240" -> "240p"
                    else -> "${value}p"
                }
            }
        }
        return ""
    }

    private fun getFileSizeAsync(url: String): String {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()

            val contentLength = connection.contentLengthLong
            connection.disconnect()

            if (contentLength > 0) {
                formatFileSize(contentLength)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1_024 -> String.format("%.1f KB", bytes / 1_024.0)
            else -> "$bytes B"
        }
    }
}
