package com.urlstream.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.urlstream.model.VideoInfo
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun PlayerScreen(
    video: VideoInfo,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scrollState = rememberScrollState()

    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekFeedback by remember { mutableIntStateOf(0) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    var audioTrackLabels by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedAudioTrack by remember { mutableIntStateOf(0) }
    var showAudioMenu by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.url))
            prepare()
            playWhenReady = true
        }
    }

    val playerListener = remember {
        object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration
                    val tracks = exoPlayer.currentTracks
                    val labels = mutableListOf<String>()
                    for (i in 0 until tracks.groups.size) {
                        val group = tracks.groups[i]
                        if (group.type == C.TRACK_TYPE_AUDIO) {
                            for (j in 0 until group.mediaTrackGroup.length) {
                                val fmt = group.mediaTrackGroup.getFormat(j)
                                val label = buildString {
                                    append(fmt.label?.ifBlank { null } ?: "Track ${labels.size + 1}")
                                    if (fmt.language != null) append(" (${fmt.language})")
                                }
                                labels.add(label)
                            }
                        }
                    }
                    audioTrackLabels = labels
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
    }

    DisposableEffect(exoPlayer) {
        exoPlayer.addListener(playerListener)
        onDispose {
            exoPlayer.removeListener(playerListener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(seekFeedback) {
        if (seekFeedback != 0) {
            delay(500)
            seekFeedback = 0
        }
    }

    LaunchedEffect(showControls) {
        if (showControls && isPlaying) {
            delay(4000)
            if (!isSeeking) showControls = false
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            if (!isSeeking) {
                currentPosition = exoPlayer.currentPosition
            }
        }
    }

    SideEffect {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    SideEffect {
        val window = activity?.window ?: return@SideEffect
        if (isFullscreen) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = {
                        Text(
                            text = video.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, video.url)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Share video link")
                            )
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isFullscreen) Modifier.padding(padding) else Modifier)
                .background(
                    if (isFullscreen) Color.Black
                    else MaterialTheme.colorScheme.surface
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isFullscreen) Modifier.weight(1f)
                        else Modifier.height(250.dp)
                    )
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    val seekMs =
                                        if (offset.x < size.width / 2) -10000L else 10000L
                                    val newPos = (exoPlayer.currentPosition + seekMs)
                                        .coerceIn(0L, exoPlayer.duration.coerceAtLeast(0))
                                    exoPlayer.seekTo(newPos)
                                    seekFeedback = if (seekMs < 0) -10 else 10
                                },
                                onTap = {
                                    showControls = !showControls
                                }
                            )
                        }
                ) {
                    if (seekFeedback != 0) {
                        Box(
                            modifier = Modifier
                                .align(
                                    if (seekFeedback < 0) Alignment.CenterStart
                                    else Alignment.CenterEnd
                                )
                                .padding(32.dp)
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (seekFeedback < 0) "-10s" else "+10s",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showControls) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                        ) {
                            IconButton(
                                onClick = {
                                    if (exoPlayer.isPlaying) exoPlayer.pause()
                                    else exoPlayer.play()
                                },
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause
                                    else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Slider(
                                    value = if (duration > 0)
                                        (currentPosition.toFloat() / duration.toFloat()) else 0f,
                                    onValueChange = { fraction ->
                                        isSeeking = true
                                        currentPosition = (fraction * duration).toLong()
                                    },
                                    onValueChangeFinished = {
                                        isSeeking = false
                                        exoPlayer.seekTo(currentPosition)
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatDuration(currentPosition),
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = formatDuration(duration),
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box {
                                        Text(
                                            text = "${playbackSpeed}x",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .clickable { showSpeedMenu = true }
                                                .background(
                                                    Color.White.copy(alpha = 0.2f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                        DropdownMenu(
                                            expanded = showSpeedMenu,
                                            onDismissRequest = { showSpeedMenu = false }
                                        ) {
                                            listOf(
                                                0.25f, 0.5f, 0.75f, 1f,
                                                1.25f, 1.5f, 1.75f, 2f
                                            ).forEach { speed ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            if (speed == 1f) "Normal (1x)"
                                                            else "${speed}x",
                                                            fontWeight = if (speed == playbackSpeed)
                                                                FontWeight.Bold
                                                            else FontWeight.Normal
                                                        )
                                                    },
                                                    onClick = {
                                                        playbackSpeed = speed
                                                        exoPlayer.setPlaybackSpeed(speed)
                                                        showSpeedMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (audioTrackLabels.size > 1) {
                                        Box {
                                            Text(
                                                text = "Audio",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .clickable { showAudioMenu = true }
                                                    .background(
                                                        Color.White.copy(alpha = 0.2f),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(
                                                        horizontal = 14.dp,
                                                        vertical = 6.dp
                                                    )
                                            )
                                            DropdownMenu(
                                                expanded = showAudioMenu,
                                                onDismissRequest = { showAudioMenu = false }
                                            ) {
                                                audioTrackLabels.forEachIndexed { index, label ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                label,
                                                                fontWeight = if (index == selectedAudioTrack)
                                                                    FontWeight.Bold
                                                                else FontWeight.Normal
                                                            )
                                                        },
                                                        onClick = {
                                                            selectedAudioTrack = index
                                                            switchAudioTrack(
                                                                exoPlayer,
                                                                index
                                                            )
                                                            showAudioMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { isFullscreen = !isFullscreen }
                                    ) {
                                        Icon(
                                            if (isFullscreen) Icons.Default.FullscreenExit
                                            else Icons.Default.Fullscreen,
                                            contentDescription = if (isFullscreen) "Exit fullscreen"
                                            else "Fullscreen",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isFullscreen) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (video.resolution.isNotBlank()) {
                            InfoChip(label = video.resolution)
                        }
                        if (video.size.isNotBlank()) {
                            InfoChip(label = video.size)
                        }
                        if (video.format.isNotBlank()) {
                            InfoChip(label = video.format)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Video URL",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = video.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    FilledTonalButton(
                        onClick = { playWithExternalApp(context, video.url) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Play with another app",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun switchAudioTrack(exoPlayer: ExoPlayer, trackIndex: Int) {
    val tracks = exoPlayer.currentTracks
    for (i in 0 until tracks.groups.size) {
        val group = tracks.groups[i]
        if (group.type == C.TRACK_TYPE_AUDIO) {
            val trackGroup = group.mediaTrackGroup
            if (trackIndex < trackGroup.length) {
                val params = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .clearOverrides()
                    .addOverride(
                        TrackSelectionOverride(trackGroup, listOf(trackIndex))
                    )
                    .build()
                exoPlayer.trackSelectionParameters = params
            }
            return
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun playWithExternalApp(context: android.content.Context, videoUrl: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(videoUrl), "video/*")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Play with"))
    } catch (e: Exception) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
        context.startActivity(Intent.createChooser(webIntent, "Open with"))
    }
}

@Composable
private fun InfoChip(label: String) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
