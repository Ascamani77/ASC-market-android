package com.asc.markets.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

object TelemetryManager {
    private const val TAG = "TelemetryManager"
    private const val FILENAME = "telemetry.log"
    private lateinit var storageDir: File
    private val initialized = AtomicBoolean(false)
    private val uploading = AtomicBoolean(false)
    private var maxFiles = 7
    private var maxBytesPerFile: Long = 2_000_000 // 2 MB default
    private var retentionDays: Long = 30L

    fun init(context: Context) {
        if (initialized.getAndSet(true)) return
        storageDir = context.filesDir
        Log.d(TAG, "Telemetry initialized at ${'$'}{storageDir.absolutePath}")
    }

    fun recordEvent(eventType: String, payload: Map<String, Any?>) {
        try {
            val ts = System.currentTimeMillis()
            val jsonParts = mutableListOf<String>()
            jsonParts.add("\"event\":\"$eventType\"")
            jsonParts.add("\"ts\":$ts")
            payload.forEach { (k, v) ->
                val value = when (v) {
                    null -> "null"
                    is Number, is Boolean -> v.toString()
                    else -> "\"${v.toString().replace("\"", "\\\"")}\""
                }
                jsonParts.add("\"$k\":$value")
            }
            val line = "{${jsonParts.joinToString(",")}}\n"
            val f = File(storageDir, FILENAME)
            FileOutputStream(f, true).use { it.write(line.toByteArray(StandardCharsets.UTF_8)) }
            rotateIfNeeded()
            purgeOldFiles()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to record telemetry: ${'$'}{t.message}")
        }
    }

    /**
     * Export telemetry file path (app-private). Returns null on error.
     */
    fun telemetryFilePath(): String? {
        return try {
            File(storageDir, FILENAME).absolutePath
        } catch (t: Throwable) {
            Log.w(TAG, "telemetryFilePath failed: ${'$'}{t.message}")
            null
        }
    }

    /**
     * Upload telemetry to a remote endpoint (simple POST JSON-lines). Returns true on success.
     * If endpoint is blank, returns false.
     */
    fun uploadTelemetry(endpoint: String): Boolean {
        if (endpoint.isBlank()) return false
        try {
            val f = File(storageDir, FILENAME)
            if (!f.exists() || f.length() == 0L) return false

            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/jsonl")
                connectTimeout = 15_000
                readTimeout = 15_000
            }

            f.inputStream().use { ins ->
                conn.outputStream.use { out -> ins.copyTo(out) }
            }

            val code = conn.responseCode
            return code in 200..299
        } catch (t: Throwable) {
            Log.w(TAG, "uploadTelemetry failed: ${'$'}{t.message}")
            return false
        }
    }

    /**
     * Configure retention: how many telemetry files to keep, and max bytes per file before rotation.
     */
    fun configureRetention(maxFilesKeep: Int = 7, maxBytes: Long = 2_000_000) {
        maxFiles = maxFilesKeep.coerceAtLeast(1)
        maxBytesPerFile = maxBytes.coerceAtLeast(256_000)
    }

    fun configureRetention(retentionDaysKeep: Long = 30L) {
        retentionDays = retentionDaysKeep.coerceAtLeast(1L)
    }

    private fun purgeOldFiles() {
        try {
            val cutoff = System.currentTimeMillis() - retentionDays * 24 * 60 * 60 * 1000L
            storageDir.listFiles { dir, name -> name.startsWith("telemetry") && name.endsWith(".log") }?.forEach { f ->
                if (f.lastModified() < cutoff) {
                    try { f.delete() } catch (_: Throwable) {}
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "purgeOldFiles failed: ${'$'}{t.message}")
        }
    }

    private fun rotateIfNeeded() {
        try {
            val f = File(storageDir, FILENAME)
            if (!f.exists()) return
            if (f.length() <= maxBytesPerFile) return

            val ts = System.currentTimeMillis()
            val rotated = File(storageDir, "telemetry-${ts}.log")
            if (!f.renameTo(rotated)) return

            // ensure we keep only `maxFiles` telemetry logs (including active file)
            val files = storageDir.listFiles { dir, name -> name.startsWith("telemetry") && name.endsWith(".log") }?.sortedByDescending { it.lastModified() } ?: return
            files.drop(maxFiles).forEach { it.delete() }
        } catch (t: Throwable) {
            Log.w(TAG, "rotateIfNeeded failed: ${'$'}{t.message}")
        }
    }

    /**
     * Uploads all telemetry files (rotated and current) one-by-one. On successful upload of a file
     * that file is deleted. Returns number of uploaded files.
     */
    fun uploadAll(endpoint: String): Int {
        if (endpoint.isBlank()) return 0
        if (!uploading.compareAndSet(false, true)) return 0
        try {
            val files = storageDir.listFiles { dir, name -> name.startsWith("telemetry") && name.endsWith(".log") }?.sortedBy { it.name } ?: return 0
            var uploaded = 0
            files.forEach { f ->
                try {
                    val url = URL(endpoint)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        setRequestProperty("Content-Type", "application/jsonl")
                        connectTimeout = 15_000
                        readTimeout = 15_000
                    }
                    f.inputStream().use { ins -> conn.outputStream.use { out -> ins.copyTo(out) } }
                    val code = conn.responseCode
                    if (code in 200..299) {
                        if (!f.delete()) Log.w(TAG, "uploaded but failed to delete ${'$'}{f.name}")
                        uploaded++
                    } else {
                        Log.w(TAG, "uploadAll: server returned $code for ${'$'}{f.name}")
                    }
                } catch (it: Throwable) {
                    Log.w(TAG, "uploadAll file ${'$'}{f.name} failed: ${'$'}{it.message}")
                }
            }
            return uploaded
        } finally {
            uploading.set(false)
        }
    }
}
