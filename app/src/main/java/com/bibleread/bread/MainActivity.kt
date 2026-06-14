package com.bibleread.bread

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.bibleread.bread.ui.screens.*
import com.bibleread.bread.ui.theme.BreadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BreadTheme {
                MainApp()
            }
        }
    }
}

sealed class Screen(val route: String, val icon: Int? = null, val label: String) {
    object Splash : Screen("splash", label = "Splash")
    object Reader : Screen("reader", R.raw.ic_nav_bible, "Bible")
    object Search : Screen("search", R.raw.ic_nav_search, "Search")
    object Community : Screen("home", R.raw.ic_nav_community, "Community")
    object Chats : Screen("chats", R.raw.ic_nav_chats, "Chats")
    object Profile : Screen("profile", R.raw.ic_nav_profile, "Profile")
    object Downloads : Screen("downloads", label = "Downloads")
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            // Permission result handled
        }
    )

    val items = listOf(
        Screen.Reader,
        Screen.Search,
        Screen.Community,
        Screen.Chats,
        Screen.Profile
    )

    Scaffold(
        bottomBar = {
            if (currentRoute != Screen.Splash.route) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = Color.DarkGray
                    )
                    NavigationBar(
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .height(72.dp)
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp, bottom = 4.dp)
                    ) {
                        items.forEach { screen ->
                            NavigationBarItem(
                                icon = { 
                                    screen.icon?.let {
                                        Icon(
                                            painter = rememberAsyncImagePainter(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(it)
                                                    .decoderFactory(SvgDecoder.Factory())
                                                    .build()
                                            ),
                                            contentDescription = screen.label,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    unselectedIconColor = Color.Gray,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Screen.Splash.route) { 
                SplashScreen(onModeSelected = { isOnline ->
                    // Logic to handle online/offline can be added here
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    navController.navigate(Screen.Community.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }) 
            }
            composable(Screen.Community.route) { HomeScreen() }
            composable(Screen.Reader.route) { ReaderScreen() }
            composable(Screen.Search.route) { SearchScreen() }
            composable(Screen.Chats.route) { ChatsScreen() }
            composable(Screen.Profile.route) { ProfileScreen(onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) }) }
            composable(Screen.Downloads.route) { DownloadsScreen() }
        }
    }
}