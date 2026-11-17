package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LumaDao
import com.example.data.local.LumaDatabase
import com.example.data.local.SessionPreferences
import com.example.data.local.StoryEntity
import com.example.data.repository.LumaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: LumaDatabase
    private lateinit var dao: LumaDao
    private lateinit var repository: LumaRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, LumaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.lumaDao()
        repository = LumaRepository(context, dao, SessionPreferences(context))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Luma", appName)
    }

    @Test
    fun `signup login and profile edit persist locally`() = runTest {
        val signUpResult = repository.signUp(
            email = "riya@example.com",
            password = "password123",
            displayName = "Riya Sharma",
            username = "riya.creates"
        )
        assertTrue(signUpResult.isSuccess)
        val user = signUpResult.getOrThrow()

        val loginResult = repository.login("riya@example.com", "password123")
        assertTrue(loginResult.isSuccess)
        assertEquals(user.id, loginResult.getOrThrow().id)

        val updateResult = repository.updateProfile(
            userId = user.id,
            displayName = "Riya S.",
            username = "riya.visuals",
            bio = "Street & sunset photographer",
            newAvatarUri = null
        )
        assertTrue(updateResult.isSuccess)

        val profile = repository.observeUserProfile(user.id, user.id).first()
        assertEquals("Riya S.", profile?.user?.displayName)
        assertEquals("riya.visuals", profile?.user?.username)
        assertEquals("Street & sunset photographer", profile?.user?.bio)
    }

    @Test
    fun `24 hour story expiration hides expired stories`() = runTest {
        repository.initialize()
        val demo = repository.loginDemoAccount().getOrThrow()
        val now = System.currentTimeMillis()

        dao.insertStory(
            StoryEntity(
                id = "expired_story_test",
                authorId = demo.id,
                mediaUri = "android.resource://${context.packageName}/${R.drawable.img_sample_nature}",
                mediaType = "IMAGE",
                caption = "Old story",
                createdAt = now - 25L * 60L * 60L * 1000L,
                expiresAt = now - 1000L
            )
        )

        val activeUri = Uri.parse("android.resource://${context.packageName}/${R.drawable.img_sample_urban}")
        val createRes = repository.createStory(demo.id, activeUri, "Fresh 24h story")
        assertTrue(createRes.isSuccess)

        val buckets = repository.observeActiveStoriesAndViews(demo.id).first()
        val ownStories = buckets.firstOrNull { it.isOwnBucket }?.stories.orEmpty()
        assertTrue(ownStories.any { it.caption == "Fresh 24h story" })
        assertFalse(ownStories.any { it.id == "expired_story_test" })
    }
}
