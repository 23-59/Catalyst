package com.goldenminute.catalyst.nfc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.provider.Settings
import java.io.IOException
import java.util.Arrays

object NfcManager {

    /**
     * Active callback to process NFC tags whether discovered via ReaderMode
     * or via fallback Intent dispatch (e.g. onNewIntent in MainActivity).
     */
    var activeTagCallback: ((Tag) -> Unit)? = null

    fun isNfcSupported(context: Context): Boolean {
        return NfcAdapter.getDefaultAdapter(context.applicationContext) != null
    }

    fun isNfcEnabled(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context.applicationContext)
        return adapter != null && adapter.isEnabled
    }

    fun getNfcSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NFC_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun enableReaderMode(activity: Activity, onTagDiscovered: (Tag) -> Unit) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        activeTagCallback = onTagDiscovered
        // Note: We deliberately do NOT include FLAG_READER_NO_PLATFORM_SOUNDS so the user
        // receives clear native haptic/audio confirmation when their card is detected.
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V
        try {
            adapter.enableReaderMode(activity, { tag ->
                activeTagCallback?.invoke(tag)
            }, flags, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disableReaderMode(activity: Activity) {
        activeTagCallback = null
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        try {
            adapter.disableReaderMode(activity)
        } catch (e: Exception) {
            // Ignored if reader mode was not active or activity is finishing
        }
    }

    fun readFromTag(tag: Tag): NfcReadResult {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            val formatable = NdefFormatable.get(tag)
            return if (formatable != null) {
                NfcReadResult.EmptyTag
            } else {
                NfcReadResult.InvalidData
            }
        }

        // First check if the system already cached the NDEF message during discovery
        var message = ndef.cachedNdefMessage
        if (message == null) {
            try {
                if (!ndef.isConnected) {
                    ndef.connect()
                }
                message = ndef.ndefMessage ?: ndef.cachedNdefMessage
                try { ndef.close() } catch (e: Exception) {}
            } catch (e: TagLostException) {
                return NfcReadResult.TagLost
            } catch (e: IOException) {
                return NfcReadResult.TagLost
            } catch (e: Exception) {
                return NfcReadResult.Error(e.message ?: "Failed to read NFC tag")
            }
        }

        if (message == null || message.records.isEmpty()) {
            return NfcReadResult.EmptyTag
        }

        return parseNdefMessage(message)
    }

    private fun parseNdefMessage(message: NdefMessage): NfcReadResult {
        for (record in message.records) {
            val payloadBytes = record.payload ?: continue

            // 1. Check if compressed GZIP stream
            if (NfcPayload.isGzipStream(payloadBytes)) {
                val payload = NfcPayload.fromCompressedBytes(payloadBytes)
                if (payload != null) return NfcReadResult.Success(payload)
            }

            // 2. Check if standard NDEF Text Record (TNF_WELL_KNOWN with RTD_TEXT)
            if (record.tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.type, NdefRecord.RTD_TEXT)) {
                if (payloadBytes.isNotEmpty()) {
                    val statusByte = payloadBytes[0].toInt()
                    val isUtf16 = (statusByte and 0x80) != 0
                    val langCodeLength = statusByte and 0x3F
                    val charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8
                    val textOffset = 1 + langCodeLength
                    if (payloadBytes.size > textOffset) {
                        val text = String(payloadBytes, textOffset, payloadBytes.size - textOffset, charset)
                        val payload = NfcPayload.fromJson(text)
                        if (payload != null) return NfcReadResult.Success(payload)
                    }
                }
            }

            // 3. Check if raw JSON or MIME payload
            val asString = try {
                String(payloadBytes, Charsets.UTF_8)
            } catch (e: Exception) {
                ""
            }
            if (asString.isNotBlank()) {
                val payload = NfcPayload.fromJson(asString)
                if (payload != null) return NfcReadResult.Success(payload)
            }
        }

        return NfcReadResult.InvalidData
    }

    fun writeToTag(tag: Tag, payload: NfcPayload): NfcWriteResult {
        try {
            val jsonString = payload.toJson()
            val compressedBytes = payload.toCompressedBytes()

            // Choose representation: if compressed is smaller and exceeds ~300 bytes, use compressed MIME record,
            // otherwise use standard Text Record for universal compatibility
            val record = if (payload.type == "full_backup" || compressedBytes.size < jsonString.toByteArray(Charsets.UTF_8).size - 50) {
                NdefRecord.createMime("application/vnd.catalyst.data", compressedBytes)
            } else {
                NdefRecord.createTextRecord("en", jsonString)
            }

            val ndefMessage = NdefMessage(arrayOf(record))
            val messageSize = ndefMessage.byteArrayLength

            val ndef = Ndef.get(tag)
            if (ndef != null) {
                if (!ndef.isConnected) {
                    ndef.connect()
                }
                if (!ndef.isWritable) {
                    try { ndef.close() } catch (e: Exception) {}
                    return NfcWriteResult.ReadOnlyTag
                }
                val availableSize = ndef.maxSize
                if (availableSize in 1 until messageSize) {
                    try { ndef.close() } catch (e: Exception) {}
                    return NfcWriteResult.InsufficientSpace(
                        requiredBytes = messageSize,
                        availableBytes = availableSize
                    )
                }
                ndef.writeNdefMessage(ndefMessage)
                try { ndef.close() } catch (e: Exception) {}
                return NfcWriteResult.Success
            } else {
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    if (!formatable.isConnected) {
                        formatable.connect()
                    }
                    formatable.format(ndefMessage)
                    try { formatable.close() } catch (e: Exception) {}
                    return NfcWriteResult.Success
                } else {
                    return NfcWriteResult.Error("Tag does not support NDEF format")
                }
            }
        } catch (e: TagLostException) {
            return NfcWriteResult.TagLost
        } catch (e: IOException) {
            return NfcWriteResult.TagLost
        } catch (e: Exception) {
            return NfcWriteResult.Error(e.message ?: "Failed to write NFC tag")
        }
    }

    fun calculatePayloadSize(payload: NfcPayload): Int {
        val jsonString = payload.toJson()
        val compressedBytes = payload.toCompressedBytes()
        val record = if (payload.type == "full_backup" || compressedBytes.size < jsonString.toByteArray(Charsets.UTF_8).size - 50) {
            NdefRecord.createMime("application/vnd.catalyst.data", compressedBytes)
        } else {
            NdefRecord.createTextRecord("en", jsonString)
        }
        return NdefMessage(arrayOf(record)).byteArrayLength
    }
}
