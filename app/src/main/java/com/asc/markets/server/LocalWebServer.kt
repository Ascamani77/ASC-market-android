package com.asc.markets.server

import android.content.res.AssetManager
import java.io.BufferedOutputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Very small single-file HTTP server to serve files from the app's assets directory.
 * Not meant for production, but sufficient to serve local static files to a WebView.
 */
class LocalWebServer(private val assets: AssetManager, private val port: Int = 8080) {
    private val running = AtomicBoolean(false)
    private var serverThread: Thread? = null

    fun start() {
        if (running.get()) return
        running.set(true)
        serverThread = Thread {
            var serverSocket: ServerSocket? = null
            try {
                serverSocket = ServerSocket(port)
                while (running.get()) {
                    val client = serverSocket.accept()
                    Thread { handleClient(client) }.start()
                }
            } catch (e: Exception) {
                // ignore or log
            } finally {
                try {
                    serverSocket?.close()
                } catch (_: Exception) {}
            }
        }
        serverThread?.start()
    }

    fun stop() {
        running.set(false)
        try {
            serverThread?.interrupt()
        } catch (_: Exception) {}
    }

    private fun handleClient(client: Socket) {
        client.use { socket ->
            val input = socket.getInputStream()
            val out = BufferedOutputStream(socket.getOutputStream())
            try {
                val requestLine = readLine(input) ?: return
                // Example: GET /index.html HTTP/1.1
                val parts = requestLine.split(" ")
                if (parts.size < 2) return
                var path = parts[1]
                if (path == "/") path = "/index.html"
                // Normalize and strip leading '/'
                if (path.startsWith("/")) path = path.substring(1)

                // Serve from assets/www/<path>
                val assetPath = "www/$path"
                val mime = mimeTypeFor(assetPath)
                val assetStream: InputStream? = try {
                    assets.open(assetPath)
                } catch (e: Exception) {
                    null
                }

                if (assetStream == null) {
                    val body = "404 Not Found".toByteArray()
                    val header = "HTTP/1.1 404 Not Found\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                    out.write(header.toByteArray())
                    out.write(body)
                    out.flush()
                    return
                }

                val contentBytes = assetStream.readBytes()
                val header = StringBuilder()
                header.append("HTTP/1.1 200 OK\r\n")
                header.append("Content-Type: $mime\r\n")
                header.append("Content-Length: ${contentBytes.size}\r\n")
                header.append("Connection: close\r\n")
                header.append("\r\n")
                out.write(header.toString().toByteArray())
                out.write(contentBytes)
                out.flush()
            } catch (_: Exception) {
            } finally {
                try { out.close() } catch (_: Exception) {}
                try { input.close() } catch (_: Exception) {}
            }
        }
    }

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val c = input.read()
            if (c == -1) break
            if (c == '\r'.code) continue
            if (c == '\n'.code) break
            sb.append(c.toChar())
            if (sb.length > 8192) break
        }
        return if (sb.isEmpty()) null else sb.toString()
    }

    private fun mimeTypeFor(path: String): String {
        return when {
            path.endsWith(".html") || path.endsWith(".htm") -> "text/html; charset=utf-8"
            path.endsWith(".js") -> "application/javascript; charset=utf-8"
            path.endsWith(".css") -> "text/css; charset=utf-8"
            path.endsWith(".svg") -> "image/svg+xml"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".json") -> "application/json; charset=utf-8"
            path.endsWith(".map") -> "application/json; charset=utf-8"
            path.endsWith(".woff2") -> "font/woff2"
            path.endsWith(".woff") -> "font/woff"
            else -> "application/octet-stream"
        }
    }
}
