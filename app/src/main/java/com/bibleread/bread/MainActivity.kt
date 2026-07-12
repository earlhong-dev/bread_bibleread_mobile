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
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.bibleread.bread.viewmodel.BibleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

suspend fun parseAllPendingXmlFiles(context: android.content.Context) {
    val xmlFiles = try {
        context.assets.list("")
            ?.filter { it.endsWith(".xml") }
            ?: emptyList()
    } catch (e: Exception) {
        Log.e("MainActivity", "Failed to list assets: ${e.message}")
        emptyList()
    }

    for (xmlFile in xmlFiles) {
        val translationCode = xmlFile.removeSuffix(".xml")
        val assetPath       = TranslationManager.assetPath(translationCode)

        val hasPrebuilt = try { context.assets.open(assetPath).use { true } } catch (_: Exception) { false }
        if (hasPrebuilt) continue

        try {
            val db    = BibleDatabase.getInstance(context, translationCode)
            val count = db.verseDao().getTotalVerseCount()
            if (count == 0) {
                Log.d("MainActivity", "Parsing $xmlFile ...")
                BibleXmlParser.parse(context, db.verseDao(), xmlFile)
                DbExporter.exportFromXml(context, xmlFile)
                Log.d("MainActivity", "Done. DB ready at: " +
                    "data/data/com.bibleread.bread/databases/$translationCode.db")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to parse $xmlFile: ${e.message}", e)
        }
    }
}

class MainActivity : ComponentActivity() {

    private val _dbReady = mutableStateOf(false)
    val dbReady: State<Boolean> = _dbReady

    override fun onCreate(savedInstanceState: Bundle?) {
        val t0 = SystemClock.elapsedRealtime()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch(Dispatchers.IO) {
            parseAllPendingXmlFiles(applicationContext)
            val elapsed = SystemClock.elapsedRealtime() - t0
            Log.d("Bread.Startup", "DB init done in ${elapsed}ms")
            withContext(Dispatchers.Main) {
                _dbReady.value = true
            }
        }

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefs = remember { context.getSharedPreferences("bible_prefs", android.content.Context.MODE_PRIVATE) }
            // Use the ViewModel's observable theme index so changes apply immediately
            val bibleVm: BibleViewModel = viewModel()
            val themeIndex = bibleVm.selectedThemeIndex

            BreadTheme(themeIndex = themeIndex) {
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
    object BookSelection : Screen("book_selection", label = "Book Selection")
    object Appearance   : Screen("appearance",   label = "Appearance")
    object NewNote      : Screen("new_note",      label = "New Note")
    object ViewNote     : Screen("view_note",     label = "View Note")
}

// Callbacks passed from JournalScreen up to MainApp
data class NoteCallbacks(
    val onSaveNew: (title: String, body: String) -> Unit,
    val onSaveEdit: (NoteEntry) -> Unit,
    val onDelete: (NoteEntry) -> Unit
)

@Composable
fun MainApp(dbReady: State<Boolean>) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDbReady by dbReady

    // Book selection
    var bookSelectionCallback by remember { mutableStateOf<((String, Int) -> Unit)?>(null) }

    // Appearance
    val bibleVm: BibleViewModel = viewModel()

    // Notes
    var showViewNote by remember { mutableStateOf<NoteEntry?>(null) }
    var noteCallbacks by remember { mutableStateOf<NoteCallbacks?>(null) }

    var splashDone by remember { mutableStateOf(false) }
    val isLoggedIn by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    val baseTabs = listOf(Screen.Reader, Screen.Search, Screen.Profile)
    val loggedInTabs = listOf(Screen.Reader, Screen.Search, Screen.Community, Screen.Chats, Screen.Profile)
    val items = if (isLoggedIn) loggedInTabs else baseTabs

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (currentRoute != Screen.Splash.route && currentRoute != Screen.BookSelection.route && currentRoute != Screen.Appearance.route && currentRoute != Screen.NewNote.route && currentRoute != Screen.ViewNote.route) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets(0.dp),
                            modifier = Modifier.fillMaxWidth().height(55.dp)
                        ) {
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = {
                                        screen.icon?.let {
                                            Icon(
                                                painter = painterResource(id = it),
                                                contentDescription = screen.label,
                                                modifier = Modifier.size(27.dp)
                                            )
                                        }
                                    },
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
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
                    composable(Screen.Reader.route) {
                        BibleScreen(
                            vm = bibleVm,
                            onOpenBookSelection = { onBookSelected ->
                                bookSelectionCallback = onBookSelected
                                navController.navigate(Screen.BookSelection.route)
                            },
                            onOpenAppearance = { navController.navigate(Screen.Appearance.route) }
                        )
                    }
                    composable(Screen.Search.route) {
                        JournalScreen(
                            onOpenNewNote = { callbacks ->
                                noteCallbacks = callbacks
                                navController.navigate(Screen.NewNote.route)
                            },
                            onOpenViewNote = { note, callbacks ->
                                noteCallbacks = callbacks
                                showViewNote = note
                                navController.navigate(Screen.ViewNote.route)
                            }
                        )
                    }
                    composable(Screen.BookSelection.route) {
                        BookSelectionOverlay(
                            books = BIBLE_BOOKS.keys.toList(),
                            onBookSelected = { book, chapter ->
                                bookSelectionCallback?.invoke(book, chapter)
                                bookSelectionCallback = null
                                navController.popBackStack()
                            },
                            onClose = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Appearance.route) {
                        val fontFileLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.OpenDocument()
                        ) { uri -> if (uri != null) bibleVm.importCustomFont(uri) }
                        AppearanceSettingsOverlay(
                            currentFontSize = bibleVm.fontSize,
                            onFontSizeChange = { bibleVm.saveFontSize(it) },
                            currentFontStyle = bibleVm.fontStyle,
                            onFontStyleChange = { bibleVm.saveFontStyle(it) },
                            customFonts = bibleVm.customFonts,
                            selectedThemeIndex = bibleVm.selectedThemeIndex,
                            onThemeChange = { bibleVm.saveThemeIndex(it) },
                            onAddFont = { fontFileLauncher.launch(arrayOf("font/ttf", "font/otf", "*/*")) },
                            onRemoveFont = { bibleVm.removeCustomFont(it) },
                            onClose = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.NewNote.route) {
                        NewNoteScreen(
                            onBack = { navController.popBackStack() },
                            onSave = { title, body ->
                                noteCallbacks?.onSaveNew?.invoke(title, body)
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(Screen.ViewNote.route) {
                        showViewNote?.let { note ->
                            ViewNoteScreen(
                                note = note,
                                onBack = {
                                    showViewNote = null
                                    navController.popBackStack()
                                },
                                onSaveEdit = { updated ->
                                    noteCallbacks?.onSaveEdit?.invoke(updated)
                                    showViewNote = updated
                                },
                                onDelete = {
                                    noteCallbacks?.onDelete?.invoke(note)
                                    showViewNote = null
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                    composable(Screen.Profile.route)   { ProfileScreen(isLoggedIn = isLoggedIn) }
                    composable(Screen.Community.route) { HomeScreen() }
                    composable(Screen.Chats.route)     { ChatsScreen() }
                }

                if (splashDone && !isDbReady) {
                    BibleLoadingOverlay()
                }
            }
        }

    } // end root Box
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
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Loading Your Bible Data", color = Color.White.copy(alpha = alpha),
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("This only happens once", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        }
    }
}
