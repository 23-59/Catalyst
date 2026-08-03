const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/goldenminute/catalyst/service/TimerService.kt', 'utf8');

const target1 = `    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_STOP_TIMER") {
            TimerStateManager.isTimerRunning.value = false
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundServiceWithNotification()
        observeTimerState()

        return START_STICKY
    }`;

const replace1 = `    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_STOP_TIMER") {
            TimerStateManager.resetCurrentMode()
            stopSelf()
            return START_NOT_STICKY
        } else if (intent?.action == "ACTION_PAUSE_TIMER") {
            TimerStateManager.isTimerRunning.value = false
            return START_STICKY
        }

        startForegroundServiceWithNotification()
        observeTimerState()

        return START_STICKY
    }`;

const target2 = `import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged`;
const replace2 = `import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.goldenminute.catalyst.data.DataStoreKeys
import com.goldenminute.catalyst.data.appDataStore`;

const target3 = `        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = "ACTION_STOP_TIMER"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (state.isPomodoro) state.pomodoroMode else "Stopwatch"
        val timeValue = if (state.isPomodoro) state.timeLeft else state.timeElapsed
        
        val totalSeconds = (timeValue / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val contentText = String.format("%02d:%02d", minutes, seconds)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_name) // Use the generated text image asset
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stat_name, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()`;

const replace3 = `        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = "ACTION_STOP_TIMER"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val pauseIntent = Intent(this, TimerService::class.java).apply {
            action = "ACTION_PAUSE_TIMER"
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val saveIntent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_SAVE_TIMER"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val savePendingIntent = PendingIntent.getActivity(
            this, 3, saveIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val isFa = runBlocking { 
            applicationContext.appDataStore.data.first()[DataStoreKeys.LANGUAGE] == "fa" 
        }

        val stopStr = if (isFa) "توقف" else "Stop"
        val pauseStr = if (isFa) "مکث" else "Pause"
        val saveStr = if (isFa) "ذخیره" else "Save"
        
        val title = if (state.isPomodoro) {
            when (state.pomodoroMode) {
                "Focus" -> if (isFa) "تمرکز" else "Focus"
                "Short Break" -> if (isFa) "استراحت کوتاه" else "Short Break"
                "Long Break" -> if (isFa) "استراحت طولانی" else "Long Break"
                else -> state.pomodoroMode
            }
        } else {
            if (isFa) "کرنومتر" else "Stopwatch"
        }
        
        val timeValue = if (state.isPomodoro) state.timeLeft else state.timeElapsed
        
        val totalSeconds = (timeValue / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val contentText = String.format("%02d:%02d", minutes, seconds)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_name) // Use the generated text image asset
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stat_name, stopStr, stopPendingIntent)
            .addAction(R.drawable.ic_stat_name, pauseStr, pausePendingIntent)
            .addAction(R.drawable.ic_stat_name, saveStr, savePendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()`;

if (content.includes(target1) && content.includes(target3)) {
    content = content.replace(target1, replace1);
    content = content.replace(target2, replace2);
    content = content.replace(target3, replace3);
    fs.writeFileSync('app/src/main/java/com/goldenminute/catalyst/service/TimerService.kt', content);
    console.log('TimerService updated');
} else {
    console.log('Targets not found');
}
