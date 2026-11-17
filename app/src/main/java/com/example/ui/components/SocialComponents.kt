package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.FeedPostUi
import com.example.ui.theme.LumaBlue
import com.example.ui.theme.LumaRose
import com.example.ui.theme.LumaStoryGradient
import com.example.ui.theme.PacificoFamily
import kotlinx.coroutines.delay

@Composable
fun LumaBrandLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontFamily = PacificoFamily,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(LumaStoryGradient)
        )
    }
}

@Composable
fun LumaAvatar(
    avatarUri: String,
    contentDescription: String,
    size: Dp = 40.dp,
    hasActiveStory: Boolean = false,
    hasUnseenStory: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ringWidth = if (hasActiveStory) 2.5.dp else 1.dp
    val innerGap = if (hasActiveStory) 2.5.dp else 0.dp
    val ringModifier = when {
        hasActiveStory && hasUnseenStory -> Modifier.border(ringWidth, LumaStoryGradient, CircleShape)
        hasActiveStory -> Modifier.border(ringWidth, MaterialTheme.colorScheme.outline, CircleShape)
        else -> Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape)
    }

    Box(
        modifier = modifier
            .size(size + (ringWidth + innerGap) * 2)
            .clip(CircleShape)
            .then(ringModifier)
            .let { mod ->
                if (onClick != null) mod.clickable(onClick = onClick) else mod
            }
            .padding(ringWidth + innerGap),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUri)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

@Composable
fun PostCard(
    post: FeedPostUi,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onSaveClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onFollowClick: () -> Unit,
    onDeletePost: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDoubleTapHeart by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var captionExpanded by remember { mutableStateOf(false) }

    val likeScale by animateFloatAsState(
        targetValue = if (post.isLikedByMe) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "like_scale"
    )

    LaunchedEffect(showDoubleTapHeart) {
        if (showDoubleTapHeart) {
            delay(750L)
            showDoubleTapHeart = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Post Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LumaAvatar(
                avatarUri = post.author.avatarUri,
                contentDescription = "${post.author.displayName} avatar",
                size = 38.dp,
                hasActiveStory = post.author.hasActiveStory,
                hasUnseenStory = post.author.hasUnseenStory,
                onClick = onAuthorClick
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAuthorClick)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = post.author.username,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (post.author.isVerified) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Verified creator",
                            tint = LumaBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "• ${formatRelativeTime(post.createdAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (post.location.isNotBlank()) {
                    Text(
                        text = post.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!post.isOwnPost && !post.author.isFollowedByMe) {
                TextButton(
                    onClick = onFollowClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("post_follow_${post.id}")
                ) {
                    Text(
                        text = stringResource(R.string.action_follow),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("post_menu_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Post options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (!post.isOwnPost) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (post.author.isFollowedByMe) "Unfollow @${post.author.username}"
                                    else "Follow @${post.author.username}"
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onFollowClick()
                            }
                        )
                    }
                    if (post.isOwnPost && onDeletePost != null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.action_delete),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDeletePost()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_share)) },
                        onClick = {
                            menuExpanded = false
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "@${post.author.username} on Luma: ${post.caption}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share post"))
                        }
                    )
                }
            }
        }

        // Media Canvas with double-tap to like
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .aspectRatio(4f / 4.5f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(post.id, post.isLikedByMe) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (!post.isLikedByMe) onLikeClick()
                            showDoubleTapHeart = true
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(post.mediaUri)
                    .crossfade(true)
                    .build(),
                contentDescription = post.caption.ifBlank { "Photo by ${post.author.username}" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = showDoubleTapHeart,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier.size(92.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = LumaRose,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }
            }
        }

        // Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.testTag("like_button_${post.id}")
            ) {
                Icon(
                    imageVector = if (post.isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(R.string.action_like),
                    tint = if (post.isLikedByMe) LumaRose else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.scale(likeScale)
                )
            }
            if (post.likeCount > 0) {
                Text(
                    text = post.likeCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            IconButton(
                onClick = onCommentClick,
                modifier = Modifier.testTag("comment_button_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = stringResource(R.string.action_comment),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            if (post.commentCount > 0) {
                Text(
                    text = post.commentCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "@${post.author.username} on Luma: ${post.caption}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share post"))
                },
                modifier = Modifier.testTag("share_button_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = stringResource(R.string.action_share),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onSaveClick,
                modifier = Modifier.testTag("save_button_${post.id}")
            ) {
                Icon(
                    imageVector = if (post.isSavedByMe) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = stringResource(R.string.action_save),
                    tint = if (post.isSavedByMe) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Caption & Comments link
        if (post.caption.isNotBlank()) {
            val captionText = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(post.author.username)
                }
                append("  ")
                append(post.caption)
            }
            Text(
                text = captionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = if (captionExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable { captionExpanded = !captionExpanded }
            )
        }

        if (post.commentCount > 0) {
            Text(
                text = "View all ${post.commentCount} comments",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = onCommentClick)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

fun formatRelativeTime(timestampMillis: Long): String {
    val diff = (System.currentTimeMillis() - timestampMillis).coerceAtLeast(0L)
    val minutes = diff / (60L * 1000L)
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> "${days / 7}w"
    }
}

fun formatStoryExpiryRemaining(expiresAtMillis: Long): String {
    val remaining = (expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    val hours = remaining / (60L * 60L * 1000L)
    val minutes = (remaining / (60L * 1000L)) % 60L
    return when {
        hours >= 1 -> "${hours}h left"
        minutes >= 1 -> "${minutes}m left"
        else -> "Expiring"
    }
}
