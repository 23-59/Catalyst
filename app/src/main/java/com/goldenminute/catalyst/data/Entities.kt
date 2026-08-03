package com.goldenminute.catalyst.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
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
    val isReminderRepeating: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RecordedTimeItem(
    val isPomodoro: Boolean,
    val value: Long,
    val timestamp: Long
) {
    fun toSerializedString(): String {
        val type = if (isPomodoro) "P" else "S"
        return "$type:$value:$timestamp"
    }

    companion object {
        fun fromSerializedString(str: String): RecordedTimeItem? {
            val parts = str.split(":")
            if (parts.size < 3) return null
            val isPomodoro = parts[0] == "P"
            val value = parts[1].toLongOrNull() ?: return null
            val timestamp = parts[2].toLongOrNull() ?: return null
            return RecordedTimeItem(isPomodoro, value, timestamp)
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
