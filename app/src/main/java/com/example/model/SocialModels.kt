package com.example.model

enum class FeedFilterMode {
    FOLLOWING,
    FOR_YOU
}

enum class FeedSortMode {
    CHRONOLOGICAL,
    POPULAR
}

enum class StoryMediaType {
    IMAGE,
    VIDEO
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class UserSummaryUi(
    val id: String,
    val username: String,
    val displayName: String,
    val bio: String,
    val avatarUri: String,
    val isVerified: Boolean,
    val isFollowedByMe: Boolean,
    val hasActiveStory: Boolean,
    val hasUnseenStory: Boolean
)

data class UserProfileUi(
    val user: UserSummaryUi,
    val email: String,
    val postCount: Int,
    val followerCount: Int,
    val followingCount: Int,
    val isOwnProfile: Boolean
)

data class FeedPostUi(
    val id: String,
    val author: UserSummaryUi,
    val mediaUri: String,
    val caption: String,
    val location: String,
    val createdAt: Long,
    val likeCount: Int,
    val commentCount: Int,
    val isLikedByMe: Boolean,
    val isSavedByMe: Boolean,
    val isOwnPost: Boolean
)

data class CommentUi(
    val id: String,
    val postId: String,
    val author: UserSummaryUi,
    val text: String,
    val createdAt: Long,
    val isOwnComment: Boolean
)

data class StoryItemUi(
    val id: String,
    val authorId: String,
    val mediaUri: String,
    val mediaType: StoryMediaType,
    val caption: String,
    val createdAt: Long,
    val expiresAt: Long,
    val isViewed: Boolean,
    val isOwnStory: Boolean
)

data class StoryBucketUi(
    val author: UserSummaryUi,
    val stories: List<StoryItemUi>,
    val hasUnseen: Boolean,
    val isOwnBucket: Boolean
)
