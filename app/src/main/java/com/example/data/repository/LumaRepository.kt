package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.R
import com.example.data.local.CommentEntity
import com.example.data.local.FollowEntity
import com.example.data.local.LumaDao
import com.example.data.local.LumaDatabase
import com.example.data.local.PostEntity
import com.example.data.local.PostLikeEntity
import com.example.data.local.SavedPostEntity
import com.example.data.local.SeedData
import com.example.data.local.SessionPreferences
import com.example.data.local.StoryEntity
import com.example.data.local.StoryViewEntity
import com.example.data.local.UserEntity
import com.example.model.CommentUi
import com.example.model.FeedFilterMode
import com.example.model.FeedPostUi
import com.example.model.FeedSortMode
import com.example.model.StoryBucketUi
import com.example.model.StoryItemUi
import com.example.model.StoryMediaType
import com.example.model.UserProfileUi
import com.example.model.UserSummaryUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class LumaRepository(
    private val appContext: Context,
    private val dao: LumaDao,
    private val prefs: SessionPreferences
) {
    constructor(context: Context) : this(
        appContext = context.applicationContext,
        dao = LumaDatabase.getInstance(context).lumaDao(),
        prefs = SessionPreferences(context.applicationContext)
    )

    val currentUserId: Flow<String?> = prefs.currentUserId
    val isDarkMode: Flow<Boolean> = prefs.isDarkMode
    val feedSortMode: Flow<FeedSortMode> = prefs.feedSortMode
    val feedFilterMode: Flow<FeedFilterMode> = prefs.feedFilterMode

    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            SeedData.ensureSeeded(appContext, dao)
            dao.deleteExpiredStories(System.currentTimeMillis())
        }
    }

    suspend fun login(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.length < 4) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
        }
        SeedData.ensureSeeded(appContext, dao)
        val user = dao.getUserByEmail(cleanEmail)
            ?: return@withContext Result.failure(IllegalArgumentException("No account found for $cleanEmail."))
        if (user.passwordHash != SeedData.hashPassword(password)) {
            return@withContext Result.failure(IllegalArgumentException("Incorrect password."))
        }
        prefs.setCurrentUserId(user.id)
        Result.success(user)
    }

    suspend fun loginDemoAccount(): Result<UserEntity> = withContext(Dispatchers.IO) {
        SeedData.ensureSeeded(appContext, dao)
        val demo = dao.getUserByEmail("alex@luma.app")
            ?: return@withContext Result.failure(IllegalStateException("Demo profile unavailable."))
        prefs.setCurrentUserId(demo.id)
        Result.success(demo)
    }

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        username: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanName = displayName.trim()
        val cleanHandle = username.trim().lowercase().removePrefix("@").replace(" ", "_")
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (cleanName.length < 2) {
            return@withContext Result.failure(IllegalArgumentException("Enter your display name."))
        }
        if (!cleanHandle.matches(Regex("^[a-z0-9._]{3,24}$"))) {
            return@withContext Result.failure(
                IllegalArgumentException("Username must be 3–24 characters (letters, numbers, . or _).")
            )
        }
        SeedData.ensureSeeded(appContext, dao)
        if (dao.getUserByEmail(cleanEmail) != null) {
            return@withContext Result.failure(IllegalArgumentException("An account with this email already exists."))
        }
        if (dao.getUserByUsername(cleanHandle) != null) {
            return@withContext Result.failure(IllegalArgumentException("@$cleanHandle is already taken."))
        }

        val now = System.currentTimeMillis()
        val newUser = UserEntity(
            id = "user_${UUID.randomUUID().toString().take(8)}",
            email = cleanEmail,
            passwordHash = SeedData.hashPassword(password),
            username = cleanHandle,
            displayName = cleanName,
            bio = "New on Luma ✨",
            avatarUri = SeedData.resUri(appContext, R.drawable.img_sample_portrait),
            isVerified = false,
            createdAt = now
        )
        dao.insertUser(newUser)
        // Follow Maya by default so the home feed isn't empty on first launch, while leaving others discoverable
        dao.insertFollow(FollowEntity(newUser.id, "user_maya", now))
        prefs.setCurrentUserId(newUser.id)
        Result.success(newUser)
    }

    suspend fun signOut() {
        prefs.setCurrentUserId(null)
    }

    suspend fun setDarkMode(enabled: Boolean) {
        prefs.setDarkMode(enabled)
    }

    suspend fun setFeedSortMode(mode: FeedSortMode) {
        prefs.setFeedSortMode(mode)
    }

    suspend fun setFeedFilterMode(mode: FeedFilterMode) {
        prefs.setFeedFilterMode(mode)
    }

    fun observeActiveStoriesAndViews(viewerId: String): Flow<List<StoryBucketUi>> {
        val now = System.currentTimeMillis()
        val baseFlow = combine(
            dao.observeAllUsers(),
            dao.observeAllFollows(),
            dao.observeActiveStories(now),
            dao.observeStoryViewsByViewer(viewerId)
        ) { users, follows, stories, views ->
            val validStories = stories.filter { it.expiresAt > System.currentTimeMillis() }
            val viewedIds = views.map { it.storyId }.toSet()
            val followingIds = follows.filter { it.followerId == viewerId }.map { it.followingId }.toSet()
            val userMap = users.associateBy { it.id }

            val grouped = validStories
                .filter { it.authorId == viewerId || it.authorId in followingIds }
                .groupBy { it.authorId }

            val buckets = mutableListOf<StoryBucketUi>()
            val ownStories = grouped[viewerId].orEmpty()
            userMap[viewerId]?.let { me ->
                val items = ownStories.map { s ->
                    s.toUi(isViewed = s.id in viewedIds, isOwnStory = true)
                }
                buckets.add(
                    StoryBucketUi(
                        author = me.toSummary(
                            isFollowedByMe = false,
                            hasActiveStory = items.isNotEmpty(),
                            hasUnseenStory = items.any { !it.isViewed }
                        ),
                        stories = items,
                        hasUnseen = items.any { !it.isViewed },
                        isOwnBucket = true
                    )
                )
            }

            grouped.filterKeys { it != viewerId }.forEach { (authorId, authorStories) ->
                val author = userMap[authorId] ?: return@forEach
                val items = authorStories.map { s ->
                    s.toUi(isViewed = s.id in viewedIds, isOwnStory = false)
                }
                val hasUnseen = items.any { !it.isViewed }
                buckets.add(
                    StoryBucketUi(
                        author = author.toSummary(
                            isFollowedByMe = true,
                            hasActiveStory = true,
                            hasUnseenStory = hasUnseen
                        ),
                        stories = items,
                        hasUnseen = hasUnseen,
                        isOwnBucket = false
                    )
                )
            }

            // Sort: Own bucket first, then unseen buckets, then seen buckets
            val own = buckets.filter { it.isOwnBucket }
            val others = buckets.filter { !it.isOwnBucket }.sortedByDescending { it.hasUnseen }
            own + others
        }
        return baseFlow
    }

    fun observeAllPostsUi(viewerId: String): Flow<List<FeedPostUi>> {
        val coreFlow = combine(
            dao.observeAllUsers(),
            dao.observeAllFollows(),
            dao.observeAllPosts(),
            dao.observeActiveStories(System.currentTimeMillis()),
            dao.observeStoryViewsByViewer(viewerId)
        ) { users, follows, posts, stories, views ->
            CoreSnapshot(users, follows, posts, stories, views)
        }

        val engagementFlow = combine(
            dao.observeAllLikes(),
            dao.observeAllSavedPosts(),
            dao.observeAllComments()
        ) { likes, saves, comments ->
            EngagementSnapshot(likes, saves, comments)
        }

        return combine(coreFlow, engagementFlow) { core, eng ->
            val followingIds = core.follows.filter { it.followerId == viewerId }.map { it.followingId }.toSet()
            val viewedStoryIds = core.views.map { it.storyId }.toSet()
            val activeStoriesByAuthor = core.stories
                .filter { it.expiresAt > System.currentTimeMillis() }
                .groupBy { it.authorId }

            val userSummaries = core.users.associate { u ->
                val userStories = activeStoriesByAuthor[u.id].orEmpty()
                u.id to u.toSummary(
                    isFollowedByMe = u.id in followingIds,
                    hasActiveStory = userStories.isNotEmpty(),
                    hasUnseenStory = userStories.any { it.id !in viewedStoryIds }
                )
            }

            val likesByPost = eng.likes.groupBy { it.postId }
            val savesByPost = eng.saves.filter { it.userId == viewerId }.map { it.postId }.toSet()
            val commentsByPost = eng.comments.groupBy { it.postId }

            core.posts.mapNotNull { post ->
                val author = userSummaries[post.authorId] ?: return@mapNotNull null
                val postLikes = likesByPost[post.id].orEmpty()
                FeedPostUi(
                    id = post.id,
                    author = author,
                    mediaUri = post.mediaUri,
                    caption = post.caption,
                    location = post.location,
                    createdAt = post.createdAt,
                    likeCount = postLikes.size,
                    commentCount = commentsByPost[post.id]?.size ?: 0,
                    isLikedByMe = postLikes.any { it.userId == viewerId },
                    isSavedByMe = post.id in savesByPost,
                    isOwnPost = post.authorId == viewerId
                )
            }
        }
    }

    fun observeAllUserSummaries(viewerId: String): Flow<List<UserSummaryUi>> {
        return combine(
            dao.observeAllUsers(),
            dao.observeAllFollows(),
            dao.observeActiveStories(System.currentTimeMillis()),
            dao.observeStoryViewsByViewer(viewerId)
        ) { users, follows, stories, views ->
            val followingIds = follows.filter { it.followerId == viewerId }.map { it.followingId }.toSet()
            val viewedStoryIds = views.map { it.storyId }.toSet()
            val activeStoriesByAuthor = stories
                .filter { it.expiresAt > System.currentTimeMillis() }
                .groupBy { it.authorId }

            users.map { u ->
                val userStories = activeStoriesByAuthor[u.id].orEmpty()
                u.toSummary(
                    isFollowedByMe = u.id in followingIds,
                    hasActiveStory = userStories.isNotEmpty(),
                    hasUnseenStory = userStories.any { it.id !in viewedStoryIds }
                )
            }
        }
    }

    fun observeUserProfile(targetUserId: String, viewerId: String): Flow<UserProfileUi?> {
        return combine(
            dao.observeAllUsers(),
            dao.observeAllFollows(),
            dao.observeAllPosts(),
            dao.observeActiveStories(System.currentTimeMillis()),
            dao.observeStoryViewsByViewer(viewerId)
        ) { users, follows, posts, stories, views ->
            val target = users.firstOrNull { it.id == targetUserId } ?: return@combine null
            val postCount = posts.count { it.authorId == targetUserId }
            val followerCount = follows.count { it.followingId == targetUserId }
            val followingCount = follows.count { it.followerId == targetUserId }
            val isFollowedByMe = follows.any { it.followerId == viewerId && it.followingId == targetUserId }
            val viewedIds = views.map { it.storyId }.toSet()
            val userStories = stories.filter {
                it.authorId == targetUserId && it.expiresAt > System.currentTimeMillis()
            }

            UserProfileUi(
                user = target.toSummary(
                    isFollowedByMe = isFollowedByMe,
                    hasActiveStory = userStories.isNotEmpty(),
                    hasUnseenStory = userStories.any { it.id !in viewedIds }
                ),
                email = target.email,
                postCount = postCount,
                followerCount = followerCount,
                followingCount = followingCount,
                isOwnProfile = targetUserId == viewerId
            )
        }
    }

    fun observeFollowersList(targetUserId: String, viewerId: String): Flow<List<UserSummaryUi>> {
        return combine(
            observeAllUserSummaries(viewerId),
            dao.observeAllFollows()
        ) { summaries, follows ->
            val followerIds = follows.filter { it.followingId == targetUserId }.map { it.followerId }.toSet()
            summaries.filter { it.id in followerIds }
        }
    }

    fun observeFollowingList(targetUserId: String, viewerId: String): Flow<List<UserSummaryUi>> {
        return combine(
            observeAllUserSummaries(viewerId),
            dao.observeAllFollows()
        ) { summaries, follows ->
            val followingIds = follows.filter { it.followerId == targetUserId }.map { it.followingId }.toSet()
            summaries.filter { it.id in followingIds }
        }
    }

    fun observeCommentsForPost(postId: String, viewerId: String): Flow<List<CommentUi>> {
        return combine(
            dao.observeAllComments(),
            observeAllUserSummaries(viewerId)
        ) { comments, users ->
            val userMap = users.associateBy { it.id }
            comments
                .filter { it.postId == postId }
                .mapNotNull { c ->
                    val author = userMap[c.authorId] ?: return@mapNotNull null
                    CommentUi(
                        id = c.id,
                        postId = c.postId,
                        author = author,
                        text = c.text,
                        createdAt = c.createdAt,
                        isOwnComment = c.authorId == viewerId
                    )
                }
        }
    }

    fun observeActiveStoriesForUser(targetUserId: String, viewerId: String): Flow<StoryBucketUi?> {
        return combine(
            dao.observeAllUsers(),
            dao.observeAllFollows(),
            dao.observeActiveStories(System.currentTimeMillis()),
            dao.observeStoryViewsByViewer(viewerId)
        ) { users, follows, stories, views ->
            val user = users.firstOrNull { it.id == targetUserId } ?: return@combine null
            val viewedIds = views.map { it.storyId }.toSet()
            val items = stories
                .filter { it.authorId == targetUserId && it.expiresAt > System.currentTimeMillis() }
                .map { it.toUi(isViewed = it.id in viewedIds, isOwnStory = targetUserId == viewerId) }
            if (items.isEmpty()) return@combine null
            val isFollowed = follows.any { it.followerId == viewerId && it.followingId == targetUserId }
            StoryBucketUi(
                author = user.toSummary(
                    isFollowedByMe = isFollowed,
                    hasActiveStory = true,
                    hasUnseenStory = items.any { !it.isViewed }
                ),
                stories = items,
                hasUnseen = items.any { !it.isViewed },
                isOwnBucket = targetUserId == viewerId
            )
        }
    }

    suspend fun toggleFollow(followerId: String, targetUserId: String) = withContext(Dispatchers.IO) {
        if (followerId == targetUserId) return@withContext
        if (dao.isFollowing(followerId, targetUserId)) {
            dao.deleteFollow(followerId, targetUserId)
        } else {
            dao.insertFollow(FollowEntity(followerId, targetUserId, System.currentTimeMillis()))
        }
    }

    suspend fun toggleLike(postId: String, userId: String) = withContext(Dispatchers.IO) {
        if (dao.isPostLiked(postId, userId)) {
            dao.deleteLike(postId, userId)
        } else {
            dao.insertLike(PostLikeEntity(postId, userId, System.currentTimeMillis()))
        }
    }

    suspend fun toggleSave(postId: String, userId: String) = withContext(Dispatchers.IO) {
        if (dao.isPostSaved(postId, userId)) {
            dao.deleteSavedPost(postId, userId)
        } else {
            dao.insertSavedPost(SavedPostEntity(postId, userId, System.currentTimeMillis()))
        }
    }

    suspend fun addComment(postId: String, authorId: String, text: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val clean = text.trim()
            if (clean.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Comment cannot be empty."))
            }
            dao.insertComment(
                CommentEntity(
                    id = "cmt_${UUID.randomUUID().toString().take(8)}",
                    postId = postId,
                    authorId = authorId,
                    text = clean,
                    createdAt = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        }

    suspend fun deleteComment(commentId: String, authorId: String) = withContext(Dispatchers.IO) {
        dao.deleteComment(commentId, authorId)
    }

    suspend fun createPost(
        authorId: String,
        sourceUri: Uri,
        caption: String,
        location: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val persistedUri = copyUriToLocalMedia(sourceUri, "post")
            ?: return@withContext Result.failure(IllegalStateException("Could not save selected media."))
        dao.insertPost(
            PostEntity(
                id = "post_${UUID.randomUUID().toString().take(8)}",
                authorId = authorId,
                mediaUri = persistedUri,
                caption = caption.trim(),
                location = location.trim(),
                createdAt = System.currentTimeMillis()
            )
        )
        Result.success(Unit)
    }

    suspend fun deletePost(postId: String, authorId: String) = withContext(Dispatchers.IO) {
        dao.deletePost(postId, authorId)
    }

    suspend fun createStory(
        authorId: String,
        sourceUri: Uri,
        caption: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val mime = appContext.contentResolver.getType(sourceUri).orEmpty()
        val isVideo = mime.startsWith("video/")
        val persistedUri = copyUriToLocalMedia(sourceUri, "story")
            ?: return@withContext Result.failure(IllegalStateException("Could not save story media."))
        val now = System.currentTimeMillis()
        val twentyFourHoursMs = 24L * 60L * 60L * 1000L
        dao.insertStory(
            StoryEntity(
                id = "story_${UUID.randomUUID().toString().take(8)}",
                authorId = authorId,
                mediaUri = persistedUri,
                mediaType = if (isVideo) "VIDEO" else "IMAGE",
                caption = caption.trim(),
                createdAt = now,
                expiresAt = now + twentyFourHoursMs
            )
        )
        Result.success(Unit)
    }

    suspend fun markStoryViewed(storyId: String, viewerId: String) = withContext(Dispatchers.IO) {
        dao.insertStoryView(StoryViewEntity(storyId, viewerId, System.currentTimeMillis()))
    }

    suspend fun deleteStory(storyId: String, authorId: String) = withContext(Dispatchers.IO) {
        dao.deleteStory(storyId, authorId)
    }

    suspend fun updateProfile(
        userId: String,
        displayName: String,
        username: String,
        bio: String,
        newAvatarUri: Uri?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanName = displayName.trim()
        val cleanHandle = username.trim().lowercase().removePrefix("@").replace(" ", "_")
        if (cleanName.length < 2) {
            return@withContext Result.failure(IllegalArgumentException("Display name must be at least 2 characters."))
        }
        if (!cleanHandle.matches(Regex("^[a-z0-9._]{3,24}$"))) {
            return@withContext Result.failure(
                IllegalArgumentException("Username must be 3–24 characters (letters, numbers, . or _).")
            )
        }
        val existingByHandle = dao.getUserByUsername(cleanHandle)
        if (existingByHandle != null && existingByHandle.id != userId) {
            return@withContext Result.failure(IllegalArgumentException("@$cleanHandle is already taken."))
        }
        val currentUser = dao.getUserById(userId)
            ?: return@withContext Result.failure(IllegalStateException("User not found."))
        val finalAvatar = if (newAvatarUri != null) {
            copyUriToLocalMedia(newAvatarUri, "avatar") ?: currentUser.avatarUri
        } else {
            currentUser.avatarUri
        }
        dao.updateUserProfile(
            userId = userId,
            displayName = cleanName,
            username = cleanHandle,
            bio = bio.trim().take(160),
            avatarUri = finalAvatar
        )
        Result.success(Unit)
    }

    private fun copyUriToLocalMedia(uri: Uri, prefix: String): String? {
        return try {
            if (uri.scheme == "android.resource" || uri.scheme == "file") {
                return uri.toString()
            }
            val resolver = appContext.contentResolver
            val mime = resolver.getType(uri)
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
            val mediaDir = File(appContext.filesDir, "media").apply { mkdirs() }
            val destFile = File(mediaDir, "${prefix}_${UUID.randomUUID().toString().take(8)}.$ext")
            resolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            Uri.fromFile(destFile).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun UserEntity.toSummary(
        isFollowedByMe: Boolean,
        hasActiveStory: Boolean,
        hasUnseenStory: Boolean
    ) = UserSummaryUi(
        id = id,
        username = username,
        displayName = displayName,
        bio = bio,
        avatarUri = avatarUri,
        isVerified = isVerified,
        isFollowedByMe = isFollowedByMe,
        hasActiveStory = hasActiveStory,
        hasUnseenStory = hasUnseenStory
    )

    private fun StoryEntity.toUi(isViewed: Boolean, isOwnStory: Boolean) = StoryItemUi(
        id = id,
        authorId = authorId,
        mediaUri = mediaUri,
        mediaType = if (mediaType == "VIDEO") StoryMediaType.VIDEO else StoryMediaType.IMAGE,
        caption = caption,
        createdAt = createdAt,
        expiresAt = expiresAt,
        isViewed = isViewed,
        isOwnStory = isOwnStory
    )

    private data class CoreSnapshot(
        val users: List<UserEntity>,
        val follows: List<FollowEntity>,
        val posts: List<PostEntity>,
        val stories: List<StoryEntity>,
        val views: List<StoryViewEntity>
    )

    private data class EngagementSnapshot(
        val likes: List<PostLikeEntity>,
        val saves: List<SavedPostEntity>,
        val comments: List<CommentEntity>
    )
}
