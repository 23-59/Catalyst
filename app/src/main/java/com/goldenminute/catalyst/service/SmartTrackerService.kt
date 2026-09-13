package com.goldenminute.catalyst.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.goldenminute.catalyst.data.TodoDatabase
import com.goldenminute.catalyst.ui.TimerStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.goldenminute.catalyst.data.appDataStore
import com.goldenminute.catalyst.data.DataStoreKeys

class SmartTrackerService : AccessibilityService() {

    companion object {
        private const val TAG = "SmartTracker"

        // 🎯 Distracting apps registry 🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯
        // Package names of apps considered distracting by default.
        val DEFAULT_DISTRACTING_PACKAGES = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",   // TikTok (international)
            "com.ss.android.ugc.trill",   // TikTok (some regions)
            "com.twitter.android",
            "com.google.android.youtube",
            "com.reddit.frontpage",
            "com.snapchat.android",
            "com.facebook.katana",
            "com.facebook.orca",          // Messenger
            "com.pinterest",
            "com.linkedin.android"
        )
        
        var activeDistractingPackages: Set<String> = DEFAULT_DISTRACTING_PACKAGES
        var isDoomscrollingAlertEnabled: Boolean = false

        // Detectable sections per known app — used by the UI to show section pickers
        val KNOWN_APP_SECTIONS: Map<String, List<String>> = mapOf(
            "com.instagram.android" to listOf("Home", "Explore", "Reels", "Messages", "Profile", "Create", "Activity"),
            "com.zhiliaoapp.musically" to listOf("ForYou", "Discover", "Inbox", "Profile"),
            "com.ss.android.ugc.trill" to listOf("ForYou", "Discover", "Inbox", "Profile"),
            "com.twitter.android" to listOf("Home", "Explore", "Notifications", "Messages", "Communities"),
            "com.google.android.youtube" to listOf("Home", "Shorts", "Subscriptions", "Library"),
            "com.reddit.frontpage" to listOf("Home", "Popular", "Create", "Chat", "Profile"),
            "com.snapchat.android" to listOf("Camera", "Chat", "Discover", "Spotlight", "Map"),
            "com.facebook.katana" to listOf("Home", "Watch", "Reels", "Marketplace", "Notifications", "Menu"),
            "com.pinterest" to listOf("Home", "Explore"),
        )

        /**
         * Parses the associatedSections string into a map of packageName → set of section names.
         * Format: "pkg1:Section1|Section2,pkg2:Section3"
         * Returns empty map if null/empty (meaning whole-app tracking).
         */
        fun parseSectionsString(sectionsStr: String?): Map<String, Set<String>> {
            if (sectionsStr.isNullOrBlank()) return emptyMap()
            val result = mutableMapOf<String, MutableSet<String>>()
            for (entry in sectionsStr.split(",")) {
                val parts = entry.split(":", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    val pkg = parts[0].trim()
                    val sections = parts[1].split("|").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
                    if (sections.isNotEmpty()) {
                        result.getOrPut(pkg) { mutableSetOf() }.addAll(sections)
                    }
                }
            }
            return result
        }

        /**
         * Serializes a map of packageName → set of section names into the storage format.
         */
        fun serializeSectionsMap(map: Map<String, Set<String>>): String? {
            if (map.isEmpty()) return null
            // Filter out empty section sets
            val filtered = map.filter { it.value.isNotEmpty() }
            if (filtered.isEmpty()) return null
            return filtered.entries.joinToString(",") { (pkg, sections) ->
                "$pkg:${sections.joinToString("|")}"
            }
        }

        /**
         * Localizes section names to Persian when isPersian is true.
         */
        fun localizeSectionName(section: String, isPersian: Boolean): String {
            if (!isPersian) return section
            return when (section.lowercase(java.util.Locale.ROOT)) {
                "general" -> "عمومی"
                "home" -> "خانه"
                "explore" -> "اکسپلور"
                "reels" -> "ریلز"
                "messages", "message", "direct", "inbox", "chat" -> "پیام‌ها"
                "profile" -> "پروفایل"
                "create" -> "ایجاد"
                "foryou" -> "برای شما"
                "discover" -> "کشف"
                "shorts" -> "شورتز"
                "subscriptions" -> "اشتراک‌ها"
                "library" -> "کتابخانه"
                "popular" -> "محبوب"
                "watch" -> "تماشا"
                "camera" -> "دوربین"
                "spotlight" -> "اسپات‌لایت"
                "map" -> "نقشه"
                "marketplace" -> "بازار"
                "menu" -> "منو"
                "communities" -> "انجمن‌ها"
                "notifications", "notification" -> "اعلان‌ها"
                "activity" -> "فعالیت"
                "shop" -> "فروشگاه"
                "unknown" -> "نامشخص"
                else -> section
            }
        }

        /**
         * Resolves the user-facing app display name, supporting Persian when isPersian is true.
         */
        fun getAppDisplayName(packageName: String, context: Context? = null, isPersian: Boolean = false): String {
            val englishName = try {
                if (context != null) {
                    val pm = context.packageManager
                    val info = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(info).toString()
                } else null
            } catch (_: Exception) {
                null
            } ?: when (packageName) {
                "com.instagram.android" -> "Instagram"
                "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> "TikTok"
                "com.twitter.android" -> "X (Twitter)"
                "com.google.android.youtube" -> "YouTube"
                "com.reddit.frontpage" -> "Reddit"
                "com.snapchat.android" -> "Snapchat"
                "com.facebook.katana" -> "Facebook"
                "com.facebook.orca" -> "Messenger"
                "com.pinterest" -> "Pinterest"
                "com.linkedin.android" -> "LinkedIn"
                else -> packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
            }

            if (!isPersian) return englishName

            return when (packageName) {
                "com.instagram.android" -> "اینستاگرام"
                "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> "تیک‌تاک"
                "com.twitter.android" -> "ایکس (توییتر)"
                "com.google.android.youtube" -> "یوتیوب"
                "com.reddit.frontpage" -> "ردیت"
                "com.snapchat.android" -> "اسنپ‌چت"
                "com.facebook.katana" -> "فیسبوک"
                "com.facebook.orca" -> "مسنجر"
                "com.pinterest" -> "پینترست"
                "com.linkedin.android" -> "لینکدین"
                else -> englishName
            }
        }

        // Sections considered "distracting" for each app — spending time
        // in these sections will trigger a mindfulness check.
        // Sections NOT listed here are considered productive (e.g. DMs, Profile).
        private val DISTRACTING_SECTIONS: Map<String, Set<String>> = mapOf(
            "com.instagram.android" to setOf("Home", "Explore", "Reels"),
            "com.zhiliaoapp.musically" to setOf("Home", "ForYou", "Discover"),
            "com.ss.android.ugc.trill" to setOf("Home", "ForYou", "Discover"),
            "com.twitter.android" to setOf("Home", "Explore"),
            "com.google.android.youtube" to setOf("Home", "Shorts"),
            "com.reddit.frontpage" to setOf("Home", "Popular"),
            "com.snapchat.android" to setOf("Discover", "Spotlight"),
            "com.facebook.katana" to setOf("Home", "Watch", "Reels"),
            "com.pinterest" to setOf("Home", "Explore"),
        )

        // ── System packages that indicate user has left an app ──
        private val SYSTEM_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",   // Samsung
            "com.huawei.android.launcher",    // Huawei
            "com.miui.home",                  // Xiaomi
            "com.oppo.launcher",              // Oppo
            "com.realme.launcher",            // Realme
            "com.oneplus.launcher",           // OnePlus
            "com.lge.launcher3",             // LG
            "com.sonymobile.home",           // Sony
            "com.asus.launcher",             // Asus
            "com.motorola.launcher3",        // Motorola
            "com.android.settings",
            "com.android.dialer",
            "com.android.phone"
        )

        // ── Session tracking state ─────────────────────────────────────
        val sessionAppSections = mutableMapOf<String, Long>()
        var currentSection: String = "Unknown"
        var currentSectionStartTime = System.currentTimeMillis()
        var currentAppPackage: String = ""

        // Per-app independent time tracking for distracting-app monitoring.
        // Maps package name → timestamp when we started tracking this app.
        private val appEntryTimes = mutableMapOf<String, Long>()
        // Maps package name → accumulated distracting-section time (ms).
        // This is reset each time the user LEAVES the app so each visit
        // starts a fresh 5-minute counter.
        private val appDistractingTime = mutableMapOf<String, Long>()
        // Per-app last-known distracting-section start time.
        private val appDistractingSectionStart = mutableMapOf<String, Long>()
        // Per-app last-known section
        private val appLastSection = mutableMapOf<String, String>()
        // Tracks the last-known selected bottom navigation section per package name
        val lastSelectedBottomNavSection = mutableMapOf<String, String>()

        // Cooldown per app to avoid spamming overlays
        private val appMindfulnessCooldownUntil = mutableMapOf<String, Long>()

        // Whether we are currently running and connected
        @Volatile var isRunning = false

        // Whether we are currently showing the overlay (prevents re-launch)
        @Volatile var isOverlayShowing = false
        @Volatile var overlayShownTime = 0L

        fun checkIsOverlayShowing(): Boolean {
            if (isOverlayShowing) {
                // Failsafe: if overlay has been marked showing for >40s without dismissal, auto-reset
                if (System.currentTimeMillis() - overlayShownTime > 40_000L) {
                    isOverlayShowing = false
                    overlayShownTime = 0L
                    return false
                }
                return true
            }
            return false
        }

        // State for Instagram Reels doomscrolling detection (header disappearance/reappearance/continued scroll)
        @Volatile var lastInstagramReelsHeaderVisible: Boolean? = null
        @Volatile var lastReelsDoomscrollTriggerTime: Long = 0L
        @Volatile var lastReelSignature: String = ""

        fun onMindfulnessDismissed() {
            isOverlayShowing = false
            overlayShownTime = 0L
            lastReelsDoomscrollTriggerTime = System.currentTimeMillis()
        }

        // State for tracking time on a Reel marked as doomscrolling
        @Volatile var isCurrentReelMarkedDoomscroll: Boolean = false
        @Volatile var currentReelDoomscrollStartTime: Long = 0L

        fun onDoomscrollingMarked(context: Context, packageName: String) {
            isCurrentReelMarkedDoomscroll = true
            currentReelDoomscrollStartTime = System.currentTimeMillis()
            Log.d(TAG, "Reel marked as doomscrolling! Started tracking watch time.")
        }

        fun finalizeCurrentDoomscrollVideo(context: Context) {
            if (isCurrentReelMarkedDoomscroll && currentReelDoomscrollStartTime > 0L) {
                val duration = System.currentTimeMillis() - currentReelDoomscrollStartTime
                val startTime = currentReelDoomscrollStartTime
                isCurrentReelMarkedDoomscroll = false
                currentReelDoomscrollStartTime = 0L

                if (duration >= 500L) {
                    Log.d(TAG, "Finalized doomscroll video: duration=${duration}ms")
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = TodoDatabase.getDatabase(context, this)
                            db.doomscrollVideoDao().insertRecord(
                                com.goldenminute.catalyst.data.DoomscrollVideoRecord(
                                    packageName = "com.instagram.android",
                                    timestamp = startTime,
                                    durationMs = duration
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving doomscroll video record", e)
                        }
                    }
                }
            }
        }

        fun resetSectionTracking(initialSection: String? = null) {
            sessionAppSections.clear()
            currentSectionStartTime = System.currentTimeMillis()
            currentSection = initialSection
                ?: (if (currentAppPackage.isNotEmpty()) lastSelectedBottomNavSection[currentAppPackage] else null)
                ?: (if (currentSection != "Unknown") currentSection else "General")
        }

        fun recordSectionTime(newSection: String) {
            if (newSection != currentSection) {
                val elapsed = System.currentTimeMillis() - currentSectionStartTime
                if (elapsed > 0) {
                    sessionAppSections[currentSection] = (sessionAppSections[currentSection] ?: 0L) + elapsed
                }
                currentSection = newSection
                currentSectionStartTime = System.currentTimeMillis()
            }
        }

        fun commitCurrentSectionTime() {
            val elapsed = System.currentTimeMillis() - currentSectionStartTime
            if (elapsed > 0) {
                sessionAppSections[currentSection] = (sessionAppSections[currentSection] ?: 0L) + elapsed
            }
            currentSectionStartTime = System.currentTimeMillis()
        }

        fun setMindfulnessCooldown(packageName: String, durationMs: Long = 15 * 60 * 1000L) {
            appMindfulnessCooldownUntil[packageName] = System.currentTimeMillis() + durationMs
            // Also reset distracting time tracking for this app so we start fresh
            appDistractingTime[packageName] = 0L
            appDistractingSectionStart[packageName] = System.currentTimeMillis()
        }

        fun resetDistractingTimeForApp(packageName: String) {
            appDistractingTime[packageName] = 0L
            appDistractingSectionStart.remove(packageName)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastPackageName = ""
    @Volatile private var isCurrentlyChecking = false
    private var livenessJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Log.d(TAG, "Service Created")
        TimerStateManager.initialize(applicationContext)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        Log.d(TAG, "Accessibility Service Connected")
        
        scope.launch {
            applicationContext.appDataStore.data.collect { prefs ->
                val apps = prefs[DataStoreKeys.DISTRACTING_APPS]
                activeDistractingPackages = apps ?: DEFAULT_DISTRACTING_PACKAGES
                isDoomscrollingAlertEnabled = prefs[DataStoreKeys.DOOMSCROLLING_ALERT_ENABLED] ?: false
            }
        }

        // ── Periodic liveness check ──
        // Every 3 seconds, verify the user is still inside the linked app.
        // Catches cases where TYPE_WINDOW_STATE_CHANGED doesn't fire
        // (home gesture, screen off, etc.).
        startLivenessCheck()
    }

    private fun startLivenessCheck() {
        livenessJob?.cancel()
        livenessJob = scope.launch {
            while (true) {
                delay(3000L)
                try {
                    if (!TimerStateManager.isTimerRunning.value) continue
                    val activeTaskId = TimerStateManager.activeTaskId.value
                    if (activeTaskId == -1) continue

                    val currentPkg = lastPackageName
                    // If user is in a system package (launcher, settings, dialer, etc.)
                    // or our own app, they've left the assigned app
                    if (currentPkg.isEmpty()) continue

                    val db = TodoDatabase.getDatabase(applicationContext, scope)
                    val activeTodo = db.todoDao().getAllTodos().first().find { it.id == activeTaskId }
                    if (activeTodo != null && !activeTodo.associatedPackageNames.isNullOrEmpty()) {
                        val linkedApps = activeTodo.associatedPackageNames.split(",")
                        if (!linkedApps.contains(currentPkg)) {
                            // User is NOT in any linked app; auto-save
                            Log.d(TAG, "Liveness: user not in linked app ($currentPkg). Auto-saving task $activeTaskId.")
                            TimerStateManager.autoPauseAndSaveTask(activeTaskId)
                        } else {
                            // User is in a linked app — check section filter (case-insensitive)
                            val sectionsMap = parseSectionsString(activeTodo.associatedSections)
                            val requiredSections = sectionsMap[currentPkg]
                            if (requiredSections != null && requiredSections.isNotEmpty()) {
                                val detectedSection = currentSection
                                val isMatch = requiredSections.any { it.equals(detectedSection, ignoreCase = true) }
                                // Don't auto-pause when section is still "Unknown" — detection may not have completed yet
                                if (detectedSection != "Unknown" && !isMatch) {
                                    Log.d(TAG, "Liveness: section '$detectedSection' not in required $requiredSections. Auto-pausing task $activeTaskId.")
                                    TimerStateManager.autoPauseAndSaveTask(activeTaskId)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Liveness check error", e)
                }
            }
        }
    }

    private fun isIgnoredTransientPackage(packageName: String): Boolean {
        if (packageName == "com.goldenminute.catalyst" && checkIsOverlayShowing()) return true
        if (packageName == "android" || packageName == "com.android.systemui") return true
        // Keyboards / Input method editors
        if (packageName.contains("inputmethod") ||
            packageName.contains("keyboard") ||
            packageName == "com.google.android.inputmethod.latin" ||
            packageName == "com.samsung.android.honeyboard" ||
            packageName == "com.touchtype.swiftkey") {
            return true
        }
        return false
    }

    private fun detectBottomNavClickSection(event: AccessibilityEvent, packageName: String): String? {
        val source = event.source ?: return null
        val rect = android.graphics.Rect()
        try {
            source.getBoundsInScreen(rect)
        } catch (_: Exception) {
            return null
        }

        val screenHeight = applicationContext.resources.displayMetrics.heightPixels
        val screenWidth = applicationContext.resources.displayMetrics.widthPixels
        val bottomThreshold = screenHeight - (screenHeight * 0.15f).toInt()
        val desc = (source.contentDescription?.toString() ?: "").lowercase(java.util.Locale.ROOT)
        val text = (source.text?.toString() ?: "").lowercase(java.util.Locale.ROOT)
        val viewId = (source.viewIdResourceName ?: "").lowercase(java.util.Locale.ROOT)

        val isBottomArea = rect.centerY() >= bottomThreshold || rect.bottom >= (screenHeight * 0.88f).toInt()
        val cx = rect.centerX()

        if (packageName.contains("instagram")) {
            if (isBottomArea || viewId.contains("tab") || desc.contains("tab") || desc.contains("برگه")) {
                return when {
                    desc.contains("reel") || desc.contains("clips") || desc.contains("video") || desc.contains("ریلز") || desc.contains("ویدیو") || (isBottomArea && cx in (screenWidth * 0.20f).toInt()..(screenWidth * 0.40f).toInt()) -> "Reels"
                    desc.contains("direct") || desc.contains("message") || desc.contains("chat") || desc.contains("پیام") || desc.contains("دایرکت") || (isBottomArea && cx in (screenWidth * 0.40f).toInt()..(screenWidth * 0.60f).toInt()) -> "Messages"
                    desc.contains("search") || desc.contains("explore") || desc.contains("جستجو") || desc.contains("کاوش") || desc.contains("اکسپلور") || (isBottomArea && cx in (screenWidth * 0.60f).toInt()..(screenWidth * 0.80f).toInt()) -> "Explore"
                    desc.contains("profile") || desc.contains("پروفایل") || desc.contains("نمایه") || (isBottomArea && cx >= (screenWidth * 0.80f).toInt()) -> "Profile"
                    desc.contains("home") || desc.contains("feed") || desc.contains("خانه") || (isBottomArea && cx < (screenWidth * 0.20f).toInt()) -> "Home"
                    else -> null
                }
            }
        } else if (packageName.contains("musically") || packageName.contains("tiktok") || packageName.contains("trill")) {
            if (isBottomArea || viewId.contains("tab")) {
                return when {
                    desc.contains("discover") || desc.contains("search") || desc.contains("جستجو") || (isBottomArea && cx in (screenWidth * 0.20f).toInt()..(screenWidth * 0.40f).toInt()) -> "Discover"
                    desc.contains("inbox") || desc.contains("notification") || desc.contains("صندوق") || (isBottomArea && cx in (screenWidth * 0.60f).toInt()..(screenWidth * 0.80f).toInt()) -> "Inbox"
                    desc.contains("profile") || desc.contains("me") || desc.contains("پروفایل") || (isBottomArea && cx >= (screenWidth * 0.80f).toInt()) -> "Profile"
                    desc.contains("home") || desc.contains("for you") || desc.contains("خانه") || (isBottomArea && cx < (screenWidth * 0.20f).toInt()) -> "ForYou"
                    else -> null
                }
            }
        } else if (packageName.contains("youtube")) {
            if (isBottomArea || viewId.contains("pivot") || viewId.contains("tab")) {
                return when {
                    desc.contains("short") || desc.contains("شورتز") || (isBottomArea && cx in (screenWidth * 0.25f).toInt()..(screenWidth * 0.50f).toInt()) -> "Shorts"
                    desc.contains("subscription") || desc.contains("اشتراک") || (isBottomArea && cx in (screenWidth * 0.50f).toInt()..(screenWidth * 0.75f).toInt()) -> "Subscriptions"
                    desc.contains("library") || desc.contains("you") || desc.contains("کتابخانه") || (isBottomArea && cx >= (screenWidth * 0.75f).toInt()) -> "Library"
                    desc.contains("home") || desc.contains("خانه") || (isBottomArea && cx < (screenWidth * 0.25f).toInt()) -> "Home"
                    else -> null
                }
            }
        } else if (packageName.contains("twitter") || packageName.contains("x.android")) {
            if (isBottomArea || viewId.contains("tab")) {
                return when {
                    desc.contains("search") || desc.contains("explore") || desc.contains("جستجو") || (isBottomArea && cx in (screenWidth * 0.20f).toInt()..(screenWidth * 0.40f).toInt()) -> "Explore"
                    desc.contains("communities") || desc.contains("انجمن") || (isBottomArea && cx in (screenWidth * 0.40f).toInt()..(screenWidth * 0.60f).toInt()) -> "Communities"
                    desc.contains("notification") || desc.contains("اعلان") || (isBottomArea && cx in (screenWidth * 0.60f).toInt()..(screenWidth * 0.80f).toInt()) -> "Notifications"
                    desc.contains("message") || desc.contains("dm") || desc.contains("پیام") || (isBottomArea && cx >= (screenWidth * 0.80f).toInt()) -> "Messages"
                    desc.contains("home") || desc.contains("خانه") || (isBottomArea && cx < (screenWidth * 0.20f).toInt()) -> "Home"
                    else -> null
                }
            }
        } else if (packageName.contains("reddit")) {
            if (isBottomArea || viewId.contains("tab")) {
                return when {
                    desc.contains("popular") || desc.contains("discover") || (isBottomArea && cx in (screenWidth * 0.20f).toInt()..(screenWidth * 0.40f).toInt()) -> "Popular"
                    desc.contains("create") || desc.contains("ایجاد") || (isBottomArea && cx in (screenWidth * 0.40f).toInt()..(screenWidth * 0.60f).toInt()) -> "Create"
                    desc.contains("chat") || desc.contains("inbox") || desc.contains("پیام") || (isBottomArea && cx in (screenWidth * 0.60f).toInt()..(screenWidth * 0.80f).toInt()) -> "Chat"
                    desc.contains("profile") || desc.contains("پروفایل") || (isBottomArea && cx >= (screenWidth * 0.80f).toInt()) -> "Profile"
                    desc.contains("home") || desc.contains("خانه") || (isBottomArea && cx < (screenWidth * 0.20f).toInt()) -> "Home"
                    else -> null
                }
            }
        } else if (packageName.contains("facebook.katana")) {
            if (isBottomArea || viewId.contains("tab")) {
                return when {
                    desc.contains("watch") || desc.contains("video") || desc.contains("ویدیو") -> "Watch"
                    desc.contains("reel") || desc.contains("ریلز") -> "Reels"
                    desc.contains("marketplace") || desc.contains("بازار") -> "Marketplace"
                    desc.contains("notification") || desc.contains("اعلان") -> "Notifications"
                    desc.contains("menu") || desc.contains("منو") -> "Menu"
                    desc.contains("home") || desc.contains("feed") || desc.contains("خانه") -> "Home"
                    else -> null
                }
            }
        } else if (packageName.contains("snapchat")) {
            if (isBottomArea || viewId.contains("tab")) {
                return when {
                    desc.contains("chat") || desc.contains("پیام") -> "Chat"
                    desc.contains("discover") || desc.contains("stories") || desc.contains("کشف") -> "Discover"
                    desc.contains("spotlight") || desc.contains("اسپات") -> "Spotlight"
                    desc.contains("map") || desc.contains("نقشه") -> "Map"
                    desc.contains("camera") || desc.contains("دوربین") -> "Camera"
                    else -> null
                }
            }
        }

        return null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        // Ignore transient system UI, keyboard, and overlay packages so they don't break tracking or inject "General"
        if (isIgnoredTransientPackage(packageName)) return
        currentAppPackage = packageName

        // ★ Immediate detection of bottom navigation item selection via click event
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val clickedSection = detectBottomNavClickSection(event, packageName)
            if (clickedSection != null) {
                Log.d(TAG, "Bottom nav click detected: switched to '$clickedSection' in $packageName")
                lastSelectedBottomNavSection[packageName] = clickedSection
                val prev = currentSection
                currentSection = clickedSection
                recordSectionTime(clickedSection)
                if (clickedSection != prev) {
                    checkSectionChange(packageName, clickedSection)
                }
                updateDistractingTracking(packageName, clickedSection)
                scope.launch {
                    performMindfulnessCheck(packageName, clickedSection)
                }
            }
            return
        }

        // App switch detection: if package has changed to another non-transient app
        if (packageName != lastPackageName) {
            // Commit any time from the previous app/section before switching
            commitCurrentSectionTime()
            flushDistractingSection(lastPackageName)

            // Reset accumulated distracting time when LEAVING an app.
            if (lastPackageName.isNotEmpty()) {
                appDistractingTime[lastPackageName] = 0L
                appDistractingSectionStart.remove(lastPackageName)
                if (lastPackageName == "com.instagram.android" && !checkIsOverlayShowing()) {
                    lastInstagramReelsHeaderVisible = null
                    lastReelSignature = ""
                    finalizeCurrentDoomscrollVideo(applicationContext)
                    pendingDoomscrollJob?.cancel()
                }
            }

            lastPackageName = packageName
            currentAppPackage = packageName

            scope.launch {
                try {
                    val rootNode = rootInActiveWindow
                    if (rootNode != null) {
                        val detectedSection = detectSection(rootNode, packageName)
                        currentSection = detectedSection
                        currentSectionStartTime = System.currentTimeMillis()
                        if (packageName == "com.instagram.android") {
                            checkInstagramReelsDoomscrolling(rootNode, packageName, isScrollEvent = false)
                        }
                        rootNode.recycle()
                    } else {
                        currentSection = getDefaultSection(packageName)
                        currentSectionStartTime = System.currentTimeMillis()
                    }
                } catch (_: Exception) {
                    currentSection = getDefaultSection(packageName)
                    currentSectionStartTime = System.currentTimeMillis()
                }
                checkAppAssociation(packageName)
            }
        } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Internal activity/window transition within the same app
            scope.launch {
                try {
                    val rootNode = rootInActiveWindow
                    if (rootNode != null) {
                        val detectedSection = detectSection(rootNode, packageName)
                        val previousSection = currentSection
                        if (detectedSection != "Unknown" || currentSection == "Unknown") {
                            recordSectionTime(detectedSection)
                        }
                        if (detectedSection != previousSection && detectedSection != "Unknown") {
                            checkSectionChange(packageName, detectedSection)
                        }
                        if (packageName == "com.instagram.android") {
                            checkInstagramReelsDoomscrolling(rootNode, packageName, isScrollEvent = false)
                        }
                        rootNode.recycle()
                    }
                } catch (_: Exception) { }
            }
        }

        // Dedicated fast handler for Instagram Reels scroll events
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && packageName == "com.instagram.android" && isDoomscrollingAlertEnabled && !checkIsOverlayShowing()) {
            scope.launch {
                try {
                    val root = rootInActiveWindow
                    if (root != null) {
                        checkInstagramReelsDoomscrolling(root, packageName, isScrollEvent = true)
                        root.recycle()
                    }
                } catch (_: Exception) {}
            }
        }

        // Mindfulness check & section tracking on content/scroll/window changes
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            if (packageName == lastPackageName && !isCurrentlyChecking) {
                isCurrentlyChecking = true
                scope.launch {
                    try {
                        val rootNode = rootInActiveWindow
                        if (rootNode != null) {
                            val detectedSection = detectSection(rootNode, packageName)
                            val previousSection = currentSection
                            if (detectedSection != "Unknown" || currentSection == "Unknown") {
                                recordSectionTime(detectedSection)
                            }
                            if (detectedSection != previousSection && detectedSection != "Unknown") {
                                checkSectionChange(packageName, detectedSection)
                            }
                            updateDistractingTracking(packageName, detectedSection)
                            performMindfulnessCheck(packageName, detectedSection)
                            if (packageName == "com.instagram.android") {
                                val isScroll = (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED)
                                checkInstagramReelsDoomscrolling(rootNode, packageName, isScrollEvent = isScroll)
                            }
                            rootNode.recycle()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in content/scroll change handler", e)
                    } finally {
                        isCurrentlyChecking = false
                    }
                }
            }
        }
    }

    /** Resolve the device's home launcher package(s) dynamically. */
    private fun resolveHomePackages(): Set<String> {
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_HOME)
            }
            val resolveInfos = applicationContext.packageManager.queryIntentActivities(intent, 0)
            resolveInfos.mapNotNull { it.activityInfo?.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    /** Returns a sensible default section for known apps instead of "Unknown". */
    private fun getDefaultSection(packageName: String): String {
        val lastSelected = lastSelectedBottomNavSection[packageName]
        if (!lastSelected.isNullOrEmpty()) {
            return lastSelected
        }

        return when {
            packageName.contains("instagram") -> "Home"
            packageName.contains("musically") || packageName.contains("tiktok") || packageName.contains("trill") -> "ForYou"
            packageName.contains("twitter") || packageName.contains("x.android") -> "Home"
            packageName.contains("youtube") -> "Home"
            packageName.contains("reddit") -> "Home"
            packageName.contains("facebook.katana") -> "Home"
            packageName.contains("snapchat") -> "Camera"
            else -> "General"
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    // ═══════════════════════════════════════════════════════════════
    // Task association (existing behavior, unchanged)
    // ═══════════════════════════════════════════════════════════════

    private fun checkAppAssociation(packageName: String) {
        scope.launch {
            val db = TodoDatabase.getDatabase(applicationContext, scope)
            val allTodos = db.todoDao().getAllTodos().first()

            val linkedTodo = allTodos.find { it.associatedPackageNames?.split(",")?.contains(packageName) == true && !it.isCompleted }

            if (linkedTodo != null) {
                // Check if section-level filtering applies for this package
                val sectionsMap = parseSectionsString(linkedTodo.associatedSections)
                val requiredSections = sectionsMap[packageName]

                if (requiredSections != null && requiredSections.isNotEmpty()) {
                    // Section-aware: only start/continue if in the right section (case-insensitive)
                    val detectedSection = currentSection
                    val isMatch = requiredSections.any { it.equals(detectedSection, ignoreCase = true) }
                    if (isMatch) {
                        // User is in a matching section — start or continue
                        val currentActiveTask = TimerStateManager.activeTaskId.value
                        val timerRunning = TimerStateManager.isTimerRunning.value
                        if (currentActiveTask == linkedTodo.id && timerRunning) {
                            Log.d(TAG, "Timer already running for task ${linkedTodo.id} in section '$detectedSection', continuing.")
                        } else {
                            Log.d(TAG, "Starting timer for task ${linkedTodo.id} (section '$detectedSection' matches)")
                            TimerStateManager.isPomodoro.value = (linkedTodo.timerMode == "Pomodoro")
                            TimerStateManager.autoStartTask(linkedTodo.id)
                            resetSectionTracking(detectedSection)
                        }
                    } else if (detectedSection != "Unknown") {
                        // User is in the linked app but WRONG section — pause if running for this task
                        val currentActiveTask = TimerStateManager.activeTaskId.value
                        if (currentActiveTask == linkedTodo.id && TimerStateManager.isTimerRunning.value) {
                            Log.d(TAG, "Section '$detectedSection' not in required sections $requiredSections. Auto-pausing task ${linkedTodo.id}.")
                            TimerStateManager.autoPauseAndSaveTask(linkedTodo.id)
                        }
                    }
                } else {
                    // No section filter — whole-app tracking (existing behavior)
                    val currentActiveTask = TimerStateManager.activeTaskId.value
                    val timerRunning = TimerStateManager.isTimerRunning.value
                    if (currentActiveTask == linkedTodo.id && timerRunning) {
                        Log.d(TAG, "Timer already running for task ${linkedTodo.id}, skipping re-start.")
                    } else {
                        Log.d(TAG, "Starting timer for task ${linkedTodo.id}")
                        TimerStateManager.isPomodoro.value = (linkedTodo.timerMode == "Pomodoro")
                        TimerStateManager.autoStartTask(linkedTodo.id)
                        resetSectionTracking(currentSection)
                    }
                }
            } else {
                if (TimerStateManager.isTimerRunning.value) {
                    val activeTaskId = TimerStateManager.activeTaskId.value
                    if (activeTaskId != -1) {
                        val activeTodo = allTodos.find { it.id == activeTaskId }
                        if (activeTodo != null && !activeTodo.associatedPackageNames.isNullOrEmpty()) {
                            val associatedList = activeTodo.associatedPackageNames.split(",")
                            if (!associatedList.contains(packageName)) {
                                Log.d(TAG, "User switched away from linked app. Pausing and auto-saving task $activeTaskId.")
                                TimerStateManager.autoPauseAndSaveTask(activeTaskId)
                            }
                        }
                    }

                    // Handle free/idle mode
                    if (activeTaskId == -1 && TimerStateManager.isIdleMode.value && packageName != "com.goldenminute.catalyst") {
                        if (activeDistractingPackages.contains(packageName)) {
                            Log.d(TAG, "Free mode: user opened distracting app $packageName. Pausing idle timer.")
                            TimerStateManager.autoPauseTask(idle = true)
                        }
                    }
                }
            }
        }
    }

    /**
     * Called when a section change is detected within the same app.
     * Re-evaluates whether a section-filtered task should start or pause.
     */
    private fun checkSectionChange(packageName: String, newSection: String) {
        scope.launch {
            val db = TodoDatabase.getDatabase(applicationContext, scope)
            val allTodos = db.todoDao().getAllTodos().first()

            // Find tasks linked to this package with section filters
            val linkedTodo = allTodos.find {
                it.associatedPackageNames?.split(",")?.contains(packageName) == true && !it.isCompleted
            } ?: return@launch

            val sectionsMap = parseSectionsString(linkedTodo.associatedSections)
            val requiredSections = sectionsMap[packageName]
            if (requiredSections == null || requiredSections.isEmpty()) return@launch // No section filter

            val activeTaskId = TimerStateManager.activeTaskId.value
            val timerRunning = TimerStateManager.isTimerRunning.value
            val isMatch = requiredSections.any { it.equals(newSection, ignoreCase = true) }

            if (isMatch) {
                // Entered a matching section — start timer if not already running for this task
                if (activeTaskId != linkedTodo.id || !timerRunning) {
                    Log.d(TAG, "Section changed to '$newSection' (matches). Starting timer for task ${linkedTodo.id}.")
                    TimerStateManager.isPomodoro.value = (linkedTodo.timerMode == "Pomodoro")
                    TimerStateManager.autoStartTask(linkedTodo.id)
                }
            } else if (newSection != "Unknown") {
                // Only pause when we're sure about the section (not "Unknown")
                // Left the matching section — pause if running for this task
                if (activeTaskId == linkedTodo.id && timerRunning) {
                    Log.d(TAG, "Section changed to '$newSection' (not in $requiredSections). Auto-pausing task ${linkedTodo.id}.")
                    TimerStateManager.autoPauseAndSaveTask(linkedTodo.id)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Section Detection — app-specific logic
    // ═══════════════════════════════════════════════════════════════

    private fun detectSection(root: AccessibilityNodeInfo, packageName: String): String {
        val detected = when {
            packageName.contains("instagram") -> detectInstagramSection(root)
            packageName.contains("musically") || packageName.contains("tiktok") || packageName.contains("trill") -> detectTikTokSection(root, packageName)
            packageName.contains("twitter") || packageName.contains("x.android") -> detectTwitterSection(root, packageName)
            packageName.contains("youtube") -> detectYouTubeSection(root, packageName)
            packageName.contains("reddit") -> detectRedditSection(root, packageName)
            packageName.contains("facebook.katana") -> detectFacebookSection(root, packageName)
            packageName.contains("snapchat") -> detectSnapchatSection(root, packageName)
            else -> detectGenericSection(root)
        }
        if (detected != "Unknown" && detected != "General") {
            lastSelectedBottomNavSection[packageName] = detected
        }
        return detected
    }

    /**
     * Instagram section detection.
     * Strategy:
     * 1. Coordinate-based Bottom Navigation Bar Analysis:
     *    Index 0: Home, 1: Reels, 2: Messages (Paperrocket), 3: Explore (Magnifier), 4: Profile.
     * 2. Dedicated Reels screen detection (e.g. fullscreen Reels infinite scroll with right action rail, Follow button, etc.).
     * 3. Deep-traversal UI heuristics for Messages, Profile, Explore, Create, Home.
     * 4. ★ PERSISTENCE: Continues to record for the LAST SELECTED bottom navigation item
     *    until the app finds out the selected item has changed!
     */
    private fun detectInstagramSection(root: AccessibilityNodeInfo): String {
        val screenBounds = android.graphics.Rect()
        root.getBoundsInScreen(screenBounds)
        val screenHeight = if (screenBounds.height() > 0) screenBounds.height() else applicationContext.resources.displayMetrics.heightPixels
        val screenWidth = if (screenBounds.width() > 0) screenBounds.width() else applicationContext.resources.displayMetrics.widthPixels

        // 1. Check if the bottom navigation bar actively reports a selected tab
        val bottomSection = detectInstagramBottomNavSection(root, screenWidth, screenHeight)
        if (bottomSection != null) {
            lastSelectedBottomNavSection["com.instagram.android"] = bottomSection
            return bottomSection
        }

        // 2. Dedicated Reels Detection (fast & reliable when doomscrolling reels)
        if (isInstagramReelsScreen(root, screenWidth, screenHeight)) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Reels"
            return "Reels"
        }

        // 3. Screen content heuristics with deep traversal
        val visibleTexts = mutableListOf<String>()
        collectVisibleTexts(root, visibleTexts, maxDepth = 25, maxNodes = 600)
        val allText = visibleTexts.joinToString(" ").lowercase(java.util.Locale.ROOT)

        val contentDescs = mutableListOf<String>()
        collectContentDescriptions(root, contentDescs, maxDepth = 25, maxNodes = 600)
        val allDescs = contentDescs.joinToString(" ").lowercase(java.util.Locale.ROOT)

        // Direct Messages / Inbox
        if ((allText.contains("messages") && allText.contains("chats")) ||
            (allDescs.contains("direct message") && !allDescs.contains("like") && !allDescs.contains("لایک")) ||
            allText.contains("search chats") || allText.contains("جستجو در گفتگوها")) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Messages"
            return "Messages"
        }

        // Profile (own profile header)
        if ((allText.contains("edit profile") || allText.contains("ویرایش نمایه") ||
            allText.contains("share profile") || allText.contains("هم‌رسانی نمایه")) &&
            (allText.contains("followers") || allText.contains("following") || allText.contains("دنبال‌کنندگان") || allText.contains("posts"))) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Profile"
            return "Profile"
        }

        // Explore (search grid)
        if ((allText.contains("search") || allText.contains("جستجو")) &&
            (allText.contains("for you") && allText.contains("tags") || allText.contains("explore"))) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Explore"
            return "Explore"
        }

        // Create / Camera
        if ((allText.contains("story") && allText.contains("reel") && allText.contains("live")) ||
            allDescs.contains("camera shutter") || allDescs.contains("شاتر")) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Create"
            return "Create"
        }

        // Home feed (Stories tray with "Your story" / "استوری شما")
        if (allText.contains("your story") || allText.contains("استوری شما")) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Home"
            return "Home"
        }

        // Additional check for Reels from texts
        if (allDescs.contains("reels") || allDescs.contains("ریلز") ||
            visibleTexts.any { it.trim().equals("reels", ignoreCase = true) || it.trim().equals("ریلز", ignoreCase = true) } ||
            allText.contains("made with edits") || allText.contains("original audio") || allText.contains("صدای اصلی")) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Reels"
            return "Reels"
        }

        // 4. ★ PERSISTENCE: Continue recording the screen/s related to the LAST SELECTED bottom navigation item
        // until the app finds out the selected item has changed!
        val lastSelected = lastSelectedBottomNavSection["com.instagram.android"]
        if (!lastSelected.isNullOrEmpty()) {
            return lastSelected
        }

        // Initial default if no tab has ever been detected yet
        return "Home"
    }

    private fun detectInstagramBottomNavSection(root: AccessibilityNodeInfo, screenWidth: Int, screenHeight: Int): String? {
        val bottomNavMinY = screenHeight - (screenHeight * 0.12f).toInt()

        // 1. Direct container check: look for known/standard bottom bar container
        val bottomBar = findNodeByViewIdContains(root, "tab_bar")
            ?: findNodeByViewIdContains(root, "bottom_bar")
            ?: findNodeByViewIdContains(root, "bottom_navigation")
            ?: findNodeByViewIdContains(root, "tab_layout")
            ?: findNodeByViewIdContains(root, "fixed_bottomsheet_container")
            ?: findNodeByViewIdContains(root, "tab_button")?.parent

        if (bottomBar != null && bottomBar.childCount in 4..6) {
            for (i in 0 until bottomBar.childCount) {
                val tab = bottomBar.getChild(i) ?: continue
                if (tab.isSelected || findSelectedInSubtree(tab) != null) {
                    return sectionNameForBottomNavIndex(i)
                }
                val desc = (tab.contentDescription?.toString() ?: "").lowercase(java.util.Locale.ROOT)
                val stateDesc = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    (tab.stateDescription?.toString() ?: "").lowercase(java.util.Locale.ROOT)
                } else ""
                if (desc.contains("selected") || desc.contains("active") || desc.contains("انتخاب") || desc.contains("فعال") ||
                    stateDesc.contains("selected") || stateDesc.contains("active") || stateDesc.contains("انتخاب") || stateDesc.contains("فعال")) {
                    return sectionNameForBottomNavIndex(i)
                }
            }
        }

        // 2. Selected tab by position / coordinates
        val selectedTab = findBottomNavSelectedTab(root)
        if (selectedTab != null) {
            val tabRect = android.graphics.Rect()
            selectedTab.getBoundsInScreen(tabRect)
            if (tabRect.centerY() >= bottomNavMinY) {
                val cx = tabRect.centerX()
                val idx = when {
                    cx < screenWidth * 0.20f -> 0
                    cx < screenWidth * 0.40f -> 1
                    cx < screenWidth * 0.60f -> 2
                    cx < screenWidth * 0.80f -> 3
                    else -> 4
                }
                return sectionNameForBottomNavIndex(idx)
            }
        }

        // 3. Scan nodes in the bottom strip (bottom ~12% of screen)
        val bottomNodes = mutableListOf<AccessibilityNodeInfo>()
        findNodesInScreenArea(root, bottomNavMinY, screenHeight, bottomNodes, maxDepth = 25)
        if (bottomNodes.isEmpty()) return null

        // 5 buckets across width (0..4)
        // 0: 0%..20% W -> Home
        // 1: 20%..40% W -> Reels
        // 2: 40%..60% W -> Messages (Paperrocket)
        // 3: 60%..80% W -> Explore (Magnifier)
        // 4: 80%..100% W -> Profile
        val bucketNodes = Array(5) { mutableListOf<AccessibilityNodeInfo>() }
        val rect = android.graphics.Rect()

        for (node in bottomNodes) {
            try {
                node.getBoundsInScreen(rect)
                if (rect.width() <= 0 || rect.height() <= 0) continue
                if (rect.width() > screenWidth * 0.45f) continue

                val cx = rect.centerX()
                val bucket = when {
                    cx < screenWidth * 0.20f -> 0
                    cx < screenWidth * 0.40f -> 1
                    cx < screenWidth * 0.60f -> 2
                    cx < screenWidth * 0.80f -> 3
                    else -> 4
                }
                bucketNodes[bucket].add(node)
            } catch (_: Exception) { }
        }

        // Check each bucket for explicit isSelected or isSelected in subtree or stateDescription
        for (i in 0..4) {
            val nodes = bucketNodes[i]
            for (node in nodes) {
                if (node.isSelected || findSelectedInSubtree(node) != null) {
                    return sectionNameForBottomNavIndex(i)
                }
                val desc = (node.contentDescription?.toString() ?: "").lowercase(java.util.Locale.ROOT)
                val stateDesc = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    (node.stateDescription?.toString() ?: "").lowercase(java.util.Locale.ROOT)
                } else ""

                if (desc.contains("selected") || desc.contains("active") || desc.contains("انتخاب") || desc.contains("فعال") ||
                    stateDesc.contains("selected") || stateDesc.contains("active") || stateDesc.contains("انتخاب") || stateDesc.contains("فعال")) {
                    return sectionNameForBottomNavIndex(i)
                }
            }
        }

        // Check for specific tab content descriptions / view IDs
        for (i in 0..4) {
            val nodes = bucketNodes[i]
            for (node in nodes) {
                val desc = (node.contentDescription?.toString() ?: "").lowercase(java.util.Locale.ROOT)
                val viewId = (node.viewIdResourceName ?: "").lowercase(java.util.Locale.ROOT)

                if (i == 1 && (desc.contains("reel") || desc.contains("ریلز") || desc.contains("clips") || viewId.contains("clips"))) {
                    if (node.isFocused || node.isSelected || desc.contains("tab") || desc.contains("selected")) {
                        return "Reels"
                    }
                }
            }
        }

        return null
    }

    private fun sectionNameForBottomNavIndex(index: Int): String {
        return when (index) {
            0 -> "Home"
            1 -> "Reels"
            2 -> "Messages"
            3 -> "Explore"
            4 -> "Profile"
            else -> "Home"
        }
    }

    private fun isInstagramReelsScreen(root: AccessibilityNodeInfo, screenWidth: Int, screenHeight: Int): Boolean {
        // Fast ID checks
        if (findNodeByViewIdContains(root, "clips") != null ||
            findNodeByViewIdContains(root, "reel_viewer") != null ||
            findNodeByViewIdContains(root, "reel_recycler") != null ||
            findNodeByViewIdContains(root, "unified_video_viewer") != null) {
            return true
        }

        val visibleTexts = mutableListOf<String>()
        val contentDescs = mutableListOf<String>()
        val rightRailNodes = mutableListOf<AccessibilityNodeInfo>()

        collectReelsScreenData(root, visibleTexts, contentDescs, rightRailNodes, screenWidth, screenHeight, maxDepth = 25, maxNodes = 600)

        val allText = visibleTexts.joinToString(" ").lowercase(java.util.Locale.ROOT)
        val allDescs = contentDescs.joinToString(" ").lowercase(java.util.Locale.ROOT)

        // 1. Explicit Reels text/contentDesc
        if (allDescs.contains("reels") || allDescs.contains("ریلز") ||
            visibleTexts.any { it.trim().equals("reels", ignoreCase = true) || it.trim().equals("ریلز", ignoreCase = true) }) {
            return true
        }

        // 2. Specific Reels badges & indicators
        if (allText.contains("made with edits") || allText.contains("remix with") ||
            allText.contains("original audio") || allText.contains("صدای اصلی") ||
            allText.contains("use audio") || allText.contains("استفاده از صدا") ||
            allText.contains("ساخته شده با") || allText.contains("ریمیکس با") ||
            allDescs.contains("original audio") || allDescs.contains("صدای اصلی")) {
            return true
        }

        // 3. Side action rail pattern (numbers like "5,768", "18.2K", "212", "۵٬۷۶۸", etc.)
        val metricNumberRegex = Regex("""^[\d\u06F0-\u06F9\u0660-\u0669]+(\.[\d\u06F0-\u06F9\u0660-\u0669]+)?[KMBkmb\u0647\u0632\u0627\u0631\u0645\u06CC\u0644\u06CC\u0648\u0646]?$""")
        var metricCount = 0
        for (node in rightRailNodes) {
            val rawText = (node.text?.toString() ?: node.contentDescription?.toString() ?: "").trim()
            val clean = rawText.replace(",", "").replace("٬", "").replace("،", "").replace(" ", "").trim()
            if (clean.matches(metricNumberRegex) || clean.endsWith("K", ignoreCase = true) || clean.endsWith("M", ignoreCase = true) ||
                clean.endsWith("هزار") || clean.endsWith("میلیون")) {
                metricCount++
            }
        }

        val hasLike = allDescs.contains("like") || allDescs.contains("پسندیدن") || allDescs.contains("لایک") || allDescs.contains("unlike") || allDescs.contains("پسند")
        val hasComment = allDescs.contains("comment") || allDescs.contains("دیدگاه") || allDescs.contains("نظر") || allDescs.contains("کامنت")
        val hasShare = allDescs.contains("share") || allDescs.contains("هم‌رسانی") || allDescs.contains("ارسال") || allDescs.contains("send") || allDescs.contains("اشتراک‌گذاری")
        val hasSave = allDescs.contains("save") || allDescs.contains("ذخیره") || allDescs.contains("نشان کردن") || allDescs.contains("bookmark")
        val hasRepost = allDescs.contains("repost") || allDescs.contains("بازنشر") || allDescs.contains("تکرار")
        val hasFollowButton = allText.contains("follow") || allText.contains("دنبال کردن")

        val actionButtonCount = (if (hasLike) 1 else 0) + (if (hasComment) 1 else 0) + (if (hasShare) 1 else 0) + (if (hasSave) 1 else 0) + (if (hasRepost) 1 else 0)

        if (actionButtonCount >= 2) {
            return true
        }

        if (metricCount >= 1 && actionButtonCount >= 1) {
            return true
        }

        if (hasFollowButton && actionButtonCount >= 1) {
            return true
        }

        if (metricCount >= 2) {
            return true
        }

        return false
    }

    private fun collectReelsScreenData(
        node: AccessibilityNodeInfo,
        texts: MutableList<String>,
        descs: MutableList<String>,
        rightRailNodes: MutableList<AccessibilityNodeInfo>,
        screenWidth: Int,
        screenHeight: Int,
        maxDepth: Int,
        maxNodes: Int
    ) {
        if (maxDepth <= 0 || (texts.size + descs.size) >= maxNodes) return

        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        if (!text.isNullOrEmpty()) texts.add(text)
        if (!desc.isNullOrEmpty()) descs.add(desc)

        val rect = android.graphics.Rect()
        try {
            node.getBoundsInScreen(rect)
            val isRailX = rect.centerX() > screenWidth * 0.60f || rect.centerX() < screenWidth * 0.40f
            val isRailY = rect.centerY() in (screenHeight * 0.15f).toInt()..(screenHeight * 0.92f).toInt()
            if (isRailX && isRailY) {
                rightRailNodes.add(node)
            }
        } catch (_: Exception) { }

        for (i in 0 until node.childCount) {
            if ((texts.size + descs.size) >= maxNodes) break
            val child = node.getChild(i) ?: continue
            collectReelsScreenData(child, texts, descs, rightRailNodes, screenWidth, screenHeight, maxDepth - 1, maxNodes)
        }
    }

    private fun findNodesInScreenArea(
        node: AccessibilityNodeInfo,
        minY: Int,
        maxY: Int,
        result: MutableList<AccessibilityNodeInfo>,
        maxDepth: Int
    ) {
        if (maxDepth <= 0 || result.size >= 150) return
        val rect = android.graphics.Rect()
        try {
            node.getBoundsInScreen(rect)
            if (rect.centerY() in minY..maxY) {
                result.add(node)
            }
        } catch (_: Exception) { }

        for (i in 0 until node.childCount) {
            if (result.size >= 150) break
            val child = node.getChild(i) ?: continue
            findNodesInScreenArea(child, minY, maxY, result, maxDepth - 1)
        }
    }

    private fun findTopHeaderNodes(
        node: AccessibilityNodeInfo,
        maxY: Int,
        result: MutableList<AccessibilityNodeInfo>,
        maxDepth: Int = 20
    ) {
        if (maxDepth <= 0 || result.size >= 150) return
        val rect = android.graphics.Rect()
        try {
            if (node.isVisibleToUser) {
                node.getBoundsInScreen(rect)
                if (rect.bottom in 40..maxY && rect.top >= -20 && rect.height() > 10) {
                    result.add(node)
                }
            }
        } catch (_: Exception) { }

        for (i in 0 until node.childCount) {
            if (result.size >= 150) break
            val child = node.getChild(i) ?: continue
            findTopHeaderNodes(child, maxY, result, maxDepth - 1)
        }
    }

    /**
     * Checks whether the Instagram Reels top header containing ("Reels", "Friends" / "ریلز", "دوستان")
     * is currently visible at the top of the screen (top ~22% of screen).
     */
    private fun isInstagramReelsHeaderPresent(root: AccessibilityNodeInfo, screenWidth: Int, screenHeight: Int): Boolean {
        val headerMaxY = (screenHeight * 0.22f).toInt()
        val headerKeywords = listOf("Reels", "Friends", "ریلز", "دوستان")

        // 1. Fast text-indexed search
        for (kw in headerKeywords) {
            try {
                val matching = root.findAccessibilityNodeInfosByText(kw)
                if (!matching.isNullOrEmpty()) {
                    val rect = android.graphics.Rect()
                    for (node in matching) {
                        try {
                            if (!node.isVisibleToUser) continue
                            node.getBoundsInScreen(rect)
                            if (rect.bottom in 40..headerMaxY && rect.top >= -20 && rect.height() > 10) {
                                val txt = (node.text?.toString() ?: "").trim()
                                val desc = (node.contentDescription?.toString() ?: "").trim()
                                if (txt.contains(kw, ignoreCase = true) || desc.contains(kw, ignoreCase = true)) {
                                    return true
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Direct scan of nodes in the top region
        val topNodes = mutableListOf<AccessibilityNodeInfo>()
        findTopHeaderNodes(root, headerMaxY, topNodes, maxDepth = 20)
        val rect = android.graphics.Rect()
        for (node in topNodes) {
            try {
                if (!node.isVisibleToUser) continue
                node.getBoundsInScreen(rect)
                if (rect.bottom in 40..headerMaxY && rect.top >= -20 && rect.height() > 10) {
                    val txt = (node.text?.toString() ?: "").trim().lowercase(java.util.Locale.ROOT)
                    val desc = (node.contentDescription?.toString() ?: "").trim().lowercase(java.util.Locale.ROOT)
                    val isShort = txt.length <= 15
                    if (isShort && (txt == "reels" || txt.startsWith("reels") ||
                        txt == "friends" || txt.startsWith("friends") ||
                        txt == "ریلز" || txt.startsWith("ریلز") ||
                        txt == "دوستان" || txt.startsWith("دوستان") ||
                        desc == "reels" || desc.startsWith("reels") ||
                        desc == "friends" || desc.startsWith("friends") ||
                        desc == "ریلز" || desc == "دوستان")) {
                        return true
                    }
                }
            } catch (_: Exception) {}
        }

        return false
    }

    private fun extractReelSignature(root: AccessibilityNodeInfo, screenWidth: Int, screenHeight: Int): String {
        val minY = (screenHeight * 0.50f).toInt()
        val maxY = (screenHeight * 0.92f).toInt()
        val texts = mutableListOf<String>()
        collectReelsLowerTexts(root, minY, maxY, texts, maxDepth = 20, maxNodes = 15)
        return texts.joinToString("|")
    }

    private fun collectReelsLowerTexts(
        node: AccessibilityNodeInfo,
        minY: Int,
        maxY: Int,
        result: MutableList<String>,
        maxDepth: Int,
        maxNodes: Int
    ) {
        if (maxDepth <= 0 || result.size >= maxNodes) return
        val rect = android.graphics.Rect()
        try {
            if (node.isVisibleToUser) {
                node.getBoundsInScreen(rect)
                if (rect.centerY() in minY..maxY) {
                    val txt = node.text?.toString()?.trim()
                    val desc = node.contentDescription?.toString()?.trim()
                    if (!txt.isNullOrEmpty() && txt.length in 2..80) {
                        result.add(txt)
                    } else if (!desc.isNullOrEmpty() && desc.length in 2..80) {
                        result.add(desc)
                    }
                }
            }
        } catch (_: Exception) {}

        for (i in 0 until node.childCount) {
            if (result.size >= maxNodes) break
            val child = node.getChild(i) ?: continue
            collectReelsLowerTexts(child, minY, maxY, result, maxDepth - 1, maxNodes)
        }
    }

    /**
     * Detects doomscrolling in Instagram Reels:
     * 1) Upward scroll: header ("Reels", "Friends") disappears
     * 2) Downward scroll: header ("Reels", "Friends") reappears
     * 3) Continued scrolling while header is invisible (user keeps scrolling through reels)
     */
    private fun checkInstagramReelsDoomscrolling(
        root: AccessibilityNodeInfo,
        packageName: String,
        isScrollEvent: Boolean = false
    ) {
        if (!isDoomscrollingAlertEnabled) return
        if (checkIsOverlayShowing()) return
        if (packageName == "com.goldenminute.catalyst") return

        if (packageName != "com.instagram.android") {
            lastInstagramReelsHeaderVisible = null
            lastReelSignature = ""
            return
        }

        val displayMetrics = applicationContext.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels

        val inReels = (currentSection == "Reels") || isInstagramReelsScreen(root, screenWidth, screenHeight)
        if (!inReels) {
            // Only reset if confirmed in another section of Instagram
            if (currentSection != "Unknown" && currentSection != "General" && currentSection.isNotEmpty()) {
                lastInstagramReelsHeaderVisible = null
                lastReelSignature = ""
                finalizeCurrentDoomscrollVideo(applicationContext)
                pendingDoomscrollJob?.cancel()
            }
            return
        }

        val headerVisible = isInstagramReelsHeaderPresent(root, screenWidth, screenHeight)
        val prevHeaderVisible = lastInstagramReelsHeaderVisible
        val currentSignature = extractReelSignature(root, screenWidth, screenHeight)
        val signatureChanged = currentSignature.isNotEmpty() && (lastReelSignature.isEmpty() || currentSignature != lastReelSignature)

        // 1. Initial Entry into Reels: establish baseline
        if (prevHeaderVisible == null) {
            lastInstagramReelsHeaderVisible = headerVisible
            if (currentSignature.isNotEmpty()) {
                lastReelSignature = currentSignature
            }
            Log.d(TAG, "Entered Instagram Reels: baseline established (headerVisible = $headerVisible, sig = $currentSignature)")
            return
        }

        val now = System.currentTimeMillis()
        val isDebounced = (now - lastReelsDoomscrollTriggerTime) > 2500L

        // 2. Trigger Case 1: Scroll UPWARD (Header "Reels", "Friends" disappeared)
        if (prevHeaderVisible && !headerVisible) {
            finalizeCurrentDoomscrollVideo(applicationContext)
            lastInstagramReelsHeaderVisible = false
            if (currentSignature.isNotEmpty()) {
                lastReelSignature = currentSignature
            }
            Log.d(TAG, "Instagram Reels scroll upward detected (header disappeared). Scheduling doomscrolling dialog in 3s.")
            if (isDebounced && !checkIsOverlayShowing()) {
                scheduleDoomscrollingOverlay(packageName)
            }
            return
        }

        // 3. Trigger Case 2: Scroll DOWNWARD (Header "Reels", "Friends" reappeared)
        if (!prevHeaderVisible && headerVisible) {
            finalizeCurrentDoomscrollVideo(applicationContext)
            lastInstagramReelsHeaderVisible = true
            if (currentSignature.isNotEmpty()) {
                lastReelSignature = currentSignature
            }
            Log.d(TAG, "Instagram Reels scroll downward detected (header reappeared). Scheduling doomscrolling dialog in 3s.")
            if (isDebounced && !checkIsOverlayShowing()) {
                scheduleDoomscrollingOverlay(packageName)
            }
            return
        }

        // 4. Trigger Case 3: User KEEPS SCROLLING while header texts are invisible
        if (!headerVisible && (isScrollEvent || signatureChanged)) {
            finalizeCurrentDoomscrollVideo(applicationContext)
            if (currentSignature.isNotEmpty()) {
                lastReelSignature = currentSignature
            }
            Log.d(TAG, "Instagram Reels continued scroll detected while header invisible (isScroll=$isScrollEvent, sigChanged=$signatureChanged). Scheduling doomscrolling dialog in 3s.")
            if (isDebounced && !checkIsOverlayShowing()) {
                scheduleDoomscrollingOverlay(packageName)
            }
            return
        }

        // Keep signature updated even if not debounced yet
        if (currentSignature.isNotEmpty()) {
            lastReelSignature = currentSignature
        }
    }

    private var pendingDoomscrollJob: Job? = null

    private fun scheduleDoomscrollingOverlay(packageName: String) {
        pendingDoomscrollJob?.cancel()
        pendingDoomscrollJob = scope.launch {
            delay(3000L)
            if (!checkIsOverlayShowing() && isDoomscrollingAlertEnabled) {
                val root = rootInActiveWindow
                val screenHeight = applicationContext.resources.displayMetrics.heightPixels
                val screenWidth = applicationContext.resources.displayMetrics.widthPixels
                val rootPkg = root?.packageName?.toString() ?: currentAppPackage
                val stillInReels = (rootPkg == "com.instagram.android" || currentAppPackage == "com.instagram.android") &&
                    ((currentSection == "Reels") || (root?.let { isInstagramReelsScreen(it, screenWidth, screenHeight) } ?: true))
                root?.recycle()
                if (stillInReels && !checkIsOverlayShowing() && isDoomscrollingAlertEnabled) {
                    lastReelsDoomscrollTriggerTime = System.currentTimeMillis()
                    launchMindfulnessOverlay(packageName, "Reels")
                }
            }
        }
    }

    /** Collects only contentDescription values from accessibility nodes up to maxDepth/maxNodes. */
    private fun collectContentDescriptions(node: AccessibilityNodeInfo, result: MutableList<String>, maxDepth: Int = 25, maxNodes: Int = 600) {
        if (maxDepth <= 0 || result.size >= maxNodes) return
        val desc = node.contentDescription?.toString()
        if (!desc.isNullOrEmpty()) result.add(desc)
        for (i in 0 until node.childCount) {
            if (result.size >= maxNodes) break
            val child = node.getChild(i) ?: continue
            collectContentDescriptions(child, result, maxDepth - 1, maxNodes)
        }
    }

    /** Collects text content from visible accessibility nodes up to maxDepth/maxNodes. */
    private fun collectVisibleTexts(node: AccessibilityNodeInfo, result: MutableList<String>, maxDepth: Int = 25, maxNodes: Int = 600) {
        if (maxDepth <= 0 || result.size >= maxNodes) return
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        if (!text.isNullOrEmpty()) result.add(text)
        if (!desc.isNullOrEmpty()) result.add(desc)

        for (i in 0 until node.childCount) {
            if (result.size >= maxNodes) break
            val child = node.getChild(i) ?: continue
            collectVisibleTexts(child, result, maxDepth - 1, maxNodes)
        }
    }

    /**
     * TikTok section detection.
     * TikTok's main feed is essentially all distracting.
     * We detect: ForYou, Following, Discover, Inbox, Profile.
     */
    private fun detectTikTokSection(root: AccessibilityNodeInfo, packageName: String): String {
        val selectedTab = findSelectedTab(root)
        if (selectedTab != null) {
            val desc = selectedTab.contentDescription?.toString()?.lowercase() ?: ""
            val viewId = selectedTab.viewIdResourceName?.lowercase() ?: ""

            val section = when {
                desc.contains("home") || desc.contains("for you") ||
                desc.contains("following") || desc.contains("خانه") ||
                viewId.contains("home") || viewId.contains("bottom_tab_item_0") -> "ForYou"

                desc.contains("discover") || desc.contains("search") ||
                desc.contains("جستجو") || desc.contains("کشف") ||
                viewId.contains("discover") || viewId.contains("search") -> "Discover"

                desc.contains("inbox") || desc.contains("notification") ||
                desc.contains("صندوق") ||
                viewId.contains("inbox") || viewId.contains("message") -> "Inbox"

                desc.contains("profile") || desc.contains("me") ||
                desc.contains("پروفایل") || desc.contains("من") ||
                viewId.contains("profile") || viewId.contains("me") -> "Profile"

                else -> "ForYou"
            }
            lastSelectedBottomNavSection[packageName] = section
            return section
        }

        return lastSelectedBottomNavSection[packageName] ?: "ForYou"
    }

    /**
     * Twitter/X section detection.
     */
    private fun detectTwitterSection(root: AccessibilityNodeInfo, packageName: String): String {
        val selectedTab = findSelectedTab(root)
        if (selectedTab != null) {
            val desc = selectedTab.contentDescription?.toString()?.lowercase() ?: ""
            val viewId = selectedTab.viewIdResourceName?.lowercase() ?: ""

            val section = when {
                desc.contains("home") || desc.contains("خانه") ||
                viewId.contains("home") -> "Home"

                desc.contains("explore") || desc.contains("search") ||
                desc.contains("جستجو") ||
                viewId.contains("search") || viewId.contains("explore") -> "Explore"

                desc.contains("notification") || desc.contains("اعلان") ||
                viewId.contains("notification") -> "Notifications"

                desc.contains("message") || desc.contains("dm") ||
                desc.contains("پیام") ||
                viewId.contains("message") || viewId.contains("dm") -> "Messages"

                desc.contains("communities") || desc.contains("انجمن") ||
                viewId.contains("communities") -> "Communities"

                else -> "Home"
            }
            lastSelectedBottomNavSection[packageName] = section
            return section
        }
        return lastSelectedBottomNavSection[packageName] ?: "Home"
    }

    /**
     * YouTube section detection.
     * Key sections: Home, Shorts, Subscriptions, Library/You.
     */
    private fun detectYouTubeSection(root: AccessibilityNodeInfo, packageName: String): String {
        // Check if we're in YouTube Shorts (full screen vertical video)
        if (findNodeByViewIdContains(root, "shorts") != null ||
            findNodeByViewIdContains(root, "reel_recycler") != null ||
            findNodeByContentDescContains(root, "short") != null) {
            lastSelectedBottomNavSection[packageName] = "Shorts"
            return "Shorts"
        }

        val selectedTab = findSelectedTab(root)
        if (selectedTab != null) {
            val desc = selectedTab.contentDescription?.toString()?.lowercase() ?: ""
            val viewId = selectedTab.viewIdResourceName?.lowercase() ?: ""

            val section = when {
                desc.contains("home") || desc.contains("خانه") ||
                viewId.contains("home") -> "Home"

                desc.contains("short") || desc.contains("شورتز") ||
                viewId.contains("shorts") || viewId.contains("pivot_short") -> "Shorts"

                desc.contains("subscription") || desc.contains("اشتراک") ||
                viewId.contains("subscriptions") -> "Subscriptions"

                desc.contains("library") || desc.contains("you") ||
                desc.contains("کتابخانه") ||
                viewId.contains("library") || viewId.contains("pivot_library") -> "Library"

                else -> "Home"
            }
            lastSelectedBottomNavSection[packageName] = section
            return section
        }
        return lastSelectedBottomNavSection[packageName] ?: "Home"
    }

    /**
     * Reddit section detection.
     */
    private fun detectRedditSection(root: AccessibilityNodeInfo, packageName: String): String {
        val selectedTab = findSelectedTab(root)
        if (selectedTab != null) {
            val desc = selectedTab.contentDescription?.toString()?.lowercase() ?: ""
            val viewId = selectedTab.viewIdResourceName?.lowercase() ?: ""

            val section = when {
                desc.contains("home") || desc.contains("خانه") ||
                viewId.contains("home") -> "Home"

                desc.contains("popular") || desc.contains("discover") ||
                desc.contains("communities") ||
                viewId.contains("popular") || viewId.contains("discover") -> "Popular"

                desc.contains("create") || desc.contains("ایجاد") ||
                viewId.contains("create") || viewId.contains("post") -> "Create"

                desc.contains("chat") || desc.contains("inbox") ||
                desc.contains("پیام") ||
                viewId.contains("chat") || viewId.contains("inbox") -> "Chat"

                desc.contains("profile") || desc.contains("پروفایل") ||
                viewId.contains("profile") -> "Profile"

                else -> "Home"
            }
            lastSelectedBottomNavSection[packageName] = section
            return section
        }
        return lastSelectedBottomNavSection[packageName] ?: "Home"
    }

    /**
     * Facebook section detection.
     */
    private fun detectFacebookSection(root: AccessibilityNodeInfo, packageName: String): String {
        val selectedTab = findSelectedTab(root)
        if (selectedTab != null) {
            val desc = selectedTab.contentDescription?.toString()?.lowercase() ?: ""
            val viewId = selectedTab.viewIdResourceName?.lowercase() ?: ""

            val section = when {
                desc.contains("home") || desc.contains("feed") ||
                desc.contains("خانه") ||
                viewId.contains("feed") || viewId.contains("home") -> "Home"

                desc.contains("watch") || desc.contains("video") ||
                desc.contains("ویدیو") ||
                viewId.contains("watch") || viewId.contains("video") -> "Watch"

                desc.contains("reel") || desc.contains("ریلز") ||
                viewId.contains("reel") -> "Reels"

                desc.contains("marketplace") || desc.contains("بازار") ||
                viewId.contains("marketplace") -> "Marketplace"

                desc.contains("notification") || desc.contains("اعلان") ||
                viewId.contains("notification") -> "Notifications"

                desc.contains("menu") || desc.contains("منو") ||
                viewId.contains("menu") || viewId.contains("bookmarks") -> "Menu"

                else -> "Home"
            }
            lastSelectedBottomNavSection[packageName] = section
            return section
        }
        return lastSelectedBottomNavSection[packageName] ?: "Home"
    }

    /**
     * Snapchat section detection.
     */
    private fun detectSnapchatSection(root: AccessibilityNodeInfo, packageName: String): String {
        val selectedTab = findSelectedTab(root)
        if (selectedTab != null) {
            val desc = selectedTab.contentDescription?.toString()?.lowercase() ?: ""
            val viewId = selectedTab.viewIdResourceName?.lowercase() ?: ""

            val section = when {
                desc.contains("camera") || desc.contains("دوربین") ||
                viewId.contains("camera") -> "Camera"

                desc.contains("chat") || desc.contains("پیام") ||
                viewId.contains("chat") || viewId.contains("feed") -> "Chat"

                desc.contains("discover") || desc.contains("stories") ||
                desc.contains("کشف") ||
                viewId.contains("discover") || viewId.contains("stories") -> "Discover"

                desc.contains("spotlight") || desc.contains("اسپات") ||
                viewId.contains("spotlight") -> "Spotlight"

                desc.contains("map") || desc.contains("نقشه") ||
                viewId.contains("map") -> "Map"

                else -> "Camera"
            }
            lastSelectedBottomNavSection[packageName] = section
            return section
        }
        return lastSelectedBottomNavSection[packageName] ?: "Camera"
    }

    /**
     * Generic fallback: just tracks "General" for unknown apps.
     */
    private fun detectGenericSection(root: AccessibilityNodeInfo): String {
        return "General"
    }

    // ═══════════════════════════════════════════════════════════════
    // Node helpers — find selected tabs & view IDs
    // ═══════════════════════════════════════════════════════════════

    /**
     * ★ Bug fix #4: Improved tab detection.
     * Finds the selected tab specifically in the bottom navigation bar.
     * Uses a two-pass strategy:
     *  1) Look for a known bottom-bar container and search for selected tabs within it.
     *  2) Fall back to collecting all selected+clickable nodes and picking the one
     *     closest to the bottom of the screen (using screen coordinates).
     */
    private fun findBottomNavSelectedTab(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Strategy 1: Look for a bottom-bar/tab-bar container first
        val bottomBar = findNodeByViewIdContains(root, "tab_bar")
            ?: findNodeByViewIdContains(root, "bottom_bar")
            ?: findNodeByViewIdContains(root, "bottom_navigation")
            ?: findNodeByViewIdContains(root, "tab_layout")
            ?: findNodeByViewIdContains(root, "fixed_bottomsheet_container") // Instagram's container

        if (bottomBar != null) {
            val selectedInBar = findSelectedInSubtree(bottomBar)
            if (selectedInBar != null) return selectedInBar
        }

        // Strategy 2: Collect all selected clickable nodes and pick the one
        // nearest the bottom of the screen (most likely a bottom nav tab)
        val selectedNodes = mutableListOf<AccessibilityNodeInfo>()
        collectSelectedClickableNodes(root, selectedNodes)

        if (selectedNodes.isNotEmpty()) {
            // Use screen position: pick the node with the highest Y coordinate
            var bestNode: AccessibilityNodeInfo? = null
            var bestY = -1
            val rect = android.graphics.Rect()
            for (node in selectedNodes) {
                try {
                    node.getBoundsInScreen(rect)
                    if (rect.bottom > bestY) {
                        bestY = rect.bottom
                        bestNode = node
                    }
                } catch (_: Exception) { }
            }
            if (bestNode != null) return bestNode
            return selectedNodes.lastOrNull()
        }

        // Strategy 3: Fall back to state-based detection
        return findNodeBySelectedState(root)
    }

    /** Legacy method kept for backward compat in other detector methods. */
    private fun findSelectedTab(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findBottomNavSelectedTab(root)
    }

    private fun findSelectedInSubtree(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Many apps (like Instagram) mark the FrameLayout as selected, but the clickable
        // behavior and content descriptions are on child nodes.
        if (node.isSelected) {
            return node
        }
        
        // Sometimes the tab indicates it is selected via content description
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (desc.contains("selected") || desc.contains("انتخاب")) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findSelectedInSubtree(child)
            if (found != null) return found
        }
        return null
    }

    private fun collectSelectedClickableNodes(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        if (node.isSelected) {
            val desc = node.contentDescription?.toString() ?: ""
            val viewId = node.viewIdResourceName ?: ""
            // Filter out nodes that are clearly not navigation tabs
            if (desc.isNotEmpty() || viewId.isNotEmpty() || node.isClickable) {
                result.add(node)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectSelectedClickableNodes(child, result)
        }
    }

    private fun findNodeBySelectedState(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isSelected) {
            val desc = node.contentDescription?.toString() ?: ""
            if (desc.isNotEmpty()) return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeBySelectedState(child)
            if (found != null) return found
        }
        return null
    }

    private fun findNodeByViewIdContains(node: AccessibilityNodeInfo, idFragment: String): AccessibilityNodeInfo? {
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        if (viewId.contains(idFragment.lowercase())) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByViewIdContains(child, idFragment)
            if (found != null) return found
        }
        return null
    }

    private fun findNodeByContentDescContains(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (desc.contains(text.lowercase())) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByContentDescContains(child, text)
            if (found != null) return found
        }
        return null
    }

    // ═══════════════════════════════════════════════════════════════
    // Distracting time tracking
    // ═══════════════════════════════════════════════════════════════

    /**
     * Called every content change: tracks whether the user is in a
     * distracting section for a distracting app.
     */
    private fun updateDistractingTracking(packageName: String, section: String) {
        if (!activeDistractingPackages.contains(packageName)) return

        val distractingSections = DISTRACTING_SECTIONS[packageName]
        val isDistracting = distractingSections?.contains(section) ?: true // If not in map, treat whole app as distracting

        val prevSection = appLastSection[packageName]
        appLastSection[packageName] = section

        if (isDistracting) {
            // If we just entered a distracting section, start timing
            if (!appDistractingSectionStart.containsKey(packageName)) {
                appDistractingSectionStart[packageName] = System.currentTimeMillis()
            }
        } else {
            // We're in a non-distracting section — flush any accumulated distracting time
            flushDistractingSection(packageName)
        }
    }

    /**
     * Called when leaving an app or moving to a non-distracting section.
     * Commits accumulated distracting time.
     */
    private fun flushDistractingSection(packageName: String) {
        val start = appDistractingSectionStart[packageName] ?: return
        val elapsed = System.currentTimeMillis() - start
        if (elapsed > 0) {
            appDistractingTime[packageName] = (appDistractingTime[packageName] ?: 0L) + elapsed
        }
        appDistractingSectionStart.remove(packageName)
    }

    /**
     * Gets the total distracting time for an app, including any currently
     * running distracting section.
     */
    private fun getTotalDistractingTime(packageName: String): Long {
        var total = appDistractingTime[packageName] ?: 0L
        // Add currently running distracting section if any
        val start = appDistractingSectionStart[packageName]
        if (start != null) {
            total += System.currentTimeMillis() - start
        }
        return total
    }

    // ═══════════════════════════════════════════════════════════════
    // Mindfulness Check — works for both task-linked & distracting apps
    // ═══════════════════════════════════════════════════════════════

    private suspend fun performMindfulnessCheck(packageName: String, section: String) {
        // Do not show doomscrolling dialog if user has disabled the option
        if (!isDoomscrollingAlertEnabled) return

        // Don't fire while the overlay is already showing
        if (checkIsOverlayShowing()) return

        // Check cooldown for this specific app
        val cooldownUntil = appMindfulnessCooldownUntil[packageName] ?: 0L
        if (System.currentTimeMillis() < cooldownUntil) return

        // ── Path 1: Task-linked apps (existing behavior) ──
        val activeTaskId = TimerStateManager.activeTaskId.value
        if (activeTaskId != -1 && TimerStateManager.isTimerRunning.value) {
            val db = TodoDatabase.getDatabase(applicationContext, scope)
            val activeTodo = db.todoDao().getAllTodos().first().find { it.id == activeTaskId }

            if (activeTodo != null && activeTodo.associatedPackageNames?.split(",")?.contains(packageName) == true) {
                // Ensure timer mode is set based on the task
                TimerStateManager.isPomodoro.value = (activeTodo.timerMode == "Pomodoro")
                val elapsedSinceSectionStart = System.currentTimeMillis() - currentSectionStartTime
                if (elapsedSinceSectionStart > 5 * 60 * 1000L) {
                    if (System.currentTimeMillis() < TimerStateManager.mindfulnessCooldownUntil) {
                        return
                    }
                    Log.d(TAG, "Task-linked app: 5+ min in section '$section'. Showing overlay.")
                    TimerStateManager.autoPauseTask(idle = true)
                    commitCurrentSectionTime()
                    launchMindfulnessOverlay(packageName, section)
                    return
                }
            }
        }

        // ── Path 2: Distracting app (no task needed) ──
        if (activeDistractingPackages.contains(packageName)) {
            val distractingSections = DISTRACTING_SECTIONS[packageName]
            val isDistracting = distractingSections?.contains(section) ?: true

            if (isDistracting) {
                val totalDistractingTime = getTotalDistractingTime(packageName)
                if (totalDistractingTime > 5 * 60 * 1000L) {
                    Log.d(TAG, "Distracting app $packageName: ${totalDistractingTime/1000}s in distracting sections. Showing overlay.")
                    launchMindfulnessOverlay(packageName, section)
                    return
                }
            }
        }
    }

    private fun launchMindfulnessOverlay(packageName: String, section: String) {
        // Do not show doomscrolling dialog if user has disabled the option
        if (!isDoomscrollingAlertEnabled) return

        // Prevent double-launch
        if (checkIsOverlayShowing()) return
        isOverlayShowing = true
        overlayShownTime = System.currentTimeMillis()

        // Set a minimum cooldown (30s) to prevent rapid re-triggering
        // even before the user presses a button
        appMindfulnessCooldownUntil[packageName] = System.currentTimeMillis() + 30_000L

        // Resolve a human-readable app name
        val appName = getAppDisplayName(packageName)

        // Overlay activity with separate taskAffinity ("")
        val intent = Intent(applicationContext, com.goldenminute.catalyst.ui.MindfulnessOverlayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_SINGLE_TOP
                or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                or Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra("EXTRA_APP_NAME", appName)
            putExtra("EXTRA_SECTION_NAME", section)
            putExtra("EXTRA_PACKAGE_NAME", packageName)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch MindfulnessOverlayActivity", e)
            isOverlayShowing = false
            overlayShownTime = 0L
        }
    }

    private fun getAppDisplayName(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            when (packageName) {
                "com.instagram.android" -> "Instagram"
                "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> "TikTok"
                "com.twitter.android" -> "X (Twitter)"
                "com.google.android.youtube" -> "YouTube"
                "com.reddit.frontpage" -> "Reddit"
                "com.snapchat.android" -> "Snapchat"
                "com.facebook.katana" -> "Facebook"
                "com.facebook.orca" -> "Messenger"
                "com.pinterest" -> "Pinterest"
                "com.linkedin.android" -> "LinkedIn"
                else -> packageName.substringAfterLast(".")
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isRunning = false
        finalizeCurrentDoomscrollVideo(applicationContext)
        pendingDoomscrollJob?.cancel()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        finalizeCurrentDoomscrollVideo(applicationContext)
        pendingDoomscrollJob?.cancel()
        livenessJob?.cancel()
    }
}
