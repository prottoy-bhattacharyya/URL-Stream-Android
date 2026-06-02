package com.urlstream.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.WindowManager
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.urlstream.model.VideoInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    video: VideoInfo,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var isFullscreen by remember { mutableStateOf(false) }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
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
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
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
                        VideoView(ctx).apply {
                            val controller = MediaController(ctx)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setVideoURI(Uri.parse(video.url))
                            requestFocus()
                            start()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { isFullscreen = !isFullscreen },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            if (isFullscreen) Icons.Default.FullscreenExit
                            else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "Exit fullscreen"
                            else "Enter fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (!isFullscreen) {
                        IconButton(
                            onClick = { playWithExternalApp(context, video.url) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.6f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = "Play with external player",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
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
                        Text("Play with another app", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
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
