package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.R
import java.security.MessageDigest

@Database(
    entities = [
        UserEntity::class,
        FollowEntity::class,
        PostEntity::class,
        PostLikeEntity::class,
        SavedPostEntity::class,
        CommentEntity::class,
        StoryEntity::class,
        StoryViewEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LumaDatabase : RoomDatabase() {
    abstract fun lumaDao(): LumaDao

    companion object {
        @Volatile
        private var INSTANCE: LumaDatabase? = null

        fun getInstance(context: Context): LumaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LumaDatabase::class.java,
                    "luma_social.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

object SeedData {
    private const val ONE_HOUR_MS = 60L * 60L * 1000L
    private const val STORY_TTL_MS = 24L * ONE_HOUR_MS

    fun resUri(context: Context, resId: Int): String {
        return "android.resource://${context.packageName}/$resId"
    }

    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(("luma_salt_v1:$password").toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun ensureSeeded(context: Context, dao: LumaDao) {
        if (dao.countUsers() > 0) return

        val now = System.currentTimeMillis()
        val demoHash = hashPassword("luma1234")

        val users = listOf(
            UserEntity(
                id = "user_alex",
                email = "alex@luma.app",
                passwordHash = demoHash,
                username = "alex.rivera",
                displayName = "Alex Rivera",
                bio = "Visual storyteller • Chasing golden hours & minimalist spaces",
                avatarUri = resUri(context, R.drawable.img_sample_portrait),
                isVerified = true,
                createdAt = now - 30 * 24 * ONE_HOUR_MS
            ),
            UserEntity(
                id = "user_maya",
                email = "maya@luma.app",
                passwordHash = demoHash,
                username = "maya.captures",
                displayName = "Maya Lin",
                bio = "Film photographer & alpine wanderer 📷 35mm diaries",
                avatarUri = resUri(context, R.drawable.img_luma_avatar_maya),
                isVerified = true,
                createdAt = now - 25 * 24 * ONE_HOUR_MS
            ),
            UserEntity(
                id = "user_arjun",
                email = "arjun@luma.app",
                passwordHash = demoHash,
                username = "arjun.designs",
                displayName = "Arjun Mehta",
                bio = "Product designer • Specialty coffee & analog cameras",
                avatarUri = resUri(context, R.drawable.img_luma_avatar_arjun),
                isVerified = true,
                createdAt = now - 20 * 24 * ONE_HOUR_MS
            ),
            UserEntity(
                id = "user_elena",
                email = "elena@luma.app",
                passwordHash = demoHash,
                username = "elena.frames",
                displayName = "Elena Voss",
                bio = "Night street photography • Neon reflections & city rain",
                avatarUri = resUri(context, R.drawable.img_sample_urban),
                isVerified = false,
                createdAt = now - 15 * 24 * ONE_HOUR_MS
            )
        )
        dao.insertUsers(users)

        val follows = listOf(
            FollowEntity("user_alex", "user_maya", now - 10 * ONE_HOUR_MS),
            FollowEntity("user_alex", "user_arjun", now - 9 * ONE_HOUR_MS),
            FollowEntity("user_maya", "user_alex", now - 8 * ONE_HOUR_MS),
            FollowEntity("user_maya", "user_elena", now - 7 * ONE_HOUR_MS),
            FollowEntity("user_arjun", "user_maya", now - 6 * ONE_HOUR_MS),
            FollowEntity("user_elena", "user_maya", now - 5 * ONE_HOUR_MS),
            FollowEntity("user_elena", "user_alex", now - 4 * ONE_HOUR_MS)
        )
        dao.insertFollows(follows)

        val posts = listOf(
            PostEntity(
                id = "post_1",
                authorId = "user_maya",
                mediaUri = resUri(context, R.drawable.img_sample_nature),
                caption = "First light breaking over the ridge. Nothing beats cold mountain air and a quiet trail.",
                location = "Dolomites, Italy",
                createdAt = now - 2 * ONE_HOUR_MS
            ),
            PostEntity(
                id = "post_2",
                authorId = "user_arjun",
                mediaUri = resUri(context, R.drawable.img_luma_post_coffee),
                caption = "Morning ritual before opening Figma: Ethiopian pour-over and sketching new grid layouts.",
                location = "Koramangala, Bengaluru",
                createdAt = now - 5 * ONE_HOUR_MS
            ),
            PostEntity(
                id = "post_3",
                authorId = "user_elena",
                mediaUri = resUri(context, R.drawable.img_luma_post_neon),
                caption = "After midnight in the rain. Every puddle turns into a prism when the neon signs flicker on.",
                location = "Shinjuku, Tokyo",
                createdAt = now - 8 * ONE_HOUR_MS
            ),
            PostEntity(
                id = "post_4",
                authorId = "user_alex",
                mediaUri = resUri(context, R.drawable.img_sample_urban),
                caption = "Concrete geometry and golden afternoon shadows downtown.",
                location = "San Francisco, CA",
                createdAt = now - 14 * ONE_HOUR_MS
            ),
            PostEntity(
                id = "post_5",
                authorId = "user_maya",
                mediaUri = resUri(context, R.drawable.img_sample_portrait),
                caption = "Golden hour portraits on 50mm f/1.4 — warm tones straight out of camera.",
                location = "Kyoto, Japan",
                createdAt = now - 26 * ONE_HOUR_MS
            )
        )
        dao.insertPosts(posts)

        val likes = listOf(
            PostLikeEntity("post_1", "user_alex", now - ONE_HOUR_MS),
            PostLikeEntity("post_1", "user_arjun", now - ONE_HOUR_MS),
            PostLikeEntity("post_1", "user_elena", now - ONE_HOUR_MS),
            PostLikeEntity("post_2", "user_maya", now - 3 * ONE_HOUR_MS),
            PostLikeEntity("post_2", "user_elena", now - 2 * ONE_HOUR_MS),
            PostLikeEntity("post_3", "user_maya", now - 4 * ONE_HOUR_MS),
            PostLikeEntity("post_3", "user_alex", now - 3 * ONE_HOUR_MS),
            PostLikeEntity("post_4", "user_maya", now - 6 * ONE_HOUR_MS)
        )
        dao.insertLikes(likes)

        val comments = listOf(
            CommentEntity(
                id = "c_1",
                postId = "post_1",
                authorId = "user_arjun",
                text = "The mist along the valley floor is unreal!",
                createdAt = now - 90 * 60 * 1000L
            ),
            CommentEntity(
                id = "c_2",
                postId = "post_1",
                authorId = "user_elena",
                text = "Adding this spot to my autumn travel list immediately.",
                createdAt = now - 45 * 60 * 1000L
            ),
            CommentEntity(
                id = "c_3",
                postId = "post_2",
                authorId = "user_maya",
                text = "That ceramic mug + film camera combo is pure aesthetics.",
                createdAt = now - 2 * ONE_HOUR_MS
            ),
            CommentEntity(
                id = "c_4",
                postId = "post_3",
                authorId = "user_alex",
                text = "Those coral reflections on the wet pavement look cinematic!",
                createdAt = now - 3 * ONE_HOUR_MS
            )
        )
        dao.insertComments(comments)

        val stories = listOf(
            StoryEntity(
                id = "story_1",
                authorId = "user_maya",
                mediaUri = resUri(context, R.drawable.img_sample_nature),
                mediaType = "IMAGE",
                caption = "6:15 AM summit coffee ☕🏔️",
                createdAt = now - 2 * ONE_HOUR_MS,
                expiresAt = (now - 2 * ONE_HOUR_MS) + STORY_TTL_MS
            ),
            StoryEntity(
                id = "story_2",
                authorId = "user_maya",
                mediaUri = resUri(context, R.drawable.img_luma_avatar_maya),
                mediaType = "IMAGE",
                caption = "New film roll coming soon",
                createdAt = now - ONE_HOUR_MS,
                expiresAt = (now - ONE_HOUR_MS) + STORY_TTL_MS
            ),
            StoryEntity(
                id = "story_3",
                authorId = "user_arjun",
                mediaUri = resUri(context, R.drawable.img_luma_post_coffee),
                mediaType = "IMAGE",
                caption = "Dialing in today's roast",
                createdAt = now - 4 * ONE_HOUR_MS,
                expiresAt = (now - 4 * ONE_HOUR_MS) + STORY_TTL_MS
            ),
            StoryEntity(
                id = "story_4",
                authorId = "user_elena",
                mediaUri = resUri(context, R.drawable.img_luma_post_neon),
                mediaType = "IMAGE",
                caption = "Late night walk in Shinjuku",
                createdAt = now - 3 * ONE_HOUR_MS,
                expiresAt = (now - 3 * ONE_HOUR_MS) + STORY_TTL_MS
            )
        )
        dao.insertStories(stories)
    }
}
