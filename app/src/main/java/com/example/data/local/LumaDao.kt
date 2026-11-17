package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LumaDao {

    @Query("SELECT * FROM users ORDER BY createdAt ASC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query(
        "UPDATE users SET displayName = :displayName, username = :username, bio = :bio, avatarUri = :avatarUri WHERE id = :userId"
    )
    suspend fun updateUserProfile(
        userId: String,
        displayName: String,
        username: String,
        bio: String,
        avatarUri: String
    )

    @Query("SELECT * FROM follows")
    fun observeAllFollows(): Flow<List<FollowEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFollow(follow: FollowEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFollows(follows: List<FollowEntity>)

    @Query("DELETE FROM follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun deleteFollow(followerId: String, followingId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE followerId = :followerId AND followingId = :followingId)")
    suspend fun isFollowing(followerId: String, followingId: String): Boolean

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun observeAllPosts(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE id = :postId AND authorId = :authorId")
    suspend fun deletePost(postId: String, authorId: String)

    @Query("SELECT * FROM post_likes")
    fun observeAllLikes(): Flow<List<PostLikeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLike(like: PostLikeEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLikes(likes: List<PostLikeEntity>)

    @Query("DELETE FROM post_likes WHERE postId = :postId AND userId = :userId")
    suspend fun deleteLike(postId: String, userId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM post_likes WHERE postId = :postId AND userId = :userId)")
    suspend fun isPostLiked(postId: String, userId: String): Boolean

    @Query("SELECT * FROM saved_posts")
    fun observeAllSavedPosts(): Flow<List<SavedPostEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSavedPost(savedPost: SavedPostEntity)

    @Query("DELETE FROM saved_posts WHERE postId = :postId AND userId = :userId")
    suspend fun deleteSavedPost(postId: String, userId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_posts WHERE postId = :postId AND userId = :userId)")
    suspend fun isPostSaved(postId: String, userId: String): Boolean

    @Query("SELECT * FROM comments ORDER BY createdAt ASC")
    fun observeAllComments(): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)

    @Query("DELETE FROM comments WHERE id = :commentId AND authorId = :authorId")
    suspend fun deleteComment(commentId: String, authorId: String)

    @Query("SELECT * FROM stories WHERE expiresAt > :nowMillis ORDER BY createdAt ASC")
    fun observeActiveStories(nowMillis: Long): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StoryEntity>)

    @Query("DELETE FROM stories WHERE id = :storyId AND authorId = :authorId")
    suspend fun deleteStory(storyId: String, authorId: String)

    @Query("DELETE FROM stories WHERE expiresAt <= :nowMillis")
    suspend fun deleteExpiredStories(nowMillis: Long)

    @Query("SELECT * FROM story_views WHERE viewerId = :viewerId")
    fun observeStoryViewsByViewer(viewerId: String): Flow<List<StoryViewEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStoryView(view: StoryViewEntity)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int
}
