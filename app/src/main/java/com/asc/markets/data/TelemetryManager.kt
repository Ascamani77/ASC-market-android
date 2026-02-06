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
}
