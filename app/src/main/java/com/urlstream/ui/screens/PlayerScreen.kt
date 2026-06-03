package com.urlstream.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.VideoView
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.urlstream.model.VideoInfo
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    video: VideoInfo,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
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
    var isPrepared by remember { mutableStateOf(false) }

    data class AudioTrackInfo(val trackInfoIndex: Int, val label: String)
    var audioTracks by remember { mutableStateOf<List<AudioTrackInfo>>(emptyList()) }
    var selectedAudioTrackDownIdx by remember { mutableIntStateOf(0) }
    var showAudioMenu by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    var savedPosition by rememberSaveable { mutableStateOf(0L) }
    var savedIsPlaying by rememberSaveable { mutableStateOf(false) }
    var shouldRestore by rememberSaveable { mutableStateOf(false) }

    var hasSubtitles by remember { mutableStateOf(false) }
    var subtitlesOn by remember { mutableStateOf(false) }

    val mediaPlayerRef = remember { mutableStateOf<MediaPlayer?>(null) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    val audioFocusChangeListener = remember {
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    mediaPlayerRef.value?.let { mp ->
                        if (mp.isPlaying) {
                            mp.pause()
                            isPlaying = false
                        }
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { }
                AudioManager.AUDIOFOCUS_GAIN -> { }
            }
        }
    }

    val mediaSession = remember {
        MediaSession(context, "UrlStreamPlayer").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    mediaPlayerRef.value?.let { mp ->
                        if (!mp.isPlaying) {
                            mp.start()
                            isPlaying = true
                        }
                    }
                }
                override fun onPause() {
                    mediaPlayerRef.value?.let { mp ->
                        if (mp.isPlaying) {
                            mp.pause()
                            isPlaying = false
                        }
                    }
                }
                override fun onSeekTo(pos: Long) {
                    @Suppress("DEPRECATION")
                    mediaPlayerRef.value?.seekTo(pos.toInt())
                    currentPosition = pos
                }
            })
            setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, video.title)
                    .putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, video.thumbnailUrl)
                    .putString(MediaMetadata.METADATA_KEY_MEDIA_URI, video.url)
                    .build()
            )
            isActive = true
        }
    }

    fun updatePlaybackState() {
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_SEEK_TO or
                    PlaybackState.ACTION_PLAY_PAUSE
                )
                .setState(
                    if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    currentPosition,
                    playbackSpeed
                )
                .build()
        )
    }

    fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioManager.abandonAudioFocusRequest(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    fun toggleSubtitles() {
        val mp = mediaPlayerRef.value ?: return
        val tracks = mp.trackInfo
        var subtitleIdx = -1
        for (i in tracks.indices) {
            val type = tracks[i].trackType
            if (type == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT ||
                type == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE
            ) {
                subtitleIdx = i
                break
            }
        }
        if (subtitleIdx < 0) return
        if (subtitlesOn) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mp.deselectTrack(subtitleIdx)
                }
            }
            subtitlesOn = false
        } else {
            runCatching { mp.selectTrack(subtitleIdx) }
            subtitlesOn = true
        }
    }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    mediaPlayerRef.value?.let { mp ->
                        savedPosition = mp.currentPosition.toLong()
                        savedIsPlaying = mp.isPlaying
                        shouldRestore = true
                        if (mp.isPlaying) {
                            mp.pause()
                            isPlaying = false
                        }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    val mp = mediaPlayerRef.value
                    if (mp != null && shouldRestore) {
                        shouldRestore = false
                        if (savedPosition > 0) {
                            @Suppress("DEPRECATION")
                            mp.seekTo(savedPosition.toInt())
                        }
                        if (savedIsPlaying) {
                            mp.start()
                            isPlaying = true
                        }
                    }
                }
                else -> {}
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        requestAudioFocus()
        onDispose {
            abandonAudioFocus()
        }
    }

    DisposableEffect(mediaSession) {
        onDispose {
            mediaSession.isActive = false
            mediaSession.release()
            mediaPlayerRef.value?.release()
            mediaPlayerRef.value = null
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
                val mp = mediaPlayerRef.value
                if (mp != null) {
                    val pos = mp.currentPosition.toLong()
                    if (pos > 0 || currentPosition == 0L) {
                        currentPosition = pos
                    }
                    val currentlyPlaying = mp.isPlaying
                    if (currentlyPlaying != isPlaying) isPlaying = currentlyPlaying
                    updatePlaybackState()

                    if (!hasSubtitles && video.subtitleTracks.isNotEmpty()) {
                        val tracks = mp.trackInfo
                        for (i in tracks.indices) {
                            val type = tracks[i].trackType
                            if (type == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT ||
                                type == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE
                            ) {
                                hasSubtitles = true
                                break
                            }
                        }
                    }
                }
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
                                putExtra(Intent.EXTRA_TEXT, "${video.title}\n${video.url}")
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
                key(retryTrigger) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setOnPreparedListener { mp ->
                                    mediaPlayerRef.value = mp
                                    duration = mp.duration.toLong()
                                    errorMessage = null

                                    val allTracks = mp.trackInfo
                                    val audioTrackList = allTracks
                                        .withIndex()
                                        .filter { (_, t) ->
                                            t.trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO
                                        }
                                    audioTracks = audioTrackList.mapIndexed { idx, (infoIdx, track) ->
                                        val lang = track.format?.getString(
                                            android.media.MediaFormat.KEY_LANGUAGE
                                        )
                                        AudioTrackInfo(
                                            trackInfoIndex = infoIdx,
                                            label = buildString {
                                                append("Track ${idx + 1}")
                                                if (lang != null) append(" ($lang)")
                                            }
                                        )
                                    }

                                    for (sub in video.subtitleTracks) {
                                        val mimeType = if (sub.url.contains(".vtt") ||
                                            sub.url.contains(".webvtt")
                                        ) {
                                            "text/vtt"
                                        } else {
                                            "text/x-microdvd"
                                        }
                                        runCatching {
                                            mp.addTimedTextSource(ctx, Uri.parse(sub.url), mimeType)
                                        }
                                    }

                                    mp.setOnSeekCompleteListener {
                                        isSeeking = false
                                    }

                                    if (shouldRestore && savedPosition > 0) {
                                        shouldRestore = false
                                        @Suppress("DEPRECATION")
                                        mp.seekTo(savedPosition.toInt())
                                        if (savedIsPlaying) {
                                            mp.start()
                                            isPlaying = true
                                        }
                                    }

                                    isPrepared = true
                                }
                                setOnErrorListener { _, what, extra ->
                                    Log.e("PlayerScreen", "MediaPlayer error: what=$what extra=$extra")
                                    errorMessage = "Playback error ($what/$extra)"
                                    isPrepared = false
                                    true
                                }
                                setOnInfoListener { _, what, _ ->
                                    when (what) {
                                        MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                                            isBuffering = true
                                            true
                                        }
                                        MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                                            isBuffering = false
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                }

                                setVideoURI(Uri.parse(video.url))
                                requestFocus()
                                videoViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (isBuffering && isPrepared) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = errorMessage ?: "Playback error",
                                color = Color.White,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FilledTonalButton(
                                onClick = {
                                    errorMessage = null
                                    isPrepared = false
                                    isPlaying = false
                                    isBuffering = false
                                    currentPosition = 0L
                                    duration = 0L
                                    mediaPlayerRef.value = null
                                    videoViewRef?.stopPlayback()
                                    videoViewRef = null
                                    retryTrigger++
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f)
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry", color = Color.White)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    val mp = mediaPlayerRef.value ?: return@detectTapGestures
                                    val seekMs =
                                        if (offset.x < size.width / 2) -10000L else 10000L
                                    @Suppress("DEPRECATION")
                                    val newPos = (mp.currentPosition.toLong() + seekMs)
                                        .coerceIn(0L, duration.coerceAtLeast(0))
                                    @Suppress("DEPRECATION")
                                    mp.seekTo(newPos.toInt())
                                    seekFeedback = if (seekMs < 0) -10 else 10
                                },
                                onTap = {
                                    if (errorMessage == null) showControls = !showControls
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

                    if (showControls && errorMessage == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                        ) {
                            IconButton(
                                onClick = {
                                    val mp = mediaPlayerRef.value ?: return@IconButton
                                    if (mp.isPlaying) {
                                        mp.pause()
                                        isPlaying = false
                                    } else {
                                        mp.start()
                                        isPlaying = true
                                    }
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
                                        @Suppress("DEPRECATION")
                                        mediaPlayerRef.value?.seekTo(currentPosition.toInt())
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
                                                        mediaPlayerRef.value?.let { mp ->
                                                            mp.playbackParams = mp.playbackParams.apply {
                                                                setSpeed(speed)
                                                            }
                                                        }
                                                        showSpeedMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (audioTracks.size > 1) {
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
                                                audioTracks.forEachIndexed { dropdownIdx, audioTrack ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                audioTrack.label,
                                                                fontWeight = if (dropdownIdx == selectedAudioTrackDownIdx)
                                                                    FontWeight.Bold
                                                                else FontWeight.Normal
                                                            )
                                                        },
                                                        onClick = {
                                                            val oldIdx = selectedAudioTrackDownIdx
                                                            selectedAudioTrackDownIdx = dropdownIdx
                                                            val mp = mediaPlayerRef.value
                                                            if (mp != null) {
                                                                runCatching {
                                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                                        val oldInfoIdx = audioTracks
                                                                            .getOrNull(oldIdx)
                                                                            ?.trackInfoIndex ?: -1
                                                                        mp.deselectTrack(oldInfoIdx)
                                                                    }
                                                                    mp.selectTrack(audioTrack.trackInfoIndex)
                                                                }
                                                            }
                                                            showAudioMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (hasSubtitles || video.subtitleTracks.isNotEmpty()) {
                                        IconButton(
                                            onClick = { toggleSubtitles() }
                                        ) {
                                            Icon(
                                                Icons.Default.ClosedCaption,
                                                contentDescription = if (subtitlesOn) "Disable subtitles"
                                                else "Enable subtitles",
                                                tint = if (subtitlesOn) Color(0xFF4FC3F7)
                                                else Color.White.copy(alpha = 0.7f)
                                            )
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

                    if (video.subtitleTracks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Subtitles",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        video.subtitleTracks.forEach { sub ->
                            Text(
                                text = "${sub.label.ifBlank { sub.srclang.ifBlank { "Unknown" } }} (${sub.srclang.ifBlank { "?" }})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

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
