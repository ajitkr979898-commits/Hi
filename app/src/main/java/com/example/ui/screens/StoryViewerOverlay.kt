package com.example.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.StoryMediaType
import com.example.ui.components.LumaAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.formatStoryExpiryRemaining
import com.example.ui.viewmodel.StoryViewerSession
import kotlinx.coroutines.delay

@Composable
fun StoryViewerOverlay(
    session: StoryViewerSession,
    onClose: () -> Unit,
    onMarkViewed: (String) -> Unit,
    onDeleteStory: (String) -> Unit,
    onAuthorClick: (String) -> Unit
) {
    BackHandler(onBack = onClose)

    var bucketIndex by remember(session) { mutableIntStateOf(session.bucketIndex) }
    var storyIndex by remember(session) { mutableIntStateOf(session.storyIndex) }
    var progress by remember(bucketIndex, storyIndex) { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }

    val currentBucket = session.buckets.getOrNull(bucketIndex)
    val currentStory = currentBucket?.stories?.getOrNull(storyIndex)

    if (currentBucket == null || currentStory == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val goNext: () -> Unit = {
        if (storyIndex + 1 < currentBucket.stories.size) {
            storyIndex += 1
        } else if (bucketIndex + 1 < session.buckets.size) {
            bucketIndex += 1
            storyIndex = 0
        } else {
            onClose()
        }
    }

    val goPrev: () -> Unit = {
        if (storyIndex > 0) {
            storyIndex -= 1
        } else if (bucketIndex > 0) {
            bucketIndex -= 1
            val prevBucket = session.buckets[bucketIndex]
            storyIndex = (prevBucket.stories.size - 1).coerceAtLeast(0)
        } else {
            progress = 0f
        }
    }

    LaunchedEffect(currentStory.id) {
        onMarkViewed(currentStory.id)
        progress = 0f
    }

    LaunchedEffect(currentStory.id, isPaused) {
        val totalMs = 5000L
        val stepMs = 50L
        while (progress < 1f) {
            if (!isPaused) {
                delay(stepMs)
                progress = (progress + stepMs.toFloat() / totalMs).coerceAtMost(1f)
            } else {
                delay(100L)
            }
        }
        goNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
    ) {
        // Media layer
        if (currentStory.mediaType == StoryMediaType.VIDEO) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(currentStory.mediaUri))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            start()
                        }
                    }
                },
                update = { view ->
                    if (isPaused) view.pause() else if (!view.isPlaying) view.start()
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentStory.mediaUri)
                    .crossfade(true)
                    .build(),
                contentDescription = currentStory.caption.ifBlank { "Story photo" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top & Bottom scrims for legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)))
                )
        )

        // Tap navigation zones (left 35% prev, right 65% next, press & hold pauses)
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .pointerInput(currentStory.id) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = { goPrev() }
                        )
                    }
            )
            Box(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
                    .pointerInput(currentStory.id) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = { goNext() }
                        )
                    }
            )
        }

        // Top Segmented Progress Bars + Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                currentBucket.stories.forEachIndexed { idx, _ ->
                    val segProgress = when {
                        idx < storyIndex -> 1f
                        idx == storyIndex -> progress
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segProgress },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LumaAvatar(
                    avatarUri = currentBucket.author.avatarUri,
                    contentDescription = currentBucket.author.username,
                    size = 36.dp,
                    onClick = {
                        onClose()
                        onAuthorClick(currentBucket.author.id)
                    }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = currentBucket.author.username,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = formatRelativeTime(currentStory.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                    Text(
                        text = "24h Story • ${formatStoryExpiryRemaining(currentStory.expiresAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }

                if (currentStory.isOwnStory) {
                    IconButton(
                        onClick = { onDeleteStory(currentStory.id) },
                        modifier = Modifier.testTag("delete_story_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete story",
                            tint = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_story_viewer")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close story",
                        tint = Color.White
                    )
                }
            }
        }

        // Caption Overlay
        if (currentStory.caption.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Text(
                    text = currentStory.caption,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}
