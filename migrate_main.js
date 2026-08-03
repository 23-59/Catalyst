const fs = require('fs');

const path = 'app/src/main/java/com/goldenminute/catalyst/MainActivity.kt';
let content = fs.readFileSync(path, 'utf8');

// 1. Add viewModels import
content = content.replace('import androidx.lifecycle.viewmodel.compose.viewModel', 'import androidx.lifecycle.viewmodel.compose.viewModel\nimport androidx.activity.viewModels');

// 2. Remove attachBaseContext
content = content.replace(/    override fun attachBaseContext\(newBase: Context\) \{[\s\S]*?super\.attachBaseContext\(localizedContext\)\n    \}\n/m, '');

// 3. Update onCreate logic
const onCreateOld = `    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val themeKey = sharedPrefs.getString("theme", "cosmic_dark") ?: "cosmic_dark"
        val customColor = sharedPrefs.getInt("custom_theme_color", android.graphics.Color.parseColor("#9D4EDD"))
        val customApplyToBg = sharedPrefs.getBoolean("custom_theme_apply_bg", true)
        
        val palette = com.goldenminute.catalyst.ui.theme.getThemePalette(themeKey, customColor, customApplyToBg)
        val isLight = themeKey == "slate_light" || themeKey.startsWith("custom_light")

        enableEdgeToEdge(
            statusBarStyle = if (isLight) {
                androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK)
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
        setContent {
            val viewModel: TodoViewModel = viewModel()
            val lifecycleOwner = LocalLifecycleOwner.current
            val context = LocalContext.current`;

const onCreateNew = `    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val viewModel: TodoViewModel by viewModels()

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

            val palette = com.goldenminute.catalyst.ui.theme.getThemePalette(themeKey, customThemeColor, customThemeApplyToBg)
            val isLight = themeKey == "slate_light" || themeKey.startsWith("custom_light")

            LaunchedEffect(themeKey, customThemeColor, customThemeApplyToBg) {
                enableEdgeToEdge(
                    statusBarStyle = if (isLight) {
                        androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK)
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
            }`;
content = content.replace(onCreateOld, onCreateNew);

// 4. Remove duplicated val themeKey etc later in setContent
const duplicateStates = `            val themeKey by viewModel.appTheme.collectAsState()
            val customThemeColor by viewModel.customThemeColor.collectAsState()
            val customThemeApplyToBg by viewModel.customThemeApplyToBg.collectAsState()
            val languageCode by viewModel.appLanguage.collectAsState()
            val isFirstTime by viewModel.isFirstTime.collectAsState()
            `;
content = content.replace(duplicateStates, '');

// 5. Update LocalizedContent
const localizedContentOld = `@Suppress("DEPRECATION")
@Composable
fun LocalizedContent(localeCode: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val locale = remember(localeCode) { Locale(localeCode) }

    LaunchedEffect(localeCode) {
        val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        if (currentLocale?.language != locale.language) {
            val activity = context.findActivity()
            activity?.let {
                it.recreate()
                @Suppress("DEPRECATION")
                it.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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
        LocalLayoutDirection provides layoutDirection
    ) {
        androidx.compose.runtime.key(localeCode) {
            content()
        }
    }
}`;

const localizedContentNew = `@Composable
fun LocalizedContent(localeCode: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val locale = remember(localeCode) { Locale(localeCode) }

    val localizedContext = remember(localeCode) {
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        context.createConfigurationContext(configuration)
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
        LocalLayoutDirection provides layoutDirection
    ) {
        androidx.compose.runtime.key(localeCode) {
            content()
        }
    }
}`;
content = content.replace(localizedContentOld, localizedContentNew);

fs.writeFileSync(path, content, 'utf8');
console.log('Success MainActivity');
