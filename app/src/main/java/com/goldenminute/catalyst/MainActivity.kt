package com.goldenminute.catalyst

import android.graphics.Bitmap
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.text.TextUtils
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.drawToBitmap
import com.goldenminute.catalyst.service.TimerService
import com.goldenminute.catalyst.ui.TimerStateManager
import com.goldenminute.catalyst.ui.TodoApp
import com.goldenminute.catalyst.ui.TodoViewModel
import com.goldenminute.catalyst.ui.WelcomeScreen
import com.goldenminute.catalyst.ui.theme.LocalThemeRevealState
import com.goldenminute.catalyst.ui.theme.MyApplicationTheme
import com.goldenminute.catalyst.ui.theme.ThemeRevealState
import androidx.compose.ui.graphics.toArgb
import java.util.Locale
import kotlin.math.hypot
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
class MainActivity : ComponentActivity() {

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val viewModel: TodoViewModel by viewModels()
        handleTimerIntent(intent, viewModel)
    }

    private fun handleTimerIntent(intent: Intent?, viewModel: TodoViewModel) {
        if (intent?.action == "ACTION_SAVE_TIMER") {
            viewModel.saveCurrentTimerSession(this)
            // Clear the action so it doesn't trigger again on rotation
            intent.action = null
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val viewModel: TodoViewModel by viewModels()
        
        handleTimerIntent(intent, viewModel)

        splashScreen.setKeepOnScreenCondition {
            !viewModel.isInitialized.value
        }

        setContent {
            val isReady by viewModel.isInitialized.collectAsState()
            
            if (!isReady) return@setContent
            
            val lifecycleOwner = LocalLifecycleOwner.current
            val context = LocalContext.current
            
            val themeKey by viewModel.appTheme.collectAsState()
            val customThemeColor by viewModel.customThemeColor.collectAsState()
            val customThemeApplyToBg by viewModel.customThemeApplyToBg.collectAsState()
            val languageCode by viewModel.appLanguage.collectAsState()
            val isFirstTime by viewModel.isFirstTime.collectAsState()

            var showBatteryDialog by remember { mutableStateOf(false) }

            val palette = com.goldenminute.catalyst.ui.theme.getThemePalette(themeKey, customThemeColor, customThemeApplyToBg)
            val isLight = themeKey == "slate_light" || themeKey.startsWith("custom_light")

            LaunchedEffect(themeKey, customThemeColor, customThemeApplyToBg) {
                enableEdgeToEdge(
                    statusBarStyle = if (isLight) {
                        androidx.activity.SystemBarStyle.light(palette.obsidianBlack.toArgb(), palette.obsidianBlack.toArgb())
                    } else {
                        androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isLight) {
                        androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK)
                    } else {
                        androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    }
                )
                window.decorView.setBackgroundColor(palette.obsidianBlack.toArgb())
            }
            
            val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (!isGranted) {
                    android.util.Log.w("MainActivity", "Notification permission denied.")
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            
            DisposableEffect(lifecycleOwner, context) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            viewModel.checkAndResetDailyTasks()
                            // Stop the service when app returns to foreground
                            context.stopService(Intent(context, TimerService::class.java))
                        }
                        Lifecycle.Event.ON_STOP -> {
                            // Start background service if a timer is running
                            if (TimerStateManager.isTimerRunning.value) {
                                val serviceIntent = Intent(context, TimerService::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }
                            }
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                
                val receiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(c: android.content.Context?, intent: android.content.Intent?) {
                        viewModel.checkAndResetDailyTasks()
                    }
                }
                val filter = android.content.IntentFilter().apply {
                    addAction(android.content.Intent.ACTION_DATE_CHANGED)
                    addAction(android.content.Intent.ACTION_TIME_CHANGED)
                    addAction(android.content.Intent.ACTION_TIMEZONE_CHANGED)
                }
                context.registerReceiver(receiver, filter)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    context.unregisterReceiver(receiver)
                }
            }
            

            // -- Circular reveal state --
            val themeRevealState = remember { ThemeRevealState() }
            var overlayBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
            var revealCenter by remember { mutableStateOf(Offset.Zero) }
            val revealRadius = remember { Animatable(0f) }
            val composeView = LocalView.current

            // Wire up the callback: capture bitmap, then animate
            LaunchedEffect(Unit) {
                themeRevealState.onRevealRequested = { center ->
                    // Capture the current screen before the theme changes
                    try {
                        val bmp = composeView.drawToBitmap()
                        overlayBitmap = bmp.asImageBitmap()
                        revealCenter = center
                        // The radius will be animated in a separate LaunchedEffect below
                    } catch (_: Exception) {
                        // If capture fails, skip the animation
                        overlayBitmap = null
                    }
                }
            }

            // When overlay appears, animate the reveal
            LaunchedEffect(overlayBitmap) {
                if (overlayBitmap != null) {
                    try {
                        // Calculate max radius to cover full screen from the touch point
                        val w = composeView.width.toFloat()
                        val h = composeView.height.toFloat()
                        val maxRadius = hypot(
                            maxOf(revealCenter.x, w - revealCenter.x),
                            maxOf(revealCenter.y, h - revealCenter.y)
                        ) * 1.1f  // a little extra to be safe

                        revealRadius.snapTo(0f)
                        revealRadius.animateTo(
                            targetValue = maxRadius,
                            animationSpec = tween(850, easing = FastOutSlowInEasing)
                        )
                    } finally {
                        // Animation done or cancelled — remove overlay and reset radius
                        overlayBitmap = null
                        themeRevealState.isAnimating = false
                        revealRadius.snapTo(0f)
                    }
                }
            }

            LocalizedContent(localeCode = languageCode) {
                MyApplicationTheme(
                    themeKey = themeKey,
                    customColor = customThemeColor,
                    customApplyToBg = customThemeApplyToBg,
                    languageCode = languageCode
                ) {
                    CompositionLocalProvider(LocalThemeRevealState provides themeRevealState) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Actual content (new theme)
                            if (isFirstTime) {
                                WelcomeScreen(
                                    viewModel = viewModel,
                                    onGetStarted = { 
                                        viewModel.completeWelcome()
                                        showBatteryDialog = true
                                    }
                                )
                            } else {
                                TodoApp(viewModel = viewModel)
                            }

                            // Old-theme bitmap overlay with circular hole
                            val bitmap = overlayBitmap
                            if (bitmap != null) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(100f)
                                        .graphicsLayer {
                                            // Required for BlendMode.Clear to work properly against the canvas
                                            compositingStrategy = CompositingStrategy.Offscreen
                                        }
                                ) {
                                    // Draw the old screen snapshot
                                    drawImage(bitmap)
                                    // Punch a growing circular hole through it to reveal the new theme underneath
                                    drawCircle(
                                        color = Color.Transparent,
                                        radius = revealRadius.value,
                                        center = revealCenter,
                                        blendMode = BlendMode.Clear
                                    )
                                }
                            }
                            
                            if (showBatteryDialog) {
                                val batteryTitle = stringResource(R.string.battery_opt_title)
                                val batteryMessage = stringResource(R.string.battery_opt_message)
                                val openSettingsText = stringResource(R.string.open_settings)
                                val cancelText = stringResource(R.string.cancel)

                                AlertDialog(
                                    onDismissRequest = { showBatteryDialog = false },
                                    title = { Text(batteryTitle) },
                                    text = { Text(batteryMessage) },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            showBatteryDialog = false
                                            try {
                                                val intent = Intent().apply {
                                                    action = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                                                }
                                                startActivity(intent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }) {
                                            Text(openSettingsText)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showBatteryDialog = false }) {
                                            Text(cancelText)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Context.findActivity(): android.app.Activity? {
    var cur = this
    while (cur is ContextWrapper) {
        if (cur is android.app.Activity) {
            return cur
        }
        cur = cur.baseContext
    }
    return null
}

@SuppressLint("LocalContextConfigurationRead")
@Composable
fun LocalizedContent(localeCode: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val locale = remember(localeCode) { Locale(localeCode) }

    val localizedContext = remember(localeCode) {
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        val configContext = context.createConfigurationContext(configuration)
        object : android.content.ContextWrapper(context) {
            override fun getResources(): android.content.res.Resources {
                return configContext.resources
            }
        }
    }

    val layoutDirection = remember(localeCode) {
        if (TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalLayoutDirection provides layoutDirection,
        androidx.compose.ui.platform.LocalConfiguration provides localizedContext.resources.configuration
    ) {
        androidx.compose.runtime.key(localeCode) {
            content()
        }
    }
}
