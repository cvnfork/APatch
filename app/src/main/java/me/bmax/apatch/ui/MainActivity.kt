package me.bmax.apatch.ui

import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.ImageLoader
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.dependency
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.component.ModuleInstallHandler
import me.bmax.apatch.ui.screen.BottomBarDestination
import me.bmax.apatch.ui.theme.APatchTheme
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold

class MainActivity : AppCompatActivity() {

    private var isLoading = true
    private val intentState = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen().setKeepOnScreenCondition { isLoading }

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalActivity.current ?: this
            val prefs = context.getSharedPreferences("config", MODE_PRIVATE)
            var colorMode by remember { mutableIntStateOf(prefs.getInt("color_mode", 0)) }

            DisposableEffect(prefs) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == "color_mode") {
                        colorMode = prefs.getInt("color_mode", 0)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            APatchTheme(colorMode = colorMode) {
                DestinationsNavHost(
                    navGraph = NavGraphs.root,
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
        intentState.value += 1
    }
}

// CompositionLocal for sharing state
val LocalPagerState = compositionLocalOf<PagerState> { error("No pager state") }
val LocalHandlePageChange = compositionLocalOf<(Int) -> Unit> { error("No handle page change") }
val LocalSelectedPage = compositionLocalOf<Int> { error("No selected page") }

@Destination<RootGraph>(start = true)
@Composable
fun MainScreen(
    intentState: MutableStateFlow<Int>,
    activity: MainActivity,
    navigator: DestinationsNavigator
) {
    val coroutineScope = rememberCoroutineScope()
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE
    val aPatchReady = state == APApplication.State.ANDROIDPATCH_INSTALLED

    val viewModel = viewModel<APModuleViewModel>()

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

    val handlePageChange: (Int) -> Unit = remember(pagerState, coroutineScope, aPatchReady) {
        { page ->
            uiSelectedPage = page
            if (page == pagerState.currentPage) {
                if (animateJob != null && lastRequestedPage != page) {
                    animateJob?.cancel()
                    animateJob = null
                    animating = false
                    userScrollEnabled = aPatchReady
                }
                lastRequestedPage = page
            } else {
                if (animateJob != null && lastRequestedPage == page) {
                    // Already animating to the requested page
                } else {
                    animateJob?.cancel()
                    animating = true
                    userScrollEnabled = false
                    val job = coroutineScope.launch {
                        try {
                            pagerState.animateScrollToPage(page)
                        } finally {
                            if (animateJob === this) {
                                userScrollEnabled = aPatchReady
                                animating = false
                                animateJob = null
                            }
                        }
                    }
                    animateJob = job
                    lastRequestedPage = page
                }
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (!animating) uiSelectedPage = page
        }
    }

    LaunchedEffect(Unit) {
        if (SuperUserViewModel.apps.isEmpty()) {
            SuperUserViewModel().fetchAppList()
        }
    }

    BackHandler {
        if (pagerState.currentPage != 0) {
            handlePageChange(0)
        } else {
            activity.moveTaskToBack(true)
        }
    }

    CompositionLocalProvider(
        LocalPagerState provides pagerState,
        LocalHandlePageChange provides handlePageChange,
        LocalSelectedPage provides uiSelectedPage
    ) {
        Scaffold(
            bottomBar = {
                BottomBar(availablePages)
            },
        ) { innerPadding ->
            HorizontalPager(
                modifier = Modifier,
                state = pagerState,
                beyondViewportPageCount = availablePages.size,
                userScrollEnabled = userScrollEnabled,
            ) { pageIndex ->
                val destination = availablePages[pageIndex]
                val bottomPadding = innerPadding.calculateBottomPadding()

                when (destination) {
                    BottomBarDestination.Home -> {
                        me.bmax.apatch.ui.screen.HomeScreen(bottomPadding, navigator)
                    }
                    BottomBarDestination.KModule -> {
                        me.bmax.apatch.ui.screen.KPModuleScreen(bottomPadding, navigator)
                    }
                    BottomBarDestination.SuperUser -> {
                        me.bmax.apatch.ui.screen.SuperUserScreen(bottomPadding)
                    }
                    BottomBarDestination.AModule -> {
                        me.bmax.apatch.ui.screen.APModuleScreen(bottomPadding, navigator)
                    }
                    BottomBarDestination.Settings -> {
                        me.bmax.apatch.ui.screen.SettingScreen(bottomPadding)
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(availablePages: List<BottomBarDestination>) {
    val selectedPage = LocalSelectedPage.current
    val handlePageChange = LocalHandlePageChange.current

    val navItems = availablePages.map { d ->
        NavigationItem(
            label = stringResource(d.label),
            icon = if (selectedPage == availablePages.indexOf(d)) d.iconSelected else d.iconNotSelected
        )
    }

    NavigationBar(
        items = navItems,
        selected = selectedPage,
        onClick = { index ->
            handlePageChange(index)
        }
    )
}

@Composable
private fun UriInstallHandler(
    intentState: MutableStateFlow<Int>,
    intent: android.content.Intent?,
    onInstall: (Uri) -> Unit
) {
    val intentStateValue by intentState.collectAsState()

    LaunchedEffect(intentStateValue) {
        val uri: Uri? = intent?.data ?: run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableArrayListExtra("uris", Uri::class.java)?.firstOrNull()
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableArrayListExtra<Uri>("uris")?.firstOrNull()
            }
        }

        uri?.let {
            onInstall(it)
        }
    }
}