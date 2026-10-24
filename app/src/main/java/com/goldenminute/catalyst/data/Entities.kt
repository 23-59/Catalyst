package com.goldenminute.catalyst.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val type: String = "task"
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "todos",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["isCompleted", "priority", "dueDate"])
    ]
)
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val dueDate: Long? = null,
    val priority: Int = 1, // 0 = Low, 1 = Medium, 2 = High
    val categoryId: Int? = null,
    val isCompleted: Boolean = false,
    val hasReminder: Boolean = false,
    val reminderTime: Long? = null,
    val isDaily: Boolean = false,
    val hasTime: Boolean = true,
    val completedDate: Long? = null,
    val position: Int = 0,
    val elapsedTime: Long = 0L,
    val pomodoroCycles: Int = 0,
    val recordedTimes: String = "",
    val isReminderRepeating: Boolean = false,
    @Json(name = "associatedPackageName") @ColumnInfo(name = "associatedPackageName") val associatedPackageNames: String? = null,
    val trackingMode: String = "Strict", // "Strict" or "Creative"
    val timerMode: String = "Stopwatch", // "Pomodoro" or "Stopwatch"
    val associatedSections: String? = null // Format: "pkg:Section1|Section2,pkg2:Section3" or null for whole-app
)

@JsonClass(generateAdapter = true)
data class RecordedTimeItem(
    val isPomodoro: Boolean,
    val value: Long,
    val timestamp: Long,
    val appSections: Map<String, Long> = emptyMap()
) {
    fun toSerializedString(): String {
        val type = if (isPomodoro) "P" else "S"
        val sectionsStr = appSections.entries.joinToString(",") { "${it.key}=${it.value}" }
        return if (sectionsStr.isNotEmpty()) "$type:$value:$timestamp:$sectionsStr" else "$type:$value:$timestamp"
    }

    companion object {
        fun fromSerializedString(str: String): RecordedTimeItem? {
            val parts = str.split(":")
            if (parts.size < 3) return null
            val isPomodoro = parts[0] == "P"
            val value = parts[1].toLongOrNull() ?: return null
            val timestamp = parts[2].toLongOrNull() ?: return null
            
            val appSections = mutableMapOf<String, Long>()
            if (parts.size > 3 && parts[3].isNotEmpty()) {
                val sectionPairs = parts[3].split(",")
                for (pair in sectionPairs) {
                    val kv = pair.split("=")
                    if (kv.size == 2) {
                        kv[1].toLongOrNull()?.let { appSections[kv[0]] = it }
                    }
                }
            }
            
            return RecordedTimeItem(isPomodoro, value, timestamp, appSections)
        }

        fun parseList(serialized: String?): List<RecordedTimeItem> {
            if (serialized.isNullOrEmpty()) return emptyList()
            return serialized.split(";").mapNotNull { fromSerializedString(it) }
        }

        fun serializeList(list: List<RecordedTimeItem>): String {
            return list.joinToString(";") { it.toSerializedString() }
        }
    }
}

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TodoItem::class,
            parentColumns = ["id"],
            childColumns = ["todoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["todoId"])]
)
data class SubTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val todoId: Int,
    val title: String,
    val isCompleted: Boolean = false
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "notes",
    indices = [Index(value = ["createdAt"])]
)
data class NoteItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val colorIndex: Int = 0,
    val categoryId: Int? = null
)

@JsonClass(generateAdapter = true)
data class BackupData(
    val categories: List<Category>,
    val todos: List<TodoItem>,
    val subTasks: List<SubTask>,
    val notes: List<NoteItem>
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "doomscroll_videos",
    indices = [Index(value = ["timestamp"])]
)
data class DoomscrollVideoRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String = "com.instagram.android",
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L
)

