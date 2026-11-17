package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.data.repository.LumaRepository
import com.example.model.CommentUi
import com.example.model.FeedFilterMode
import com.example.model.FeedPostUi
import com.example.model.FeedSortMode
import com.example.model.StoryBucketUi
import com.example.model.UiState
import com.example.model.UserProfileUi
import com.example.model.UserSummaryUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StoryViewerSession(
    val buckets: List<StoryBucketUi>,
    val bucketIndex: Int,
    val storyIndex: Int = 0
)

data class FollowListRequest(
    val userId: String,
    val showFollowers: Boolean // true = followers, false = following
)

@OptIn(ExperimentalCoroutinesApi::class)
class LumaViewModel(
    private val repository: LumaRepository,
    val currentUserId: String
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = repository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val feedFilterMode: StateFlow<FeedFilterMode> = repository.feedFilterMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), FeedFilterMode.FOLLOWING)

    val feedSortMode: StateFlow<FeedSortMode> = repository.feedSortMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), FeedSortMode.CHRONOLOGICAL)

    val storyBuckets: StateFlow<List<StoryBucketUi>> =
        repository.observeActiveStoriesAndViews(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val allPostsFlow = repository.observeAllPostsUi(currentUserId)

    val feedPostsState: StateFlow<UiState<List<FeedPostUi>>> = combine(
        allPostsFlow,
        repository.feedFilterMode,
        repository.feedSortMode
    ) { posts, filterMode, sortMode ->
        val filtered = when (filterMode) {
            FeedFilterMode.FOLLOWING -> posts.filter { it.author.isFollowedByMe || it.isOwnPost }
            FeedFilterMode.FOR_YOU -> posts
        }
        val sorted = when (sortMode) {
            FeedSortMode.CHRONOLOGICAL -> filtered.sortedByDescending { it.createdAt }
            FeedSortMode.POPULAR -> filtered.sortedByDescending {
                (it.likeCount * 2) + (it.commentCount * 3)
            }
        }
        UiState.Success(sorted) as UiState<List<FeedPostUi>>
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val exploreUsers: StateFlow<List<UserSummaryUi>> = combine(
        repository.observeAllUserSummaries(currentUserId),
        _searchQuery
    ) { users, query ->
        val q = query.trim().lowercase()
        users.filter { u ->
            u.id != currentUserId && (
                q.isBlank() ||
                    u.username.lowercase().contains(q) ||
                    u.displayName.lowercase().contains(q) ||
                    u.bio.lowercase().contains(q)
                )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val explorePosts: StateFlow<List<FeedPostUi>> = combine(
        allPostsFlow,
        _searchQuery
    ) { posts, query ->
        val q = query.trim().lowercase()
        if (q.isBlank()) posts
        else posts.filter { p ->
            p.caption.lowercase().contains(q) ||
                p.location.lowercase().contains(q) ||
                p.author.username.lowercase().contains(q)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val ownProfile: StateFlow<UserProfileUi?> =
        repository.observeUserProfile(currentUserId, currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val ownPosts: StateFlow<List<FeedPostUi>> = allPostsFlow
        .map { posts -> posts.filter { it.author.id == currentUserId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val savedPosts: StateFlow<List<FeedPostUi>> = allPostsFlow
        .map { posts -> posts.filter { it.isSavedByMe } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _inspectedUserId = MutableStateFlow<String?>(null)
    val inspectedUserProfile: StateFlow<UserProfileUi?> = _inspectedUserId
        .flatMapLatest { uid ->
            if (uid == null) flowOf(null)
            else repository.observeUserProfile(uid, currentUserId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val inspectedUserPosts: StateFlow<List<FeedPostUi>> = combine(
        allPostsFlow,
        _inspectedUserId
    ) { posts, uid ->
        if (uid == null) emptyList() else posts.filter { it.author.id == uid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _activeCommentsPostId = MutableStateFlow<String?>(null)
    val activeCommentsPost: StateFlow<FeedPostUi?> = combine(
        allPostsFlow,
        _activeCommentsPostId
    ) { posts, postId ->
        if (postId == null) null else posts.firstOrNull { it.id == postId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val commentsForActivePost: StateFlow<List<CommentUi>> = _activeCommentsPostId
        .flatMapLatest { postId ->
            if (postId == null) flowOf(emptyList())
            else repository.observeCommentsForPost(postId, currentUserId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedPostDetailId = MutableStateFlow<String?>(null)
    val selectedPostDetail: StateFlow<FeedPostUi?> = combine(
        allPostsFlow,
        _selectedPostDetailId
    ) { posts, id ->
        if (id == null) null else posts.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _followListRequest = MutableStateFlow<FollowListRequest?>(null)
    val followListRequest: StateFlow<FollowListRequest?> = _followListRequest.asStateFlow()

    val followListUsers: StateFlow<List<UserSummaryUi>> = _followListRequest
        .flatMapLatest { req ->
            when {
                req == null -> flowOf(emptyList())
                req.showFollowers -> repository.observeFollowersList(req.userId, currentUserId)
                else -> repository.observeFollowingList(req.userId, currentUserId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _storyViewer = MutableStateFlow<StoryViewerSession?>(null)
    val storyViewer: StateFlow<StoryViewerSession?> = _storyViewer.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFeedFilterMode(mode: FeedFilterMode) {
        viewModelScope.launch { repository.setFeedFilterMode(mode) }
    }

    fun setFeedSortMode(mode: FeedSortMode) {
        viewModelScope.launch { repository.setFeedSortMode(mode) }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkMode(enabled) }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch { repository.toggleLike(postId, currentUserId) }
    }

    fun toggleSave(postId: String) {
        viewModelScope.launch { repository.toggleSave(postId, currentUserId) }
    }

    fun toggleFollow(targetUserId: String) {
        viewModelScope.launch { repository.toggleFollow(currentUserId, targetUserId) }
    }

    fun openComments(postId: String) {
        _activeCommentsPostId.value = postId
    }

    fun closeComments() {
        _activeCommentsPostId.value = null
    }

    fun addComment(postId: String, text: String) {
        viewModelScope.launch {
            repository.addComment(postId, currentUserId, text).onFailure { err ->
                _statusMessage.value = err.message ?: "Could not post comment."
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch { repository.deleteComment(commentId, currentUserId) }
    }

    fun openPostDetail(postId: String) {
        _selectedPostDetailId.value = postId
    }

    fun closePostDetail() {
        _selectedPostDetailId.value = null
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId, currentUserId)
            if (_selectedPostDetailId.value == postId) _selectedPostDetailId.value = null
            _statusMessage.value = "Post deleted"
        }
    }

    fun inspectUser(userId: String) {
        _inspectedUserId.value = userId
    }

    fun openFollowList(userId: String, showFollowers: Boolean) {
        _followListRequest.value = FollowListRequest(userId, showFollowers)
    }

    fun closeFollowList() {
        _followListRequest.value = null
    }

    fun openStoryBucket(bucketIndex: Int) {
        val bucketsWithStories = storyBuckets.value.filter { it.stories.isNotEmpty() }
        val targetBucket = storyBuckets.value.getOrNull(bucketIndex) ?: return
        val filteredIndex = bucketsWithStories.indexOfFirst { it.author.id == targetBucket.author.id }
        if (filteredIndex >= 0) {
            _storyViewer.value = StoryViewerSession(buckets = bucketsWithStories, bucketIndex = filteredIndex)
        }
    }

    fun openUserStories(userId: String) {
        val fromFeed = storyBuckets.value.filter { it.stories.isNotEmpty() }
        val idx = fromFeed.indexOfFirst { it.author.id == userId }
        if (idx >= 0) {
            _storyViewer.value = StoryViewerSession(buckets = fromFeed, bucketIndex = idx)
            return
        }
        viewModelScope.launch {
            repository.observeActiveStoriesForUser(userId, currentUserId).collect { bucket ->
                if (bucket != null && bucket.stories.isNotEmpty()) {
                    _storyViewer.value = StoryViewerSession(buckets = listOf(bucket), bucketIndex = 0)
                }
                return@collect
            }
        }
    }

    fun closeStoryViewer() {
        _storyViewer.value = null
    }

    fun markStoryViewed(storyId: String) {
        viewModelScope.launch { repository.markStoryViewed(storyId, currentUserId) }
    }

    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            repository.deleteStory(storyId, currentUserId)
            _storyViewer.value = null
            _statusMessage.value = "Story removed"
        }
    }

    fun publishPost(
        mediaUri: Uri,
        caption: String,
        location: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.createPost(currentUserId, mediaUri, caption, location).fold(
                onSuccess = {
                    _statusMessage.value = "Post shared to your feed"
                    onSuccess()
                },
                onFailure = { err ->
                    _statusMessage.value = err.message ?: "Failed to share post."
                }
            )
        }
    }

    fun publishStory(
        mediaUri: Uri,
        caption: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.createStory(currentUserId, mediaUri, caption).fold(
                onSuccess = {
                    _statusMessage.value = "Story live for 24 hours"
                    onSuccess()
                },
                onFailure = { err ->
                    _statusMessage.value = err.message ?: "Failed to share story."
                }
            )
        }
    }

    fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        newAvatarUri: Uri?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateProfile(currentUserId, displayName, username, bio, newAvatarUri).fold(
                onSuccess = {
                    _statusMessage.value = "Profile updated"
                    onSuccess()
                },
                onFailure = { err ->
                    _statusMessage.value = err.message ?: "Could not update profile."
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch { repository.signOut() }
    }

    companion object {
        fun factory(repository: LumaRepository, currentUserId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    return LumaViewModel(repository, currentUserId) as T
                }
            }
    }
}
