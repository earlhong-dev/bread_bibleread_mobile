package com.bibleread.bread

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bibleread.bread.data.BibleDatabase
import com.bibleread.bread.data.BibleXmlParser
import com.bibleread.bread.data.DbExporter
import com.bibleread.bread.data.TranslationManager
import com.bibleread.bread.ui.screens.*
import com.bibleread.bread.ui.theme.BreadTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Scans assets/ for any .xml files and parses them into a database if:
 *   a) the translation has no pre-built .db in assets/translations/, AND
 *   b) the on-device DB for that translation is empty
 *
 * After parsing it exports the .db so you can pull it with Device File Explorer.
 * Once you place the .db in assets/translations/ and remove the .xml,
 * this function skips that translation entirely — no code changes needed.
 */
suspend fun parseAllPendingXmlFiles(context: android.content.Context) {
    val xmlFiles = try {
        context.assets.list("")
            ?.filter { it.endsWith(".xml") }
            ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    for (xmlFile in xmlFiles) {
        val translationCode = xmlFile.removeSuffix(".xml")
        val assetPath       = TranslationManager.assetPath(translationCode)

        // Skip if a pre-built DB already exists in assets/translations/
        val hasPrebuilt = try { context.assets.open(assetPath).use { true } } catch (e: Exception) { false }
        if (hasPrebuilt) continue

        // Parse only if the on-device DB is empty
        val db    = BibleDatabase.getInstance(context, translationCode)
        val count = db.verseDao().getTotalVerseCount()
        if (count == 0) {
            android.util.Log.d("MainActivity", "Parsing $xmlFile ...")
            BibleXmlParser.parse(context, db.verseDao(), xmlFile)
            // Export the freshly-populated DB so you can pull it and ship it as a pre-built asset
            DbExporter.exportFromXml(context, xmlFile)
            android.util.Log.d("MainActivity", "Done. DB ready at: " +
                "data/data/com.bibleread.bread/databases/$translationCode.db")
        }
    }
}

class MainActivity : ComponentActivity() {

    private val _dbReady = mutableStateOf(false)
    val dbReady: State<Boolean> = _dbReady

    override fun onCreate(savedInstanceState: Bundle?) {
        val t0 = SystemClock.elapsedRealtime()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // lifecycleScope is tied to the Activity — no coroutine leak on rotation or finish
        lifecycleScope.launch(Dispatchers.IO) {
            parseAllPendingXmlFiles(applicationContext)
            val elapsed = SystemClock.elapsedRealtime() - t0
            Log.d("Bread.Startup", "DB init done in ${elapsed}ms")
            withContext(Dispatchers.Main) {
                _dbReady.value = true
            }
        }

        setContent {
            BreadTheme {
                MainApp(dbReady = dbReady)
            }
        }
    }
}

sealed class Screen(val route: String, val icon: Int? = null, val label: String) {
    object Splash    : Screen("splash", label = "Splash")
    object Reader    : Screen("reader",    R.drawable.ic_bibletab,   "Bible")
    object Search    : Screen("search",    R.drawable.ic_journaltab,  "Journal")
    object Profile   : Screen("profile",   R.drawable.ic_profiletab, "Profile")
    object Community : Screen("home",      R.drawable.ic_commtab,    "Community")
    object Chats     : Screen("chats",     R.drawable.ic_chattab,    "Chats")
}

@Composable
fun MainApp(dbReady: State<Boolean>) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDbReady by dbReady

    var splashDone by remember { mutableStateOf(false) }

    // Hardcoded for now — flip to true once auth is implemented
    val isLoggedIn by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    // Tabs shown when logged out
    val baseTabs = listOf(Screen.Reader, Screen.Search, Screen.Profile)
    // Extra tabs unlocked after login
    val loggedInTabs = listOf(Screen.Reader, Screen.Search, Screen.Community, Screen.Chats, Screen.Profile)
    val items = if (isLoggedIn) loggedInTabs else baseTabs

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
                                            painter = painterResource(id = it),
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
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(onFinished = {
                        splashDone = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        navController.navigate(Screen.Reader.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    })
                }
                composable(Screen.Reader.route)    { BibleScreen() }
                composable(Screen.Search.route)    { JournalScreen() }
                composable(Screen.Profile.route)   { ProfileScreen(isLoggedIn = isLoggedIn) }
                composable(Screen.Community.route) { HomeScreen() }
                composable(Screen.Chats.route)     { ChatsScreen() }
            }

            // Loading overlay — shows after splash until DB is ready
            if (splashDone && !isDbReady) {
                BibleLoadingOverlay()
            }
        }
    }
}

@Composable
fun BibleLoadingOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Loading Your Bible Data",
                color = Color.White.copy(alpha = alpha),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This only happens once",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }
    }
}