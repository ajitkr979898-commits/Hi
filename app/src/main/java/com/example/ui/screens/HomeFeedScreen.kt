package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FeedFilterMode
import com.example.model.FeedPostUi
import com.example.model.FeedSortMode
import com.example.model.StoryBucketUi
import com.example.model.UiState
import com.example.model.UserProfileUi
import com.example.ui.components.LumaAvatar
import com.example.ui.components.LumaBrandLogo
import com.example.ui.components.PostCard

@Composable
fun HomeFeedScreen(
    feedState: UiState<List<FeedPostUi>>,
    storyBuckets: List<StoryBucketUi>,
    ownProfile: UserProfileUi?,
    filterMode: FeedFilterMode,
    sortMode: FeedSortMode,
    onSelectFilterMode: (FeedFilterMode) -> Unit,
    onSelectSortMode: (FeedSortMode) -> Unit,
    onOpenStoryBucket: (Int) -> Unit,
    onAddStoryClick: () -> Unit,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onFollowClick: (String) -> Unit,
    onDeletePost: (String) -> Unit,
    onDiscoverClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LumaBrandLogo()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sortMode == FeedSortMode.CHRONOLOGICAL,
                        onClick = {
                            val next = if (sortMode == FeedSortMode.CHRONOLOGICAL) {
                                FeedSortMode.POPULAR
                            } else {
                                FeedSortMode.CHRONOLOGICAL
                            }
                            onSelectSortMode(next)
                        },
                        label = {
                            Text(
                                text = if (sortMode == FeedSortMode.CHRONOLOGICAL) {
                                    stringResource(R.string.sort_latest)
                                } else {
                                    stringResource(R.string.sort_popular)
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (sortMode == FeedSortMode.CHRONOLOGICAL) {
                                    Icons.Filled.Schedule
                                } else {
                                    Icons.Filled.AutoAwesome
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.testTag("feed_sort_toggle")
                    )

                    IconButton(
                        onClick = onAddStoryClick,
                        modifier = Modifier.testTag("top_add_story_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircleOutline,
                            contentDescription = stringResource(R.string.add_story),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Following vs For You Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeedSegmentPill(
                    label = stringResource(R.string.feed_following),
                    selected = filterMode == FeedFilterMode.FOLLOWING,
                    onClick = { onSelectFilterMode(FeedFilterMode.FOLLOWING) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("feed_tab_following")
                )
                FeedSegmentPill(
                    label = stringResource(R.string.feed_for_you),
                    selected = filterMode == FeedFilterMode.FOR_YOU,
                    onClick = { onSelectFilterMode(FeedFilterMode.FOR_YOU) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("feed_tab_for_you")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (feedState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = feedState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is UiState.Success -> {
                    val posts = feedState.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // 24h Expiring Stories Horizontal Bar
                        item(key = "stories_bar") {
                            StoriesRail(
                                storyBuckets = storyBuckets,
                                ownProfile = ownProfile,
                                onOpenBucket = onOpenStoryBucket,
                                onAddStoryClick = onAddStoryClick
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp
                            )
                        }

                        if (posts.isEmpty()) {
                            item(key = "empty_feed") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp, vertical = 56.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Explore,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = stringResource(R.string.empty_feed_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = stringResource(R.string.empty_feed_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = onDiscoverClick,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.testTag("empty_feed_discover_button")
                                    ) {
                                        Text(stringResource(R.string.empty_feed_cta))
                                    }
                                }
                            }
                        } else {
                            items(posts, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    onLikeClick = { onLikeClick(post.id) },
                                    onCommentClick = { onCommentClick(post.id) },
                                    onSaveClick = { onSaveClick(post.id) },
                                    onAuthorClick = { onAuthorClick(post.author.id) },
                                    onFollowClick = { onFollowClick(post.author.id) },
                                    onDeletePost = if (post.isOwnPost) {
                                        { onDeletePost(post.id) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedSegmentPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun StoriesRail(
    storyBuckets: List<StoryBucketUi>,
    ownProfile: UserProfileUi?,
    onOpenBucket: (Int) -> Unit,
    onAddStoryClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val ownBucketIdx = storyBuckets.indexOfFirst { it.isOwnBucket }
        val ownBucket = if (ownBucketIdx >= 0) storyBuckets[ownBucketIdx] else null

        item(key = "own_story_slot") {
            val avatarUri = ownBucket?.author?.avatarUri ?: ownProfile?.user?.avatarUri.orEmpty()
            val hasStories = ownBucket != null && ownBucket.stories.isNotEmpty()
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .clickable {
                        if (hasStories) onOpenBucket(ownBucketIdx) else onAddStoryClick()
                    }
                    .testTag("own_story_item"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    LumaAvatar(
                        avatarUri = avatarUri,
                        contentDescription = stringResource(R.string.your_story),
                        size = 60.dp,
                        hasActiveStory = hasStories,
                        hasUnseenStory = ownBucket?.hasUnseen == true
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.background),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(onClick = onAddStoryClick)
                            .testTag("add_story_badge")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.add_story),
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.your_story),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        itemsIndexed(storyBuckets, key = { _, b -> b.author.id }) { index, bucket ->
            if (!bucket.isOwnBucket) {
                Column(
                    modifier = Modifier
                        .width(72.dp)
                        .clickable { onOpenBucket(index) }
                        .testTag("story_bucket_${bucket.author.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LumaAvatar(
                        avatarUri = bucket.author.avatarUri,
                        contentDescription = "${bucket.author.username} story",
                        size = 60.dp,
                        hasActiveStory = true,
                        hasUnseenStory = bucket.hasUnseen
                    )
                    Text(
                        text = bucket.author.username,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
