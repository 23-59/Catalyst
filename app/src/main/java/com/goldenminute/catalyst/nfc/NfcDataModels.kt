package com.goldenminute.catalyst.nfc

import com.goldenminute.catalyst.data.Category
import com.goldenminute.catalyst.data.NoteItem
import com.goldenminute.catalyst.data.SubTask
import com.goldenminute.catalyst.data.TodoItem
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@JsonClass(generateAdapter = true)
data class NfcPayload(
    val version: Int = 1,
    val type: String = "quick_tasks", // "quick_tasks", "custom", "full_backup"
    val timestamp: Long = System.currentTimeMillis(),
    val categories: List<Category> = emptyList(),
    val todos: List<TodoItem> = emptyList(),
    val subTasks: List<SubTask> = emptyList(),
    val notes: List<NoteItem> = emptyList()
) {
    fun toJson(): String {
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(NfcPayload::class.java)
        return adapter.toJson(this)
    }

    fun toCompressedBytes(): ByteArray {
        val jsonBytes = toJson().toByteArray(Charsets.UTF_8)
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { gzip ->
            gzip.write(jsonBytes)
        }
        return baos.toByteArray()
    }

    companion object {
        fun fromJson(json: String): NfcPayload? {
            return try {
                val moshi = Moshi.Builder().build()
                val adapter = moshi.adapter(NfcPayload::class.java)
                adapter.fromJson(json)
            } catch (e: Exception) {
                // Fallback attempt: maybe it's a legacy BackupData JSON?
                try {
                    val moshi = Moshi.Builder().build()
                    val backupAdapter = moshi.adapter(com.goldenminute.catalyst.data.BackupData::class.java)
                    val backupData = backupAdapter.fromJson(json)
                    if (backupData != null) {
                        NfcPayload(
                            version = 1,
                            type = "legacy_backup",
                            timestamp = System.currentTimeMillis(),
                            categories = backupData.categories,
                            todos = backupData.todos,
                            subTasks = backupData.subTasks,
                            notes = backupData.notes
                        )
                    } else null
                } catch (e2: Exception) {
                    null
                }
            }
        }

        fun fromCompressedBytes(bytes: ByteArray): NfcPayload? {
            return try {
                val bais = ByteArrayInputStream(bytes)
                val uncompressed = GZIPInputStream(bais).bufferedReader(Charsets.UTF_8).use { it.readText() }
                fromJson(uncompressed)
            } catch (e: Exception) {
                null
            }
        }

        fun isGzipStream(bytes: ByteArray): Boolean {
            return bytes.size >= 2 &&
                    bytes[0] == 0x1f.toByte() &&
                    bytes[1] == 0x8b.toByte()
        }
    }
}

sealed interface NfcWriteResult {
    object Success : NfcWriteResult
    object ReadOnlyTag : NfcWriteResult
    data class InsufficientSpace(val requiredBytes: Int, val availableBytes: Int) : NfcWriteResult
    object TagLost : NfcWriteResult
    data class Error(val message: String) : NfcWriteResult
}

sealed interface NfcReadResult {
    data class Success(val payload: NfcPayload) : NfcReadResult
    object EmptyTag : NfcReadResult
    object InvalidData : NfcReadResult
    object TagLost : NfcReadResult
    data class Error(val message: String) : NfcReadResult
}
