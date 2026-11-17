package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.LumaRepository
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.FollowListBottomSheet
import com.example.ui.components.PostDetailDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.EditProfileScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.StoryViewerOverlay
import com.example.ui.theme.LumaTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.LumaViewModel

const val ROUTE_FEED = "feed"
const val ROUTE_EXPLORE = "explore"
const val ROUTE_CREATE = "create"
const val ROUTE_PROFILE = "profile"
const val ROUTE_EDIT_PROFILE = "edit_profile"
const val ROUTE_USER_PROFILE = "user_profile"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LumaRootApp()
        }
    }
}

@Composable
fun LumaRootApp() {
    val context = LocalContext.current
    val repository = remember(context.applicationContext) {
        LumaRepository(context.applicationContext)
    }
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(repository)
    )
    val currentUserId by authViewModel.currentUserId.collectAsStateWithLifecycle()
    val isDarkMode by authViewModel.isDarkMode.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    LumaTheme(darkTheme = isDarkMode) {
        val uid = currentUserId
        if (uid == null) {
            AuthScreen(
                uiState = authUiState,
                onEmailChange = authViewModel::updateEmail,
                onPasswordChange = authViewModel::updatePassword,
                onDisplayNameChange = authViewModel::updateDisplayName,
                onUsernameChange = authViewModel::updateUsername,
                onSubmit = authViewModel::submit,
                onToggleMode = authViewModel::toggleMode,
                onDemoSignIn = authViewModel::continueWithDemoAccount
            )
        } else {
            key(uid) {
                val lumaViewModel: LumaViewModel = viewModel(
                    key = uid,
                    factory = LumaViewModel.factory(repository, uid)
                )
                LumaAuthenticatedShell(viewModel = lumaViewModel)
            }
        }
    }
}

private data class BottomTabItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun LumaAuthenticatedShell(viewModel: LumaViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val feedState by viewModel.feedPostsState.collectAsStateWithLifecycle()
    val storyBuckets by viewModel.storyBuckets.collectAsStateWithLifecycle()
    val filterMode by viewModel.feedFilterMode.collectAsStateWithLifecycle()
    val sortMode by viewModel.feedSortMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val exploreUsers by viewModel.exploreUsers.collectAsStateWithLifecycle()
    val explorePosts by viewModel.explorePosts.collectAsStateWithLifecycle()
    val ownProfile by viewModel.ownProfile.collectAsStateWithLifecycle()
    val ownPosts by viewModel.ownPosts.collectAsStateWithLifecycle()
    val savedPosts by viewModel.savedPosts.collectAsStateWithLifecycle()
    val inspectedUserProfile by viewModel.inspectedUserProfile.collectAsStateWithLifecycle()
    val inspectedUserPosts by viewModel.inspectedUserPosts.collectAsStateWithLifecycle()
    val activeCommentsPost by viewModel.activeCommentsPost.collectAsStateWithLifecycle()
    val commentsList by viewModel.commentsForActivePost.collectAsStateWithLifecycle()
    val selectedPostDetail by viewModel.selectedPostDetail.collectAsStateWithLifecycle()
    val followListRequest by viewModel.followListRequest.collectAsStateWithLifecycle()
    val followListUsers by viewModel.followListUsers.collectAsStateWithLifecycle()
    val storyViewerSession by viewModel.storyViewer.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var createStoryInitialMode by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val tabs = remember {
        listOf(
            BottomTabItem(
                route = ROUTE_FEED,
                labelRes = R.string.nav_home,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                testTag = "nav_home"
            ),
            BottomTabItem(
                route = ROUTE_EXPLORE,
                labelRes = R.string.nav_explore,
                selectedIcon = Icons.Filled.Explore,
                unselectedIcon = Icons.Outlined.Explore,
                testTag = "nav_explore"
            ),
            BottomTabItem(
                route = ROUTE_CREATE,
                labelRes = R.string.nav_create,
                selectedIcon = Icons.Filled.AddBox,
                unselectedIcon = Icons.Outlined.AddBox,
                testTag = "nav_create"
            ),
            BottomTabItem(
                route = ROUTE_PROFILE,
                labelRes = R.string.nav_profile,
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                testTag = "nav_profile"
            )
        )
    }

    val showNavigationChrome = currentRoute in tabs.map { it.route }

    val navigateToTab: (String) -> Unit = { route ->
        if (route == ROUTE_CREATE) createStoryInitialMode = false
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openAuthorProfile: (String) -> Unit = { userId ->
        if (userId == viewModel.currentUserId) {
            navigateToTab(ROUTE_PROFILE)
        } else {
            viewModel.inspectUser(userId)
            navController.navigate("$ROUTE_USER_PROFILE/$userId")
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showNavigationChrome && !useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showNavigationChrome && useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.weight(0.2f))
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_FEED
                    ) {
                        composable(ROUTE_FEED) {
                            HomeFeedScreen(
                                feedState = feedState,
                                storyBuckets = storyBuckets,
                                ownProfile = ownProfile,
                                filterMode = filterMode,
                                sortMode = sortMode,
                                onSelectFilterMode = viewModel::setFeedFilterMode,
                                onSelectSortMode = viewModel::setFeedSortMode,
                                onOpenStoryBucket = viewModel::openStoryBucket,
                                onAddStoryClick = {
                                    createStoryInitialMode = true
                                    navController.navigate(ROUTE_CREATE) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                    }
                                },
                                onLikeClick = viewModel::toggleLike,
                                onCommentClick = viewModel::openComments,
                                onSaveClick = viewModel::toggleSave,
                                onAuthorClick = openAuthorProfile,
                                onFollowClick = viewModel::toggleFollow,
                                onDeletePost = viewModel::deletePost,
                                onDiscoverClick = { navigateToTab(ROUTE_EXPLORE) }
                            )
                        }

                        composable(ROUTE_EXPLORE) {
                            ExploreScreen(
                                searchQuery = searchQuery,
                                suggestedUsers = exploreUsers,
                                posts = explorePosts,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onUserClick = openAuthorProfile,
                                onToggleFollow = viewModel::toggleFollow,
                                onPostClick = viewModel::openPostDetail
                            )
                        }

                        composable(ROUTE_CREATE) {
                            CreateScreen(
                                initialStoryMode = createStoryInitialMode,
                                onPublishPost = { uri, caption, location ->
                                    viewModel.publishPost(uri, caption, location) {
                                        createStoryInitialMode = false
                                        navigateToTab(ROUTE_FEED)
                                    }
                                },
                                onPublishStory = { uri, caption ->
                                    viewModel.publishStory(uri, caption) {
                                        createStoryInitialMode = false
                                        navigateToTab(ROUTE_FEED)
                                    }
                                }
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                profile = ownProfile,
                                posts = ownPosts,
                                savedPosts = savedPosts,
                                isDarkMode = isDarkMode,
                                showBackButton = false,
                                onToggleDarkMode = viewModel::setDarkMode,
                                onEditProfileClick = { navController.navigate(ROUTE_EDIT_PROFILE) },
                                onAvatarStoryClick = { viewModel.openUserStories(viewModel.currentUserId) },
                                onFollowersClick = { viewModel.openFollowList(viewModel.currentUserId, true) },
                                onFollowingClick = { viewModel.openFollowList(viewModel.currentUserId, false) },
                                onPostClick = viewModel::openPostDetail,
                                onSignOut = viewModel::signOut
                            )
                        }

                        composable(ROUTE_EDIT_PROFILE) {
                            EditProfileScreen(
                                profile = ownProfile,
                                onBack = { navController.popBackStack() },
                                onSave = { name, handle, bio, avatar ->
                                    viewModel.updateProfile(name, handle, bio, avatar) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        composable(
                            route = "$ROUTE_USER_PROFILE/{userId}",
                            arguments = listOf(navArgument("userId") { type = NavType.StringType })
                        ) { entry ->
                            val targetId = entry.arguments?.getString("userId") ?: return@composable
                            LaunchedEffect(targetId) {
                                viewModel.inspectUser(targetId)
                            }
                            ProfileScreen(
                                profile = inspectedUserProfile,
                                posts = inspectedUserPosts,
                                savedPosts = emptyList(),
                                isDarkMode = isDarkMode,
                                showBackButton = true,
                                onBack = { navController.popBackStack() },
                                onToggleFollow = { viewModel.toggleFollow(targetId) },
                                onAvatarStoryClick = { viewModel.openUserStories(targetId) },
                                onFollowersClick = { viewModel.openFollowList(targetId, true) },
                                onFollowingClick = { viewModel.openFollowList(targetId, false) },
                                onPostClick = viewModel::openPostDetail
                            )
                        }
                    }
                }
            }
        }
    }

    // Comments Bottom Sheet
    val commentsPost = activeCommentsPost
    if (commentsPost != null) {
        CommentsBottomSheet(
            post = commentsPost,
            comments = commentsList,
            onDismiss = viewModel::closeComments,
            onSubmitComment = { text -> viewModel.addComment(commentsPost.id, text) },
            onDeleteComment = viewModel::deleteComment,
            onAuthorClick = openAuthorProfile
        )
    }

    // Followers / Following Bottom Sheet
    val followReq = followListRequest
    if (followReq != null) {
        FollowListBottomSheet(
            title = if (followReq.showFollowers) {
                stringResource(R.string.profile_followers)
            } else {
                stringResource(R.string.profile_following)
            },
            users = followListUsers,
            currentUserId = viewModel.currentUserId,
            onDismiss = viewModel::closeFollowList,
            onUserClick = openAuthorProfile,
            onToggleFollow = viewModel::toggleFollow
        )
    }

    // Post Detail Dialog (from Profile / Explore Grids)
    val detailPost = selectedPostDetail
    if (detailPost != null) {
        PostDetailDialog(
            post = detailPost,
            onDismiss = viewModel::closePostDetail,
            onLikeClick = { viewModel.toggleLike(detailPost.id) },
            onCommentClick = { viewModel.openComments(detailPost.id) },
            onSaveClick = { viewModel.toggleSave(detailPost.id) },
            onAuthorClick = { openAuthorProfile(detailPost.author.id) },
            onFollowClick = { viewModel.toggleFollow(detailPost.author.id) },
            onDeletePost = if (detailPost.isOwnPost) {
                { viewModel.deletePost(detailPost.id) }
            } else null
        )
    }

    // Full-Screen 24h Story Viewer Overlay
    val storySession = storyViewerSession
    if (storySession != null) {
        StoryViewerOverlay(
            session = storySession,
            onClose = viewModel::closeStoryViewer,
            onMarkViewed = viewModel::markStoryViewed,
            onDeleteStory = viewModel::deleteStory,
            onAuthorClick = openAuthorProfile
        )
    }
}
