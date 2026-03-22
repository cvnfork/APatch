package me.bmax.apatch.ui

import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import coil.Coil
import coil.ImageLoader
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ExecuteAPMActionScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.dependency
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.component.BottomBar
import me.bmax.apatch.ui.component.BottomBarDestination
import me.bmax.apatch.ui.component.ModuleInstallHandler
import me.bmax.apatch.ui.screen.APModuleScreen
import me.bmax.apatch.ui.screen.HomeScreen
import me.bmax.apatch.ui.screen.KPModuleScreen
import me.bmax.apatch.ui.screen.SettingScreen
import me.bmax.apatch.ui.screen.SuperUserScreen
import me.bmax.apatch.ui.theme.APatchTheme
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

class MainActivity : ComponentActivity() {

    private var isLoading = true
    private val intentState = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen().setKeepOnScreenCondition { isLoading }

        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalActivity.current ?: this
            val prefs = context.getSharedPreferences("config", MODE_PRIVATE)
            var colorMode by remember { mutableIntStateOf(prefs.getInt("color_mode", 0)) }
            var keyColorInt by remember { mutableIntStateOf(prefs.getInt("key_color", 0)) }
            val keyColor = remember(keyColorInt) { if (keyColorInt == 0) null else Color(keyColorInt) }

            val darkMode = when (colorMode) {
                2, 5 -> true
                0, 3 -> isSystemInDarkTheme()
                else -> false
            }

            DisposableEffect(prefs, darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }

                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        "color_mode" -> colorMode = prefs.getInt("color_mode", 0)
                        "key_color" -> keyColorInt = prefs.getInt("key_color", 0)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            APatchTheme(colorMode = colorMode, keyColor = keyColor) {
                DestinationsNavHost(
                    navGraph = NavGraphs.root,
                    defaultTransitions = object : NavHostAnimatedDestinationStyle() {
                        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                            {
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(
                                        durationMillis = 500,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }

                        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                            {
                                slideOutHorizontally(
                                    targetOffsetX = { -it / 5 },
                                    animationSpec = tween(
                                        durationMillis = 500,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }

                        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                            {
                                slideInHorizontally(
                                    initialOffsetX = { -it / 5 },
                                    animationSpec = tween(
                                        durationMillis = 500,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }

                        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                            {
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(
                                        durationMillis = 500,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                    },
                    dependenciesContainerBuilder = {
                        dependency(intentState)
                        dependency(this@MainActivity)
                    }
                )
            }
        }

        // Initialize Coil
        val iconSize = resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components {
                    add(AppIconKeyer())
                    add(AppIconFetcher.Factory(iconSize, false, this@MainActivity))
                }
                .build()
        )

        isLoading = false
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.data != null || intent.hasExtra("uris") || intent.hasExtra("shortcut_type")) {
            intentState.value += 1
        }
    }
}

// CompositionLocal for sharing state
val LocalHandlePageChange = compositionLocalOf<(Int) -> Unit> { error("No handle page change") }
val LocalSelectedPage = compositionLocalOf<Int> { error("No selected page") }

@Destination<RootGraph>(start = true)
@Composable
fun MainScreen(
    intentState: MutableStateFlow<Int>,
    activity: MainActivity,
    navigator: DestinationsNavigator
) {
    val isExternalIntent = remember {
        activity.intent?.data != null ||
                activity.intent?.hasExtra("uris") == true ||
                activity.intent?.hasExtra("shortcut_type") == true
    }

    val coroutineScope = rememberCoroutineScope()
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE
    val aPatchReady = state == APApplication.State.ANDROIDPATCH_INSTALLED

    val viewModel = viewModel<APModuleViewModel>()
    val superUserViewModel = viewModel<SuperUserViewModel>()

    var installUri by remember { mutableStateOf<Uri?>(null) }

    UriInstallHandler(intentState, activity.intent) { uri ->
        installUri = uri
    }

    installUri?.let { uri ->
        ModuleInstallHandler(
            uri = uri,
            viewModel = viewModel,
            navigator = navigator,
            onReset = { installUri = null }
        )
    }

    ShortcutIntentHandler(
        intentState = intentState,
        navigator = navigator
    )

    val availablePages = remember(kPatchReady, aPatchReady) {
        BottomBarDestination.entries.filter { d ->
            !(d.kPatchRequired && !kPatchReady) && !(d.aPatchRequired && !aPatchReady)
        }
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { availablePages.size })
    var userScrollEnabled by remember(aPatchReady) { mutableStateOf(aPatchReady) }
    var animating by remember { mutableStateOf(false) }
    var uiSelectedPage by remember { mutableIntStateOf(0) }
    var animateJob by remember { mutableStateOf<Job?>(null) }
    var lastRequestedPage by remember { mutableIntStateOf(pagerState.currentPage) }

    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = colorScheme.surface,
        tint = HazeTint(colorScheme.surface.copy(0.8f))
    )

    val handlePageChange: (Int) -> Unit = remember(pagerState, coroutineScope, aPatchReady) {
        { page ->
            uiSelectedPage = page

            // If already at target page, cancel any ongoing animation and reset state
            if (page == pagerState.currentPage) {
                animateJob?.cancel()
                animateJob = null
                animating = false
                userScrollEnabled = aPatchReady
                lastRequestedPage = page
            }
            // If this is a new target page (not currently animating to it), start new animation
            else if (lastRequestedPage != page) {
                animateJob?.cancel()
                animating = true
                userScrollEnabled = false
                lastRequestedPage = page

                animateJob = coroutineScope.launch {
                    try {
                        pagerState.animateScrollToPage(page)
                    } finally {
                        userScrollEnabled = aPatchReady
                        animating = false
                        animateJob = null
                    }
                }
            }
            // Otherwise, already animating to target page, do nothing
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (!animating) {
                uiSelectedPage = page
                lastRequestedPage = page
            }
        }
    }

    LaunchedEffect(Unit) {
        SuperUserViewModel.fetchAppListIfEmpty(superUserViewModel)
    }

    BackHandler {
        if (pagerState.currentPage != 0) {
            handlePageChange(0)
        } else {
            if (isExternalIntent) {
                activity.finish()
            } else {
                activity.moveTaskToBack(true)
            }
        }
    }

    CompositionLocalProvider(
        LocalHandlePageChange provides handlePageChange,
        LocalSelectedPage provides uiSelectedPage
    ) {
        Scaffold(
            bottomBar = {
                BottomBar(hazeState, hazeStyle)
            },
        ) { innerPadding ->
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
                state = pagerState,
                beyondViewportPageCount = availablePages.size,
                userScrollEnabled = userScrollEnabled,
            ) { pageIndex ->
                val bottomPadding = innerPadding.calculateBottomPadding()

                when (availablePages[pageIndex]) {
                    BottomBarDestination.Home -> HomeScreen(bottomPadding, navigator)
                    BottomBarDestination.KModule -> KPModuleScreen(bottomPadding, navigator)
                    BottomBarDestination.SuperUser -> SuperUserScreen(bottomPadding)
                    BottomBarDestination.AModule -> APModuleScreen(bottomPadding, navigator)
                    BottomBarDestination.Settings -> SettingScreen(bottomPadding, navigator)
                }
            }
        }
    }
}

@Composable
private fun UriInstallHandler(
    intentState: MutableStateFlow<Int>,
    intent: android.content.Intent?,
    onInstall: (Uri) -> Unit
) {
    val intentStateValue by intentState.collectAsState()

    LaunchedEffect(intentStateValue) {
        val uri = intent?.data ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableArrayListExtra("uris", Uri::class.java)?.firstOrNull()
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableArrayListExtra<Uri>("uris")?.firstOrNull()
        }

        uri?.let {
            onInstall(it)
            intent?.data = null
            intent?.removeExtra("uris")
            intent?.removeExtra("shortcut_type")
        }
    }
}


@Composable
private fun ShortcutIntentHandler(
    intentState: MutableStateFlow<Int>,
    navigator: DestinationsNavigator
) {
    val activity = LocalActivity.current ?: return
    val context = LocalContext.current
    val intentStateValue by intentState.collectAsState()
    LaunchedEffect(intentStateValue) {
        val intent = activity.intent
        val type = intent?.getStringExtra("shortcut_type") ?: return@LaunchedEffect
        when (type) {
            "module_action" -> {
                val moduleId = intent.getStringExtra("module_id") ?: return@LaunchedEffect
                navigator.navigate(ExecuteAPMActionScreenDestination(moduleId = moduleId)) {
                    launchSingleTop = true
                }
            }

            "module_webui" -> {
                val moduleId = intent.getStringExtra("module_id") ?: return@LaunchedEffect
                val moduleName = intent.getStringExtra("module_name") ?: moduleId
                val webIntent = android.content.Intent(context, WebUIActivity::class.java)
                    .setData("kernelsu://webui/$moduleId".toUri())
                    .putExtra("id", moduleId)
                    .putExtra("name", moduleName)
                    .putExtra("from_webui_shortcut", true)
                    .addFlags(
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                context.startActivity(webIntent)
            }

            else -> return@LaunchedEffect
        }
    }
}