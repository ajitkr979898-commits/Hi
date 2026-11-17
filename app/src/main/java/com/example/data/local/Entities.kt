package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["username"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val passwordHash: String,
    val username: String,
    val displayName: String,
    val bio: String,
    val avatarUri: String,
    val isVerified: Boolean = false,
    val createdAt: Long
)

@Entity(
    tableName = "follows",
    primaryKeys = ["followerId", "followingId"],
    indices = [Index(value = ["followingId"])]
)
data class FollowEntity(
    val followerId: String,
    val followingId: String,
    val createdAt: Long
)

@Entity(
    tableName = "posts",
    indices = [Index(value = ["authorId"]), Index(value = ["createdAt"])]
)
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val mediaUri: String,
    val caption: String,
    val location: String = "",
    val createdAt: Long
)

@Entity(
    tableName = "post_likes",
    primaryKeys = ["postId", "userId"],
    indices = [Index(value = ["userId"])]
)
data class PostLikeEntity(
    val postId: String,
    val userId: String,
    val createdAt: Long
)

@Entity(
    tableName = "saved_posts",
    primaryKeys = ["postId", "userId"],
    indices = [Index(value = ["userId"])]
)
data class SavedPostEntity(
    val postId: String,
    val userId: String,
    val createdAt: Long
)

@Entity(
    tableName = "comments",
    indices = [Index(value = ["postId"]), Index(value = ["createdAt"])]
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorId: String,
    val text: String,
    val createdAt: Long
)

@Entity(
    tableName = "stories",
    indices = [Index(value = ["authorId"]), Index(value = ["expiresAt"])]
)
data class StoryEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val mediaUri: String,
    val mediaType: String, // "IMAGE" or "VIDEO"
    val caption: String,
    val createdAt: Long,
    val expiresAt: Long
)

@Entity(
    tableName = "story_views",
    primaryKeys = ["storyId", "viewerId"],
    indices = [Index(value = ["viewerId"])]
)
data class StoryViewEntity(
    val storyId: String,
    val viewerId: String,
    val viewedAt: Long
)
