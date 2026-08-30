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
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.BlurMaskFilter
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.indication
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bibleread.bread.data.BibleDatabase
import com.bibleread.bread.data.BibleXmlParser
import com.bibleread.bread.data.DbExporter
import com.bibleread.bread.data.TranslationManager
import com.bibleread.bread.notifications.DailyVerseScheduler
import com.bibleread.bread.ui.screens.*
import com.bibleread.bread.ui.theme.BreadTheme
import com.bibleread.bread.ui.theme.LocalThemeIndex
import com.bibleread.bread.viewmodel.BibleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Interaction source that swallows all interactions — suppresses Material3's built-in ripple on NavigationBarItem
private class NoRippleInteractionSource : MutableInteractionSource {
    override val interactions = kotlinx.coroutines.flow.MutableSharedFlow<Interaction>()
    override suspend fun emit(interaction: Interaction) {}
    override fun tryEmit(interaction: Interaction) = false
}

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

    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var exactAlarmPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        val t0 = SystemClock.elapsedRealtime()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                DailyVerseScheduler.scheduleDailyVerseAlarms(applicationContext)
            }
        }
        exactAlarmPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                DailyVerseScheduler.scheduleDailyVerseAlarms(applicationContext)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            exactAlarmPermissionLauncher.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            parseAllPendingXmlFiles(applicationContext)
            val elapsed = SystemClock.elapsedRealtime() - t0
            Log.d("Bread.Startup", "DB init done in ${elapsed}ms")
            withContext(Dispatchers.Main) {
                _dbReady.value = true
            }
        }

        DailyVerseScheduler.scheduleDailyVerseAlarms(applicationContext)

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
    object Scripture : Screen("scripture", label = "Scripture")
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

    // Scripture overlay — shown on top of Bible tab without recomposing it
    var showScripture by remember { mutableStateOf(false) }

    // Appearance overlay — shown on top without recomposing the underlying screen
    var showAppearance by remember { mutableStateOf(false) }

    // Appearance
    val bibleVm: BibleViewModel = viewModel()

    // Notes
    var showViewNote by remember { mutableStateOf<NoteEntry?>(null) }
    var noteCallbacks by remember { mutableStateOf<NoteCallbacks?>(null) }

    val isLoggedIn by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )
    val exactAlarmPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                DailyVerseScheduler.scheduleDailyVerseAlarms(context.applicationContext)
            }
        }
    )

    val items = listOf(
        Screen.Reader,
        Screen.Search,
        Screen.Community,
        Screen.Chats,
        Screen.Profile
    )

    // Nav bar is 64dp tall + 16dp vertical padding on each side = 96dp total footprint
    // Screens pad their bottom content by this so nothing hides under the pill
    val navBarBottomPadding = 96.dp

    val themeIndex = LocalThemeIndex.current
    val view = androidx.compose.ui.platform.LocalView.current

    // Re-apply status and navigation bar icon appearance on every route / theme change so splash or theme switch is always in sync
    SideEffect {
        val window = (context as android.app.Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        val isSplash = currentRoute == null || currentRoute == Screen.Splash.route
        val darkIcons = if (isSplash) {
            true // splash is yellow — dark icons
        } else {
            themeIndex == 1 || themeIndex == 2 // light (1) & paper (2) = dark icons, dark (0) = light icons
        }
        insetsController.isAppearanceLightStatusBars = darkIcons
        insetsController.isAppearanceLightNavigationBars = darkIcons
    }

    val showNavBar = currentRoute != null &&
        currentRoute != Screen.Splash.route &&
        currentRoute != Screen.Scripture.route &&
        !showScripture &&
        !showAppearance &&
        currentRoute != Screen.Appearance.route &&
        currentRoute != Screen.NewNote.route &&
        currentRoute != Screen.ViewNote.route

    // Handle system back button across the entire app
    androidx.activity.compose.BackHandler(
        enabled = showScripture || showAppearance || showViewNote != null || (currentRoute != null && currentRoute != Screen.Reader.route && currentRoute != Screen.Splash.route)
    ) {
        when {
            showAppearance -> {
                showAppearance = false
            }
            showScripture -> {
                if (bookSelectionCallback != null) {
                    bookSelectionCallback?.invoke(bibleVm.lastBook, bibleVm.lastChapter)
                    bookSelectionCallback = null
                }
                showScripture = false
            }
            showViewNote != null -> {
                showViewNote = null
                navController.popBackStack()
            }
            currentRoute == Screen.NewNote.route || currentRoute == Screen.ViewNote.route || currentRoute == Screen.Appearance.route -> {
                navController.popBackStack()
            }
            currentRoute != null && currentRoute != Screen.Reader.route && currentRoute != Screen.Splash.route -> {
                // If on another main tab (Journal, Community, Chats, Profile), go back to Bible tab instead of closing
                navController.navigate(Screen.Reader.route) {
                    popUpTo(Screen.Reader.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // ── Content layer ──────────────────────────────────────────────────
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (currentRoute != null && currentRoute != Screen.Splash.route)
                        Modifier.statusBarsPadding().navigationBarsPadding()
                    else
                        Modifier
                ),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    isDbReady = isDbReady,
                    onEnter = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        navController.navigate(Screen.Reader.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onOpenVerse = { book, chapter ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            exactAlarmPermissionLauncher.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
                        }
                        bibleVm.loadChapter(book, chapter)
                        navController.navigate(Screen.Reader.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                        showScripture = true
                    }
                )
            }
            // Bible tab — shows book selection as the entry point
            composable(Screen.Reader.route) {
                Box(modifier = Modifier.fillMaxSize()) {
                    BookSelectionOverlay(
                        onBookSelected = { book, chapter ->
                            bibleVm.loadChapter(book, chapter)
                            showScripture = true
                        },
                        onOpenAppearance = { showAppearance = true },
                        onClose = {},
                        fontStyle = bibleVm.fontStyle,
                        customFonts = bibleVm.customFonts,
                        getReadCount = { book -> bibleVm.readChapterCount(book) },
                        getTotalCount = { book -> bibleVm.totalChapterCount(book) },
                        initialViewMode = bibleVm.bibleViewMode,
                        onViewModeChange = { bibleVm.saveBibleViewMode(it) },
                        initialFilter = "All Books",
                        onFilterChange = { bibleVm.saveBibleFilter(it) },
                        initialCarouselIndex = 0,
                        onCarouselIndexChange = { bibleVm.saveBibleCarouselIndex(it) }
                    )
                    // Scripture overlay
                    if (showScripture) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .pointerInput(Unit) { detectTapGestures { } }
                        ) {
                        BibleScreen(
                            vm = bibleVm,
                            onOpenBookSelection = { onBookSelected ->
                                bookSelectionCallback = onBookSelected
                                showScripture = false
                            },
                            onOpenAppearance = { showAppearance = true },
                            onBack = {
                                if (bookSelectionCallback != null) {
                                    bookSelectionCallback?.invoke(bibleVm.lastBook, bibleVm.lastChapter)
                                    bookSelectionCallback = null
                                }
                                showScripture = false
                            }
                        )
                        }
                    }
                    // Appearance overlay (Bible Settings)
                    if (showAppearance) {
                        val fontFileLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.OpenDocument()
                        ) { uri -> if (uri != null) bibleVm.importCustomFont(uri) }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .pointerInput(Unit) { detectTapGestures { } }
                        ) {
                        BibleSettingsOverlay(
                            currentFontStyle = bibleVm.fontStyle,
                            onFontStyleChange = { bibleVm.saveFontStyle(it) },
                            customFonts = bibleVm.customFonts,
                            selectedThemeIndex = bibleVm.selectedThemeIndex,
                            onThemeChange = { bibleVm.saveThemeIndex(it) },
                            onAddFont = { fontFileLauncher.launch(arrayOf("font/ttf", "font/otf", "*/*")) },
                            onRemoveFont = { bibleVm.removeCustomFont(it) },
                            onClose = { showAppearance = false }
                        )
                        }
                    }
                }
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
            composable(Screen.Appearance.route) {
                val fontFileLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri -> if (uri != null) bibleVm.importCustomFont(uri) }
                BibleSettingsOverlay(
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

        // ── Gradient fade behind nav pill ─────────────────────────────────
        if (showNavBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        // ── Floating nav pill ──────────────────────────────────────────────
        if (showNavBar) {
            val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val isGestureNav = navBarHeight < 24.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 16.dp,
                        vertical = if (isGestureNav) 16.dp else 8.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                val navBarColor = MaterialTheme.colorScheme.surfaceVariant

                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 0.dp,
                    shape = RoundedCornerShape(50.dp),
                    color = navBarColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .drawBehind {
                            drawIntoCanvas { canvas ->
                                val paint = Paint().apply {
                                    asFrameworkPaint().apply {
                                        isAntiAlias = true
                                        color = android.graphics.Color.TRANSPARENT
                                        setShadowLayer(
                                            8f, 0f, 2f,
                                            android.graphics.Color.argb(120, 0, 0, 0)
                                        )
                                    }
                                }
                                val cornerRadius = size.height / 2f
                                canvas.drawRoundRect(
                                    left = 0f,
                                    top = 0f,
                                    right = size.width,
                                    bottom = size.height,
                                    radiusX = cornerRadius,
                                    radiusY = cornerRadius,
                                    paint = paint
                                )
                            }
                        }
                ) {
                    val selectedIndex = items.indexOfFirst { it.route == currentRoute }
                        .coerceAtLeast(0)

                    var previousIndex by remember { mutableIntStateOf(selectedIndex) }
                    val animatedIndex by animateFloatAsState(
                        targetValue = selectedIndex.toFloat(),
                        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
                        label = "navCircleSlide",
                        finishedListener = { previousIndex = selectedIndex }
                    )
                    val prevIndexSnapshot = remember(selectedIndex) { previousIndex }

                    val circleSizeDp = 54.dp
                    val density = androidx.compose.ui.platform.LocalDensity.current

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 0.dp)
                    ) {
                        val totalWidthPx = with(density) { maxWidth.toPx() }
                        val slotWidthPx = totalWidthPx / items.size
                        val circleSizePx = with(density) { circleSizeDp.toPx() }
                        val circleOffsetX = (animatedIndex * slotWidthPx + (slotWidthPx - circleSizePx) / 2f).toInt()

                        // Sliding circle
                        Box(
                            modifier = Modifier
                                .size(circleSizeDp)
                                .offset { IntOffset(circleOffsetX, 0) }
                                .align(Alignment.CenterStart)
                                .drawBehind {
                                    drawIntoCanvas { canvas ->
                                        val paint = Paint().apply {
                                            asFrameworkPaint().apply {
                                                isAntiAlias = true
                                                color = android.graphics.Color.TRANSPARENT
                                                setShadowLayer(6f, 0f, 1f, android.graphics.Color.argb(100, 0, 0, 0))
                                            }
                                        }
                                        canvas.drawCircle(center, size.minDimension / 2f, paint)
                                    }
                                }
                                .background(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    shape = CircleShape
                                )
                        )

                        // Icons
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items.forEachIndexed { index, screen ->
                                val iconAlpha = when (index) {
                                    selectedIndex -> {
                                        val proximity = (1f - kotlin.math.abs(animatedIndex - index)).coerceIn(0f, 1f)
                                        androidx.compose.ui.util.lerp(0.45f, 1f, proximity)
                                    }
                                    prevIndexSnapshot -> {
                                        val proximity = (1f - kotlin.math.abs(animatedIndex - index)).coerceIn(0f, 1f)
                                        androidx.compose.ui.util.lerp(0.45f, 1f, proximity)
                                    }
                                    else -> 0.45f
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { NoRippleInteractionSource() },
                                            indication = null
                                        ) {
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    screen.icon?.let {
                                        Icon(
                                            painter = painterResource(id = it),
                                            contentDescription = screen.label,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = iconAlpha)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Bottom blocker — covers system nav bar area below the pill ─────
        if (showNavBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    } // end root Box
}
