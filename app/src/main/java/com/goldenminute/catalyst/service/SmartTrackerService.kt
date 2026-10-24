package com.goldenminute.catalyst.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.KeyEvent
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
        var isDoomscrollingAlertEnabled: Boolean = true

        // Detectable sections per known app — used by the UI to show section pickers
        val KNOWN_APP_SECTIONS: Map<String, List<String>> = mapOf(
            "com.instagram.android" to listOf("Home", "Explore", "Reels", "Messages", "Profile", "Create", "Activity")
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
                // If the overlay activity is definitely not active/showing anymore, auto-reset
                if (!com.goldenminute.catalyst.ui.MindfulnessOverlayActivity.isShowing()) {
                    if (System.currentTimeMillis() - overlayShownTime > 1500L) {
                        isOverlayShowing = false
                        overlayShownTime = 0L
                        return false
                    }
                }
                // Failsafe: if overlay has been marked showing for >15s without dismissal, auto-reset
                if (System.currentTimeMillis() - overlayShownTime > 15_000L) {
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
        @Volatile var activeReelSignature: String = ""
        @Volatile var lastDialogReelSignature: String = ""
        @Volatile var lastDialogReelCounter: Long = 0L
        @Volatile var lastReelScrollTriggerTime: Long = 0L
        @Volatile var lastReelScrollEventTime: Long = 0L
        @Volatile var pendingDoomscrollTargetReel: String = ""
        @Volatile var reelScrollCounter: Long = 0L
        @Volatile var currentAppLanguage: String = "en"

        @Volatile var overlayDismissedTime = 0L
        @Volatile var ignoredReelSignature: String = ""
        @Volatile var ignoredReelCounter: Long = 0L
        @Volatile var isInspirationActiveForCurrentReel: Boolean = false
        @Volatile var ignoredReelTimestamp: Long = 0L
        @Volatile var lastInspirationMarkedTime: Long = 0L
        @Volatile var lastInspirationReelCounter: Long = 0L

        val shownDialogReelSignatures: MutableSet<String> = java.util.Collections.synchronizedSet(LinkedHashSet())
        val inspiredReelSignatures: MutableSet<String> = java.util.Collections.synchronizedSet(LinkedHashSet())

        fun recordShownReel(signature: String) {
            if (signature.isEmpty()) return
            shownDialogReelSignatures.add(signature)
            if (shownDialogReelSignatures.size > 100) {
                val iter = shownDialogReelSignatures.iterator()
                if (iter.hasNext()) {
                    iter.next()
                    iter.remove()
                }
            }
        }

        fun recordInspiredReel(signature: String) {
            if (signature.isEmpty()) return
            inspiredReelSignatures.add(signature)
            if (inspiredReelSignatures.size > 100) {
                val iter = inspiredReelSignatures.iterator()
                if (iter.hasNext()) {
                    iter.next()
                    iter.remove()
                }
            }
            recordShownReel(signature)
        }

        private val GENERIC_INSTAGRAM_WORDS = setOf(
            "original audio", "صدای اصلی", "use audio", "استفاده از صدا",
            "original sound", "صدای پیش‌فرض", "audio", "video", "صدا", "ویدیو",
            "reels", "reel", "ریلز", "follow", "following", "دنبال کردن", "دنبال می‌کنید",
            "like", "likes", "liked", "پسندیدن", "پسندیدید", "لایک",
            "comment", "comments", "دیدگاه", "دیدگاه‌ها", "نظر", "نظرات", "کامنت", "add comment",
            "share", "اشتراک‌گذاری", "هم‌رسانی", "ارسال", "send",
            "save", "saved", "ذخیره", "ذخیره‌شده", "remix", "ریمیکس",
            "sponsored", "تبلیغ", "حمایت‌شده", "suggested for you", "پیشنهاد برای شما",
            "see translation", "مشاهده ترجمه", "دیدن ترجمه", "more", "بیشتر", "less", "کمتر",
            "view all", "مشاهده همه", "add a comment", "افزودن دیدگاه", "threads", "تردز",
            "template", "الگو", "قالب", "watch more", "مشاهده بیشتر", "verified", "تأییدشده",
            "double", "tap", "play", "pause", "playing", "paused", "دوبار", "ضربه", "بزنید", "پخش", "توقف", "مکث",
            "repost", "reposted", "reshare", "reshared", "بازنشر",
            "seekbar", "seek", "slider", "progress", "نوار پیشرفت",
            "story", "stories", "highlight", "highlights", "استوری", "هایلایت",
            "grid", "view", "column", "row", "photo", "photos", "شبکه", "نما", "نمای",
            "seen", "unseen", "unread", "دیده شده", "خوانده",
            "number", "times", "تعداد", "بار", "post", "posts", "پست",
            "home", "search", "explore", "message", "messages", "profile", "activity", "direct",
            "خانه", "جستجو", "کاوش", "پیام", "پیام‌ها", "نمایه", "پروفایل", "فعالیت"
        )

        fun isSameReel(sig1: String, sig2: String): Boolean {
            if (sig1.isEmpty() || sig2.isEmpty()) return false
            if (sig1.equals(sig2, ignoreCase = true)) return true

            // Synthetic IDs like "reel_1" must only match via exact equality
            val isSig1Synthetic = sig1.startsWith("reel_")
            val isSig2Synthetic = sig2.startsWith("reel_")
            if (isSig1Synthetic || isSig2Synthetic) {
                return false
            }

            fun extractAuthor(sig: String): String {
                if (sig.startsWith("author:")) {
                    return sig.substringAfter("author:").substringBefore("|").trim().lowercase(java.util.Locale.ROOT)
                }
                return ""
            }

            val author1 = extractAuthor(sig1)
            val author2 = extractAuthor(sig2)
            if (author1.isNotEmpty() && author2.isNotEmpty() && author1 != author2) {
                // Different creators means definitely different reels
                return false
            }

            fun extractTokens(sig: String): Set<String> {
                val cleanSig = if (sig.startsWith("author:")) sig.substringAfter("|") else sig
                return cleanSig.split(Regex("[\\s|\\n\\t\\.,;:!?…()@#]+"))
                    .map { it.trim().lowercase(java.util.Locale.ROOT).removeSuffix("'s") }
                    .filter { token ->
                        token.length >= 3
                            && !token.matches(Regex("^[0-9.,:\\s]+[kmbKMB]?$"))
                            && !GENERIC_INSTAGRAM_WORDS.contains(token)
                    }
                    .toSet()
            }

            val tokens1 = extractTokens(sig1)
            val tokens2 = extractTokens(sig2)

            if (tokens1.isEmpty() || tokens2.isEmpty()) {
                return false
            }

            val common = tokens1.intersect(tokens2)
            if (common.isEmpty()) return false

            // If same author was confirmed:
            if (author1.isNotEmpty() && author1 == author2) {
                val minSize = minOf(tokens1.size, tokens2.size)
                if (minSize <= 2) {
                    return common.size == minSize
                }
                return common.size >= 2
            }

            // Without confirmed same author:
            // MUST share at least 3 distinct non-generic tokens and have >= 70% overlap
            val smallerSize = minOf(tokens1.size, tokens2.size)
            if (smallerSize < 3) {
                // Never match disparate reels based on only 1 or 2 isolated words
                return false
            }

            val similarity = common.size.toDouble() / smallerSize
            return common.size >= 3 && similarity >= 0.70
        }

        fun onReelMarkedInspiration() {
            val currentReel = activeReelSignature.ifEmpty { lastDialogReelSignature }
            ignoredReelSignature = currentReel
            ignoredReelCounter = reelScrollCounter
            isInspirationActiveForCurrentReel = true
            ignoredReelTimestamp = System.currentTimeMillis()
            lastInspirationMarkedTime = System.currentTimeMillis()
            lastInspirationReelCounter = reelScrollCounter
            lastDialogReelCounter = reelScrollCounter
            isCurrentReelMarkedDoomscroll = false
            overlayDismissedTime = System.currentTimeMillis()

            if (currentReel.isNotEmpty() && !currentReel.startsWith("reel_")) {
                recordInspiredReel(currentReel)
            }
            if (lastDialogReelSignature.isNotEmpty() && !lastDialogReelSignature.startsWith("reel_")) {
                recordInspiredReel(lastDialogReelSignature)
            }
            if (activeReelSignature.isNotEmpty() && !activeReelSignature.startsWith("reel_")) {
                recordInspiredReel(activeReelSignature)
            }

            Log.d(TAG, "Reel marked as inspiration: '$ignoredReelSignature' (counter: $ignoredReelCounter). Suppressing alerts for this video.")
        }

        fun onMindfulnessDismissed() {
            isOverlayShowing = false
            overlayShownTime = 0L
            overlayDismissedTime = System.currentTimeMillis()
            pendingDoomscrollTargetReel = ""
            lastReelsDoomscrollTriggerTime = 0L
            if (lastDialogReelSignature.isNotEmpty() && !lastDialogReelSignature.startsWith("reel_")) {
                recordShownReel(lastDialogReelSignature)
            }
            if (activeReelSignature.isNotEmpty() && !activeReelSignature.startsWith("reel_")) {
                recordShownReel(activeReelSignature)
            }
        }

        // State for tracking time on a Reel marked as doomscrolling
        @Volatile var isCurrentReelMarkedDoomscroll: Boolean = false
        @Volatile var currentReelDoomscrollStartTime: Long = 0L
        @Volatile var currentSessionDoomscrollWastedMs: Long = 0L
        @Volatile var hasUserAcknowledgedDoomscrolling: Boolean = false

        fun resetDoomscrollSession() {
            currentSessionDoomscrollWastedMs = 0L
            hasUserAcknowledgedDoomscrolling = false
            Log.d(TAG, "Doomscroll session reset. currentSessionDoomscrollWastedMs = 0")
        }

        fun onDoomscrollingMarked(context: Context, packageName: String) {
            hasUserAcknowledgedDoomscrolling = true
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
                    currentSessionDoomscrollWastedMs += duration
                    Log.d(TAG, "Finalized doomscroll video: duration=${duration}ms, sessionWasted=${currentSessionDoomscrollWastedMs}ms")
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
                isDoomscrollingAlertEnabled = prefs[DataStoreKeys.DOOMSCROLLING_ALERT_ENABLED] ?: true
                currentAppLanguage = prefs[DataStoreKeys.LANGUAGE] ?: if (java.util.Locale.getDefault().language == "fa") "fa" else "en"

                val powerShortcut = prefs[DataStoreKeys.POWER_BUTTON_SHORTCUT_MODE] ?: "none"
                PowerButtonShortcutManager.applyMode(applicationContext, powerShortcut)
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
        if (packageName == "com.goldenminute.catalyst") {
            if (checkIsOverlayShowing() || System.currentTimeMillis() - overlayDismissedTime < 5000L) {
                return true
            }
        }
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
                // Content description / viewId matching first (exact & robust)
                if (desc.contains("create") || desc.contains("camera") || desc.contains("ایجاد") || desc.contains("پست جدید") || desc.contains("دوربین")) return "Create"
                if (desc.contains("reel") || desc.contains("clips") || desc.contains("ریلز")) return "Reels"
                if (desc.contains("search") || desc.contains("explore") || desc.contains("جستجو") || desc.contains("کاوش") || desc.contains("اکسپلور")) return "Explore"
                if (desc.contains("direct") || desc.contains("message") || desc.contains("chat") || desc.contains("پیام") || desc.contains("دایرکت")) return "Messages"
                if (desc.contains("profile") || desc.contains("پروفایل") || desc.contains("نمایه")) return "Profile"
                if (desc.contains("home") || desc.contains("feed") || desc.contains("خانه")) return "Home"

                // Fallback to coordinates: 0: Home, 1: Explore, 2: Create, 3: Reels, 4: Profile
                if (isBottomArea) {
                    return when {
                        cx < screenWidth * 0.20f -> "Home"
                        cx < screenWidth * 0.40f -> "Explore"
                        cx < screenWidth * 0.60f -> "Create"
                        cx < screenWidth * 0.80f -> "Reels"
                        else -> "Profile"
                    }
                }
            }
        }

        return null
    }

    private fun isPowerMenuWindow(event: AccessibilityEvent): Boolean {
        val textList = event.text.map { it.toString().lowercase(java.util.Locale.ROOT) }
        val hasPowerOff = textList.any { it.contains("power off") || it.contains("خاموش کردن") || it.contains("خاموش شود") }
        val hasRestart = textList.any { it.contains("restart") || it.contains("راه‌اندازی مجدد") || it.contains("راه اندازی مجدد") }
        if (hasPowerOff && hasRestart) {
            return true
        }
        val root = try { rootInActiveWindow } catch (e: Exception) { null }
        if (root != null) {
            val rootText = extractAllText(root).lowercase(java.util.Locale.ROOT)
            root.recycle()
            val rootHasOff = rootText.contains("power off") || rootText.contains("خاموش کردن") || rootText.contains("خاموش شود")
            val rootHasRestart = rootText.contains("restart") || rootText.contains("راه‌اندازی مجدد") || rootText.contains("راه اندازی مجدد")
            if (rootHasOff && rootHasRestart) {
                return true
            }
        }
        return false
    }

    private fun extractAllText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        if (text.isNotEmpty()) sb.append(text).append(" ")
        if (desc.isNotEmpty()) sb.append(desc).append(" ")
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            sb.append(extractAllText(child))
            child.recycle()
        }
        return sb.toString()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        // Power menu / Assistant interception for long-press power button shortcut
        if (PowerButtonShortcutManager.currentMode == "long_press") {
            // ONLY check TYPE_WINDOW_STATE_CHANGED for explicit dialog appearances
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val className = event.className?.toString() ?: ""

                // 1. Samsung Global Actions power menu
                if (packageName == "com.samsung.android.globalactions") {
                    PowerButtonShortcutManager.handleGlobalActionsEvent(this)
                    return
                }

                // 2. AOSP / Pixel / other OEM GlobalActions power dialog
                if (className.contains("GlobalActionsDialog", ignoreCase = true) ||
                    className.contains("SecGlobalActions", ignoreCase = true)
                ) {
                    PowerButtonShortcutManager.handleGlobalActionsEvent(this)
                    return
                }

                // 3. Strict content check only if className or package indicates a power / shutdown dialog
                if (className.contains("PowerDialog", ignoreCase = true) ||
                    className.contains("ShutdownActivity", ignoreCase = true) ||
                    className.contains("PowerMenu", ignoreCase = true) ||
                    packageName.contains("powerkeeper")
                ) {
                    if (isPowerMenuWindow(event)) {
                        PowerButtonShortcutManager.handleGlobalActionsEvent(this)
                        return
                    }
                }
            }
        }

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
                // If user clicked away from Reels in Instagram, immediately cancel doomscrolling and dismiss overlay
                if (packageName == "com.instagram.android" && clickedSection != "Reels") {
                    pendingDoomscrollJob?.cancel()
                    pendingDoomscrollJob = null
                    lastInstagramReelsHeaderVisible = null
                    lastReelSignature = ""
                    lastDialogReelSignature = ""
                    lastDialogReelCounter = 0L
                    activeReelSignature = ""
                    // Note: shownDialogReelSignatures and inspiredReelSignatures are preserved
                    finalizeCurrentDoomscrollVideo(applicationContext)
                    resetDoomscrollSession()
                    com.goldenminute.catalyst.ui.MindfulnessOverlayActivity.dismissIfShowing()
                } else if (packageName == "com.instagram.android" && clickedSection == "Reels") {
                    val now = System.currentTimeMillis()
                    if (isDoomscrollingAlertEnabled && !checkIsOverlayShowing() && now - overlayDismissedTime > 1500L) {
                        reelScrollCounter++
                        val reelId = "reel_$reelScrollCounter"
                        lastReelSignature = reelId
                        Log.i(TAG, "Switched to Reels tab via click: scheduling alert for initial reel ($reelId)")
                        scheduleDoomscrollingOverlay(packageName, reelId)
                    }
                }
                updateDistractingTracking(packageName, clickedSection)
                scope.launch {
                    performMindfulnessCheck(packageName, clickedSection)
                }
            }
            return
        }

        // Ignore events from our own transparent overlay activity
        if (packageName == "com.goldenminute.catalyst") {
            return
        }

        // App switch detection: if package has changed to another non-transient app
        if (packageName != lastPackageName) {
            // Commit any time from the previous app/section before switching
            commitCurrentSectionTime()
            flushDistractingSection(lastPackageName)

            // Reset accumulated distracting time when LEAVING an app.
            if (lastPackageName.isNotEmpty() && lastPackageName != "com.goldenminute.catalyst") {
                appDistractingTime[lastPackageName] = 0L
                appDistractingSectionStart.remove(lastPackageName)
                if (lastPackageName == "com.instagram.android" && packageName != "com.goldenminute.catalyst") {
                    lastInstagramReelsHeaderVisible = null
                    lastReelSignature = ""
                    lastDialogReelSignature = ""
                    lastDialogReelCounter = 0L
                    activeReelSignature = ""
                    ignoredReelSignature = ""
                    ignoredReelCounter = 0L
                    isInspirationActiveForCurrentReel = false
                    finalizeCurrentDoomscrollVideo(applicationContext)
                    resetDoomscrollSession()
                    pendingDoomscrollJob?.cancel()
                    pendingDoomscrollJob = null
                    com.goldenminute.catalyst.ui.MindfulnessOverlayActivity.dismissIfShowing()
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
                        // On app switch TO Instagram, check if we landed directly in Reels
                        val now = System.currentTimeMillis()
                        if (packageName == "com.instagram.android" && detectedSection == "Reels"
                            && isDoomscrollingAlertEnabled && !checkIsOverlayShowing()
                            && lastReelSignature.isEmpty()
                            && now - overlayDismissedTime > 1500L) {
                            val screenHeight = applicationContext.resources.displayMetrics.heightPixels
                            val screenWidth = applicationContext.resources.displayMetrics.widthPixels
                            val sig = extractReelSignature(rootNode, screenWidth, screenHeight)
                            if (sig.isNotEmpty()) {
                                if ((isInspirationActiveForCurrentReel && isSameReel(sig, ignoredReelSignature))
                                    || (lastDialogReelSignature.isNotEmpty() && isSameReel(sig, lastDialogReelSignature))
                                    || inspiredReelSignatures.any { isSameReel(it, sig) }
                                    || shownDialogReelSignatures.any { isSameReel(it, sig) }) {
                                    Log.d(TAG, "Entered Reels (app switch): reel matches inspired/shown reel, skipping alert.")
                                    return@launch
                                }
                                activeReelSignature = sig
                            } else if (isInspirationActiveForCurrentReel) {
                                return@launch
                            }
                            reelScrollCounter++
                            val reelId = "reel_$reelScrollCounter"
                            lastReelSignature = reelId
                            Log.i(TAG, "Entered Instagram Reels (app switch): scheduling alert for initial reel ($reelId)")
                            scheduleDoomscrollingOverlay(packageName, reelId)
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
                        // Detect transition INTO Reels section
                        val nowTransition = System.currentTimeMillis()
                        if (packageName == "com.instagram.android" && detectedSection == "Reels"
                            && (previousSection != "Reels" || lastReelSignature.isEmpty())
                            && isDoomscrollingAlertEnabled && !checkIsOverlayShowing()
                            && nowTransition - overlayDismissedTime > 1500L) {
                            val screenHeight = applicationContext.resources.displayMetrics.heightPixels
                            val screenWidth = applicationContext.resources.displayMetrics.widthPixels
                            val sig = extractReelSignature(rootNode, screenWidth, screenHeight)
                            if (sig.isNotEmpty()) {
                                if ((isInspirationActiveForCurrentReel && isSameReel(sig, ignoredReelSignature))
                                    || (lastDialogReelSignature.isNotEmpty() && isSameReel(sig, lastDialogReelSignature))
                                    || inspiredReelSignatures.any { isSameReel(it, sig) }
                                    || shownDialogReelSignatures.any { isSameReel(it, sig) }) {
                                    Log.d(TAG, "Navigated to Reels: reel matches inspired/shown reel, skipping alert.")
                                    return@launch
                                }
                                activeReelSignature = sig
                            } else if (isInspirationActiveForCurrentReel) {
                                return@launch
                            }
                            reelScrollCounter++
                            val reelId = "reel_$reelScrollCounter"
                            lastReelSignature = reelId
                            Log.i(TAG, "Navigated to Reels section: scheduling alert for initial reel ($reelId)")
                            scheduleDoomscrollingOverlay(packageName, reelId)
                        }
                        rootNode.recycle()
                    }
                } catch (_: Exception) { }
            }
        }

        // Dedicated fast handler for Instagram Reels scroll events.
        // This bypasses the isCurrentlyChecking throttle so scroll events are NEVER dropped.
        // Scroll events are the canonical reel-transition signal — each swipe = new reel.
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
            && packageName == "com.instagram.android"
            && isDoomscrollingAlertEnabled
            && !checkIsOverlayShowing()) {

            val eventClass = event.className?.toString() ?: ""
            // Filter out non-swipe scroll sources:
            // 1. Marquee audio title scrolling text / ticker
            // 2. Seekbars / progress bars
            // 3. Horizontal scroll views
            // 4. EditText typing
            if (!eventClass.contains("TextView") &&
                !eventClass.contains("SeekBar") &&
                !eventClass.contains("ProgressBar") &&
                !eventClass.contains("HorizontalScrollView") &&
                !eventClass.contains("EditText")) {

                val now = System.currentTimeMillis()
                // Ignore scroll events during the overlay dismissal settling period (touch release)
                if (now - overlayDismissedTime >= 600L && now - lastReelScrollEventTime >= 600L) {
                    // CRITICAL: Positively verify the active screen is ACTUALLY Instagram Reels!
                    // Never rely solely on lastSelectedBottomNavSection which bleeds into Notifications/DMs/Profile!
                    val rootNode = rootInActiveWindow
                    var isReels = false
                    if (rootNode != null) {
                        val screenHeight = applicationContext.resources.displayMetrics.heightPixels
                        val screenWidth = applicationContext.resources.displayMetrics.widthPixels
                        isReels = isInstagramReelsScreen(rootNode, screenWidth, screenHeight)
                        rootNode.recycle()
                    }
                    if (!isReels && (currentSection == "Reels" || lastSelectedBottomNavSection["com.instagram.android"] == "Reels")) {
                        isReels = true
                    }

                    if (isReels) {
                        lastReelScrollEventTime = now
                        reelScrollCounter++
                        finalizeCurrentDoomscrollVideo(applicationContext)
                        val reelId = "reel_$reelScrollCounter"
                        lastReelSignature = reelId
                        Log.i(TAG, "Reels scroll detected (counter=$reelScrollCounter): checking alert for $reelId")
                        scheduleDoomscrollingOverlay(packageName, reelId)
                    }
                }
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
                                if (packageName == "com.instagram.android" && detectedSection != "Reels" && !checkIsOverlayShowing()) {
                                    val now = System.currentTimeMillis()
                                    if (now - overlayDismissedTime > 1000L) {
                                        pendingDoomscrollJob?.cancel()
                                        pendingDoomscrollJob = null
                                        lastInstagramReelsHeaderVisible = null
                                        lastReelSignature = ""
                                        activeReelSignature = ""
                                        // Preserve shown/inspired reel history across section flickers
                                        finalizeCurrentDoomscrollVideo(applicationContext)
                                        resetDoomscrollSession()
                                        com.goldenminute.catalyst.ui.MindfulnessOverlayActivity.dismissIfShowing()
                                    }
                                }
                            }
                            updateDistractingTracking(packageName, detectedSection)
                            performMindfulnessCheck(packageName, detectedSection)
                            if (packageName == "com.instagram.android" && detectedSection == "Reels" && !checkIsOverlayShowing()) {
                                val screenHeight = applicationContext.resources.displayMetrics.heightPixels
                                val screenWidth = applicationContext.resources.displayMetrics.widthPixels
                                val sig = extractReelSignature(rootNode, screenWidth, screenHeight)
                                if (sig.isNotEmpty()) {
                                    activeReelSignature = sig
                                }
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
    private fun hasVisibleEditText(node: AccessibilityNodeInfo, maxDepth: Int = 15): Boolean {
        if (maxDepth <= 0) return false
        val className = (node.className?.toString() ?: "").lowercase(java.util.Locale.ROOT)
        if ((node.isEditable || className.contains("edittext")) && node.isFocused) {
            return true
        }
        val viewId = (node.viewIdResourceName ?: "").lowercase(java.util.Locale.ROOT)
        if (viewId.contains("comment_thread") || viewId.contains("layout_comment_thread") || viewId.contains("comment_composer")) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (hasVisibleEditText(child, maxDepth - 1)) return true
        }
        return false
    }

    private fun isInstagramCreationOrShareScreen(
        root: AccessibilityNodeInfo,
        texts: List<String>? = null,
        allTextIn: String? = null,
        allDescsIn: String? = null
    ): Boolean {
        // Guard: If this is an active Reel or Feed post viewer, it is NEVER a creation screen!
        if (findNodeByViewIdContains(root, "clips_author_username") != null ||
            findNodeByViewIdContains(root, "clips_viewer") != null ||
            (findNodeByViewIdContains(root, "like_button") != null && findNodeByViewIdContains(root, "comment_button") != null)) {
            return false
        }

        // Guard: If already in Reels section, do not falsely flag normal reel UI as creation
        val isReelsSection = lastSelectedBottomNavSection["com.instagram.android"] == "Reels"

        // 1. Check for interactive text editing (captions, search, comments)
        if (hasVisibleEditText(root)) {
            return true
        }

        // 2. Check for characteristic view IDs in Instagram creation / camera / editor / share sheets
        val creationViewIds = listOf(
            "cam_shutter", "camera_switch", "camera_roll", "shutter_button",
            "dial_picker", "capture_mode", "quick_capture", "camera_button",
            "camera_stub", "layout_camera",
            "gallery_grid", "media_picker", "album_filter", "gallery_recycler",
            "gallery_container", "gallery_folder", "gallery_title_container",
            "clips_editor", "clips_timeline", "video_timeline", "trim_editor",
            "clips_audio_mix_view", "text_tool", "sticker_tool",
            "filter_tool", "draw_tool", "story_sticker_container",
            "photo_view", "trim_handle", "voiceover", "audio_filter",
            "caption_text_view", "cover_photo_preview", "share_sheet",
            "share_footer", "clips_share", "metadata_share", "share_to_feed",
            "tag_people", "tag_users", "add_location", "advanced_settings"
        )
        for (idPart in creationViewIds) {
            if (findNodeByViewIdContains(root, idPart) != null) {
                return true
            }
        }

        // 3. Texts and Content Descriptions analysis
        val visibleTexts = texts?.toMutableList() ?: mutableListOf<String>().also {
            collectVisibleTexts(root, it, maxDepth = 25, maxNodes = 400)
        }
        val allText = allTextIn ?: visibleTexts.joinToString(" ").lowercase(java.util.Locale.ROOT)
        val allDescs = allDescsIn ?: run {
            val descs = mutableListOf<String>()
            collectContentDescriptions(root, descs, maxDepth = 25, maxNodes = 400)
            descs.joinToString(" ").lowercase(java.util.Locale.ROOT)
        }

        // Camera Shutter / Capture indicators
        if (allDescs.contains("camera shutter") || allDescs.contains("shutter") || allDescs.contains("شاتر") ||
            allDescs.contains("عکس‌برداری") || allDescs.contains("switch camera") || allDescs.contains("تغییر دوربین")) {
            return true
        }

        if (isReelsSection) {
            // Normal reels viewer content shouldn't be treated as creation
            return false
        }

        // Posting / Details / Share screens
        val shareIndicators = listOf(
            "new reel", "ریلز جدید",
            "new post", "پست جدید",
            "share to reels", "هم‌رسانی در ریلز",
            "edit cover", "ویرایش کاور",
            "write a caption", "نوشتن شرح", "یک شرح بنویسید", "نوشتن زیرنویس",
            "also share to feed", "همچنین هم‌رسانی در فید", "هم‌رسانی در فید",
            "tag people", "برچسب‌گذاری افراد", "تگ کردن افراد",
            "tag products", "برچسب‌گذاری محصولات",
            "add audience", "افزودن مخاطب", "مخاطبان",
            "add topics", "افزودن موضوع‌ها",
            "add location", "افزودن مکان",
            "save draft", "ذخیره پیش‌نویس",
            "edit video", "ویرایش ویدیو",
            "voiceover", "صدابرداری",
            "select multiple", "انتخاب چند مورد"
        )
        for (indicator in shareIndicators) {
            if (allText.contains(indicator) || allDescs.contains(indicator)) {
                return true
            }
        }

        // Story tray / Story creation buttons
        if ((allText.contains("your story") || allText.contains("استوری شما") || allText.contains("داستان شما")) &&
            (allText.contains("close friends") || allText.contains("دوستان صمیمی") || allDescs.contains("close friends") || allDescs.contains("دوستان صمیمی") || allText.contains("send to") || allText.contains("ارسال به"))) {
            return true
        }

        // Mode switcher in camera: POST, STORY, REEL, LIVE (in En or Fa)
        var modeCount = 0
        if (allText.contains("story") || allText.contains("استوری")) modeCount++
        if (allText.contains("reel") || allText.contains("ریلز")) modeCount++
        if (allText.contains("post") || allText.contains("پست")) modeCount++
        if (allText.contains("live") || allText.contains("زنده")) modeCount++
        if (modeCount >= 2 && (allText.contains("recents") || allText.contains("اخیراً") || allText.contains("gallery") || allText.contains("گالری") || allDescs.contains("camera") || allDescs.contains("دوربین"))) {
            return true
        }

        return false
    }

    private fun isInstagramNotificationsScreen(
        root: AccessibilityNodeInfo,
        visibleTexts: List<String>,
        allText: String,
        allDescs: String
    ): Boolean {
        // 1. View ID indicators for Notifications / Activity
        if (findNodeByViewIdContains(root, "newsfeed") != null ||
            findNodeByViewIdContains(root, "activity_center") != null ||
            findNodeByViewIdContains(root, "notification_center") != null ||
            findNodeByViewIdContains(root, "notification") != null) {
            return true
        }

        // 2. Action bar title view ID check (multiple common Instagram view IDs)
        val notifHeaders = setOf(
            "notifications", "activity", "اعلان‌ها", "اعلان", "فعالیت", "فعالیت‌ها",
            "اعلان های شما", "اعلانهای شما", "notifs", "updates", "به‌روزرسانی‌ها"
        )
        for (viewId in listOf(
            "com.instagram.android:id/action_bar_title",
            "com.instagram.android:id/title_text_view",
            "com.instagram.android:id/action_bar_textview_title",
            "com.instagram.android:id/action_bar_title_view"
        )) {
            val titleNodes = root.findAccessibilityNodeInfosByViewId(viewId)
            if (!titleNodes.isNullOrEmpty()) {
                for (node in titleNodes) {
                    val t = node.text?.toString()?.trim()?.lowercase(java.util.Locale.ROOT) ?: ""
                    if (notifHeaders.any { t.contains(it) }) return true
                }
            }
        }

        // 3. Top header text for Notifications / Activity
        val topTexts = visibleTexts.take(8).map { it.trim().lowercase(java.util.Locale.ROOT) }
        if (topTexts.any { txt -> notifHeaders.any { txt == it || txt.startsWith("$it ") || txt.endsWith(" $it") } }) {
            return true
        }

        // 4. Characteristic notification feed phrases in text
        val hasNotifPhrases = allText.contains("started following you") ||
            allText.contains("شما را دنبال کرد") ||
            allText.contains("liked your") ||
            allText.contains("پسندید") ||
            allText.contains("commented:") ||
            allText.contains("دیدگاه:") ||
            allText.contains("requested to follow") ||
            allText.contains("درخواست دنبال کردن")
        if (hasNotifPhrases && (allText.contains("today") || allText.contains("امروز") || allText.contains("this week") || allText.contains("این هفته") || allText.contains("earlier") || allText.contains("قبلی"))) {
            return true
        }

        return false
    }

    private fun isInstagramProfileScreen(
        root: AccessibilityNodeInfo,
        visibleTexts: List<String>,
        allText: String,
        allDescs: String
    ): Boolean {
        // 1. View ID indicators
        if (findNodeByViewIdContains(root, "user_detail_fragment") != null ||
            findNodeByViewIdContains(root, "profile_header_container") != null ||
            findNodeByViewIdContains(root, "row_profile_header") != null) {
            return true
        }

        // 2. Profile header buttons: Edit profile / Share profile
        val hasEditOrShare = allText.contains("edit profile") || allText.contains("share profile") ||
            allText.contains("ویرایش نمایه") || allText.contains("هم‌رسانی نمایه") ||
            allText.contains("ویرایش پروفایل") || allText.contains("هم‌رسانی پروفایل")

        if (hasEditOrShare) return true

        return false
    }

    private fun isInstagramInsightsScreen(
        root: AccessibilityNodeInfo,
        visibleTexts: List<String>,
        allText: String,
        allDescs: String
    ): Boolean {
        if (findNodeByViewIdContains(root, "insights") != null ||
            findNodeByViewIdContains(root, "business_hub") != null ||
            findNodeByViewIdContains(root, "pro_home") != null) {
            return true
        }

        val topTexts = visibleTexts.take(6).map { it.trim().lowercase(java.util.Locale.ROOT) }
        return topTexts.any { it == "professional dashboard" || it == "داشبورد حرفه‌ای" || it == "insights" || it == "بینش‌ها" }
    }

    private fun detectInstagramSection(root: AccessibilityNodeInfo): String {
        val screenBounds = android.graphics.Rect()
        root.getBoundsInScreen(screenBounds)
        val screenHeight = if (screenBounds.height() > 0) screenBounds.height() else applicationContext.resources.displayMetrics.heightPixels
        val screenWidth = if (screenBounds.width() > 0) screenBounds.width() else applicationContext.resources.displayMetrics.widthPixels

        // 1. Stories viewer
        if (findNodeByViewIdContains(root, "story_progress_bar") != null ||
            findNodeByViewIdContains(root, "progress_bar_wrapper") != null ||
            findNodeByViewIdContains(root, "stories_viewer_container") != null) {
            return "Stories"
        }

        // Deep screen traversal
        val visibleTexts = mutableListOf<String>()
        collectVisibleTexts(root, visibleTexts, maxDepth = 25, maxNodes = 600)
        val allText = visibleTexts.joinToString(" ").lowercase(java.util.Locale.ROOT)

        val contentDescs = mutableListOf<String>()
        collectContentDescriptions(root, contentDescs, maxDepth = 25, maxNodes = 600)
        val allDescs = contentDescs.joinToString(" ").lowercase(java.util.Locale.ROOT)

        // 2. Create / Camera / Story & Reels posting
        if (isInstagramCreationOrShareScreen(root, visibleTexts, allText, allDescs)) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Create"
            return "Create"
        }

        // 3. Notifications / Activity center (PRIORITIZED BEFORE REELS)
        if (isInstagramNotificationsScreen(root, visibleTexts, allText, allDescs)) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Activity"
            return "Activity"
        }

        // 4. Profile (PRIORITIZED BEFORE REELS)
        if (isInstagramProfileScreen(root, visibleTexts, allText, allDescs)) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Profile"
            return "Profile"
        }

        // 5. Insights / Professional Dashboard (PRIORITIZED BEFORE REELS)
        if (isInstagramInsightsScreen(root, visibleTexts, allText, allDescs)) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Profile"
            return "Profile"
        }

        // 6. Direct Messages / Inbox
        if ((allText.contains("messages") && allText.contains("chats")) ||
            (allDescs.contains("direct message") && !allDescs.contains("like") && !allDescs.contains("لایک")) ||
            allText.contains("search chats") || allText.contains("جستجو در گفتگوها")) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Messages"
            return "Messages"
        }

        // 7. Check if bottom navigation actively reports a selected tab
        val bottomSection = detectInstagramBottomNavSection(root, screenWidth, screenHeight)
        if (bottomSection != null) {
            lastSelectedBottomNavSection["com.instagram.android"] = bottomSection
            return bottomSection
        }

        // 8. Dedicated Reels Detection
        if (isInstagramReelsScreen(root, screenWidth, screenHeight, visibleTexts, allText, allDescs)) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Reels"
            return "Reels"
        }

        // 9. Explore (search grid)
        if ((allText.contains("search") || allText.contains("جستجو")) &&
            (allText.contains("for you") && allText.contains("tags") || allText.contains("explore"))) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Explore"
            return "Explore"
        }

        // 10. Home feed (Stories tray with "Your story" / "استوری شما" or Instagram header)
        if (allText.contains("your story") || allText.contains("استوری شما")) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Home"
            return "Home"
        }
        val topNodes = mutableListOf<AccessibilityNodeInfo>()
        findTopHeaderNodes(root, (screenHeight * 0.15f).toInt(), topNodes, maxDepth = 20)
        val topTextsAndDescs = topNodes.map { (it.text?.toString() ?: "") + " " + (it.contentDescription?.toString() ?: "") }.joinToString(" ").lowercase(java.util.Locale.ROOT)
        if (topTextsAndDescs.contains("instagram") || (topTextsAndDescs.contains("direct") && (topTextsAndDescs.contains("activity") || topTextsAndDescs.contains("notification")))) {
            lastSelectedBottomNavSection["com.instagram.android"] = "Home"
            return "Home"
        }

        // 11. Persistence: Continue recording the screen related to LAST SELECTED bottom navigation item
        val lastSelected = lastSelectedBottomNavSection["com.instagram.android"]
        if (!lastSelected.isNullOrEmpty()) {
            return lastSelected
        }

        // Initial default if no tab has ever been detected yet
        return "Home"
    }

    private fun getInstagramSectionFromTabNode(tab: AccessibilityNodeInfo): String? {
        val desc = (tab.contentDescription?.toString() ?: "").lowercase(java.util.Locale.ROOT)
        val text = (tab.text?.toString() ?: "").lowercase(java.util.Locale.ROOT)
        val viewId = (tab.viewIdResourceName ?: "").lowercase(java.util.Locale.ROOT)
        val combined = "$desc $text $viewId"

        return when {
            combined.contains("reel") || combined.contains("clips") || combined.contains("ریلز") -> "Reels"
            combined.contains("search") || combined.contains("explore") || combined.contains("جستجو") || combined.contains("کاوش") || combined.contains("اکسپلور") -> "Explore"
            combined.contains("home") || combined.contains("feed") || combined.contains("خانه") -> "Home"
            combined.contains("profile") || combined.contains("نمایه") || combined.contains("پروفایل") -> "Profile"
            combined.contains("create") || combined.contains("post") || combined.contains("ایجاد") || combined.contains("پست جدید") || combined.contains("camera") -> "Create"
            combined.contains("activity") || combined.contains("notification") || combined.contains("اعلان") -> "Activity"
            else -> null
        }
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
                val desc = (tab.contentDescription?.toString() ?: "").lowercase(java.util.Locale.ROOT)
                val stateDesc = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    (tab.stateDescription?.toString() ?: "").lowercase(java.util.Locale.ROOT)
                } else ""
                val isTabSelected = tab.isSelected || findSelectedInSubtree(tab) != null ||
                    desc.contains("selected") || desc.contains("active") || desc.contains("انتخاب") || desc.contains("فعال") ||
                    stateDesc.contains("selected") || stateDesc.contains("active") || stateDesc.contains("انتخاب") || stateDesc.contains("فعال")

                if (isTabSelected) {
                    return getInstagramSectionFromTabNode(tab) ?: sectionNameForBottomNavIndex(i)
                }
            }
        }

        // 2. Selected tab by position / coordinates
        val selectedTab = findBottomNavSelectedTab(root)
        if (selectedTab != null) {
            val tabRect = android.graphics.Rect()
            selectedTab.getBoundsInScreen(tabRect)
            if (tabRect.centerY() >= bottomNavMinY) {
                val fromTab = getInstagramSectionFromTabNode(selectedTab)
                if (fromTab != null) return fromTab

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
                    val fromNode = getInstagramSectionFromTabNode(node)
                    if (fromNode != null) return fromNode
                    return sectionNameForBottomNavIndex(i)
                }
                val desc = (node.contentDescription?.toString() ?: "").lowercase(java.util.Locale.ROOT)
                val stateDesc = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    (node.stateDescription?.toString() ?: "").lowercase(java.util.Locale.ROOT)
                } else ""

                if (desc.contains("selected") || desc.contains("active") || desc.contains("انتخاب") || desc.contains("فعال") ||
                    stateDesc.contains("selected") || stateDesc.contains("active") || stateDesc.contains("انتخاب") || stateDesc.contains("فعال")) {
                    val fromNode = getInstagramSectionFromTabNode(node)
                    if (fromNode != null) return fromNode
                    return sectionNameForBottomNavIndex(i)
                }
            }
        }

        return null
    }

    private fun sectionNameForBottomNavIndex(index: Int): String {
        val lastSelected = lastSelectedBottomNavSection["com.instagram.android"]
        if (lastSelected == "Reels" && (index == 2 || index == 3)) {
            return "Reels"
        }
        return when (index) {
            0 -> "Home"
            1 -> "Explore"
            2 -> "Create"
            3 -> "Reels"
            4 -> "Profile"
            else -> "Home"
        }
    }

    private fun isInstagramReelsScreen(
        root: AccessibilityNodeInfo,
        screenWidth: Int,
        screenHeight: Int,
        texts: List<String>? = null,
        allTextIn: String? = null,
        allDescsIn: String? = null
    ): Boolean {
        // 1. EXCLUSIONS FIRST (Fail-fast against non-Reels screens)
        val visibleTexts = texts?.toMutableList() ?: mutableListOf<String>().also {
            collectVisibleTexts(root, it, maxDepth = 25, maxNodes = 400)
        }
        val allText = allTextIn ?: visibleTexts.joinToString(" ").lowercase(java.util.Locale.ROOT)
        val allDescs = allDescsIn ?: run {
            val descs = mutableListOf<String>()
            collectContentDescriptions(root, descs, maxDepth = 25, maxNodes = 400)
            descs.joinToString(" ").lowercase(java.util.Locale.ROOT)
        }

        // A. Creation, editing, selecting, or sharing a Reel/Story/Post
        if (isInstagramCreationOrShareScreen(root, visibleTexts, allText, allDescs)) {
            return false
        }

        // B. Interactive text inputs (comments sheet, DM composer, search bar, caption)
        if (hasVisibleEditText(root)) {
            return false
        }

        // C. Notifications / Activity screen (Strictly excluded)
        if (isInstagramNotificationsScreen(root, visibleTexts, allText, allDescs)) {
            return false
        }

        // D. Profile screen (Strictly excluded)
        if (isInstagramProfileScreen(root, visibleTexts, allText, allDescs)) {
            return false
        }

        // E. Insights / Professional Dashboard screen (Strictly excluded)
        if (isInstagramInsightsScreen(root, visibleTexts, allText, allDescs)) {
            return false
        }

        // F. Instagram Stories viewer check
        if (findNodeByViewIdContains(root, "story_progress_bar") != null ||
            findNodeByViewIdContains(root, "progress_bar_wrapper") != null ||
            findNodeByViewIdContains(root, "stories_viewer_container") != null) {
            return false
        }

        // G. Home feed top header check
        val topHeaderNodes = mutableListOf<AccessibilityNodeInfo>()
        findTopHeaderNodes(root, (screenHeight * 0.15f).toInt(), topHeaderNodes, maxDepth = 20)
        val topHeaderCombined = topHeaderNodes.map {
            (it.text?.toString() ?: "") + " " + (it.contentDescription?.toString() ?: "")
        }.joinToString(" ").lowercase(java.util.Locale.ROOT)
        if (topHeaderCombined.contains("instagram") ||
            (topHeaderCombined.contains("direct") && (topHeaderCombined.contains("notification") || topHeaderCombined.contains("activity")))) {
            return false
        }

        // 2. POSITIVE MATCHES:
        // A. Specific Reels viewer container IDs (ONLY viewer IDs, not generic "clips" which matches clips_editor)
        if (findNodeByViewIdContains(root, "clips_viewer") != null ||
            findNodeByViewIdContains(root, "layout_clips_viewer") != null ||
            findNodeByViewIdContains(root, "reel_viewer") != null ||
            findNodeByViewIdContains(root, "clips_video_container") != null ||
            findNodeByViewIdContains(root, "clips_author_username") != null) {
            return true
        }

        // B. Top header explicitly has "Reels" / "Friends" ("ریلز", "دوستان")
        if (isInstagramReelsHeaderPresent(root, screenWidth, screenHeight)) {
            return true
        }

        // C. Bottom Navigation actively indicates Reels tab is selected or was selected
        val bottomSection = detectInstagramBottomNavSection(root, screenWidth, screenHeight)
        if (bottomSection == "Reels") {
            return true
        }
        if (lastSelectedBottomNavSection["com.instagram.android"] == "Reels") {
            return true
        }

        // C. Vertical action rail check (tightened to small buttons pinned at screen edge)
        val railNodes = mutableListOf<AccessibilityNodeInfo>()
        collectReelsScreenData(root, railNodes, screenWidth, screenHeight, maxDepth = 25, maxNodes = 400)

        val likeNode = railNodes.firstOrNull { node ->
            val desc = (node.contentDescription?.toString() ?: node.text?.toString() ?: "").trim().lowercase(java.util.Locale.ROOT)
            val hasLikeKeyword = desc.contains("like") || desc.contains("liked") || desc.contains("پسندیدن") || desc.contains("پسند") || desc.contains("لایک")
            val notSentence = !desc.contains("liked your") && !desc.contains("liked a") &&
                !desc.contains("others") && !desc.contains("دیگران") && !desc.contains("شما را پسندید")
            desc.length <= 60 && hasLikeKeyword && notSentence
        }
        val commentNode = railNodes.firstOrNull { node ->
            val desc = (node.contentDescription?.toString() ?: node.text?.toString() ?: "").trim().lowercase(java.util.Locale.ROOT)
            val hasCommentKeyword = desc.contains("comment") || desc.contains("دیدگاه") || desc.contains("نظر") || desc.contains("کامنت")
            val notSentence = !desc.contains("commented") && !desc.contains("نظر داد") &&
                !desc.contains("دیدگاه نوشت") && !desc.contains("کامنت گذاشت")
            desc.length <= 60 && hasCommentKeyword && notSentence
        }

        if (likeNode != null && commentNode != null) {
            val likeRect = android.graphics.Rect()
            val commentRect = android.graphics.Rect()
            likeNode.getBoundsInScreen(likeRect)
            commentNode.getBoundsInScreen(commentRect)

            val sameSide = (likeRect.centerX() > screenWidth * 0.5f) == (commentRect.centerX() > screenWidth * 0.5f)
            if (sameSide) {
                val xDiff = kotlin.math.abs(likeRect.centerX() - commentRect.centerX())
                val yDiff = kotlin.math.abs(likeRect.centerY() - commentRect.centerY())

                // In Reels: Like & Comment share almost the same X coordinate, and are stacked vertically
                val minYDiff = (screenHeight * 0.03f).toInt()
                val maxYDiff = (screenHeight * 0.35f).toInt()
                if (xDiff < screenWidth * 0.15f && yDiff in minYDiff..maxYDiff) {
                    return true
                }
            }
        }

        return false
    }

    private fun collectReelsScreenData(
        node: AccessibilityNodeInfo,
        rightRailNodes: MutableList<AccessibilityNodeInfo>,
        screenWidth: Int,
        screenHeight: Int,
        maxDepth: Int,
        maxNodes: Int
    ) {
        if (maxDepth <= 0 || rightRailNodes.size >= maxNodes) return

        val rect = android.graphics.Rect()
        try {
            if (node.isVisibleToUser) {
                node.getBoundsInScreen(rect)
                val isRailX = rect.centerX() > screenWidth * 0.65f || rect.centerX() < screenWidth * 0.35f
                val isRailY = rect.centerY() in (screenHeight * 0.15f).toInt()..(screenHeight * 0.92f).toInt()
                val isSmallButton = rect.width() in 20..(screenWidth * 0.30f).toInt() &&
                    rect.height() in 20..(screenHeight * 0.20f).toInt()

                if (isRailX && isRailY && isSmallButton) {
                    rightRailNodes.add(node)
                }
            }
        } catch (_: Exception) { }

        for (i in 0 until node.childCount) {
            if (rightRailNodes.size >= maxNodes) break
            val child = node.getChild(i) ?: continue
            collectReelsScreenData(child, rightRailNodes, screenWidth, screenHeight, maxDepth - 1, maxNodes)
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
     * Strictly excludes creation headers like "New reel", "Close friends", "Share to Reels", drafts, etc.
     */
    private fun isInstagramReelsHeaderPresent(root: AccessibilityNodeInfo, screenWidth: Int, screenHeight: Int): Boolean {
        val headerMaxY = (screenHeight * 0.22f).toInt()
        val forbiddenSubstrings = listOf(
            "جدید", "new", "صمیمی", "close", "پیش‌نویس", "draft",
            "هم‌رسانی", "اشتراک", "share", "تنظیمات", "settings",
            "ویرایش", "edit", "پست", "post", "استوری", "story",
            "شرح", "caption", "مکان", "location", "افراد", "people",
            "followers", "following", "دنبال‌کنندگان", "دنبال‌شونده‌ها", "posts"
        )
        val validTabs = setOf("reels", "friends", "ریلز", "دوستان")

        // 1. Fast text-indexed search
        for (kw in listOf("Reels", "Friends", "ریلز", "دوستان")) {
            try {
                val matching = root.findAccessibilityNodeInfosByText(kw)
                if (!matching.isNullOrEmpty()) {
                    val rect = android.graphics.Rect()
                    for (node in matching) {
                        try {
                            if (!node.isVisibleToUser) continue
                            node.getBoundsInScreen(rect)
                            if (rect.bottom in 40..headerMaxY && rect.top >= -20 && rect.height() > 10) {
                                val txt = (node.text?.toString() ?: "").trim().lowercase(java.util.Locale.ROOT)
                                val desc = (node.contentDescription?.toString() ?: "").trim().lowercase(java.util.Locale.ROOT)

                                if (forbiddenSubstrings.any { txt.contains(it) || desc.contains(it) }) {
                                    continue
                                }
                                if (validTabs.contains(txt) || validTabs.contains(desc)) {
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

                    if (forbiddenSubstrings.any { txt.contains(it) || desc.contains(it) }) {
                        continue
                    }
                    if (validTabs.contains(txt) || validTabs.contains(desc)) {
                        return true
                    }
                }
            } catch (_: Exception) {}
        }

        return false
    }

    private fun extractReelSignature(root: AccessibilityNodeInfo, screenWidth: Int, screenHeight: Int): String {
        val rootPkg = root.packageName?.toString() ?: ""
        if (rootPkg == "com.goldenminute.catalyst") {
            return ""
        }

        var author = ""
        val authorNodes = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_author_username")
        if (!authorNodes.isNullOrEmpty()) {
            for (node in authorNodes) {
                val t = node.text?.toString()?.trim() ?: ""
                if (t.isNotEmpty()) {
                    author = t
                    break
                }
            }
        }

        // Confine the scanned area strictly to the reel content zone (8% to 92% of screen height)
        // to capture caption, username, and audio info while avoiding system bars.
        val minY = (screenHeight * 0.08f).toInt()
        val maxY = (screenHeight * 0.92f).toInt()
        val texts = mutableListOf<String>()
        collectReelsUniqueTexts(root, minY, maxY, texts, maxDepth = 45, maxNodes = 8)

        if (author.isEmpty()) {
            val possibleAuthor = texts.firstOrNull { it.startsWith("@") || (it.length in 3..25 && !it.contains(" ") && !it.contains("\n")) }
            if (possibleAuthor != null) {
                author = possibleAuthor
            }
        }

        return if (author.isNotEmpty()) {
            "author:$author|${texts.joinToString("|")}"
        } else {
            texts.joinToString("|")
        }
    }

    private fun collectReelsUniqueTexts(
        node: AccessibilityNodeInfo,
        minY: Int,
        maxY: Int,
        result: MutableList<String>,
        maxDepth: Int,
        maxNodes: Int
    ) {
        if (maxDepth <= 0 || result.size >= maxNodes) return
        val nodePkg = node.packageName?.toString() ?: ""
        if (nodePkg == "com.goldenminute.catalyst") return

        try {
            // Must be visible to user to prevent scraping offscreen preloaded reels or hidden views
            if (!node.isVisibleToUser) return

            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            if (rect.centerY() in minY..maxY && rect.width() > 0 && rect.height() > 0) {
                val txt = node.text?.toString()?.trim() ?: ""
                val desc = node.contentDescription?.toString()?.trim() ?: ""
                val candidate = if (txt.isNotEmpty()) txt else desc

                if (candidate.length in 2..100 && isStableReelContent(candidate)) {
                    if (!result.contains(candidate)) {
                        result.add(candidate)
                    }
                }
            }
        } catch (_: Exception) {}

        for (i in 0 until node.childCount) {
            if (result.size >= maxNodes) break
            val child = node.getChild(i) ?: continue
            collectReelsUniqueTexts(child, minY, maxY, result, maxDepth - 1, maxNodes)
        }
    }

    private fun isStableReelContent(text: String): Boolean {
        val lower = text.lowercase(java.util.Locale.ROOT).trim()
        val overlayKeywords = listOf(
            "هشدار اسکرول", "اسکرول بی‌هدف", "اسکرول بی‌پایان", "صرف‌شده", "هدفمند هستم", "بی‌هدف اسکرول",
            "doomscrolling", "mindfulness", "wasted time", "are you sure", "inspiring"
        )
        if (overlayKeywords.any { lower.contains(it) }) {
            return false
        }
        val genericActions = listOf(
            "home", "reels", "reel", "search", "search and explore", "explore", "message", "messages", "profile", "activity", "direct", "camera",
            "خانه", "ریلز", "جستجو", "کاوش", "پیام", "پیام‌ها", "پروفایل", "نمایه", "فعالیت", "دوربین",
            "like", "likes", "liked", "comment", "comments", "add comment", "add a comment", "share", "remix", "send", "save", "saved", "bookmark",
            "more options", "audio", "video", "button", "play", "pause", "tagged",
            "follow", "following", "sponsored", "threads", "suggested for you", "see translation",
            "more", "less", "view all", "template", "use template", "watch more", "verified",
            "double tap", "tap to play", "play or pause", "double tap to play or pause",
            "seekbar", "seek bar", "repost", "reposted", "reshare", "reshared", "story", "stories", "highlight", "highlights", "grid view", "seen",
            "پسندیدن", "پسندیدید", "دیدگاه", "دیدگاه‌ها", "نظر", "نظرات", "کامنت", "هم‌رسانی", "اشتراک", "اشتراک‌گذاری", "ذخیره", "ذخیره‌شده",
            "ارسال", "ریمیکس", "صدا", "ویدیو", "پخش", "توقف", "گزینه‌ها", "دکمه", "برچسب‌گذاری",
            "دنبال کردن", "دنبال می‌کنید", "تبلیغ", "حمایت‌شده", "پیشنهاد برای شما", "مشاهده ترجمه", "دیدن ترجمه",
            "بیشتر", "کمتر", "مشاهده همه", "افزودن دیدگاه", "تردز", "الگو", "قالب", "استفاده از الگو", "مشاهده بیشتر", "تأییدشده"
        )
        if (genericActions.any { lower == it || lower.startsWith("$it ") || lower.endsWith(" $it") }) {
            return false
        }
        if (lower.matches(Regex("^[0-9.,:\\s]+[kmbKMB]?$"))) {
            return false
        }
        return true
    }

    // NOTE: checkInstagramReelsDoomscrolling has been removed.
    // Doomscrolling detection is now handled directly in onAccessibilityEvent:
    //   1) Initial Reels entry → detected via TYPE_WINDOW_STATE_CHANGED section transition
    //   2) Reel swipes → detected via dedicated TYPE_VIEW_SCROLLED handler (not throttled)
    // This eliminates signature-instability debounce storms and dropped scroll events.

    private var pendingDoomscrollJob: Job? = null

    private fun scheduleDoomscrollingOverlay(packageName: String, reelId: String) {
        val now = System.currentTimeMillis()
        if (now - overlayDismissedTime < 600L) {
            return
        }
        if (pendingDoomscrollTargetReel == reelId && pendingDoomscrollJob?.isActive == true) {
            // Already scheduled and counting down for this exact reel, keep it running!
            return
        }

        pendingDoomscrollTargetReel = reelId
        pendingDoomscrollJob?.cancel()
        pendingDoomscrollJob = scope.launch {
            delay(500L) // Let the swipe animation settle before showing the dialog
            if (!checkIsOverlayShowing() && isDoomscrollingAlertEnabled) {
                val stillInReels = (currentAppPackage == "com.instagram.android")
                if (!stillInReels || checkIsOverlayShowing() || !isDoomscrollingAlertEnabled) return@launch

                val currentTime = System.currentTimeMillis()
                if (currentTime - overlayDismissedTime < 600L) {
                    Log.d(TAG, "Overlay was dismissed recently, skipping overlay")
                    return@launch
                }

                // Extract actual reel signature from the active screen!
                val rootNode = rootInActiveWindow ?: return@launch
                val screenHeight = applicationContext.resources.displayMetrics.heightPixels
                val screenWidth = applicationContext.resources.displayMetrics.widthPixels

                // Live screen verification: Must be strictly Instagram Reels!
                val isReels = isInstagramReelsScreen(rootNode, screenWidth, screenHeight) ||
                    ((currentSection == "Reels" || lastSelectedBottomNavSection["com.instagram.android"] == "Reels")
                     && !isInstagramNotificationsScreen(rootNode, emptyList(), "", "")
                     && !isInstagramProfileScreen(rootNode, emptyList(), "", "")
                     && !hasVisibleEditText(rootNode))

                if (!isReels) {
                    Log.d(TAG, "Active screen is not Instagram Reels. Aborting overlay.")
                    rootNode.recycle()
                    return@launch
                }

                var currentScreenSig = extractReelSignature(rootNode, screenWidth, screenHeight)
                rootNode.recycle()

                // If signature was not ready yet, retry once after 250ms
                if (currentScreenSig.isEmpty()) {
                    delay(250L)
                    val retryRoot = rootInActiveWindow
                    if (retryRoot != null) {
                        if (!isInstagramReelsScreen(retryRoot, screenWidth, screenHeight)) {
                            retryRoot.recycle()
                            return@launch
                        }
                        currentScreenSig = extractReelSignature(retryRoot, screenWidth, screenHeight)
                        retryRoot.recycle()
                    }
                }

                // 1. Check if user is still on the inspired reel!
                if (isInspirationActiveForCurrentReel) {
                    val hasSwipedPast = reelScrollCounter > lastInspirationReelCounter
                    val matchesInspiration = (currentScreenSig.isNotEmpty() && ignoredReelSignature.isNotEmpty() && isSameReel(currentScreenSig, ignoredReelSignature))

                    if (!hasSwipedPast || matchesInspiration) {
                        Log.d(TAG, "User still on inspiration reel (swipedPast=$hasSwipedPast, matchesInspiration=$matchesInspiration). Skipping overlay.")
                        return@launch
                    }

                    // The user has swiped past the inspired reel to a new reel!
                    Log.d(TAG, "User swiped past inspiration reel (counter=$reelScrollCounter > lastInspiration=$lastInspirationReelCounter). Clearing inspiration lock.")
                    isInspirationActiveForCurrentReel = false
                    ignoredReelSignature = ""
                }

                // 2. Check if current reel was EVER marked as inspiration in this session (e.g. scrolled back up to it)
                if (currentScreenSig.isNotEmpty() && inspiredReelSignatures.any { isSameReel(it, currentScreenSig) }) {
                    Log.d(TAG, "Current reel ($currentScreenSig) is in inspiredReelSignatures set. Skipping overlay.")
                    return@launch
                }

                // 3. Prevent duplicate dialog on the SAME reel!
                if (reelScrollCounter == lastDialogReelCounter && lastDialogReelCounter > 0L) {
                    Log.d(TAG, "Already showed dialog for reel counter $reelScrollCounter. Skipping duplicate overlay.")
                    return@launch
                }
                if (currentScreenSig.isNotEmpty() && lastDialogReelSignature.isNotEmpty() && isSameReel(currentScreenSig, lastDialogReelSignature)) {
                    Log.d(TAG, "Current reel signature matches last dialog reel ($currentScreenSig). Skipping duplicate overlay.")
                    return@launch
                }
                if (currentScreenSig.isNotEmpty() && shownDialogReelSignatures.any { isSameReel(it, currentScreenSig) }) {
                    Log.d(TAG, "Current reel was already shown previously ($currentScreenSig). Skipping duplicate overlay.")
                    return@launch
                }

                lastDialogReelCounter = reelScrollCounter
                lastDialogReelSignature = currentScreenSig.ifEmpty { reelId }
                activeReelSignature = currentScreenSig.ifEmpty { reelId }
                lastReelsDoomscrollTriggerTime = System.currentTimeMillis()

                Log.i(TAG, "Showing doomscrolling alert dialog for reel: ${lastDialogReelSignature.take(50)}")
                launchMindfulnessOverlay(packageName, "Reels")
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
    // Mindfulness Check — Instagram-only doomscrolling alert
    // ═══════════════════════════════════════════════════════════════

    private suspend fun performMindfulnessCheck(packageName: String, section: String) {
        // Doomscrolling alert dialog is strictly for Instagram Reels only.
        // It must NEVER trigger in any other app or in non-Reels sections of Instagram (Home, Explore, etc.)
        return
    }

    private fun launchMindfulnessOverlay(packageName: String, section: String) {
        // Strict guard: doomscrolling overlay dialog is exclusively for Instagram Reels
        if (packageName != "com.instagram.android" || section != "Reels") {
            Log.d(TAG, "Ignoring MindfulnessOverlay: only allowed in Instagram Reels (got $packageName / $section)")
            return
        }

        // Do not show doomscrolling dialog if user has disabled the option
        if (!isDoomscrollingAlertEnabled) return

        // Ensure Display over other apps (SYSTEM_ALERT_WINDOW) is permitted
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot launch MindfulnessOverlayActivity: SYSTEM_ALERT_WINDOW (Display over other apps) permission is NOT granted!")
            return
        }

        // Prevent double-launch
        if (checkIsOverlayShowing()) return
        isOverlayShowing = true
        overlayShownTime = System.currentTimeMillis()

        // Set a minimum cooldown (30s) to prevent rapid re-triggering of performMindfulnessCheck
        appMindfulnessCooldownUntil[packageName] = System.currentTimeMillis() + 30_000L

        // Resolve a human-readable app name
        val appName = getAppDisplayName(packageName)

        // Overlay activity with separate taskAffinity ("")
        val intent = Intent(this, com.goldenminute.catalyst.ui.MindfulnessOverlayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_SINGLE_TOP
                or Intent.FLAG_ACTIVITY_CLEAR_TOP
                or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            putExtra("EXTRA_APP_NAME", appName)
            putExtra("EXTRA_SECTION_NAME", section)
            putExtra("EXTRA_PACKAGE_NAME", packageName)
            putExtra("EXTRA_TOTAL_DOOMSCROLL_MS", currentSessionDoomscrollWastedMs)
            putExtra("EXTRA_LANGUAGE", currentAppLanguage)
            putExtra("EXTRA_SHOW_HIGH_PRIORITY_TASKS", false)
        }

        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }.toBundle()
        } else null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && options != null) {
                val pi = android.app.PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                pi.send(this, 0, null, null, null, null, options)
            } else {
                startActivity(intent, options)
            }
            Log.d(TAG, "Successfully started MindfulnessOverlayActivity for $section in $packageName")
        } catch (e: Exception) {
            try {
                startActivity(intent)
                Log.d(TAG, "Fallback direct startActivity succeeded for $section in $packageName")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to launch MindfulnessOverlayActivity", e2)
                isOverlayShowing = false
                overlayShownTime = 0L
            }
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
